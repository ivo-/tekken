#!/bin/bash

# Building requirements
# gcc (g++) 4.5 or greater
# wxWidgets 2.8 devel

# On Ubuntu, you need to install:
# libwxgtk2.8-dev

# On Fedora, the requirements are
# wxGTK-devel

g++ -std=c++0x -O2 -o recognizer *.cpp `wx-config --cflags --libs`
