package tools;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import subway.newclass.*;

public class RouteUtil {

	public static final int ROUTE_WITH_MIN_DELAY = 0;		//不同的路由方式
	public static final int ROUTE_WITH_MIN_HOP = 1;
	public static final int ROUTE_WITH_TIME_CONSTRAIN = 2;
	public static final int MIN_ZONE_INTERVAL = 1800;
	public static final int SMALL_TIME_INTERVAL = 1800;		//区间大小为30分钟
	public static final int MIN_COVER_TIME = 3600 * 5;		//最小覆盖时间为5小时
	public CSVReader csvReader;
	public HashMap<String, HashMap<String, TreeMap<Integer, SimpleDelay>>> delayItemMap;	//每个区间的人流量 平均运行时间 前后30分钟人流量
	public TreeSet<String> storageStation;					//储藏柜集合
	public SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
	public HashMap<String, ArrayList<TimeZoneRoute>> staticRouteMap = new HashMap<>();		//从文件直接读取的路由
	public HashMap<String, HashMap<String, TreeMap<Integer, RouteInfo>>> staticInfoMap = new HashMap<>();
	public HashMap<String, Integer>	reachHourMap = new HashMap<>();							//每个站点的预计可达时间
	public HashMap<Integer, HashMap<String, ArrayList<TimeZoneRoute>>> staticRouteMapWithProb = new HashMap<>();
	public HashMap<Integer, HashMap<String, Integer>> reachHourMapWithProb = new HashMap<>();	//只是在不同参与率下使用
	public HashMap<String, ArrayList<SubwayArrivalTime>> recordItemMap = new HashMap<>();	//所有的打卡记录
	public String participateRatio;		//参与率
	public int routeConstrain;			//最大跳数
	public int timeConstrain;			//最长传递时间 小时

	public RouteUtil(){
		this.csvReader = null;
		this.delayItemMap = null;
		this.storageStation = null;
		this.routeConstrain = ROUTE_WITH_MIN_DELAY;
		this.timeConstrain = 10;
	}
	public RouteUtil(int routeConstrain, int timeConstrain, CSVReader reader, TreeSet<String> storage,
			HashMap<String, HashMap<String, TreeMap<Integer, SimpleDelay>>> map) {
		this.csvReader = reader;
		this.delayItemMap = map;
		this.storageStation = storage;
		this.routeConstrain = routeConstrain;
		this.timeConstrain = timeConstrain;
	}

	/**
	 * 合并相邻的区间
	 * @param routeList
     */
	private void adjustZone(ArrayList<TimeZoneRoute> routeList) {
		for(int i = 0; i < routeList.size() - 1; i++)
		{
			TimeZoneRoute zone1 = routeList.get(i);
			TimeZoneRoute zone2 = routeList.get(i + 1);
			//如果前后两个区间的间隔小于半小时 并且每个站点都相同 合并
			if(zone2.beginTime.getTime() - zone1.endTime.getTime() <= MIN_ZONE_INTERVAL * 1000
				&& zone1.routeList.size() == zone2.routeList.size())
			{
				boolean flag = true;
				for(int j = 0; j < zone1.routeList.size(); j++)
				{
					if (!zone1.routeList.get(j).equals(zone2.routeList.get(j))) {
						flag = false;
						break;
					}
				}
				if(flag)
				{
					zone1.endTime = zone2.endTime;
					routeList.remove(i + 1);
					i--;
				}
			}
		}
	}

	/**
	 * 合并相邻的RouteInfo, limitHour是规定的小时数
	 * @param routeList
	 * @param maxMinute
	 * @param minProb
     */
	private void adjustRouteInfoZone(ArrayList<RouteInfo> routeList, double maxMinute, double minProb) {
		for(int i = 0; i < routeList.size() - 1; i++)
		{
			RouteInfo zone1 = routeList.get(i);
			RouteInfo zone2 = routeList.get(i + 1);
            //如果前后时间相邻 车站序列也相同 合并
			if (zone2.getBeginTime().getTime() - zone1.getEndTime().getTime() <= SMALL_TIME_INTERVAL * 1000 &&
					zone1.getStationList().equals(zone2.getStationList()))
			{
				//重新计算E(X) E(X^2) D(X)
				double avgExcept = (zone1.getExpect() + zone2.getExpect()) / 2;
				RouteInfo routeInfo = new RouteInfo();
				routeInfo.getBeginTime().setTime(zone1.getBeginTime().getTime());
				routeInfo.getEndTime().setTime(zone2.getEndTime().getTime());
				routeInfo.setRunExcept(avgExcept);
				routeInfo.setStationList(zone1.getStationList());
				routeList.remove(i);
				routeList.remove(i);
				routeList.add(i, routeInfo);
				i--;
			}
		}
	}

	/**
	 * 将两个路径进行融合 首先将时间点排序 分段决定
	 * @param list1
	 * @param list2
	 * @return
	 */
	private ArrayList<RouteInfo> combineRouteInfoList(ArrayList<RouteInfo> list1, ArrayList<RouteInfo> list2,
													  double maxMinute, double minProb) {
		if(list1 == null || list1.size() == 0)
		{
			return list2;
		}
		if(list2 == null || list2.size() == 0)
		{
			return list1;
		}
		ArrayList<RouteInfo> routeList = new ArrayList<RouteInfo>();
		int first = 0, second = 0;
		while (first < list1.size() && second < list2.size()) {
			RouteInfo info1 = list1.get(first);
			RouteInfo info2 = list2.get(second);
			if (info1.getBeginTime().getTime() > info2.getBeginTime().getTime()) {
				routeList.add(info2);
				second++;
			}
			else if (info1.getBeginTime().getTime() < info2.getBeginTime().getTime()) {
				routeList.add(info1);
				first++;
			}
			else {
				RouteInfo minDelayRoute = info1;
				if (info1.getExpect() > info2.getExpect()) {
					minDelayRoute = info2;
				}

				RouteInfo minHopRoute = info1;
				if(info2.getStationList().size() < info1.getStationList().size()) {
					minHopRoute = info2;
				}
				else if(info1.getStationList().size() == info2.getStationList().size()) {
					minHopRoute = minDelayRoute;
				}

				//最小延迟 选择延迟更小的
				if (routeConstrain == ROUTE_WITH_MIN_DELAY) {
					routeList.add(minDelayRoute);
				}
				//最少跳数 选择跳数较小的
				else if(routeConstrain == ROUTE_WITH_MIN_HOP) {
					routeList.add(minHopRoute);
				}
				//带上时间限制 优先检查跳数更少的是否满足
				else if (routeConstrain == ROUTE_WITH_TIME_CONSTRAIN) {
					double p = minHopRoute.getProbability(maxMinute);
					if(p - minProb >= 0.0001) {
						routeList.add(minHopRoute);
					}
					else {
						p = minDelayRoute.getProbability(maxMinute);
						if(p - minProb >= 0.0001) {
							routeList.add(minDelayRoute);
						}
					}
				}
				first++;
				second++;
			}
		}
		while (first < list1.size()) {
			routeList.add(list1.get(first));
			first++;
		}
		while (second < list2.size()) {
			routeList.add(list2.get(second));
			second++;
		}
		return routeList;
	}

	/**
	 * 将两个路径进行融合 首先将时间点排序 分段决定
	 * @param list1
	 * @param list2
     * @return
     */
	private ArrayList<TimeZoneRoute> combineZoneList(ArrayList<TimeZoneRoute> list1, ArrayList<TimeZoneRoute> list2) {
		if(list1 == null || list1.size() == 0)
		{
			return list2;
		}
		if(list2 == null || list2.size() == 0)
		{
			return list1;
		}
		ArrayList<TimeZoneRoute> routeList = new ArrayList<TimeZoneRoute>();
		ArrayList<Date> timeList = new ArrayList<Date>();
		
		//首先将两个路径上的时间点排序
		int left = 1;
		int right = 0;
		timeList.add(list1.get(0).beginTime);
		if(list2.get(0).beginTime.getTime() < list1.get(0).beginTime.getTime())
		{
			left = 0;
			right = 1;
			timeList.clear();
			timeList.add(list2.get(0).beginTime);
		}
		while(left / 2 < list1.size() && right / 2 < list2.size())
		{
			Date first = null;
			Date second = null;
			if (left % 2 == 0) {
				first = list1.get(left / 2).beginTime;
			}
			else
			{
				first = list1.get(left / 2).endTime;
			}
			if (right % 2 == 0) {
				second = list2.get(right / 2).beginTime;
			}
			else
			{
				second = list2.get(right / 2).endTime;
			}
			if(first.getTime() < second.getTime())
			{
				timeList.add(first);
				left++;
			}
			else
			{
				timeList.add(second);
				right++;
			}
		}
		while(left / 2 < list1.size())
		{
			if(left % 2 == 0)
			{
				timeList.add(list1.get(left / 2).beginTime);
			}
			else
			{
				timeList.add(list1.get(left / 2).endTime);
			}
			left++;
		}
		while(right / 2 < list2.size())
		{
			if(right % 2 == 0)
			{
				timeList.add(list2.get(right / 2).beginTime);
			}
			else
			{
				timeList.add(list2.get(right /2).endTime);
			}
			right++;
		}
		
		
		//如果前后的时间一样 删除一个
		for(int i = 0; i < timeList.size() - 1; i++)
		{
			if (timeList.get(i).equals(timeList.get(i + 1))) 
			{
				timeList.remove(i + 1);
				i--;
			}
		}
		
		//重置下标 判断每个新的区间应该选什么线路
		left = right = 0;
		for(int i = 0; i < timeList.size() - 1; i++)
		{
			Date begin = timeList.get(i);
			Date end = timeList.get(i + 1);
			TimeZoneRoute route1 = list1.get(left);
			TimeZoneRoute route2 = list2.get(right);
			TimeZoneRoute tmpZone = new TimeZoneRoute();
			tmpZone.beginTime.setTime(begin.getTime());
			tmpZone.endTime.setTime(end.getTime());;
			//如果新的区间开始段大于等于当前区间的结束段
			if (begin.getTime() >= route1.endTime.getTime()) {
				if (left < list1.size() - 1) {
					left++;
					route1 = list1.get(left);
				}
			}
			if (begin.getTime() >= route2.endTime.getTime()) {
				if (right < list2.size() - 1) {
					right++;
					route2 = list2.get(right);
				}
			}
			
			boolean flag1 = false;
			boolean flag2 = false;
			
			if (begin.getTime() >= route1.beginTime.getTime() 
				&& end.getTime() <= route1.endTime.getTime()) {
				flag1 = true;
			}
			if (begin.getTime() >= route2.beginTime.getTime()
				&& end.getTime() <= route2.endTime.getTime()) {
				flag2 = true;
			}
			//当前区间如果位于第1个链表的当前区间中间
			if (flag1 && !flag2) 
			{
				tmpZone.routeList.addAll(route1.routeList);
				routeList.add(tmpZone);
			}
			//当前区间如果位于第2个链表的当前区间中间
			if (!flag1 && flag2)
			{
				tmpZone.routeList.addAll(route2.routeList);
				routeList.add(tmpZone);
			}
			//同时位于两个链表
			if(flag1 && flag2)
			{
				//默认是按照最小延迟
				TimeZoneRoute zone1 = new TimeZoneRoute();
				TimeZoneRoute zone2 = new TimeZoneRoute();
				zone1.beginTime.setTime(begin.getTime());
				zone1.endTime.setTime(end.getTime());
				zone1.routeList.addAll(route1.routeList);

				zone2.beginTime.setTime(begin.getTime());
				zone2.endTime.setTime(end.getTime());
				zone2.routeList.addAll(route2.routeList);
				TimeZoneRoute minDelayRoute = compareInZone(zone1, zone2);
				
				TimeZoneRoute minHopRoute = route1;
				if(route2.routeList.size() == route1.routeList.size()) {
					minHopRoute = minDelayRoute;
				}
				else if(route2.routeList.size() < route1.routeList.size()) {
					minHopRoute = route2;
				}
				//最小延迟 选择延迟更小的
				if (routeConstrain == ROUTE_WITH_MIN_DELAY) {
					tmpZone.routeList.addAll(minDelayRoute.routeList);
					routeList.add(tmpZone);
				}
				//最少跳数 选择跳数较小的
				else if(routeConstrain == ROUTE_WITH_MIN_HOP) {
					tmpZone.routeList.addAll(minHopRoute.routeList);
					routeList.add(tmpZone);
				}
				//带上时间限制 优先检查跳数更少的是否满足
				else if (routeConstrain == ROUTE_WITH_TIME_CONSTRAIN) {
					int delay = getDelayOfRoute(minHopRoute);
					if(delay / 3600 < timeConstrain) {
						tmpZone.routeList.addAll(minHopRoute.routeList);
						routeList.add(tmpZone);
					}
					else {
						delay = getDelayOfRoute(minDelayRoute);
						if(delay / 3600 < timeConstrain) {
							tmpZone.routeList.addAll(minDelayRoute.routeList);
							routeList.add(tmpZone);
						}
					}
				}
			}
		}
		
		//缩小区间
		adjustZone(routeList);
		return routeList;
	}

	/**
	 * 得到一个TimeZoneRoute的延迟
 	 * @param src
	 * @param dst
	 * @param medium
     * @return
     */
	private int getDelayOfRouteTime(String src, String dst, int medium)
	{
		medium = medium - medium % SMALL_TIME_INTERVAL;
		int left = medium;
		int right = medium;

		//以mid为中心 在窗口大小为1小时内查找
		while(left >= medium - MIN_ZONE_INTERVAL / 2 &&
				right <= medium + MIN_ZONE_INTERVAL / 2)
		{
			if (delayItemMap.get(src).get(dst).containsKey(right)) {
				return (int) delayItemMap.get(src).get(dst).get(right).getRunDelay();
			}
			if (delayItemMap.get(src).get(dst).containsKey(left)) {
				return (int) delayItemMap.get(src).get(dst).get(left).getRunDelay();
			}
			left -= SMALL_TIME_INTERVAL;
			right += SMALL_TIME_INTERVAL;
		}
		return -1;
	}

	/**
	 * 粗略的返回一个路由的延迟
	 * @param route
	 * @return
	 */
	private int getDelayOfRoute(TimeZoneRoute route)
	{
		if(route.routeList.size() <= 1)
		{
			return -1;
		}
		int time = (int) (route.beginTime.getTime() / 1000);
		int delay = 0;
		for(int i = 0; i < route.routeList.size() - 1; i++)
		{
			String src = route.routeList.get(i);
			String dst = route.routeList.get(i + 1);
			int tmpDelay = getDelayOfRouteTime(src, dst, time);
			
			if(tmpDelay  < 0)
			{
				return -1;
			}
			time += tmpDelay;
			delay += tmpDelay;
		}
		return delay;
	}


	/**
	 * 比较两个TimeZoneRoue 返回时间较小的
	 * @param zone1
	 * @param zone2
     * @return
     */
	private TimeZoneRoute compareInZone(TimeZoneRoute zone1, TimeZoneRoute zone2) {
		if(!zone1.beginTime.equals(zone2.beginTime) ||
			!zone1.endTime.equals(zone2.endTime))
		{
			return null;
		}
		
		TimeZoneRoute route = new TimeZoneRoute();
		route.beginTime.setTime(zone1.beginTime.getTime());
		route.endTime.setTime(zone1.endTime.getTime());
		int count = (int) ((zone1.endTime.getTime() - zone1.beginTime.getTime()) / 1000 / SMALL_TIME_INTERVAL);
		//比较所有区间 选平均时间较小的
		int first = 0;
		int second = 0;
		for(int i  = 0; i < count; i++)
		{
			//设置每个区间的时间
			TimeZoneRoute route1 = new TimeZoneRoute(zone1);
			TimeZoneRoute route2 = new TimeZoneRoute(zone2);
			route1.beginTime.setTime(route1.beginTime.getTime() + SMALL_TIME_INTERVAL * i * 1000);
			route2.beginTime.setTime(route2.beginTime.getTime() + SMALL_TIME_INTERVAL * i * 1000);
			route1.endTime.setTime(route1.beginTime.getTime() + SMALL_TIME_INTERVAL * 1000);
			route2.endTime.setTime(route2.beginTime.getTime() + SMALL_TIME_INTERVAL * 1000);
			int delay1 = getDelayOfRoute(route1);
			int delay2 = getDelayOfRoute(route2);
			
			if(delay1 < delay2)
			{
				if (delay1 < 0) {
					second++;
				}
				else
				{
					first++;
				}
			}
			if(delay2 < delay1)
			{
				if (delay2 < 0) {
					first++;
				}
				else
				{
					second++;
				}
			}
		}
		if(first < second)
		{
			route.routeList.addAll(zone2.routeList);
		}
		else if(second < first)
		{
			route.routeList.addAll(zone1.routeList);
		}
		//相同的话 取跳数更少的那个
		else
		{
			if (zone1.routeList.size() <= zone2.routeList.size()) {
				route.routeList.addAll(zone1.routeList);
			}
			else
			{
				route.routeList.addAll(zone2.routeList);
			}
		}
		return route;
	}


	/**
	 * 计算当前OD的覆盖率 一个OD覆盖的前提是起可达总时间大于10小时
	 * @param level	跳数
	 * @param total	总的OD数
	 * @param baseRouteMap	当前的路由
	 * @return
	 */
	public double calculateCoverRatio(int level, int total,
			HashMap<String, HashMap<String, ArrayList<TimeZoneRoute>>> baseRouteMap) {
		double count = 0d;
		double cnt = 0d;
		//如果一个站点对的总可达时间超过了10小时 算作可达
		for(String src : baseRouteMap.keySet()) {
			for(String dst : baseRouteMap.get(src).keySet()) {
				long second = 0;
				for(TimeZoneRoute route : baseRouteMap.get(src).get(dst)) {
					second += route.endTime.getTime() / 1000 - route.beginTime.getTime() / 1000;
					if(second >= 36000) {
						break;
					}
				}
				if(second >= 36000) {
					count++;
				}
				cnt++;
			}
		}
		double ratio = count / total;
		double ratioTotal = cnt / total;
		System.out.println("第" + level + "层: 总的OD:" + total + " 现在覆盖: " + count + 
				" 覆盖率: " + ratio + " 总覆盖: " + cnt + " 总覆盖率: " + ratioTotal);
		return ratio;
	}

	/**
	 * 计算当前OD的覆盖率 一个OD覆盖的前提是起可达总时间大于10小时
	 * @param level	跳数
	 * @param total	总的OD数
	 * @param baseRouteMap	当前的路由
	 * @return
	 */
	public double calculateCoverRatioOfRouteInfo(int level, int total,
												 HashMap<String, HashMap<String, ArrayList<RouteInfo>>> baseRouteMap) {
		double count = 0d;
		double cnt = 0d;
		//如果一个站点对的总可达时间超过了10小时 算作可达
		for(String src : baseRouteMap.keySet()) {
			for(String dst : baseRouteMap.get(src).keySet()) {
				long second = 0;
				for(RouteInfo route : baseRouteMap.get(src).get(dst)) {
					second += route.getEndTime().getTime() / 1000 - route.getBeginTime().getTime() / 1000;
					if(second >= 36000) {
						break;
					}
				}
				if(second >= 36000) {
					count++;
				}
				cnt++;
			}
		}
		double ratio = count / total;
		double ratioTotal = cnt / total;
		System.out.println("第" + level + "层: 总的OD:" + total + " 现在覆盖: " + count +
				" 覆盖率: " + ratio + " 总覆盖: " + cnt + " 总覆盖率: " + ratioTotal);
		return ratio;
	}
	/**
	 * 计算所有的路由路径 跳数从1到10递增
	 * @throws Exception
	 */
	public void getRouteOfAllOD() throws Exception{
		
		//首先计算总的OD对 应该有289*289 - 289 = 83232个OD 
		//根据实验统计,每个站点都有人流进出
		//进站人数最少的站点是华夏中路，平均308人次 出站人数最少的站点也是华夏中路，平均236人次
		//进站人数最多的站点是人民广场，平均101625人次 出战人数最多的站点是人民广场，平均105466人次
		int totalOD = csvReader.mixStationMap.size() * (csvReader.mixStationMap.size() - 1);
		double coverRatio = 0d;
		
		//存储每一层的路径
		HashMap<String, HashMap<String, ArrayList<TimeZoneRoute>>> baseRouteMap = 
				new HashMap<String, HashMap<String,ArrayList<TimeZoneRoute>>>();
		//首先根据delayItemMap读取可以直达的OD 
		for(String src : delayItemMap.keySet())
		{
			baseRouteMap.put(src, new HashMap<String, ArrayList<TimeZoneRoute>>());
			for(String dst : delayItemMap.get(src).keySet())
			{
				ArrayList<TimeZoneRoute> tmpRoutes = new ArrayList<TimeZoneRoute>();
				int beginIndex = (int) (format.parse("2015-04-01 07:00:00").getTime() / 1000);
				int endIndex = (int) (format.parse("2015-04-01 22:00:00").getTime() / 1000);
				while(beginIndex < endIndex)
				{
					if (delayItemMap.get(src).get(dst).containsKey(beginIndex)) {
						TimeZoneRoute route2 = new TimeZoneRoute();
						route2.beginTime = new Date((long)beginIndex * 1000);
						route2.endTime = new Date(route2.beginTime.getTime() + 
								RouteUtil.SMALL_TIME_INTERVAL * 1000);
						route2.routeList.add(src);
						route2.routeList.add(dst);
						tmpRoutes.add(route2);
					}
					beginIndex += RouteUtil.SMALL_TIME_INTERVAL;
				}
				if(tmpRoutes.size() > 0)
				{
					//将区间进行合并
					adjustZone(tmpRoutes);
					//删除小的区间
					for(int i = 0; i < tmpRoutes.size(); i++)
					{
						TimeZoneRoute route = tmpRoutes.get(i);
						if (route.endTime.getTime() - route.beginTime.getTime() <= RouteUtil.MIN_ZONE_INTERVAL * 1000) 
						{
							tmpRoutes.remove(i);
							i--;
						}
					}
					if (tmpRoutes.size() > 0) {
						baseRouteMap.get(src).put(dst, tmpRoutes);
					}
				}
			}
			if (baseRouteMap.get(src).size() == 0) {
				baseRouteMap.remove(src);
			}
		}
		
		coverRatio = calculateCoverRatio(1, totalOD, baseRouteMap);
		
		for(int hop = 1; hop <= 2; hop++)
		{
			HashMap<String, HashMap<String, ArrayList<TimeZoneRoute>>> curZoneMap =
					new HashMap<String, HashMap<String,ArrayList<TimeZoneRoute>>>();
			for(String src : csvReader.mixStationMap.keySet())
			{
				//src到中间站点必须在前一层
				if (!baseRouteMap.containsKey(src)) {
					continue;
				}
				curZoneMap.put(src, new HashMap<String, ArrayList<TimeZoneRoute>>());
				for(String dst : csvReader.mixStationMap.keySet())
				{
					//跳过相同的站点
					if(src.equals(dst)) 
					{
						continue;
					}
					
					//这个list存储通过不同中间站点到达目的地的时间区间 最后要进行合并
					ArrayList<ArrayList<TimeZoneRoute>> totalRouteList = 
							new ArrayList<ArrayList<TimeZoneRoute>>();
					//每个中间节点和上一层的区间进行比较
					for(String mid : baseRouteMap.keySet())
					{
						//中间节点不能是起点或是目的地
						if(mid.equals(src) || mid.equals(dst))
						{
							continue;
						}
						//中间站点必须是储藏柜
						if(!storageStation.contains(mid)) {
							continue;
						}
						//src到中间站点必须在前一层
						if(!baseRouteMap.get(src).containsKey(mid))
						{
							continue;
						}
						//中间节点到目的地必须直接到
						if (!delayItemMap.get(mid).containsKey(dst)) {
							continue;
						}
						
						ArrayList<TimeZoneRoute> tmpRoutes = new ArrayList<TimeZoneRoute>();
						
						int beginIndex = (int) (format.parse("2015-04-01 07:00:00").getTime() / 1000);
						int endIndex = (int) (format.parse("2015-04-01 22:00:00").getTime() / 1000);
						
						//记录当前是哪一个时间区间
						int curZoneIndex = 0;
						
						//生成每5分钟的到站时间
						while(beginIndex < endIndex)
						{
							//更新时间
							TimeZoneRoute route =  baseRouteMap.get(src).get(mid).get(curZoneIndex);
							while (route.endTime.getTime() / 1000 <= beginIndex + RouteUtil.SMALL_TIME_INTERVAL) {
								curZoneIndex++;
								if (curZoneIndex >= baseRouteMap.get(src).get(mid).size()) {
									break;
								}
								route =  baseRouteMap.get(src).get(mid).get(curZoneIndex);
							}
							
							if (curZoneIndex >= baseRouteMap.get(src).get(mid).size()) {
								break;
							}
							//当前的时间要在区间里面
							if(beginIndex >= route.beginTime.getTime() / 1000)
							{
								int medium = beginIndex;
								//在到达src的时间处计算从src到mid的时间
								int delay = getDelayOfRoute(route);
								
								//前面的路径能够找到 继续找mid到dst
								if (delay > 0) {

									medium += delay;
									delay = getDelayOfRouteTime(mid, dst, medium);
									if (delay >= 0) {
										TimeZoneRoute route2 = new TimeZoneRoute();
										route2.beginTime = new Date((long)beginIndex * 1000);
										route2.endTime = new Date(route2.beginTime.getTime() + RouteUtil.SMALL_TIME_INTERVAL * 1000);
										route2.routeList.addAll(route.routeList);
										route2.routeList.add(dst);
										tmpRoutes.add(route2);
									}
								}
							}
							beginIndex += RouteUtil.SMALL_TIME_INTERVAL;
						}
						if(tmpRoutes.size() > 0)
						{
							//将区间进行合并
							adjustZone(tmpRoutes);
							//删除小的区间
//							for(int i = 0; i < tmpRoutes.size(); i++)
//							{
//								TimeZoneRoute route = tmpRoutes.get(i);
//								if (route.endTime.getTime() - route.beginTime.getTime() <= MIN_ZONE_INTERVAL * 1000) 
//								{
//									tmpRoutes.remove(i);
//									i--;
//								}
//							}
							if(tmpRoutes.size() > 0)
							{
								totalRouteList.add(tmpRoutes);
							}
						}
					}
					if(totalRouteList.size() > 0)
					{
						//合并线路
						ArrayList<TimeZoneRoute> result = null;
						for(ArrayList<TimeZoneRoute> list : totalRouteList)
						{
							result = combineZoneList(result, list);
						}
						if(baseRouteMap.get(src).containsKey(dst))
						{
							result = combineZoneList(result, baseRouteMap.get(src).get(dst));
						}
						
						if (result != null && result.size() > 0) {
							curZoneMap.get(src).put(dst, result);
						}

					}
				}
				if(curZoneMap.get(src).size() == 0)
				{
					curZoneMap.remove(src);
				}
			}
			baseRouteMap = curZoneMap;
			coverRatio = calculateCoverRatio(hop + 1, totalOD, baseRouteMap);
			if(hop == 2){
				System.out.println("第" + (hop + 1) + "层的路由信息如下:");
				for(String src : baseRouteMap.keySet()) {
					for(String dst : baseRouteMap.get(src).keySet()) {
						System.out.println(src + " " + dst + " " + baseRouteMap.get(src).get(dst).size());
						for(TimeZoneRoute route : baseRouteMap.get(src).get(dst)) {
							System.out.print(simpleDateFormat.format(route.beginTime) + " " +
									simpleDateFormat.format(route.endTime) + " ");
							for(String string : route.routeList)
							{
								System.out.print(string + " ");
							}
							System.out.println();
						}
					}
				}
			}
			if(coverRatio >= 0.99d) {
				break;
			}
		}
	}
	
	/**
	 * 从文件中直接读取路由信息 同时获取每个车站的时间
	 * @param file
	 */
	public void readRouteFromFile(File file) {
		try {
			Scanner scanner = new Scanner(file);
			String line = null;
			String currentSrcDst = null;
			while (scanner.hasNextLine()) {
				line = scanner.nextLine();
				String[] result = line.split(" ");
				if (result.length > 3) {	//生成一个带时间区间的路由
					TimeZoneRoute timeZoneRoute = new TimeZoneRoute();
					timeZoneRoute.beginTime = format.parse("2015-04-01 " + result[0]);
					timeZoneRoute.endTime = format.parse("2015-04-01 " + result[1]);
					for(int i = 2; i < result.length; i++) {
						timeZoneRoute.routeList.add(result[i]);
					}
					staticRouteMap.get(currentSrcDst).add(timeZoneRoute);
				}
				else {
					if(line.contains(":")) {
						System.out.println("不合法的数据" + line);
					}
					currentSrcDst = result[0] + " " + result[1];
					staticRouteMap.put(currentSrcDst, new ArrayList<TimeZoneRoute>());
				}
			}
			scanner.close();
			
			for(String srcDst : staticRouteMap.keySet()) {
				int maxHour = 0;
				for(TimeZoneRoute timeZoneRoute : staticRouteMap.get(srcDst)) {
					//有一定的范围
					int delay = (int) (1.2d * getDelayOfRoute(timeZoneRoute));
					delay = delay / 3600 + 1;
					if(maxHour < delay) {
						maxHour = delay;
					}
				}
				reachHourMap.put(srcDst, maxHour);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	/**
	 * 直接读取时间信息
	 * @param file
	 * @throws Exception
	 */
	public void readReachedHourFromFile(File file) throws Exception {
		reachHourMap.clear();
		Scanner scanner = new Scanner(file);
		String line = null;
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			String[] result = line.split(" ");
			reachHourMap.put(result[0] + " " + result[1], Integer.parseInt(result[2]));
		}
		scanner.close();
	}
	/**
	 * 新的方法 逐跳读取 从文件中直接读取路由信息 同时获取每个车站的时间
	 * @param file
	 */
	public void readRouteFromFileNew(File file, int p) throws Exception {
		System.out.println("readRouteFromFileNew");
		Scanner scanner = new Scanner(file);
		String line = null;
		String currentSrcDst = null;
		int hourLimit = 0;
		int probability = 0;
		ArrayList<TimeZoneRoute> list = new ArrayList<>();
		staticRouteMapWithProb.put(p, new HashMap<String, ArrayList<TimeZoneRoute>>());
		reachHourMapWithProb.put(p, new HashMap<String, Integer>());
		while (scanner.hasNextLine()) {
			line = scanner.nextLine();
			if(line.contains("第")) {
				continue;
			}
			String[] result = line.split(" ");
			if(result.length > 5 && result[4].equals("TimeConstrain:")) {
				hourLimit = Integer.parseInt(result[5]);
			}
			else if(result[0].equals("Probability:")) {
				probability = Integer.parseInt(result[1].substring(2));
			}
			else if(probability == p) {
				if(result.length == 3) {
					//计算所有的时间 如果超过一定范围 则认为属于该时间需求
					int coverTime = 0;
					int maxDelay = 0;
					for(TimeZoneRoute route : list) {
						int cover = (int) (route.endTime.getTime() / 1000 - route.beginTime.getTime() / 1000);
						coverTime += cover;
						int tmpDelay = getDelayOfRoute(route);
						if(tmpDelay > maxDelay) {
							maxDelay = tmpDelay;
						}
					}
					if (coverTime >= MIN_COVER_TIME) {
						ArrayList<TimeZoneRoute> tmpList = new ArrayList<>();
						tmpList.addAll(list);
						staticRouteMapWithProb.get(probability).put(currentSrcDst, tmpList);
						int reachedHour = (int) (1.2 * maxDelay / 3600) + 1;
						if (reachedHour < hourLimit) {
							reachedHour = hourLimit;
						}
						reachHourMapWithProb.get(probability).put(currentSrcDst, reachedHour);
					}
					list.clear();
					currentSrcDst = result[0] + " " + result[1];
				}
				else {
					if(staticRouteMapWithProb.get(probability).containsKey(currentSrcDst)) {
						continue;
					}
					TimeZoneRoute timeZoneRoute = new TimeZoneRoute();
					timeZoneRoute.beginTime = format.parse("2015-04-01 " + result[0]);
					timeZoneRoute.endTime = format.parse("2015-04-01 " + result[1]);
					for(int i = 2; i < result.length; i++) {
						timeZoneRoute.routeList.add(result[i]);
					}
					list.add(timeZoneRoute);
				}
			}
		}
		scanner.close();

		if (list.size() > 0) {
			int maxDelay = 0;
			for(TimeZoneRoute route : list) {
				int tmpDelay = getDelayOfRoute(route);
				if(tmpDelay > maxDelay) {
					maxDelay = tmpDelay;
				}
			}
			ArrayList<TimeZoneRoute> tmpList = new ArrayList<>();
			tmpList.addAll(list);
			staticRouteMapWithProb.get(probability).put(currentSrcDst, tmpList);
			int reachedHour = maxDelay / 3600 + 1;
			if (reachedHour < hourLimit) {
				reachedHour = hourLimit;
			}
			reachHourMapWithProb.get(probability).put(currentSrcDst, reachedHour);
		}

		for(Integer prob : staticRouteMapWithProb.keySet()) {
			FileOutputStream outputStream = new FileOutputStream(new File("route" + prob));
			for(String srcDst : staticRouteMapWithProb.get(prob).keySet()) {
				outputStream.write((srcDst + " " + staticRouteMapWithProb.get(prob).get(srcDst).size() + "\n").getBytes());
				for(TimeZoneRoute route : staticRouteMapWithProb.get(prob).get(srcDst)) {
					outputStream.write((simpleDateFormat.format(route.beginTime) +
							" " + simpleDateFormat.format(route.endTime)).getBytes());
					for(String string : route.routeList){
						outputStream.write((" " + string).getBytes());
					}
					outputStream.write('\n');
				}
			}
			outputStream.close();
		}

		for(Integer prob : reachHourMapWithProb.keySet()) {
			FileOutputStream outputStream = new FileOutputStream(new File("hour" + prob));
			for(String srcDst : reachHourMapWithProb.get(prob).keySet()) {
				outputStream.write((srcDst + " " + reachHourMapWithProb.get(prob).get(srcDst) + "\n").getBytes());
			}
			outputStream.close();
		}
	}
	
	ArrayList<String> getRoute(String srcDst, Date startTime) {
		ArrayList<String> routeList = new ArrayList<>();

		if(!staticRouteMap.containsKey(srcDst)) {
			return routeList;
		}
		for(TimeZoneRoute timeZoneRoute : staticRouteMap.get(srcDst))
		{
			if(timeZoneRoute.beginTime.getTime() <= startTime.getTime() &&
					timeZoneRoute.endTime.getTime() >= startTime.getTime())
			{
				routeList.addAll(timeZoneRoute.routeList);
			}
		}
		//没有符合条件的话 选择下一个区间
		if(routeList.size() == 0)
		{
			for(TimeZoneRoute timeZoneRoute : staticRouteMap.get(srcDst))
			{
				if(timeZoneRoute.beginTime.getTime() >= startTime.getTime())
				{
					routeList.addAll(timeZoneRoute.routeList);
					break;
				}
			}

			//在没有的话 选择前一个区间
			if(routeList.size() == 0)
			{
				routeList.addAll(staticRouteMap.get(srcDst).
						get(staticRouteMap.get(srcDst).size() - 1).routeList);
			}
		}

		return routeList;
	}

	/**
	 * 按广度优先搜索的方式查找，直到找到一个合法的路径
     * 只要找到一个合法的就返回
	 * @param src
	 * @param dst
	 * @param queryTime
	 * @param limitedHop
	 * @param limitedHour
	 * @param limitedProb
     * @return
     * @throws Exception
     */
	public RouteInfo broadSearch(String src, String dst, Date queryTime, Date endTime, int limitedHop,
								 double limitedHour, double limitedProb) throws Exception {
		//初始化第一层
	    ArrayList<ArrayList<String>> prevRouteList = new ArrayList<ArrayList<String>>();	//保存不同跳数的路径
		ArrayList<String> list = new ArrayList<String>();
		list.add(src);
		prevRouteList.add(list);

		int hop = 0;
		while(hop++ < limitedHop) {
//		    System.out.println("\t第" + hop + "层查找 " + prevRouteList.size() +" 储藏柜 " + storageStation.size());
		    //遍历当前层 看是否有符合的记录
			for(ArrayList<String> prevList : prevRouteList) {
				String mid = prevList.get(prevList.size() - 1);
				//每层都首先尝试直接到目的地的情况
				if(staticInfoMap.containsKey(mid) && staticInfoMap.get(mid).containsKey(dst)) {
					prevList.add(dst);
					RouteInfo routeInfo = getInfoOfRoute(prevList, limitedHour * 60, limitedProb, queryTime, endTime);
					//找到路径的话 跳出搜索
					prevList.remove(prevList.size() - 1);

					if(routeInfo != null) {
						return routeInfo;
					}
				}
			}

			ArrayList<ArrayList<String>> nextRouteList = new ArrayList<ArrayList<String>>();	//保存不同跳数的路径
			//执行到这里说明当前跳数没有符合要求的 生成当前跳数的路径
			for(ArrayList<String> prevList : prevRouteList) {
			    if(prevList.size() >= 2) {
					RouteInfo routeInfo = getInfoOfRoute(prevList, limitedHour * 60, limitedProb, queryTime, endTime);
					if(routeInfo == null) {
						continue;
					}
				}
				for(String mid : storageStation) {
					if(mid.equals(dst) || prevList.contains(mid)) {
						continue;
					}

					ArrayList<String> nextList = new ArrayList<>(prevList);
					nextList.add(mid);
					nextRouteList.add(nextList);
				}
            }
            prevRouteList = nextRouteList;
		}
		return null;
	}

	/**
	 * 计算两个直达站点的期望和方差
	 * @param src
	 * @param dst
	 * @param begin
	 * @param end
     * @return
     */
	private RouteInfo getDirectorRouteInfo(String src, String dst, Date begin, Date end) {

		if(!staticInfoMap.containsKey(src) || !staticInfoMap.get(src).containsKey(dst)) {
			return null;
		}
		//方差公式：D(X) = E(X^2)- E(X)^2
		double runX1 = 0;
		double runX2 = 0;
		double waitX1 = 0;
		double waitX2 = 0;
        int waitCnt = 0;
		int runCnt = 0;

		int beginIndex = (int) (begin.getTime() / 1000);
		beginIndex -= beginIndex % SMALL_TIME_INTERVAL;
		int endIndex = (int)(end.getTime() / 1000);
		endIndex -= endIndex % SMALL_TIME_INTERVAL;
        while(beginIndex < endIndex) {
        	if(staticInfoMap.get(src).get(dst).containsKey(beginIndex)) {
        	    RouteInfo info = staticInfoMap.get(src).get(dst).get(beginIndex);
                runX1 += info.getTotalRunExcept();
				runX2 += info.getTotalRunExceptX2();
				waitX1 += info.getTotalWaitExcept();
				waitX2 += info.getTotalWaitExceptX2();
				waitCnt += info.getWaitCount();
				runCnt += info.getRunCount();
			}
        	beginIndex += SMALL_TIME_INTERVAL;
		}

		//必须满足：结束时间减去起始时间 不小于平均等待时间
		if(waitCnt == 0 || waitX2 / waitCnt - waitX1 / waitCnt * waitX1 / waitCnt < 0.1 ||
				runX2 / runCnt - runX1 / runCnt * runX1 / runCnt < 0.1) {
			return null;
		}
		//区间内的记录要够多
//        int zoneCnt = (endIndex - beginIndex) / SMALL_TIME_INTERVAL;
//		if(runCnt <= zoneCnt * 3) {
//			return null;
//		}

        RouteInfo routeInfo = new RouteInfo();
        routeInfo.getStationList().add(src);
		routeInfo.getStationList().add(dst);
		routeInfo.getBeginTime().setTime(begin.getTime());
		routeInfo.getEndTime().setTime(end.getTime());
		routeInfo.setWaitCount(waitCnt);
		routeInfo.setRunCount(runCnt);
		routeInfo.setWaitExcept(waitX1 / waitCnt);
		routeInfo.setWaitExceptX2(waitX2 / waitCnt);
		routeInfo.setRunExcept(runX1 / runCnt);
		routeInfo.setRunExceptX2(runX2 / runCnt);
		return routeInfo;
	}

	/**
	 * 根据传入的路径计算出一个符合要求的RouteInfo，以二分查找的方式
	 * @param route
	 * @param maxMinute
	 * @param minProb
	 * @param queryTime
	 * @param limitedTime
	 * @return
     * @throws Exception
     */
	private RouteInfo getInfoOfRoute(ArrayList<String> route, double maxMinute, double minProb, Date queryTime,
									Date limitedTime)
		throws  Exception {
		//首先看一天内是否满足条件，不符合的话将时间区间缩小一半再次检查
		if(route.size() < 2) {
			return null;
		}
        RouteInfo routeInfoInDay = null;

		//传入的limitedTime是表示区间的最大范围，endTime表示用来计算期望和方差的区间最大范围 从limitedTime开始每次减半
		Date beginTime = new Date(queryTime.getTime());
		Date endTime = new Date(limitedTime.getTime());
		//时间间隔至少是5分钟
		while(routeInfoInDay == null && beginTime.getTime() + SMALL_TIME_INTERVAL * 1000 <= endTime.getTime()) {

			double totalWaitExpect = 0d;
			double totalRunExcept = 0d;
			double totalWaitVAR = 0d;
			double totalRunVAR = 0d;
			//0表示能找到路径；1表示当前的时间区间太长，但是可以缩小区间继续查找；2表示没有必要继续找下去了，直接返回
			int flag = 0;
			int endIndex = (int) (endTime.getTime() / 1000);
			endIndex -= endIndex % SMALL_TIME_INTERVAL;
			//求每一段的时间 将均值和方差相加
			for(int index = 0; index < route.size() - 1; index++) {
				String src = route.get(index);
				String dst = route.get(index + 1);
				if(!staticInfoMap.containsKey(src) || !staticInfoMap.get(src).containsKey(dst)) {
					return null;
				}
				int lastKey = staticInfoMap.get(src).get(dst).lastKey();
				//如果中间某一段的时间不够长，跳出
				if(lastKey < endIndex){
					flag = 1;
					break;
				}

				//计算每一跳的均值和方差
				RouteInfo tmp = getDirectorRouteInfo(src, dst, beginTime, endTime);

				//tmp为null是因为：1. 没有从src到dst的记录； 2. src到dsr在这段时间里面记录数为0
				if(tmp == null) {
					flag = 2;
					break;
				}
				if(tmp.getWaitVariance() <= 0 || tmp.getRunVariance() <= 0) {
					System.out.println("小于0");
					continue;
				}
				beginTime.setTime(beginTime.getTime() +
						(long) (tmp.getRunExcept() * 60000 + tmp.getWaitExcept() * 60000));
				//累加均值和方差
				totalWaitExpect += tmp.getWaitExcept();
				totalRunExcept += tmp.getRunExcept();
				totalWaitVAR += tmp.getWaitVariance();
				totalRunVAR += tmp.getRunVariance();

				//当前的延迟过长
				if (totalWaitExpect + totalRunExcept - maxMinute > 0.0001) {
					flag = 1;
					break;
				}
			}

			//检查概率是否满足要求
			//切尔雪夫不等式
			//P([limitedHour - AVG] < x) >= 1 - VAR / x^2
			if(flag == 0) {
				//首先要确保这条路径的时间不会太长
				double diff = maxMinute - (totalWaitExpect + totalRunExcept);
				if(diff > 0) {
					double p = 1 - (totalWaitVAR + totalRunVAR) / Math.pow(diff, 2);
					if(p - minProb > 0.0001) {
						routeInfoInDay = new RouteInfo();
						routeInfoInDay.getBeginTime().setTime(queryTime.getTime());
						routeInfoInDay.getEndTime().setTime(endTime.getTime());
						routeInfoInDay.getStationList().addAll(route);
						routeInfoInDay.setWaitExcept(totalWaitExpect);
						routeInfoInDay.setRunExcept(totalRunExcept);
						return routeInfoInDay;
					}
				}
			}
			else if(flag == 2) {
				break;
			}
			beginTime.setTime(queryTime.getTime());
			Date midTime = new Date((beginTime.getTime() + endTime.getTime()) / 2);
			endTime.setTime(midTime.getTime());
		}
		return routeInfoInDay;
	}


	/**
	 * 判断给定的查询是否能够满足，是的话返回对应的路径
	 * @param src
	 * @param dst
	 * @param queryTime
	 * @param maxHop
	 * @param prob
	 * @return
     * @throws Exception
     */
	public RouteInfo getTemporalRouteWithProb (String src, String dst, Date queryTime, Date endTime, int maxHop,
											  double maxHour, double prob ) throws Exception {
	    if(!staticInfoMap.containsKey(src)) {
	    	return null;
		}
		RouteInfo info = broadSearch(src, dst, queryTime, endTime, maxHop, maxHour, prob);
		return info;
	}

	/**
	 * 生成一天不同区间的路径 然后进行合并
	 * @param queryTime 一天开始时间
	 * @param endTime	一天结束时间
	 * @param maxHop	最大跳数
	 * @param maxMinute 预计多少时间可达
	 * @param minProb	预计成功率
	 * @return
	 * @throws Exception
	 */
	public RouteInfo getLongestRouteWithProb(Date queryTime, Date endTime, int maxHop,
											 double maxMinute, double minProb) throws Exception {
		int totalOD = csvReader.mixStationMap.size() * (csvReader.mixStationMap.size() - 1);
        int totalZoneSize = (int)((endTime.getTime() - queryTime.getTime()) / 1000 / SMALL_TIME_INTERVAL);
		int beginIndex = (int) (queryTime.getTime() / 1000);
		beginIndex = beginIndex - beginIndex % SMALL_TIME_INTERVAL;
		int endIndex = (int) (endTime.getTime() / 1000);
		endIndex = endIndex - endIndex % SMALL_TIME_INTERVAL;
        int maxMilSecond = (int)(maxMinute * 60000);
		double coverRatio = 0d;

		HashMap<String, HashMap<String, ArrayList<RouteInfo>>> baseRouteMap = new HashMap<>();
        //首先计算直达的情况
		ArrayList<String> srcToRemove = new ArrayList<>();
        for(String src : staticInfoMap.keySet()) {
        	baseRouteMap.put(src, new HashMap<String, ArrayList<RouteInfo>>());
			ArrayList<String> dstToRemove = new ArrayList<>();
        	for(String dst : staticInfoMap.get(src).keySet()) {
        		baseRouteMap.get(src).put(dst, new ArrayList<RouteInfo>());
        	    for(Integer index : staticInfoMap.get(src).get(dst).keySet()) {
        	        if (index < beginIndex || index >= endIndex) {
        	        	continue;
					}
					//筛选出所有满足预计投递成功率的直达OD
        	    	RouteInfo info = staticInfoMap.get(src).get(dst).get(index);
					if (info.getProbability(maxMinute) - minProb >= 0.0001) {
						baseRouteMap.get(src).get(dst).add(info);
					}
				}
				if (baseRouteMap.get(src).get(dst).size() == 0) {
					dstToRemove.add(dst);
				}
			}
			for (String dst : dstToRemove) {
				baseRouteMap.get(src).remove(dst);
			}
			if (baseRouteMap.get(src).size() == 0) {
				srcToRemove.add(src);
			}
		}
		for(String src : srcToRemove) {
			baseRouteMap.remove(src);
		}

		//存储每一层的路径
		for(int hop = 1; hop < maxHop; hop++)
		{
			HashMap<String, HashMap<String, ArrayList<RouteInfo>>> curZoneMap =
					new HashMap<String, HashMap<String,ArrayList<RouteInfo>>>();
            //起点必须是中转站点
			for(String src : storageStation)
			{
				//src到中间站点必须在前一层
				if (!baseRouteMap.containsKey(src)) {
					continue;
				}
				curZoneMap.put(src, new HashMap<String, ArrayList<RouteInfo>>());
				for(String dst : csvReader.mixStationMap.keySet())
				{
					//跳过相同的站点
					if(src.equals(dst))
					{
						continue;
					}

					//对每一个OD按30分钟一个区间生成路径，如果一个区间在前一层已经有满足的路径了，跳过该区间
					ArrayList<Date> timeList = new ArrayList<>();
                    if(baseRouteMap.get(src).containsKey(dst)) {
                    	ArrayList<RouteInfo> prevRoute = baseRouteMap.get(src).get(dst);
						//说明全部区间都可达
						if (prevRoute.size() >= totalZoneSize) {
						    curZoneMap.get(src).put(dst, prevRoute);
							continue;
						}
                        //首先添加前面的时间
						{
							Date begin = new Date(queryTime.getTime());
							while (begin.getTime() < prevRoute.get(0).getBeginTime().getTime()) {
								timeList.add(new Date(begin.getTime()));
								begin.setTime(begin.getTime() + SMALL_TIME_INTERVAL * 1000);
							}
						}
						//添加中间的时间
                    	for (int i = 0; i < prevRoute.size() - 1; i++) {
                    		Date begin = new Date(prevRoute.get(i).getEndTime().getTime());
							Date end = new Date(prevRoute.get(i + 1).getBeginTime().getTime());
							while (begin.getTime() < end.getTime()) {
								timeList.add(new Date(begin.getTime()));
                                begin.setTime(begin.getTime() + SMALL_TIME_INTERVAL * 1000);
							}
						}
						//后面的时间
						{
							Date begin = new Date(prevRoute.get(prevRoute.size() - 1).getEndTime().getTime());
							while (begin.getTime() < endTime.getTime()) {
								timeList.add(new Date(begin.getTime()));
								begin.setTime(begin.getTime() + SMALL_TIME_INTERVAL * 1000);
							}
						}
					}
					else {
					    Date begin = new Date(queryTime.getTime());
						while (begin.getTime() < endTime.getTime()) {
							timeList.add(new Date(begin.getTime()));
							begin.setTime(begin.getTime() + SMALL_TIME_INTERVAL * 1000);
						}
					}

					//这个list存储通过不同中间站点到达目的地的时间区间 最后要进行合并
					ArrayList<ArrayList<RouteInfo>> totalRouteList =
							new ArrayList<ArrayList<RouteInfo>>();
					//每个中间节点和上一层的区间进行比较
					for(String mid : storageStation)
					{
						//中间节点不能是起点或是目的地
						if(mid.equals(src) || mid.equals(dst))
						{
							continue;
						}
						//src到中间站点必须在前一层
						if(!baseRouteMap.get(src).containsKey(mid))
						{
							continue;
						}
						//中间节点到目的地必须直接到
						if (!staticInfoMap.get(mid).containsKey(dst)) {
							continue;
						}

						ArrayList<RouteInfo> tmpRoutes = new ArrayList<>();

						//记录下标
						int arrIndex = 0;
                        for(Date begin : timeList) {
                            int timeIndex = (int)(begin.getTime() / 1000);
							int index = (int) (baseRouteMap.get(src).get(mid).get(arrIndex).getBeginTime().getTime() / 1000);
                            //找到第一个不小于当前begin
                            while (index < timeIndex) {
                            	arrIndex++;
                                if (arrIndex >= baseRouteMap.get(src).get(mid).size()) {
                                	break;
								}
								index = (int) (baseRouteMap.get(src).get(mid).get(arrIndex).getBeginTime().getTime() / 1000);
							}
							if (arrIndex >= baseRouteMap.get(src).get(mid).size()) {
								break;
							}
							//没有相同的区间
							if (index > timeIndex) {
								continue;
							}
							ArrayList<String> stationList =
									new ArrayList<>(baseRouteMap.get(src).get(mid).get(arrIndex).getStationList());
                            if (stationList.size() < hop + 1) {
                            	continue;
							}
							stationList.add(dst);
							//最长的区间设置为当前时间加上最大时间
							Date end = new Date(begin.getTime() + maxMilSecond);
							RouteInfo info = getInfoOfRoute(stationList, maxMinute, minProb, begin, end);
                            if (info != null) {
                                info.getEndTime().setTime(begin.getTime() + SMALL_TIME_INTERVAL * 1000);
                            	tmpRoutes.add(info);
							}
						}
						if(tmpRoutes.size() > 0)
						{
							totalRouteList.add(tmpRoutes);
						}
					}
					if(totalRouteList.size() > 0)
					{
						//合并线路
						ArrayList<RouteInfo> result = null;
						for(ArrayList<RouteInfo> list : totalRouteList)
						{
							result = combineRouteInfoList(result, list, maxMinute, minProb);
						}
						if(baseRouteMap.get(src).containsKey(dst))
						{
							result = combineRouteInfoList(result, baseRouteMap.get(src).get(dst), maxMinute, minProb);
						}

						if (result != null && result.size() > 0) {
							curZoneMap.get(src).put(dst, result);
						}
					}
					else {
						if(baseRouteMap.get(src).containsKey(dst))
						{
						    curZoneMap.get(src).put(dst, baseRouteMap.get(src).get(dst));
                        }

					}
				}
				if(curZoneMap.get(src).size() == 0)
				{
					curZoneMap.remove(src);
				}
			}
			baseRouteMap = curZoneMap;
//			calculateCoverRatioOfRouteInfo(hop + 1, totalOD, baseRouteMap);
            //已经覆盖了超过99%的OD
			if(coverRatio >= 0.99d) {
				break;
			}
		}
		//输出路由结果
//		System.out.println("路由信息如下:");
		for(String src : baseRouteMap.keySet()) {
			for(String dst : baseRouteMap.get(src).keySet()) {
				adjustRouteInfoZone(baseRouteMap.get(src).get(dst), maxMinute, minProb);
				System.out.println(src + " " + dst + " " + baseRouteMap.get(src).get(dst).size());
				for(RouteInfo route : baseRouteMap.get(src).get(dst)) {
					System.out.print(simpleDateFormat.format(route.getBeginTime()) + " " +
							simpleDateFormat.format(route.getEndTime()) + " " + route.getExpect() + " " +
							route.getStationList().size() + " ");
					for(String string : route.getStationList())
					{
						System.out.print(string + " ");
					}
					System.out.println();
				}
			}
		}
		return null;
	}

	/**
	 * 从文件中读取刷卡记录
     */
	public void readRecordFromFile() {
		try {

			File dir = new File("/home/lu/workplace/data/subwayData/rawData/all/" + participateRatio);
//			System.out.println("目录：" + dir.getAbsolutePath());
			for(File file : dir.listFiles()) {
				if(file.getName().startsWith(".")) {
					continue;
				}
			    int day = Integer.parseInt(file.getName().substring(6));
				String line;
				String preSrcDst = null;
				Date prevTime = null;
				String[] result;
				Scanner scanner = new Scanner(file);
				while (scanner.hasNextLine()) {
					line = scanner.nextLine();
					result = line.split(" ");
					if (result.length != 4) {
						System.out.println("不合法的数据:" + line);
						continue;
					}
					String srcDst = csvReader.stationMap.get(result[0]).getMixStation().getChineseName() + " "
							+ csvReader.stationMap.get(result[1]).getMixStation().getChineseName();
					Date arrive = format.parse("2015-04-01 " + result[2]);
					Date leave = format.parse("2015-04-01 " + result[3]);
					SubwayArrivalTime subwayArrivalTime = new SubwayArrivalTime();
					subwayArrivalTime.setDay(day);
					subwayArrivalTime.setArriveTime(arrive);
					subwayArrivalTime.setLeaveTime(leave);
					if(!recordItemMap.containsKey(srcDst)) {
						recordItemMap.put(srcDst, new ArrayList<SubwayArrivalTime>());
					}

					recordItemMap.get(srcDst).add(subwayArrivalTime);
				}
				scanner.close();
				System.gc();
			}

			//OD--><时间区间-->路由信息>
			for(String od : recordItemMap.keySet()) {

			    String[] srcDst = od.split(" ");
				String src = srcDst[0];
				String dst = srcDst[1];
				int prevDay = 0;
				Date prevArrive = null;
				TreeMap<Integer, ArrayList<Integer>> waitList = new TreeMap<>();
				TreeMap<Integer, Integer> waitCnt = new TreeMap<>();
				TreeMap<Integer, ArrayList<Integer>> runList = new TreeMap<>();

				for(SubwayArrivalTime arrivalTime : recordItemMap.get(od)) {

					int index = (int) (arrivalTime.getArriveTime().getTime() / 1000);
					index = index - index % SMALL_TIME_INTERVAL;
					int runDelay = (int)((arrivalTime.getLeaveTime().getTime() - arrivalTime.getArriveTime().getTime())
							/ 60000);

					if(!runList.containsKey(index)) {
						runList.put(index, new ArrayList<Integer>());
						waitList.put(index, new ArrayList<Integer>());
						waitCnt.put(index, 0);
					}

					runList.get(index).add(runDelay);
					waitCnt.put(index, waitCnt.get(index) + 1);

					if(prevDay == arrivalTime.getDay()) {
						int waitDelay = (int)((arrivalTime.getArriveTime().getTime() - prevArrive.getTime())
								/ 60000);
						if (waitDelay < 0) {
							waitDelay = 0;
						}
						waitList.get(index).add(waitDelay);
					}
					prevDay = arrivalTime.getDay();
					prevArrive = arrivalTime.getArriveTime();
				}

				for(Integer timeIndex : runList.keySet()) {
					//方差公式：D(X) = E(X^2)- E(X)^2
					double runX1 = 0;
					double runX2 = 0;
					double waitX1 = 0;
					double waitX2 = 0;
					for(Integer wait : waitList.get(timeIndex)) {
						waitX1 += wait;
						waitX2 += wait * wait;
					}
					for(Integer run : runList.get(timeIndex)) {
						runX1 += run;
						runX2 += run * run;
					}

					//只有每天的第一个时间区间才可能为0
					if(waitList.get(timeIndex).size() == 0) {
						continue;
					}
					double routeRunAVG = runX1 / runList.get(timeIndex).size();
					double routeWaitAVG = waitX1 / waitList.get(timeIndex).size();
					routeWaitAVG = SMALL_TIME_INTERVAL / 60 * 21 / (double)waitCnt.get(timeIndex);
					double routeRunX2AVG = runX2 / runList.get(timeIndex).size();
					double routeWaitX2AVG = waitX2 / waitList.get(timeIndex).size();
                    routeWaitX2AVG = routeWaitAVG * routeWaitAVG * 2;

					double routeRunVAR = routeRunX2AVG - routeRunAVG * routeRunAVG;
					double routeWaitVAR = routeWaitX2AVG - routeWaitAVG * routeWaitAVG;

					RouteInfo routeInfo = new RouteInfo();
					ArrayList<String> routes = new ArrayList<>();
					routes.add(src);
					routes.add(dst);
					routeInfo.setStationList(routes);
					routeInfo.setBeginTime(new Date((long)timeIndex * 1000));
					routeInfo.setEndTime(new Date((long)timeIndex * 1000 + SMALL_TIME_INTERVAL * 1000));
					routeInfo.setWaitExcept(routeWaitAVG);
					routeInfo.setWaitExceptX2(routeWaitX2AVG);
					routeInfo.setRunExcept(routeRunAVG);
					routeInfo.setRunExceptX2(routeRunX2AVG);
					routeInfo.setWaitCount(waitList.get(timeIndex).size());
					routeInfo.setWaitCount(waitCnt.get(timeIndex) / 21);
					routeInfo.setRunCount(runList.get(timeIndex).size());
                    System.out.println(routeInfo);
//                    System.out.println(src + " " + dst + " " + simpleDateFormat.format(routeInfo.getBeginTime()) + " "
//						+ simpleDateFormat.format(routeInfo.getEndTime()) + " " + routeInfo.getRunExcept());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 从文件中读取最原始的期望和方差信息
	 * @throws Exception
     */
	public void readRouteInfoFromFile(String ratio) throws Exception {
		Scanner scanner = new Scanner(new File("/home/lu/workplace/experiment/routeInfo/" +
				(SMALL_TIME_INTERVAL / 60) + "/" + ratio));
		String line = null;
		while(scanner.hasNextLine()) {
			line = scanner.nextLine();
			String[] result = line.split(" ");
			String od = result[0] + " " + result[1];
			RouteInfo routeInfo = new RouteInfo();
			routeInfo.initWithLine(line);
			if(!staticInfoMap.containsKey(result[0])) {
			    staticInfoMap.put(result[0], new HashMap<String, TreeMap<Integer, RouteInfo>>());
			}
			if(!staticInfoMap.get(result[0]).containsKey(result[1])) {
				staticInfoMap.get(result[0]).put(result[1], new TreeMap<Integer, RouteInfo>());
			}
			int timeIndex = (int) (routeInfo.getBeginTime().getTime() / 1000);
			timeIndex = timeIndex - timeIndex % SMALL_TIME_INTERVAL;
			staticInfoMap.get(result[0]).get(result[1]).put(timeIndex, routeInfo);
		}
		scanner.close();

        int cnt = 0;
		ArrayList<String> srcToRemove = new ArrayList<>();
		for(String src : staticInfoMap.keySet()) {
			ArrayList<String> dstToRemove = new ArrayList<>();
			for (String dst : staticInfoMap.get(src).keySet()) {
				ArrayList<Integer> indexToRemove = new ArrayList<>();
				for(Integer timeIndex : staticInfoMap.get(src).get(dst).keySet()) {
//					if (staticInfoMap.get(src).get(dst).get(timeIndex).getWaitExcept() > 1.2 * SMALL_TIME_INTERVAL / 60) {
//					if (staticInfoMap.get(src).get(dst).get(timeIndex).getWaitExcept() >= SMALL_TIME_INTERVAL / 60 + 2) {
//						indexToRemove.add(timeIndex);
//						cnt++;
//					}
				}
				for (Integer index : indexToRemove) {
					staticInfoMap.get(src).get(dst).remove(index);
				}
				if (staticInfoMap.get(src).get(dst).size() == 0) {
					dstToRemove.add(dst);
				}
			}
			for (String dst : dstToRemove) {
				staticInfoMap.get(src).remove(dst);
			}
			if (staticInfoMap.get(src).size() == 0) {
				srcToRemove.add(src);
			}
		}
		for (String src: srcToRemove) {
			staticInfoMap.remove(src);
		}
//		System.out.println("删除了" + cnt + "条记录");
	}
}
