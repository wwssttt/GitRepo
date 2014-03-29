#!/usr/bin/python
# -*- coding:utf-8 -*-
"""Main class to do any experiments.
Dependecies:
  Any modules you want.
  module 'test' may be needed.
"""
__author__ = 'Jason Wong(wwssttt@163.com)'
__version__ = '1.0'


import test
import test_session
import util
import sys
import const
import time

reload(sys)
sys.setdefaultencoding('utf-8')

if __name__ == "__main__":

  #test.showRecallTrendWithDifferentCoeff_MostSimilarHybrid()
  
  #test.showRecallTrendWithDifferentCoeff_AverageHybrid()

  #test.testRecMethod(const.SIMILAR)
  
  #test_session.showResult()
  test.showResult()
  #test.getErrorOfRecMethod(const.MARKOV)
