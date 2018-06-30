#!/bin/sh

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters (1): ./run.sh <Folder>"
    exit -1
fi

mkdir -p /data/autoScratch/weekly/$USER/$1 # Create a new directory for our job

cp -R $HOME/GVGAI_jar/GVGAI.jar /data/autoScratch/weekly/$USER/$1
cp -R $HOME/GVGAI_jar/examples/ /data/autoScratch/weekly/$USER/$1/
