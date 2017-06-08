package tools;

import subway.newclass.RouteInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by 60347 on 2016/8/4.
 */
public class MyThread extends Thread{
    public double limitedProb;
    public SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
    public RouteUtil routeUtil;
    public ArrayList<String> srcList;
    public ArrayList<String> dstList;
    public void run() {
        validateInEachZone();
    }

    public void validateInOneDay() {
        try {
            //每个od都从早到晚跑一次
            for (int i = 0; i < srcList.size(); i++) {
                String src = srcList.get(i);
                String dst = dstList.get(i);

                //最大跳数3 概率0.99
                boolean findInZone = false;
                Date queryTime = format.parse("2015-04-01 07:00:00");
                Date endTime = format.parse("2015-04-01 22:00:00");
                //时间限制从1到6
                for (int hour = 1; hour <= 6; hour += 1) {
                    //设置最大时间
                    routeUtil.timeConstrain = hour;

                    //成功的输出格式：src dst 时间区间 结束时间 满足概率下的预计小时数 期望 方差 路径
                    RouteInfo info = routeUtil.getTemporalRouteWithProb(src, dst, queryTime, endTime, 5, hour,
                            limitedProb);
                    if (info != null) {
                        findInZone = true;
                        //输出内容：src dst 开始时间 结束时间 规定的小时数 平均延迟（分钟） 方差 具体的路径
                        StringBuilder sb = new StringBuilder();
                        sb.append(src + " " + dst + " " + simpleDateFormat.format(info.getBeginTime()) +
                                " " + simpleDateFormat.format(info.getEndTime()) + " " + hour + " " +
                                (info.getWaitExcept() + info.getRunExcept()) +
                                " " + (info.getWaitVariance() + info.getRunVariance()));
                        for(String station : info.getStationList()) {
                            sb.append(" " + station);
                        }
                        System.out.println(sb.toString());
                        break;
                    }
                }
                if (!findInZone) {
                    //失败的输出格式：src dst 时间区间
                    System.out.println(src + " " + dst + " " + simpleDateFormat.format(queryTime));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public void validateInEachZone() {
        try {
            //每个od都从早到晚跑一次
            for (int i = 0; i < srcList.size(); i++) {
                String src = srcList.get(i);
                String dst = dstList.get(i);

                //最大跳数3 概率0.99
                for (int beginHour = 7; beginHour < 19; beginHour++) {

                    boolean findInZone = false;
                    Date queryTime = format.parse("2015-04-01 07:00:00");
                    Date endTime = format.parse("2015-04-01 22:00:00");
                    queryTime.setTime(queryTime.getTime() + (beginHour - 7) * 3600000);
                    //时间限制从1到6
                    for (int hour = 1; hour <= 6; hour += 1) {
                        //设置最大时间
                        routeUtil.timeConstrain = hour;

                        //成功的输出格式：src dst 时间区间 结束时间 满足概率下的预计小时数 期望 方差 路径
                        RouteInfo info = routeUtil.getTemporalRouteWithProb(src, dst, queryTime, endTime, 5, hour,
                                limitedProb);
                        if (info != null) {
                            findInZone = true;
                            //输出内容：src dst 开始时间 结束时间 规定的小时数 平均延迟（分钟） 方差 具体的路径
                            StringBuilder sb = new StringBuilder();
                            sb.append(src + " " + dst + " " + simpleDateFormat.format(info.getBeginTime()) +
                                    " " + simpleDateFormat.format(info.getEndTime()) + " " + hour + " " +
                                    (info.getWaitExcept() + info.getRunExcept()) +
                                    " " + (info.getWaitVariance() + info.getRunVariance()));
                            for(String station : info.getStationList()) {
                                sb.append(" " + station);
                            }
                            System.out.println(sb.toString());
                            break;
                        }
                    }
                    if (!findInZone) {
                        //失败的输出格式：src dst 时间区间
                        System.out.println(src + " " + dst + " " + simpleDateFormat.format(queryTime));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
