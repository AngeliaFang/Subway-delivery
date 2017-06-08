package subway.newclass;

import java.util.Date;

public class SubwayArrivalTime {
	private Date arriveTime;
	private Date leaveTime;
	private int day;
	
	public Date getArriveTime() {
		return arriveTime;
	}
	public void setArriveTime(Date arriveTime) {
		this.arriveTime = arriveTime;
	}
	public Date getLeaveTime() {
		return leaveTime;
	}
	public void setLeaveTime(Date leaveTime) {
		this.leaveTime = leaveTime;
	}

	public int getDay() { return day; }

	public void setDay(int day) { this.day = day; }
}
