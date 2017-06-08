package subway.newclass;

import java.util.Date;

public class SubwayRouteKey implements Comparable<SubwayRouteKey>{

	private String src;
	private String dst;
	private Date arriveTime;
	
	public SubwayRouteKey() {
		
	}
	public SubwayRouteKey(String src, String dst, Date time) {
		this.src = src;
		this.dst = dst;
		this.arriveTime = new Date(time.getTime());
		this.arriveTime.setMinutes(0);
		this.arriveTime.setSeconds(0);
	}
	public String getSrc() {
		return src;
	}
	public void setSrc(String src) {
		this.src = src;
	}
	public String getDst() {
		return dst;
	}
	public void setDst(String dst) {
		this.dst = dst;
	}
	public Date getArriveTime() {
		return arriveTime;
	}
	public void setArriveTime(Date arriveTime) {
		this.arriveTime = arriveTime;
	}

	@Override
	public int compareTo(SubwayRouteKey route) {
		
		if(!src.equals(route.src)) {
			return src.compareTo(route.src);
		}
		if(!dst.equals(route.dst)) {
			return dst.compareTo(route.dst);
		}
//		if(arriveTime.getHours() != route.getArriveTime().getHours()) {
//			return arriveTime.getHours() - route.getArriveTime().getHours();
//		}
//		//前后5分钟的也可以
//		long diff = arriveTime.getTime() - route.getArriveTime().getTime();
//		if(diff <= 300000 && diff >= -300000) {
//			return 0;
//		}
		
		return (int) (arriveTime.getTime() - route.getArriveTime().getTime());
//		return arriveTime.getMinutes() - route.getArriveTime().getMinutes();
	}
}
