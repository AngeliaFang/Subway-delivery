package mapreduce;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class SubwayMapper extends Mapper<Object, Text, Text, Text>{

	public void map(Object key, Text value, Context context) {
		String[] result = value.toString().split(" ");
		if(result.length == 4) {
			Text outKey = new Text(result[0] + " " + result[1] + " " + result[2].substring(0, 2));
			try {
				context.write(outKey, new Text(result[2]));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
