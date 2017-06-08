/**
 * 
 */
package subway.newclass;

/**
 * @author 60347
 *
 */
public class CombinationStation implements Comparable<CombinationStation>{
	public AbstractStationItem src;
	public AbstractStationItem dst;
	public boolean equals(Object obj) {
		CombinationStation com = (CombinationStation)obj;
		if(src.getId().equals(com.src.getId()) && dst.getId().equals(com.dst.getId())) {
			return true;
		}
		return false;
	}
	public CombinationStation(AbstractStationItem src, AbstractStationItem dst) {
		this.src = src;
		this.dst = dst;
	}
	
	public int hashCode() {
		return (src.getId().hashCode() >> 16) | (dst.getId().hashCode() << 16);
	}
	
	@Override
	public int compareTo(CombinationStation com) {
		int res = src.getId().compareTo(com.src.getId());
		if(res != 0) {
			return res;
		}
		else {
			return dst.getId().compareTo(com.dst.getId());
		}
	}
}