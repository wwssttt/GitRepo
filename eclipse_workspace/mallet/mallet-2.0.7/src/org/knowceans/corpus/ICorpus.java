/*
 * Created on 07.06.2007
 */
package org.knowceans.corpus;

import java.util.Random;

public interface ICorpus {

	int getNumTerms();

	int getNumDocs();

	int getNumWords();

	int[][] getDocWords(Random rand);

	int[] getDocWords(int i, Random rand);

}
