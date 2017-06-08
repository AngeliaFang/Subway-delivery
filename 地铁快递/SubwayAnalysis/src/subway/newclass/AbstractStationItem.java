/**
 * 
 */
package subway.newclass;

import java.util.TreeSet;

/**
 * @author 60347
 * 抽象类的站点，有全局id，中文名，线路号，邻居集合等信息。是其他车站的基类
 */
public class AbstractStationItem implements Comparable<AbstractStationItem>{
	private String id;
	private String chineseName;
	private int line;
	private boolean legal = false;
	private boolean access = false;
	private boolean changeable = false;;	
	private boolean single = true;
	private TreeSet<AbstractStationItem> neighborSet = new TreeSet<AbstractStationItem>();
	private MixStation mixStation;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getChineseName() {
		return chineseName;
	}
	public void setChineseName(String chineseName) {
		this.chineseName = chineseName;
	}
	public int getLine() {
		return line;
	}
	public void setLine(int line) {
		this.line = line;
	}
	public boolean isLegal() {
		return legal;
	}
	public void setLegal(boolean legal) {
		this.legal = legal;
	}
	public boolean isAccess() {
		return access;
	}
	public void setAccess(boolean access) {
		this.access = access;
	}
	
	public boolean isFakedChangeable() {
		if(changeable || this.getId().equals("1045") || this.getId().equals("1134")) {//换乘站，嘉定北，嘉定新城
			return true;
		}
		
		return false;
	}
	
	public boolean isChangeable() {
		return changeable;
	}
	public void setChangeable(boolean changeable) {
		this.changeable = changeable;
	}

	public boolean isSingle() {
		return single;
	}
	public void setSingle(boolean isSingle) {
		this.single = isSingle;
	}
	
	public AbstractStationItem() {
	}
	
	public AbstractStationItem(AbstractStationItem item) {
		if(!(item instanceof AbstractStationItem)) {
			System.out.println(" not AbstractStationItem class");
			setLegal(false);
			return;
		}
		setId(item.getId());
		setChineseName(item.getChineseName());
		setLine(item.getLine());
		setChangeable(item.isChangeable());
		setLegal(item.isLegal());
		setMixStation(item.getMixStation());
	}
	
	public boolean equals(Object item) {
		if(isSingle() && ((AbstractStationItem)item).isSingle()) {
			return getId().equals(((AbstractStationItem)item).getId());
		}
		
		if(!isSingle() && !((AbstractStationItem)item).isSingle()) {
			return getId().equals(((AbstractStationItem)item).getId());
		}
		
		return false;	
	}
	
	public int hashcode(){
		return getId().hashCode();
	}
	public TreeSet<AbstractStationItem> getNeighborSet() {
		return neighborSet;
	}
	public void setNeighborSet(TreeSet<AbstractStationItem> neighborSet) {
		this.neighborSet = neighborSet;
	}
	
	@Override
	public int compareTo(AbstractStationItem item) {
		return this.getId().compareTo(item.getId());
	}
	public MixStation getMixStation() {
		return mixStation;
	}
	public void setMixStation(MixStation mixStation) {
		this.mixStation = mixStation;
	}

}
