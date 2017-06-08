/**
 * 
 */
package subway.newclass;

import java.util.ArrayList;

/**
 * @author 60347
 *
 */
public class Line {

	private int number;
	private boolean access = false;
	private ArrayList<AbstractStationItem> stationList = new ArrayList<AbstractStationItem>();
	
	public Line() {
	}
	
	public Line(ArrayList<AbstractStationItem> stationList) {

		number = stationList.get(0).getLine();
		//为每个车站增加邻居
		for(int i = 0; i < stationList.size(); i++) {
			this.stationList.add(stationList.get(i));
			
			if( i < stationList.size() - 1) {
				stationList.get(i).getNeighborSet().add(stationList.get(i + 1));
				stationList.get(i + 1).getNeighborSet().add(stationList.get(i));
			}
			stationList.get(i).getNeighborSet().add(stationList.get(i));
		}
	}
	
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	public ArrayList<AbstractStationItem> getStationList() {
		return stationList;
	}
	public void setStationList(ArrayList<AbstractStationItem> stationList) {
		this.stationList = stationList;
	}
	
	public String toString() {
		String content = "" + number + ":" + ":" + stationList.size() + ":";
		for(AbstractStationItem station : stationList) {
			content += station.getChineseName() + ":";
		}
		return content;
	}
	
	public boolean contains(AbstractStationItem station) {
		for(AbstractStationItem item : stationList) {
			if(item instanceof MultipleStationItem) {
				MultipleStationItem multipleStationItem = (MultipleStationItem)station;
				for(AbstractStationItem abstractStationItem : multipleStationItem.getStationList()) {
					if(abstractStationItem.getId().equals(station.getId())) {
						return true;
					}
				}
			}
			else if(item.getId().equals(station.getId())) {
				return true;
			}
		}
		return false;
	}

	public boolean isAccess() {
		return access;
	}

	public void setAccess(boolean access) {
		this.access = access;
	}
}
