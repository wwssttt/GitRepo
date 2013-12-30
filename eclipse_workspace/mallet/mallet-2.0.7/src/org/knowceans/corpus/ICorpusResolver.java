package org.knowceans.corpus;

public interface ICorpusResolver extends ICorpusIdResolver {

	public static final int KDOCS = 0;
	public static final int KTERMS = 1;
	public static final int KAUTHORS = 2;
	public static final int KCATEGORIES = 3;
	public static final int KTAGS = 4;
	public static final int KVOLS = 5;
	public static final int KYEARS = 6;
	// document name
	public static final int KDOCNAME = 7;
	// full reference for easy lookup
	public static final int KDOCREF = 8;
	// allow to display abstract
	public static final int KDOCCONTENT = 9;
	// term source information (stem mappings etc.)
	public static final int KTERMSOURCE = 10;

	/**
	 * resolve the numeric label id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveCategory(int i);

	/**
	 * resolve the numeric author id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveAuthor(int i);

	/**
	 * resolve the numeric term id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveDocTitle(int i);

	/**
	 * resolve the numeric term id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveDocName(int i);

	/**
	 * resolve the numeric term id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveDocRef(int i);

	/**
	 * resolve document content (abstract or body)
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveDocContent(int i);

	/**
	 * resolve the numeric volume id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveVolume(int i);

	/**
	 * resolve the numeric year id
	 * 
	 * @param i
	 * @return
	 */
	public abstract String resolveYear(int i);
}