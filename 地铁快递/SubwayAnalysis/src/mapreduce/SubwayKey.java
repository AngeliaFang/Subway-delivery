package mapreduce;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class SubwayKey implements WritableComparable<SubwayKey>{

	private Text srcDst;
	private Text time;
	
	public void setSrcDst(String str) {
		srcDst = new Text(str);
	}
	
	public Text getSrcDst() {
		return srcDst;
	}
	
	public void setTime(String time) {
		this.time = new Text(time);
	}
	
	public Text getTime() {
		return time;
	}
	
	public SubwayKey() {
		srcDst = new Text();
		time = new Text();
	}
	@Override
	public void readFields(DataInput arg0) throws IOException {
		srcDst.readFields(arg0);
		time.readFields(arg0);
	}

	@Override
	public void write(DataOutput arg0) throws IOException {
		srcDst.write(arg0);
		time.write(arg0);
	}

	@Override
	public int compareTo(SubwayKey arg0) {
		return srcDst.compareTo(arg0.getSrcDst());
	}

}
