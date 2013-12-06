public class CONST {
	public static int TOPIC_NUM = 530;
	public static int KNN = 50;
	public static int LAST_LISTENED_NUM = 10;
	public static int TOPN_MIN = 1;
	public static int TOPN_MAX = 301;
	public static int REC_N = 31;
	public static int REC_ARR_LEN = 301;
	public static int TOPN_STEP = 10;
	public static enum RECALG_TYPE{LDA_ARIMA,LDA_SIMILAR,MOST_POPULAR,UserKNN,LSA_SIMILAR,USERKNN_CONTEXT,SIMILAR_ARIMA,UNIFIED,BASELINE};
	public static double THREASHOLD = 0.15;
	public static int STATE_COUNT = 30;
	public static int MARKOV_ORDER = 7;
	public static String BASE_RESULT_PATH = "result_new/";
	public static int FOLDER_NUM = 10;
	public static String BASE_PATH = "documents/";
	
	public static int MIN_SUB_SEQ = 3;
	public static int MAX_SUB_SEQ = 7;
	
	public static enum USER_TYPE{TRAIN,TEST,NONE};
}
