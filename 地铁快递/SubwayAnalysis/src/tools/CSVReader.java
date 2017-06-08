/**
 * 
 */
package tools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import subway.newclass.AbstractStationItem;
import subway.newclass.MixStation;
import subway.newclass.SingleStationItem;

import java.util.TreeMap;
import java.util.TreeSet;


/**
 * @author 60347
 *
 */
public class CSVReader {

	public String stationPath;
	public TreeMap<String, AbstractStationItem> stationMap;
	public TreeMap<String, AbstractStationItem> chineseMap;
	public TreeMap<String, MixStation> mixStationMap;
	public HashMap<String, MixStation> regionSationMap; 
	
	public CSVReader(String stationPath) {
		this.stationPath = stationPath;
		stationMap = new TreeMap<String, AbstractStationItem>();
		chineseMap = new TreeMap<String, AbstractStationItem>();
		mixStationMap = new TreeMap<String, MixStation>();
		regionSationMap = new HashMap<String, MixStation>();
	}
	
	/**
	 * 把每个车站读取值stationMap和chineseMap中，分别通过id和中文名查找station，并初始化zoneMap
	 */
	@SuppressWarnings("resource")
	public void readStationItem() {
		try {
			FileInputStream inputStream = new FileInputStream(stationPath);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "GBK"));
			
			String line = null;
			reader.readLine();
			
			while((line = reader.readLine()) != null) {
				AbstractStationItem item = new SingleStationItem(line);
				if(item.isLegal()) {
					if(item.isChangeable()) {
						Iterator<Entry<String, AbstractStationItem>> ite = stationMap.entrySet().iterator();
						while(ite.hasNext()) {
							AbstractStationItem stationItem = ite.next().getValue();
							if(stationItem.getChineseName().equals(item.getChineseName())) {
								((SingleStationItem)stationItem).getStationSet().add(item);
								((SingleStationItem)item).getStationSet().add(stationItem);
							}
						}
					}
					stationMap.put(item.getId(), item);
					chineseMap.put(item.getLine() + "号线" + item.getChineseName(), item);
					//新的车站类型,混合车站，中文名相同的即为混合车站
					if(item.getChineseName().equals("浦电路")) {
						MixStation station = new MixStation();
						station.getStationList().add(item);
						mixStationMap.put(item.getChineseName() + "_" + item.getLine(), station);
						item.setMixStation(station);
					}
					else {
						if(mixStationMap.containsKey(item.getChineseName())) {
							mixStationMap.get(item.getChineseName()).getStationList().add(item);
							item.setMixStation(mixStationMap.get(item.getChineseName()));
						}
						else {
							MixStation station = new MixStation();
							station.getStationList().add(item);
							mixStationMap.put(item.getChineseName(), station);
							item.setMixStation(station);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}
	public void readPosition()
	{
		try {
			String fileName = SubwayMain.FILE_PREV + "data/subway_coordination"; 
			FileInputStream inputStream = new FileInputStream(fileName);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "GBK"));
			String line = null;
			while((line = reader.readLine()) != null)
			{
				String[] result = line.split(":");
				if(result.length < 2)
				{
					System.out.println(line);
					continue;
				}
				if(!mixStationMap.containsKey(result[0]) || 
					mixStationMap.get(result[0]).getLatitude() != Double.MIN_VALUE ||
					mixStationMap.get(result[0]).getLongitude() != Double.MIN_VALUE)
				{
					System.out.println("不合法的车站" + result[0]);
					continue;
				}
				String[] values = result[1].split(",");
				MixStation station = mixStationMap.get(result[0]);
				station.setLongitude(Double.parseDouble(values[0]));
				station.setLatitude(Double.parseDouble(values[1]));
				
				//设置车站的位置 保留三位有效数字
				String longitude = "" + station.getLongitude();
				String latitude = "" + station.getLatitude();
				station.setPosition(longitude.substring(0, 6) + "_" + latitude.substring(0, 7));
				
			}
			reader.close();
			
			//设置车站坐标到车站的映射
			for(MixStation station : mixStationMap.values())
			{
				regionSationMap.put(station.getPosition(), station);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public AbstractStationItem getStationByChinese(String str){
		return chineseMap.get(str);
	}

	/**
	 * 这个函数设置每个车站的包裹应该分配到那哪个最近的中转车站
	 * @param transferStation	中转车站集合
	 */
	public void setCorrespondingStation(TreeSet<String> transferStation)
	{
		for(String station : mixStationMap.keySet())
		{
		    MixStation myself = mixStationMap.get(station);
			myself.setCorrespondingStation(null);
			myself.getProxyClientStationList().clear();
			myself.setTransferForExpress(false);
        }
		//是中转车站的话设置为自己 否则设置为最近的中转车站
		for(String station : mixStationMap.keySet())
		{
			MixStation myself = mixStationMap.get(station);
			if (transferStation.contains(station))
			{
				myself.setTransferForExpress(true);
				myself.setCorrespondingStation(station);
				myself.getProxyClientStationList().add(station);
			}
			else
			{
				double minDistance  = Double.MAX_VALUE;
				String minNeigh = null;
				for (String neigh : transferStation)
				{
					if (neigh.equals(station))
					{
						continue;
					}
					double distance = Math.pow((myself.getLatitude() - mixStationMap.get(neigh).getLatitude()), 2) +
							Math.pow((myself.getLongitude() - mixStationMap.get(neigh).getLongitude()), 2);
					if (distance < minDistance) {
						minDistance = distance;
						minNeigh = neigh;
					}
				}
				myself.setCorrespondingStation(minNeigh);
				mixStationMap.get(minNeigh).getProxyClientStationList().add(station);
			}
		}
	}
}