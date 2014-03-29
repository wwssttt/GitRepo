package com.wst.sweetfm.bolt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.wst.sweetfm.util.MTSA_Const;

import backtype.storm.coordination.BatchOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBatchBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class MTSA_DRPC_DistanceCalculationBatchBolt extends BaseBatchBolt{
	private BatchOutputCollector _collector;
    private HashMap<Integer,Double> _songDis = new HashMap<Integer,Double>();
    private Object _id;
	@Override
	public void prepare(Map conf, TopologyContext context,
			BatchOutputCollector collector, Object id) {
		// TODO Auto-generated method stub
		_collector = collector;
	}

	@Override
	public void execute(Tuple tuple) {
		// TODO Auto-generated method stub
		_id = tuple.getValue(0);
		String inputStr = tuple.getString(1);
	    //System.err.println("dis cal: input = "+inputStr);
	    HashMap<Integer,Double> predictTopicMap = new HashMap<Integer,Double>();
	    HashMap<Integer,Double> songTopicMap = new HashMap<Integer,Double>();
	      
	    String[] items = inputStr.split(";");
	    
	    String songStr = items[1];
	    String[] songItems = songStr.split(">");
	    Integer sid = Integer.valueOf(songItems[0]);
	    
	    if(!_songDis.containsKey(sid)){
	    	String predictedStr = items[0];
		      
		    String[] predictedTopicStr = predictedStr.split("#");
		    for(int i = 0; i < predictedTopicStr.length; i++){
		    	String topicStr = predictedTopicStr[i];
		    	String[] topicItems = topicStr.split(":");
		    	Integer tid = Integer.valueOf(topicItems[0]);
		    	Double probability = Double.valueOf(topicItems[1]);
		    	if(!predictTopicMap.containsKey(tid)){
		    		predictTopicMap.put(tid, probability);
		    	}
		    }
		      
		    String[] songTopicStr = songItems[1].split("#"); 
		    for(int i = 0; i < songTopicStr.length; i++){
		    	String topicStr = songTopicStr[i];
		    	String[] topicItems = topicStr.split(":");
		    	Integer tid = Integer.valueOf(topicItems[0]);
		    	Double probability = Double.valueOf(topicItems[1]);
		    	if(!songTopicMap.containsKey(tid)){
		    		songTopicMap.put(tid, probability);
		    	}
		    }
		      
		    double hellDis = HellingerDistance(predictTopicMap,songTopicMap);
		    _songDis.put(sid, hellDis);
	    }
	}

	@Override
	public void finishBatch() {
		// TODO Auto-generated method stub
		List<Map.Entry<Integer, Double>> disInfos =
			    new ArrayList<Map.Entry<Integer, Double>>(_songDis.entrySet());
		
		//排序
		Collections.sort(disInfos, new Comparator<Map.Entry<Integer, Double>>() {   
		    public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {      
		    	if ((o1.getValue() - o2.getValue())>0)  
		            return 1;  
		        else if((o1.getValue() - o2.getValue())==0)  
		            return 0;  
		        else   
		            return -1; 
		    }
		}); 
		
		int size = disInfos.size();
		
		for(int i = 0; i < size; i++){
			if(i > MTSA_Const.REC_NUM){
				break;
			}
			Integer sid = disInfos.get(i).getKey();
			Double distance = disInfos.get(i).getValue();
			String resultStr = sid+":"+distance;
			_collector.emit(new Values(_id,resultStr));
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// TODO Auto-generated method stub
		declarer.declare(new Fields("id", "distance"));
	}
	
	private double HellingerDistance(HashMap<Integer,Double> map1,HashMap<Integer,Double> map2){
    	double distance = 0;
    	if (map1.size() != map2.size() || map1.size() != MTSA_Const.TOPIC_NUM){
    		return MTSA_Const.MAX_HELLDIS;
    	}
    	
    	Iterator<Integer> iter = map1.keySet().iterator();
    	while(iter.hasNext()){
    		Integer tid = iter.next();
    		double probability1 = map1.get(tid); 
    		if(!map2.containsKey(tid)){
    			return MTSA_Const.MAX_HELLDIS;
    		}
    		double probability2 = map2.get(tid);
    		if(probability1 <= 0){
    			probability1 = 1.0 / 10000000;
    		}
    		if(probability2 <= 0){
    			probability2 = 1.0 / 10000000;
    		}
    		distance += Math.pow(Math.sqrt(probability1)-Math.sqrt(probability2), 2);
    	}
    	distance = Math.sqrt(distance);
		distance = distance * (1.0 / Math.sqrt(2));
    	return distance;
    }

}