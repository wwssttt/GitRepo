#!/usr/bin/python
# -*- coding:utf-8 -*-
"""Useful statistics of the database.
   Dependencies:
     None.
"""
__author__ = 'Jason Wong'
__version__ = '1.0'

import MySQLdb
import sys
import matplotlib.pyplot as plt

# set default encoding as utf-8.
reload(sys)
sys.setdefaultencoding('utf-8')

def statisticsOfUserInLastfm(datasetType = 0):
  """Get useful statistics of different datasets in database.
     Input:
       userType - type of dataset.
                  0 - small dataset, default.
                  1 - whole dataset.
                  2 - session dataset.
     Output:
       userCount - count of users.
       songCount - count of songs.
       sparsity - sparsity of the dataset.
       maxLength - max length of users' sequences.
       minLength - min length of users' sequences.
       avgLength - average length of users' sequences.
       medianLength - median length of users' sequences.
       userDict - dict of lengths of users' sequences.
       lengthDict - dict of how many times the length appears.
  """
  # set dataset type to default if it is illegal.
  if datasetType > 2:
    datasetType = 0

  # define some variables.
  songs = []
  actionCount = 0.0
  sparsity = 0
  userDict = {}
  lengthDict = {}
  maxLength = 0
  minLength = 1000
  totalLength = 0

  # db process
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  if datasetType == 0:
    sql = 'select uid,playlist from user where scale >= 0 and scale < 10'
  elif datasetType == 1:
    sql = 'select uid,playlist from user where scale >= 0 and scale < 30'
  elif datasetType == 2:
    sql = 'select uid,playlist from user where scale >= 30'
  userCount = cur.execute(sql)
  userInfo = cur.fetchall()
  for user in userInfo:
    uid = user[0]
    playlistStr = user[1]
    songItems = playlistStr.split('==>')
    length = len(songItems)
    userDict[uid] = length
    if length not in lengthDict:
      lengthDict[length] = 1
    else:
      lengthDict[length] += 1
    totalLength += length
    if length > maxLength:
      maxLength = length
    if length < minLength:
      minLength = length
    actions = []
    for songItem in songItems:
      items = songItem.split(':')
      sid = items[0]
      if sid not in songs:
        songs.append(sid)
      if sid not in actions:
        actions.append(sid)
        actionCount += 1
  
  # calculate
  avgLength = (totalLength * 1.0) / userCount
  songCount = len(songs)
  sparsity = 1.0 - (actionCount * 1.0) / (userCount * songCount)
  # median
  sortedList = sorted(userDict.iteritems(),key=lambda x:x[1])
  userList = []
  for ele in sortedList:
    userList.append(ele[1])
  n = len(userList)
  if n % 2 == 0:
    medianLength = (userList[n/2] + userList[n/2+1]) / 2.0
  else:
    medianLength = userList[(n+1)/2]

  cur.close()
  conn.close()

  print 'userCount = ',userCount
  print 'songCount = ',songCount
  print 'sparsity = ',sparsity
  print 'maxLength = ',maxLength
  print 'minLength = ',minLength
  print 'avgLength = ',avgLength  
  print 'medianLength = ',medianLength 

  #draw img
  lenList = []
  countList = []
  for length in lengthDict:
    lenList.append(length)
    countList.append(lengthDict[length])
  if datasetType == 0:
    datasetName = 'Small'
  elif datasetType == 1:
    datasetName = 'Whole'
  elif datasetType == 2:
    datasetName = 'Session'
  plt.title('Count of Different Lengths in %s Dataset' % datasetName)
  plt.xlabel('Lengths')
  plt.ylabel('Count')
  plt.grid()
  plt.plot(lenList,countList,'bD')
  plt.savefig('../img/count_of_length_%s.png' % datasetName)
  plt.show()

  return userCount,songCount,sparsity,maxLength,minLength,avgLength,medianLength,userDict,lengthDict

if __name__ == '__main__':
  print sys.argv
  datasetType = int(sys.argv[1])
  statisticsOfUserInLastfm(datasetType)
