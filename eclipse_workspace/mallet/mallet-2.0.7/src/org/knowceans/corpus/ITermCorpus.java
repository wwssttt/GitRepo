/*
 * Created on 07.06.2007
 */
package org.knowceans.corpus;

public interface ITermCorpus extends ICorpus {

	/**
	 * get corpus in term frequency representation
	 * 
	 * @return termfreqs[0 = terms, 1 = freqs][m][t]
	 */
	int[][][] getDocTermsFreqs();

}
