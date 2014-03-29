# -*- coding:utf-8 -*-
"""Give definition of class Song and Playlist.
   Dependencies:
     util.
"""
__author__ = 'Jason Wong'
__version__ = '1.0'

import math
import sys

# set default encoding
reload(sys)
sys.setdefaultencoding('utf-8')

class Song:
  """Definition of Song.
  """

  def __init__(self,sIndex,sid,topicDict):
    """initialize a Song object.
       Input:
         sIndex - index of the song.
         sid - id of the song.
         topicDict - topic dictionary of the song 
                     with tid as key and probability as value.
       Output:
         None.
    """
    self.sIndex = sIndex
    self.sid = sid
    self.topicDict = {}
    for key in topicDict.keys():
      self.topicDict[key] = topicDict[key]

  def getTopicDict(self):
    """Get the topic dictionary of the song.
       Input:
         None.
       Output:
         topic dictionary of the song.
    """
    return self.topicDict

  def getSid(self):
    """Get the id of the song.
       Input:
         None.
       Output:
         id of the song.
    """
    return self.sid

  def getIndex(self):
    """Get the index of the song.
       Input:
         None.
       Output:
         index of the song.
    """
    return self.sIndex

  def compareWithDict(self,topicDict):
    """Get the similarity between self and other distribute.
       Input:
         topicDict - topic dictionary of a song.
       Output:
         similarity between two distributions.
    """
    return util.similarity(self.topicDict,topicDict)

  def compareWithAno(self,another,simType):
    """Get the similarity between self and other song.
       Input:
         topicDict - topic dictionary of a song.
       Output:
         similarity between two songs.
    """
    return util.similarity(self.topicDict,another.getTopicDict())


class Playlist:
  """Definition of Playlist.
  """
  def __init__(self,pid,scale,playlist):
    """initialize a Playlist object.
       Input:
         pid - id of the playlist.
         scale - scale of the playlist.
         playlist - list of the playlist.
       Output:
         None.
    """
    self.scale = scale
    self.pid = pid
    count = len(playlist)
    self.lastSid = playlist[count-1] # get the last song
    self.trainingList = [] # get the training list
    for i in range(0,count-1):
      self.trainingList.append(playlist[i])

  def getTrainingList(self):
    """Get the training list of the playlist.
       Input:
         None.
       Output:
         training list of the playlist.
    """
    return self.trainingList

  def getScale(self):
    """Get the scale of the playlist.
       Input:
         None.
       Output:
         scale of the playlist.
    """
    return self.scale

  def getPid(self):
    """Get id of the playlist.
       Input:
         None.
       Output:
         id of the playlist.
    """
    return self.pid

  def getLastSid(self):
    """Get last sid of the playlist.
       Input:
         None.
       Output:
         last sid of the playlist.
    """
    return self.lastSid
