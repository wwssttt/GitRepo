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

# set log's localtion and level
logging.basicConfig(filename=os.path.join(os.getcwd(),'../log/test_log.txt'),level=logging.DEBUG,format='%(asctime)s-%(levelname)s:%(message)s')

#show mae and rmse trends of hybrid methods with different coefficients
def showRecallTrendWithDifferentCoeff_Hybrid():
  songDict = persist.readSongFromFile()
  playlistDict = persist.readPlaylistFromFile()
  coeffs = [float(x) / 10 for x in range(0,11,1)]
  print coeffs
  recalls = []
  for coeff in coeffs:
    print 'hybrid  coeff = %f' % coeff
    recDict = predict.getRecDict(playlistDict,songDict,4,coeff)
    recall,precision,f1 = util.getTopNIndex(recDict,playlistDict)
    recalls.append(recall)
  plt.plot(coeffs,recalls,label="Recall")
  plt.title("Recall trends of Different Hybrid Coefficients")
  plt.xlabel("lambda")
  plt.ylabel("Recall")
  plt.legend(loc="upper right")
  plt.savefig("../img/hybrid_trend.png")
  plt.show()

#show mae and rmse trends of cold-law methods with different coefficients
def showRecallTrendWithDifferentCoeff_ColdLaw():
  songDict = persist.readSongFromFile()
  playlistDict = persist.readPlaylistFromFile()
  coeffs = [float(x) / 10 for x in range(0,100,5)]
  recalls = []
  for coeff in coeffs:
    print 'coldlaw coeff = %f' % coeff
    recDict = predict.getRecDict(playlistDict,songDict,2,0.5,coeff)
    recall,precision,f1 = util.getTopNIndex(recDict,playlistDict)
    recalls.append(recall)
  plt.plot(coeffs,recalls,label="Recall")
  plt.title("Recall trends of Different ColdLaw Coefficients")
  plt.xlabel("coefficients")
  plt.ylabel("Recall")
  plt.legend(loc="upper right")
  plt.savefig("../img/coldlaw_trend.png")
  plt.show()

#show weight trends of different coefficients
def showColdLawWithDifferentCoeff():
  coeffs = [0.25,0.5,0.75,1.0,5.0]
  x = range(0,20,1)
  for coeff in coeffs:
    weight = [1*math.pow(math.e,-1*coeff*delta) for delta in x]
    label = "coeff = %f" % coeff
    plt.plot(x,weight,label=label)
  plt.xlabel("time")
  plt.ylabel("weight")
  plt.title("Weight Trend of Cold Law with Different Coefficients")
  plt.legend(loc = "upper right")
  plt.savefig("../img/cold-law.png")
  plt.show()

#test traditional method
#0: most similar
#1: average
#2: cold law
#3: Arima
#4: Hybrid
#5: Dis-Arima
#6: Sd-Arima
#7: Sd-SVM
def testRecMethod(recType,subType = 0):
  if recType < 0 or recType > 6:
    print '0 <= recType <= 6'
    return
  if subType < 0 or subType > 1:
    print '0 <= subType <= 1'
    return
  start_time = time.time()
  songDict = persist.readSongFromFile()
  playlistDict = persist.readPlaylistFromFile()
  if recType < 5:
    recDict = predict.getRecDict(playlistDict,songDict,recType)
  elif recType == 5:
    recDict = predict.getRecDictOfDis(playlistDict,songDict)
  elif recType == 6:
    recDict = predict.getRecDictOfSd(playlistDict,songDict,recType=subType)
  recall,precision,f1 = util.getTopNIndex(recDict,playlistDict)
  mae,rmse = util.getMAEandRMSE(recDict,playlistDict,songDict)
  if recType == 0:
    info = '################Most Similar####################'
  elif recType == 1:
    info = '################Average####################'
  elif recType == 2:
    info = '################Cold Law####################'
  elif recType == 3:
    info = '################Arima####################'
  elif recType == 4:
    info = '################Hybrid####################'
  elif recType == 5:
    info = '################Dis-Arima####################'
  elif recType == 6:
    if subType == 0:
      info = '################Sd-Arima####################'
    else:
      info = '################Sd-SVM####################'
  else:
    info = '################Most Similar####################'
  print info
  logging.info(info)
  print 'Recall = ',recall
  logging.info('Recall = %f' % recall)
  print 'Precision = ',precision
  logging.info('Precision = %f' % precision)
  print 'F1-Score = ',f1
  logging.info('F1-Score = %f' % f1)
  print 'MAE = ',mae
  logging.info('MAE = %f' % mae)
  print 'RMSE = ',rmse
  logging.info('RMSE = %f' % rmse)
  print 'Consumed: %ds' % (time.time()-start_time)
  logging.info('Consumed: %ds' % (time.time()-start_time))

#test traditional method
#0: most similar
#1: average
#2: cold law
#3: Arima
#4: Hybrid
#5: Dis-Arima
#6: Sd-Arima
#7: Sd-SVM
def getErrorOfRecMethod(recType = 0,subType = 0):
  start_time = time.time()
  songDict = persist.readSongFromFile()
  playlistDict = persist.readPlaylistFromFile()
  if recType < 5:
    recDict = predict.getRecDict(playlistDict,songDict,recType)
  elif recType == 5:
    recDict = predict.getRecDictOfDis(playlistDict,songDict)
  elif recType == 6:
    recDict = predict.getRecDictOfSd(playlistDict,songDict,recType=subType)
  recalls = []
  precisions = []
  f1s = []
  maes = []
  rmses = []
  for topN in range(1,301,5):
    recall,precision,f1 = util.getTopNIndex(recDict,playlistDict,topN)
    mae,rmse = util.getMAEandRMSE(recDict,playlistDict,songDict,topN)
    recalls.append(recall)
    precisions.append(precision)
    f1s.append(f1)
    maes.append(mae)
    rmses.append(rmse)
    print '%d:TopN = %d:%f %f %f %f %f' % (recType,topN,recall,precision,f1,mae,rmse)
    logging.info('%d>%d:%f %f %f %f %f' % (recType,topN,recall,precision,f1,mae,rmse))
  return recalls,precisions,f1s,maes,rmses  

def showStatistics():
  logging.info('I am in showStatistics......')
  most_recalls,most_precisions,most_f1s,most_maes,most_rmses = getErrorOfRecMethod(0)
  avg_recalls,avg_precisions,avg_f1s,avg_maes,avg_rmses = getErrorOfRecMethod(1)
  cold_recalls,cold_precisions,cold_f1s,cold_maes,cold_rmses = getErrorOfRecMethod(2)
  arima_recalls,arima_precisions,arima_f1s,arima_maes,arima_rmses = getErrorOfRecMethod(3)
  hybrid_recalls,hybrid_precisions,hybrid_f1s,hybrid_maes,hybrid_rmses = getErrorOfRecMethod(4)
  dis_recalls,dis_precisions,dis_f1s,dis_maes,dis_rmses = getErrorOfRecMethod(5)
  sd_recalls,sd_precisions,sd_f1s,sd_maes,sd_rmses = getErrorOfRecMethod(6,0)
  svm_recalls,svm_precisions,svm_f1s,svm_maes,svm_rmses = getErrorOfRecMethod(6,1)
  plt.figure(1)
  plt.plot(most_recalls,label="MostSimilar")
  plt.plot(avg_recalls,label="Average")
  plt.plot(cold_recalls,label="ColdLaw")
  plt.plot(arima_recalls,label="Arima")
  plt.plot(hybrid_recalls,label="Hybrid")
  plt.plot(dis_recalls,label="Dis-Arima")
  plt.plot(sd_recalls,label="Sd-Arima")
  plt.plot(svm_recalls,label="Sd-SVM")
  plt.title("Recall of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("Recall")
  plt.savefig("../img/recall.png")
  plt.show()
  plt.figure(2)
  plt.plot(most_precisions,label="MostSimilar")
  plt.plot(avg_precisions,label="Average")
  plt.plot(cold_precisions,label="ColdLaw")
  plt.plot(arima_precisions,label="Arima")
  plt.plot(hybrid_precisions,label="Hybrid")
  plt.plot(dis_precisions,label="Dis-Arima")
  plt.plot(sd_precisions,label="Sd-Arima")
  plt.plot(svm_precisions,label="Sd-SVM")
  plt.title("Precision of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("Precision")
  plt.savefig("../img/precision.png")
  plt.show()
  plt.figure(3)
  plt.plot(most_f1s,label="MostSimilar")
  plt.plot(avg_f1s,label="Average")
  plt.plot(cold_f1s,label="ColdLaw")
  plt.plot(arima_f1s,label="Arima")
  plt.plot(hybrid_f1s,label="Hybrid")
  plt.plot(dis_f1s,label="Dis-Arima")
  plt.plot(sd_f1s,label="Sd-Arima")
  plt.plot(svm_f1s,label="Sd-SVM")
  plt.title("F1-Score of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("F1-Score")
  plt.savefig("../img/f1.png")
  plt.show()
  plt.figure(4)
  plt.plot(most_maes,label="MostSimilar")
  plt.plot(avg_maes,label="Average")
  plt.plot(cold_maes,label="ColdLaw")
  plt.plot(arima_maes,label="Arima")
  plt.plot(hybrid_maes,label="Hybrid")
  plt.plot(dis_maes,label="Dis-Arima")
  plt.plot(sd_maes,label="Sd-Arima")
  plt.plot(svm_maes,label="Sd-SVM")
  plt.title("MAE of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("MAE")
  plt.savefig("../img/mae.png")
  plt.show()
  plt.figure(5)
  plt.plot(most_rmses,label="MostSimilar")
  plt.plot(avg_rmses,label="Average")
  plt.plot(cold_rmses,label="ColdLaw")
  plt.plot(arima_rmses,label="Arima")
  plt.plot(hybrid_rmses,label="Hybrid")
  plt.plot(dis_rmses,label="Dis-Arima")
  plt.plot(sd_rmses,label="Sd-Arima")
  plt.plot(svm_rmses,label="Sd-SVM")
  plt.title("RMSE of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("RMSE")
  plt.savefig("../img/rmse.png")
  plt.show()
  logging.info('I am out showStatistics......')
