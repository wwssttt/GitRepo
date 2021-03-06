/**
 * MTSA_Song.java
 * 版权所有(C) 2014 
 * 创建:wwssttt 2014-03-07 22:00:00
 * 描述:定义歌曲类，一首歌曲有标识符和主题分布两个属性
 */
package com.wst.sweetfm.model;

import java.util.HashMap;

public class MTSA_Song {
	//id of song
	private long sid;
	//topic distribution
	HashMap<Integer,Double> topicMap;
	
	//get sid
	public long getSid() {
		return sid;
	}
	//set sid
	public void setSid(long sid) {
		this.sid = sid;
	}
	//get topic map
	public HashMap<Integer, Double> getTopicMap() {
		return new HashMap<Integer,Double>(this.topicMap);
	}
	//set topic map
	public void setTopicMap(HashMap<Integer, Double> topicMap) {
		this.topicMap = new HashMap<Integer,Double>(topicMap);
	}
	//construction method
	public MTSA_Song(long sid, HashMap<Integer, Double> topicMap) {
		super();
		this.sid = sid;
		this.topicMap = new HashMap<Integer,Double>(topicMap);
	}
	//toString
	@Override
	public String toString() {
		return "MTSA_Song [sid=" + sid + ", topicMap=" + topicMap + "]";
	}
}
