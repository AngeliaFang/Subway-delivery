package subway.newclass;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SubwayItem implements Comparable<SubwayItem>{

	private String src;
	private String dst;
	private Date arrive;
	private Date leave;
	private boolean carry;
	private static String day = "2015-04-01 ";
	public SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static int MAX_CARRY_COUNT = 1;//乘客最多可同时携带几个包裹
	public int currentCarryCount = 0;
	
	public SubwayItem() {
	}
	
	public SubwayItem(String str) {
		String[] results = str.split(" ");
		this.src = results[0];
		this.dst = results[1];
		this.carry = false;
		try {
			this.arrive = format.parse(day + results[2]);
			this.leave = format.parse(day + results[3]);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	public String getSrc() {
		return src;
	}
	public void setSrc(String src) {
		this.src = src;
	}
	public String getDst() {
		return dst;
	}
	public void setDst(String dst) {
		this.dst = dst;
	}
	public Date getArrive() {
		return arrive;
	}
	public void setArrive(Date arrive) {
		this.arrive = arrive;
	}
	public Date getLeave() {
		return leave;
	}
	public void setLeave(Date leave) {
		this.leave = leave;
	}
	@Override
	public int compareTo(SubwayItem item) {
		int result = this.getArrive().compareTo(item.getArrive());
		if(result != 0 ) {
			return result;
		}
		
		result = this.getDst().compareTo(item.getDst());
		if(result != 0 ) {
			return result;
		}
		
		result = this.getLeave().compareTo(item.getLeave());
		if(result != 0 ) {
			return result;
		}
		return 0;
	}
	public boolean isCarry() {
		return carry;
	}
	public void setCarry(boolean carry) {
		this.carry = carry;
	}
}
