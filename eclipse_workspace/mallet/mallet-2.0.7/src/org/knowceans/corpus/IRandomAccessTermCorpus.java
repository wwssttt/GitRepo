/*
 * Created on 14.05.2006
 */
package org.knowceans.corpus;

import java.util.ArrayList;
import java.util.Map;

import org.knowceans.map.IBijectiveMap;

/**
 * IRandomAccessCorpus provides term corpus functionality that allows direct
 * access to all fields in the corpus, i.e., the complete lists / map of indices
 * can be read. This (older method used for instance with Freshmind) may be used
 * instead of the CorpusResolver solution (which, however, is more mature).
 * 
 * @author gregor
 * @version $Id: IRandomAccessTermCorpusFm.java,v 1.2 2006/11/22 07:06:39 gregor
 *          Exp $
 */
public interface IRandomAccessTermCorpus extends ITermCorpus {

	public ArrayList<String> getDocNames();

	public ArrayList<Map<Integer, Integer>> getDocTerms();

	public IBijectiveMap<String, Integer> getTermIndex();

	public int getNwords();

}