#!/usr/bin python
#-coding:utf-8-------
#############################
#some methods to crawl data from lastfm
#############################

import urllib
import json
import os
import time
import util
import MySQLdb
import types
import sys

reload(sys)
sys.setdefaultencoding('utf-8')

#define function to crawl friends of user with specific username from lastfm
def crawlInfoOfUser(username,infoType = 0,page = 1):
  if infoType == 0:
    #get friends
    url = 'http://ws.audioscrobbler.com/2.0/?method=user.getfriends&user=%s&api_key=550633c179112c8002bc6a0942d55b2a&format=json&page=%d' % (username,page)
  elif infoType == 1:
    #get recent tracks
    url = 'http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=%s&api_key=550633c179112c8002bc6a0942d55b2a&format=json&page=%d' % (username,page)
  #url = url.replace('&','%26')
  #url = url.replace('+','%2b')
  page = urllib.urlopen(url)
  data = page.read()
  ddata = json.loads(data)
  return ddata

def crawlUsersFromLastfm():
  filename = "../txt/users.txt"
  if os.path.exists(filename):
    print '%s is existing......'
    return
  uFile = open(filename,'w')
  allUserName = []
  allUserId = []
  index = -1
  while len(allUserId) < 10000:
    if index == -1:
      username = 'rj'
    else:
      username = allUserName[index]
    print 'curUser = %s' % username
    try:
      friendDict = crawlInfoOfUser(username,0)
      users = friendDict['friends']['user']
      count = len(users)
      for i in range(count):
        uid = users[i]['id']
        name = users[i]['name']
        country = users[i]['country']
        if country == '':
          country = 'n'
        age = users[i]['age']
        if age == '':
          age = 0  
        gender = users[i]['gender']
        if gender == '':
          gender = 'n'
        registeredText = users[i]['registered']['#text']
        if registeredText == '':
          registeredText = time.ctime()
        registeredTime = users[i]['registered']['unixtime']
        if registeredTime == '':
          registeredTime = time.time()
        if not uid in allUserId:
          info = '%s+%s+%s+%s+%s+%s+%s\n' % (uid,name,country,age,gender,registeredTime,registeredText)
          uFile.write(info)
          allUserId.append(uid)
          allUserName.append(name)
    except:
      print '%s causes exception......' % username
    print '%d loop has %d users......' % (index,len(allUserId))
    index += 1
  uFile.close()

#get all users from file named user.txt in txt folder
def getAllUserFromFile():
  filename = "../txt/users.txt"
  if not os.path.exists(filename):
    crawlUsersFromLastfm()
  allUserId = []
  allUserName = []
  uFile = open(filename,'r')
  lines = uFile.readlines()
  for line in lines:
    items = line.rstrip('\n').split('+')
    allUserId.append(items[0])
    allUserName.append(items[1])
  print 'There are %d users...' % len(allUserId)
  uFile.close()
  return allUserId,allUserName

#store users to db
def storeUsersToDB():
  filename = "../txt/users.txt"
  if not os.path.exists(filename):
    crawlUsersFromLastfm()
  
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  
  uFile = open(filename,'r')
  lines = uFile.readlines()
  for line in lines:
    value = line.rstrip('\n').split('+')
    cur.execute('insert ignore into user values(%s,%s,%s,%s,%s,%s,%s)',value)
  conn.commit()
  uFile.close()
  cur.close()
  conn.close()
  
def crawlRecentTracksFromLastfm():
  filename = "../txt/tracks.txt"
  if os.path.exists(filename):
    print '%s is existing......'
    return
  sFile = open(filename,'w')
  allUserId,allUserName = getAllUserFromFile()
  userCount = len(allUserId)
  for index in range(userCount):
    user = allUserName[index]
    uid = allUserId[index]
    for page in range(1,4,1):
      try:
        print '%d/%d >>> %d/4' % (index,userCount,page)
        trackDict = crawlInfoOfUser(user,1,page)
        tracks = trackDict['recenttracks']['track']
        count = len(tracks)
        for i in range(count):
          mbid = tracks[i]['mbid']
          uts = tracks[i]['date']['uts']
          dateText = tracks[i]['date']['#text']
          info = '%s+%s+%s+%s\n' % (uid,mbid,uts,dateText)
          sFile.write(info)
        attr = trackDict['recenttracks']['@attr']
        totalPages = int(attr['totalPages'])
        if totalPages < (page+1):
          break
      except:
        print '%s(%d/%d) causes exception in page %d......' % (user,index,userCount,page)
        continue
  sFile.close()

def getAllSongFromFile():
  filename = "../txt/tracks.txt"
  if not os.path.exists(filename):
    crawlRecentTracksFromLastfm()
  sFile = open(filename,'r')
  allSid = []
  lines = sFile.readlines()
  for line in lines:
    items = line.rstrip('\n').split('+')
    mbid = items[1]
    if not mbid in allSid:
      allSid.append(mbid)
  info = 'There are %d unique songs...' % len(allSid)
  print info
  #util.sendMail('wwssttt@163.com','Crawl Tracks Finished',info)
  sFile.close()
  return allSid

#store records to db
def storeRecordsToDB():
  filename = "../txt/tracks.txt"
  if not os.path.exists(filename):
    crawlRecentTracksFromLastfm()
  newfilename = "../txt/tracks_new.txt"
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  records = []
  uFile = open(filename,'r')
  nFile = open(newfilename,'w')
  lines = uFile.readlines()
  for line in lines:
    value = line.rstrip('\n').split('+')
    uid = value[0]
    uts = value[2]
    s = '%s#%s' % (uid,uts)
    if not s in records:
      records.append(s)
      cur.execute('insert ignore into record values(0,%s,%s,%s,%s)',value)
      nFile.write(line)
  conn.commit()
  uFile.close()
  nFile.close()
  cur.close()
  conn.close()

#get all records
def getAllScaleRecordsOfDB():
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  userCount = cur.execute('select distinct(uid) from record')
  print 'There are %d distinct users...' % userCount
  users = cur.fetchall()
  userList = [user[0] for user in users]
  cur.close()
  conn.close()
  print 'There are %d all users.' % len(userList)
  return userList

#filter records with session or pause
#all songs must be listenered in n*8*60s
#that is, length of per song is 8 min
def getMiddleScaleRecordsOfDB():
  effective = []
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  userCount = cur.execute('select distinct(uid) from record')
  print 'There are %d distinct users...' % userCount
  users = cur.fetchall()
  userList = [user[0] for user in users]
  index = 0
  userCount = len(userList)
  for index in range(userCount):
    print 'getMiddleScaleRecordsOfDB:%d/%d' % (index,userCount)
    uid = userList[index]
    rCount = cur.execute('select max(uts),min(uts),count(uts) from record where uid=%s',uid)
    results = cur.fetchall()
    new = int(results[0][0])
    old = int(results[0][1])
    sCount = int(results[0][2])
    sCount -= 1
    delta = new - old
    totalDelta = sCount * 8 * 60
    if delta <= totalDelta:
      effective.append(uid)
  #set scale to 1 for middle records
  eCount = len(effective)
  for index in range(eCount):
    print 'getMiddleScaleRecordsOfDB/setScale=1:%d/%d' % (index,eCount)
    uid = effective[index]
    cur.execute('update record set scale=1 where uid=%s',uid)
  conn.commit()
  cur.close()
  conn.close()
  print 'There are %d middle users.' % len(effective)
  return effective

#get small scale records:500
def getSmallScaleRecordsOfDB():
  effective = []
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  #get all songs in db
  sql = 'select mbid from song'
  cur.execute(sql)
  results = cur.fetchall()
  allSongsInDb = [result[0] for result in results]
  #get all user ids
  cur.execute('select distinct(uid) from record where scale = 1')
  users = cur.fetchall()
  userList = [user[0] for user in users]
  userCount = len(userList)
  for index in range(userCount):
    print 'getSmallScaleRecordsOfDB():%d/%d...' % (index,userCount)
    uid = userList[index]
    sql = "select mbid from record where uid = '%s'" % uid
    cur.execute(sql)
    results = cur.fetchall()
    allMbids = [result[0] for result in results]
    flag = True
    for mbid in allMbids:
      if mbid not in allSongsInDb:
        flag = False
        break
    if flag == True:
      effective.append(uid)
      sql = "update record set scale = 0 where uid = '%s'" % uid
      cur.execute(sql)  
  conn.commit()
  cur.close()
  conn.close()
  print 'There are %d small users.' % len(effective)
  return effective

#get distinct songs' mbids of effective uids
def getDistinctSongs(scale = 0):
  songs = []
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  sql = 'select distinct(mbid) from record where scale <= %d' % scale
  cur.execute(sql)
  results = cur.fetchall()
  for result in results:
    mbid  = result[0]
    if not mbid in songs:
      songs.append(mbid)
  print len(songs)
  print 'There are %d distinct songs...' % len(songs)
  sql = 'select mbid from song'
  cur.execute(sql)
  results = cur.fetchall()
  allSongs = [result[0] for result in results]
  delta = [val for val in songs if val not in allSongs]
  if len(delta) > 0:
    print 'There are %d songs not crawled...' % len(delta)
    #print delta
  cur.close()
  conn.close()
  return songs

#define function to crawl info of songs with specific mbid from lastfm
def crawlInfoOfSong(mbid,infoType = 0):
  if infoType == 0:
    #get base info of song
    url = 'http://ws.audioscrobbler.com/2.0/?method=track.getInfo&api_key=550633c179112c8002bc6a0942d55b2a&mbid=%s&format=json' % mbid
  elif infoType == 1:
    #get top tags of song
    url = 'http://ws.audioscrobbler.com/2.0/?method=track.gettoptags&mbid=%s&api_key=550633c179112c8002bc6a0942d55b2a&format=json' % mbid
  elif infoType == 2:
    #get base info of artist
    url = 'http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&mbid=%s&api_key=550633c179112c8002bc6a0942d55b2a&format=json' % mbid
  elif infoType == 3:
    #get top tags of artist
    url = 'http://ws.audioscrobbler.com/2.0/?method=artist.gettoptags&mbid=%s&api_key=550633c179112c8002bc6a0942d55b2a&format=json' % mbid
  #url = url.replace('&','%26')
  #url = url.replace('+','%2b')
  page = urllib.urlopen(url)
  data = page.read()
  ddata = json.loads(data)
  return ddata

#get content text of mbid
def getContentOfSong(mbid):
  infoDict = crawlInfoOfSong(mbid,0)
  if 'track' not in infoDict:
    print '%s is not found...' % mbid
    return '-1\n'
  track = infoDict['track']
  sid = track['id']
  name = track['name']
  duration = track['duration']
  artistId = track['artist']['mbid']
  artistName = track['artist']['name']
  if 'album' not in track:
    album = ''
  else:
    album = track['album']['mbid']
  listeners = track['listeners']
  playcount = track['playcount']

  tagDict = crawlInfoOfSong(mbid,1)
  sTags = {}
  if 'tag' in tagDict['toptags']:
    tagList = tagDict['toptags']['tag']
    if type(tagList) is types.DictType:
      tagName = tagList['name']
      tagCount = tagList['count']
      sTags[tagName] = tagCount
    elif type(tagList) is types.ListType:
      tagNum = len(tagList)
      for i in range(tagNum):
        tagName = tagList[i]['name']
        tagCount = tagList[i]['count']
        sTags[tagName] = tagCount
  tagStr = str(sTags)
      
  content = '%s()%s()%s()%s()%s()%s()%s()%s()%s()%s\n' % (sid,mbid,name,duration,artistId,artistName,album,listeners,playcount,tagStr)
  #print content
  return content

#crawl song infos from lastfm
def crawlSongsFromLastfm(scale = 0):
  #target file
  filename = "../txt/songs%d.txt" % scale
  exceptionName = "../txt/exception%d.txt" % scale
  if os.path.exists(filename):
    print '%s is existing......' % filename
    oldExceptionCount = 0
    while True:
      exceptionCount = crawlSongsInException(scale)
      if exceptionCount == 0 or oldExceptionCount == exceptionCount:
        print 'No Exception or No Promotion......'
        break
      oldExceptionCount = exceptionCount
    return
  sFile = open(filename,'w')
  eFile = open(exceptionName,'w')
  #get all songs' mbid
  allMbid = getDistinctSongs(scale)
  #loop every mbid and crawl its info
  count = len(allMbid)
  for index in range(count):
    mbid = allMbid[index]
    print 'crawlSongsFromLastfm:%d/%d : %s......' % (index,count,mbid)
    try:
      content = getContentOfSong(mbid)
      if not content.startswith('-1'):
        sFile.write(content)
    except:
      print '%s(%d/%d) causes exception......' % (mbid,index,count)
      eFile.write('%s\n' % mbid)
      continue 
  sFile.close()
  eFile.close()
  
  print 'processing exception......'
  oldExceptionCount = 0
  while True:
    exceptionCount = crawlSongsInException(scale)
    if exceptionCount == 0 or oldExceptionCount == exceptionCount:
      break
    oldExceptionCount = exceptionCount

#deal with exception
def crawlSongsInException(scale = 0):
  filename = "../txt/exception%d.txt" % scale
  if not os.path.exists(filename):
    print '%s is not existing...' % filename
    return 0
  exceptionCount = 0
  eFile = open(filename,'r')
  lines = eFile.readlines()
  mbids = [line.rstrip('\n') for line in lines]
  eFile.close()
  newFile = '../txt/new_songs%d.txt' % scale
  oldFile = '../txt/songs%d.txt' % scale
  eFile = open(filename,'w')
  sFile = open(newFile,'w')
  count = len(mbids)
  for index in range(count):
    mbid = mbids[index]
    print 'crawlSongsInException:%d/%d : %s......' % (index,count,mbid)
    try:
      content = getContentOfSong(mbid)
      if not content.startswith('-1'):
        sFile.write(content)
    except Error,e:
      print 'crawlSongsInException:%d/%d : %s causes exception......' % (index,count,mbid)
      print e
      exceptionCount += 1
      eFile.write(mbid)
      continue
  sFile.close()
  eFile.close()
  #call shell command to combine file
  command = "cat %s >> %s" % (newFile,oldFile)
  os.system(command)
  storeSongsToDB(scale)
  command = "rm -rf %s" % newFile
  os.system(command)
  if exceptionCount == 0:
    command = "rm -rf %s" % filename
    os.system(command)
  return exceptionCount

#store users to db
def storeSongsToDB(scale = 0):
  filename = "../txt/songs%d.txt" % scale
  if not os.path.exists(filename):
    crawlUsersFromLastfm(scale)
  
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  
  uFile = open(filename,'r')
  lines = uFile.readlines()
  for line in lines:
    value = line.rstrip('\n').split('()')
    sql = 'insert ignore into song values(%%s,%%s,%%s,%%s,%%s,%%s,%%s,%%s,%%s,%%s,%d)' % scale
    cur.execute(sql,value)
  conn.commit()
  uFile.close()
  cur.close()
  conn.close()

def getAllArtistFromFile():
  filename = "../txt/songs.txt"
  if not os.path.exists(filename):
    crawlSongsFromLastfm()
  sFile = open(filename,'r')
  allAid = []
  lines = sFile.readlines()
  for line in lines:
    items = line.rstrip('\n').split('()')
    mbid = items[4]
    if not mbid in allAid:
      allAid.append(mbid)
  info = 'There are %d unique artists...' % len(allAid)
  print info
  #util.sendMail('wwssttt@163.com','Crawl Artists Finished',info)
  sFile.close()

#crawl artist infos from lastfm
def crawlArtistsFromLastfm():
  #target file
  filename = "../txt/artists.txt"
  exceptionName = "../txt/exception.txt"
  if os.path.exists(filename):
    print '%s is existing......'
    return
  aFile = open(filename,'w')
  eFile = open(exceptionName,'w')
  #get all songs' mbid
  allMbid = getAllArtistFromFile()
  #loop every mbid and crawl its info
  count = len(allMbid)
  for index in range(count):
    mbid = allMbid[index]
    print '%d/%d : %s......' % (index,count,mbid)
    try:
      infoDict = crawlInfoOfSong(mbid,2)
      artist = infoDict['artist']
      name = artist['name']
      imageList = artist['image']
      imageCount = len(imageList)
      if imageCount >= 1:
        imageUrl = imageList[imageCount-1]['#text']
      else:
        imageUrl = ""

      tagDict = crawlInfoOfSong(mbid,3)
      tagList = tagDict['toptags']['tag']
      tagNum = len(tagList)
      sTags = {}
      for i in range(tagNum):
        tagName = tagList[i]['name']
        tagCount = tagList[i]['count']
        sTags[tagName] = tagCount
      tagStr = str(sTags)
      
      content = '%s++%s++%s++%s\n' % (mbid,name,imageUrl,tagStr)
      aFile.write(content)
    except:
      print '%s(%d/%d) causes exception......' % (mbid,index,count)
      eFile.write('%s\n' % mbid)
      continue 
  aFile.close()
  eFile.close()

#define function to crawl info of tags with specific mbid from lastfm
def crawlInfoOfTag(tagname):
  url = 'http://ws.audioscrobbler.com/2.0/?method=tag.getinfo&tag=%s&api_key=550633c179112c8002bc6a0942d55b2a&format=json' % tagname
  #url = url.replace('&','%26')
  #url = url.replace('+','%2b')
  page = urllib.urlopen(url)
  data = page.read()
  ddata = json.loads(data)
  return ddata

#generate exception mbids of scale
def generateExceptionFile(scale = 0):
  filename = '../txt/exception%d.txt' % scale
  eFile = open(filename,'w')
  allMbid = getDistinctSongs(scale)
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  cur.execute('select mbid from song where scale = %d' % scale)
  results = cur.fetchall()
  mbids = [result[0] for result in results]
  deltas = [val for val in allMbid if val not in mbids]
  print 'There are %d exception...' % len(deltas)
  for delta in deltas:
    eFile.write('%s\n' % delta)
    print delta
    #sql = "delete from record where mbid = '%s'" % delta
    #cur.execute(sql)
  conn.commit()
  cur.close()
  conn.close()
  eFile.close()
  return deltas

#filter users with too short records
def filterShortRecords():
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  cur.execute('select uid from record group by uid having count(mbid) < 10')
  results = cur.fetchall()
  uids = [result[0] for result in results]
  count = len(uids)
  for index in range(count):
    print 'Begin:%d/%d' % (index,count)
    uid = uids[index]
    sql = "delete from record where uid = '%s'" % uid
    cur.execute(sql)
    print 'End:%d/%d' % (index,count)
  conn.commit()
  cur.close()
  conn.close()
if __name__ == "__main__":
  #getMiddleScaleRecordsOfDB()
  #getSmallScaleRecordsOfDB()
  #generateExceptionFile(scale = 0)
  #crawlSongsFromLastfm(scale = 0)
  #storeSongsToDB(scale = 0)
  #getDistinctSongs(scale = 0)
  filterShortRecords()
