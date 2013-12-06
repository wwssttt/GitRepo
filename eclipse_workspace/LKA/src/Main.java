import java.io.IOException;
import java.util.HashMap;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.lsa.LatentSemanticAnalysis;
import edu.ucla.sspace.matrix.LogEntropyTransform;
import edu.ucla.sspace.matrix.SVD;


public class Main {
	public static void main(String[] args) throws RserveException, IOException{
		System.out.println("Begin of main......");
		
//		System.out.println("most popular......");
//		LKAUtil.saveRecSongsToFile(CONST.RECALG_TYPE.MOST_POPULAR,null,null,null,null);
//		LKAUtil.resultOfRecAlg(CONST.RECALG_TYPE.MOST_POPULAR);
//		
//		System.out.println("userKNN......");
//		LKAUtil.saveRecSongsToFile(CONST.RECALG_TYPE.UserKNN,null,null,null,null);
//		LKAUtil.resultOfRecAlg(CONST.RECALG_TYPE.UserKNN);
		
		String songPath = CONST.BASE_PATH+"result/doc_topic.txt";
		SongMatrix sMatrix = new SongMatrix(songPath,false);
		
//		for(int i = 0; i < CONST.FOLDER_NUM; i++){
//			sMatrix.generateTopicSequence(CONST.BASE_PATH+i+"/trainingSet/playlist.txt", i);
//		}
		
//		System.out.println("lda similar......");
//		LKAUtil.saveRecSongsToFile(CONST.RECALG_TYPE.LDA_SIMILAR,sMatrix,null,null,null);
//		LKAUtil.resultOfRecAlg(CONST.RECALG_TYPE.LDA_SIMILAR);
//		
		RConnection rc = new RConnection();
		rc.voidEval("library('forecast')");
//		
//		System.out.println("similar arima......");
//		LKAUtil.saveRecSongsToFile(CONST.RECALG_TYPE.SIMILAR_ARIMA,sMatrix, rc,null,null);
//		LKAUtil.resultOfRecAlg(CONST.RECALG_TYPE.SIMILAR_ARIMA);
//		
//		System.out.println("lda arima......");
//		LKAUtil.saveRecSongsToFile(CONST.RECALG_TYPE.LDA_ARIMA,sMatrix, rc,null,null);
//		LKAUtil.resultOfRecAlg(CONST.RECALG_TYPE.LDA_ARIMA);
//		rc.close();
//		
//		System.out.println("userknn context......");
//		LKAUtil.saveRecSongsToFile(CONST.RECALG_TYPE.USERKNN_CONTEXT,sMatrix,null,null,null);
//		LKAUtil.resultOfRecAlg(CONST.RECALG_TYPE.USERKNN_CONTEXT);
		
//		System.out.println("lsa similar......");
//		HashMap<Integer,Integer> docToIndex = new HashMap<Integer,Integer>();
//		LatentSemanticAnalysis lsa = new LatentSemanticAnalysis(true, 500, new LogEntropyTransform(), SVD.getFastestAvailableFactorization(), false, new StringBasisMapping());
//		LKAUtil.generateLSAModel(docToIndex, lsa);
//		LKAUtil.saveRecSongsToFile(CONST.RECALG_TYPE.LSA_SIMILAR,sMatrix,null,docToIndex,lsa);
//		LKAUtil.resultOfRecAlg(CONST.RECALG_TYPE.LSA_SIMILAR);
		
		//System.out.println("calculate average......");
		//LKAUtil.generateAverageResult(CONST.RECALG_TYPE.MOST_POPULAR);
		//LKAUtil.generateAverageResult(CONST.RECALG_TYPE.UserKNN);
		//LKAUtil.generateAverageResult(CONST.RECALG_TYPE.LDA_SIMILAR);
		//LKAUtil.generateAverageResult(CONST.RECALG_TYPE.LSA_SIMILAR);
		//LKAUtil.generateAverageResult(CONST.RECALG_TYPE.SIMILAR_ARIMA);
		//LKAUtil.generateAverageResult(CONST.RECALG_TYPE.LDA_ARIMA);
		//LKAUtil.generateAverageResult(CONST.RECALG_TYPE.USERKNN_CONTEXT);
		
//		LKAUtil.saveRecSongsToFile(CONST.RECALG_TYPE.UNIFIED,sMatrix, null,null,null);
//		LKAUtil.resultOfRecAlg(CONST.RECALG_TYPE.UNIFIED);
//		LKAUtil.generateAverageResult(CONST.RECALG_TYPE.UNIFIED);
		
		LKAUtil.saveRecSongsToFile(CONST.RECALG_TYPE.BASELINE,sMatrix, rc,null,null);
		LKAUtil.resultOfRecAlg(CONST.RECALG_TYPE.BASELINE);
		rc.close();
		LKAUtil.generateAverageResult(CONST.RECALG_TYPE.BASELINE);
		
		System.out.println("End of main......");
	}
}
