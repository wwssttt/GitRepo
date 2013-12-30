package org.knowceans.corpus;

/**
 * @author gregor
 * 
 */
public interface ICorpusIdResolver {

	/**
	 * check whether labels exist
	 * 
	 * @param i
	 * @return 0 for no label values, 1 for yes, 2 for loaded, -1 for illegal
	 *         index
	 */
	public abstract int hasLabelKeys(int kind);

	/**
	 * resolve the numeric term id
	 * 
	 * @param t
	 * @return
	 */
	public abstract String resolveTerm(int t);

	/**
	 * find id for string term or return -1 if unknown
	 * 
	 * @param term
	 * @return
	 */
	public abstract int getTermId(String term);

	/**
	 * resolve a label
	 * 
	 * @param type
	 * @param id
	 * @return
	 */
	public abstract String resolveLabel(int type, int id);

	/**
	 * reverse lookup of label to id
	 * 
	 * @param type
	 * @param label
	 * @return
	 */
	public abstract int getId(int type, String label);
}