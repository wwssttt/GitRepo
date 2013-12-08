#!/usr/bin python
#coding=utf-8
############################
# @author Jason Wong
# @date 2013-12-08
############################
# connect to aotm db
# generate documents of songs
############################

import MySQLdb
import sys
import numpy
import pylab as pl
import logging
import os
from nltk.stem.lancaster import LancasterStemmer
import DBProcess

# reload sys and set encoding to utf-8
reload(sys)
sys.setdefaultencoding('utf-8')
# set log's localtion and level
logging.basicConfig(filename=os.path.join(os.getcwd(),'docgenerate_log.txt'),level=logging.DEBUG,format='%(asctime)s-%(levelname)s:%(message)s')

# define some global varibale
DBHOST = 'localhost'
DBUSER = 'root'
DBPWD = 'wst'
DBPORT = 3306
DBNAME = 'aotm'
DBCHARSET = 'utf8'

#read stop words from stop words file
# return stop words list
def readStopwordsFromFile(filename):
  words = []

  stopfile = open(filename,"r")
  while 1:
    line = stopfile.readline()
    if not line:
      break
    line = line.rstrip('\n')
    line = line.strip()
    line = line.lower()
    if line not in words:
      words.append(line)
  stopfile.close()
  return words

#combine two stop words lists
#return a combined stopwords list/file
def combineTwoStopwordsFile():
  if os.path.exists("stopwords.txt"):
    print "stop words file is existing......"
    return
  first = readStopwordsFromFile("EnglishStopWords_datatang.txt")
  second = readStopwordsFromFile("EnglishStopWords_url.txt")
  result = list(set(first).union(set(second)))
  rFile = open("stopwords.txt","w")
  for word in result:
    rFile.write(word+'\n')
  rFile.close()
  return result

#get stopwords list
stopwords = readStopwordsFromFile("stopwords.txt")
vocabulary = {}

#add word to word dictionary
#tagDict:word dictionary of a song(word:count)
#tagStr:tag string
#tagCount:the count of tag's appearance
def addItemToDict(tagDict,tagStr,tagCount):
  global vocabulary
  st = LancasterStemmer()
  items = tagStr.split()
  for item in items:
    item = item.lower()
    item = st.stem(item)
    if item not in stopwords:
      if item not in tagDict:
        tagDict[item] = tagCount
      else:
        tagDict = tagDict[item] + tagCount
      if item not in vocabulary:
        vocalbulary[item] = 1
      else:
        vocabulary[item] = vocabulary[item] + 1

#generate tag dictionary of given song
def generateTagDictofSong(sname,aname,tags):
  tagDict = {}
  addItemToDict(tagDict,sname,50)
  addItemToDict(tagDict,aname,50)
  tagInfos = tags.split("##==##")
  for tagInfo in tagInfos:
    items = tagInfo.split(":")
    tagStr = items[0]
    tagCount = int(items[1])
    addItemToDict(tagDict,tagStr,tagCount)
  return tagDict

#generate document of given song from its tagDict
def generateDocofSong(tagDict):
  if os.path.exists("songs/%d" % sid):
    print '%d is existing...' % sid
    logging.warning('%d is existing...' % sid)
    return
  sFile = open("songs/%d" % sid, "w")
  for tag in tagDict.keys():
    count = tagDict[tag] / 25
    content = ""
    for i in range(0,count):
      content = "%s %s" % (content,tag)
      sFile.write(content+'\n')
  sFile.close()

def statisticsOfSongDB():
  global DBHOST
  global DBUSER
  global DBPWD
  global DBPORT
  global DBNAME
  global DBCHARSET
  try:
    conn = MySQLdb.Connect(host=DBHOST,user=DBUSER,passwd=DBPWD,port=DBPORT,charset=DBCHARSET)
    cur = conn.cursor()
    conn.select_db(DBNAME)
    songDict,playlistDict = DBProcess.genEffectivePlaylist()
    countDict = {}
    lisDict = {}
    playDict = {}
    for sid in songDict.keys():
      cur.execute('select sname,aname,count,tags,listeners,playcount,useful from effective_song where id = %d' % sid)
      result = cur.fetchone()
      sname = result[0]
      aname = result[1]
      count = int(result[2])
      if count not in countDict:
        countDict[count] = 1
      else:
        countDict[count] = countDict[count] + 1
      tags = result[3]
      listeners = int(result[4])
      if listeners not in lisDict:
        lisDict[listeners] = 1
      else:
        lisDict[listeners] = lisDict[listeners] + 1
      playcount = int(result[5])
      if playcount not in playDict:
        playDict[playcount] = 1
      else:
        playDict[playcount] = playDict[playcount] + 1

      useful = int(result[6])
      if useful == 0:
        print '%d useful is 0...' % sid
        logging.warning('%d useful is 0...' % sid)
        return
    conn.commit()
    cur.close()
    conn.close()
  except MySQLdb.Error,e:
    print 'Mysql Error %d:%s' % (e.args[0],e.args[1])
    logging.error('Mysql Error %d:%s' % (e.args[0],e.args[1]))

if __name__ == "__main__":
  statisticsOfSongDB()

