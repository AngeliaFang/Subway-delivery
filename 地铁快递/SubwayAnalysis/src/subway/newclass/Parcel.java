package subway.newclass;

import java.util.ArrayList;
import java.util.Date;

public class Parcel implements Comparable<Parcel>{

	private int id;
    private boolean delivered;
	private String dst;
	private Date currentTime;
	private Date pickInTime;
	private ArrayList<String> route;

	public int expectHour;
    public int transferCnt = 0;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getDst() {
		return dst;
	}
	public void setDst(String dst) {
		this.dst = dst;
	}
	public Date getCurrentTime() {
		return currentTime;
	}
    public void setCurrentTime(Date currentTime) {
		this.currentTime = new Date(currentTime.getTime());
	}
	public void setCurrentTime(long time) {
		this.currentTime = new Date(time);
	}
	@Override
	public int compareTo(Parcel o) {
		
		return this.id - o.id;
	}
	public ArrayList<String> getRoute() {
		return route;
	}
	public void setRoute(ArrayList<String> route) {
		if(route == null) {
			this.route = null;
		}
		else {
//			this.route = new ArrayList<String>(route);
			this.route = route;
		}
	}

	public Date getPickInTime() {
		return pickInTime;
	}

	public void setPickInTime(Date pickInTime)
	{
		this.pickInTime = pickInTime;
	}

	public boolean isDelivered() {
		return delivered;
	}

	public void setDelivered(boolean delivered) {
		this.delivered = delivered;
	}
}
