#!/usr/bin python
#coding=utf-8

import os
import sys
import json
import shutil
import jieba
from pytagcloud import create_tag_image, make_tags
from pytagcloud.lang.counter import get_tag_counts
from gensim import corpora, models, similarities
import random
import logging

reload(sys)
sys.setdefaultencoding('utf-8')

logging.basicConfig(filename=os.path.join(os.getcwd(),'log.txt'),level=logging.DEBUG,format='%(asctime)s-%(levelname)s:%(message)s')

def generateDocs():
  tooLargeWords = ['pop','rihanna','rock','single','electronic','elva','mj','台湾']
  vocabulary = {}
  invalid = []
  stopwords = []
  stopFile = open('stopwords.txt','r')
  lines = stopFile.readlines()
  for line in lines:
    line = line.strip('n')
    line = line.strip('\r\n')
    stopwords.append(line)
  stopFile.close()

  rootDir = 'json'
  tarDir = 'douban'
  if os.path.exists(tarDir):
    shutil.rmtree(tarDir)
  os.mkdir(tarDir)
  count = 0
  for dirpath,dirname,filenames in os.walk(rootDir):
    for filename in filenames:
      filepath = os.path.join(dirpath,filename)
      if filepath.endswith('~'):
        continue
      print filepath
      sid = os.path.splitext(filename)[0]
      srcFile = file(filepath)
      data = json.load(srcFile)
      did = data['id']
      tags = data['tags']
      flag = 0
      for tag in tags:
        count = tag['count']
        if count >= 10:
          flag += 1
      if flag < 5:
        invalid.append(sid)
        continue
      tarFile = open('%s/%s' % (tarDir,sid),'w')
      for tag in tags:
        count = tag['count']
        if count < 10:
          continue
        old = count
        count /= 10
        if count == 0:
          print old
          return
        name = tag['name']
        name = name.lower()
        name = name.replace('/',' ')
        if name.find('r&b') == -1:
          name = name.replace('&',' ')
        name = name.replace('=',' ')
        name = name.replace('%',' ')
        name = name.replace('-',' ')
        name = name.replace('*',' ')
        name = name.replace('|',' ')
        name = name.replace(':','')
        name = name.replace('、','')
        name = name.replace('#',' ')
        name = name.replace('"','')
        name = name.replace("'",'')
        name = name.replace(',','')
        name = name.replace('.','')
        name = name.replace('!','')
        seg_list = jieba.cut(name) # 默认是精确模式
        words = []
        for seg in seg_list:
          if seg not in stopwords and seg != ' ':
            seg = seg.encode('utf-8')
            if seg in tooLargeWords:
              continue
            words.append(seg)
            if seg not in vocabulary:
              vocabulary[seg] = count
            else:
              vocabulary[seg] += count
        name = ' '.join(words)
        for index in range(count):
          tarFile.write(' '+name+' ')
      tarFile.close()
  print len(vocabulary)
  print len(invalid)
  vjson = json.dumps(vocabulary,encoding='utf-8',ensure_ascii=False)
  vFile = open('vocabulary.json','w')
  vFile.write(vjson)
  vFile.close()

#pop 18469641
#rihanna 1680136
#rock 1679676
#single 1678486
#electronic 1678346
#elva 1678001
#mj 1677809
#~~~~~~~~~~~~~~~~~~~
#台湾 21445
#孙燕姿 9371
#绿 7815
#苏打 7815
#王菲 6432
#流行 6189
#欧美 5010
#华语 4906

def vocabularyWordCloud():
  vFile = file('vocabulary.json')
  d = json.load(vFile)
  vocabulary = dict((k.decode('utf-8'), v) for (k, v) in d.items())
  print 'Total:%d' % len(vocabulary)
  recList = sorted(vocabulary.items(), key=lambda item: -item[1])
  tmp = recList[-50:]
  for t in tmp:
    print t[0],t[1]
  print tmp
  tmp = recList[:20]
  for t in tmp:
    print t[0],t[1]
  print tmp
  recList = recList[:100]
  tags = make_tags(recList,maxsize=115)
  create_tag_image(tags,'tag_cloud.png',background=(0, 0, 0, 255),size=(900, 600),fontname="SimHei")

def doc2Str():
  tarDir = 'douban'
  if not os.path.exists(tarDir):
    print '%s is not a directory...' % tarDir
    return
  sids = []
  documents = []
  for dirpath,dirname,filenames in os.walk(tarDir):
    for filename in filenames:
      filepath = os.path.join(dirpath,filename)
      if filepath.endswith('~'):
        continue
      sid = filename
      sids.append(sid)
      sFile = open(filepath,'r')
      line = sFile.readline()
      line.strip('\n')
      line.strip('\r\n')
      documents.append(line)
      sFile.close()
  return sids,documents

def ldaModel():
  sids,documents = doc2Str()
  texts = [[word for word in document.lower().split()] for document in documents]
  dictionary = corpora.Dictionary(texts)
  print len(dictionary)
  corpus = [dictionary.doc2bow(text) for text in texts]
  tfidf = models.TfidfModel(corpus)
  corpus_tfidf = tfidf[corpus]
  lda = models.LdaModel(corpus_tfidf, id2word=dictionary, num_topics=30)
  lda.print_topics(30)

#generateDocs()
#vocabularyWordCloud()
ldaModel()
