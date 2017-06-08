package subway.newclass;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by yaho on 16-7-29.
 */
public class RouteInfo {
    private double waitExcept;
    private double runExcept;
    private double waitExceptX2;
    private double runExceptX2;
//    private double waitVariance;
//    private double runVariance;
    private int waitCount;
    private int runCount;
    private Date beginTime;
    private Date endTime;
    private ArrayList<String> stationList;
    public static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

    public RouteInfo() {
        waitExcept = runExcept = waitExceptX2 = runExceptX2 = 0.0d;
        waitCount = runCount = 0;
        beginTime = new Date();
        endTime = new Date();
        stationList = new ArrayList<>();
    }

    public RouteInfo(RouteInfo route) {
        copy(route);
    }

    public void copy(RouteInfo route) {
        this.waitExcept = route.getWaitExcept();
        this.runExcept = route.getRunExcept();
        this.waitCount = route.getWaitCount();
        this.runCount = route.getRunCount();
        this.waitExceptX2 = route.getWaitExceptX2();
        this.runExceptX2 = route.getRunExceptX2();
        this.beginTime = new Date(route.getBeginTime().getTime());
        this.endTime = new Date(route.getEndTime().getTime());
        this.stationList = new ArrayList<>(route.getStationList());
    }

    public double getProbability(double maxMinute) {
        //P([limitedHour - AVG] < x) >= 1 - VAR / x^2
        double diff = maxMinute - getExpect();
        if(diff <= 0) {
            return 0d;
        }

        double prob = 1 - getVariance() / Math.pow(diff, 2);
        return prob;
    }

    public double getTotalWaitExcept() {
        return waitExcept * waitCount;
    }

    public double getTotalRunExcept() {
        return  runExcept * runCount;
    }

    public double getTotalWaitExceptX2() {
        return waitExceptX2 * waitCount;
    }

    public double getTotalRunExceptX2() {
        return runExceptX2 * runCount;
    }
    public double getExpect() {
        return runExcept + waitExcept;
    }

    public double getVariance() {
        return getRunVariance() + getWaitVariance();
    }

    public Date getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Date beginTime) {
        this.beginTime = new Date(beginTime.getTime());
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = new Date(endTime.getTime());
    }

    public ArrayList<String> getStationList() {
        return stationList;
    }

    public void setStationList(ArrayList<String> stationList) {
        this.stationList = new ArrayList<>(stationList);
    }

    public double getWaitExcept() {
        return waitExcept;
    }

    public void setWaitExcept(double waitExcept) {
        this.waitExcept = waitExcept;
    }

    public double getRunExcept() {
        return runExcept;
    }

    public void setRunExcept(double runExcept) {
        this.runExcept = runExcept;
    }

    public double getWaitExceptX2() {
        return waitExceptX2;
    }

    public void setWaitExceptX2(double waitExceptX2) {
        this.waitExceptX2 = waitExceptX2;
    }

    public double getRunExceptX2() {
        return runExceptX2;
    }

    public void setRunExceptX2(double runExceptX2) {
        this.runExceptX2 = runExceptX2;
    }

    public double getWaitVariance() {
        return getWaitExceptX2() - getWaitExcept() * getWaitExcept();
    }

    public double getRunVariance() {
        return getRunExceptX2() - getRunExcept() * getRunExcept();
    }

    public int getWaitCount() {
        return waitCount;
    }

    public void setWaitCount(int waitCount) {
        this.waitCount = waitCount;
    }

    public int getRunCount() {
        return runCount;
    }

    public void setRunCount(int runCount) {
        this.runCount = runCount;
    }

    /**
     * 输出内容：od 开始时间 结束时间 等待期望 等待平方的期望 等待期望计数 运行期望 运行平方的期望 运行计数 详细路径
     * @return
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(stationList.get(0));
        sb.append(" ");
        sb.append(stationList.get(stationList.size() - 1));
        sb.append(" ");
        sb.append(simpleDateFormat.format(beginTime));
        sb.append(" ");
        sb.append(simpleDateFormat.format(endTime));
        sb.append(" ");
        sb.append(waitExcept);
        sb.append(" ");
        sb.append(waitExceptX2);
        sb.append(" ");
        sb.append(waitCount);
        sb.append(" ");
        sb.append(runExcept);
        sb.append(" ");
        sb.append(runExceptX2);
        sb.append(" ");
        sb.append(runCount);
        for(String station : stationList) {
            sb.append(" ");
            sb.append(station);
        }
        return sb.toString();
    }

    /**
     * 输入内容：od 开始时间 结束时间 等待期望 等待平方的期望 等待期望计数 运行期望 运行平方的期望 运行计数 详细路径
     * @param line
     * @throws Exception
     */
    public void initWithLine(String line) throws Exception {
        String[] result = line.split(" ");
        setBeginTime(format.parse("2015-04-01 " + result[2]));
        setEndTime(format.parse("2015-04-01 " + result[3]));
        setWaitExcept(Double.parseDouble(result[4]));
        setWaitExceptX2(Double.parseDouble(result[5]));
        setWaitCount(Integer.parseInt(result[6]));
        setRunExcept(Double.parseDouble(result[7]));
        setRunExceptX2(Double.parseDouble(result[8]));
        setRunCount(Integer.parseInt(result[9]));
        for(int i = 10; i < result.length; i++) {
            stationList.add(result[i]);
        }
    }
}
