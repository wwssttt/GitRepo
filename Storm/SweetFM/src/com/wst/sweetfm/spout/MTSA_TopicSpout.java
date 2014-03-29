/**
 * MTSA_TopicSpout.java
 * 版权所有(C) 2014 
 * 创建:wwssttt 2014-03-07 22:05:00
 * 描述:主题分发喷管，将序列按照主题序号展开并发射到下级处理单位
 */
package com.wst.sweetfm.spout;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class MTSA_TopicSpout extends BaseRichSpout{
	
	private SpoutOutputCollector collector;
	private int spoutIndex;
	private static HashMap<Integer,HashMap<Integer,Double>> songMap = new HashMap<Integer,HashMap<Integer,Double>>(){
		{
		  put(0,new HashMap<Integer,Double>(){
			  {
			    put(0,0.5);
			    put(1,0.2);
			    put(2,0.3);
			  }
		   });
		  put(1,new HashMap<Integer,Double>(){
			  {
			    put(0,0.1);
			    put(1,0.4);
			    put(2,0.5);
			  }
		   });
		  put(2,new HashMap<Integer,Double>(){
			  {
			    put(0,0.15);
			    put(1,0.4);
			    put(2,0.45);
			  }
		   });
		  put(3,new HashMap<Integer,Double>(){
			  {
			    put(0,0.25);
			    put(1,0.4);
			    put(2,0.35);
			  }
		   });
		}
	};
	private Queue<String> queue;
	
	@Override
	public void open(Map conf, TopologyContext context,
			SpoutOutputCollector collector) {
		// TODO Auto-generated method stub
		this.collector = collector;
		this.spoutIndex = context.getThisTaskIndex();
	}

	@Override
	public void nextTuple() {
		// TODO Auto-generated method stub
		//System.err.println("queue size = "+queue.size());
		if (!this.queue.isEmpty()){
			String seqStr = this.queue.poll();
			System.err.println("spoutIndex = "+this.spoutIndex+" seqStr = "+seqStr);
			this.collector.emit(new Values(seqStr));
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// TODO Auto-generated method stub
		declarer.declare(new Fields("topic"));
	}
	//construction method
	//parse original sequence to sub sequences of different topics
	public MTSA_TopicSpout(String originalSeq){
		this.queue = new LinkedList<String>();
		String[] items = originalSeq.split(">");
		Integer[] sids = new Integer[items.length];
		for(int i = 0; i < sids.length; i++){
			sids[i] = Integer.valueOf(items[i]);
		}
		for(int tid = 0; tid < 3; tid++){
			StringBuffer subSeq = new StringBuffer();
			subSeq.append(tid+"#");
			for(int sIndex = 0; sIndex < sids.length; sIndex++){
				Integer sid = sids[sIndex];
				if (sIndex == (sids.length - 1)){
					subSeq.append(songMap.get(sid).get(tid));
				}else{
					subSeq.append(songMap.get(sid).get(tid)+">");
				}
			}
			this.queue.add(subSeq.toString());
		}
	}
}