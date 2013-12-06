import java.util.ArrayList;
import java.util.HashMap;

public class User {
	private int uid;//用户id
	private CONST.USER_TYPE type;//用户类型,训练、测试
	private int lastSid;//最后一首歌曲id
	private ArrayList<Integer> playlists;//列表
	private HashMap<Integer,Integer> countMap;//歌曲频率
	
	/**
	 * 构造函数
	 * @param uid 用户id
	 */
	public User(int uid, CONST.USER_TYPE type){
		super();
		this.uid = uid;
		this.type = type;
		this.lastSid = -1;
		this.playlists = new ArrayList<Integer>();
		this.countMap = new HashMap<Integer,Integer>();
	}
	
	/**
	 * 构造函数.
	 * @param uid 用户id
	 * @param type 用户类型
	 * @param lastSid 最后一首歌曲的Id
	 * @param playlists 用户所听的歌曲序列
	 * @param countMap 用户所听的歌曲频率， sid:count
	 */
	public User(int uid, CONST.USER_TYPE type, int lastSid, ArrayList<Integer> playlists,
			HashMap<Integer, Integer> countMap) {
		super();
		this.uid = uid;
		this.type = type;
		this.lastSid = lastSid;
		this.playlists = new ArrayList<Integer>();
		for(int i = 0; i < playlists.size(); i++){
			this.playlists.add(playlists.get(i));
		}
		this.countMap = new HashMap<Integer,Integer>();
		for(Integer key:countMap.keySet()){
			this.countMap.put(key, countMap.get(key));
		}
	}
	
	/**
	 * 获取用户Id
	 */
	public int getUid() {
		return uid;
	}
	/**
	 * 设置用户Id
	 * @param uid 用户id
	 */
	public void setUid(int uid) {
		this.uid = uid;
	}
	
	/**
	 * <p>获取用户类型<br>
	 */
	public CONST.USER_TYPE getType(){
		return this.type;
	}
	
	/**
	 * 设置用户类型.
	 * @param type 用户类型
	 */
	public void setType(CONST.USER_TYPE type){
		this.type = type;
	}
	
	/**
	 * 获取最后一首歌曲的sid
	 */
	public int getLastSid() {
		return this.lastSid;
	}
	/**
	 * 设置最后一首歌曲的sid
	 * @param lastSid 最后一首歌曲的sid
	 */
	public void setLastSid(int lastSid) {
		this.lastSid = lastSid;
	}
	
	/**
	 * 获取用户的播放序列
	 */
	public ArrayList<Integer> getPlaylists() {
		return playlists;
	}
	/**
	 * 设置播放序列
	 * @param playlists 用户的播放序列
	 */
	public void setPlaylists(ArrayList<Integer> playlists) {
		this.playlists.clear();
		for(int i = 0; i < playlists.size(); i++){
			this.playlists.add(playlists.get(i));
		}
	}
	/**
	 * 获取用户的听歌频率
	 */
	public HashMap<Integer, Integer> getCountMap() {
		return countMap;
	}
	/**
	 * 设置用户的听歌频率
	 * @param countMap 听歌频率的哈希表  歌曲sid:收听次数count
	 */
	public void setCountMap(HashMap<Integer, Integer> countMap) {
		this.countMap.clear();
		for(Integer key:countMap.keySet()){
			this.countMap.put(key, countMap.get(key));
		}
	}

	/**
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((countMap == null) ? 0 : countMap.hashCode());
		result = prime * result
				+ ((playlists == null) ? 0 : playlists.hashCode());
		result = prime * result + uid;
		return result;
	}

	/**
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (uid != other.uid)
			return false;
		return true;
	}
	
	/**
	 * 向用户的收听序列桌添加歌曲并更新用户的收听频率
	 * @param sid  添加的歌曲sid
	 */
	public void addSong(int sid){
		this.playlists.add(sid);
		
		if(this.countMap.containsKey(sid)){
			int count = this.countMap.get(sid);
			count++;
			this.countMap.put(sid, count);
		}else{
			this.countMap.put(sid, 1);
		}
	}
	
	/**
	 * 计算当前用户和另一用户的余弦相似度
	 * @param anoUser 待计算的另一用户
	 * @return 余弦相似度
	 */
	public float cosinSimilarity(User anoUser){
		float similarity = 0;
		
		HashMap<Integer,Integer> anoCountMap = anoUser.getCountMap();
		
		float product = 0;
		float curSum = 0;
		float anoSum = 0;
		
		//计算当前用户和待计算用户共同听过的歌曲
		ArrayList<Integer> commonSong = new ArrayList<Integer>();
		for(Integer key : this.countMap.keySet()){
			int curCount = this.countMap.get(key);
			curSum += (curCount*curCount);
		}
		for(Integer key : anoCountMap.keySet()){
			int anoCount = anoCountMap.get(key);
			anoSum += (anoCount*anoCount);
			if(this.countMap.containsKey(key)){
				commonSong.add(key);
			}
		}
		
		for(int j = 0; j < commonSong.size(); j++){
			int curSid = commonSong.get(j);
			if(!this.countMap.containsKey(curSid) || !anoCountMap.containsKey(curSid)){
				try{
					throw new LKAException(curSid+"不在commonSong中");
				}catch(LKAException e){
					e.printStackTrace();
				}
				
			}
			int curCount = this.countMap.get(curSid);
			int anoCount = anoCountMap.get(curSid);
			product += (curCount*anoCount);
		}
		
		curSum = (float) Math.sqrt(curSum);
		anoSum = (float) Math.sqrt(anoSum);
		
		similarity = product / (curSum*anoSum);
		
		return similarity;
	}
	
	/**
	 * 获取用户最近所听的若干首歌曲
	 * @return 用户最近所听的若干首歌曲
	 */
	public ArrayList<Integer> getRecentListenedSongs(){
		ArrayList<Integer> recentListenedSongs = new ArrayList<Integer>();
		
		for(int i = this.playlists.size() - CONST.LAST_LISTENED_NUM; i < this.playlists.size(); i++){
			int sid = this.playlists.get(i);
			if(!recentListenedSongs.contains(sid)){
				recentListenedSongs.add(sid);
			}
		}
		
		return recentListenedSongs;
	}
}
