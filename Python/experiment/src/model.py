#!/usr/bin python
#coding:utf-8
############################
#give definition of Song and Playlist
############################

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
  def compareWithDict(self,topicDict,simType = 0):
    if simType == 1:
      return cosineSim(self.topicDict,topicDict)
    else:
      return KLSim(self.topicDict,topicDict)
  def compareWithAno(self,another,simType):
    if simType == 1:
      return cosineSim(self.topicDict,another.getTopicDict())
    else:
      return KLSim(self.topicDict,another.getTopicDict())

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
