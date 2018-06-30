#!/bin/sh

cd /data/autoScratch/weekly/$USER/$1/ # Move to the scratch directory

for ((i = 0; i <= 9; i++))
do
    java -jar GVGAI.jar "rhcp_rhea_$i.csv" RHCP RHEA $i 10 &
    java -jar GVGAI.jar "rhcp_mcts_$i.csv" RHCP MCTS $i 10 &
    java -jar GVGAI.jar "rhcp_rand_$i.csv" RHCP RAND $i 10 &
    java -jar GVGAI.jar "rhea_mcts_$i.csv" RHEA MCTS $i 10 &
    java -jar GVGAI.jar "rhea_rand_$i.csv" RHEA RAND $i 10 &
    java -jar GVGAI.jar "mcts_rand_$i.csv" MCTS RAND $i 10 &
done