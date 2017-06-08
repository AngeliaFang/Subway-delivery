package mapreduce;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

public class SubwayGrouper extends WritableComparator{

	protected SubwayGrouper() {
		super(SubwayKey.class, true);
	}

	@SuppressWarnings("rawtypes")
	public int compare(WritableComparable a, WritableComparable b) {
		SubwayKey first = (SubwayKey)a;
		SubwayKey second = (SubwayKey)b;
		return first.getSrcDst().compareTo(second.getSrcDst());
	}
}
