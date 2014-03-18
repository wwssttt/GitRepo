#!/usr/bin python
#coding:utf-8
############################
#give some test function
############################

import time
import persist
import predict
import util
import logging
import os
import math
import matplotlib.pyplot as plt
import sys
import const

reload(sys)
sys.setdefaultencoding('utf-8')

# set log's localtion and level
logging.basicConfig(filename=os.path.join(os.getcwd(),'../log/test_log_%s.txt' % const.DATASET_NAME),level=logging.DEBUG,format='%(asctime)s-%(levelname)s:%(message)s')

#test traditional method
#0: most similar
#1: average
#2: Arima
def getErrorOfRecMethod(recType = 0):
  start_time = time.time()
  songDict = persist.readSongFromFile()
  allPlaylist = persist.readPlaylistFromFile_Session()
  recalls = []
  precisions = []
  f1s = []
  maes = []
  rmses = []
  for scale in range(10):
    playlistDict = allPlaylist[scale]
    if recType == const.ARIMA:
      recDict = predict.getRecDict(playlistDict,songDict,recType,scale)
    elif recType == const.SIMILAR:
      recDict = predict.getRecDict(playlistDict,songDict,recType,scale)
    elif recType == const.AVG:
      recDict = predict.getRecDict(playlistDict,songDict,recType,scale)
    index = 0
    for topN in range(1,const.TOP_N,1):
      recall,precision,f1 = util.getTopNIndex(recDict,playlistDict,topN)
      mae,rmse = util.getMAEandRMSE(recDict,playlistDict,songDict,topN)
      if scale == 0:
        recalls.append(recall)
        precisions.append(precision)
        f1s.append(f1)
        maes.append(mae)
        rmses.append(rmse)
      else:
        recalls[index] += recall
        precisions[index] += precision
        f1s[index] += f1
        maes[index] += mae
        rmses[index] += rmse
      index += 1

  #cal the avg value
  recalls = [recall / 10 for recall in recalls]
  precisions = [precision / 10 for precision in precisions]
  f1s = [f1 / 10 for f1 in f1s]
  maes = [mae / 10 for mae in maes]
  rmses = [rmse / 10 for rmse in rmses]

  #logging info to log
  index = 0
  for topN in range(1,const.TOP_N,1):
    print '%d:TopN = %d:%f %f %f %f %f' % (recType,topN,recalls[index],precisions[index],f1s[index],maes[index],rmses[index])
    logging.info('%d>%d:%f %f %f %f %f' % (recType,topN,recalls[index],precisions[index],f1s[index],maes[index],rmses[index]))
    index += 1
  end_time = time.time()
  print 'Consumed:%d' % (end_time-start_time)
  return recalls,precisions,f1s,maes,rmses  

#show all recommend results with different methods
def showResult():
  logging.info('I am in showResult......')
  filename = "../txt/%s_testall_%d_%d.txt" % (const.DATASET_NAME,const.TOPIC_NUM,const.TOP_N)
  x = range(1,const.TOP_N,1)
  result = [[[] for i in range(5)] for i in range(3)]
  #read result from file to result
  if os.path.exists(filename):
    print '%s is existing......' % filename    
    rFile = open(filename,"r")
    lines = rFile.readlines()
    for line in lines:
      line = line.rstrip('\n')
      items = line.split("INFO:")
      line = items[1]
      items = line.split(":")
      ids = items[0]
      values = items[1]
      idItems = ids.split(">")
      mid = int(idItems[0])
      topN = int(idItems[1])
      valueItems = values.split()
      result[mid][0].append(float(valueItems[0]))
      result[mid][1].append(float(valueItems[1]))
      result[mid][2].append(float(valueItems[2]))
      result[mid][3].append(float(valueItems[3]))
      result[mid][4].append(float(valueItems[4]))
    rFile.close()
  else:
    rFile = open(filename,"w")
    rFile.close()
  #if some method is not in file, recreate it
  for mid in range(3):
    if len(result[mid][0]) == 0:
      recalls,precisions,f1s,maes,rmses = getErrorOfRecMethod(mid)
      result[mid][0] = recalls
      result[mid][1] = precisions
      result[mid][2] = f1s
      result[mid][3] = maes
      result[mid][4] = rmses

  #plt img of comparing with pure method
  for index in range(5):
    plt.figure(index)
    indexName = util.getIndexName(index)
    mids = [const.ARIMA,const.SIMILAR,const.AVG]
    marker = ['k','k-.','k:']
    markerIndex = 0
    for mid in mids:
      if index == 1 or index == 2:
        plt.plot(x[20:],result[mid][index][20:],marker[markerIndex],label=util.getMethodName(mid))
      else:
        plt.plot(x,result[mid][index],marker[markerIndex],label=util.getMethodName(mid))
      markerIndex += 1
    plt.title("%s of Different Recommend Algorithms" % indexName)
    plt.xlabel("Number of recommendations")
    plt.ylabel(indexName)
    plt.legend()
    plt.xlim(1,160)
    plt.savefig("../img/pure_%s_%s_%d_%d.png" % (const.DATASET_NAME,indexName,const.TOPIC_NUM,const.TOP_N))
    #plt.show()
  logging.info('I am out showResult......')
