/**
 * 
 */
package subway.newclass;

import java.util.ArrayList;

/**
 * @author 60347
 *
 */
public class Route {
	private ArrayList<AbstractStationItem> routes;
	private int changCount = 0;
	private double probability = 0.0;
	
	public Route(ArrayList<AbstractStationItem> routes, int changeCount) {
		this.setRoutes(new ArrayList<AbstractStationItem>(routes));
		this.setChangCount(changeCount);
	}

	public ArrayList<AbstractStationItem> getRoutes() {
		return routes;
	}

	private void setRoutes(ArrayList<AbstractStationItem> routes) {
		this.routes = routes;
	}

	public int getChangCount() {
		return changCount;
	}

	public void setChangCount(int changCount) {
		this.changCount = changCount;
	}

	public double getProbability() {
		return probability;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}
}
