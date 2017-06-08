package mapreduce;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

public class SubwayComparator extends WritableComparator {

	public SubwayComparator() {
		super(SubwayKey.class, true);
	}
	
	@SuppressWarnings("rawtypes")
	public int compare(WritableComparable a, WritableComparable b) {
		SubwayKey first = (SubwayKey)a;
		SubwayKey second = (SubwayKey)b;
		
		int result = first.getSrcDst().compareTo(second.getSrcDst());
		if(result != 0) {
			return result;
		}
		
		return first.getTime().compareTo(second.getTime());
		
	}
}
