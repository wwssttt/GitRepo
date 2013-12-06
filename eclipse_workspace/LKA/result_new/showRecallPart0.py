import numpy as np
import pylab as pl
import matplotlib.pyplot as plt
import sys
reload(sys)
sys.setdefaultencoding('utf-8')

def main(index):
  if(index < 1 or index > 4):
    print "Error Parameter\n"
  x = []
  pop = []
  knn = []
  lda_similar = []
  similar_arima = []
  lda_arima = []
  lsa_similar = []
  f = open("MOST_POPULAR0.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    x.append(int(items[0]))
    pop.append(float(items[index]))
    line = f.readline()
  f.close()
  f = open("UserKNN0.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    knn.append(float(items[index]))
    line = f.readline()
  f.close()
  f.close()
  f = open("LDA_SIMILAR0.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    lda_similar.append(float(items[index]))
    line = f.readline()
  f.close()
  f = open("SIMILAR_ARIMA0.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    similar_arima.append(float(items[index]))
    line = f.readline()
  f.close()
  f = open("LDA_ARIMA0.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    lda_arima.append(float(items[index]))
    line = f.readline()
  f.close()
  f = open("LSA_SIMILAR0.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    lsa_similar.append(float(items[index]))
    line = f.readline()
  f.close()
  plt.plot(x,pop,"b",label="Most Popular")
  plt.plot(x,knn,"y",label="UserKNN")
  plt.plot(x,lda_similar,"r",label="LDA Similar")
  plt.plot(x,similar_arima,"r--",label="Similar Arima")
  plt.plot(x,lda_arima,"b--",label="LDA Arima")
  plt.plot(x,lsa_similar,"y--",label="LSA Similar")
  if(index == 1):
    pl.title('Hit Ratio of Different Methods to Predicting Next Song')
    pl.ylabel('Hit Ratio')
  elif(index == 2):
    pl.title('Recall Ratio of Different Methods to Predicting Next Song')
    pl.ylabel('Recall Ratio')
  elif(index == 3):
    pl.title('Precision Ratio of Different Methods to Predicting Next Song')
    pl.ylabel('Precision Ratio')
  elif(index == 4):
    pl.title('Coverage Ratio of Different Methods to Predicting Next Song')
    pl.ylabel('Coverage Ratio')
  pl.xlabel('number of recommendations')
  pl.legend(loc='upper left')
  pl.show()

if __name__ == '__main__':
  index = int(sys.argv[1])
  main(index)
