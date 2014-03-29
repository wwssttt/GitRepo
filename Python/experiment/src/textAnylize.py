#!/usr/bin/python
# -*- coding:utf-8 -*-
"""Test Different Recommenders of different text models.
   Dependencies:
     persist.
     predict.
     util.
     const.
"""
__author__ = 'Jason Wong'
__version__ = '1.0'

from gensim import corpora, models, similarities
import math
import time
import persist
import predict
import util
import logging
import os
import math
import matplotlib.pyplot as plt
import sys
import const

reload(sys)
sys.setdefaultencoding('utf-8')

# set log's localtion and level
logging.basicConfig(filename=os.path.join(os.getcwd(),'../log/test_log_text_analysis.txt'),level=logging.DEBUG,format='%(asctime)s-%(levelname)s:%(message)s')

def getSongTextInfo():  
  """Get song text info.
     Input:
       None.
     Output:
       sids - list of sids.
       documents - list of documents.
  """
  sids = []
  documents = []
  sFile = open('../txt/two__Lastfm_song_Docs.txt')
  lines = sFile.readlines()
  index = 0
  for line in lines:
    line.strip('\n')
    line.strip('\r\n')
    items = line.split('>>')
    sid = int(items[0])
    text = items[1]
    documents.append(text)
    sids.append(sid)
  sFile.close()
  print 'len = ',len(sids)
  print 'len = ',len(documents)
  return sids,documents

def getVSMSpace():
  """Get the VSM Model of documents.
     Input:
       None.
     Output:
       songMap - vsm model of songs.
  """
  sids,documents = getSongTextInfo()
  texts = [[word for word in document.lower().split()] for document in documents]
  dictionary = corpora.Dictionary(texts)
  corpus = [dictionary.doc2bow(text) for text in texts]
  songMap = {}
  index = 0
  for doc in corpus:
    sid = sids[index]
    rMap = {}
    for item in doc:
      wid = item[0]
      count = item[1]
      rMap[wid] = count
    songMap[sid] = rMap
    index += 1
  return songMap

def getTF_IDFSpace():
  """Get the TF-IDF Model of documents.
     Input:
       None.
     Output:
       songMap - tf-idf model of songs.
  """
  sids,documents = getSongTextInfo()
  texts = [[word for word in document.lower().split()] for document in documents]
  dictionary = corpora.Dictionary(texts)
  corpus = [dictionary.doc2bow(text) for text in texts]
  tfidf = models.TfidfModel(corpus)
  corpus_tfidf = tfidf[corpus]
  songMap = {}
  index = 0
  for doc in corpus_tfidf:
    sid = sids[index]
    rMap = {}
    for item in doc:
      wid = item[0]
      count = item[1]
      rMap[wid] = count
    songMap[sid] = rMap
    index += 1
  return songMap

def getLSASpace():
  """Get the LSA Model of documents.
     Input:
       None.
     Output:
       songMap - lsa model of songs.
  """
  sids,documents = getSongTextInfo()
  texts = [[word for word in document.lower().split()] for document in documents]
  dictionary = corpora.Dictionary(texts)
  corpus = [dictionary.doc2bow(text) for text in texts]
  tfidf = models.TfidfModel(corpus)
  corpus_tfidf = tfidf[corpus]
  lsi = models.LsiModel(corpus_tfidf, id2word=dictionary, num_topics=30)
  corpus_lsi = lsi[corpus_tfidf]
  songMap = {}
  index = 0
  for doc in corpus_lsi:
    sid = sids[index]
    rMap = {}
    for item in doc:
      wid = item[0]
      count = item[1]
      rMap[wid] = count
    songMap[sid] = rMap
    index += 1
  return songMap

def getLDASpace():
  """Get the LDA Model of documents.
     Input:
       None.
     Output:
       songMap - lda model of songs.
  """
  sids,documents = getSongTextInfo()
  texts = [[word for word in document.lower().split()] for document in documents]
  dictionary = corpora.Dictionary(texts)
  corpus = [dictionary.doc2bow(text) for text in texts]
  tfidf = models.TfidfModel(corpus)
  corpus_tfidf = tfidf[corpus]
  lda = models.LdaModel(corpus_tfidf, id2word=dictionary, num_topics=30)
  corpus_lda = lda[corpus_tfidf]
  songMap = {}
  index = 0
  for doc in corpus_lda:
    sid = sids[index]
    rMap = {}
    for item in doc:
      wid = item[0]
      count = item[1]
      rMap[wid] = count
    songMap[sid] = rMap
    index += 1
  return songMap

def cosineSimilarity(dict1,dict2):
  """Calculate cosine similarity of two vectors.
     Input:
       dict1 - the 1st vector.
       dict2 - the 2nd vector.
     Output:
       cosine similarity of two vectors.
  """
  product1 = 0.0
  product2 = 0.0
  for key in dict1.keys():
    product1 += (dict1[key] * dict1[key])
  for key in dict2.keys():
    product2 += (dict2[key] * dict2[key])
  product1 = math.sqrt(product1)
  product2 = math.sqrt(product2)
  fenmu = product1 * product2
  fenzi = 0.0
  for key in dict1.keys():
    if key in dict2:
      fenzi += (dict1[key] * dict2[key])
  cosSim = fenzi / fenmu
  return cosSim

def hellSimilarity(topicDict1,topicDict2):
  """Calculate hellinger distance of two distributions.
     Input:
       topicDict1 - the 1st distribution.
       topicDict2 - the 2nd distribution.
     Output:
       hellinger siatance of two distributions.
  """
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
  #distance
  hellDis = hellDis * (1.0/math.sqrt(2))
  if hellDis == 0:
    hellDis = 1.0 / 10000000
  #similarity
  hellSimilarity = 1.0 / hellDis
  return hellSimilarity

def getRecSongs(recType,songMap,tarSid,topN):
  """Get recommended songs list.
     Input:
       recType - recommender type.
       songMap - text model of song.
       tarSid - sid of target song.
       topN - count of recommendations.
     Output:
       result - a list of songs.
  """
  recDict = {}
  sims = {}
  for sid in songMap.keys():
    srcMap = songMap[sid]
    if len(srcMap) != 0:
      tarMap = songMap[tarSid]
      if recType != 3:
        sims[sid] = cosineSimilarity(srcMap,tarMap)
      else:
        sims[sid] = hellSimilarity(srcMap,tarMap)
    else:
      if recType != 3:
        sims[sid] = 0.0
      else:
        sims[sid] = 1000
  
  if recType != 3:
    #big to small
    recList = sorted(sims.items(), key=lambda item: -item[1])
  else:
    #small to big
    recList = sorted(sims.items(), key=lambda item: item[1])
  result = []
  for i in range(0,topN):
    result.append(recList[i][0])
  return result

def getRecDict(playlistDict,songMap,scale = 0,recType = 0, topN = const.TOP_N):
  """Get recommendations using different method.
     Input:
       playlistDict - dict of playlist of specific scale.
       songMap - text model of songs.
       recType - type of recommender.
       scale - scale of current playlist.
       topN - count of recommendations.
     Output:
       recDict - dict of recommendations 
                 with pid as key and recommendation list as value.
  """
  recDict = {}
  index = 0
  count = len(playlistDict)
  recName = ['VSM','TF-IDF','LSA','LDA']
  for pid in playlistDict.keys():
    print '%s:scale = %d >> %d/%d' % (recName[recType],scale,index,count)
    playlist = playlistDict[pid]
    trainingList = playlist.getTrainingList()
    tarSid = trainingList[-1]
    recSong = getRecSongs(recType,songMap,tarSid,topN)
    recDict[pid] = recSong
    index = index + 1
  return recDict

def hitRatio(recDict,playlistDict,topN = const.TOP_N):
  """Calculate hit ratio of different recommender.
     Input:
       recDict - dict of recommendations from getRectDict.
       playlistDict - dict of all playlists.
       topN - count of recommendations.
     Output:
       recall - hit ratio.
  """
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
    if lastSid in newList:
      hit = hit + 1
  recall = float(hit * 1.0) / testNum
  return recall

def getErrorOfRecMethod(recType = 0):
  """Get error of different recommemenders.
     Input:
       recType - type of recommender.
     Output:
       recalls - all hit ratios.
  """
  start_time = time.time()
  if recType == 0:
    songMap = getVSMSpace()
  elif recType == 1:
    songMap = getTF_IDFSpace()
  elif recType == 2:
    songMap = getLSASpace()
  elif recType == 3:
    songMap = getLDASpace()
  allPlaylist = persist.readPlaylistFromFile_Session()
  recalls = []
  for scale in range(10):
    playlistDict = allPlaylist[scale]
    recDict = getRecDict(playlistDict,songMap,scale,recType)
    index = 0
    for topN in range(1,const.TOP_N,1):
      recall = hitRatio(recDict,playlistDict,topN)
      if scale == 0:
        recalls.append(recall)
      else:
        recalls[index] += recall
      index += 1

  #cal the avg value
  recalls = [recall / 10.0 for recall in recalls]

  #logging info to log
  index = 0
  for topN in range(1,const.TOP_N,1):
    print '%d:TopN = %d:%f' % (recType,topN,recalls[index])
    logging.info('%d>%d:%f' % (recType,topN,recalls[index]))
    index += 1
  end_time = time.time()
  print 'Consumed:%d' % (end_time-start_time)
  return recalls  


logging.info('Begin......')
#getErrorOfRecMethod(0)
#getErrorOfRecMethod(1)
#getErrorOfRecMethod(2)
#getErrorOfRecMethod(3)
logging.info('End......')
