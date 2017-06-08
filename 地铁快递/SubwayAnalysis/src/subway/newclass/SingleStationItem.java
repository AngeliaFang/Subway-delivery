/**
 * 
 */
package subway.newclass;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * @author 60347
 * 真实的车站类，有额外的车站英文名字和线路上的站点ID
 */
public class SingleStationItem extends AbstractStationItem{
	private String name;
	private int stationID;
	private TreeSet<AbstractStationItem> stationSet;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getStationID() {
		return stationID;
	}
	
	public void setStationID(int stationID) {
		this.stationID = stationID;
	}
	
	public TreeSet<AbstractStationItem> getStationSet() {
		return stationSet;
	}
	public void setStationSet(TreeSet<AbstractStationItem> stationSet) {
			this.stationSet = stationSet;
	}

	public SingleStationItem(String str) {
		String[] results = str.split(",");
		if(results.length != 9) {
			setLegal(false);
			return;
		}
		//跳过严御路地铁站
		if(results[1].equals("严御路"))
		{
			setLegal(false);
			return;
		}
		setLegal(true);
		setSingle(true);
		setId(results[0].trim());
		setChineseName(results[1]);
		setName(results[2].trim());
		setLine(Integer.parseInt(results[3]));
		stationID = Integer.parseInt(results[7]);
		if("换乘站".equals(results[6])) {
			setChangeable(true);
			setStationSet(new TreeSet<AbstractStationItem>(new Comparator<AbstractStationItem>() {

				@Override
				public int compare(AbstractStationItem station1, AbstractStationItem station2) {
					return station1.getId().compareTo(station2.getId());
				}
			}));
		}
		else {
			setChangeable(false);
			setStationSet(null);
		}
	}
	
	public SingleStationItem(SingleStationItem item) {
		if(!(item instanceof SingleStationItem)) {
			System.out.println("不是SingleStationItem类");
			setLegal(false);
			return;
		}
		setId(item.getId());
		setName(item.getName());
		setStationID(item.getStationID());
		setChineseName(item.getChineseName());
		setLine(item.getLine());
		setChangeable(item.isChangeable());
		setLegal(item.isLegal());
		setSingle(item.isSingle());
		setStationSet(item.getStationSet());
	}
}
