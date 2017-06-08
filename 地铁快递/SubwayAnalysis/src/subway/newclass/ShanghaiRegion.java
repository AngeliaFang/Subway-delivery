package subway.newclass;

import java.util.ArrayList;
import java.util.TreeSet;

import subway.newclass.AbstractStationItem;

public class ShanghaiRegion {

	public int regionID;
	public String name;
	public int area;
	public float population;
	public int postID;
	public int agentNumber;
	public int totalParcelToSend;
	public int totalParcelToRecv;
	public int totalTraffic;
	public ArrayList<String> stationList = new ArrayList<String>();
//	public ArrayList<TreeSet<Parcel>> parcelList = new ArrayList<TreeSet<Parcel>>();
	public TreeSet<AbstractStationItem> stationSet = new TreeSet<AbstractStationItem>();
	public int getRegionID() {
		return regionID;
	}
	public void setRegionID(int regionID) {
		this.regionID = regionID;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getArea() {
		return area;
	}
	public void setArea(int area) {
		this.area = area;
	}
	public float getPopulation() {
		return population;
	}
	public void setPopulation(float population) {
		this.population = population;
	}
	public int getPostID() {
		return postID;
	}
	public void setPostID(int postID) {
		this.postID = postID;
	}
	public TreeSet<AbstractStationItem> getStationSet() {
		return stationSet;
	}
	public void setStationSet(TreeSet<AbstractStationItem> stationset) {
		this.stationSet = stationset;
	}
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getRegionID() + ",");
		sb.append(getName() + ",");
		sb.append(getArea() + ",");
		sb.append(getPopulation() + ",");
		sb.append(getPostID() + ",");
		for(AbstractStationItem station : stationSet) {
			sb.append(station.getId() + ",");
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}
	public int getAgentNumber() {
		return agentNumber;
	}
	public void setAgentNumber(int agentNumber) {
		this.agentNumber = agentNumber;
	}
	
	
}
