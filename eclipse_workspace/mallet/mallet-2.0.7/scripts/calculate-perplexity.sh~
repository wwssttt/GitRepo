#!/bin/bash
# Run this script from 'scripts' directory of mallet directory

# One by one traverse each project
dirs=`ls -d ../../../data/`
for dir in $dirs
do
	# Read names of major versions
	project=`basename $dir`
	if [[ $project == "amazon" ]]
	then 
		continue
	fi
	# Read the version file in the project folder
	# Version file has on each line a name of a version for which
	# we would like to run LDA over a range of number of topics and
	# calculae perplexity for each number of topics
	versionFile=${dir}"version-major.txt"
	# Convert to unix format
	# Following command is equivalent to running dos2unix command
	# Some systems do not have dos2unix installed, hence using tr to delete '/r'
	# that Windows puts in the files
	tr -d '\r' < $versionFile > ${dir}"version-major-unix.txt"
	text=`cat ${dir}"version-major-unix.txt"`
	rm -f ${dir}"version-major-unix.txt"

	# Since projects such as eclipse, drools, etc. are quite bigger, we run LDA for greater range of number of topics
	if [[ $project == "eclipse" || $project == "drools" || $project == "coffeemud" || $project == "netbeans" ]]
	then
		numTopics=({20..200..15})
	else
		numTopics=({100..200..10})
	fi

	# Iterate over major project versions
	for version in $text
	do
		# Train LDA model for a range of number of topics
		# and calculate perplexity for each
		for ((idx=0; idx<${#numTopics[@]}; idx++))
		do
			echo -e "#######################################################################"
			echo -e "         Project: "${project}" Version: "${version}" Iteration: "${numTopics[$idx]}
			echo -e "#######################################################################"

			# Train LDA model
			../bin/mallet train-topics --num-topics ${numTopics[$idx]} --input ${dir}"LDA/"${version}"-train.mallet" --evaluator-filename ${dir}"LDA/"${version}"-evaluator" --num-iterations 100 --show-topics-interval 1000 --alpha 1 --beta .07
			# Calculate perplexity on test data
			# First calculate document probabilities for each 
			# test document
			../bin/mallet evaluate-topics --evaluator ${dir}"LDA/"${version}"-evaluator" --input ${dir}"LDA/"${version}"-test.mallet" --output-doc-probs ${dir}"LDA/"${version}"-docprobs.txt"
			# Calculate document lengths to calculate perplexity
			../bin/mallet run cc.mallet.util.DocumentLengths --input ${dir}"LDA/"${version}"-test.mallet" > ${dir}"LDA/"${version}"-doclengths.txt"
			# Calculate perplexity
			docLen=($(cat ${dir}"LDA/"${version}"-doclengths.txt"))
			docProb=($(cat ${dir}"LDA/"${version}"-docprobs.txt"))
			logLikelihood=0
			totalWords=0
			for ((i=0; i<${#docProb[@]}; i++))
			do
				logLikelihood=`echo "$logLikelihood + ${docProb[$i]}" | bc -l`
				totalWords=`echo "$totalWords + ${docLen[$i]}" | bc -l`
			done
			perplexity=`echo "e(-1*$logLikelihood / $totalWords)" | bc -l`
			ppx[$idx]=$perplexity
		done
		# Write perplexity values to a file
		rm -f ${dir}"LDA/"${version}"-perplexity.txt"
		for ((i=0; i<${#numTopics[@]}; i++))
		do
			echo -e ${numTopics[$i]}"\t"${ppx[$i]} >> ${dir}"LDA/"${version}"-perplexity.txt"
		done

                # Delete intermediate files
                rm -f ${dir}"LDA/"${version}"-evaluator"
                rm -f ${dir}"LDA/"${version}"-doclengths.txt"
                rm -f ${dir}"LDA/"${version}"-docprobs.txt"
	done
done
