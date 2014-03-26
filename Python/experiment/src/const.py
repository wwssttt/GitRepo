#!/usr/bin python
#--coding:utf-8
##########################
#define some constant variables
##########################

DATASET_NAME = 'Lastfm'

DBHOST = 'localhost'
DBUSER = 'root'
DBPWD = 'wst'
DBPORT = 3306
DBNAME = 'lastfm'
DBCHARSET = 'utf8'

LDA_LIB = 'mallet'

TOPIC_NUM = 30
TOP_N = 100

ARIMA = 0

SIMILAR = 1
AVG = 2

ARIMA_SIMILAR = 3
ARIMA_AVG = 4

KNN = 5
MARKOV = 6
PATTERN = 7
MARKOV_3 = 8

ALL_HYBRID = 9

METHOD_SIZE = 10

locs = ['upper right','upper right','upper right','upper right','upper right']
marker = ['k','k-.','k1','k--','k:']

#group of comparision
#1:ARIMA +  Similar + Avergae + Popular(Pure)
#2:ARIMA + MF + User_KNN(CF)
#3:ARIMA + LSA + Markov + Pattern(Sequential)
#4:ARIMA + Arima_Average + Arima_Similar(Hybrid)
