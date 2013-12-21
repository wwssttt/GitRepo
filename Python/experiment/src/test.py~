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

#show mae and rmse trends of most similarhybrid methods with different coefficients
def showRecallTrendWithDifferentCoeff_MostSimilarHybrid():
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
  #plt.show()

#show mae and rmse trends of average hybrid methods with different coefficients
def showRecallTrendWithDifferentCoeff_AverageHybrid():
  songDict = persist.readSongFromFile()
  playlistDict = persist.readPlaylistFromFile()
  coeffs = [float(x) / 10 for x in range(0,11,1)]
  print coeffs
  recalls = []
  for coeff in coeffs:
    print 'hybrid  coeff = %f' % coeff
    recDict = predict.getRecDict(playlistDict,songDict,7,coeff)
    recall,precision,f1 = util.getTopNIndex(recDict,playlistDict)
    recalls.append(recall)
  plt.plot(coeffs,recalls,label="Recall")
  plt.title("Recall trends of Different Hybrid Coefficients")
  plt.xlabel("lambda")
  plt.ylabel("Recall")
  plt.legend(loc="upper right")
  plt.savefig("../img/average_hybrid_trend.png")
  #plt.show()

#show mae and rmse trends of cold-law methods with different coefficients
def showRecallTrendWithDifferentCoeff_ColdLaw():
  songDict = persist.readSongFromFile()
  playlistDict = persist.readPlaylistFromFile()
  coeffs = [float(x) / 10 for x in range(0,100,10)]
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
  #plt.show()

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
#4: Most Similar Hybrid
#5: Dis-Arima
#6: Sd-Arima(0) Sd-SVM(1)
#7: Average Hybrid
def testRecMethod(recType,subType = 0):
  if recType < 0 or recType > 6:
    print '0 <= recType <= 7'
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
    info = '################Most Similar Hybrid####################'
  elif recType == 5:
    info = '################Dis-Arima####################'
  elif recType == 6:
    if subType == 0:
      info = '################Sd-Arima####################'
    else:
      info = '################Sd-SVM####################'
  elif recType == 7:
    info = '################Average Hybrid####################'
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
#4: Most Similar Hybrid
#5: Dis-Arima
#6: Sd-Arima(0) Sd-SVM(1)
#7: Average Hybrid
def getErrorOfRecMethod(recType = 0,subType = 0):
  start_time = time.time()
  songDict = persist.readSongFromFile()
  playlistDict = persist.readPlaylistFromFile()
  if recType < 5 or recType == 7:
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
  filename = "../txt/testall.txt"
  x = range(1,301,5)
  if os.path.exists(filename):
    print '%s is existing......' % filename
    most_recalls = []
    most_precisions = []
    most_f1s = []
    most_maes = []
    most_rmses = []
    avg_recalls = []
    avg_precisions = [] 
    avg_f1s = []
    avg_maes = []
    avg_rmses = []
    cold_recalls = []
    cold_precisions = []
    cold_f1s = []
    cold_maes = []
    cold_rmses = []
    arima_recalls = []
    arima_precisions = []
    arima_f1s = []
    arima_maes = []
    arima_rmses = []
    hybrid_recalls = []
    hybrid_precisions = []
    hybrid_f1s = []
    hybrid_maes = []
    hybrid_rmses = []
    average_hybrid_recalls = []
    average_hybrid_precisions = []
    average_hybrid_f1s = []
    average_hybrid_maes = []
    average_hybrid_rmses = []
    dis_recalls = []
    dis_precisions = []
    dis_f1s = []
    dis_maes = []
    dis_rmses = []
    sd_recalls = []
    sd_precisions = []
    sd_f1s = []
    sd_maes = []
    sd_rmses = []
    svm_recalls = []
    svm_precisions = []
    svm_f1s = []
    svm_maes = []
    svm_rmses = []
    rFile = open(filename,"r")
    flag = False
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
      recall = float(valueItems[0])
      precision = float(valueItems[1])
      f1 = float(valueItems[2])
      mae = float(valueItems[3])
      rmse = float(valueItems[4])
      if mid == 0:
        most_recalls.append(recall)
        most_precisions.append(precision)
        most_f1s.append(f1)
        most_maes.append(mae)
        most_rmses.append(rmse)
      elif mid == 1:
        avg_recalls.append(recall)
        avg_precisions.append(precision)
        avg_f1s.append(f1)
        avg_maes.append(mae)
        avg_rmses.append(rmse)
      elif mid == 2:
        cold_recalls.append(recall)
        cold_precisions.append(precision)
        cold_f1s.append(f1)
        cold_maes.append(mae)
        cold_rmses.append(rmse)
      elif mid == 3:
        arima_recalls.append(recall)
        arima_precisions.append(precision)
        arima_f1s.append(f1)
        arima_maes.append(mae)
        arima_rmses.append(rmse)
      elif mid == 4:
        hybrid_recalls.append(recall)
        hybrid_precisions.append(precision)
        hybrid_f1s.append(f1)
        hybrid_maes.append(mae)
        hybrid_rmses.append(rmse)
      elif mid == 5:
        dis_recalls.append(recall)
        dis_precisions.append(precision)
        dis_f1s.append(f1)
        dis_maes.append(mae)
        dis_rmses.append(rmse)
      elif mid == 6:
        if not flag:
          sd_recalls.append(recall)
          sd_precisions.append(precision)
          sd_f1s.append(f1)
          sd_maes.append(mae)
          sd_rmses.append(rmse)
          if topN == x[len(x)-1]:
            flag = True
        else:
          svm_recalls.append(recall)
          svm_precisions.append(precision)
          svm_f1s.append(f1)
          svm_maes.append(mae)
          svm_rmses.append(rmse)
      elif mid == 7:
        average_hybrid_recalls.append(recall)
        average_hybrid_precisions.append(precision)
        average_hybrid_f1s.append(f1)
        average_hybrid_maes.append(mae)
        average_hybrid_rmses.append(rmse)
    rFile.close()
  else:
    most_recalls,most_precisions,most_f1s,most_maes,most_rmses = getErrorOfRecMethod(0)
    avg_recalls,avg_precisions,avg_f1s,avg_maes,avg_rmses = getErrorOfRecMethod(1)
    cold_recalls,cold_precisions,cold_f1s,cold_maes,cold_rmses = getErrorOfRecMethod(2)
    arima_recalls,arima_precisions,arima_f1s,arima_maes,arima_rmses = getErrorOfRecMethod(3)
    hybrid_recalls,hybrid_precisions,hybrid_f1s,hybrid_maes,hybrid_rmses = getErrorOfRecMethod(4)
    dis_recalls,dis_precisions,dis_f1s,dis_maes,dis_rmses = getErrorOfRecMethod(5)
    sd_recalls,sd_precisions,sd_f1s,sd_maes,sd_rmses = getErrorOfRecMethod(6,0)
    svm_recalls,svm_precisions,svm_f1s,svm_maes,svm_rmses = getErrorOfRecMethod(6,1)
    average_hybrid_recalls,average_hybrid_precisions,average_hybrid_f1s,average_hybrid_maes,average_hybrid_rmses = getErrorOfRecMethod(7)
  plt.figure(1)
  plt.plot(x,most_recalls,label="MostSimilar")
  plt.plot(x,avg_recalls,label="Average")
  plt.plot(x,cold_recalls,label="ColdLaw")
  plt.plot(x,arima_recalls,label="Multi-Arima")
  plt.plot(x,hybrid_recalls,label="Neighbor+Arima")
  plt.plot(x,average_hybrid_recalls,label="Average+Arima")
  plt.title("Recall of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("Recall")
  plt.legend(loc="lower right")
  plt.savefig("../img/recall.png")
  #plt.show()
  plt.figure(2)
  plt.plot(x,most_precisions,label="MostSimilar")
  plt.plot(x,avg_precisions,label="Average")
  plt.plot(x,cold_precisions,label="ColdLaw")
  plt.plot(x,arima_precisions,label="Multi-Arima")
  plt.plot(x,hybrid_precisions,label="Neighbor+Arima")
  plt.plot(x,average_hybrid_precisions,label="Average+Arima")
  plt.title("Precision of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("Precision")
  plt.legend()
  plt.savefig("../img/precision.png")
  #plt.show()
  plt.figure(3)
  plt.plot(x,most_f1s,label="MostSimilar")
  plt.plot(x,avg_f1s,label="Average")
  plt.plot(x,cold_f1s,label="ColdLaw")
  plt.plot(x,arima_f1s,label="Multi-Arima")
  plt.plot(x,hybrid_f1s,label="Neighbor+Arima")
  plt.plot(x,average_hybrid_f1s,label="Average+Arima")
  plt.title("F1-Score of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("F1-Score")
  plt.legend()
  plt.savefig("../img/f1.png")
  #plt.show()
  plt.figure(4)
  plt.plot(x,most_maes,label="MostSimilar")
  plt.plot(x,avg_maes,label="Average")
  plt.plot(x,cold_maes,label="ColdLaw")
  plt.plot(x,arima_maes,label="Multi-Arima")
  plt.plot(x,hybrid_maes,label="Neighbor+Arima")
  plt.plot(x,average_hybrid_maes,label="Average+Hybrid")
  plt.title("MAE of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("MAE")
  plt.legend(loc="lower right")
  plt.savefig("../img/mae.png")
  #plt.show()
  plt.figure(5)
  plt.plot(x,most_rmses,label="MostSimilar")
  plt.plot(x,avg_rmses,label="Average")
  plt.plot(x,cold_rmses,label="ColdLaw")
  plt.plot(x,arima_rmses,label="Multi-Arima")
  plt.plot(x,hybrid_rmses,label="Neighbor+Arima")
  plt.plot(x,average_hybrid_rmses,label="Average+Arima")
  plt.title("RMSE of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("RMSE")
  plt.legend(loc="lower right")
  plt.savefig("../img/rmse.png")
  #plt.show()

  plt.figure(6)
  plt.plot(x,hybrid_recalls,label="Neighbor+Arima")
  plt.plot(x,average_hybrid_recalls,label="Average+Arima")
  plt.plot(x,dis_recalls,label="Dis-Arima")
  plt.plot(x,sd_recalls,label="Sd-Arima")
  plt.plot(x,svm_recalls,label="Sd-SVM")
  plt.title("Recall of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("Recall")
  plt.legend(loc="center right")
  plt.savefig("../img/recall1.png")
  #plt.show()
  plt.figure(7)
  plt.plot(x,hybrid_precisions,label="Neighbor+Arima")
  plt.plot(x,average_hybrid_precisions,label="Average+Arima")
  plt.plot(x,dis_precisions,label="Dis-Arima")
  plt.plot(x,sd_precisions,label="Sd-Arima")
  plt.plot(x,svm_precisions,label="Sd-SVM")
  plt.title("Precision of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("Precision")
  plt.legend()
  plt.savefig("../img/precision1.png")
  #plt.show()
  plt.figure(8)
  plt.plot(x,hybrid_f1s,label="Neighbor+Arima")
  plt.plot(x,average_hybrid_f1s,label="Average+Arima")
  plt.plot(x,dis_f1s,label="Dis-Arima")
  plt.plot(x,sd_f1s,label="Sd-Arima")
  plt.plot(x,svm_f1s,label="Sd-SVM")
  plt.title("F1-Score of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("F1-Score")
  plt.legend()
  plt.savefig("../img/f11.png")
  #plt.show()
  plt.figure(9)
  plt.plot(x,hybrid_maes,label="Neighbor+Arima")
  plt.plot(x,average_hybrid_maes,label="Average+Arima")
  plt.plot(x,dis_maes,label="Dis-Arima")
  plt.plot(x,sd_maes,label="Sd-Arima")
  plt.plot(x,svm_maes,label="Sd-SVM")
  plt.title("MAE of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("MAE")
  plt.legend(loc="lower right")
  plt.savefig("../img/mae1.png")
  #plt.show()
  plt.figure(10)
  plt.plot(x,hybrid_rmses,label="Neighbor+Arima")
  plt.plot(x,average_hybrid_rmses,label="Average+Arima")
  plt.plot(x,dis_rmses,label="Dis-Arima")
  plt.plot(x,sd_rmses,label="Sd-Arima")
  plt.plot(x,svm_rmses,label="Sd-SVM")
  plt.title("RMSE of Different Recommend Algorithms")
  plt.xlabel("Number of recommendations")
  plt.ylabel("RMSE")
  plt.legend(loc="lower right")
  plt.savefig("../img/rmse1.png")
  #plt.show()
  logging.info('I am out showStatistics......')
