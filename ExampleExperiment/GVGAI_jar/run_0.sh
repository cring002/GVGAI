#!/bin/sh
#$ -pe smp 10 # Request 60 CPU cores
#$ -l h_rt=6:00:00 # Request h of walltime
#$ -l h_vmem=0.5G # Request 0.5GB RAM for each core requested
#$ -M c.ringer@qmul.ac.uk # Sends notifications email to this address
#$ -m bea # Emails are sent on begin, end and abortion

cd /data/autoScratch/weekly/$USER/$1/ # Move to the scratch directory

for ((i = 0; i <= 9; i++))
do
    java -jar -Xmx500m GVGAI.jar "rhcp_rhea_$i.csv" RHCP RHEA $i 10 &
done

wait
echo "done"

