#!/usr/bin python
#coding=utf-8
############################
# @author Jason Wong
# @date 2013-12-08
############################
# connect to aotm db
# get useful playlists and songs
############################

import MySQLdb
import sys
import numpy
import pylab as pl
import logging
import os
import matplotlib.pyplot as plt

# reload sys and set encoding to utf-8
reload(sys)
sys.setdefaultencoding('utf-8')
# set log's localtion and level
logging.basicConfig(filename=os.path.join(os.getcwd(),'dbprocess_log.txt'),level=logging.DEBUG,format='%(asctime)s-%(levelname)s:%(message)s')

# define some global varibale
DBHOST = 'localhost'
DBUSER = 'root'
DBPWD = 'wst'
DBPORT = 3306
DBNAME = 'aotm'
DBCHARSET = 'utf8'

# get effective_playlists which part < 2
# @return: two dictionaries
# the first dict contains songs in selected playlists,that is, the keys of the dict are effective_songs' ids and values are appearance count numbers of these songs in selected playlists
# the second dict stands for length of each playlist like <pid:count>
def genEffectivePlaylist():
  #init the dicts to be returned
  songDict = {}
  playlistDict = {}
  #include global varibles
  global DBHOST
  global DBUSER
  global DBPWD
  global DBPORT
  global DBNAME
  global DBCHARSET
  
  try:
    #connect to db
    conn = MySQLdb.Connect(host=DBHOST,user=DBUSER,passwd=DBPWD,port=DBPORT,charset=DBCHARSET)
    #get the cursor of db
    cur = conn.cursor()
    #select db
    conn.select_db(DBNAME)
    #select effective playlists
    count = cur.execute('select id,count,songs from effective_playlists where part != -1 and part < 2')
    print 'there are %d playlists selected' % count
    logging.debug('there are %d playlists selected' % count)
    if count == 0:
      print 'No effective playlists selected......'
      logging.warning('No effective playlists selected......')
      return
    else:
      #get all results
      results = cur.fetchall()
      #loop every playlist 
      for result in results:
        pid = int(result[0])
        length = int(result[1])
        playlistDict[pid] = length
        songs = result[2]
        songItems = songs.split('==>')
        #add songs of a playlist to songDict
        for song in songItems:
          sid = int(song)
          if sid not in songDict:
            songDict[sid] = 1
          else:
            old = songDict[sid]
            new = old + 1
            songDict[sid] = new
    print 'There are %d songs in %d playlists' % (len(songDict),len(playlistDict))
    logging.debug('There are %d songs in %d playlists' % (len(songDict),len(playlistDict)))

    conn.commit()
    cur.close()
    conn.close()
    return songDict,playlistDict       
  except MySQLdb.Error,e:
    print 'Mysql Error %d:%s' % (e.args[0],e.args[1])
    logging.error('Mysql Error %d:%s' % (e.args[0],e.args[1]))

#give the statistics of datas
def getStatistics():
  songDict,playlistDict = genEffectivePlaylist()
  #get max and min frequency of songs
  maxSongFreq = max(songDict.values())
  minSongFreq = min(songDict.values())
  #get max and min length of playlists
  maxPlaylistLength = max(playlistDict.values())
  minPlaylistLength = min(playlistDict.values())
  print 'maxSongFreq = %d' % maxSongFreq
  logging.debug('maxSongFreq = %d' % maxSongFreq)
  print 'minSongFreq = %d' % minSongFreq
  logging.debug('minSongFreq = %d' % minSongFreq)
  print 'maxPlaylistLength = %d' % maxPlaylistLength
  logging.debug('maxPlaylistLength = %d' % maxPlaylistLength)
  print 'minPlaylistLength = %d' % minPlaylistLength
  logging.debug('minPlaylistLength = %d' % minPlaylistLength)
  #calculate how many songs in specific frequency
  songFreqDict = {}
  playlistLenDict = {}
  for count in songDict.values():
    if count not in songFreqDict:
      songFreqDict[count] = 1
    else:
      old = songFreqDict[count]
      new = old + 1
      songFreqDict[count] = new
  songNum = len(songDict)
  for freq in songFreqDict.keys():
    ratio = songFreqDict[freq] * 100.0 / songNum
    print 'freq %d : %d (%d/%d  %f%%)' % (freq,songFreqDict[freq],songFreqDict[freq],songNum,ratio)
    logging.info('freq %d : %d (%d/%d  %f%%)' % (freq,songFreqDict[freq],songFreqDict[freq],songNum,ratio))
  #calculate how many playlists in specific length
  for length in playlistDict.values():
    if length not in playlistLenDict:
      playlistLenDict[length] = 1
    else:
      old = playlistLenDict[length]
      new = old + 1
      playlistLenDict[length] = new
  playlistNum = len(playlistDict)
  for length in playlistLenDict.keys():
    ratio = playlistLenDict[length] * 100.0 / playlistNum
    print 'length %d: %d (%d/%d %f%%)' % (length,playlistLenDict[length],playlistLenDict[length],playlistNum,ratio)
    logging.info('length %d: %d (%d/%d %f%%)' % (length,playlistLenDict[length],playlistLenDict[length],playlistNum,ratio))
  return songFreqDict,playlistLenDict

def showStatistics():
  songFreqDict,playlistLenDict = getStatistics()
  plt.bar(left=songFreqDict.keys(),height=songFreqDict.values(),align="center")
  plt.show()
  plt.bar(left=playlistLenDict.keys(),height=playlistLenDict.values(),align="center")
  plt.show()

if __name__ == '__main__':
  showStatistics()
  
  


