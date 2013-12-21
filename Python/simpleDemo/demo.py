#--coding:utf-8--

from ctypes import *
import os

def callPmathFromC():
  pmath = cdll.LoadLibrary(os.getcwd()+'/pmath.so')
  print pmath.mul(2,3)
  print pmath.add(2,3)
  print pmath.sub(2,3)
  print pmath.div(2,3)
  print pmath.mod(2,3)

if __name__ == "__main__":
  callPmathFromC()
