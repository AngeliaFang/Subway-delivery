package subway.newclass;

import java.util.TreeMap;
import java.util.TreeSet;

public class SubwayItemSet {

	public final static int TIME_ZONE = 300;
	

	private TreeMap<Integer, TreeSet<SubwayItem>> arriveSet 
		= new TreeMap<Integer, TreeSet<SubwayItem>>();

	public TreeMap<Integer, TreeSet<String>> dstSet = new TreeMap<Integer, TreeSet<String>>();
	
	public TreeMap<Integer, TreeSet<SubwayItem>> getArriveSet() {
		return arriveSet;
	}

	public void setArriveSet(TreeMap<Integer, TreeSet<SubwayItem>> arriveSet) {
		this.arriveSet = arriveSet;
	}

	public void add(SubwayItem item) {
		int intTime = (int) (item.getArrive().getTime() / 1000);
		int time = intTime - intTime % TIME_ZONE;
		if(!arriveSet.containsKey(time)) {
			arriveSet.put(time, new TreeSet<SubwayItem>());
			dstSet.put(time, new TreeSet<String>());
		}
		arriveSet.get(time).add(item);
		dstSet.get(time).add(item.getDst());
		
	}

}
