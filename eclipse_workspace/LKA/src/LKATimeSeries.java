import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import com.numericalmethod.suanshu.stats.timeseries.datastructure.univariate.realtime.inttime.IntTimeTimeSeries;

public class LKATimeSeries implements IntTimeTimeSeries{

	private ArrayList<IntTimeTimeSeries.Entry> series;
	
	public LKATimeSeries(){
		this.series = new ArrayList<IntTimeTimeSeries.Entry>();
	}
	
	public LKATimeSeries(HashMap<Integer,Double> series){
		this.series = new ArrayList<IntTimeTimeSeries.Entry>();
		for(Integer key: series.keySet()){
			IntTimeTimeSeries.Entry entry = new IntTimeTimeSeries.Entry(key,series.get(key));
			if(!this.series.contains(entry)){
				this.series.add(entry);
			}
		}
	}
	
	public void addEntry(int time, double value){
		IntTimeTimeSeries.Entry entry = new IntTimeTimeSeries.Entry(time,value);
		if(!this.series.contains(entry)){
			this.series.add(entry);
		}
	}
	
	@Override
	public double[] toArray() {
		
		if(this.series.size() == 0){
			return null;
		}
		
		double[] sequences = new double[this.series.size()];
		
		//按时间排序
		Collections.sort(this.series, new Comparator<IntTimeTimeSeries.Entry>() {   
			@Override
			public int compare(Entry arg0, Entry arg1) {
				return arg0.getTime()-arg1.getTime();
			}
		}); 
		
		System.out.println("toArray==");
		for (int i = 0; i < series.size(); i++) {
		    double val = series.get(i).getValue();
		    sequences[i] = val;
		    System.out.print(val+"==>");
		}
		System.out.println();
		
		return sequences;
	}

	@Override
	public int size() {
		return series.size();
	}

	@Override
	public Iterator<Entry> iterator() {
		if(this.series.size() == 0){
			return null;
		}
		
		//按时间排序
		Collections.sort(this.series, new Comparator<IntTimeTimeSeries.Entry>() {   
			@Override
			public int compare(Entry arg0, Entry arg1) {
				return arg0.getTime()-arg1.getTime();
			}
		}); 
		
		System.out.println("Iterator==");
		Iterator<Entry> iterator = this.series.iterator();
		while(iterator.hasNext()){
			Entry e = iterator.next();
			System.out.print(e.getValue()+"==>");
		}
		System.out.println();
		return this.series.iterator();
	}

	@Override
	public double get(int arg0) {
		if(arg0 < series.size()){
			return series.get(arg0).getValue();
		}else{
			return 0;
		}
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < series.size(); i++){
			sb.append(series.get(i).getValue() + "==>");
		}
		return sb.toString();
	}
}
