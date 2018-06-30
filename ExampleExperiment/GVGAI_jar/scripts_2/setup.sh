#!/bin/sh

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters (1): ./run.sh <Folder>"
    exit -1
fi

mkdir -p /data/autoScratch/weekly/$USER/$1 # Create a new directory for our job

cp -R $HOME/GVGAI_jar/GVGAI.jar /data/autoScratch/weekly/$USER/$1
cp -R $HOME/GVGAI_jar/examples/ /data/autoScratch/weekly/$USER/$1/

qsub run_rhcp_rhea.sh $1
qsub run_rhea_rhcp.sh $1
qsub run_rhcp_mcts.sh $1
qsub run_mcts_rhcp.sh $1
qsub run_rhcp_rand.sh $1
qsub run_rand_rhcp.sh $1

qsub run_rhea_rand.sh $1
qsub run_rand_rhea.sh $1
qsub run_rhea_mcts.sh $1
qsub run_mcts_rhea.sh $1


