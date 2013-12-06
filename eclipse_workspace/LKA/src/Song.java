import java.util.ArrayList;
import java.util.HashMap;

public class Song {
	private int sid;//歌曲id
	private HashMap<Integer,Float> topicMap;//歌曲——主题
	private ArrayList<Integer> highTopics;//歌曲显著主题
	
	/**
	 * 构造函数
	 * @param sid 歌曲sid
	 * @param topicMap 主题概率
	 */
	public Song(int sid, HashMap<Integer,Float> topicMap) {
		super();
		this.sid = sid;
		this.highTopics = new ArrayList<Integer>();
		this.topicMap = new HashMap<Integer,Float>();
		for(Integer key: topicMap.keySet()){
			float pro = topicMap.get(key);
			if(!this.topicMap.containsKey(key)){
				this.topicMap.put(key, pro);
			}
		}
	}
	
	/**
	 * 获取歌曲sid
	 * @return 歌曲sid
	 */
	public int getSid() {
		return sid;
	}
	
	/**
	 * 设置歌曲sid
	 * @param sid 歌曲sid
	 */
	public void setSid(int sid) {
		this.sid = sid;
	}
	
	/**
	 * 获取歌曲显著主题列表
	 * @return
	 */
	public ArrayList<Integer> getHighTopics(){
		return this.highTopics;
	}
	
	/**
	 * 添加显著主题
	 * @param topicId 主题id
	 */
	public void addHighTopic(int topicId){
		if(!this.highTopics.contains(topicId)){
			this.highTopics.add(topicId);
		}
	}
	
	/**
	 * 批量设置显著主题
	 * @param highTopics  显著主题
	 */
	public void setHighTopic(ArrayList<Integer> highTopics){
		this.highTopics.clear();
		for(int i = 0; i < highTopics.size(); i++){
			int topicId = highTopics.get(i);
			if(!this.highTopics.contains(topicId)){
				this.highTopics.add(topicId);
			}
		}
	}
	
	/**
	 * 获取歌曲的主题概率表
	 * @return
	 */
	public HashMap<Integer,Float> getTopicMap() {
		return topicMap;
	}
	
	/**
	 * 设置歌曲的主题概率表
	 * @param topicMap 主题概率表
	 */
	public void setTopicMap(HashMap<Integer,Float> topicMap) {
		this.topicMap.clear();
		for(Integer key: topicMap.keySet()){
			float pro = topicMap.get(key);
			if(!this.topicMap.containsKey(key)){
				this.topicMap.put(key, pro);
			}
		}
	}
	
	/**
	 * 基于序列模式挖掘的上下文评分
	 * @param predictTopics 预测的主题列表
	 * @return 上下文评分
	 */
	public float contextScore(ArrayList<Integer> predictTopics){
		float score  = 0;
		for(int i = 0; i < predictTopics.size(); i++){
			score += topicMap.get(predictTopics.get(i));
		}
		score /= predictTopics.size();
		return score;
	}
	
	/**
	 * 计算当前歌曲与另一歌曲的余弦相似度
	 * @param anoSong 待计算歌曲
	 * @return 余弦相似度
	 */
	public float cosineSimilarity(Song anoSong){
		float sim = 0;
		HashMap<Integer,Float> anoTopicMap = anoSong.getTopicMap();
		if(anoTopicMap.size() != this.topicMap.size()){
			try{
				throw new LKAException("两首歌曲的topicmap维度不一致");
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		float product = 0;
		float curSum = 0;
		float anoSum = 0;
		for(Integer key : anoTopicMap.keySet()){
			float curPro = this.topicMap.get(key);
			float anoPro = anoTopicMap.get(key);
			product += (curPro*anoPro);
			curSum += (curPro*curPro);
			anoSum += (anoPro*anoPro);
		}
		curSum = (float) Math.sqrt(curSum);
		anoSum = (float) Math.sqrt(anoSum);
		
		sim = product / (curSum * anoSum);
		
		return sim;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(sid+"=");
		for(Integer key: topicMap.keySet()){
			sb.append(key+":"+topicMap.get(key)+"##");
		}
		return sb.toString();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + sid;
		result = prime * result + ((topicMap == null) ? 0 : topicMap.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Song other = (Song) obj;
		if (sid != other.sid)
			return false;
		return true;
	}
}
