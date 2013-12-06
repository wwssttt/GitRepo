import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class TopicSequence {
	private ArrayList<ArrayList<Integer>> prefix; //序列前缀
	private ArrayList<Integer>    last;  //序列最后一个项目
	private int    length;  //序列长度
	private int    support; //序列支持度
	
	/**
	 * 构造函数
	 * @param prefix  前缀
	 * @param last    后缀
	 * @param length  长度
	 * @param support 支持度
	 */
	public TopicSequence(ArrayList<ArrayList<Integer>> prefix, ArrayList<Integer> last, int length, int support) {
		super();
		this.prefix = new ArrayList<ArrayList<Integer>>();
		setPrefix(prefix);
		this.last = new ArrayList<Integer>();
		setLast(last);
		this.length = length;
		this.support = support;
	}
	
	/**
	 * 获取序列前缀
	 * @return  序列前缀
	 */
	public ArrayList<ArrayList<Integer>> getPrefix() {
		return prefix;
	}
	
	/**
	 * 设置序列前缀
	 * @param prefix  待设置前缀
	 */
	public void setPrefix(ArrayList<ArrayList<Integer>> prefix) {
		this.prefix.clear();
		for(int i = 0; i < prefix.size(); i++){
			ArrayList<Integer> tidArr = new ArrayList<Integer>();
			ArrayList<Integer> curArr = prefix.get(i);
			for(int j = 0; j < curArr.size(); j++){
				tidArr.add(curArr.get(j));
			}
			this.prefix.add(tidArr);
		}
	}
	
	/**
	 * 获取序列最后一个项目
	 * @return  最后一个项目
	 */
	public ArrayList<Integer> getLast() {
		return last;
	}
	
	/**
	 * 设置最后一个项目
	 * @param last
	 */
	public void setLast(ArrayList<Integer> last) {
		this.last.clear();
		for(int i = 0; i < last.size(); i++){
			this.last.add(last.get(i));
		}
	}
	
	/**
	 * 获取序列长度
	 * @return  序列长度
	 */
	public int getLength() {
		return length;
	}
	
	/**
	 * 获取序列支持度
	 * @return  序列支持度
	 */
	public int getSupport() {
		return support;
	}
	
	/**
	 * 设置序列支持度
	 * @param support
	 */
	public void setSupport(int support) {
		this.support = support;
	}

	/**
	 * 获取序列长度为len的子序列
	 * @param len  子序列长度
	 * @return  子序列
	 */
	public TopicSequence subSeq(int len){
		if(len < CONST.MIN_SUB_SEQ || len > this.length){
			return null;
		}else{
			ArrayList<ArrayList<Integer>> subPrefix = new ArrayList<ArrayList<Integer>>(); 
			for(int i = this.prefix.size() - len + 1; i < this.prefix.size(); i++){
				ArrayList<Integer> tidArr = new ArrayList<Integer>();
				ArrayList<Integer> curArr = prefix.get(i);
				for(int j = 0; j < curArr.size(); j++){
					tidArr.add(curArr.get(j));
				}
				subPrefix.add(tidArr);
			}
			ArrayList<Integer> subLast = new ArrayList<Integer>();
			for(int i = 0; i < this.last.size(); i++){
				subLast.add(this.last.get(i));
			}
			
			TopicSequence sub = new TopicSequence(subPrefix,subLast,len,-1);
			return sub;
		}
	}
	
	/**
	 * 生成频繁序列模式
	 * @param seqPath 序列文件路径
	 * @return 频繁序列模式列表
	 */
	public static ArrayList<TopicSequence> getFrequentPatternSequence(String seqPath){
		ArrayList<TopicSequence> seqs = new ArrayList<TopicSequence>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(seqPath));
			String line = null;
			while((line = br.readLine()) != null){
				TopicSequence seq = getTopicSequenceFromString(line);
				if(seq != null){
					seqs.add(seq);
				}
			}
			br.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return seqs;
	}
	
	/**
	 * 字符串转换为合法主题序列
	 * @param seqStr 序列字符串
	 * @return  主题序列
	 */
	public static TopicSequence getTopicSequenceFromString(String seqStr){
		String[] eles = seqStr.split("-1");
		if(eles.length < 3){
			return null;
		}else{
			ArrayList<ArrayList<Integer>> prefix = new ArrayList<ArrayList<Integer>>();
			ArrayList<Integer> last = new ArrayList<Integer>();
			int sup = 0;
			int len = eles.length - 1;
			for(int i = 0; i < eles.length; i++){
				String ele = eles[i].trim();
				if(ele.startsWith("#SUP")){
					String[] blabla = ele.split(" ");
					sup = Integer.valueOf(blabla[1]);
				}else{
//					ele = ele.substring(ele.lastIndexOf(">")+2);
//					ele = ele.trim();
					if(i == len - 1){
						String[] blabla = ele.split(" ");
						for(int j = 0; j < blabla.length; j++){
							last.add(Integer.valueOf(blabla[j]));
						}
					}else{
						String[] blabla = ele.split(" ");
						ArrayList<Integer> topics = new ArrayList<Integer>();
						for(int j = 0; j < blabla.length; j++){
							topics.add(Integer.valueOf(blabla[j]));
						}
						prefix.add(topics);
					}
				}
			}
			TopicSequence seq = new TopicSequence(prefix,last,len,sup);
			return seq;
		}
	}
	
	/**
	 * 预测目标序列下一个可能出现的主题集合
	 * @param targetSeq  目标序列
	 * @param allSeq     序列库
	 * @return 下一个可能出现的主题集合
	 */
	public static ArrayList<Integer> predcitTopics(TopicSequence targetSeq, ArrayList<TopicSequence> allSeq){
		ArrayList<Integer> preTopics = new ArrayList<Integer>();
		
		HashMap<Integer,TopicSequence> tarSeqMap = new HashMap<Integer,TopicSequence>();
		for(int tarLen = CONST.MIN_SUB_SEQ; tarLen <= CONST.MAX_SUB_SEQ; tarLen++){
			TopicSequence tSeq = targetSeq.subSeq(tarLen);
			if(tSeq != null){
				tarSeqMap.put(tarLen, tSeq);
			}
		}
		
		for(int i = 0; i < allSeq.size(); i++){
			TopicSequence seq = allSeq.get(i);
			int len = seq.getLength();
			TopicSequence tarSeq = tarSeqMap.get(len-1);
			if(tarSeq == null){
				continue;
			}
			ArrayList<ArrayList<Integer>> seqPrefix = seq.getPrefix();
			ArrayList<Integer> seqLast = seq.getLast();
			ArrayList<ArrayList<Integer>> tarPrefix = tarSeq.getPrefix();
			ArrayList<Integer> tarLast = tarSeq.getLast();
			int oIndex = 0;
			boolean outFlag = true;
			for(oIndex = 0; oIndex < tarPrefix.size(); oIndex++){
				ArrayList<Integer> seqArr = tarPrefix.get(oIndex);
				boolean innerflag = false;
				for(int iIndex = 0; iIndex < seqArr.size(); iIndex++){
					if(seqPrefix.get(oIndex).contains(seqArr.get(iIndex))){
						innerflag = true;
						break;
					}
				}
				if(!innerflag){
					outFlag = false;
					break;
				}
			}
			if(!outFlag){
				continue;
			}
			
			boolean flag = false;
			for(int lIndex = 0; lIndex < tarLast.size(); lIndex++){
				if(seqPrefix.get(oIndex).contains(tarLast.get(lIndex))){
					flag = true;
					break;
				}
			}
			if(!flag){
				outFlag = false;
				continue;
			}else{
				for(int pIndex = 0; pIndex < seqLast.size(); pIndex++){
					if(!preTopics.contains(seqLast.get(pIndex))){
						preTopics.add(seqLast.get(pIndex));
					}
				}
			}
		}
		
		return preTopics;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < this.prefix.size(); i++){
			for(int j = 0; j < this.prefix.get(i).size(); j++){
				sb.append(this.prefix.get(i).get(j)+" ");
			}
			sb.append("-1 ");
		}
		for(int i = 0; i < this.last.size(); i++){
			sb.append(this.last.get(i)+" ");
		}
		sb.append("-1 ");
		sb.append("#Sup: ");
		sb.append(this.support);
		sb.append(" #Len: ");
		sb.append(this.length);
		sb.append("\n");
		return sb.toString();
	}
}
