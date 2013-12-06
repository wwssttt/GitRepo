import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.lsa.LatentSemanticAnalysis;
import edu.ucla.sspace.vector.DoubleVector;

public class UserMatrix {
	private ArrayList<User> trainingUsers;//训练用户列表
	private ArrayList<User> testingUsers;//测试用户列表
	private float[][]  userSimilarity;//用户余弦相似度矩阵
	private HashMap<Integer,Integer> popularSongMap;//歌曲收听次数统计
	private ArrayList<Integer> songsSortByPopularity;
	private int[][]  knnMatrix;//测试用户最近邻的N个训练集用户
	
	/**
	 * 用户矩阵的构造函数
	 * @param trainingPath  训练集数据路径
	 * @param testingPath   测试集数据路径
	 * @return 构造一个新的用户矩阵
	 */
	public UserMatrix(String trainingPath, String testingPath, boolean knnOrPopular){
		System.out.println("I am in UserMatrix......");
		this.trainingUsers = new ArrayList<User>();
		this.testingUsers = new ArrayList<User>();
		loadUserFromFile(trainingPath,testingPath,knnOrPopular);
		constructTrainingStatistics();
		
		if(knnOrPopular){
			constructPopularityModel();
			constructKnnMatrix();
		}
		
		System.out.println("I am out UserMatrix......");
	}
	
	/**
	 * 从文件中读取播放列表.
	 * <p>构造训练集用户列表和测试集用户列表<br>
	 * <p>同时构造用户的相似度矩阵<br>
	 * @param trainingPath  训练集数据路径
	 * @param testingPath   测试集数据路径
	 */
	public void loadUserFromFile(String trainingPath, String testingPath,boolean knnOrPopular){
		System.out.println("I am in loadUserFromFile......");
		System.out.println("trainingPath = "+trainingPath);
		System.out.println("testingPath = "+trainingPath);
		
		//清空原始矩阵
		this.trainingUsers.clear();
		this.testingUsers.clear();
		
		//开始加载并读取相关文件
		try{
			
			//逐行读取训练集用户数据
			BufferedReader trainingBr = new BufferedReader(new FileReader(trainingPath));
			String trainingLine = null;
			int trainingUid = 0;
			while((trainingLine = trainingBr.readLine()) != null){
				User trainingUser = new User(trainingUid,CONST.USER_TYPE.TRAIN);
				
				String[] trainingSongs = trainingLine.split("==>");
				int trainingLen = trainingSongs.length;
				
				//将最后一首歌曲作为目标歌曲
				trainingUser.setLastSid(Integer.valueOf(trainingSongs[trainingLen-1]));
				
				//读取训练集用户的历史记录
				for(int i = 0; i < trainingLen; i++){
					trainingUser.addSong(Integer.valueOf(trainingSongs[i]));
				}
				
				this.trainingUsers.add(trainingUser);
				
				trainingUid++;
			}
			trainingBr.close();
			
			//逐行读取测试集用户数据
			BufferedReader testingBr = new BufferedReader(new FileReader(testingPath));
			String testingLine = null;
			int testingUid = 0;
			while((testingLine = testingBr.readLine()) != null){
				User testingUser = new User(testingUid,CONST.USER_TYPE.TEST);
				
				String[] testingSongs = testingLine.split("==>");
				int testingLen = testingSongs.length;
				
				//将最后一首歌曲作为目标歌曲
				testingUser.setLastSid(Integer.valueOf(testingSongs[testingLen-1]));
				
				//读取训练集用户的历史记录
				for(int i = 0; i < testingLen - 1; i++){
					testingUser.addSong(Integer.valueOf(testingSongs[i]));
				}
				
				this.testingUsers.add(testingUser);
				
				testingUid++;
			}
			testingBr.close();
			
			if(knnOrPopular){
				//构造用户相似度矩阵,行为测试用户，列为训练用户
				System.out.println("I am constructing userSimilarityMatrix......");
				int trainingSize = this.trainingUsers.size();
				int testingSize = this.testingUsers.size();
				
				this.userSimilarity = new float[testingSize][trainingSize];
				for(int i = 0; i < testingSize; i++){
					this.userSimilarity [i] = new float[trainingSize];
					User testingUser = this.testingUsers.get(i);
					for(int j = 0; j < trainingSize; j++){
						User trainingUser = this.trainingUsers.get(j);
						this.userSimilarity [i][j] = testingUser.cosinSimilarity(trainingUser);
					}
				}
				System.out.println("I have finished constructing userSimilarityMatrix......");
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		System.out.println("I am out loadUserFromFile......");
	}
	
	private void constructTrainingStatistics(){
		this.popularSongMap = new HashMap<Integer,Integer>();
		//统计歌曲收听次数
		for(int i = 0; i < this.trainingUsers.size(); i++){
			HashMap<Integer,Integer> countMap = this.trainingUsers.get(i).getCountMap();
			for(Integer key : countMap.keySet()){
				int count = countMap.get(key);
				if(!this.popularSongMap.containsKey(key)){
					this.popularSongMap.put(key, count);
				}else{
					int curCount = this.popularSongMap.get(key);
					curCount += count;
					this.popularSongMap.put(key, curCount);
				}
			}
		}
	}
	
	/**
	 * 构造用户收听歌曲的流行度模型
	 */
	private void constructPopularityModel(){
		
		System.out.println("I am in constructPopularityModel......");
		this.songsSortByPopularity = new ArrayList<Integer>();
		//按流行度排序
		List<Map.Entry<Integer, Integer>> popularList = new ArrayList<Map.Entry<Integer,Integer>>(this.popularSongMap.entrySet());
		Collections.sort(popularList, new Comparator<Map.Entry<Integer, Integer>>(){

			@Override
			public int compare(Entry<Integer, Integer> arg0,
					Entry<Integer, Integer> arg1) {
				// TODO Auto-generated method stub
				if(arg1.getValue() > arg0.getValue()){
					return 1;
				}else if(arg1.getValue() < arg0.getValue()){
					return -1;
				}else{
					return 0;
				}
			}

		});
		
		for(int i = 0; i < popularList.size(); i++){
			this.songsSortByPopularity.add(popularList.get(i).getKey());
		}
		
		System.out.println("共有歌曲"+this.songsSortByPopularity.size()+"首");
		
		System.out.println("I am out constructPopularityModel......");
	}
	
	
	/**
	 * 构造knn矩阵,行为测试用户，列为最近邻的N个训练集用户id
	 */
	private void constructKnnMatrix(){
		
		System.out.println("I am in constructKnnMatrix......");
		
		int testingSize = this.testingUsers.size();
		int trainingSize = this.trainingUsers.size();
		this.knnMatrix = new int[testingSize][CONST.KNN];
		
		//生成最近邻的K个用户id并存入knnMatrix
		for(int i = 0; i < testingSize; i++){
			HashMap<Integer,Float> similarityMap = new HashMap<Integer,Float>();
			for(int j = 0; j < trainingSize; j++){
				similarityMap.put(j, this.userSimilarity[i][j]);
			}
			List<Map.Entry<Integer, Float>> similarList = new ArrayList<Map.Entry<Integer,Float>>(similarityMap.entrySet());
			Collections.sort(similarList, new Comparator<Map.Entry<Integer, Float>>(){

				@Override
				public int compare(Entry<Integer, Float> o1,
						Entry<Integer, Float> o2) {
					// TODO Auto-generated method stub
					if(o2.getValue() > o1.getValue()){
						return 1;
					}else if(o2.getValue() < o1.getValue()){
						return -1;
					}else{
						return 0;
					}
				}
				
			});
			
			for(int k = 0; k < similarList.size() && k < CONST.KNN; k++){
				this.knnMatrix[i][k] = similarList.get(k).getKey();
			}
		}
		
		System.out.println("I am out constructKnnMatrix......");
	}
	
	/**
	 * 对某歌曲sid计算其在用户uid的最近邻下的评分
	 * @param uid
	 * @param sid
	 * @return
	 */
	private float KNNScore(int uid, int sid){
		float score = 0;
		for(int i = 0; i < CONST.KNN; i++){
			int nid = this.knnMatrix[uid][i];
			User nUser = this.trainingUsers.get(nid);
			HashMap<Integer,Integer> countMap = nUser.getCountMap();
			if(countMap.containsKey(sid)){
				int count = countMap.get(sid);
				score += (this.userSimilarity[uid][nid] * count); 
			}
		}
		return score;
	}
	
	/**
	 * 向用户推荐若干首歌曲
	 * @param uid 用户uid
	 * @return 用户可能喜欢的N首歌曲
	 */
	private ArrayList<Integer> recKNNSongsForUid(int uid){
		User curUser = this.testingUsers.get(uid);
		ArrayList<Integer> recentList = curUser.getRecentListenedSongs();
		ArrayList<Integer> recSongs = new ArrayList<Integer>();
		HashMap<Integer,Float> scoreMap = new HashMap<Integer,Float>();
		for(Integer sid : this.popularSongMap.keySet()){
			scoreMap.put(sid, KNNScore(uid,sid));
		}
		List<Map.Entry<Integer, Float>> scoreList = new ArrayList<Map.Entry<Integer,Float>>(scoreMap.entrySet());
		Collections.sort(scoreList, new Comparator<Map.Entry<Integer, Float>>(){

			@Override
			public int compare(Entry<Integer, Float> o1,
					Entry<Integer, Float> o2) {
				// TODO Auto-generated method stub
				if(o2.getValue() > o1.getValue()){
					return 1;
				}else if(o2.getValue() < o1.getValue()){
					return -1;
				}else{
					return 0;
				}
			}
		});
		
		for(int i = 0; i < scoreList.size(); i++){
			int sid = scoreList.get(i).getKey();
			if(recentList.contains(sid)){
				continue;
			}	
			if(recSongs.size() >= CONST.REC_ARR_LEN){
				break;
			}
			if(!recSongs.contains(sid)){
				recSongs.add(sid);
			}
		}	
		return recSongs;
	}
	/**
	 * 为用户生成最流行的N首歌曲
	 * @param uid  用户uid
	 * @param songCount  歌曲数目
	 * @return
	 */
	private ArrayList<Integer> recMostPopularSongsForUid(int uid){
		User curUser = this.testingUsers.get(uid);
		ArrayList<Integer> recentList = curUser.getRecentListenedSongs();
		ArrayList<Integer> recSongs = new ArrayList<Integer>();
		
		for(int i = 0; i < this.songsSortByPopularity.size(); i++){
			int sid = this.songsSortByPopularity.get(i);
			if(recentList.contains(sid)){
				continue;
			}	
			if(recSongs.size() >= CONST.REC_ARR_LEN){
				break;
			}
			if(!recSongs.contains(sid)){
				recSongs.add(sid);
			}
		}
		return recSongs;
	}
	
	

	/**
	 * 为用户uid返回基于LDA最相似的推荐列表
	 * @param uid 用户id
	 * @param songCount 推荐数目
	 * @return 针对uid的推荐列表
	 */
	private ArrayList<Integer> recMostSimilarSongsBasedLDAForUid(int uid, SongMatrix sMatrix){
		HashMap<Integer,Integer> idMap = sMatrix.getIdMap();
		float[][] songSimilarity = sMatrix.getSongSimilarity();
		ArrayList<Integer> uPlaylist = this.testingUsers.get(uid).getPlaylists();
		int targetSongIndex = idMap.get(uPlaylist.get(uPlaylist.size()-1));
		//将训练集中的歌曲与目标歌曲的相似度记录到simMap中
		HashMap<Integer,Float> simMap = new HashMap<Integer,Float>();
		for(Song song : sMatrix.getAllSong()){
			int sid = song.getSid();
			float sim = 0;
			int songIndex = idMap.get(sid);
			if(songIndex < targetSongIndex){
				sim = songSimilarity[targetSongIndex][songIndex];
			}else{
				sim = songSimilarity[songIndex][targetSongIndex];
			}
			if(!simMap.containsKey(sid)){
				simMap.put(sid, sim);
			}else{
				try{
					throw new LKAException("simMap 包含重复歌曲");
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		//按照sim由大到小排序
		List<Map.Entry<Integer, Float>> simList = new ArrayList<Map.Entry<Integer,Float>>(simMap.entrySet());
		Collections.sort(simList, new Comparator<Map.Entry<Integer, Float>>(){

			@Override
			public int compare(Entry<Integer, Float> o1,
					Entry<Integer, Float> o2) {
				// TODO Auto-generated method stub
				if(o2.getValue() > o1.getValue()){
					return 1;
				}else if(o2.getValue() < o1.getValue()){
					return -1;
				}else{
					return 0;
				}
			}
		});
		
		//生成最终列表
		ArrayList<Integer> recSongs = new ArrayList<Integer>();
		ArrayList<Integer> recentList = this.testingUsers.get(uid).getRecentListenedSongs();
		for(int i = 0; i < simList.size(); i++){
			int sid = simList.get(i).getKey();
			if(recentList.contains(sid)){
				continue;
			}	
			if(recSongs.size() >= CONST.REC_ARR_LEN){
				break;
			}
			if(!recSongs.contains(sid)){
				recSongs.add(sid);
			}
		}
		
		return recSongs;
	}
	/**
	 * 为用户uid返回下一首歌曲的各主题分布
	 * @param uid  用户id
	 * @param sMatrix  歌曲矩阵
	 * @return 下一首歌曲的主题分布
	 */
	private HashMap<Integer,Float> nextSongTopicProForUid(int uid, SongMatrix sMatrix, RConnection rc){
		HashMap<Integer,Float> next = new HashMap<Integer,Float>();//下一首歌曲的主题分布
		HashMap<Integer,Integer> idMap = sMatrix.getIdMap();
		ArrayList<Song> allSongs = sMatrix.getAllSong();
		User testingUser = this.testingUsers.get(uid);
		ArrayList<Integer> playlist = testingUser.getPlaylists();
		int size = playlist.size();
		float[][] topicTable = new float[size][CONST.TOPIC_NUM];//歌曲-主题概率分布表
		for(int i = 0; i < size; i++){
			int sid = playlist.get(i);
			int sIndex = idMap.get(sid);
			Song song = allSongs.get(sIndex);
			HashMap<Integer,Float> topicMap = song.getTopicMap();
			for(int j = 0; j < CONST.TOPIC_NUM; j++){
				topicTable[i][j] = topicMap.get(j);
			}
		}
		
		try{
		
			//arima预测
			for(int i = 0; i < CONST.TOPIC_NUM; i++){
				double[] topicSeries = new double[size];
				for(int j = 0; j < size; j++){
					topicSeries[j] = topicTable[j][i];
				}
				rc.assign("x", topicSeries);
				rc.voidEval("xseries <- ts(x)");
				rc.voidEval("fit<-auto.arima(xseries,trace=T)");
				RList rl = rc.eval("xfore<-forecast(fit,h=1,fan=T)").asList();
				
				double[] result = rl.at("mean").asDoubles();
				
				if(!next.containsKey(i)){
					next.put(i, (float)result[0]);
				}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return next;
	}
	
	/**
	 * 为用户uid返回基于LDA_ARIMA的推荐列表
	 * @param uid 用户id
	 * @param songCount 推荐数目
	 * @return 针对uid的推荐列表
	 */
	private ArrayList<Integer> recSongsBasedLDAARIMAForUid(int uid, SongMatrix sMatrix, RConnection rc){
		ArrayList<Integer> recSongs = new ArrayList<Integer>();
		
		ArrayList<Song> allSongs = sMatrix.getAllSong();
		
		HashMap<Integer,Float> next = nextSongTopicProForUid(uid,sMatrix,rc);
		Song tarSong = new Song(-1,next);
		HashMap<Integer,Float> simMap = new HashMap<Integer,Float>();
		for(Song song : allSongs){
			int sid = song.getSid();
			if(!simMap.containsKey(sid)){
				simMap.put(sid, tarSong.cosineSimilarity(song));
			}
		}
		
		//按照sim由大到小排序
		List<Map.Entry<Integer, Float>> simList = new ArrayList<Map.Entry<Integer,Float>>(simMap.entrySet());
		Collections.sort(simList, new Comparator<Map.Entry<Integer, Float>>(){

			@Override
			public int compare(Entry<Integer, Float> o1,
					Entry<Integer, Float> o2) {
				// TODO Auto-generated method stub
				if(o2.getValue() > o1.getValue()){
					return 1;
				}else if(o2.getValue() < o1.getValue()){
					return -1;
				}else{
					return 0;
				}
			}
		});
				
		//生成最终列表
		ArrayList<Integer> recentList = this.testingUsers.get(uid).getRecentListenedSongs();
		for(int i = 0; i < simList.size(); i++){
			int sid = simList.get(i).getKey();
			if(recentList.contains(sid)){
				continue;
			}	
			if(recSongs.size() >= CONST.REC_ARR_LEN){
				break;
			}
			if(!recSongs.contains(sid)){
				recSongs.add(sid);
			}
		}
				
		return recSongs;
	}
	
	/**
	 * 为用户uid返回基于SIMILAR_ARIMA的推荐列表
	 * @param uid 用户id
	 * @return 针对uid的推荐列表
	 */
	private ArrayList<Integer> recSongsBasedOnSimilarArimaForUid(int uid, SongMatrix sMatrix, RConnection rc){
		HashMap<Integer,Integer> idMap = sMatrix.getIdMap();
		float[][] simTable = sMatrix.getSongSimilarity();
		//生成相似度序列
		ArrayList<Integer> playlist = this.testingUsers.get(uid).getPlaylists();
		int pSize = playlist.size();
		double[] simSeries = new double[pSize-1];
		for(int i = 1; i < pSize; i++){
			int nextSid = playlist.get(i);
			int nextIndex = idMap.get(nextSid);
			int preSid = playlist.get(i-1);
			int preIndex = idMap.get(preSid);
			if(nextIndex > preIndex){
				simSeries[i-1] = simTable[nextIndex][preIndex];
			}else{
				simSeries[i-1] = simTable[preIndex][nextIndex];
			}
		}
		float nextSim = LKAUtil.predictNextSimilarityByArima(simSeries, rc);
		
		//记录训练集各歌曲与目标歌曲的相似度sim与预测相似度nextSim的差距
		int targetSid = playlist.get(pSize-1);
		int targetIndex = idMap.get(targetSid);
		
		HashMap<Integer,Float> deltaMap = new HashMap<Integer,Float>();
		
		for(Song song : sMatrix.getAllSong()){
			int curSid = song.getSid();
			int curIndex = idMap.get(curSid);
			float delta = 0;
			if(curIndex > targetIndex){
				delta = simTable[curIndex][targetIndex];
			}else{
				delta = simTable[targetIndex][curIndex];
			}
			delta = Math.abs(delta-nextSim);
			if(!deltaMap.containsKey(curSid)){
				deltaMap.put(curSid, delta);
			}
		}
		
		//按照delta由小到大排序
		List<Map.Entry<Integer, Float>> deltaList = new ArrayList<Map.Entry<Integer,Float>>(deltaMap.entrySet());
		Collections.sort(deltaList, new Comparator<Map.Entry<Integer, Float>>(){

			@Override
			public int compare(Entry<Integer, Float> o1,
					Entry<Integer, Float> o2) {
				// TODO Auto-generated method stub
				if(o2.getValue() < o1.getValue()){
					return 1;
				}else if(o2.getValue() > o1.getValue()){
					return -1;
				}else{
					return 0;
				}
			}
		});
				
		//生成最终列表
		ArrayList<Integer> recSongs = new ArrayList<Integer>();
		ArrayList<Integer> recentList = this.testingUsers.get(uid).getRecentListenedSongs();
		for(int i = 0; i < deltaList.size(); i++){
			int sid = deltaList.get(i).getKey();
			if(recentList.contains(sid)){
				continue;
			}	
			if(recSongs.size() >= CONST.REC_ARR_LEN){
				break;
			}
			if(!recSongs.contains(sid)){
				recSongs.add(sid);
			}
		}
		
		return recSongs;
	}
	
	private ArrayList<Integer> recSongsBasedBaselineModelForUid(int uid, SongMatrix sMatrix, RConnection rc,HashMap<Integer,Float>disMap){
		ArrayList<Integer> playlist = this.testingUsers.get(uid).getPlaylists();
		int pSize = playlist.size();
		double[] disSeries = new double[pSize];
		for(int i = 0; i < pSize; i++){
			int sid = playlist.get(i);
			disSeries[i] = disMap.get(sid);
		}
		float nextDis = LKAUtil.predictNextSimilarityByArima(disSeries, rc);
		
		HashMap<Integer,Float> deltaMap = new HashMap<Integer,Float>();
		
		for(Song song : sMatrix.getAllSong()){
			int curSid = song.getSid();
			float delta = Math.abs(disMap.get(curSid)-nextDis);
			deltaMap.put(curSid, delta);
		}
		
		//按照delta由小到大排序
		List<Map.Entry<Integer, Float>> deltaList = new ArrayList<Map.Entry<Integer,Float>>(deltaMap.entrySet());
		Collections.sort(deltaList, new Comparator<Map.Entry<Integer, Float>>(){

			@Override
			public int compare(Entry<Integer, Float> o1,
					Entry<Integer, Float> o2) {
				// TODO Auto-generated method stub
				if(o2.getValue() < o1.getValue()){
					return 1;
				}else if(o2.getValue() > o1.getValue()){
					return -1;
				}else{
					return 0;
				}
			}
		});
				
		//生成最终列表
		ArrayList<Integer> recSongs = new ArrayList<Integer>();
		ArrayList<Integer> recentList = this.testingUsers.get(uid).getRecentListenedSongs();
		for(int i = 0; i < deltaList.size(); i++){
			int sid = deltaList.get(i).getKey();
			if(recentList.contains(sid)){
				continue;
			}	
			if(recSongs.size() >= CONST.REC_ARR_LEN){
				break;
			}
			if(!recSongs.contains(sid)){
				recSongs.add(sid);
			}
		}
		
		return recSongs;
	}
	
	/**
	 * 为用户生成基于LSA最近邻的推荐列表
	 * @param uid 用户id
	 * @param docToIndex 文档索引表
	 * @param lsa LSA空间
	 * @return
	 */
	public ArrayList<Integer> recSongsBasedLSAForUid(int uid, HashMap<Integer,Integer> docToIndex, LatentSemanticAnalysis lsa, SongMatrix sMatrix){
		ArrayList<Integer> recSongs = new ArrayList<Integer>();
		try{
			ArrayList<Integer> playlist = this.testingUsers.get(uid).getPlaylists();
			int pSize = playlist.size();
			int targetSid = playlist.get(pSize-1);
			HashMap<Integer,Float> simMap = new HashMap<Integer,Float>();
			DoubleVector targetDocument=lsa.getDocumentVector(docToIndex.get(targetSid));
			for(Song song : sMatrix.getAllSong()){
				DoubleVector doc = lsa.getDocumentVector(docToIndex.get(song.getSid()));
				float sim = (float) Similarity.getSimilarity(Similarity.SimType.COSINE,
	                    doc,
	                   targetDocument);
				simMap.put(song.getSid(), sim);
			}
			
			//按相似度由大到小排序
			List<Map.Entry<Integer, Float>> simList =
				    new ArrayList<Map.Entry<Integer, Float>>(simMap.entrySet());
			Collections.sort(simList, new Comparator<Map.Entry<Integer, Float>>() {   
			    public int compare(Map.Entry<Integer, Float> arg0, Map.Entry<Integer, Float> arg1) {      
			    	if(arg0.getValue() < arg1.getValue()){
			    		return 1;
			    	}
			    	if(arg0.getValue() > arg1.getValue()){
			    		return -1;
			    	}
			    	return 0;
			    }
			}); 
			
			//生成最终列表
			ArrayList<Integer> recentList = this.testingUsers.get(uid).getRecentListenedSongs();
			for(int i = 0; i < simList.size(); i++){
				int sid = simList.get(i).getKey();
				if(recentList.contains(sid)){
					continue;
				}	
				if(recSongs.size() >= CONST.REC_ARR_LEN){
					break;
				}
				if(!recSongs.contains(sid)){
					recSongs.add(sid);
				}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return recSongs;
	}
	
	public ArrayList<Integer> recSongsBasedUserKNNwithContextForUid(int uid, SongMatrix sMatrix, ArrayList<TopicSequence> allSeqs){
		HashMap<Integer,Integer> idMap = sMatrix.getIdMap();
		ArrayList<Song> songs = sMatrix.getAllSong();
		
		User curUser = this.testingUsers.get(uid);
		ArrayList<Integer> playlist = curUser.getPlaylists();
		
		ArrayList<ArrayList<Integer>> prefix = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> last = new ArrayList<Integer>();
		//生成目标序列
		for(int index = playlist.size() - CONST.MAX_SUB_SEQ; index < playlist.size() - 1; index++){
			ArrayList<Integer> prefixArr = new ArrayList<Integer>();
			ArrayList<Integer> highTopics = songs.get(idMap.get(playlist.get(index))).getHighTopics();
			for(int i = 0; i < highTopics.size(); i++){
				prefixArr.add(highTopics.get(i));
			}
			prefix.add(prefixArr);
		}
		
		ArrayList<Integer> lastHighTopics = songs.get(idMap.get(playlist.get(playlist.size()-1))).getHighTopics();
		for(int i = 0; i < lastHighTopics.size(); i++){
			last.add(lastHighTopics.get(i));
		}
		//生成预测主题列表
		TopicSequence targetSequence = new TopicSequence(prefix,last,CONST.MAX_SUB_SEQ,-1);
		ArrayList<Integer> predictTopics = TopicSequence.predcitTopics(targetSequence, allSeqs);
		
		ArrayList<Integer> recentList = curUser.getRecentListenedSongs();
		ArrayList<Integer> recSongs = new ArrayList<Integer>();
		//计算各个待选歌曲的评分并记入到map中
		HashMap<Integer,Float> scoreMap = new HashMap<Integer,Float>();
		for(Integer sid : this.popularSongMap.keySet()){
			Song song = songs.get(idMap.get(sid));
			float score = (float) ((KNNScore(uid,sid) + 0.1) * (song.contextScore(predictTopics) + 0.1));
			//float score = song.contextScore(predictTopics);
			scoreMap.put(sid, score);
		}
		List<Map.Entry<Integer, Float>> scoreList = new ArrayList<Map.Entry<Integer,Float>>(scoreMap.entrySet());
		Collections.sort(scoreList, new Comparator<Map.Entry<Integer, Float>>(){

			@Override
			public int compare(Entry<Integer, Float> o1,
					Entry<Integer, Float> o2) {
				// TODO Auto-generated method stub
				if(o2.getValue() > o1.getValue()){
					return 1;
				}else if(o2.getValue() < o1.getValue()){
					return -1;
				}else{
					return 0;
				}
			}
		});
		
		for(int i = 0; i < scoreList.size(); i++){
			int sid = scoreList.get(i).getKey();
			if(recentList.contains(sid)){
				continue;
			}	
			if(recSongs.size() >= CONST.REC_ARR_LEN){
				break;
			}
			if(!recSongs.contains(sid)){
				recSongs.add(sid);
			}
		}	
		return recSongs;
	}
	
	public ArrayList<Integer> recSongsBasedUnifiedModelForUid(int uid, SongMatrix sMatrix){
		ArrayList<Integer> recSongs = new ArrayList<Integer>();
		
		HashMap<Integer,Integer> idMap = sMatrix.getIdMap();
		ArrayList<Song> songs = sMatrix.getAllSong();
		
		User curUser = this.testingUsers.get(uid);
		ArrayList<Integer> playlist = curUser.getPlaylists();
		
		HashMap<Integer, Float> userTopicMap = new HashMap<Integer, Float>();
		
		int sum = 0;
		for(int i = 0; i < playlist.size(); i++){
			sum += i;
			int sid = playlist.get(i);
			int sIndex = idMap.get(sid);
			Song s = songs.get(sIndex);
			HashMap<Integer, Float> topicMap = s.getTopicMap();
			for(Integer key : topicMap.keySet()){
				if(!userTopicMap.containsKey(key)){
					userTopicMap.put(key, topicMap.get(key)*i);
				}else{
					float pro = userTopicMap.get(key);
					userTopicMap.put(key, topicMap.get(key)+pro*i);
				}
			}
		}
		
		for(Integer key : userTopicMap.keySet()){
			float pro = userTopicMap.get(key);
			pro /= sum;
			userTopicMap.put(key, pro);
		}
		
		//计算各个待选歌曲的评分并记入到map中
		HashMap<Integer,Float> scoreMap = new HashMap<Integer,Float>();
		Song userSong = new Song(-1,userTopicMap);
		for(Integer sid : this.popularSongMap.keySet()){
			Song song = songs.get(idMap.get(sid));
			float score = song.cosineSimilarity(userSong);
			scoreMap.put(sid, score);
		}
		List<Map.Entry<Integer, Float>> scoreList = new ArrayList<Map.Entry<Integer,Float>>(scoreMap.entrySet());
		Collections.sort(scoreList, new Comparator<Map.Entry<Integer, Float>>(){
			@Override
			public int compare(Entry<Integer, Float> o1,
					Entry<Integer, Float> o2) {
				// TODO Auto-generated method stub
				if(o2.getValue() > o1.getValue()){
					return 1;
				}else if(o2.getValue() < o1.getValue()){
					return -1;
				}else{
					return 0;
				}
			}
		});
		
		for(int i = 0; i < scoreList.size(); i++){
			int sid = scoreList.get(i).getKey();	
			if(recSongs.size() >= CONST.REC_ARR_LEN){
				break;
			}
			if(!recSongs.contains(sid)){
				recSongs.add(sid);
			}
		}
		
		return recSongs;
		
	}
	public void saveRecSongToFile(int part, CONST.RECALG_TYPE type, SongMatrix sMatrix, RConnection rc,HashMap<Integer,Integer> docToIndex, LatentSemanticAnalysis lsa,HashMap<Integer,Float>disMap){
		try{
			int len = this.testingUsers.size();
			
			ArrayList<TopicSequence> allSeqs = null;
			if(type == CONST.RECALG_TYPE.USERKNN_CONTEXT){
				allSeqs = TopicSequence.getFrequentPatternSequence(CONST.BASE_RESULT_PATH+"topic/patterns"+part+".txt");
			}
			
			
			for(int i = 0; i < len; i++){
				PrintWriter pw = new PrintWriter(new File("rec/"+type.toString()+part+"_"+i+".txt"));
				System.out.println("testingUser"+i+"......");
				ArrayList<Integer> recSongs = null;
				switch(type){
				case MOST_POPULAR:
					recSongs = recMostPopularSongsForUid(i);
					break;
				case UserKNN:
					recSongs = recKNNSongsForUid(i);
					break;
				case LDA_SIMILAR:
					recSongs = recMostSimilarSongsBasedLDAForUid(i,sMatrix);
					break;
				case SIMILAR_ARIMA:
					recSongs = recSongsBasedOnSimilarArimaForUid(i,sMatrix,rc);
					break;
				case LDA_ARIMA:
					recSongs = recSongsBasedLDAARIMAForUid(i,sMatrix,rc);
					break;
				case LSA_SIMILAR:
					recSongs = recSongsBasedLSAForUid(i,docToIndex,lsa,sMatrix);
					break;
				case USERKNN_CONTEXT:
					recSongs = recSongsBasedUserKNNwithContextForUid(i,sMatrix,allSeqs);
				case UNIFIED:
					recSongs = recSongsBasedUnifiedModelForUid(i,sMatrix);
				case BASELINE:
					recSongs = recSongsBasedBaselineModelForUid(i,sMatrix,rc,disMap);
				default:
					break;
				}
				for(int j = 0; j < recSongs.size(); j++){
					pw.println(recSongs.get(j));
				}
				pw.flush();
				pw.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * 获取Most Popular的若干评测标准，包括命中率hitRatio、召回率recall、准确度precision、覆盖率coverage
	 * @param recCount 推荐数目
	 * @return  命中率hitRatio、召回率recall、准确度precision、覆盖率coverage组成的数组
	 */
	public float[][] validateAlg(int part, CONST.RECALG_TYPE type){
		
		System.out.println("I am in validateAlg......");
		
		float[][] result = new float[CONST.REC_N][4];
		
		int len = this.testingUsers.size();
		int songSize = this.popularSongMap.size();
		HashMap<Integer,ArrayList<Integer>> recMap = new HashMap<Integer,ArrayList<Integer>>();
		try{
			for(int i = 0; i < len; i++){
				BufferedReader br = new BufferedReader(new FileReader("rec/"+type.toString()+part+"_"+i+".txt"));
				String line = null;
				ArrayList<Integer> rSongs = new ArrayList<Integer>();
				while((line = br.readLine()) != null){
					rSongs.add(Integer.valueOf(line));
				}
				br.close();
				recMap.put(i, rSongs);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		
		for(int step = 0; step < CONST.REC_N; step++){
			int recCount = CONST.TOPN_MIN+CONST.TOPN_STEP*step;
			int hit = 0;
			ArrayList<Integer> recs = new ArrayList<Integer>();
			for(int i = 0; i < len; i++){
				ArrayList<Integer> totalRecs = recMap.get(i);
				User testingUser = this.testingUsers.get(i);
				int  lastSid = testingUser.getLastSid();
				ArrayList<Integer> recSongs = new ArrayList<Integer>();
				for(int index = 0; index < recCount; index++){
					recSongs.add(totalRecs.get(index));
				}
				recs.removeAll(recSongs);
				recs.addAll(recSongs);
				if(recSongs.contains(lastSid)){
					hit++;
				}
			}
			result[step][0] = (float) ((hit*1.0) / (len*1.0));
			result[step][1] = (float) ((hit*1.0) / (len*1.0));
			result[step][2] = (float) ((hit*1.0) / (len*recCount));
			result[step][3] = (float) ((recs.size() * 1.0) / (songSize * 1.0));
		}
		
		System.out.println("I am out validateAlg......");
		
		return result;
	}
	
	/**
	 * 获取训练集用户
	 * @return  训练集用户
	 */
	public ArrayList<User> getTrainingUsers() {
		return trainingUsers;
	}

	/**
	 * 获取测试集用户
	 * @return  测试集用户
	 */
	public ArrayList<User> getTestingUsers() {
		return testingUsers;
	}

	/**
	 * 获取训练集用户数目
	 * @return  训练集用户数目
	 */
	public int getTrainingSize(){
		return this.trainingUsers.size();
	}
	
	/**
	 * 获取测试集用户数目
	 * @return  测试集用户数目
	 */
	public int getTestingSize(){
		return this.testingUsers.size();
	}
	
	/**
	 * 获取用户相似度矩阵
	 * @return
	 */
	public float[][] getUserSimilarity() {
		return userSimilarity;
	}

	/**
	 * 获取歌曲收听次数统计
	 * @return
	 */
	public HashMap<Integer,Integer> getPopularSongMap(){
		return this.popularSongMap;
	}
	
	/**
	 * 获取按流行度排列的歌曲列表
	 * @return
	 */
	public ArrayList<Integer> getSongsSortByPopularity() {
		return this.songsSortByPopularity;
	}
}
