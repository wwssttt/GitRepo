import storm
import rpy2.robjects as robjects
from rpy2.robjects.packages import importr

class MTSA_DRPC_PythonArimaBolt(storm.BasicBolt):
    def process(self, tup):
        importr("forecast")
        subSeq = tup.values[1]
        items = subSeq.split('#')
        tid = int(items[0])
        topicStr = items[1]
        topicItems = topicStr.split('>')
        topicProbability = []
        size = len(topicItems)
        for i in range(size):
          topicProbability.append(float(topicItems[i]))
        vec = robjects.FloatVector(topicProbability)
        ts = robjects.r['ts'](vec)
        fit = robjects.r['auto.arima'](ts)
        next = robjects.r['forecast'](fit,h=1)
        result = float(next.rx('mean')[0][0])
        resultStr = '%d:%f' % (tid,result)
        storm.emit([tup.values[0],resultStr])

MTSA_DRPC_PythonArimaBolt().run()
