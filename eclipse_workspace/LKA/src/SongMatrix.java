import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class SongMatrix {
	private ArrayList<Song> songs;//所有歌曲
	private HashMap<Integer,Integer> idMap;//歌曲id与相似度矩阵映射
	private float[][] songSimilarity;//相似度矩阵
	
	/**
	 * 构造函数
	 * @param path 歌曲-主题文件路径
	 * @param shouldCalDisMatrix 是否需要计算相似度矩阵
	 */
	public SongMatrix(String path, boolean shouldCalDisMatrix){
		this.songs = new ArrayList<Song>();
		this.idMap = new HashMap<Integer,Integer>();
		loadSongsFromFile(path);
		if(shouldCalDisMatrix){
			constructSongSimilarityMatrix();
		}
	}
	
	/**
	 * 从歌曲-主题文件中读取歌曲信息
	 * @param path 歌曲-主题文件路径
	 */
	private void loadSongsFromFile(String path){
		System.out.println("I am in loadSongsFromFile......");
		this.songs.clear();
		this.idMap.clear();
		try{
			BufferedReader br = new BufferedReader(new FileReader(path));
//			PrintWriter pw = new PrintWriter(new File("cosineMap.txt"));
//			HashMap<Integer,Float> baseMap = new HashMap<Integer,Float>();
//			float basePro = (float) (1.0 / CONST.TOPIC_NUM);
//			for(int i = 0; i < CONST.TOPIC_NUM; i++){
//			  baseMap.put(i, basePro);
//			}
//			Song baseSong = new Song(0,baseMap);
			String line = null;
			int index = 0;
			//逐行读取歌曲与主题分布
			while((line = br.readLine()) != null){
				if(line.startsWith("#doc")){
					continue;
				}
				String[] eles = line.split("\t");
				int pos = eles[1].lastIndexOf("/");
				int sid = Integer.valueOf(eles[1].substring(pos+1));
				
				//歌曲对应的主题及其概率
				HashMap<Integer,Float> topicMap = new HashMap<Integer,Float>();
				//歌曲显著主题
				ArrayList<Integer> highTopics = new ArrayList<Integer>();
				int firstTopicId = -1;
				//读入所有主题
				for(int i = 2; i < eles.length; i += 2){
					int topicId = Integer.valueOf(eles[i]);
					float topicPro = Float.valueOf(eles[i+1]);
					
					//记录歌曲的第一个主题，当歌曲无明显主题是顶上
					if(firstTopicId == -1){
						firstTopicId = topicId;
					}
					
					if(topicPro >= CONST.THREASHOLD){
						highTopics.add(topicId);
					}
					
					if(!topicMap.containsKey(topicId)){
						topicMap.put(topicId, topicPro);
					}
				}
				
				Song song = new Song(sid,topicMap);
//				pw.println(sid+"\t"+song.cosineSimilarity(baseSong));
				
				if(highTopics.size() == 0){
					highTopics.add(firstTopicId);
				}
				song.setHighTopic(highTopics);
				
				if(!songs.contains(song)){
					songs.add(song);
					idMap.put(sid, index);
				}else{
					try{
						throw new LKAException("歌曲-主题文件中存在重复歌曲");
					}catch(Exception ee){
						ee.printStackTrace();
					}
				}
				index++;
			}
			br.close();
//			pw.flush();
//			pw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		System.out.println("SongMatrix has finish loading "+songs.size()+" songs......");
		System.out.println("I am out loadSongsFromFile......");
	}
	
	/**
	 * 返回歌曲的相似度矩阵
	 * @return 歌曲相似度矩阵
	 */
	public float[][] getSongSimilarity(){
		return this.songSimilarity;
	}
	
	/**
	 * 获取所有歌曲列表
	 * @return 所有歌曲
	 */
	public ArrayList<Song> getAllSong(){
		return this.songs;
	}
	
	/**
	 * 将训练集历史记录中的数据转行为可供处理的格式"1 2 -1 2 3 -1 3 2 -1 12 -2"
	 * @param playlist 训练集数据
	 * @param part  测试集编号
	 */
	public void generateTopicSequence(String playlist, int part){
		try{
			
			PrintWriter pw = new PrintWriter(new File(CONST.BASE_RESULT_PATH+"topic/topic"+part+".txt"));
			
			BufferedReader br = new BufferedReader(new FileReader(playlist));
			String line = null;
			while((line = br.readLine()) != null){
				String[] songStr = line.split("==>");
				for(int i = 0; i < songStr.length; i++){
					ArrayList<Integer> highTopics = this.songs.get(this.idMap.get(Integer.valueOf(songStr[i]))).getHighTopics();
					for(int j = 0; j < highTopics.size(); j++){
						pw.print(highTopics.get(j)+" ");
					}
					if(i < songStr.length -1){
						pw.print("-1 ");
					}else{
						pw.print("-1 -2\n");
					}
				}
			}
			br.close();
			pw.flush();
			pw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 返回歌曲sid和index的对应关系
	 * @return 歌曲sid和index的对应关系
	 */
	public HashMap<Integer,Integer> getIdMap(){
		return idMap;
	}
	
	/**
	 * 构造歌曲之间的相似度矩阵
	 */
	private void constructSongSimilarityMatrix(){
		System.out.println("I am in constructSongSimilarityMatrix......");
		//初始化歌曲之间的距离
		int size = this.songs.size();
		this.songSimilarity = new float[size][];
		for(int i = 0; i < size; i++){
			this.songSimilarity[i] = new float[i+1];
			Song curSong = this.songs.get(i);
			for(int j = 0; j < this.songSimilarity[i].length; j++){
				Song anoSong = this.songs.get(j);
				this.songSimilarity[i][j] = curSong.cosineSimilarity(anoSong);
			}
		}
		
		System.out.println("I am out constructSongSimilarityMatrix......");
	}
	
	public int count(){
		return songs.size();
	}
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("===========================================================\n");
		sb.append("Here are"+songs.size()+" songs\n");
		sb.append("===========================================================\n");
		for(Song song: songs){
			sb.append(song.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
}
