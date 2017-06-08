package tools;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;


import subway.newclass.*;

public class SubwayMain {

	/**
	 * 得到两个经纬度的距离
	 * 
	 * @param input
	 * @return
	 */
	public double getDistance(String input) {
		String[] result = input.split("_");
		Double lat1 = Double.parseDouble(result[1]);
		Double lng1 = Double.parseDouble(result[0]);
		Double lat2 = Double.parseDouble(result[3]);
		Double lng2 = Double.parseDouble(result[2]);

		double a, b, R;
		R = 6378137; // 地球半径
		lat1 = lat1 * Math.PI / 180.0;
		lat2 = lat2 * Math.PI / 180.0;
		a = lat1 - lat2;
		b = (lng1 - lng2) * Math.PI / 180.0;
		double d;
		double sa2, sb2;
		sa2 = Math.sin(a / 2.0);
		sb2 = Math.sin(b / 2.0);
		d = 2 * R * Math.asin(Math.sqrt(sa2 * sa2 + Math.cos(lat1) * Math.cos(lat2) * sb2 * sb2));
		return d;
	}

	/**
	 * 根据上海一天的包裹需求进行模拟投递，统计包裹的投递情况
	 * 每个区域要发出的包裹数目等于区域代理点个数占总代理点个数的比例乘以包裹总数
	 * 每个区域要接收的包裹数目等于区域人口比例乘以总包裹数
	 * 每个车站要发出的包裹数等于其人流量占区域人流量的比例乘以区域内发出的总包裹数
	 * 每个车站要接收的包裹数也等于其人流量占区域人流量的比例乘以区域内接收的总包裹数
	 * 车站是中站站点的包裹起始地是自身，否则将其分配到附近最近的中转站点
	 * @param transferStations 中转站点集合
     * @param itemMap 乘客历史进出站记录
	 * @param routeMap 路由
	 * @param TOTAL_PARCEL_PER_DAY 一天总的包裹数
	 * @throws Exception
	 */
	public void deliveryParcelInOneDay(TreeSet<String> transferStations,
									   HashMap<String, SubwayItemSet> itemMap,
									   HashMap<String, TreeMap<Integer, ArrayList<TimeZoneRoute>>> routeMap,
									   int TOTAL_PARCEL_PER_DAY)
			throws Exception {

		int TOTAL_AGENT = 103;                                        //总代理点个数
		int parcelID = 0;
		float totalPopulation = 0;                                        //总人口数
		ArrayList<ShanghaiRegion> regionList = new ArrayList<>();

		//首先读取每个车站的人流量
		HashMap<String, Integer> volumeMap = new HashMap<>();
		Scanner scan = new Scanner(new File(FILE_PREV + "data/tools/backup/old/taxi/python/车站人流量"));
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			String station = csvReader.stationMap.get(line.substring(0, 4)).getMixStation().getChineseName();
			Integer value = Integer.parseInt(line.substring(5));
			if (!volumeMap.containsKey(station)) {
				volumeMap.put(station, 0);
			}
			volumeMap.put(station, value + volumeMap.get(station));
		}
		scan.close();

		//读取每个行政区域的信息
		scan = new Scanner(new File(FILE_PREV + "data/tools/backup/old/taxi/python/行政区"));
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			String[] results = line.split(",");
			ShanghaiRegion region = new ShanghaiRegion();
			region.setRegionID(Integer.parseInt(results[0]));
			region.setName(results[1]);
			region.setArea(Integer.parseInt(results[2]));
			region.setPopulation(Float.parseFloat(results[3]));
			region.setPostID(Integer.parseInt(results[4]));
			region.setAgentNumber(Integer.parseInt(results[5]));
			//每个区域要发送的包裹数是代理点比例乘以总包裹数
			region.totalParcelToSend = TOTAL_PARCEL_PER_DAY * region.getAgentNumber() / TOTAL_AGENT;
			region.totalTraffic = 0;

			if (results.length <= 6) {
				continue;
			}
			totalPopulation += region.getPopulation();
			for (int i = 6; i < results.length; i++) {
				String station = results[i];
				if (!volumeMap.containsKey(station)) {
					System.out.println("没有" + station);
					continue;
				}
				region.stationList.add(station);
				region.totalTraffic += volumeMap.get(station);
			}
			regionList.add(region);
		}
		scan.close();

		//设置每个区域要接受的包裹数目
		for (ShanghaiRegion region : regionList) {
			region.totalParcelToRecv = (int) (TOTAL_PARCEL_PER_DAY * region.getPopulation() / totalPopulation);
		}

		//parcelsInTransferStation存储每个中转站点的包裹
		//expectHourMap存储预计N小时内送达的包裹数有多少
		//actualHourMap存储实际N小时内送达的包裹数有多少
		HashMap<String, ArrayList<Parcel>> parcelsInTransferStation = new HashMap<>();
		TreeMap<Integer, Integer> expectHourMap = new TreeMap<>();
		TreeMap<Integer, Integer> actualHourMap = new TreeMap<>();

		for(int i = 1; i <= 10; i++) {
			expectHourMap.put(i, 0);
			actualHourMap.put(i, 0);
		}

		int parcelNumWIthRoute = 0;
		Date baseTime = format.parse("2015-04-01 07:00:00");
        //recvCntMap存储每个车站需要接收多少个包裹
		HashMap<String, Integer> recvCntMap = new HashMap<>();
        for(String realSrcStation : transferStations) {
			int parcelNumInStation = TOTAL_PARCEL_PER_DAY / transferStations.size();
			if (!parcelsInTransferStation.containsKey(realSrcStation)) {
				parcelsInTransferStation.put(realSrcStation, new ArrayList<Parcel>());
			}

			//生成每个包裹的信息 包裹的时间是早上7点到晚上7点的随机值
			for (int i = 0; i < parcelNumInStation; i++) {
				Parcel parcel = new Parcel();
				parcel.setId(i + parcelID);
				parcel.setDelivered(false);
				parcel.setCurrentTime(new Date(baseTime.getTime() + random.nextInt(3600000 * 12)));
				parcel.setPickInTime(new Date(parcel.getCurrentTime().getTime()));
				parcel.setDst(realSrcStation);
				parcel.setRoute(null);
				parcelsInTransferStation.get(realSrcStation).add(parcel);

				//设置包裹的目的地
				int id = random.nextInt(TOTAL_PARCEL_PER_DAY);
				int count = 0;
                for(ShanghaiRegion region : regionList) {
                    if (count <= id && id < region.totalParcelToRecv + count) {
						id = random.nextInt(region.totalTraffic);
						count = 0;
						for(String dstStation : region.stationList) {
							int stationParcels = volumeMap.get(dstStation);
							if (count <= id && id < count + stationParcels) {
								parcel.setDst(dstStation);
								if (!recvCntMap.containsKey(dstStation)) {
									recvCntMap.put(dstStation, 0);
								}
								recvCntMap.put(dstStation, recvCntMap.get(dstStation) + 1);
                                break;
							}
							count += stationParcels;
						}
						break;
					}
					count += region.totalParcelToRecv;
				}

				// 获取合适的路由
				String srcDst = realSrcStation + " " + parcel.getDst();
				if (!routeMap.containsKey(srcDst)) {
					continue;
				}
				boolean found = false;
				for (Integer expectHour : routeMap.get(srcDst).keySet()) {
					ArrayList<TimeZoneRoute> zoneList = routeMap.get(srcDst).get(expectHour);
					for (TimeZoneRoute zone : zoneList) {
						if (zone.beginTime.getTime() <= parcel.getPickInTime().getTime() &&
								zone.endTime.getTime() > parcel.getPickInTime().getTime()) {
							found = true;
							parcel.setRoute(zone.routeList);
							parcel.expectHour = expectHour;
							parcelNumWIthRoute++;

							break;
						}
					}
					if (found) {
						break;
					}
				}
				//如果包裹没有合适的路由 设置成直达的路由
				if (parcel.getRoute() == null)
				{
					ArrayList<String> list = new ArrayList<>();
					list.add(realSrcStation);
					list.add(parcel.getDst());
					parcel.setRoute(list);
                    parcel.expectHour = 10;
				}
			}
			parcelID += parcelNumInStation;
		}

		int totalParcel = 0;
		for (String station : parcelsInTransferStation.keySet()) {
			totalParcel += parcelsInTransferStation.get(station).size();
		}

		//开始模拟包裹投递 每隔一秒更新包裹状态
		Date currentDate = format.parse("2015-04-01 07:00:00");
		int currentHour = currentDate.getHours();
		int successDelivered = 0;
        long totalCostMinutes = 0;
		System.out.println("开始试验 总包裹数 " + totalParcel + " 有路径的包裹数 " + parcelNumWIthRoute);

		while (true) {
			//遍历所有的通行记录进行投递
			for (Entry<String, SubwayItemSet> entry : itemMap.entrySet()) {
				String src = entry.getKey();

				int timeIndex = (int) (currentDate.getTime() / 1000);
				timeIndex = timeIndex - timeIndex % SubwayItemSet.TIME_ZONE;

				if (itemMap.get(src).getArriveSet().containsKey(timeIndex)) {
					//遍历每条记录
					for (SubwayItem item : itemMap.get(src).getArriveSet().get(timeIndex)) {
						//首先判断当前乘客是否可以携带
                        if (item.currentCarryCount >= SubwayItem.MAX_CARRY_COUNT) {
							continue;
						}
						//然后遍历车站对应的包裹列表 检查是否有合适的包裹
						for (int i = 0; i < parcelsInTransferStation.get(src).size(); i++) {
							Parcel parcel = parcelsInTransferStation.get(src).get(i);
							//没有路径或是到达目的地的包裹
							if (parcel.getRoute() == null || parcel.getDst().equals(src) || parcel.isDelivered()) {
								continue;
							}
							//乘客到达时间比包裹时间更早的
							if (parcel.getCurrentTime().getTime() > item.getArrive().getTime()) {
								continue;
							}

							int currentIndex = parcel.getRoute().indexOf(src);
							int nextIndex = parcel.getRoute().indexOf(item.getDst());
							if (currentIndex < 0) {
								System.out.println("路径出错了");
								continue;
							}

							//乘客的出行符合投递路径
							if (currentIndex == nextIndex - 1) {
								item.currentCarryCount++;
                                //把包裹放置在相应的车站 并从当前车站删除
								if (parcelsInTransferStation.containsKey(item.getDst())) {
									parcelsInTransferStation.get(item.getDst()).add(parcel);
									parcelsInTransferStation.get(src).remove(i);
									i--;
								}
								//设置包裹的到达下一站的时间
								parcel.setCurrentTime(item.getLeave());
								parcel.transferCnt++;

								if (parcel.getDst().equals(item.getDst())) {
									successDelivered++;
									int actualHour = (int) ((item.getLeave().getTime() - parcel.getPickInTime().getTime())
											/ 1000 / 3600 + 1);
									totalCostMinutes += (item.getLeave().getTime() - parcel.getPickInTime().getTime())
											/ 60000;
									if (!actualHourMap.containsKey(actualHour)) {
										actualHourMap.put(actualHour, 0);
									}
									expectHourMap.put(parcel.expectHour, expectHourMap.get(parcel.expectHour) + 1);
									actualHourMap.put(actualHour, actualHourMap.get(actualHour) + 1);
									parcel.setDelivered(true);
								}
							}
							if (item.currentCarryCount >= SubwayItem.MAX_CARRY_COUNT) {
								break;
							}
						}
					}
				}
			}

			currentDate.setTime(currentDate.getTime() + SubwayItemSet.TIME_ZONE * 1000);
			if (currentDate.getHours() >= 22 || totalParcel == successDelivered) {
				System.out.println("Experiment Done!");
				System.out.println("总包裹数: " + totalParcel+ " 总成功投递数: "
						+ successDelivered + " 百分比: " + (float) successDelivered / totalParcel);
				System.out.println("平均每个包裹耗时: " + (totalCostMinutes) / successDelivered + " 分钟");

				break;
			}
			//每过1小时更新下实验信息
			if (currentHour < currentDate.getHours()) {
				System.out.println(format.format(currentDate) + " 目前成功了: " + successDelivered +
						" 个包裹. 各个预计时间的情况如下");
				String output1 = "", output2 = "";
				//输出格式：预计i小时内送达 对应该时间下总的包裹数
				for (Integer i : actualHourMap.keySet()) {
					output1 = output1 + expectHourMap.get(i) + " ";
				}
				//输出格式：预计i小时内送达 对应该时间下实际送达的包裹数
				for (Integer i : actualHourMap.keySet()) {
					output2 = output2 + actualHourMap.get(i) + " ";
				}
				System.out.println(output1);
				System.out.println(output2);
//				System.out.println("平均每个包裹耗时: " + (totalCostMinutes) / successDelivered + " 分钟");
//				System.out.println("参与的乘客数: " + participatedPassenger);
				currentHour = currentDate.getHours();
			}
		}
	}

	/**
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		FILE_PREV = "/home/lu/workplace/";
		SubwayMain subwayMain = new SubwayMain();
		subwayMain.random = new Random();
		subwayMain.csvReader = new CSVReader(FILE_PREV + "data/station.csv");
		subwayMain.csvReader.readStationItem();
		subwayMain.csvReader.readPosition();
		subwayMain.shortestPath = new ShortestPath(subwayMain.csvReader);

        //模拟一天包裹的投递情况实验
        //参数args分别为： 乘客参与率 一天总的包裹数 每个乘客携带的最大包裹数
        if (args.length == 3)
		{
			HashMap<String, SubwayItemSet> itemMap = new HashMap<>();
		    TreeSet<String> transferSet = new TreeSet<>();

            //设置乘客最多能同时携带多少个包裹数
			SubwayItem.MAX_CARRY_COUNT = Integer.parseInt(args[2]);

			//读取中转站点集合
			Scanner scanner = new Scanner(new File(FILE_PREV + "data/tools/backup/old/taxi/python/中转站点"));
			while (scanner.hasNextLine())
			{
				String line = scanner.nextLine();
				transferSet.add(line);
                itemMap.put(line, new SubwayItemSet());
			}
			scanner.close();

			//读取出行记录
            int itemCnt = 0;
			scanner = new Scanner(new File(FILE_PREV + "data/subwayData/rawData/all/" + args[0] + "/20150403"));
			while (scanner.hasNextLine())
			{
				String line = scanner.nextLine();
				SubwayItem item = new SubwayItem(line);
				item.setSrc(subwayMain.csvReader.stationMap.get(item.getSrc()).getMixStation().getChineseName());
				item.setDst(subwayMain.csvReader.stationMap.get(item.getDst()).getMixStation().getChineseName());
				//跳过七点不是中转站点 过早或过晚的记录
				if (!transferSet.contains(item.getSrc()) ||
						item.getArrive().getHours() >= 22 ||
						item.getArrive().getHours() < 7 ||
						item.getSrc().equals(item.getDst()))
				{
					continue;
				}
				itemMap.get(item.getSrc()).add(item);
                itemCnt++;
			}
			scanner.close();
			System.out.println("中转站点的总人流量 " + itemCnt);

            //设置每个站点最近的中转车站
            subwayMain.csvReader.setCorrespondingStation(transferSet);

			//OD 预计小时数 路由
            HashMap<String, TreeMap<Integer, ArrayList<TimeZoneRoute>>> routeMap = new HashMap<>();

			boolean flag = false;
            int currentHour = 1;
			//读取各个OD的路由信息 args[0]表示不同的乘客参与率
			scanner = new Scanner(new File("/home/lu/workplace/experiment/subwayFinal/sample" + args[0]));
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] result = line.split(" ");
				if (result[0].equals("参与率"))
				{
					if (result[5].startsWith("0.95")) //预计成功率为95%的路由
					{
                        flag = true;
						currentHour = Integer.parseInt(result[7]) / 60;
					}
					else
					{
						flag = false;
					}
					continue;
				}
				if (flag)
				{
					if (result.length == 3 && transferSet.contains(result[0]))
					{
						String srcDst = result[0] + " " + result[1];
                        if (!routeMap.containsKey(srcDst)) {
							routeMap.put(srcDst, new TreeMap<Integer, ArrayList<TimeZoneRoute>>());
						}
						routeMap.get(srcDst).put(currentHour, new ArrayList<TimeZoneRoute>());
					}
					else if (transferSet.contains(result[4]))
					{
						String srcDst = result[4] + " " + result[result.length - 1];
						TimeZoneRoute timeZoneRoute = new TimeZoneRoute();
						timeZoneRoute.beginTime = subwayMain.format.parse("2015-04-01 " + result[0]);
						timeZoneRoute.endTime = subwayMain.format.parse("2015-04-01 " + result[1]);
						for(int i = 4; i < result.length; i++) {
							timeZoneRoute.routeList.add(result[i]);
						}
						routeMap.get(srcDst).get(currentHour).add(timeZoneRoute);
					}
				}
			}
			scanner.close();
			//进行实验
			subwayMain.deliveryParcelInOneDay(transferSet, itemMap, routeMap, Integer.parseInt(args[1]));
			return;
		}

		//生成各个OD在各个时间区间下的路由实验
		//参数为乘客参与率
		else
		{
			subwayMain.probability = args[0]; 		//乘客参与率 0.1 0.2 0.5等
			//首先构建所有的OD 放置在odList中
			ArrayList<String> odList = new ArrayList<>();
			for(String src : subwayMain.csvReader.mixStationMap.keySet()) {
				for(String dst : subwayMain.csvReader.mixStationMap.keySet()) {
					if(src.equals(dst)) {
						continue;
					}
					odList.add(src + " " + dst);
				}
			}

			RouteUtil routeUtil = new RouteUtil(RouteUtil.ROUTE_WITH_TIME_CONSTRAIN, 0, subwayMain.csvReader,
					null, null);
			routeUtil.participateRatio = subwayMain.probability;
            //首先从文件中读取每个OD在不同时间区间下的均值和方差
			routeUtil.readRouteInfoFromFile(subwayMain.probability);

			//下一行被注释掉的是根据人流量决定的中转站点，中转站点个数分别为10 20 30 40 50，可根据需要修改
			//代码里读取的"中转站点"文件是考虑了人流量和覆盖范围决定的20个站点
//		    Scanner scanner = new Scanner(new File("/home/lu/workplace/experiment/subwayWithTaxi/station/top" + storage));
			Scanner scanner = new Scanner(new File("/home/lu/workplace/data/tools/backup/old/taxi/python/中转站点"));
			TreeSet<String> storageSet = new TreeSet<>();
			String line = null;
			while (scanner.hasNextLine()) {
				line = scanner.nextLine();
				storageSet.add(line);
			}

			//设置储藏柜站点
			routeUtil.storageStation = storageSet;
            //预计成功率分别为0.8 0.85 0.9 0.95 0.99
			for(double limitProb = 0.80; limitProb <= 1.02; limitProb += 0.05) {
				if(limitProb > 0.99) {
					limitProb = 0.99;
				}
				//预计投递时间分别为1小时 2小时 3小时 4小时 5小时 6小时。也就是说先算1小时内是否可达，可达的下一轮忽略，不可达的再算2小时是否可达。。。
				for (int minute = 60; minute <= 360; minute += 60) {
					//需求是，给定一个概率 求满足该概率的最小传送时间（小时） 以及对应的跳数
					System.out.println("参与率 " + routeUtil.participateRatio + " 储物柜 " + storageSet.size() +
							" 概率 " + limitProb + " 规定时间 " + minute);
					Date begin = subwayMain.format.parse("2015-04-01 07:00:00");
					Date end = subwayMain.format.parse("2015-04-01 21:00:00");
					routeUtil.getLongestRouteWithProb(begin, end, 4, minute, limitProb);
				}
			}
		}

	}

	public static String FILE_PREV ;
	public String probability;
	public String storageNumber;
	public String routeMethod;
	public Random random;
	public CSVReader csvReader;
	public ShortestPath shortestPath;
	public SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
}
