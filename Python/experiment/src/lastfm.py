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
import random

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
  uCount = cur.execute('select uid from user where scale = -1 and uid % 50 = 1')
  print 'There are %d users in table record...' % uCount
  results = cur.fetchall()
  uids = [result[0] for result in results]
  index = 0
  for uid in uids:
    index += 1
    #get 4 pages of am user
    for page in range(1,5,1):
      try:
        values = []
        print '%d/%d >>> %d/5' % (index,uCount,page)
        trackDict = crawlInfoOfUser(uid,1,page)
        tracks = trackDict['recenttracks']['track']
        count = len(tracks)
        for i in range(count):
          value = []
          value.append(uid)
          mbid = tracks[i]['mbid']
          value.append(mbid)
          uts = tracks[i]['date']['uts']
          value.append(uts)
          dateText = tracks[i]['date']['#text']
          value.append(dateText)
          values.append(value)
        #insert into db
        for value in values:
          cur.execute('insert ignore into record values(0,%s,%s,%s,%s,-2)',value)
        conn.commit()
        #check total pages
        attr = trackDict['recenttracks']['@attr']
        totalPages = int(attr['totalPages'])
        if totalPages < (page+1):
          break
      except:
        print '%s(%d/%d) causes exception in page %d......' % (uid,index,uCount,page)
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
  count = cur.execute('select distinct(mbid) from record where scale = -2')
  print 'There are %d songs in table record to be cralwed...' % count
  results = cur.fetchall()
  allSids = [result[0] for result in results]
  index = 0
  for mbid in allSids:
    index += 1
    if mbid in sids:
      print 'crawlSongFromLastfm:%d/%d:%s...Exsiting' % (index,count,mbid)
      continue
    else:
      try:
        contentList = getContentOfSong(mbid)
        if len(contentList) < 10:
          print 'crawlSongFromLastfm:%d/%d:%s...Error' % (index,count,mbid)
          sql = "delete from record where mbid = '%s'" % mbid
          cur.execute(sql)
          conn.commit()
          continue
        else:
          cur.execute('insert ignore into song values(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)',contentList)
          sids.append(mbid)
          print 'crawlSongFromLastfm:%d/%d:%s...OK' % (index,count,mbid)
      except:
        print 'crawlSongFromLastfm:%d/%d:%s...Exception' % (index,count,mbid)
        sql = "delete from record where mbid = '%s'" % mbid
        cur.execute(sql)
        conn.commit()
        continue

  conn.commit()
  cur.close()
  conn.close()

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

#get content text of mbid
def getContentOfArtist(mbid):
  infoDict = crawlInfoOfSong(mbid,2)
  if 'artist' not in infoDict:
    print '%s is not found...' % mbid
    return []
  artist = infoDict['artist']
  if 'name' not in artist:
    name = ''
  else:
    name = artist['name']
    name = name.encode('utf-8')
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
    imageUrl = imageUrl.encode('utf-8')
      
  tagDict = crawlInfoOfSong(mbid,3)
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
  
  content = []
  content.append(mbid)
  content.append(name)
  content.append(imageUrl)
  content.append(tagStr)
  #print content
  return content

#crawl artist infos from lastfm
def crawlArtistsFromLastfm():
  #target file
  #get all songs' artists' mbid
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  count = cur.execute('select mbid from artist')
  print 'There are %d different artists in artist' % count
  results = cur.fetchall()
  idsInArtist = [result[0] for result in results]
  count = cur.execute('select distinct(aid) from song')
  print 'There are %d different artists in song' % count
  results = cur.fetchall()
  idsInSong = [result[0] for result in results]
  ok = 0
  exception = 0
  null = 0
  for index in range(count):
    mbid = idsInSong[index]
    if mbid in idsInArtist:
      print 'crawlArtistsFromLastfm:%d/%d:%s...Existing' % (index,count,mbid)
      continue
    content = getContentOfArtist(mbid)
    if len(content) != 4:
      print 'crawlArtistsFromLastfm:%d/%d:%s...Null' % (index,count,mbid)
      null += 1
      continue
    else:
      cur.execute('insert ignore into artist values(%s,%s,%s,%s,1)',content)
      conn.commit()
      idsInArtist.append(mbid)
      print 'crawlArtistsFromLastfm:%d/%d:%s...OK' % (index,count,mbid)
  conn.commit()
  cur.close()
  conn.close()
  print 'Ok = ',ok
  print 'Exception = ',exception
  print 'Null = ',null

#get all tags
def getAllTags():
  alltags = []
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  #get toptags in song
  cur.execute('select toptag from song')
  toptags = cur.fetchall()
  for toptag in toptags:
    tagDict = eval(toptag[0])
    tags = tagDict.keys()
    for tag in tags:
      if tag not in alltags:
        alltags.append(tag)
  #get toptags in artist
  cur.execute('select toptag from artist')
  toptags = cur.fetchall()
  for toptag in toptags:
    tagDict = eval(toptag[0])
    tags = tagDict.keys()
    for tag in tags:
      if tag not in alltags:
        alltags.append(tag)
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

def getContentOfTag(tagname):
  tagDict = crawlInfoOfTag(tagname)
  if 'tag' not in tagDict:
    print '%s is not existing...' % tag
    return []
  else:
    tagInfo = tagDict['tag']
    if 'name' not in tagInfo:
      name = tag
    else:
      name = tagInfo['name']
    name = name.encode('utf-8')
    if 'reach' not in tagInfo:
      reach = '0'
    else:
      reach = tagInfo['reach']
    if 'taggings' not in tagInfo:
      taggings = '0'
    else:
      taggings = tagInfo['taggings']

    content = [name,reach,taggings]
    return content

#store tag into db
def crawlTagFromLastfm():
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  count = cur.execute('select id from tag')
  print 'There are %d tags in tag now...' % count
  results = cur.fetchall()
  tagsInTag = [result[0] for result in results]
 
  alltags = getAllTags()
  tagCount = len(alltags)

  for index in range(tagCount):
    try:
      tag = alltags[index]
      md5 = util.getMD5(tag)
      if md5 in tagsInTag:
        print 'crawlTagFromLastfm:%d/%d:%s...Existing' % (index,count,tag)
        continue
      content = getContentOfTag(tagname)
      if len(content) == 0:
        print 'crawlTagFromLastfm:%d/%d:%s...Null' % (index,count,tag)
        continue
      else:
        sql = "insert ignore into tag values('%s',%%s,%%s,%%s,0)" % md5
        #print sql
        cur.execute(sql,content)
        conn.commit()
        tagsInTag.append(md5)
        print 'crawlTagFromLastfm:%d/%d:%s...OK' % (index,count,tag)
    except:
      print 'crawlTagFromLastfm:%d/%d:%s...Exception' % (index,count,tag)
      continue
  conn.commit()
  cur.close()
  conn.close()

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
def insertPlaylistToTableUser():
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  cur.execute('select sid,mbid,duration from song')
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
  print 'There are %d songs in table song' % len(songDict)
  cur.execute('select uid from record where scale >= 30 group by uid')
  results = cur.fetchall()
  users = [result[0] for result in results]
  userCount = len(users)
  step = 0
  for user in users:
    step += 1
    print 'insertPlaylistToTableUser:%d/%d' % (step,userCount)
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
    curSessionStart = -1
    curSessionStartSid = -1;
    curSessionStartRaio = -1;
    for index in range(count):
      mbid = playlists[index]
      sid = songDict[mbid]
      if index < (count-1):
        uts = utss[index+1] - utss[index]
        if durationDict[mbid] == 0:
          ratio = 1.0
        else:
          ratio = uts / durationDict[mbid]
        #3 hour
        if uts > 10800:
          curSessionStart = index
          curSessionStartSid = sid
          curSessionStartRatio = ratio
      else:
        ratio = 1.0
      result.append('%s:%.2f' % (sid,ratio))
    if curSessionStart != -1:
      result[curSessionStart] = '%s:%.2f>>' % (curSessionStartSid,curSessionStartRatio)
    playlistStr = "==>".join(result)
    sql = "update user set playlist = '%s' where uid = '%s'" % (playlistStr,user)
    cur.execute(sql)
  conn.commit()
  cur.close()
  conn.close()

#write playlists to file
def readPlaylistFromDB():
  filename = '../txt/Lastfm_playlists_MultiSession_one.txt'
  conn=MySQLdb.connect(host="localhost",user="root",passwd="wst",db="lastfm")
  cur = conn.cursor()
  pFile = open(filename,'w')
  count = cur.execute('select uid,playlist from user where scale >= 30')
  print 'There are %d playlists...' % count
  results = cur.fetchall()
  allSids = []
  for result in results:
    uid = result[0]
    playlistStr = result[1]
    #print '%s:%s' % (uid,playlistStr)
    sids = []
    items = playlistStr.split('==>')
    count = len(items)
    target = count
    flag = False
    for index in range(count):
      item = items[index]
      if item.find('>>') != -1:
        item = item.replace('>>','')
        flag = True
      value = item.split(':')
      sid = value[0]
      if sid not in allSids:
        allSids.append(sid)
      sids.append(sid)

      if flag:
        index += 1
        item = items[index]
        value = item.split(':')
        sid = value[0]
        if sid not in allSids:
          allSids.append(sid)
        sids.append(sid)
        break
    #print '%s:%d' % (uid,len(sids))
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
  #crawlSongFromLastfm()
  #insertPlaylistToTableUser()
  #crawlArtistsFromLastfm()
  #crawlTagFromLastfm()
  readPlaylistFromDB()
