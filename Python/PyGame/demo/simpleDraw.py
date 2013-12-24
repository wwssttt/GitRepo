#!/usr/bin python
#--coding:utf-8----
#####################
#guide of pygame
#####################
import pygame
from pygame.color import THECOLORS
import sys
import random

#init
pygame.init()
#set title of window
screencaption = pygame.display.set_caption('PyGame Guide')
#new a screen with fixed size
screen = pygame.display.set_mode([640,480])
#set fill color
screen.fill([255,255,255])
#draw a circle
pygame.draw.circle(screen,THECOLORS["red"],[100,400],30,2)
#define a rect
my_rect = pygame.Rect([250,150,300,200]) # my_rect = [250,150,300,200]
#draw a rect
pygame.draw.rect(screen,THECOLORS["blue"],my_rect,3)
pygame.draw.rect(screen,[255,0,0],[150,50,1,1],1)
#generate some random circle and rect
for i in range(10):
    zhijing = random.randint(0,100)
    width = random.randint(0,255)
    height = random.randint(0,100)
    top = random.randint(0,400)
    left = random.randint(0,500)
    pygame.draw.circle(screen,THECOLORS["red"],[top,left],zhijing,1)
    pygame.draw.rect(screen,THECOLORS["blue"],[left,top,width,height],2)
#set pixel of some random position
for i in range(200000):
    x = random.randint(0,640)
    y = random.randint(0,480)
    r = random.randint(0,255)
    g= random.randint(0,255)
    b = random.randint(0,255)
    screen.set_at([x,y],[r,g,b])

pygame.display.flip()

while True:
    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            sys.exit()
