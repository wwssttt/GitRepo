#!/usr/bin python
#coding:utf-8
############################
#give some useful function
############################

import math
import smtplib
from email.mime.text import MIMEText
from hashlib import md5
import sys
import numpy
import const
import persist

reload(sys)
sys.setdefaultencoding('utf-8')

#calculate cosine similarity of two distribution
#input are two topic dicts
#output is the cosine similarity
def cosineSim(topicDict1,topicDict2):
  dotProduct = 0.0
  dictPower1 = 0.0
  dictPower2 = 0.0
  for key in topicDict1.keys():
    if key not in topicDict2:
      print '%d is not in another dict...' % key
      return
    else:
      dotProduct = dotProduct + topicDict1[key] * topicDict2[key]
      dictPower1 = dictPower1 + topicDict1[key]**2
      dictPower2 = dictPower2 + topicDict2[key]**2
  cosSimilarity = dotProduct / (math.sqrt(dictPower1) * math.sqrt(dictPower2))
  return cosSimilarity

#calculate cosine similarity of two users
#input are history list of user
#output is similarity of the two users
def cosineSimOfUser(list1,list2):
  dotProduct = 0.0
  dictPower1 = 0.0
  dictPower2 = 0.0
  count1 = len(list1)
  count2 = len(list2)
  if count1 != count2:
    print 'Two list have different lengths...'
    return -1
  for i in range(count1):
    dotProduct = dotProduct + list1[i] * list2[i]
    dictPower1 = dictPower1 + list1[i]**2
    dictPower2 = dictPower2 + list2[i]**2
  similarity = dotProduct / (math.sqrt(dictPower1) * math.sqrt(dictPower2))
  return similarity

#calculate KL distance of two distribution
#input are two topic dicts
#output is the cosine similarity
def KLDis(topicDict1,topicDict2):
  distance = 0
  for key in topicDict1.keys():
    if key not in topicDict2:
      print '%d is not in another dict...' % key
      return
    else:
      pro1 = topicDict1[key]
      pro2 = topicDict2[key]
      if pro1 <= 0:
        pro1 = 1.0 / 10000000
      if pro2 <= 0:
        pro2 = 1.0 / 10000000
      distance = distance + pro1 * math.log(pro1 / pro2)
  return distance

#calculate KL similarity of two distribution
#input are two topic dicts
#output is the cosine similarity
def KLSim(topicDict1,topicDict2):
  dis1 = KLDis(topicDict1,topicDict2)
  dis2 = KLDis(topicDict2,topicDict1)
  return (dis1 + dis2) / 2.0

#calculate Hellinger distance of two discrete distribution
def HellDis(topicDict1,topicDict2):
  K = len(topicDict1)
  hellDis = 0
  for key in topicDict1.keys():
    if key not in topicDict2:
      print '%d is not in another dict...' % key
      return
    else:
      if topicDict1[key] < 0:
        topicDict1[key] = 1.0 / 10000000
      if topicDict2[key] < 0:
        topicDict2[key] = 1.0 / 10000000
      hellDis += (math.sqrt(topicDict1[key]) - math.sqrt(topicDict2[key]))**2
  hellDis = math.sqrt(hellDis)
  hellDis = hellDis * (1.0/math.sqrt(2))
  return hellDis

#universe interface to calculate similarity of two distributions
def similarity(topicDict1,topicDict2):
  return HellDis(topicDict1,topicDict2)

#calculate recall,preision and F1-Score
def getTopNIndex(recDict,playlistDict,topN = const.TOP_N):
  if topN < 0:
    print 'topN should be > 0'
    return 0
  hit = 0
  testNum = len(playlistDict)
  total = 0
  for pid in playlistDict.keys():
    playlist = playlistDict[pid]
    lastSid = playlist.getLastSid()
    recList = recDict[pid]
    recNum = len(recList)
    if recNum >= topN:
      recNum = topN
    newList = recList[0:recNum]
    total = total + recNum
    if lastSid in newList:
      hit = hit + 1
  recall = float(hit * 1.0) / testNum
  if total == 0:
    total = 1
  precision = float(hit * 1.0) / total
  if recall == 0 and precision == 0:
    f1 = 0
    print 'recall = 0 and precision = 0'
  else:
    f1 = 2 * ((recall * precision) / (recall + precision))
  return recall,precision,f1

#calculate mae and rmse
def getMAEandRMSE(recDict,playlistDict,songDict,topN = const.TOP_N):
  if topN < 0:
    print 'topN should be > 0'
    return 0
  mae = 0
  rmse = 0
  testNum = len(playlistDict)
  for pid in playlistDict.keys():
    playlist = playlistDict[pid]
    lastSid = playlist.getLastSid()
    tarDict = songDict[lastSid].getTopicDict()
    recList = recDict[pid]
    recNum = len(recList)
    if recNum >= topN:
      recNum = topN
    totalError = 0
    for i in range(0,recNum):
      recSid = recList[i]
      recTopicDict = songDict[recSid].getTopicDict()
      recError = KLSim(recTopicDict,tarDict)
      totalError = totalError + recError
    if recNum == 0:
      recNum = 0.0001
    avgError = float(totalError*1.0) / recNum
    mae = mae + math.fabs(avgError)
    rmse = rmse + avgError**2
  mae = mae / testNum
  rmse = rmse / (testNum - 1)
  rmse = math.sqrt(rmse)
  return mae,rmse

#return text info of method
def getMethodName(mid):
  if mid == const.ARIMA:
    return "Arima"
  elif mid == const.SIMILAR:
    return "MostSimilar"
  elif mid == const.AVG:
    return "Average"
  elif mid == const.POPULAR:
    return "MostPopular"
  elif mid == const.ARIMA_SIMILAR:
    return "Arima+Similar"
  elif mid == const.ARIMA_AVG:
    return "Arima+Average"
  elif mid == const.MF:
    return "MF"
  elif mid == const.KNN:
    return "UserKNN"
  elif mid == const.LSA:
    return "LSA"
  elif mid == const.MARKOV:
    return "Markov"
  elif mid == const.PATTERN:
    return "Pattern"
  elif mid == const.RANDOM:
    return "Random"
  else:
    print '%d does not exist......' % mid
    return

#return text info of validation
def getIndexName(index):
  if index == 0:
    return "Hit Ratio"
  elif index == 1:
    return "Precision"
  elif index == 2:
    return "F1-Score"
  elif index == 3:
    return "MAE"
  elif index == 4:
    return "RMSE"
  else:
    print '%d does not exist......' % index
    return

#get MD5
def getMD5(string):
  m = md5()
  m.update(string)
  return m.hexdigest()

#send email to me
def sendMail(to,subtitle,content):
    #定义发送列表
    #mailto_list = ['wwssttt@163.com']
    #设置服务器
    mail_host = 'smtp.163.com'
    mail_port = '25'
    mail_user = 'wwssttt'
    mail_password = 'hxl111wst'
    mail_postfix = '163.com'
    me = mail_user+'<'+mail_user+'@'+mail_postfix+'>'
    msg = MIMEText(content)
    msg['Subject'] = subtitle
    msg['From'] = mail_user+'@'+mail_postfix
    msg['To'] = to
    try:
        send_smtp = smtplib.SMTP()
        send_smtp.connect(mail_host,mail_port)
        send_smtp.login(mail_user,mail_password)
        send_smtp.sendmail(me,to,msg.as_string())
        send_smtp.close()
        print 'success'
        return True
    except Exception as e:
        print(str(e))
        print 'false'

#matrix factorization 
def matrix_factorization(R, P, Q, K, steps=5000, alpha=0.0002, beta=0.02):
    print 'I am in matrix_factorization....'
    Q = Q.T
    for step in xrange(steps):
        for i in xrange(len(R)):
            for j in xrange(len(R[i])):
                if R[i][j] > 0:
                    eij = R[i][j] - numpy.dot(P[i,:],Q[:,j])
                    for k in xrange(K):
                        P[i][k] = P[i][k] + alpha * (2 * eij * Q[k][j] - beta * P[i][k])
                        Q[k][j] = Q[k][j] + alpha * (2 * eij * P[i][k] - beta * Q[k][j])
        eR = numpy.dot(P,Q)
        e = 0
        for i in xrange(len(R)):
            for j in xrange(len(R[i])):
                if R[i][j] > 0:
                    e = e + pow(R[i][j] - numpy.dot(P[i,:],Q[:,j]), 2)
                    for k in xrange(K):
                        e = e + (beta/2) * (pow(P[i][k],2) + pow(Q[k][j],2))
        print 'MF:%d/%d:%f...' % (step,steps,e)
        if e < 0.001:
            break
    print 'I am out matrix_factorization....'
    return P, Q.T

#given a maxtrix R an then user matrix fatorization to make a predicted Matrix
#K is feature number
def predictMatrix(R,K):
  print 'I am in predictMatrix....'
  R1 = numpy.array(R)
  N = len(R1)
  M = len(R1[0])
  
  P = numpy.random.rand(N,K)
  Q = numpy.random.rand(M,K)
 
  nP, nQ = matrix_factorization(R1, P, Q, K)
  nR = numpy.dot(nP, nQ.T)
  print 'I am out predictMatrix....'
  return nR

#construct matrix with songDict and playlistDict
def getUserSongMatrix(songDict,playlistDict):
  print 'I am in getUserSongMatrix......'
  #map id to index
  id2Index = {}
  for sid in songDict.keys():
    song = songDict[sid]
    id2Index[sid] = song.getIndex()
  sCount = len(songDict)
  pCount = len(playlistDict)
  print 'There are %d users and %d songs.' % (pCount,sCount)
  #initail all element to zero
  matrix =[[0 for i in range(sCount)] for j in range(pCount)]
  #fill matrix using playlistDict
  index = 0
  for pid in playlistDict.keys():
    index += 1
    print 'construct count matrix:%d/%d.' % (index,pCount)
    playlist = playlistDict[pid]
    pIndex = playlist.getIndex()
    trainingList = playlist.getTrainingList()
    for sid in trainingList:
      sIndex = id2Index[sid]
      matrix[pIndex][sIndex] += 1
  print 'I am out getUserSongMatrix......'
  return matrix

#construct sim matrix of users
def getUserSimMatrix(songDict,playlistDict):
  print 'I am in getUserSimMatrix....'
  countMatrix = getUserSongMatrix(songDict,playlistDict)
  pCount = len(playlistDict)
  simMatrix = [[-1 for i in range(pCount)] for j in range(pCount)]
  for i in range(pCount):
    for j in range(pCount):
      if simMatrix[i][j] == -1:
        simMatrix[i][j] = cosineSimOfUser(countMatrix[i],countMatrix[j])
        simMatrix[j][i] = simMatrix[i][j]
  print 'I am out getUserSimMatrix....'
  return simMatrix

#construct a dict to represent a song by its dominant topics
def getDominantTopicDict(songDict):
  domDict = {}
  for sid in songDict.keys():
    song = songDict[sid]
    topicDict = song.getTopicDict()
    topicList = sorted(topicDict.iteritems(),key=lambda x:-x[1])
    result = []
    for index in range(len(topicList)):
      tid = int(topicList[index][0])
      tpro = float(topicList[index][1])
      if tpro >= 0.20:
        result.append(tid)
    if len(result) == 0:
      result.append(int(topicList[0][0]))
    domDict[sid] = result

  return domDict

#construct training set of playlist to mining frequent mining
def getPatternTrainingSet(playlistDict,songDict):
  domDict = getDominantTopicDict(songDict)
  patternDict = {}
  for pid in playlistDict.keys():
    playlist = playlistDict[pid]
    trainingList = playlist.getTrainingList()
    patternList = []
    for sid in trainingList[-7:]:
      patternList.append(domDict[sid])
    patternDict[pid] = patternList
  return patternDict

#construct markov chain database
def getTransitionMatrix(playlistDict,songDict):
  print 'I am in constructing transition matrix...'
  sid2Index = {}
  index2Id = {}
  for sid in songDict.keys():
    song = songDict[sid]
    index = song.getIndex()
    sid2Index[sid] = index  
    index2Id[index] = sid

  size = len(songDict)

  matrix = [[0.0 for i in range(size)] for j in range(size)]
  for pid in playlistDict.keys():
    playlist = playlistDict[pid]
    trainingList = playlist.getTrainingList()
    length = len(trainingList)
    #remove last sid
    length -= 1
    #construct frequency matrix
    for index in range(length):
      sid = trainingList[index]
      nextSid = trainingList[index+1]
      curIndex = sid2Index[sid]
      nextIndex = sid2Index[nextSid]
      matrix[curIndex][nextIndex] += 1.0
  #construct transition matrix
  count = 0
  for oIndex in range(size):
    total = sum(matrix[oIndex])
    if total == 0:
      #print index2Id[oIndex]
      count += 1
      continue
    for iIndex in range(size):
      matrix[oIndex][iIndex] = matrix[oIndex][iIndex] / total
  print 'count = ',count
  print 'I am out constructing transition matrix...'
  return sid2Index,index2Id,matrix
