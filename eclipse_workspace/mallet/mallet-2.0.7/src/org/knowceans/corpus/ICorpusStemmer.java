package org.knowceans.corpus;

/**
 * Stemming functionality for a corpus. As the ICorpusResolver, this follows an
 * operator design rather than aggregation of stemming in the corpus itself.
 * 
 * @author gregor
 * 
 */
public interface ICorpusStemmer {

	/**
	 * stem the list of terms
	 * 
	 * @param terms
	 * @return stemmed terms
	 */
	public String[] stemTerms(String[] terms);

	/**
	 * stem the term. This method should be synchronised if the stemmer is not
	 * thread-safe.
	 * 
	 * @param term
	 * @return
	 */
	public String stem(String term);

	/**
	 * apply stemming to the corpus, updating both the tokens and term index in
	 * its resolver.
	 * 
	 * @param corpus a corpus with a resolver
	 * @return the old2new mapping applied to the corpus
	 */
	public int[] stem(NumCorpus corpus);

}