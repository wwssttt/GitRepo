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
    return "MTSA"
  elif mid == const.SIMILAR:
    return "MostSimilar"
  elif mid == const.AVG:
    return "Average"
  elif mid == const.ARIMA_SIMILAR:
    return "Arima+Similar"
  elif mid == const.ARIMA_AVG:
    return "Arima+Average"
  elif mid == const.KNN:
    return "UserKNN"
  elif mid == const.MARKOV:
    return "1st-Markov"
  elif mid == const.PATTERN:
    return "PatternMining"
  elif mid == const.MARKOV_3:
    return "3rd-Markov"
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
def matrix_factorization(R, P, Q, K, steps=1000, alpha=0.02, beta=0.02):
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
def getUserSongMatrix(allPlaylist,songDict):
  print 'I am in getUserSongMatrix......'
  #map id to index
  id2Index = {}
  pid2Index = {}
  index2Pid = {}
  for sid in songDict.keys():
    song = songDict[sid]
    id2Index[sid] = song.getIndex()
  sCount = len(songDict)
  pCount = 0 
  for scale in range(10):
    pCount += len(allPlaylist[scale])

  print 'There are %d users and %d songs.' % (pCount,sCount)

  #initail all element to zero
  matrix =[[0 for i in range(sCount)] for j in range(pCount)]
  #construct the matrix
  pIndex = 0
  for scale in range(10):
    playlistDict = allPlaylist[scale]
    for pid in playlistDict.keys():
      playlist = playlistDict[pid]
      trainingList = playlist.getTrainingList()
      pid2Index[pid] = pIndex
      index2Pid[pIndex] = pid
      for sid in trainingList:
        sIndex = id2Index[sid]
        matrix[pIndex][sIndex] += 1
      pIndex += 1
  print 'I am out getUserSongMatrix......'
  return pid2Index,index2Pid,matrix

#construct sim matrix of users
def getUserSimMatrix(countMatrix,pid2Index):
  print 'I am in getUserSimMatrix....'
  pCount = len(pid2Index)
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
def getPatternTrainingSet(allPlaylist,songDict,scale):
  domDict = getDominantTopicDict(songDict)
  allTrainingPattern = []
  testingPatternDict = {}
  for part in range(10):
    if part == scale:
      continue
    playlistDict = allPlaylist[part]
    for pid in playlistDict.keys():
      playlist = playlistDict[pid]
      trainingList = playlist.getTrainingList()
      patternList = []
      for sid in trainingList:
        patternList.append(domDict[sid])
      lastSid = playlist.getLastSid()
      patternList.append(domDict[lastSid])

      allTrainingPattern.append(patternList)
     
  #testing
  playlistDict = allPlaylist[scale]
  for pid in playlistDict.keys():
    playlist = playlistDict[pid]
    trainingList = playlist.getTrainingList()
    patternList = []
    for sid in trainingList[-8:]:
      patternList.append(domDict[sid])
    
    testingPatternDict[pid] = patternList  

  return allTrainingPattern,testingPatternDict

#construct markov chain database
def getTransitionMatrix(allPlaylist,songDict,scale):
  domDict = getDominantTopicDict(songDict)
  print 'I am in constructing transition matrix...'
  transMatrix = [[1.0 for i in range(const.TOPIC_NUM)] for j in range(const.TOPIC_NUM)]

  size = len(songDict)
  
  for part in range(10):
    if part == scale:
      continue
    playlistDict = allPlaylist[part]
    for pid in playlistDict.keys():
      playlist = playlistDict[pid]
      trainingList = playlist.getTrainingList()
      length = len(trainingList)
      #remove last sid
      length -= 1
      #construct frequency matrix
      for index in range(length):
        sid = trainingList[index]
        topicList = domDict[sid]
        nextSid = trainingList[index+1]
        nextTopicList = domDict[nextSid]
        for i in range(len(topicList)):
          tid = topicList[i]
          for j in range(len(nextTopicList)):
            nextTid = nextTopicList[j]
            transMatrix[tid][nextTid] += 1.0

      sid = trainingList[length]
      topicList = domDict[sid]
      nextSid = playlist.getLastSid()
      nextTopicList = domDict[nextSid]
      for i in range(len(topicList)):
        tid = topicList[i]
        for j in range(len(nextTopicList)):
          nextTid = nextTopicList[j]
          transMatrix[tid][nextTid] += 1.0

  #construct transition matrix
  for tidIndex in range(const.TOPIC_NUM):
    total = sum(transMatrix[tidIndex])
    for nextTidIndex in range(const.TOPIC_NUM):
      transMatrix[tidIndex][nextTidIndex] = transMatrix[tidIndex][nextTidIndex] / total

  #print transMatrix

  print 'I am out constructing transition matrix...'
  return domDict,transMatrix

#construct markov chain database:3-order
def getThreeOrderTransitionMatrix(allPlaylist,songDict,scale):
  domDict = getDominantTopicDict(songDict)
  print 'I am in constructing transition matrix...'
  transDict = {}

  size = len(songDict)
  
  for part in range(10):
    if part == scale:
      continue
    playlistDict = allPlaylist[part]
    for pid in playlistDict.keys():
      playlist = playlistDict[pid]
      trainingList = playlist.getTrainingList()
      length = len(trainingList)
      #remove last sid
      length -= 3
      #construct frequency matrix
      for index in range(length):
        startSid = trainingList[index]
        startTopicList = domDict[startSid]
        secondSid = trainingList[index+1]
        secondTopicList = domDict[secondSid]
        threeSid = trainingList[index+2]
        threeTopicList = domDict[threeSid]
        targetSid = trainingList[index+3]
        targetTopicList = domDict[targetSid]
        for i in range(len(startTopicList)):
          for j in range(len(secondTopicList)):
            for s in range(len(threeTopicList)):
              key = '%d#%d#%d' % (startTopicList[i],secondTopicList[j],threeTopicList[s])
              if key not in transDict:
                transDict[key] = {}
              for t in range(len(targetTopicList)):
                tid = targetTopicList[t]
                if tid not in transDict[key]:
                  transDict[key][tid] = 1.0
                transDict[key][tid] += 1.0


      startSid = trainingList[length]
      startTopicList = domDict[startSid]
      secondSid = trainingList[length+1]
      secondTopicList = domDict[secondSid]
      threeSid = trainingList[length+2]
      threeTopicList = domDict[threeSid]
      targetSid = playlist.getLastSid()
      targetTopicList = domDict[targetSid]
      for i in range(len(startTopicList)):
        for j in range(len(secondTopicList)):
          for s in range(len(threeTopicList)):
            key = '%d#%d#%d' % (startTopicList[i],secondTopicList[j],threeTopicList[s])
            if key not in transDict:
              transDict[key] = {}
            for t in range(len(targetTopicList)):
              tid = targetTopicList[t]
              if tid not in transDict[key]:
                transDict[key][tid] = 1.0
              transDict[key][tid] += 1.0

  #construct transition matrix
  for key in transDict.keys():
    total = sum(transDict[key].values())
    for subKey in transDict[key].keys():
      transDict[key][subKey] = transDict[key][subKey] / total

  #print transMatrix

  print 'I am out constructing transition matrix...'
  return domDict,transDict
