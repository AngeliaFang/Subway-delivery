package subway.newclass;

public class SimpleDelay{
	private long runDelay;
	private long traffic;
	private long nearTrcffic;
	private int routeCount;
	public long getRunDelay() {
		return runDelay;
	}
	public void setRunDelay(long runDelay) {
		this.runDelay = runDelay;
	}
	public long getTraffic() {
		return traffic;
	}
	public void setTraffic(long traffic) {
		this.traffic = traffic;
	}
	public long getNearTrcffic() {
		return nearTrcffic;
	}
	public void setNearTrcffic(long nearTrcffic) {
		this.nearTrcffic = nearTrcffic;
	}
	public int getRouteCount() {
		return routeCount;
	}
	public void setRouteCount(int routeCount) {
		this.routeCount = routeCount;
	}
}
