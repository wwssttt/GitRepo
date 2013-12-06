import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class MarkovModel {
	//sequences of input, integer represents states id
	private ArrayList<ArrayList<Integer>> sequences;
	//trans matrix of markov
	private HashMap<String,MarkovTransElement> transMatrix;
	
	/*
	 * @pram:sequencePath file path of sequence
	 */
	public MarkovModel(String sequencePath){
		try{
			//read file and transfer into arraylist
			BufferedReader br = new BufferedReader(new FileReader(sequencePath));
			String line = null;
			ArrayList<ArrayList<Integer>> sequenceArr = new ArrayList<ArrayList<Integer>>();
			while((line = br.readLine()) != null){
				ArrayList<Integer> arr = new ArrayList<Integer>();
				String[] states = line.split(" ");
				for(int i = 0; i < states.length; i++){
					arr.add(Integer.valueOf(states[i]));
				}
				sequenceArr.add(arr);
			}
			br.close();
			//init the model with sequence
			this.initMarkovWithSequences(sequenceArr);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public MarkovModel(ArrayList<ArrayList<Integer>> sequences){
		super();
		this.initMarkovWithSequences(sequences);
	}
	
	private void initMarkovWithSequences(ArrayList<ArrayList<Integer>> sequences) {
		//init the member variable
		this.sequences = new ArrayList<ArrayList<Integer>>();
		for(int i = 0; i < sequences.size(); i++){
			ArrayList<Integer> seq = new ArrayList<Integer>();
			ArrayList<Integer> arr = sequences.get(i);
			for(int j = 0; j < arr.size(); j++){
				seq.add(arr.get(j));
			}
			this.sequences.add(seq);
		}
		this.transMatrix = new HashMap<String,MarkovTransElement>();
		
		//loop for every sequence
		for(int index = 0; index < this.sequences.size(); index++){
			ArrayList<Integer> sequence = this.sequences.get(index);
			//loop for every order
			for(int order = 1; order <= CONST.MARKOV_ORDER; order++){
				for(int i = 0; i < sequence.size() - order; i++){
					String key = "";
					for(int j = 0; j < order; j++){
						if(j == order - 1){
							key = key + sequence.get(i+j);
						}else{
							key = key + sequence.get(i+j) + ">";
						}
					}
					int next = Integer.valueOf(sequence.get(i+order));
					
					if(this.transMatrix.containsKey(key)){
						this.transMatrix.get(key).updateNextState(next);
					}else{
						MarkovTransElement ele = new MarkovTransElement(key);
						ele.setOrder(order);
						ele.updateNextState(next);
						this.transMatrix.put(key, ele);
					}
					
				}
			}
		}
		
		for(String eleKey:this.transMatrix.keySet()){
			this.transMatrix.get(eleKey).generateTransProbability();
		}
	}
	
	public ArrayList<ArrayList<Integer>> getSequences() {
		return sequences;
	}

	public HashMap<String, MarkovTransElement> getTransMatrix() {
		return transMatrix;
	}
	
	public MarkovTransElement getTransElement(String eleKey){
		return this.transMatrix.get(eleKey);
	}
}
