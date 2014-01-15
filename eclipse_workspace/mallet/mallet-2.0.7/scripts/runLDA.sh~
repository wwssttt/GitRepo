#!/bin/bash
# Run this script from 'scripts' directory of mallet directory

# One by one traverse each project
dirs=`ls -d ../data/`
for dir in $dirs
do
	# Read names of major versions
	project=`basename $dir`
	if [[ $project == "amazon" ]]
	then 
		continue
	fi
	# Read the version file in the project folder
	# Version file has following information in comma separated manner
	# <version-name>,<LOC>,<number of source files>,<optimal number of topics>
	versionFile=${dir}"version.txt"
	# Convert to unix format
	# Following command is equivalent to running dos2unix command
	# Some systems do not have dos2unix installed, hence using tr to delete '/r'
	# that Windows puts in the files
	tr -d '\r' < $versionFile > ${dir}"version-unix.txt"
	text=`cat ${dir}"version-unix.txt"`
	rm -f ${dir}"version-unix.txt"

	# Iterate over major project versions
	for line in $text
	do
		version=`echo $line | cut -f 1 -d ,`
		numTopics=`echo $line | cut -f 4 -d ,`
		# Run LDA
		echo -e "#######################################################################"
		echo -e "          Project: "${project}" Version: "${version}" # Topics: "${numTopics[$idx]}
		echo -e "#######################################################################"
		../bin/mallet train-topics --num-topics ${numTopics[$idx]} --input ${dir}"LDA/"${version}".mallet" --show-topics-interval 1000 --alpha 1 --beta .07 --output-doc-topics ${dir}"LDA/"${version}"-doc-topics.txt" --output-topic-keys ${dir}"LDA/"${version}"-topic-keys.txt"

		# Convert Mallet doc-topics output to MATLAB readable sparce matrix form
		# Following line creates a file with three columns - row, column, count i.e. docID, wordID and count of occurrence
		# It essentially creates a sparce word-doc matrix
		awk -F '\t' '{if (NR > 1) for(i=3; i<NF; i+=2) if ($(i+1) != 0) print ($1+1)"\t"$i+1"\t"$(i+1);}' ${dir}"LDA/"${version}"-doc-topics.txt" > ${dir}"LDA/"${version}"-DP.txt"
		
		# Create a file with names of source code files and give them IDs starting from 1
		awk -F '\t' '{if (NR > 1) print (NR-2)","$2;}' ${dir}"LDA/"${version}"-doc-topics.txt" > ${dir}"LDA/"${version}"-fileNames.txt"
	done
done
