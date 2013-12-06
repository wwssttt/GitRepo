import numpy as np
import pylab as pl
import matplotlib.pyplot as plt
import sys

reload(sys)
sys.setdefaultencoding('utf-8')

def main(index):
  if(index < 1 or index > 4):
    print "Error Parameter\n"
    return
  x = []
  pop = []
  knn = []
  lda_arima = []
  lsa_similar = []
  knn_context = []
  unified_model = []
  unified = []
  baseline = []

  f = open("MOST_POPULAR.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    x.append(int(items[0]))
    pop.append(float(items[index]))
    line = f.readline()
  f.close()

  f = open("UserKNN.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    knn.append(float(items[index]))
    line = f.readline()
  f.close()

  f = open("LDA_ARIMA.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    lda_arima.append(float(items[index]))
    line = f.readline()
  f.close()

  f = open("LSA_SIMILAR.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    lsa_similar.append(float(items[index]))
    line = f.readline()
  f.close()

  f = open("USERKNN_CONTEXT.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    knn_context.append(float(items[index]))
    line = f.readline()
  f.close()

  f = open("UNIFIED_MODEL.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    unified_model.append(float(items[index]))
    line = f.readline()
  f.close()

  f = open("UNIFIED.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    unified.append(float(items[index]))
    line = f.readline()
  f.close()

  f = open("BASELINE.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    baseline.append(float(items[index]))
    line = f.readline()
  f.close()

  size = 15

  plt.plot(x,lda_arima,"k",label="LDA_Arima")
  plt.plot(x,pop,"k:",label="Most_Popular")
  plt.plot(x,knn,"k+",label="UserBased_KNN")
  plt.plot(x,knn_context,"k--",label="UserKNN_Context")
  plt.plot(x,lsa_similar,"kx",label="LSA_Similar")
  plt.plot(x,unified_model,"r",label="Unified_Model")
  plt.plot(x,unified,"b",label="Unified")
  plt.plot(x,baseline,"r+",label="Baseline")

  if(index == 1):
    #pl.title('Hit Ratio of Different Methods to Predicting Next Song')
    pl.ylabel('Hit Ratio',fontsize=size)
    pl.xlim(0,300)
    pl.ylim(0,0.45)
    pl.legend(loc='upper left')
  elif(index == 2):
    #pl.title('Recall Ratio of Different Methods to Predicting Next Song')
    pl.ylabel('Recall Ratio',fontsize=size)
  elif(index == 3):
    #pl.title('Precision of Different Methods to Predicting Next Song')
    pl.ylabel('Precision',fontsize=size)
    pl.xlim(0,300)
    pl.legend(loc='upper right')
  elif(index == 4):
    #pl.title('Coverage Ratio of Different Methods to Predicting Next Song')
    pl.ylabel('Coverage Ratio')
    pl.legend(loc='upper left')

  pl.xlabel('number of recommendations',fontsize=size)
  pl.show()


if __name__ == '__main__':
  index = int(sys.argv[1])
  main(index)
