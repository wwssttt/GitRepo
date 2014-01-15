#!/usr/bin python
#coding:utf-8
############################
#store sth to file or read sth from file
############################

import os
import model
import predict
import sys
import const

reload(sys)
sys.setdefaultencoding('utf-8')
#write topic dict of Arima to file to avoid re-computation
def writeTopicDictOfArimaToFile(playlistDict,songDict):
  print 'Begin tp write topic dict to file......'
  filename = "../txt/%s_arima_%d.txt" % (const.DATASET_NAME,const.TOPIC_NUM)
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
    predictTopicDict = predict.topicDictForNextSongByArima(playlist,songDict)
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
  filename = "../txt/%s_arima_%d.txt" % (const.DATASET_NAME,const.TOPIC_NUM)
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
  filename = "../txt/%s_songs-doc-topics_%d_%s.txt" % (const.DATASET_NAME,const.TOPIC_NUM,const.LDA_LIB)
  if os.path.exists(filename):
    songDict = {}
    dtFile = open(filename,"r")
    content = dtFile.readlines()
    count = len(content)
    #loop all lines to construct all songs
    for i in range(0,count):
      items = content[i].rstrip('\n').split('>>')
      sid = int(items[0])
      topicList = eval(items[1])
      topicDict = {}
      for topic in topicList:
        tid = int(topic[0])
        tpro = float(topic[1])
        topicDict[tid] = tpro
      song = model.Song(i,sid,topicDict)
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
  filename = "../txt/%s_playlists.txt" % const.DATASET_NAME
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
  filename = "../txt/%s_playlists.txt" % const.DATASET_NAME
  if not os.path.exists(filename):
    writePlaylistsToFile()
  pFile = open(filename,"r")
  playlistDict = {}
  lines = pFile.readlines()
  pIndex = 0
  for line in lines:
    line = line.rstrip('\n')
    items = line.split(":")
    pid = int(items[0])
    sids = items[1].split(",")
    pList = [int(sid) for sid in sids]
    playlist = model.Playlist(pIndex,pid,pList)
    playlistDict[pid] = playlist
    pIndex += 1
  print 'Thare are %d playlist have been read.' % len(playlistDict)
  return playlistDict

if __name__ == "__main__":
  dirname = '../../../eclipse_workspace/mallet/mallet-2.0.7/data/LDA/'
  for dirpath, dirnames, filenames in os.walk(dirname):
    for filename in filenames:
      if filename.endswith('doc-topics.txt'):
        subIndex = filename.rfind('.txt')
        name = filename[:subIndex]
        topicNum = -1
        tFile = open('%s/%s' % (dirname,filename),'r')
        lines = tFile.readlines()
        count = len(lines)
        for index in range(1,count):
          line = lines[index].rstrip('\n')
          items = line.split()
          itemsCount = len(items)
          sidText = items[1]
          rIndex = sidText.rfind('/')
          sid = int(sidText[rIndex+1:])
          topicDict = {}
          tIndex = 2
          while tIndex < itemsCount:
            tid = int(items[tIndex])
            tIndex += 1
            tpro = float(items[tIndex])
            tIndex += 1
            if tid not in topicDict:
              topicDict[tid] = tpro
          if topicNum == -1:
            topicNum = len(topicDict)
            rFile = open('../txt/%s_%d_mallet.txt' % (name,topicNum),'w')
          if topicNum != len(topicDict):
            print 'Topic Number is Error'
            rFile.close()
            sys.exit()
          result = sorted(topicDict.iteritems(),key=lambda x:x[0],reverse=True)
          rFile.write('%d>>%s\n' % (sid,str(result)))
        rFile.close()
        tFile.close()
