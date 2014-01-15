#!/usr/bin python
#--coding:utf-8
###########################
#topic models
###########################

import sys
from gensim import corpora, models, similarities
import const
import logging
import os

reload(sys)
sys.setdefaultencoding('utf-8')

logging.basicConfig(filename=os.path.join(os.getcwd(),'../log/topicmodel_log.txt'),level=logging.DEBUG,format='%(asctime)s-%(levelname)s:%(message)s')

#read docs of file to documents
filename = '../txt/%s_song_Docs.txt' % const.DATASET_NAME
docFile = open(filename,'r')
documents = []
index2Id = {}
textDict = {}
index = 0
while 1:
  line = docFile.readline()
  line = line.rstrip('\n')
  if not line:
    break
  pos = line.find('>>')
  sid = int(line[:pos])
  content = line[(pos+2):]
  index2Id[index] = sid
  textDict[sid] = content
  documents.append(content)
  index += 1
docFile.close()

#LDA
#make every document to a vector of words
texts = [[word for word in document.lower().split()] for document in documents]
#map text to id
dictionary = corpora.Dictionary(texts)
#map every document to a vector of ids of words
corpus = [dictionary.doc2bow(text) for text in texts]
#make a TF-IDF model
tfidf = models.TfidfModel(corpus)
#map every document to a vector of tf-idf
corpus_tfidf = tfidf[corpus]
#make topic models
lda = models.LdaModel(corpus_tfidf,id2word=dictionary,num_topics=const.TOPIC_NUM)

wFile = open('../txt/%s_topic-word_%d_gensim.txt' % (const.DATASET_NAME,const.TOPIC_NUM),'w')
result = lda.show_topics(-1,topn=10)
wFile.write(str(result))
wFile.close()

corpus_lda = lda[corpus]

wFile = open('../txt/%s_songs-doc-topics_%d_gensim.txt' % (const.DATASET_NAME,const.TOPIC_NUM),'w')

for result in enumerate(corpus_lda):
  index = int(result[0])
  topic = str(result[1])
  wFile.write('%d>>%s\n' % (index2Id[index],topic))
wFile.close()
