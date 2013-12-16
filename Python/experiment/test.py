#!/usr/bin python
#coding:utf-8
############################
#give some test function
############################

#show mae and rmse trends of cold-law methods with different coefficients
def showErrorTrendWithDifferentCoeff_ColdLaw(playlistDict,songDict):
  coeffs = [x / 10 for x in range(0,100,1)]
  maes = []
  rmses = []
  for coeff in coeffs:
    mae,rmse = MAEandRMSE(playlistDict,songDict,3,coeff)
    maes.append(mae)
    rmses.append(rmse)
  plt.plot(coeffs,maes,label="MAE")
  plt.plot(coeffs,rmses,label="RMSE")
  plt.title("MAE and RMSE trends of Different Cold Coefficients")
  plt.xlabel("coefficient")
  plt.ylabel("error")
  plt.legend(loc="upper right")
  plt.savefig("img/coldlaw.png")
  plt.show()

#show weight trends of different coefficients
def showColdLawWithDifferentCoeff():
  coeffs = [0.25,0.5,0.75,1.0,5.0]
  x = range(0,20,1)
  for coeff in coeffs:
    weight = [1*math.pow(math.e,-1*coeff*delta) for delta in x]
    label = "coeff = %f" % coeff
    plt.plot(x,weight,label=label)
  plt.xlabel("time")
  plt.ylabel("weight")
  plt.title("Weight Trend of Cold Law with Different Coefficients")
  plt.legend(loc = "upper right")
  plt.savefig("img/cold-law.png")
  plt.show()

def testAverage():
  print '################Average####################'
  songDict = readSongFromFile()
  playlistDict = readPlaylistFromFile()
  start_time = time.time()
  mae,rmse = MAEandRMSE(playlistDict,songDict,1)
  print 'MAE = ',mae
  print 'RMSE = ',rmse
  print 'Average Consumed: %ds' % (time.time()-start_time)

def testMostSimilar():
  print '################Most Similar####################'
  songDict = readSongFromFile()
  playlistDict = readPlaylistFromFile()
  start_time = time.time()
  mae,rmse = MAEandRMSE(playlistDict,songDict,2)
  print 'MAE = ',mae
  print 'RMSE = ',rmse
  print 'MostSimilar Consumed: %ds' % (time.time()-start_time)

def testColdLaw():
  print '################Cold Law####################'
  songDict = readSongFromFile()
  playlistDict = readPlaylistFromFile()
  start_time = time.time()
  mae,rmse = MAEandRMSE(playlistDict,songDict,3)
  print 'MAE = ',mae
  print 'RMSE = ',rmse
  print 'Cold Law Consumed: %ds' % (time.time()-start_time)

def testArima():
  print '################ARIMA####################'
  songDict = readSongFromFile()
  playlistDict = readPlaylistFromFile()
  start_time = time.time()
  mae,rmse = MAEandRMSE(playlistDict,songDict,4)
  print 'MAE = ',mae
  print 'RMSE = ',rmse
  print 'ARIMA Consumed: %ds' % (time.time()-start_time)

def testHybrid():
  print '################Hybrid####################'
  songDict = readSongFromFile()
  playlistDict = readPlaylistFromFile()
  start_time = time.time()
  mae,rmse = MAEandRMSE(playlistDict,songDict,5)
  print 'MAE = ',mae
  print 'RMSE = ',rmse
  print 'Hybrid Consumed: %ds' % (time.time()-start_time)
