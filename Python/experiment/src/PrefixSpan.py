#!/usr/bin/python
# -*- coding: utf-8 -*-
"""Mining Frequent Patterns Using PrefixSpan.
   Dependencies:
     persist.
     util.
"""
__author__ = 'Tianming Lu & Jason Wong'
__version__ = '1.0'

import sys
import persist
import util


PLACE_HOLDER = '_'

def read(filename):
    """Read patterns from file.
       Input:
         filename - file contains topic sequences.
       Output:
         S - sequence list.
    """
    S = []
    with open(filename, 'r') as input:
        for line in input.readlines():
            elements = line.split(',')
            s = []
            for e in elements:
                s.append(e.split())
            S.append(s)
    print S
    return S


class SquencePattern:
    """Definition of SequencePattern.
    """
    def __init__(self, squence, support):
        """Initialize SequencePattern.
           Input:
             sequence - source sequence.
             support - support of sequence.
           Output:
             None.
        """
        self.squence = []
        for s in squence:
            self.squence.append(list(s))
        self.support = support

    def append(self, p):
        """Append another SequencePattern to self.
           Input:
             p - another sequence pattern.
           Output:
             None.
        """
        if p.squence[0][0] == PLACE_HOLDER:
            first_e = p.squence[0]
            first_e.remove(PLACE_HOLDER)
            self.squence[-1].extend(first_e)
            self.squence.extend(p.squence[1:])
        else:
            self.squence.extend(p.squence)
        self.support = min(self.support, p.support)

def prefixSpan(pattern, S, threshold):
    """Do PrefixSpan.
       Input:
         pattern - source pattern.
         S - seqeunce database.
         threshold - support threshold.
    """
    patterns = []
    f_list = frequent_items(S, pattern, threshold)

    for i in f_list:
        p = SquencePattern(pattern.squence, pattern.support)
        p.append(i)
        patterns.append(p)

        p_S = build_projected_database(S, p)
        p_patterns = prefixSpan(p, p_S, threshold)
        patterns.extend(p_patterns)

    return patterns


def frequent_items(S, pattern, threshold):
    items = {}
    _items = {}
    f_list = []
    if S is None or len(S) == 0:
        return []

    if len(pattern.squence) != 0:
        last_e = pattern.squence[-1]
    else:
        last_e = []
    for s in S:

        #class 1
        is_prefix = True
        for item in last_e:
            if item not in s[0]:
                is_prefix = False
                break
        if is_prefix and len(last_e) > 0:
            index = s[0].index(last_e[-1])
            if index < len(s[0]) - 1:
                for item in s[0][index + 1:]:
                    if item in _items:
                        _items[item] += 1
                    else:
                        _items[item] = 1

        #class 2
        if PLACE_HOLDER in s[0]:
            for item in s[0][1:]:
                if item in _items:
                    _items[item] += 1
                else:
                    _items[item] = 1
            s = s[1:]

        #class 3
        counted = []
        for element in s:
            for item in element:
                if item not in counted:
                    counted.append(item)
                    if item in items:
                        items[item] += 1
                    else:
                        items[item] = 1

    f_list.extend([SquencePattern([[PLACE_HOLDER, k]], v)
                   for k, v in _items.iteritems()
                   if v >= threshold])
    f_list.extend([SquencePattern([[k]], v)
                   for k, v in items.iteritems()
                   if v >= threshold])
    sorted_list = sorted(f_list, key=lambda p: p.support)
    return sorted_list


def build_projected_database(S, pattern):
    """
    suppose S is projected database base on pattern's prefix,
    so we only need to use the last element in pattern to
    build projected database
    """
    p_S = []
    last_e = pattern.squence[-1]
    last_item = last_e[-1]
    for s in S:
        p_s = []
        for element in s:
            is_prefix = False
            if PLACE_HOLDER in element:
                if last_item in element and len(pattern.squence[-1]) > 1:
                    is_prefix = True
            else:
                is_prefix = True
                for item in last_e:
                    if item not in element:
                        is_prefix = False
                        break

            if is_prefix:
                e_index = s.index(element)
                i_index = element.index(last_item)
                if i_index == len(element) - 1:
                    p_s = s[e_index + 1:]
                else:
                    p_s = s[e_index:]
                    index = element.index(last_item)
                    e = element[i_index:]
                    e[0] = PLACE_HOLDER
                    p_s[0] = e
                break
        if len(p_s) != 0:
            p_S.append(p_s)

    return p_S


def print_patterns(patterns):
    for p in patterns:
        print("pattern:{0}, support:{1}".format(p.squence, p.support))

def checkTargetInPattern(targetPattern,pattern):
  """Check whether targetPattern matchs prefix of pattern.
     Input:
       targetPattern - target pattern.
       pattern - source pattern to be matched.
     Output:
       True - match.
       False - unmathch.
  """
  tarSize = len(targetPattern)
  size = len(pattern) - 1
  if tarSize != size:
    print 'two pattern have different length...'
    return False
  for index in range(size):
    eles = targetPattern[index]
    p = pattern[index]
    flag = False
    for ele in eles:
      if ele in p:
        flag = True
        break
    if flag == False:
      return False
  return True

def getPredictTopic(patterns,targetPattern):
  """Get predict topic of given pattern.
     Input:
       patterns - source patterns.
       targetPattern - target pattern.
     Output:
       predictTopic - predicted  topic list.
  """
  predictTopic = []
  #cut window size
  start = 0
  while len(predictTopic) == 0:
    cutPattern = targetPattern[start:]
    size = len(cutPattern)
    #print 'start=',start
    #print '%d-%d' % (size,len(targetPattern))
    if size == 0:
      break
    #loop every pattern in pattern database
    for pattern in patterns:
      #caution those patterns that have length of size+1
      if len(pattern.squence) == (size+1):
        #check validation
        if checkTargetInPattern(cutPattern,pattern.squence) == True:
          #check prefix of pattern to cal coff
          for prefix in patterns:
            if len(prefix.squence) == size \
               and prefix.squence ==  pattern.squence[:-1]:
              coff = pattern.support * 1.0 / prefix.support * 1.0
              if coff >= 0.3:
                #print pattern.squence
                #print cutPattern
                for ele in pattern.squence[-1:]:
                  tid = ele[0]
                  if tid not in predictTopic:
                    predictTopic.append(tid)
    start += 1
  if len(predictTopic) == 0:
    for ele in pattern.squence[-1:]:
      tid = ele[0]
      if tid not in predictTopic:
        predictTopic.append(tid)
  return predictTopic
          
def getPredictTopicDict(allPlaylist,songDict,scale):
  """Get predict topics.
     Input:
       allPLaylist - all playlists.
       songDict - dict of all songs.
       scale - scale of current playlists.
     Output:
       predictTopicDict - predicted topic dict 
                          with pid as key and topic list as value.
  """
  print 'I am in getPredictTopicDict......'
  predictTopicDict = {}
  allTrainingPattern,testingPatternDict = util.getPatternTrainingSet(allPlaylist,
                                                                    songDict,
                                                                    scale)
  print 'get trainingPatterns and testingPatterns...'
  #print allPattern
  #print len(allPattern)
  #S = read("../txt/PrefixSpan.txt")
  print 'get frequent pattern...'
  patterns = prefixSpan(SquencePattern([], sys.maxint), allTrainingPattern, 30)
  print 'Finish getting frequent pattern...'
  #print_patterns(patterns)
  #print len(patterns)
  #count = 0
  size = len(testingPatternDict)
  index = 0
  for pid in testingPatternDict.keys():
    #print '======%d======' % pid
    index += 1
    print 'scale=%d > %d/%d:%d' % (scale,index,size,pid)
    pattern = testingPatternDict[pid]      
    predictTopic = getPredictTopic(patterns,pattern)
    predictTopicDict[pid] = predictTopic
    #count += len(predictTopic)
  #print 'avg = ',count*2.0/len(patternDict)
  print 'I am out getPredictTopicDict......'
  return predictTopicDict
 
if __name__ == "__main__":
  songDict = persist.readSongFromFile()
  playlistDict = persist.readPlaylistFromFile()
  getPredictTopicDict(playlistDict,songDict)
