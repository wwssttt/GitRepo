/*
 * Created on 09.06.2007
 */
package org.knowceans.corpus;

/**
 * ILabelCorpus is a corpus with label information. It can be used to add an
 * additional observed node to a topic model.
 * 
 * @author gregor
 */
public interface ILabelCorpus extends ICorpus {

	public static final int LDOCS = -2;
	public static final int LTERMS = -1;
	// these are the rows of the data field in label corpus
	public static final int LAUTHORS = 0;
	public static final int LCATEGORIES = 1;
	public static final int LTAGS = 2;
	public static final int LVOLS = 3;
	public static final int LYEARS = 4;
	public static final int LREFERENCES = 5;
	public static final int LMENTIONS = 6;

	// public static final int LCITATIONS = 7;

	/**
	 * array with label ids for documents
	 * 
	 * @param kind of labels
	 * @return
	 */
	int[][] getDocLabels(int kind);

	/**
	 * get the number of tokens in the label field
	 * 
	 * @param kind
	 * @return
	 */
	public int getLabelsW(int kind);

	/**
	 * get the number of distinct labels in the label field
	 * 
	 * @param kind
	 * @return
	 */
	public int getLabelsV(int kind);

}
