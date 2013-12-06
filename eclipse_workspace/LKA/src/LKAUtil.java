import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;

import edu.ucla.sspace.lsa.LatentSemanticAnalysis;

public class LKAUtil {
	public static float cosSim(float[] from, float[] to){
		float sim = 0;
		float fenzi = 0;
		float fenmu1 = 0;
		float fenmu2 = 0;
		for(int i = 0; i < from.length; i++){
			fenzi += (from[i]*to[i]);
			fenmu1 += (from[i]*from[i]);
			fenmu2 += (to[i]*to[i]);
		}
		fenmu1 = (float) Math.sqrt(fenmu1);
		fenmu2 = (float) Math.sqrt(fenmu2);
		sim = fenzi / (fenmu1*fenmu2);
		return sim;
	}
	
	public static float EuclidDis(float[] from, float[] to){
		float dis = 0;
		for(int i = 0; i < from.length; i++){
			dis += (to[i] - from[i]) * (to[i] - from[i]);
		}
		dis = (float) Math.sqrt(dis);
		return dis;
	}
	
	public static float KLDis(float[] from, float[] to){
		float dis = 0;
		for(int i = 0; i < from.length; i++){
			dis += from[i]*Math.log(from[i]/to[i]);
		}
		return dis;
	}
	
	public static float predictNextSimilarityByArima(double[] simSeries, RConnection rc){
		float nextSim = 0;
		try{
			if(simSeries.length >= 50){
				for(int i = 0; i < simSeries.length; i++){
					System.out.print(simSeries[i]+",");
				}
				System.out.println();
			}
			//arima预测
			rc.assign("sim", simSeries);
			rc.voidEval("xseries <- ts(sim)");
			rc.voidEval("fit<-auto.arima(xseries,trace=T)");
			RList rl = rc.eval("xfore<-forecast(fit,h=1,fan=T)").asList();
			
			double[] result = rl.at("mean").asDoubles();
			
			nextSim = (float) result[0];
		}catch(Exception e){
			e.printStackTrace();
		}
		return nextSim;
	}
	
	/**
	 * 将推荐的歌曲保存到文件
	 */
	public static void saveRecSongsToFile(CONST.RECALG_TYPE type, SongMatrix sMatrix, RConnection rc,HashMap<Integer,Integer> docToIndex, LatentSemanticAnalysis lsa){
		System.out.println("I am in saveRecSongsToFile......");
		boolean knnOrPopular = false;
		if(type == CONST.RECALG_TYPE.MOST_POPULAR || type == CONST.RECALG_TYPE.UserKNN || type == CONST.RECALG_TYPE.USERKNN_CONTEXT){
			knnOrPopular = true;
		}
		HashMap<Integer,Float> disMap = new HashMap<Integer,Float>();
		if(type == CONST.RECALG_TYPE.BASELINE){
			try{
				BufferedReader br = new BufferedReader(new FileReader(new File("cosineMap.txt")));
				String line = null;
				while((line = br.readLine()) != null){
					String[] items = line.split("\t");
					disMap.put(Integer.valueOf(items[0]), Float.valueOf(items[1]));
				}
				br.close();
			}catch(Exception e){
				System.out.println(e.getMessage());
			}
		}
		for(int part = 0; part < CONST.FOLDER_NUM; part++){
			System.out.println("I am in "+part+"......");
			String trainingPlaylistPath = CONST.BASE_PATH+part+"/trainingSet/playlist.txt";
			String testingPlaylistPath = CONST.BASE_PATH+part+"/testingSet/playlist.txt";
			UserMatrix uMatrix = new UserMatrix(trainingPlaylistPath,testingPlaylistPath,knnOrPopular);
			uMatrix.saveRecSongToFile(part, type, sMatrix, rc,docToIndex,lsa,disMap);
		}
		System.out.println("I am out saveRecSongsToFile......");
	}
	
	/**
	 * 测试不同的算法，并保存预测结果
	 * @param part  测试的组数
	 * @param type  测试的算法类型
	 * @param trainingSongPath  训练集歌曲路径
	 * @param testingSongPath   测试集歌曲路径
	 * @param trainingPlaylistPath  训练集用户路径
	 * @param testingPlaylistPath   测试集用户路径
	 * @param topicNum    主题数目
	 */
	public static void resultOfRecAlg(CONST.RECALG_TYPE type){
		System.out.println("I am in resultOfRecAlg......");
		try{
			for(int part = 0; part < CONST.FOLDER_NUM; part++){
				System.out.println("I am in "+part+"......");
				String trainingPlaylistPath = CONST.BASE_PATH+part+"/trainingSet/playlist.txt";
				String testingPlaylistPath = CONST.BASE_PATH+part+"/testingSet/playlist.txt";
				UserMatrix uMatrix = new UserMatrix(trainingPlaylistPath,testingPlaylistPath,true);
				PrintWriter pw = new PrintWriter(new File(CONST.BASE_RESULT_PATH+type.toString()+part+".txt"));
				float[][] result = uMatrix.validateAlg(part, type);
				for(int i = 0; i < CONST.REC_N; i++){
					int topN = CONST.TOPN_MIN + CONST.TOPN_STEP * i;
					pw.println(topN+"\t"+result[i][0]+"\t"+result[i][1]+"\t"+result[i][2]+"\t"+result[i][3]);
				}
				pw.flush();
				pw.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		System.out.println("I am out resultOfRecAlg......");
	}
	public static void generateTopicSequenceForPrefixSpan(){
		String trainingSongPath = CONST.BASE_PATH+"result/lda_100.theta";
		SongMatrix sMatrix = new SongMatrix(trainingSongPath,false);
		for(int part = 0; part < CONST.FOLDER_NUM; part++){
			String trainingPlaylistPath = CONST.BASE_PATH+part+"/trainingSet/playlist.txt";
			sMatrix.generateTopicSequence(trainingPlaylistPath, part);
		}
	}
	
	public static void generateLSAModel(HashMap<Integer,Integer> docToIndex,LatentSemanticAnalysis lsa){
		try{
			String dirPath = CONST.BASE_PATH+"song/";
			
			ArrayList<String> allFile = FileManager.searchDir(dirPath);
			System.out.println("I find "+allFile.size()+" files");
			ArrayList<String> documents = new ArrayList<String>();
			System.out.println("Begin of loading documents' content......");
			int index = 0;
			for(String file : allFile){
				try{
					BufferedReader br = new BufferedReader(new FileReader(file));
					String line = null;
					String content = "";
					while((line = br.readLine()) != null){
						content = content+" "+line;
					}
					String idStr = file.substring(file.lastIndexOf("/")+1);
					documents.add(content);
					docToIndex.put(Integer.valueOf(idStr), index);
					index++;
					br.close();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			System.out.println("End of loading documents' content......");
			
			for(String document:documents){
				lsa.processDocument(new BufferedReader(new StringReader(document)));
			}
			lsa.processSpace(System.getProperties());
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void generateAverageResult(CONST.RECALG_TYPE type){
		float[][] average = new float[CONST.REC_N][4];
		for(int i = 0; i < CONST.REC_N; i++){
			for(int j = 0; j < 4; j++){
				average[i][j] = 0;
			}
		}
		for(int i = 0; i < CONST.FOLDER_NUM; i++){
			try{
				BufferedReader br = new BufferedReader(new FileReader(CONST.BASE_RESULT_PATH+type.toString()+i+".txt"));
				String line = null;
				int step = 0;
				while((line = br.readLine()) != null){
					String[] eles = line.split("\t");
					for(int j = 0; j < 4; j++){
						average[step][j] += Float.valueOf(eles[j+1]);
					}
					step++;
				}
				br.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		try{
			PrintWriter pw = new PrintWriter(CONST.BASE_RESULT_PATH+type.toString()+".txt");
			for(int i = 0; i < CONST.REC_N; i++){
				int recCount = CONST.TOPN_MIN+CONST.TOPN_STEP*i;
				for(int j = 0; j < 4; j++){
					average[i][j] /= CONST.FOLDER_NUM;
				}
				pw.println(recCount+"\t"+average[i][0]+"\t"+average[i][1]+"\t"+average[i][2]+"\t"+average[i][3]);
			}
			pw.flush();
			pw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
