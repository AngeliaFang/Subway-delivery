package subway.newclass;

import java.util.ArrayList;
import java.util.TreeSet;

public class MixStation implements Comparable<MixStation>{

	private ArrayList<AbstractStationItem> stationList;
	private TreeSet<MixStation> neighbor;
	private boolean access;
	private boolean transferForExpress;						//是否用作快递的中转车站
	private double latitude = Double.MIN_VALUE;
	private double longitude = Double.MIN_VALUE;
	private String position;
	private String correspondingStation;					//当前车站的包裹分配给哪个中转车站
	private ArrayList<String> proxyClientStationList;		//当前车站代理了哪些车站的包裹
	/**
	 * stationList保存当前车站是由哪些车站合并的
	 * regionList保存当前车站处于那些区域
	 * neighbor保存邻居车站
	 */
	public MixStation() {
		this.stationList = new ArrayList<AbstractStationItem>();
		this.neighbor = new TreeSet<MixStation>();
		this.access = false;
		this.transferForExpress = false;
		this.proxyClientStationList = new ArrayList<>();
	}

	public String getChineseName() {
		if(stationList.size() > 0) {
			return stationList.get(0).getChineseName();
		}
		
		return null;
	}

	public double radias(double value)
	{
		return value * Math.PI / 180;
	}

	public double getDistance(MixStation dst)
	{
        double lat1 = radias(this.latitude);
		double lat2 = radias(dst.latitude);
		double a = lat1 - lat2;
		double b = radias(this.longitude) - radias(dst.longitude);
		double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(b / 2), 2)));
		s = s * 6378.137;
		return 1000 * s;
	}
	public TreeSet<MixStation> getNeighbor() {
		return neighbor;
	}
	
	public void setNeighbor(TreeSet<MixStation> set) {
		this.neighbor = set;
	}
	
	public ArrayList<AbstractStationItem> getStationList() {
		return stationList;
	}

	public void setStationList(ArrayList<AbstractStationItem> stationList) {
		this.stationList = stationList;
	}

	@Override
	public int compareTo(MixStation mixStation) {
		if(mixStation.getChineseName().equals("浦电路")
				&& this.getChineseName().equals("浦电路")) {
			return this.getStationList().get(0).getLine() 
					- mixStation.getStationList().get(0).getLine();
		}
		return this.getChineseName().compareTo(mixStation.getChineseName());
	}

	public boolean equals(Object mixStation) {
		int result = this.getChineseName().compareTo(((MixStation)mixStation).getChineseName());
		if(result == 0) {
			return true;
		}
		return false;
	}

	public boolean isAccess() {
		return access;
	}

	public void setAccess(boolean access) {
		this.access = access;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public boolean isTransferForExpress() {
		return transferForExpress;
	}

	public void setTransferForExpress(boolean transferForExpress) {
		this.transferForExpress = transferForExpress;
	}

	public String getCorrespondingStation() {
		return correspondingStation;
	}

	public void setCorrespondingStation(String correspondingStation) {
		this.correspondingStation = correspondingStation;
	}

	public ArrayList<String> getProxyClientStationList() {
		return proxyClientStationList;
	}

	public void setProxyClientStationList(ArrayList<String> proxyClientStationList) {
		this.proxyClientStationList = proxyClientStationList;
	}
}
