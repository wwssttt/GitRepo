#!/usr/bin python
#coding:utf-8

import matplotlib.pyplot as plt
import sys
import os

reload(sys)
sys.setdefaultencoding("utf8")

def showPerplexity(filename):
  if os.path.exists(filename):
    step = []
    perplexity = []
    pFile = open(filename,"r")
    for line in pFile.readlines():
      line = line.rstrip('\n')
      items = line.split()
      step.append(int(items[0]))
      perplexity.append(float(items[1]))
    plt.title("Perplexity of LDA models with different topics")
    plt.xlabel("Topic Number")
    plt.ylabel("Perplexity")
    plt.plot(step,perplexity,"b-D",label="Perplexity")
    plt.legend(loc="upper right",numpoints=1)
    plt.savefig("%s.png" % filename)
    plt.show()
  else:
    print '%s do not exist...' % filename

if __name__ == "__main__":
  showPerplexity("Lastfm_songs-perplexity.txt")
