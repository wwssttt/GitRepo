/*
 * Created on Jul 23, 2009
 */
package org.knowceans.corpus;

import java.util.Random;

/**
 * ISplitCorpus allows a corpus to resize and split a cross validation data set.
 * 
 * @author gregor
 */
public interface ISplitCorpus {

	/**
	 * splits two child corpora of size 1/nsplit off the original corpus, which
	 * itself is left unchanged (except storing the splits). The corpora can be
	 * retrieved using getTrainCorpus and getTestCorpus after using this
	 * function.
	 * 
	 * @param order number of partitions
	 * @param split 0-based split of corpus returned
	 * @param rand random source (null for reusing existing splits)
	 */
	public void split(int order, int split, Random rand);

	/**
	 * called after split()
	 * 
	 * @return the training corpus according to the last splitting operation
	 */
	public ICorpus getTrainCorpus();

	/**
	 * called after split()
	 * 
	 * @return the test corpus according to the last splitting operation
	 */
	public ICorpus getTestCorpus();

	/**
	 * get the original ids of documents
	 * 
	 * @return [training documents, test documents]
	 */
	public int[][] getSplit2corpusDocIds();
}
