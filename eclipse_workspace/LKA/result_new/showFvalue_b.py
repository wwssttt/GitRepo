import numpy as np
import pylab as pl
import matplotlib.pyplot as plt
import sys
reload(sys)
sys.setdefaultencoding('utf-8')

def fvalue(recall, precision):
  if((recall + precision) == 0):
    fv = 0
  else:
    fv = (recall * precision * 2) / (recall + precision)
  return (fv)
def main():
  x = []
  pop = []
  knn = []
  lda_arima = []
  lsa_similar = []
  knn_context = []
  f = open("MOST_POPULAR.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    x.append(int(items[0]))
    recall = float(items[1])
    precision = float(items[3])
    pop.append(fvalue(recall,precision))
    line = f.readline()
  f.close()
  f = open("UserKNN.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    recall = float(items[1])
    precision = float(items[3])
    knn.append(fvalue(recall,precision))
    line = f.readline()
  f.close()
  f = open("LDA_ARIMA.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    recall = float(items[1])
    precision = float(items[3])
    lda_arima.append(fvalue(recall,precision))
    line = f.readline()
  f.close()
  f = open("LSA_SIMILAR.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    recall = float(items[1])
    precision = float(items[3])
    lsa_similar.append(fvalue(recall,precision))
    line = f.readline()
  f.close()
  f = open("USERKNN_CONTEXT.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    recall = float(items[1])
    precision = float(items[3])
    knn_context.append(fvalue(recall,precision))
    line = f.readline()
  f.close()
  plt.plot(x,lda_arima,"k",label="LDA_Arima")
  plt.plot(x,pop,"k:",label="Most_Popular")
  plt.plot(x,knn,"k+",label="UserBased_KNN")
  plt.plot(x,knn_context,"k--",label="UserKNN_Context")
  plt.plot(x,lsa_similar,"kx",label="LSA_Similar")
  pl.ylabel('F1-Score')
  pl.xlim(0,300)
  pl.ylim(0,0.012)
  pl.legend(loc='upper right')
  pl.xlabel('number of recommendations')
  pl.show()

if __name__ == '__main__':
  main()
