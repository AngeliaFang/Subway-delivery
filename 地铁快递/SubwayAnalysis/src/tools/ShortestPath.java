/**
 * 
 */
package tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import subway.newclass.AbstractStationItem;
import subway.newclass.CombinationStation;
import subway.newclass.Line;
import subway.newclass.Route;
import subway.newclass.SingleStationItem;

/**
 * @author 60347
 * 地铁的最短路径算法
 */
public class ShortestPath {

	//分别是实际的线路和抽象的线路图
	private HashMap<Integer, Line> singleLineMap = new HashMap<Integer, Line>();
		
	//把10号线和11号线多出的几站放到下面的链表
	private ArrayList<AbstractStationItem> singleExtra10 = new ArrayList<AbstractStationItem>();
	private ArrayList<AbstractStationItem> singleExtra11 = new ArrayList<AbstractStationItem>();
	
	//这是10号线和11号线的另外一条线路
	private Line singleLine10;
	private Line singleLine11;
//	private Line singleTinyLine10;
//	private Line singleTinyLine11;
	
	/*线路历史hashmap*/
	private Random random = new Random();
	private HashMap<CombinationStation, ArrayList<Route>> history = new HashMap<CombinationStation,  ArrayList<Route>>();
//	private CSVReader csv;
	
	public static Comparator<AbstractStationItem> comparator = new Comparator<AbstractStationItem>() {
		
		@Override
		public int compare(AbstractStationItem station1, AbstractStationItem station2) {
			return station1.getId().compareTo(station2.getId());
		}
	};
	/**
	 * 初始化各条线路，邻居车站，抽象车站等信息
	 */
	public ShortestPath(CSVReader csv) {
//		this.csv = csv;
		int number = 1;
		ArrayList<AbstractStationItem> list = new ArrayList<AbstractStationItem>();
		for(Entry<String, AbstractStationItem> entry: csv.stationMap.entrySet()) {
			if(number == entry.getValue().getLine()) {
				list.add(entry.getValue());
			}
			else {
				
				if(number == 10) {
					ArrayList<AbstractStationItem> list1 = new ArrayList<AbstractStationItem>();
					ArrayList<AbstractStationItem> list2 = new ArrayList<AbstractStationItem>();
//					ArrayList<AbstractStationItem> tinyList = new ArrayList<AbstractStationItem>();
					for(int i = 0; i < list.size(); i++) {
						if(i < 3) {
							list1.add(list.get(i));
//							tinyList.add(list.get(i));
						}
						else if(i <= 6) {
							list2.add(list.get(i));
							singleExtra10.add(list.get(i));
						} else {
							list1.add(list.get(i));
							list2.add(list.get(i));
						}
					}
//					tinyList.add(list.get(7));
//					tinyList.add(list.get(6));
//					tinyList.add(list.get(5));
//					tinyList.add(list.get(4));
//					tinyList.add(list.get(3));

					singleLineMap.put(number, new Line(list1));
					singleLine10 = new Line(list2);
//					singleTinyLine10 = new Line(tinyList);
				} else if(number == 11) {
					ArrayList<AbstractStationItem> list1 = new ArrayList<AbstractStationItem>();
					ArrayList<AbstractStationItem> list2 = new ArrayList<AbstractStationItem>();
//					ArrayList<AbstractStationItem> tinyList = new ArrayList<AbstractStationItem>();
					for(int i = 0; i < list.size(); i++) {
						if(i < 7) {
							list1.add(list.get(i));
//							tinyList.add(list.get(i));
						} else if(i <= 9) {
							list2.add(list.get(i));
							singleExtra11.add(list.get(i));
						} else {
							list1.add(list.get(i));
							list2.add(list.get(i));
						}
					}
//					tinyList.add(list.get(10));
//					tinyList.add(list.get(9));
//					tinyList.add(list.get(8));
//					tinyList.add(list.get(7));

					singleLineMap.put(number, new Line(list1));
					singleLine11 = new Line(list2);
//					singleTinyLine11 = new Line(tinyList);
				}
				else {

					singleLineMap.put(number, new Line(list));
//					System.out.println(number + ": "  + list.size());
				}
				number = entry.getValue().getLine();
				list.clear();
				list.add(entry.getValue());
			}
		}
		singleLineMap.put(number, new Line(list));
	}

	private ArrayList<AbstractStationItem> getRoutOfTheSameLine(CombinationStation com) {
		ArrayList<AbstractStationItem> listResult = new ArrayList<AbstractStationItem>();
		AbstractStationItem src = com.src;
		AbstractStationItem dst = com.dst;
		if(history.containsKey(com)) {
			int index = random.nextInt(history.get(com).size());
			return history.get(com).get(index).getRoutes();
		}
		if(src.getLine() != dst.getLine()) {
			return null;
		}
		if(src.getId().equals(dst.getId())) {
			listResult.add(src);
			return listResult;
		}
		
		ArrayList<AbstractStationItem> list = new ArrayList<AbstractStationItem>();
		
		//每次将id更小的作为初始点
		boolean isInvert = false;
		if(src.getId().compareTo(dst.getId()) > 0) {
			AbstractStationItem temp = new AbstractStationItem(src);
			src = dst;
			dst = temp;
			isInvert = true;
		}
		if(src.getLine() == 10) {
			if(singleLineMap.get(10).contains(src) && singleExtra10.contains(dst)) {	//起始地是第1条十号线,目的地是第二条十号线,这种情况只有起始地是第一条10号线的前三站
				list.add(singleLineMap.get(10).getStationList().get(0));
				list.add(singleLineMap.get(10).getStationList().get(1));
				list.add(singleLineMap.get(10).getStationList().get(2));
				list.add(singleLineMap.get(10).getStationList().get(3));
				list.add(singleLine10.getStationList().get(3));
				list.add(singleLine10.getStationList().get(2));
				list.add(singleLine10.getStationList().get(1));
				list.add(singleLine10.getStationList().get(0));
			} 
			else if (singleExtra10.contains(src) && singleLineMap.get(10).contains(dst)) {	//起始地是第2条十号线,目的地是第1条十号线,这种情况只有目的地是第一条10号线的三站之后
				list = singleLine10.getStationList();			
			}
			
			else if (singleLineMap.get(10).contains(src) && singleLineMap.get(10).contains(dst)) {	//起始地是第1条十号线,目的地是第1条十号线
				list = singleLineMap.get(10).getStationList();			
			}
			else if (singleExtra10.contains(src) && singleExtra10.contains(dst)) {	//起始地是第2条十号线,目的地是第2条十号线
				list = singleLine10.getStationList();			
			}
			
		} else if (src.getLine() == 11) {
			if(singleLineMap.get(11).contains(src) && singleExtra11.contains(dst)) {	//起始地是第1条11号线,目的地是第二条11号线,这种情况只有起始地是第一条11号线的前7站
				list.add(singleLineMap.get(11).getStationList().get(0));
				list.add(singleLineMap.get(11).getStationList().get(1));
				list.add(singleLineMap.get(11).getStationList().get(2));
				list.add(singleLineMap.get(11).getStationList().get(3));
				list.add(singleLineMap.get(11).getStationList().get(4));
				list.add(singleLineMap.get(11).getStationList().get(5));
				list.add(singleLineMap.get(11).getStationList().get(6));
				list.add(singleLineMap.get(11).getStationList().get(7));
				list.add(singleLine11.getStationList().get(2));
				list.add(singleLine11.getStationList().get(1));
				list.add(singleLine11.getStationList().get(0));
			} 
			else if (singleExtra11.contains(src) && singleLineMap.get(11).contains(dst)) {	//起始地是第2条11号线,目的地是第1条11号线,这种情况只有目的地是第一条11号线的7站之后
				list = singleLine11.getStationList();			
			}
			
			else if (singleLineMap.get(11).contains(src) && singleLineMap.get(11).contains(dst)) {	//起始地是第1条11号线,目的地是第1条11号线
				list = singleLineMap.get(11).getStationList();			
			}
			else if (singleExtra11.contains(src) && singleExtra11.contains(dst)) {	//起始地是第2条11号线,目的地是第2条11号线
				list = singleLine11.getStationList();			
			}
		} else {
			list = singleLineMap.get(src.getLine()).getStationList();
		}

		int srcIndex = -1;
		int dstIndex = -1;
		for(int i = 0; i < list.size(); i++) {
			if((list.get(i)).getChineseName().equals(src.getChineseName())) {
				srcIndex = i;
			}
			if((list.get(i)).getChineseName().equals(dst.getChineseName())) {
				dstIndex = i;
			}
		}
		
		if(srcIndex < 0 || dstIndex < 0) {
			listResult.clear();
			return listResult;
		}
		
		//4号线是环线
		if(src.getLine() == 4 && (dstIndex - srcIndex) > (list.size() + srcIndex - dstIndex)) {

			//从后往前
			listResult.clear();
			for(int i = srcIndex; i >= 0; i--) {
				listResult.add(list.get(i));
			}
			for(int i = list.size() - 1; i >= dstIndex; i--) {
				listResult.add(list.get(i));
			}
			
		}
		else {
			for(int i = srcIndex; i <= dstIndex; i++) {
				listResult.add(list.get(i));
			}
		}

		CombinationStation com1 = new CombinationStation(src, dst);
		if(!history.containsKey(com1)) {
			history.put(com1, new ArrayList<Route>());
		}
		history.get(com1).add(new Route(listResult, 0));
		
		ArrayList<AbstractStationItem> list1 = new ArrayList<AbstractStationItem>();
		for(int i = listResult.size() - 1; i >= 0; i--) {
			list1.add(listResult.get(i));
		}
		CombinationStation com2 = new CombinationStation(dst, src);
		if(!history.containsKey(com2)) {
			history.put(com2, new ArrayList<Route>());
		}
		history.get(com2).add(new Route(list1, 0));
		
		if(isInvert) {
			return list1;
		}
		return listResult;
	}
	
	
	/**
	 * 该函数替换10号线和11号线
	 */
	public Line getLineOfStation(AbstractStationItem src) {

		if(src.getLine() == 10) {
			if(singleExtra10.contains(src)) {
				return singleLine10;
			}
		}
		else if (src.getLine() == 11) {
			if(singleExtra11.contains(src)) {
				return singleLine11;
			}
		}
		return singleLineMap.get(src.getLine());
	}
	
	public ArrayList<Route> getAllRoutes(CombinationStation com) {
		
		if(!history.containsKey(com)) {
			getRouteByChangeTime(com);
		}
		
		return history.get(com);
	}
	
	public Route getRouteByChangeTime(CombinationStation com) {
		
		if(history.containsKey(com)) {
			int index = random.nextInt(history.get(com).size());
			return history.get(com).get(index);
		}

		if(com.src.getLine() == com.dst.getLine()) {
			return new Route(getRoutOfTheSameLine(com), 0);
		}

		return getRoutOfDifferentLine(com);
	}

	private Route getRoutOfDifferentLine(CombinationStation com) {
		ArrayList<AbstractStationItem> listResult = new ArrayList<AbstractStationItem>();
		AbstractStationItem src = com.src;
		AbstractStationItem dst = com.dst;
		if(src.getLine() == dst.getLine()) {
			return null;
		}
		if(src.getChineseName().equals(dst.getChineseName()) && !src.getChineseName().equals("浦电路")) {
			listResult.add(src);
			listResult.add(dst);
			return new Route(listResult, 1);
		}

		CombinationStation sourceCom = new CombinationStation(src, dst);
		getSubRoute(listResult, com, 0, sourceCom);
		
		if(!history.containsKey(sourceCom)) {
			System.out.println("没有找到 " + com.src.getChineseName() + " 至 " + com.dst.getChineseName());
			return null;
		}
		int index = random.nextInt(history.get(sourceCom).size());
		return history.get(sourceCom).get(index);
	}
	
	/**
	 * 改函数利用回溯法递归获取子路径
	 */
	private void getSubRoute(ArrayList<AbstractStationItem> prevStations, 
			CombinationStation com, int changeCount, CombinationStation sourceCom) {
		AbstractStationItem src = com.src;
		AbstractStationItem dst = com.dst;
		ArrayList<AbstractStationItem> currentList = new ArrayList<AbstractStationItem>(prevStations);
		//到达相同线路目的地
		if(src.getLine() == dst.getLine()) {
			
			ArrayList<AbstractStationItem> list = getRoutOfTheSameLine(com);
			if(list != null && list.size() > 0) {
				
				for(AbstractStationItem station : list) {
					currentList.add(station);
				}
				if(history.containsKey(sourceCom)) {
					if(history.get(sourceCom).get(0).getChangCount() > changeCount) {	//当前的记录较大，替换
								
						//虽然当前记录换乘次数多，但是如果最后一站是换乘车站
						boolean needChange = true;
						Route currentRoute = history.get(sourceCom).get(0);
						if(sourceCom.dst.isChangeable()) {
							AbstractStationItem last = currentRoute.getRoutes().get(currentRoute.getRoutes().size() - 1);
							AbstractStationItem second = currentRoute.getRoutes().get(currentRoute.getRoutes().size() - 2);
							if(last.getChineseName().equals(second.getChineseName())) {
								if(currentRoute.getRoutes().size() < currentList.size()) {
									needChange = false;
								}
							}
						}
						
						if(sourceCom.src.isChangeable()) {
							AbstractStationItem first = currentRoute.getRoutes().get(0);
							AbstractStationItem second = currentRoute.getRoutes().get(1);
							if(first.getChineseName().equals(second.getChineseName())) {
								if(currentRoute.getRoutes().size() < currentList.size()) {
									needChange = false;
								}
							}
						}
						
						if(needChange) {
							history.remove(sourceCom);
							history.put(sourceCom, new ArrayList<Route>());
							history.get(sourceCom).add(new Route(currentList, changeCount));
						}
					} else if(history.get(sourceCom).get(0).getChangCount() == changeCount) {	//或是换乘次数相同，但是历史记录的站数更多，替换
						
						if(history.get(sourceCom).get(0).getRoutes().size() > currentList.size()) {
							history.remove(sourceCom);
							history.put(sourceCom, new ArrayList<Route>());
							history.get(sourceCom).add(new Route(currentList, changeCount));
						}
						//如果相等的话
						else if(history.get(sourceCom).get(0).getRoutes().size() == currentList.size()) {
							history.get(sourceCom).add(new Route(currentList, changeCount));
						}
					}
				}
				else {
					history.put(sourceCom, new ArrayList<Route>());
					history.get(sourceCom).add(new Route(currentList, changeCount));
				}
			}
			return;
		}
		
		
		//得到下标，分别在线路上向前后搜索
		int index = getLineOfStation(src).getStationList().indexOf(src);
		getLineOfStation(src).setAccess(true);

		int size =  getLineOfStation(src).getStationList().size();
		ArrayList<Integer> foreList = new ArrayList<Integer>();		//向前搜索的车站列表,值是在stationList中的下标
		foreList.add(index);
		ArrayList<Integer> behindList = new ArrayList<Integer>();	//向后搜索的车站列表
		//4号线是环线，另外处理，向前和向后的分别占一半
		if(src.getLine() == 4) {
			//foreList.clear();
			for(int i = 1; i <=  size / 2; i++) {
				foreList.add((index + i + size) % size);
			}

			//向后的队列从这开始
			for(int i = 1; i < size /2; i++) {
				behindList.add((index - i + size) % size);
			}
		}
		else {	//分别获取向前和向后的车站
			for(int i = index + 1; i < size; i++) {
				foreList.add(i);
			}
			
			for(int i = index - 1; i >= 0; i--) {
				behindList.add(i);
			}
		}
		
		for(int i = 0; i < foreList.size(); i++) {
			AbstractStationItem station = getLineOfStation(src).getStationList().get(foreList.get(i));
			currentList.add(station);
			
			//如果index不是起始点，是一个后继的换乘车站的话，不应该遍历其换乘车站
			if(i == index && changeCount > 0) {
				continue;
			}
			if(station.isChangeable()) {				//在换乘区间上找
				station.setAccess(true);
				for(AbstractStationItem changeStation : ((SingleStationItem)station).getStationSet()) {
					if(!changeStation.isAccess() && !getLineOfStation(changeStation).isAccess()) {
						getLineOfStation(changeStation).setAccess(true);
						CombinationStation subCom = new CombinationStation(changeStation, dst);
						
						//剪枝
						if(history.containsKey(sourceCom)) {
							if(history.get(sourceCom).get(0).getChangCount() >= (changeCount)) {
								getSubRoute(currentList, subCom, changeCount + 1, sourceCom);
							}
						}
						else {
							getSubRoute(currentList, subCom, changeCount + 1, sourceCom);
						}
						
						getLineOfStation(changeStation).setAccess(false);
					}
				}
				station.setAccess(false);
				
			}
		}
		currentList.clear();
		currentList.addAll(prevStations);
		currentList.add(src);
		for(int i = 0; i < behindList.size(); i++) {
			AbstractStationItem station = getLineOfStation(src).getStationList().get(behindList.get(i));
			currentList.add(station);
			if(station.isChangeable()) {				//在换乘区间上找
				//对所有换乘车站进行遍历
				station.setAccess(true);
				for(AbstractStationItem changeStation : ((SingleStationItem)station).getStationSet()) {
					if(!changeStation.isAccess() && !getLineOfStation(changeStation).isAccess()) {
						getLineOfStation(changeStation).setAccess(true);
						CombinationStation subCom = new CombinationStation(changeStation, dst);
						//剪枝
						if(history.containsKey(sourceCom)) {
							if(history.get(sourceCom).get(0).getChangCount() >= (changeCount )) {
								getSubRoute(currentList, subCom, changeCount + 1, sourceCom);
							}
						}
						else {
							getSubRoute(currentList, subCom, changeCount + 1, sourceCom);
						}
						getLineOfStation(changeStation).setAccess(false);
					}
				}
				station.setAccess(false);
			}
		}
		getLineOfStation(src).setAccess(false);	
	}
}
