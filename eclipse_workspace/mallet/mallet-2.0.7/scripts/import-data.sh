#!/bin/bash
# Run this script from 'scripts' directory of mallet directory

# One by one import data from each project version into Mallet format
dirs=`ls -d ../data/`
for  dir in $dirs
do	
	name=`basename $dir`
	echo $name
	if [[ $name == "amazon" ]]
	then
		continue
	fi

	# Create a directory named LDA and put all of the mallet traning and test 
	# data in it
	rm -rf ${dir}"LDA"
	mkdir ${dir}"LDA"

	# Iterate over every project version
	versions=`ls -d $dir*/`
        echo $versions
	for version in $versions
	do	
		name=`basename $version`
		if [ $name == "LDA" ]
		then
			continue
		fi

		echo $name
		# Import data in mallet format
		mkdir -p ${dir}"LDA"
		../bin/mallet import-dir --input $version --output ${dir}"LDA/"${name}".mallet" --keep-sequence
		# Split data into 80% training and 20% testing
		../bin/mallet split --input ${dir}"LDA/"${name}".mallet" --training-file ${dir}"LDA/"${name}"-train.mallet" --testing-file ${dir}"LDA/"${name}"-test.mallet" --training-portion 0.8
	done
done
