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

#crawl song infos from lastfm
def crawlSongsFromLastfm():
  #target file
  filename = "../txt/songs.txt"
  exceptionName = "../txt/exception.txt"
  if os.path.exists(filename):
    print '%s is existing......'
    return
  sFile = open(filename,'w')
  eFile = open(exceptionName,'w')
  #get all songs' mbid
  allMbid = getAllSongFromFile()
  #loop every mbid and crawl its info
  count = len(allMbid)
  for index in range(count):
    mbid = allMbid[index]
    print '%d/%d : %s......' % (index,count,mbid)
    try:
      infoDict = crawlInfoOfSong(mbid,0)
      track = infoDict['track']
      sid = track['id']
      name = track['name']
      duration = track['duration']
      artist = track['artist']['mbid']
      album = track['album']['mbid']
      listeners = track['listeners']
      playcount = track['playcount']

      tagDict = crawlInfoOfSong(mbid,1)
      tagList = tagDict['toptags']['tag']
      tagNum = len(tagList)
      sTags = {}
      for i in range(tagNum):
        tagName = tagList[i]['name']
        tagCount = tagList[i]['count']
        sTags[tagName] = tagCount
      tagStr = str(sTags)
      
      content = '%s++%s++%s++%s++%s++%s++%s++%s++%s\n' % (sid,mbid,name,duration,artist,album,listeners,playcount,tagStr)
      sFile.write(content)
    except:
      print '%s(%d/%d) causes exception......' % (mbid,index,count)
      eFile.write('%s\n' % mbid)
      continue 
  sFile.close()
  eFile.close()

def getAllArtistFromFile():
  filename = "../txt/songs.txt"
  if not os.path.exists(filename):
    crawlSongsFromLastfm()
  sFile = open(filename,'r')
  allAid = []
  lines = sFile.readlines()
  for line in lines:
    items = line.rstrip('\n').split('++')
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

if __name__ == "__main__":
  crawlSongsFromLastfm()
      
