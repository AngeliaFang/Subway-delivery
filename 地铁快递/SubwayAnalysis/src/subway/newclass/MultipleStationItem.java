/**
 * 
 */
package subway.newclass;

import java.util.ArrayList;

/**
 * @author 60347
 * 聚合的站点，有聚合的车站链表
 */
public class MultipleStationItem extends AbstractStationItem{

	private ArrayList<AbstractStationItem> stationList;
	private int startIndex;
	private int endIndex;
	
	public MultipleStationItem(ArrayList<AbstractStationItem> list) {
		
		if(list.size() < 2) {
			System.out.println("车站太少");
		}
		else {
			setLegal(true);
			setChangeable(false);
			setSingle(false);
			setId(list.get(0).getId() + "|" + list.get(list.size() - 1).getId());
			setLine(list.get(0).getLine());
			setChineseName(list.get(0).getChineseName() + "——" + list.get(list.size() - 1).getChineseName());
			setStationList(list);
			setStartIndex(0);
			setEndIndex(list.size() - 1);
		}
	}
	
	public ArrayList<AbstractStationItem> getStationList() {
		return stationList;
	}

	public void setStationList(ArrayList<AbstractStationItem> stationList) {
		if(stationList != null) {
			this.stationList = new ArrayList<AbstractStationItem>(stationList);
		}
		else {
			this.stationList = null;
		}
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public int getRealSize() {
		return Math.abs(endIndex - startIndex) + 1;
	}
	
	public ArrayList<AbstractStationItem> getRealStationInSequence() {
		ArrayList<AbstractStationItem> list = new ArrayList<AbstractStationItem>();
		
		if(endIndex < startIndex) {
			for(int i = startIndex; i >= endIndex; i--) {
				list.add(getStationList().get(i));
			}
		}
		else {
			for(int i = startIndex; i <= endIndex; i++) {
				list.add(getStationList().get(i));
			}
		}
		return list;
	}
}
