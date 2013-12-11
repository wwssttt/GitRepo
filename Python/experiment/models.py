#!/usr/bin python
#coding:utf-8
############################
#define models of song ans playlist
############################

import math
import os
import DBProcess
import sys

#set default encoding
reload(sys)
sys.setdefaultencoding("utf-8")

#calculate cosine similarity of two distribution
#input are two topic dicts
#output is the cosine similarity
def cosineSim(topicDict1,topicDict2):
  dotProduct = 0
  dictPower1 = 0
  dictPower2 = 0
  for key in topicDict1.keys():
    if key not in topicDict2:
      print '%d is not in another dict...' % key
      return
    else:
      dotProduct = dotProduct + topicDict1[key] * topicDict2[key]
      dictPower1 = dictPower1 + topicDict1[key]**2
      dictPower2 = dictPower2 + topicDict2[key]**2
  similarity = dotProduct / (math.sqrt(dictPower1) * math.sqrt(dictPower2))
  return similarity

#define model of song
class Song:
  #constructor
  def __init__(self,sid,topicDict):
    self.sid = sid
    self.topicDict = {}
    for key in topicDict.keys():
      self.topicDict[key] = topicDict[key]
  def getTopicDict(self):
    return self.topicDict
  def getSid(self):
    return self.sid
  #get the cosine similarity between self and other song or distribute
  def cosineSimilarityWithDict(self,topicDict):
    return cosineSim(self.topicDict,topicDict)
  def cosineSimilarityWithAno(self,another):
    return cosineSim(self.topicDict,another.getTopicDict())

#define model of playlist
class Playlist:
  #constructor
  def __init__(self,pid,playlist):
    self.pid = pid
    count = len(playlist)
    self.lastSid = playlist[count-1]
    self.trainingList = []
    for i in range(0,count-1):
      self.trainingList.append(playlist[i])
  def getTrainingList(self):
    return self.trainingList
  def getPid(self):
    return self.pid
  def getLastSid(self):
    return self.lastSid

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

#return MAE and RMSE of testing set
#mae = sum of abs of predict value and real value and then return sum divide count of testing set
#RMSE = sum of square of similarity and divide N-1 and the sqrt
# predictType means different methods to predict topic dict of next song
#1: average
#2: most similar
#3: cold law
#4: arima
#5: hybrid
def MAEandRMSE(playlistDict,songDict,predictType):
  count = len(playlistDict)
  mae = 0
  rmse = 0
  for pid in playlistDict.keys():
    playlist = playlistDict[pid]
    if predictType == 1:
      predictTopicDict = topicDictForNextSongByAverage(playlist,songDict)
    elif predictType == 2:
      predictTopicDict = topicDictForNextSongByMostSimilar(playlist,songDict)
    elif predictType == 3:
      predictTopicDict = topicDictForNextSongByColdLaw(playlist,songDict)
    elif predictType == 4:
      predictTopicDict = topicDictForNextSongByArima(playlist,songDict)
    elif predictType == 5:
      predictTopicDict = topicDictForNextSongByHybrid(playlist,songDict)
    song = songDict[playlist.getLastSid()]
    mae = mae + math.fabs(song.cosineSimilarityWithDict(predictTopicDict))
    rmse = rmse + song.cosineSimilarityWithDict(predictTopicDict)**2
  mae = mae / count
  rmse = rmse / (count - 1)
  rmse = math.sqrt(rmse)
  return mae,rmse

#read all songs from file and construct them
#output is a dict whose key is sid and value is song object
def readSongFromFile():
  print 'I am reading songs from doc-topic file......'
  filename = "data/LDA/songs-doc-topics.txt"
  if os.path.exists(filename):
    songDict = {}
    dtFile = open(filename,"r")
    content = dtFile.readlines()
    #remove the first extra info
    del content[0]
    count = len(content)
    #loop all lines to construct all songs
    for i in range(0,count):
      items = content[i].rstrip('\n').split()
      rIndex = items[1].rfind('/')
      sid = int(items[1][rIndex+1:])
      del items[0]
      del items[0]
      num = len(items)
      j = 0
      topicDict = {}
      while 1:
        #get tid
        tid = int(items[j])
        #move to next:topic pro
        j = j + 1
        #get topic pro
        tpro = float(items[j])
        #move to next topic pair
        topicDict[tid] = tpro
        j = j + 1
        if j >= num:
          break
      song = Song(sid,topicDict)
      songDict[sid] = song
    print 'There are %d songs have been read.' % len(songDict)
    dtFile.close()
    print 'Finish reading songs from doc-topic file......'
    return songDict
  else:
    print 'cannot find doc-topic file......'

#read playlists and construct dict of playlists
def readPlaylistFromDB():
  playlistDict = {}
  effectivePlaylist = DBProcess.getEffectivePlaylist()
  for pid in effectivePlaylist.keys():
    pList = Playlist(pid,effectivePlaylist[pid])
    playlistDict[pid] = pList
  print 'Thare are %d playlist have been read.' % len(playlistDict)
  return playlistDict

if __name__ == "__main__":
  songDict = readSongFromFile()
  playlistDict = readPlaylistFromDB()
  print '################Average####################'
  mae,rmse = MAEandRMSE(playlistDict,songDict,1)
  print 'MAE = ',mae
  print 'RMSE = ',rmse
  print '################Most Similar####################'
  mae,rmse = MAEandRMSE(playlistDict,songDict,2)
  print 'MAE = ',mae
  print 'RMSE = ',rmse
