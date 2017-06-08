package subway.newclass;

import java.util.ArrayList;
import java.util.Date;


public class TimeZoneRoute {

	public Date beginTime = new Date();
	public Date endTime = new Date();
	public ArrayList<String> routeList = new ArrayList<String>();
	public TimeZoneRoute(){}
	public TimeZoneRoute(TimeZoneRoute route)
	{
		this.beginTime = new Date(route.beginTime.getTime());
		this.endTime = new Date(route.endTime.getTime());
		this.routeList.addAll(route.routeList);
	}
}
