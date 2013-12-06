import java.util.HashMap;

public class MarkovTransElement {
	private String key;
	private int order;
	private int count;
	private int maxNext;
	private HashMap<Integer,Double> nextState;
	
	public MarkovTransElement(String key){
		this.key = key;
		this.order = 0;
		this.count = 0;
		this.maxNext = 0;
		this.nextState = new HashMap<Integer,Double>();
		
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public HashMap<Integer, Double> getNextState() {
		return nextState;
	}
	public void setNextState(HashMap<Integer, Double> nextState) {
		this.nextState.clear();
		for(Integer key : nextState.keySet()){
			this.nextState.put(key, nextState.get(key));
		}
	}
	public void updateNextState(int next){
		if(this.nextState.containsKey(next)){
			double nextCount = this.nextState.get(key);
			nextCount++;
			this.nextState.put(next, nextCount);
		}else{
			this.nextState.put(next, 1.0);
		}
		this.count++;
	}
	
	public void generateTransProbability(){
		double maxPro = -1;
		for(Integer key: this.nextState.keySet()){
			this.nextState.put(key, this.nextState.get(key) / this.count);
			if(nextState.get(key) > maxPro){
				maxPro = nextState.get(key);
				maxNext = key;
			}
		}
	}
	
	public boolean containsNextState(int nextId){
		if(this.nextState.containsKey(nextId)){
			return true;
		}
		return false;
	}
	
	public int getMaxNext(){
		return maxNext;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("\n");
		sb.append("----------List of State Element----------");
		sb.append("\n");
		sb.append("key = "+this.key+"\norder = "+order+"\ncount = "+count+"\n");
		for(Integer nextKey: nextState.keySet()){
			sb.append("->"+nextKey+":"+nextState.get(nextKey)+"  ");
		}
		sb.append("\n");
		return sb.toString();
	}
}
