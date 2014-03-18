package storm.starter;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import storm.starter.bolt.PersonBolt;
import storm.starter.model.Person;
import storm.starter.spout.PersonSpout;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.utils.Utils;

public class PersonTopology{
	
	private static Log log = (Log) LogFactory.getLog(PersonTopology.class.getName()); 
	
	public static StormTopology buildTopology(ArrayList<Person> list){
		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("1", new PersonSpout(list));
		builder.setBolt("2", new PersonBolt()).globalGrouping("1");
		return builder.createTopology();
	}
	
	public static ArrayList<Person> getPerson(){
		ArrayList<Person> list = new ArrayList<Person>();
		for (int i = 1; i < 10; i++){
			Person p = new Person();
			p.setId((long)i);
			p.setName("Hello"+i);
			p.setAge(20+i);
			list.add(p);
		}
		return list;
	}
	
	public static ArrayList<String> getStr(){
		ArrayList<String> list = new ArrayList<String>();
		for(int i = 1; i < 10; i++){
			list.add("test"+i);
		}
		return list;
	}
	
	public static void testTopology(){
		log.debug("PersornTopology starts......");
		long startTime = System.currentTimeMillis();
		Config conf = new Config();
		conf.setDebug(true);
		conf.setNumWorkers(2);
		conf.setMaxSpoutPending(1);
		LocalCluster cluster = new LocalCluster();
		ArrayList<Person> list = getPerson();
		
		cluster.submitTopology("demo", conf, buildTopology(list));
		long executeTime = System.currentTimeMillis();
		Utils.sleep(3000);
		log.debug("PersonTopology ends......");
		long stopTime = System.currentTimeMillis();
		log.debug("Consumed: Run="+(executeTime-startTime)+",Total:"+(stopTime-startTime)+"");
		
		cluster.killTopology("demo");
		cluster.shutdown();
		
	}
	
	public static void main(String[] args){
		testTopology();
	}
}
