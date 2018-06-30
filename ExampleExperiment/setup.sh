#!/bin/sh
#$ -pe smp 30 # Request 60 CPU cores
#$ -l h_rt=12:00:00 # Request 12h of walltime
#$ -l h_vmem=2G # Request 2GB RAM for each core requested
#$ -M c.ringer@qmul.ac.uk # Sends notifications email to this address
#$ -m bea # Emails are sent on begin, end and abortion
#$ -t 1-10
#$ -tc 10

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters (1): ./run.sh <Folder>"
    exit -1
fi

mkdir -p /data/autoScratch/weekly/$USER/$1 # Create a new directory for our job

cp -R $HOME/GVGAI_jar/GVGAI.jar /data/autoScratch/weekly/$USER/$1
cp -R $HOME/GVGAI_jar/examples/ /data/autoScratch/weekly/$USER/$1/
