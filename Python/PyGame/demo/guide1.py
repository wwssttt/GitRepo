#!/usr/bin python
#--coding:utf-8---
######################
#guide of writing game with pygame
######################

from pygame.locals import *
from sys import exit
import pygame

bg_image_filename = "sushiplate.jpg"
mouse_image_filename = "fugu.png"

pygame.init()

screen = pygame.display.set_mode((640,480),0,32)
pygame.display.set_caption("The FIrst Guide")

bg = pygame.image.load(bg_image_filename).convert()
mouse_cursor = pygame.image.load(mouse_image_filename).convert_alpha()

while True:
    for event in pygame.event.get():
        if event.type == QUIT:
            exit()
    screen.blit(bg,(0,0))

    x,y = pygame.mouse.get_pos()
    x -= mouse_cursor.get_width()/2
    y -= mouse_cursor.get_height()/2

    screen.blit(mouse_cursor,(x,y))

    pygame.display.update()
