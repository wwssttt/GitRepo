#!/usr/bin/python
# -*- coding:utf-8 -*-
"""Different methods to predict next song of a playlist.
   Dependencies:
     persist.
     util.
     const.
     PrefixSpan.
"""
__author__ = 'Jason Wong'
__version__ = '1.0'

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
import logging
import os

# set default encoding
reload(sys)
sys.setdefaultencoding("utf-8")

# set log's localtion and level
logging.basicConfig(filename=os.path.join(os.getcwd(),'../log/predict_log_%s.txt' % const.DATASET_NAME),level=logging.DEBUG,format='%(message)s')

def topicDictForNextSongByAverage(playlist,songDict):
  """Predict topic dict of next song by averaging all songs' topic distribution.
     Here, we treat it as the user's global preference.
     Input:
       playlist - the playlist to be predicted.
       songDict - song dictionaries with sid as key and Song as value.
     Output:
       topicDict - predicted topic dict the next song.
  """
  # get playlist's training list
  trainingList = playlist.getTrainingList()
  count = len(trainingList)
  topicDict = {}
  # add each key of every song to topicDict
  for i in range(0,count):
    sid = trainingList[i]
    sTopicDict = songDict[sid].getTopicDict()
    for key in sTopicDict.keys():
      if key not in topicDict:
        topicDict[key] = sTopicDict[key]
      else:
        topicDict[key] = topicDict[key] + sTopicDict[key]
  # average
  for key in topicDict.keys():
    topicDict[key] = topicDict[key] / count
  return topicDict

def topicDictForNextSongByMostSimilar(playlist,songDict):
  """Predict topic dict of next song using most similar to last song.
     Here, we treat it as the user's local preference.
     Input:
       playlist - the playlist to be predicted.
       songDict - song dictionaries with sid as key and Song as value.
     Output:
       topicDict - predicted topic dict the next song.
  """
  trainingList = playlist.getTrainingList()
  count = len(trainingList)
  sid = trainingList[count-1]
  return songDict[sid].getTopicDict()

def topicDictForNextSongByArima(playlist,songDict,maxLength):
  """Predict topic dict of next song using auto_arima.
     Here, we treat it as the user's sequential preference.
     Input:
       playlist - the playlist to be predicted.
       songDict - song dictionaries with sid as key and Song as value.
       maxLength - max window size.
     Output:
       topicDict - predicted topic dict the next song.
  """
  importr("forecast")
  # get playlist's training list
  trainingList = playlist.getTrainingList()
  # cut the trainingList
  if maxLength != -1 and len(trainingList) >= maxLength:
      trainingList = trainingList[-maxLength:]
  count = len(trainingList)
  print 'count = ',count
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

def topicDictForNextSongByMostSimilarHybrid(playlist,songDict,arimaDict):
  """Predict topic dict of next song by combining sequential and local.
     Input:
       playlist - the playlist to be predicted.
       songDict - song dictionaries with sid as key and Song as value.
       arimaDict - predicted topic dict using auto_arima.
     Output:
       topicDict - predicted topic dict the next song.
  """
  trainingList = playlist.getTrainingList()
  pid = playlist.getPid()
  count = len(trainingList)

  lamda = count / (count + 10.0)

  sid = trainingList[count-1]
  lastTopicDict =  songDict[sid].getTopicDict()
  arima = arimaDict[pid]
  topicDict = {}
  for topic in lastTopicDict.keys():
    pro = (1.0 - lamda)*lastTopicDict[topic] + lamda*arima[topic]
    topicDict[topic] = pro
  return topicDict

def topicDictForNextSongByAverageHybrid(playlist,songDict,arimaDict):
  """Predict topic dict of next song by combining sequential and global.
     Input:
       playlist - the playlist to be predicted.
       songDict - song dictionaries with sid as key and Song as value.
       arimaDict - predicted topic dict using auto_arima.
     Output:
       topicDict - predicted topic dict the next song.
  """
  trainingList = playlist.getTrainingList()
  pid = playlist.getPid()
  count = len(trainingList)

  lamda = count / (count + 10.0)

  sid = trainingList[count-1]
  avgTopicDict = topicDictForNextSongByAverage(playlist,songDict)
  arima = arimaDict[pid]
  topicDict = {}
  for topic in avgTopicDict.keys():
    pro = (1.0 - lamda)*avgTopicDict[topic] + lamda*arima[topic]
    topicDict[topic] = pro
  return topicDict

def topicDictForNextSongByAllHybrid(playlist,songDict,arimaDict):
  """Predict topic dict of next song by combining sequential/local/global.
     Input:
       playlist - the playlist to be predicted.
       songDict - song dictionaries with sid as key and Song as value.
       arimaDict - predicted topic dict using auto_arima.
     Output:
       topicDict - predicted topic dict the next song.
  """
  trainingList = playlist.getTrainingList()
  pid = playlist.getPid()
  count = len(trainingList)

  #lamda = (count - 5.0) / (count + 10.0)
  lamda = math.log(count) - 0.75
  if lamda > 1.0:
    lamda = 1.0
  alpha = 0.75 * (1-lamda)
  beta = 0.25 * (1-lamda)

  sid = trainingList[count-1]
  
  avgTopicDict = topicDictForNextSongByAverage(playlist,songDict)
  lastTopicDict =  songDict[sid].getTopicDict()
  arima = arimaDict[pid]
  
  lastSid = playlist.getLastSid()
  trueDict = songDict[lastSid].getTopicDict()

  logging.info("pid = %s" % pid)
  logging.info("count = %s" % count)
  logging.info("true = %s" % str(trueDict)) 
  logging.info("arima = %s" % str(arima)) 
  logging.info("avg = %s" % str(avgTopicDict)) 
  logging.info("similar = %s" % str(lastTopicDict))  

  topicDict = {}
  for topic in avgTopicDict.keys():
    pro = alpha*lastTopicDict[topic] \
          +beta*avgTopicDict[topic] \
          + lamda*arima[topic]
    topicDict[topic] = pro
  return topicDict

def getRecSongs(songDict,topN,tarDict):
  """Get recommended songs list of a playlist comparing with target dict.
     Input:
       songDict - song dictionaries with sid as key and Song as value.
       topN - count of recommendation.
       tarDict - predicted topic dict.
     Output:
       result - a list of songs.
  """
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

def getRecDict(playlistDict,songDict,recType = 0,scale = 0,topN = const.TOP_N
               ,maxLength = -1):
  """Get recommendations of all playlists using different method.
     Input:
       playlistDict - dict of playlist of specific scale.
       songDict - all songs dicts.
       recType - type of recommender.
       scale - scale of current playlist.
       topN - count of recommendations.
       maxLength - max window size.
     Output:
       recDict - dict of recommendations 
                 with pid as key and recommendation list as value.
  """
  recDict = {}
  # reuse arima results
  if recType == const.ARIMA or recType == const.ARIMA_SIMILAR \
                or recType == const.ARIMA_AVG or recType == const.ALL_HYBRID:
    arimaDict = persist.readPredictedTopicDictOfArima(playlistDict,songDict,
                                                      scale,maxLength)
  index = 0
  count = len(playlistDict)
  typeName = util.getMethodName(recType)
  # predict next song of each playlist
  for pid in playlistDict.keys():
    print 'length = %d:scale = %d >> %s:%d/%d' \
          % (maxLength,scale,typeName,index,count)
    playlist = playlistDict[pid]
    if recType == const.SIMILAR:
      tarDict = topicDictForNextSongByMostSimilar(playlist,songDict)
    elif recType == const.AVG:
      tarDict = topicDictForNextSongByAverage(playlist,songDict)
    elif recType == const.ARIMA:
      tarDict = arimaDict[pid]
    elif recType == const.ARIMA_SIMILAR:
      tarDict = topicDictForNextSongByMostSimilarHybrid(playlist,
                                                        songDict,
                                                        arimaDict)
    elif recType == const.ARIMA_AVG:
      tarDict = topicDictForNextSongByAverageHybrid(playlist,
                                                    songDict,
                                                    arimaDict)
    elif recType == const.ALL_HYBRID:
      tarDict = topicDictForNextSongByAllHybrid(playlist,
                                                songDict,
                                                arimaDict)
    else:
      print '%d is an Error Type......' % recType
      return
    recSong = getRecSongs(songDict,topN,tarDict)
    recDict[pid] = recSong
    index = index + 1
  return recDict

def getRecDictOfUserKNN(playlistDict,songDict,scale,pid2Index,countMatrix,
                        simMatrix,topN = const.TOP_N):
  """Get recommendations of all playlists using UserKNN recommender.
     Input:
       playlistDict - dict of playlist of specific scale.
       songDict - dict of all songs.
       scale - scale of current playlists.
       pid2Index - dict with pid as key and index as value.
       countMatrix - user-song matrix.
       simMatrix - user similarity matrix.
       topN - how many recommendations.
     Output:
       recDict - dict of recommendations 
                 with pid as key and recommendation list as value.
  """
  print 'Begin to generate rec list...'

  recDict = {}

  sid2Index = {}
  for sid in songDict.keys():
    song = songDict[sid]
    sIndex = song.getIndex()
    sid2Index[sid] = sIndex  

  step = 0
  count = len(playlistDict)
  for oPid in playlistDict.keys():
    step += 1
    print 'scale = %d >> UserKNN:%d/%d...' % (scale,step,count)
    scoreDict = {}
    oPindex = pid2Index[oPid] 
    for sid in songDict.keys():
      sIndex = sid2Index[sid]
      totalSim = 0.0
      total = 0.0
      for iPindex in range(len(pid2Index)):
        if oPindex == iPindex:
          continue
        if oPindex >= iPindex:
          sim = simMatrix[oPindex][iPindex]
        else:
          sim = simMatrix[iPindex][oPindex]
        total = total + sim * countMatrix[iPindex][sIndex]
        totalSim += sim
      if totalSim == 0:
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

def getRecDictOfFirstMarkov(allPlaylist,songDict,scale,topN = const.TOP_N):
  """Get recommendations of all playlists using 1st-Markov recommender.
     Input:
       allPlaylist - all playlist.
       songDict - dict of all songs.
       scale - scale of current playlists.
       topN - how many recommendations.
     Output:
       recDict - dict of recommendations 
                 with pid as key and recommendation list as value.
  """
  domDict,transMatrix = util.getTransitionMatrix(allPlaylist,songDict,scale)
  recDict = {}
  index = 0
  playlistDict = allPlaylist[scale]
  playlistSize = len(playlistDict)
  for pid in playlistDict.keys():
    index += 1
    print 'scale = %d >> First Order Markov Chain:%d:%d' \
          % (scale,index,playlistSize)
    playlist = playlistDict[pid]
    trainingList = playlist.getTrainingList()
    lastSid = trainingList[-1]
    topicList = domDict[lastSid]
    scoreDict = {}
    for sid in songDict.keys():
      song = songDict[sid]
      topicDict = song.getTopicDict()
      curTopicList = domDict[sid]
      if sid not in scoreDict:
        scoreDict[sid] = 0.0
      for i in range(len(topicList)):
        oTid = topicList[i]
        for j in range(len(curTopicList)):
          iTid = curTopicList[j]
          scoreDict[sid] = scoreDict[sid] \
                           + transMatrix[oTid][iTid]*topicDict[iTid]
    songList = sorted(scoreDict.iteritems(),key=lambda x:x[1],reverse=True)
    result = []
    for i in range(0,topN):
      result.append(songList[i][0])
    recDict[pid] = result
  return recDict

def getRecDictOfThreeOrderMarkov(allPlaylist,
                                 songDict,
                                 scale,
                                 topN = const.TOP_N):
  """Get recommendations of all playlists using 3rd-Markov recommender.
     Input:
       allPlaylist - all playlist.
       songDict - dict of all songs.
       scale - scale of current playlists.
       topN - how many recommendations.
     Output:
       recDict - dict of recommendations 
                 with pid as key and recommendation list as value.
  """
  domDict,transDict = util.getThreeOrderTransitionMatrix(allPlaylist,
                                                         songDict,
                                                         scale)
  recDict = {}
  index = 0
  playlistDict = allPlaylist[scale]
  playlistSize = len(playlistDict)
  for pid in playlistDict.keys():
    index += 1
    print 'scale = %d >> Three Order Markov Chain:%d:%d' \
           % (scale,index,playlistSize)
    playlist = playlistDict[pid]
    trainingList = playlist.getTrainingList()
    sidList = trainingList[-3:]
    startTopicList = domDict[sidList[0]]
    secondTopicList = domDict[sidList[1]]
    threeTopicList = domDict[sidList[2]]
    topicList = []
    for i in range(len(startTopicList)):
      for j in range(len(secondTopicList)):
        for s in range(len(threeTopicList)):
          key = '%d#%d#%d' \
                % (startTopicList[i],secondTopicList[j],threeTopicList[s])
          if key not in topicList:
            topicList.append(key)

    scoreDict = {}
    for sid in songDict.keys():
      song = songDict[sid]
      topicDict = song.getTopicDict()
      curTopicList = domDict[sid]
      if sid not in scoreDict:
        scoreDict[sid] = 0.0
      for i in range(len(topicList)):
        topicKey = topicList[i]
        if topicKey not in transDict:
          continue
        for j in range(len(curTopicList)):
          tid = curTopicList[j]
          if tid not in transDict[topicKey]:
            scoreDict[sid] += 0.0
          else:
            scoreDict[sid] = scoreDict[sid] \
                             + topicDict[tid]*transDict[topicKey][tid]
    songList = sorted(scoreDict.iteritems(),key=lambda x:x[1],reverse=True)
    result = []
    for i in range(0,topN):
      result.append(songList[i][0])
    recDict[pid] = result
  return recDict

def getRecDictOfMostPattern(allPlaylist,songDict,scale,topN = const.TOP_N):
  """Get recommendations of all playlists using PatternMining recommender.
     Input:
       allPlaylist - all playlist.
       songDict - dict of all songs.
       scale - scale of current playlists.
       topN - how many recommendations.
     Output:
       recDict - dict of recommendations 
                 with pid as key and recommendation list as value.
  """
  predictTopicDict = PrefixSpan.getPredictTopicDict(allPlaylist,songDict,scale)
  recDict = {}
  index = 0
  playlistDict = allPlaylist[scale]
  playlistSize = len(playlistDict)
  for pid in playlistDict.keys():
    index += 1
    print 'scale = %d >> ContextPattern:%d:%d' % (scale,index,playlistSize)
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
