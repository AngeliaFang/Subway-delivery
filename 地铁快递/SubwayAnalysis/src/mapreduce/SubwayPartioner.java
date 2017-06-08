package mapreduce;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Partitioner;

public class SubwayPartioner extends Partitioner<SubwayKey, Text>{

	@Override
	public int getPartition(SubwayKey arg0, Text arg1, int arg2) {
		return (arg0.getSrcDst().hashCode() & Integer.MAX_VALUE) % arg2;
	}

}
