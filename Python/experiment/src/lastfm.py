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

#crawl users from lastfm and then store them into db
def crawlUsersFromLastfm():
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  #get existing users
  count = cur.execute('select uid,username from user')
  print 'There are %d  users in table user now...' % count
  users = cur.fetchall()
  allUserName = []
  allUserId = []
  for user in users:
    allUserId.append(user[0])
    allUserName.append(user[1])
  index = count - 1
  step = 0
  total = count
  #max is 30000
  while total <= 30000:
    if index == -1:
      username = 'rj'
    else:
      username = allUserName[index]
    print 'curUser = %s' % username
    try:
      #read json
      friendDict = crawlInfoOfUser(username,0)
      users = friendDict['friends']['user']
      count = len(users)
      for i in range(count):
        value = []
        uid = users[i]['id']
        value.append(uid)
        name = users[i]['name']
        value.append(name)
        country = users[i]['country']
        if country == '':
          country = 'n'
        value.append(country)
        age = users[i]['age']
        if age == '':
          age = 0  
        value.append(age)
        gender = users[i]['gender']
        if gender == '':
          gender = 'n'
        value.append(gender)
        registeredTime = users[i]['registered']['unixtime']
        if registeredTime == '':
          registeredTime = time.time()
        value.append(registeredTime)
        registeredText = users[i]['registered']['#text']
        if registeredText == '':
          registeredText = time.ctime()
        value.append(registeredText)
        value.append('')
        value.append('2')
        if not uid in allUserId:
          cur.execute('insert ignore into user values(%s,%s,%s,%s,%s,%s,%s,%s,%s)',value)
          allUserId.append(uid)
          allUserName.append(name)
          total += 1
          if step % 50 == 0:
            print 'To be update database. There are %d users now...' % len(allUserId)
            conn.commit()
    except:
      print '%s causes exception......' % username
    print '%d loop has %d users......' % (index,len(allUserId))
    index += 1
    step += 1
  conn.commit()
  cur.close()
  conn.close()
  
#crawl recent tracks from lastfm and then store them into db
def crawlRecentTracksFromLastfm():
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  #get uids in table record
  count = cur.execute('select distinct(uid) from record')
  print 'There are records of %d users in table record...' % count
  results = cur.fetchall()
  uids = [result[0] for result in results]
  #get uids in table user
  cur.execute('select uid,username from user where scale != -1')
  results = cur.fetchall()
  userDict = {}
  for result in results:
    userDict[result[0]] = result[1]
  userCount = len(userDict)
  index = 0
  for key in userDict.keys():
    index += 1
    #ignore uid in uids
    if key in uids:
      continue
    #get 4 pages of am user
    for page in range(1,4,1):
      try:
        values = []
        print '%d/%d >>> %d/4' % (index,userCount,page)
        trackDict = crawlInfoOfUser(userDict[key],1,page)
        tracks = trackDict['recenttracks']['track']
        count = len(tracks)
        for i in range(count):
          value = []
          value.append(key)
          mbid = tracks[i]['mbid']
          value.append(mbid)
          uts = tracks[i]['date']['uts']
          value.append(uts)
          dateText = tracks[i]['date']['#text']
          value.append(dateText)
          values.append(value)
        #insert into db
        for value in values:
          cur.execute('insert ignore into record values(0,%s,%s,%s,%s,2)',value)
        conn.commit()
        #check total pages
        attr = trackDict['recenttracks']['@attr']
        totalPages = int(attr['totalPages'])
        if totalPages < (page+1):
          break
      except:
        print '%s(%d/%d) causes exception in page %d......' % (userDict[key],index,userCount,page)
  conn.commit()
  cur.close()
  conn.close()

#filter records with session or pause
#all songs must be listenered in n*8*60s
#that is, length of per song is 8 min
def selectRecordsInOneSession():
  effective = []
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  userCount = cur.execute('select distinct(uid) from record where scale = 2')
  print 'There are %d distinct users...' % userCount
  users = cur.fetchall()
  userList = [user[0] for user in users]
  index = 0
  userCount = len(userList)
  for index in range(userCount):
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
      print 'selectRecordsInOneSession:%d/%d:Good' % (index,userCount) 
    else:
      print 'selectRecordsInOneSession:%d/%d:Bad' % (index,userCount)
  #set scale to 1 for middle records
  eCount = len(effective)
  for index in range(eCount):
    print 'selectRecordsInOneSession/setScale=1:%d/%d' % (index,eCount)
    uid = effective[index]
    cur.execute('update record set scale=1 where uid=%s',uid)
  conn.commit()
  cur.close()
  conn.close()
  print 'There are %d middle users.' % len(effective)
  return effective

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

#crawl recent tracks from lastfm and then store them into db
def crawlSongFromLastfm():
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  #get uids in table record
  count = cur.execute('select distinct(mbid) from song')
  print 'There are %d songs in table song...' % count
  results = cur.fetchall()
  sids = [result[0] for result in results]
  #get uids in table user
  count = cur.execute('select distinct(mbid) from record where scale = 1')
  print 'There are %d songs in table record to be cralwed...' % count
  results = cur.fetchall()
  allSids = [result[0] for result in results]
  error = 0
  existis = 0
  crawl = 0
  left = []
  exception = 0
  for sid in allSids:
    if sid not in sids:
      left.append(sid)
      sql = "delete from record where mbid = '%s'" % sid
      cur.execute(sql)
      conn.commit()
  count = len(left)
  print 'left = ',count
  return
  for index in range(count):
    try:
      mbid = left[index]
      #print 'crawlSongFromLastfm:%d/%d:%s...' % (index,count,mbid)
      if mbid in sids:
        existis += 1
        continue
      else:
        contentList = getContentOfSong(mbid)
        if len(contentList) < 10:
          error += 1
          sql = "delete from record where mbid = '%s'" % mbid
          cur.execute(sql)
          conn.commit()
          continue
        else:
          crawl += 1
          sids.append(mbid)
          print 'crawlSongFromLastfm:%d/%d:%s...' % (index,count,mbid)
          cur.execute('insert ignore into song values(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,1)',contentList)
    except:
      exception += 1
      sql = "delete from record where mbid = '%s'" % mbid
      cur.execute(sql)
      conn.commit()
      continue

  conn.commit()
  cur.close()
  conn.close()
  print 'Error = ',error
  print 'Existis = ',existis
  print 'Crawl = ',crawl
  print 'Exception = ',exception

#get content text of mbid
def getContentOfSong(mbid):
  infoDict = crawlInfoOfSong(mbid,0)
  if 'track' not in infoDict:
    print '%s is not found...' % mbid
    return []
  track = infoDict['track']
  sid = track['id']
  name = track['name']
  name = name.encode('utf-8')
  duration = track['duration']
  artistId = track['artist']['mbid']
  artistName = track['artist']['name']
  artistName = artistName.encode('utf-8')
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
      tagName = tagName.encode('utf-8')
      tagCount = tagList['count']
      sTags[tagName] = tagCount
    elif type(tagList) is types.ListType:
      tagNum = len(tagList)
      for i in range(tagNum):
        tagName = tagList[i]['name']
        tagName = tagName.encode('utf-8')
        tagCount = tagList[i]['count']
        sTags[tagName] = tagCount
  tagStr = str(sTags)
      
  content = [sid,mbid,name,duration,artistId,artistName,album,listeners,playcount,tagStr]
  return content

def getAllArtistFromFile(scale = 0):
  filename = "../txt/Lastfm_songs%d.txt" % scale
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
  return allAid

#crawl artists in exception files
def crawlArtistsInException(scale = 0):
  filename = "../txt/Lastfm_exception_artist%d.txt" % scale
  if not os.path.exists(filename):
    print '%s is not existing...' % filename
    return 0
  exceptionCount = 0
  eFile = open(filename,'r')
  lines = eFile.readlines()
  mbids = [line.rstrip('\n') for line in lines]
  eFile.close()
  artistFilename = '../txt/Lastfm_artists%d.txt' % scale
  aFile = open(artistFilename,'a')
  eFile = open(filename,'w')
  count = len(mbids)
  for index in range(count):
    mbid = mbids[index]
    print 'crawlArtistsInException:%d/%d : %s......' % (index,count,mbid)
    try:
      content = getContentOfArtist(mbid)
      if not content.startswith('-1'):
        aFile.write(content)
    except Error,e:
      print 'crawlArtistsInException:%d/%d : %s causes exception......' % (index,count,mbid)
      print e
      exceptionCount += 1
      eFile.write(mbid)
      continue
  aFile.close()
  eFile.close()
  return exceptionCount

#get content text of mbid
def getContentOfArtist(mbid):
  infoDict = crawlInfoOfSong(mbid,2)
  if 'artist' not in infoDict:
    print '%s is not found...' % mbid
    return '-1\n'
  artist = infoDict['artist']
  if 'name' not in artist:
    name = ''
  else:
    name = artist['name']
  if 'image' not in artist:
    imageUrl = ''
  else:
    imageList = artist['image']
    imageCount = len(imageList)
    if imageCount >= 1:
      if '#text' not in imageList[imageCount-1]:
        imageUrl = ''
      else:
        imageUrl = imageList[imageCount-1]['#text']
    else:
      imageUrl = ""
      
  tagDict = crawlInfoOfSong(mbid,3)
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
  
  content = '%s()%s()%s()%s\n' % (mbid,name,imageUrl,tagStr)
  #print content
  return content

#crawl artist infos from lastfm
def crawlArtistsFromLastfm(scale = 0):
  #target file
  filename = "../txt/Lastfm_artists%d.txt" % scale
  exceptionName = "../txt/Lastfm_exception_artist%d.txt" % scale
  if os.path.exists(filename):
    print '%s is existing......' % filename
    oldExceptionCount = 0
    while True:
      exceptionCount = crawlArtistsInException(scale)
      if exceptionCount == 0 or oldExceptionCount == exceptionCount:
        print 'No Exception or No Promotion......'
        break
      oldExceptionCount = exceptionCount
    return
  aFile = open(filename,'w')
  eFile = open(exceptionName,'w')
  #get all songs' mbid
  allMbid = getAllArtistFromFile(scale)
  #loop every mbid and crawl its info
  count = len(allMbid)
  for index in range(count):
    mbid = allMbid[index]
    print 'crawlArtistsFromLastfm:%d/%d : %s......' % (index,count,mbid)
    try:
      content = getContentOfArtist(mbid)
      if not content.startswith('-1'):
        aFile.write(content)
    except:
      print '%s(%d/%d) causes exception......' % (mbid,index,count)
      eFile.write('%s\n' % mbid) 
  aFile.close()
  eFile.close()

  print 'processing exception......'
  oldExceptionCount = 0
  while True:
    exceptionCount = crawlSongsInException(scale)
    if exceptionCount == 0 or oldExceptionCount == exceptionCount:
      break
    oldExceptionCount = exceptionCount

#store artists in filename
def storeArtistsToDB(scale = 0):
  filename = "../txt/Lastfm_artists%d.txt" % scale
  if not os.path.exists(filename):
    crawlArtistsFromLastfm(scale)
  
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  
  uFile = open(filename,'r')
  lines = uFile.readlines()
  for line in lines:
    value = line.rstrip('\n').split('()')
    sql = 'insert ignore into artist values(%%s,%%s,%%s,%%s,%d)' % scale
    cur.execute(sql,value)
  conn.commit()
  uFile.close()
  cur.close()
  conn.close()

#get all tags
def getAllTags(tagType = 0,scale = 0):
  alltags = []
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  #sql select
  if tagType == 0:
    sql = 'select toptag from song where scale <= %d' % scale
  else:
    sql = 'select toptag from artist where scale <= %d' % scale
  cur.execute(sql)
  toptags = cur.fetchall()
  for toptag in toptags:
    tagDict = eval(toptag[0])
    tags = tagDict.keys()
    for tag in tags:
      if tag not in alltags:
        alltags.append(tag)
  if tagType == 0:
    print 'There are %d different tags tagged to songs.' % len(alltags)
  else:
    print 'There are %d different tags tagged to artists.' % len(alltags)
  cur.close()
  conn.close()
  return alltags

#define function to crawl info of tags with specific mbid from lastfm
def crawlInfoOfTag(tagname):
  url = 'http://ws.audioscrobbler.com/2.0/?method=tag.getinfo&tag=%s&api_key=550633c179112c8002bc6a0942d55b2a&format=json' % tagname
  #url = url.replace('&','%26')
  #url = url.replace('+','%2b')
  page = urllib.urlopen(url)
  data = page.read()
  ddata = json.loads(data)
  return ddata

def crawAllTagInfo(alltags,tagType = 0,scale = 0):
  if tagType == 0:
    filename = '../txt/Lastfm_song_tags%d.txt' % scale
    exception = '../txt/Lastfm_exception_song_tags%d.txt' % scale
  else:
    filename = '../txt/Lastfm_artist_tags%d.txt' % scale
    exception = '../txt/Lastfm_exception_artist_tags%d.txt' % scale
  if os.path.exists(filename):
    print '%s is existing...' % filename
    return
  tFile = open(filename,'w')
  eFile = open(exception,'w')
  tagCount = len(alltags)
  for index in range(tagCount):
    tag = alltags[index]
    print '%d/%d tag is %s...' % (index,tagCount,tag)
    try:
      tagDict = crawlInfoOfTag(tag)
    except:
      eFile.write(tag)
      continue
    if 'tag' not in tagDict:
      print '%s is not existing...' % tag
      eFile.write(tag)
      continue
    else:
      tagInfo = tagDict['tag']
      if 'name' not in tagInfo:
        name = tag
      else:
        name = tagInfo['name']
      if 'reach' not in tagInfo:
        reach = '0'
      else:
        reach = tagInfo['reach']
      if 'taggings' not in tagInfo:
        taggings = '0'
      else:
        taggings = tagInfo['taggings']
      content = '%s()%s()%s()\n' % (name,reach,taggings)
      tFile.write(content)
  tFile.close()
  eFile.close()

#store tag into db
def storeTagIntoDB(tagType = 0, scale = 0):
  if tagType == 0:
    filename = '../txt/Lastfm_song_tags%d.txt' % scale
  else:
    filename = '../txt/Lastfm_artist_tags%d.txt' % scale
  if not os.path.exists(filename):
    print '%s is not existing...' % filename
    return
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  
  tFile = open(filename,'r')
  lines = tFile.readlines()
  count = len(lines)
  for index in range(count):
    line = lines[index]
    print 'storeTagIntoDB:%d/%d...' % (index,count)
    value = line.rstrip('\n').split('()')
    name = value[0]
    md5 = util.getMD5(name)
    sql = "insert ignore into tag values('%s',%%s,%%s,%%s,%d)" % (md5,scale)
    #print sql
    cur.execute(sql,value)
  conn.commit()
  tFile.close()
  cur.close()
  conn.close()

#generate exception mbids of scale
def generateExceptionFile(scale = 0):
  filename = '../txt/Lastfm_exception%d.txt' % scale
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

#filter songs 
def filterSongs():
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  cur.execute('select distinct(mbid) from record where scale = 0')
  results = cur.fetchall()
  mbids = [result[0] for result in results]
  count = len(mbids)
  print 'There are %d unique songs in record where scale = 0.' % count
  cur.execute('select mbid from song where scale = 0')
  songs = cur.fetchall()
  sids = [song[0] for song in songs]
  sCount = len(sids)
  print 'There are %d songs in song where scale = 0.' % len(sids)
  for index in range(sCount):
    #print '%d/%d...' % (index,sCount)
    sid = sids[index]
    if sid not in mbids:
      sql = "update song set scale = 2 where mbid = '%s'" % sid
      cur.execute(sql)
  conn.commit()
  cur.close()
  conn.close()

#generate playlist
def generatePlaylistFromDB(scale = 0):
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  cur.execute('select sid,mbid,duration from song where scale <= %d' % scale)
  results = cur.fetchall()
  songDict = {}
  durationDict = {}
  for result in results:
    sid = result[0]
    mbid = result[1]
    duration = int(result[2])
    if mbid not in songDict:
      songDict[mbid] = sid
      durationDict[mbid] = duration / 1000.0
    else:
      print '%s is in dict now...' % mbid
      return
  cur.execute('select uid from record where scale <= %d group by uid' % scale)
  results = cur.fetchall()
  users = [result[0] for result in results]
  for user in users:
    playlists = []
    utss = []
    result = []
    cur.execute("select mbid,uts from record where uid = '%s' order by uts asc" % user)
    records = cur.fetchall()
    for record in records:
      mbid = record[0]
      uts = int(record[1])
      playlists.append(mbid)
      utss.append(uts)
    count = len(playlists)
    for index in range(count):
      mbid = playlists[index]
      sid = songDict[mbid]
      if index < (count-1):
        uts = utss[index+1] - utss[index]
        ratio = uts / durationDict[mbid]
      else:
        ratio = 1.0
      result.append('%s:%.2f' % (sid,ratio))
    playlistStr = "==>".join(result)
    sql = "update user set playlist = '%s',scale = %d where uid = '%s'" % (playlistStr,scale,user)
    cur.execute(sql)
  conn.commit()
  cur.close()
  conn.close()

#write playlists to file
def readPlaylistFromDB():
  filename = '../txt/Lastfm_playlists.txt'
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  pFile = open(filename,'w')
  count = cur.execute('select uid,playlist from user where scale = 0')
  print 'There are %d playlists...' % count
  results = cur.fetchall()
  allSids = []
  for result in results:
    uid = result[0]
    playlistStr = result[1]
    sids = []
    items = playlistStr.split('==>')
    for item in items:
      value = item.split(':')
      sid = value[0]
      if sid not in allSids:
        allSids.append(sid)
      sids.append(sid)
    sidStr = ",".join(sids)
    pFile.write('%s:%s\n' % (uid,sidStr))
  pFile.close()
  print 'There are %d unique songs...' % len(allSids)
  cur.close()
  conn.close()

if __name__ == "__main__":
  #crawlUsersFromLastfm()
  #crawlRecentTracksFromLastfm()
  #selectRecordsInOneSession()
  crawlSongFromLastfm()
  """
  contentList = getContentOfSong('d03a0d3b-a3c5-44f4-9af7-34c76ccaedb2')
  print contentList
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  cur.execute('insert ignore into song values(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,1)',contentList)
  conn.commit()
  cur.close()
  conn.close()
  """
