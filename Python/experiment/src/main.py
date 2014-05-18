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
import numpy as np

reload(sys)
sys.setdefaultencoding('utf-8')

def errorTest():
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

def bestParametersTraning():
  resultFile = open("../txt/result.txt")
  true = []
  arima = []
  avg = []
  similar = []
  lines = resultFile.readlines()
  for line in lines:
    line = line.strip("\n")
    if "pid" in line or "count" in line:
      continue
    if "true" in line:
      items = line.split("true = ")
      trueDict = eval(items[1])
      for value in trueDict.values():
        true.append(value)
    elif "arima" in line:
      items = line.split("arima = ")
      arimaDict = eval(items[1])
      for value in arimaDict.values():
        arima.append(value)
    elif "avg" in line:
      items = line.split("avg = ")
      avgDict = eval(items[1])
      for value in avgDict.values():
        avg.append(value)
    elif "similar" in line:
      items = line.split("similar = ")
      similarDict = eval(items[1])
      for value in similarDict.values():
        similar.append(value)

  resultFile.close()

  b = np.matrix([true])
  b = b.T 
  A = np.matrix([arima,avg,similar])
  A = A.T
  
  B = A.T
  C = B.dot(A)
  I = np.matrix([[1,0,0],[0,1,0],[0,0,1]])
  I = I.dot(0.5)
  C = C + I
  D = C.I
  E = D.dot(B)
  F = E.dot(b)

  print F
  

if __name__ == "__main__":

  #test.showRecallTrendWithDifferentCoeff_MostSimilarHybrid()
  
  #test.showRecallTrendWithDifferentCoeff_AverageHybrid()

  #test.testRecMethod(const.ARIMA)
  
  #test_session.showResult()
  test.showResult()
  #test.getErrorOfRecMethod(const.MARKOV)
  #bestParametersTraning()

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
#############HYbrid###################
#[[ arima:0.36197451]
# [ avg:0.3338634 ]
# [ similar:0.17563684]]

