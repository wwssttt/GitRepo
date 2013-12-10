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
      dotProduct = dotPorduct + topicDict1[key] * topicDict2[key]
      dictPower1 = dictPower1 + topicDict1[key]**2
      dictPower2 = dictPower2 + topicDict2[key]**2
  similarity = dotProduct / (math.sqrt(dictPower1) * math.sqrt(dictPower2))
  return similarity

#define model of song
class Song:
  #define basic attribute of Song
  sid = 0
  topicDict = {}
  #constructor
  def __init__(self,sid,topicDict):
    self.sid = sid
    for key in topicDict.keys():
      self.topicDict[key] = topicDict[key]
  def getTopicDict():
    return self.topicDict
  #get the cosine similarity between self and other song or distribute
  def cosineSimilarity(topicDict):
    return cosineSim(self.topicDict,topicDict)
  def cosineSimilarity(another):
    return cosineSim(self.topicDict,another.getTopicDict())

#define model of playlist
class Playlist:
  #define basic attributes of Playlist
  pid = 0
  trainingList = []
  lastSid = 0
  #constructor
  def __init__(self,pid,playlist):
    self.pid = pid
    count = len(playlist)
    lastSid = playlist[count-1]
    for i in range(0,count-1):
      self.trainingList.append(playlist[i])

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
    print 'There are %d songs have been read.' % len(songs)
    dtFile.close()
    return songDict
  else:
    print 'cannot find doc-topic file......'
  print 'Finish reading songs from doc-topic file......'

#read playlists and construct dict of playlists
def readPlaylistFromDB():
  playlistDict = {}
  effectivePlaylist = DBProcess.getEffectivePlaylist()
  for pid in effectivePlaylist,keys():
    pList = Playlist(pid,effectivePlaylist[pid])
    playlistDict[pid] = pList
  return playlistDict

if __name__ == "__main__":
  readSongFromFile()
