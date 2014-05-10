#!/usr/bin/python
# -*- coding:utf-8 -*-
"""Main class to do any experiments.
Dependecies:
  Any modules you want.
  module 'test' may be needed.
"""
__author__ = 'Jason Wong(wwssttt@163.com)'
__version__ = '1.0'


import test
import test_session
import util
import sys
import const
import time
import persist

reload(sys)
sys.setdefaultencoding('utf-8')

if __name__ == "__main__":

  #test.showRecallTrendWithDifferentCoeff_MostSimilarHybrid()
  
  #test.showRecallTrendWithDifferentCoeff_AverageHybrid()

  #test.testRecMethod(const.ARIMA)
  
  #test_session.showResult()
  #test.showResult()
  #test.getErrorOfRecMethod(const.MARKOV)

  songDict = persist.readSongFromFile()
  print len(songDict)
  disDict = {}
  tarDict = songDict[672661981].getTopicDict()
  for sid in songDict:
    song = songDict[sid]
    topicDict = song.getTopicDict()
    dis = util.similarity(tarDict,topicDict)
    disDict[sid] = dis

  disList = sorted(disDict.iteritems(),key=lambda x:x[1])
  for i in range(0,100):
    print disList[i]


############Local#############
#Recall =  0.327450980392
#Precision =  0.00327450980392
#F1-Score =  0.00648417782955
#MAE =  0.601613917171
#RMSE =  0.647433601473
############Global#############
#Recall =  0.277777777778
#Precision =  0.00277777777778
#F1-Score =  0.00550055005501
#MAE =  0.614774254473
#RMSE =  0.647192165829
############MTSA#############
#Recall =  0.362091503268
#Precision =  0.00362091503268
#F1-Score =  0.00717012877758
#MAE =  0.571116720254
#RMSE =  0.611083092443
