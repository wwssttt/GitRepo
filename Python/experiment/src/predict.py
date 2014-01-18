#!/usr/bin python
#coding:utf-8
############################
#define models of song ans playlist
############################

import math
import sys
import rpy2.robjects as robjects
from rpy2.robjects.packages import importr
from gensim import corpora, models, similarities
import persist
import util
import const
import PrefixSpan
import random

#set default encoding
reload(sys)
sys.setdefaultencoding("utf-8")

#get predicted topic dict of next song by averaging all songs' topic distribution
#we treat it as the user's global preference
def topicDictForNextSongByAverage(playlist,songDict):
  #get playlist's training list
  trainingList = playlist.getTrainingList()
  count = len(trainingList)
  topicDict = {}
  #add each key of every song to topicDict
  for i in range(0,count):
    sid = trainingList[i]
    sTopicDict = songDict[sid].getTopicDict()
    for key in sTopicDict.keys():
      if key not in topicDict:
        topicDict[key] = sTopicDict[key]
      else:
        topicDict[key] = topicDict[key] + sTopicDict[key]
  #average
  for key in topicDict.keys():
    topicDict[key] = topicDict[key] / count
  return topicDict

#get predicted topic dict of next song using most similar to last song
def topicDictForNextSongByMostSimilar(playlist,songDict):
  trainingList = playlist.getTrainingList()
  count = len(trainingList)
  sid = trainingList[count-1]
  return songDict[sid].getTopicDict()

#get predicted topic dict of next song by auto_arima
def topicDictForNextSongByArima(playlist,songDict):
  importr("forecast")
  #get playlist's training list
  trainingList = playlist.getTrainingList()
  count = len(trainingList)
  #predicted topic distribution
  topicDict = {}
  #multi-dimensional time series
  #the number of topics is the dimension
  tsDict = {}
  #loop every song in training list
  #add distribution of sids to tsDict to construct some time series
  for i in range(0,count):
    sid = trainingList[i]
    sTopicDict = songDict[sid].getTopicDict()
    for key in sTopicDict.keys():
      #if the topic do not exist,new a list and append it to dict
      if key not in tsDict:
        tsDict[key] = []
        tsDict[key].append(sTopicDict[key])
      #else append it directly
      else:
        tsDict[key].append(sTopicDict[key])
  #using auto arima to forecast the next value of all time series
  total = 0
  for key in tsDict.keys():
    if total == 0:
      total = len(tsDict[key])
    if len(tsDict[key]) != total:
      print '....Error:Time Series do not have same length......'
      return
    tsList = tsDict[key]
    vec = robjects.FloatVector(tsList)
    ts = robjects.r['ts'](vec)
    fit = robjects.r['auto.arima'](ts)
    next = robjects.r['forecast'](fit,h=1)
    topicDict[key] = float(next.rx('mean')[0][0])
  return topicDict

#get predicted topic dict of next song by most similar hybrid method
def topicDictForNextSongByMostSimilarHybrid(playlist,songDict,arimaDict,lamda):
  trainingList = playlist.getTrainingList()
  pid = playlist.getPid()
  count = len(trainingList)
  sid = trainingList[count-1]
  lastTopicDict =  songDict[sid].getTopicDict()
  arima = arimaDict[pid]
  topicDict = {}
  for topic in lastTopicDict.keys():
    pro = lamda*lastTopicDict[topic] + (1 - lamda)*arima[topic]
    topicDict[topic] = pro
  return topicDict

#get predicted topic dict of next song by average hybrid method
def topicDictForNextSongByAverageHybrid(playlist,songDict,arimaDict,lamda):
  trainingList = playlist.getTrainingList()
  pid = playlist.getPid()
  count = len(trainingList)
  sid = trainingList[count-1]
  avgTopicDict = topicDictForNextSongByAverage(playlist,songDict)
  arima = arimaDict[pid]
  topicDict = {}
  for topic in avgTopicDict.keys():
    pro = lamda*avgTopicDict[topic] + (1 - lamda)*arima[topic]
    topicDict[topic] = pro
  return topicDict

#get recommend songs list of playlist comparing with target dict
def getRecSongs(songDict,topN,tarDict):
  recDict = {}
  for sid in songDict.keys():
    song = songDict[sid]
    topicDict = song.getTopicDict()
    sim = util.similarity(topicDict,tarDict)
    recDict[sid] = sim
  recList = sorted(recDict.iteritems(),key=lambda x:x[1])
  result = []
  for i in range(0,topN):
    result.append(recList[i][0])
  return result

#generate rec dict
#0: most similar
#1: average
#2: Arima
#3: Arima + Similar
#4: Arima + Average
#default: most similar
def getRecDict(playlistDict,songDict,recType = 0,scale = 0,lamda = 0.45,topN = const.TOP_N):
  recDict = {}
  if recType == const.ARIMA or recType == const.ARIMA_SIMILAR or recType == const.ARIMA_AVG:
    arimaDict = persist.readPredictedTopicDictOfArima()
  index = 0
  count = len(playlistDict)
  typeName = util.getMethodName(recType)
  for pid in playlistDict.keys():
    print 'scale = %d >> %s:%d/%d' % (scale,typeName,index,count)
    playlist = playlistDict[pid]
    if recType == const.SIMILAR:
      tarDict = topicDictForNextSongByMostSimilar(playlist,songDict)
    elif recType == const.AVG:
      tarDict = topicDictForNextSongByAverage(playlist,songDict)
    elif recType == const.ARIMA:
      tarDict = arimaDict[pid]
      total = sum(tarDict.values())
      for tid in tarDict.keys():
        tarDict[tid] /= total
      print sum(tarDict.values())
    elif recType == const.ARIMA_SIMILAR:
      tarDict = topicDictForNextSongByMostSimilarHybrid(playlist,songDict,arimaDict,lamda)
    elif recType == const.ARIMA_AVG:
      tarDict = topicDictForNextSongByAverageHybrid(playlist,songDict,arimaDict,lamda)
    else:
      print '%d is an Error Type......' % recType
      return
    recSong = getRecSongs(songDict,topN,tarDict)
    recDict[pid] = recSong
    index = index + 1
  return recDict

#get recommend songs list of playlist by MF
def getRecDictOfMF(songDict,playlistDict,topN = const.TOP_N):
  print 'I am in getRecDictOfMF....'
  index2Id = {}
  for sid in songDict.keys():
    song = songDict[sid]
    sIndex = song.getIndex()
    index2Id[sIndex] = sid

  countMatrix = util.getUserSongMatrix(songDict,playlistDict)
  predictMatrix = util.predictMatrix(countMatrix,const.TOPIC_NUM)
  print 'Begin to generate rec list...'
  recDict = {}
  
  for pid in playlistDict.keys():
    scoreDict = {}
    playlist = playlistDict[pid]
    pIndex = playlist.getIndex()
    trainingList = playlist.getTrainingList()
    predictList = predictMatrix[pIndex]
    count = len(predictList)
    for index in range(count):
      sid = index2Id[index]
      if sid not in trainingList:
        score = predictList[index]
        scoreDict[sid] = score
    recList = sorted(scoreDict.iteritems(),key=lambda x:x[1],reverse=True)
    result = []
    for i in range(0,topN):
      result.append(recList[i][0])
    
    recDict[pid] = result
  print 'I am out getRecDictOfMF....'
  return recDict


#get recommend songs list of playlist by MF
def getRecDictOfUserKNN(songDict,playlistDict,topN = const.TOP_N):
  print 'I am in getRecDictOfUserKNN....'
  countMatrix = util.getUserSongMatrix(songDict,playlistDict)
  simMatrix = util.getUserSimMatrix(songDict,playlistDict)
  print 'Begin to generate rec list...'

  recDict = {}
  
  pid2Index = {}
  for pid in playlistDict.keys():
    playlist = playlistDict[pid]
    pIndex = playlist.getIndex()
    pid2Index[pid] = pIndex

  sid2Index = {}
  for sid in songDict.keys():
    song = songDict[sid]
    sIndex = song.getIndex()
    sid2Index[sid] = sIndex  

  step = 0
  count = len(playlistDict)
  for oPid in playlistDict.keys():
    step += 1
    print 'UserKNN:%d/%d...' % (step,count)
    scoreDict = {}
    oPindex = pid2Index[oPid] 
    for sid in songDict.keys():
      sIndex = sid2Index[sid]
      totalSim = 0
      total = 0
      for iPid in playlistDict.keys():
        iPindex = pid2Index[iPid]
        if oPindex == iPindex:
          continue
        sim = simMatrix[oPindex][iPindex]
        total = total + sim * countMatrix[iPindex][sIndex]
        totalSim += sim
      if totalSim == 0:
        print 'pid = %s,sid = %s,total = %f,totalSim = %f' % (oPid,sid,total,totalSim)
        totalSim = 1
      total = total / totalSim
      scoreDict[sid] = total

    recList = sorted(scoreDict.iteritems(),key=lambda x:x[1],reverse=True)

    result = []
    for i in range(0,topN):
      result.append(recList[i][0])
    
    recDict[oPid] = result

  print 'I am out getRecDictOfUserKNN....'
  return recDict

#get recommend songs list of playlist by MF
def getRecDictOfMostPopular(songDict,playlistDict,topN = const.TOP_N):
  songFreq = {}
  for pid in playlistDict.keys():
    playlist = playlistDict[pid]
    trainingList = playlist.getTrainingList()
    for sid in trainingList:
      if sid not in songFreq:
        songFreq[sid] = 1
      else:
        songFreq[sid] += 1

  songSeq = sorted(songFreq.iteritems(),key=lambda x:x[1],reverse=True)
  result = []
  for i in range(0,topN):
    result.append(songSeq[i][0])
  
  recDict = {}
  for pid in playlistDict.keys():
    recDict[pid] = result

  return recDict

def getRecDictOfMostMarkov(songDict,playlistDict,topN = const.TOP_N):
  sid2Index,index2Id,transitionMatrix = util.getTransitionMatrix(playlistDict,songDict)
  recDict = {}
  index = 0
  playlistSize = len(playlistDict)
  for pid in playlistDict.keys():
    index += 1
    print 'First Order Markov Chain:%d:%d' % (index,playlistSize)
    playlist = playlistDict[pid]
    trainingList = playlist.getTrainingList()
    lastSid = trainingList[-1]
    lastIndex = sid2Index[lastSid]
    transList = transitionMatrix[lastIndex]
    scoreDict = {}
    for i in range(len(transList)):
      sid = index2Id[i]
      scoreDict[sid] = transList[i]
    songList = sorted(scoreDict.iteritems(),key=lambda x:x[1],reverse=True)
    result = []
    for i in range(0,topN):
      result.append(songList[i][0])
    recDict[pid] = result
  return recDict

def getRecDictOfMostPattern(songDict,playlistDict,topN = const.TOP_N):
  predictTopicDict = PrefixSpan.getPredictTopicDict(playlistDict,songDict)
  recDict = {}
  index = 0
  playlistSize = len(playlistDict)
  for pid in playlistDict.keys():
    index += 1
    print 'ContextPattern:%d:%d' % (index,playlistSize)
    predictTopic = predictTopicDict[pid]
    size = len(predictTopic)
    scoreDict = {}
    for sid in songDict.keys():
      song = songDict[sid]
      topicDict = song.getTopicDict()
      score = 0.0
      for tid in predictTopic:
        score += topicDict[tid]
      score = score / size
      scoreDict[sid] = score
    songList = sorted(scoreDict.iteritems(),key=lambda x:x[1],reverse=True)
    result = []
    for i in range(0,topN):
      result.append(songList[i][0])
    recDict[pid] = result
  return recDict
     
#get recommend songs list of playlist by MF
def getRecDictOfLSA(songDict,playlistDict,topN = const.TOP_N):
  #read docs of file to documents
  filename = '../txt/%s_song_Docs.txt' % const.DATASET_NAME
  docFile = open(filename,'r')
  documents = []
  index2Id = {}
  textDict = {}
  index = 0
  while 1:
    line = docFile.readline()
    line = line.rstrip('\n')
    if not line:
      break
    pos = line.find('>>')
    sid = int(line[:pos])
    content = line[(pos+2):]
    index2Id[index] = sid
    textDict[sid] = content
    documents.append(content)
    index += 1
  docFile.close()

  #LSA
  #make every document to a vector of words
  texts = [[word for word in document.lower().split()] for document in documents]
  #map text to id
  dictionary = corpora.Dictionary(texts)
  #map every document to a vector of ids of words
  corpus = [dictionary.doc2bow(text) for text in texts]
  #make a TF-IDF model
  #tfidf = models.TfidfModel(corpus)
  #map every document to a vector of tf-idf
  #corpus_tfidf = tfidf[corpus]
  #make topic models
  lsi = models.LsiModel(corpus,id2word=dictionary,num_topics=const.TOPIC_NUM)
  similarity = similarities.MatrixSimilarity(lsi[corpus])

  recDict = {} 
 
  for pid in playlistDict.keys():
    playlist = playlistDict[pid]
    trainingList = playlist.getTrainingList()
    lastSid = trainingList[-1]
    query = textDict[lastSid]
    query_bow = dictionary.doc2bow(query.lower().split())
    query_lsi = lsi[query_bow]
    sims = similarity[query_lsi]
    sort_sims = sorted(enumerate(sims), key=lambda item: -item[1])
    result = []
    for i in range(0,topN):
      result.append(index2Id[sort_sims[i][0]])
    recDict[pid] = result

  return recDict

#get recommend songs list of playlist by random
def getRecDictOfRandom(songDict,playlistDict,topN = const.TOP_N):
  recDict = {} 
  index2Id = {}
  for sid in songDict.keys():
    song = songDict[sid]
    index = song.getIndex()
    index2Id[index] = sid

  size = len(songDict) - 1

  for pid in playlistDict.keys():
    result = []
    for i in range(0,topN):
      randomIndex = random.randint(0,size)
      result.append(index2Id[randomIndex])
    recDict[pid] = result

  return recDict
