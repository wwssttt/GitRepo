#!/usr/bin python
#--coding:utf-8--

#1 - Import library
import pygame
from pygame.locals import *

#2 - Initialize the game
pygame.init()
width,height = 640,480
screen = pygame.display.set_mode((width,height))

#3 - load images
rabit = pygame.image.load("resources/images/dude.png")
grass = pygame.image.load("resources/images/grass.png")
castle = pygame.image.load("resources/images/castle.png")

#4 - keep looping through
while 1:
    #5 - clear the screen before drawing it agamin
    screen.fill(0)
    #6 - draw the screen elements
    #draw rabit
    #repeat drawing grass
    for x in range(width/grass.get_width()+1):
      for y in range(height/grass.get_height()+1):
        screen.blit(grass,(x*100,y*100))
    #draw castle
    screen.blit(castle,(0,30))
    screen.blit(castle,(0,135))
    screen.blit(castle,(0,240))
    screen.blit(castle,(0,345))
    screen.blit(rabit,(100,100))
    #7 - update tje screen
    pygame.display.flip()
    #8 - loop through the events
    for event in pygame.event.get():
        #check if the event is the X button
        if event.type == pygame.QUIT:
            #if it is qiut the game
            pygame.quit()
            exit(0)
