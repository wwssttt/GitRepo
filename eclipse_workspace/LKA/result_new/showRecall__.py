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
  f = open("Most_Popular0.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    x.append(int(items[0]))
    pop.append(float(items[index]))
    line = f.readline()
  f.close()
  f = open("User_KNN0.txt")
  line = f.readline()
  while(line):
    items = line.split("\t")
    knn.append(float(items[index]))
    line = f.readline()
  f.close()
  plt.plot(x,pop,"k",label="Most Popular")
  plt.plot(x,knn,"y",label="UserKNN")
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
