#!/usr/bin python
#coding:utf-8
############################
#store sth to file or read sth from file
############################

import os
import model

#write topic dict of Arima to file to avoid re-computation
def writeTopicDictOfArimaToFile(playlistDict,songDict):
  print 'Begin tp write topic dict to file......'
  filename = "../txt/arima.txt"
  if os.path.exists(filename):
    print '%s is existing......' % filename
    return
  print 'Begin to write topic dict to file......'
  aFile = open(filename,"w")
  index = 0
  many = len(playlistDict)
  for pid in playlistDict.keys():
    print '%d/%d' % (index,many)
    index += 1
    playlist = playlistDict[pid]
    predictTopicDict = topicDictForNextSongByArima(playlist,songDict)
    content = '%d#' % pid
    for topic in predictTopicDict.keys():
      content = '%s%d:%f,' % (content,topic,predictTopicDict[topic])
    content = content[:len(content)-1]
    aFile.write(content+"\n")
  aFile.close()
  print 'End of writing topic dict to file......'

#read Predicted Topic Dict Of Arima
def readPredictedTopicDictOfArima():
  print 'I am reading predicted topic dict of arima......'
  filename = "../txt/arima.txt"
  if not os.path.exists(filename):
    playlistDict = readPlaylistFromFile()
    songDict = readSongFromFile()
    writeTopicDictOfArimaToFile(playlistDict,songDict)
  predictDict = {}
  aFile = open(filename,"r")
  lines = aFile.readlines()
  for line in lines:
    line = line.rstrip("\n")
    items = line.split("#")
    pid = int(items[0])
    topicDict = {}
    topics = items[1].split(",")
    for topic in topics:
      info = topic.split(":")
      tid = int(info[0])
      pro = float(info[1])
      topicDict[tid] = pro
    #normalize
    #make sum of pro equals to 1
    proSum = sum(topicDict.values())
    for tid in topicDict.keys():
      topicDict[tid] = topicDict[tid] / proSum
    predictDict[pid] = topicDict
  print 'Finish reading predicted topic dict of arima......'
  return predictDict

#read all songs from file and construct them
#output is a dict whose key is sid and value is song object
def readSongFromFile():
  print 'I am reading songs from doc-topic file......'
  filename = "../txt/songs-doc-topics.txt"
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
        if tpro == 0:
          print 'pro of topic must bigger than 0......'
          return
        #move to next topic pair
        topicDict[tid] = tpro
        j = j + 1
        if j >= num:
          break
      song = model.Song(sid,topicDict)
      songDict[sid] = song
    print 'There are %d songs have been read.' % len(songDict)
    dtFile.close()
    print 'Finish reading songs from doc-topic file......'
    return songDict
  else:
    print 'cannot find doc-topic file......'

#read playlists from db and construct dict of playlists
def readPlaylistFromDB():
  playlistDict = {}
  effectivePlaylist = DBProcess.getEffectivePlaylist()
  for pid in effectivePlaylist.keys():
    pList = Playlist(pid,effectivePlaylist[pid])
    playlistDict[pid] = pList
  print 'Thare are %d playlist have been read.' % len(playlistDict)
  return playlistDict

#write playlists to file
def writePlaylistsToFile():
  filename = "../txt/playlists.txt"
  if os.path.exists(filename):
    print '%s is existing......' % filename
    return
  else:
    print 'Begin to write playlists......'
    pFile = open(filename,"w")
    effectivePlaylist = DBProcess.getEffectivePlaylist()
    for pid in effectivePlaylist.keys():
      pList = effectivePlaylist[pid]
      content = "%d:" % pid
      count = len(pList)
      for i in range(0,count-1):
        content = "%s%d," % (content,pList[i])
      content = "%s%d" % (content,pList[count-1])
      pFile.write(content+'\n')
    pFile.close()
    print 'End of writing playlists......'

#read playlists from file and construct dict of playlists
def readPlaylistFromFile():
  filename = "../txt/playlists.txt"
  if not os.path.exists(filename):
    writePlaylistsToFile()
  pFile = open(filename,"r")
  playlistDict = {}
  lines = pFile.readlines()
  for line in lines:
    line = line.rstrip('\n')
    items = line.split(":")
    pid = int(items[0])
    sids = items[1].split(",")
    pList = [int(sid) for sid in sids]
    playlist = model.Playlist(pid,pList)
    playlistDict[pid] = playlist
  print 'Thare are %d playlist have been read.' % len(playlistDict)
  return playlistDict
