#!/bin/sh
#$ -pe smp 10 # Request 60 CPU cores
#$ -l h_rt=6:00:00 # Request 12h of walltime
#$ -l h_vmem=0.5G # Request 2GB RAM for each core requested
#$ -M c.ringer@qmul.ac.uk # Sends notifications email to this address
#$ -m bea # Emails are sent on begin, end and abortion
#$ -t 1-10
#$ -tc 10

cd /data/autoScratch/weekly/$USER/$1/ # Move to the scratch directory

for ((i = 0; i <= 9; i++))
do
    java -jar -xmx500m GVGAI.jar "rhea_mcts_$i.csv" RHEA MCTS $i 10 &
done

