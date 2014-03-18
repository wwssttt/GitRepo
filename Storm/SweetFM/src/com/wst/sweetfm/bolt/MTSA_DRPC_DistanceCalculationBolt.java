package com.wst.sweetfm.bolt;

import java.util.HashMap;
import java.util.Iterator;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import com.wst.sweetfm.util.MTSA_Const;

public class MTSA_DRPC_DistanceCalculationBolt extends BaseBasicBolt{
	@Override
    public void execute(Tuple tuple, BasicOutputCollector collector) {
      //String input = tuple.getString(1);
      //collector.emit(new Values(tuple.getValue(0), input + "!"));
      String inputStr = tuple.getString(1);
      System.err.println("dis cal: input = "+inputStr);
      HashMap<Integer,Double> predictTopicMap = new HashMap<Integer,Double>();
      HashMap<Integer,Double> songTopicMap = new HashMap<Integer,Double>();
      
      String[] items = inputStr.split(";");
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
      
      String songStr = items[1];
      String[] songItems = songStr.split(">");
      Integer sid = Integer.valueOf(songItems[0]);
      
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
      
      String result = sid+":"+hellDis;
      
      collector.emit(new Values(tuple.getValue(0),result));
      
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
      declarer.declare(new Fields("id", "result"));
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
