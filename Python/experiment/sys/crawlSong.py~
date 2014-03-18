# -*- coding:utf-8 -*-
# Filename : ex38.py
# http://s5s5.me
# 抓取豆瓣fm华语频道音乐列表
 
import os
import urllib
import json
import time
import sys

reload(sys)                      # reload 才能调用 setdefaultencoding 方法  
sys.setdefaultencoding('utf-8')  # 设置 'utf-8'  

allSids = []
#1:华语(400)  2:欧美(200)  3:70后(100) 4:80后(400) 5:90后(100) 
#6:粤语(200) 7:摇滚(200) 8:民谣(200) 10:电影原声(100) 20:女生(100)  
channel = [1,2,3,4,5,6,7,8,10,20]
number = [400,200,100,400,100,200,200,200,100,100]
count = [0,0,0,0,0,0,0,0,0,0]
maxRating = 0.0
minRating = 100

def get_music_json(index,output):
  global maxRating
  global minRating
  # 抓取json并写入临时txt
  url = 'http://douban.fm/j/mine/playlist?type=n&channel=%d' % channel[index]   # 定义json地址
  music_json = urllib.urlopen(url)    # urllib去抓json回来
  base_json = json.load(music_json)   # json把json解析
  for i in base_json['song']:     # 找到json中的相关元素
    if count[index] == number[index]:
      break
    #print i
    #title = i['title'].encode('utf8')   # 写入txt用utf8码
    #artist = i['artist'].encode('utf8')
    #album = i['album'].encode('utf8')
    #picture = i['picture'].encode('utf8')
    #ssid = i['ssid'].encode('utf8')
    #url = i['url'].encode('utf8')
    #company = i['company'].encode('utf8')
    rating_avg = i['rating_avg']
    if rating_avg > maxRating:
      maxRating = rating_avg
    if rating_avg < minRating:
      minRating = rating_avg
    #public_time = i['public_time'].encode('utf8')
    #subtype = i['subtype'].encode('utf8')
    #length = i['length'] %d
    sid = i['sid'].encode('utf8')
    #aid = i['aid'].encode('utf8')
    #sha = i['sha256'].encode('utf8')
    #kbps = i['kbps'].encode('utf8')
    #albumtitle = i['albumtitle'].encode('utf8')]
    #未被抓取并且平均评分在4.5以上
    if sid not in allSids and rating_avg >= 4.5:
      allSids.append(sid)
      sjson = json.dumps(i,encoding='utf8',ensure_ascii=False)
      #print vjson
      output.write(sjson + '\n')   # 一行一首歌的写
      count[index] += 1
  
 
def no_repeat():
  # 对临时txt去重并排序
  read_txt = file('tempfje_-83838399wfjefie.txt', 'r')    # 读临时txt
  write_txt = file('songlist.txt', 'w')   # 要写入的txt
  s = set()   # 用set去重
  for i in read_txt:  # 把txt写到set过的变量中
    s.add(i)
  s = list(s)     # 先转成列表才能排序
  s.sort()        # 排序
  for i in s:     # 写入txt
    i = i.replace('/', '&')     # 替换/为&
    write_txt.write(i)
  os.remove('tempfje_-83838399wfjefie.txt')   # 删除临时txt
 
def main():
  size = len(channel)
  for index in range(size):
    output = open('%d.txt' % channel[index], 'w')  # 增量写入txt
    for i in range(0, 5000):   # 抓它100次，因为每条json只有10首歌左右
      if count[index] == number[index]:
        break
      get_music_json(index,output)
      print channel[index],i     # 显示一下进度
      time.sleep(2)   # 延时1秒去抓，抓太快会被封IP
    output.close()  # 关了文件
  print count
  print 'all songs = ',len(allSids)
  print 'max rating = ',maxRating
  print 'min rating = ',minRating

if __name__ == "__main__":
  main()
