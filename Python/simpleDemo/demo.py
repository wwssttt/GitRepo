#!/usr/bin python
#coding:utf-8

import urllib 
import urllib2 
import requests 

def fun(index):
  if index >= 0 and index < 10:
    return "000%d" % index 
  if index >= 10 and index < 100:
    return "00%d" % index 
  if index >= 100 and index < 1000:
    return "0%d" % index 

page = 1
url = "http://58.51.95.67:9896/dm13//ok-comic13/L/LiangRenShiJie/vol_01/dmeden-0001-11004.JPG" 
path = '%d.JPG' % page
urllib.urlretrieve(url, path)   
