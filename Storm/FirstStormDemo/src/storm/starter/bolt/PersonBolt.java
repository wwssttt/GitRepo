/**
 * ListBolt.java
 * 版权所有(C) 2013 
 * 创建:cuiran 2013-01-15 16:05:25
 */
package storm.starter.bolt;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import storm.starter.model.Person;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;

/**
 * TODO
 * @author cuiran
 * @version TODO
 */
public class PersonBolt implements IRichBolt {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6467077126541265761L;
	
	private static Log log = LogFactory.getLog(PersonBolt.class.getName());
	
	private OutputCollector collector;

	/* (non-Javadoc)
	 * @see backtype.storm.task.IBolt#cleanup()
	 */
	@Override
	public void cleanup() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see backtype.storm.task.IBolt#execute(backtype.storm.tuple.Tuple)
	 */
	@Override
	public void execute(Tuple tuple) {
		// TODO Auto-generated method stub
		try {
			log.debug("execute----->处理数据");
			Person p= (Person)tuple.getValueByField("person");
			
			log.debug("处理数据  用户="+p.toString());
			
			collector.ack(tuple);
		}catch (Exception e) {
			collector.fail(tuple);
			// TODO: handle exception
			log.error("处理数据出现异常", e);
		}
		
	}

	/* (non-Javadoc)
	 * @see backtype.storm.task.IBolt#prepare(java.util.Map, backtype.storm.task.TopologyContext, backtype.storm.task.OutputCollector)
	 */
	@Override
	public void prepare(Map map, TopologyContext context, OutputCollector collector) {
		// TODO Auto-generated method stub
		this.collector=collector;
	}

	/* (non-Javadoc)
	 * @see backtype.storm.topology.IComponent#declareOutputFields(backtype.storm.topology.OutputFieldsDeclarer)
	 */
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see backtype.storm.topology.IComponent#getComponentConfiguration()
	 */
	@Override
	public Map<String, Object> getComponentConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

}
