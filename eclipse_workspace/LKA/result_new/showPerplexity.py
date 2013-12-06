import numpy as np
import pylab as pl
import matplotlib.pyplot as plt
import sys
reload(sys)
sys.setdefaultencoding('utf-8')

def main():
  x = []
  perplexity = []
  f = open("perplexity.txt")
  for line in f.readlines():
    line = line.strip('\n')
    items = line.split('\t')
    print items[0]
    print items[1]
    step = int(items[0])
    p = float(items[1])
    x.append(step)
    perplexity.append(p)
  f.close()
  size = 15
  plt.plot(x,perplexity,"k")
  pl.ylabel('Perplexity',fontsize=size)
  pl.xlabel('Topic Number',fontsize=size)
  pl.show()

if __name__ == '__main__':
  main()
