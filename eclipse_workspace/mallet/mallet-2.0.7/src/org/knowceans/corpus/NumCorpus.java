/*
 * (C) Copyright 2005, Gregor Heinrich (gregor :: arbylon : net) (This file is
 * part of the lda-j (org.knowceans.lda.*) experimental software package.)
 */
/*
 * lda-j is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 */
/*
 * lda-j is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */
/*
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * Created on Dec 3, 2004
 */
package org.knowceans.corpus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.knowceans.util.ArrayUtils;
import org.knowceans.util.CokusRandom;
import org.knowceans.util.RandomSamplers;
import org.knowceans.util.Vectors;

/**
 * Represents a corpus of documents, using numerical data only.
 * <p>
 * 
 * 
 * @author heinrich
 */
public class NumCorpus implements ICorpus, ITermCorpus, ISplitCorpus {

	/**
	 * test corpus reading and splitting
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// NumCorpus nc = new NumCorpus("corpus-example/berry95");
		NumCorpus nc = new NumCorpus("corpus-example/nips");
		CokusRandom rand = new CokusRandom();

		boolean dofilter = true;
		boolean doresolve = false;

		if (doresolve) {
			nc.getResolver();
		}

		if (dofilter) {
			// filterTest(nc, rand);
		}

		nc.split(10, 0, rand);
		System.out.println("train");
		NumCorpus ncc = (NumCorpus) nc.getTrainCorpus();
		System.out.println(ncc);
		if (dofilter) {
			filterTest(ncc, rand);
		}
		System.out.println(ncc);
		int[][] x = ncc.getDocWords(rand);
		System.out.println(Vectors.print(x));
		System.out.println("test");
		ncc = (NumCorpus) nc.getTestCorpus();
		System.out.println(ncc);
		x = ncc.getDocWords(rand);
		System.out.println(Vectors.print(x));
		System.out.println("document mapping");
		System.out.println(Vectors.print(nc.getSplit2corpusDocIds()));
		System.out.println(Vectors.print(nc.getCorpus2splitDocIds()));
	}

	protected static void filterTest(NumCorpus corpus, Random rand) {
		// filter all short documents
		final int mint = corpus.getMinDocTerms();
		System.out.println("orig numdocs = " + corpus.numDocs);
		corpus.filterDocs(new DocPredicate() {
			@Override
			public boolean doesApply(NumCorpus self, int m) {
				if (self.docs[m].numWords <= mint) {
					return false;
				}
				return true;
			}
		}, rand);
		System.out.println("new numdocs = " + corpus.numDocs);

		System.out.println("orig numterms = " + corpus.numTerms);
		corpus.filterTermsDf(3, 10);
		System.out.println("new numterms = " + corpus.numTerms);
	}

	protected Document[] docs;

	protected int numTerms;

	protected int numDocs;

	protected int numWords;

	protected boolean debug = false;

	protected String dataFilebase = null;

	/**
	 * permutation of the corpus used for splitting
	 */
	protected int[] splitperm;

	/**
	 * starting points of the corpus segments
	 */
	protected int[] splitstarts;

	protected NumCorpus trainCorpus;

	protected NumCorpus testCorpus;

	/**
	 * for splitting, these are the original document ids [0] = training, [1] =
	 * test. previously called: origDocIds
	 */
	protected int[][] split2corpusDocIds;
	/**
	 * these are the corpus ids to split ids, with training positive and test
	 * using mtest = -mcorpus - 1
	 */
	protected int[] corpus2splitDocIds;

	/**
	 * term element before which a paragraph ends. Iterating through this allows
	 * to read the corpus by paragraph.
	 * <p>
	 * when paragraph mode is enabled, duplicate term ids in one document's term
	 * vector are allowed. Conversion to docWords is transparent, merging to
	 * unstructured docs using mergeDocuments.
	 */
	protected int[][] parbounds;

	/**
	 * word before a paragraph end.
	 */
	protected int[][] wordparbounds;

	protected int readlimit = -1;

	protected CorpusResolver resolver;

	/**
	 * is this corpus derived from another one, i.e., we should not save to the
	 * original corpus
	 */
	protected boolean derived = false;

	/**
	 * @param dataFilebase (without .corpus)
	 */
	public NumCorpus(String dataFilebase) {
		this.dataFilebase = dataFilebase;
		read(dataFilebase + ".corpus");
	}

	/**
	 * init the corpus with a reduced set of documents
	 * 
	 * @param dataFilebase (without .corpus)
	 * @param readlimit
	 */
	public NumCorpus(String dataFilebase, int readlimit) {
		this.dataFilebase = dataFilebase;
		this.readlimit = readlimit;
		this.dataFilebase = dataFilebase;
		read(dataFilebase + ".corpus");
	}

	public NumCorpus() {

	}

	public NumCorpus(Document[] docs, int numTerms, int numWords) {
		this.numTerms = numTerms;
		this.numWords = numWords;
		numDocs = docs.length;
		this.docs = docs;
	}

	static int OFFSET = 0; // offset for reading data

	/**
	 * read a file in "pseudo-SVMlight" format. The format is extended by a
	 * paragraph-aware version that repeats the pattern
	 * <p>
	 * nterms (term:freq){nterms}
	 * <p>
	 * for each paragraph in the document. This way, each paragraph
	 * 
	 * @param dataFilename
	 */
	public void read(String dataFilename) {
		int length, count = 0, word, n, nd, nt, ns, nw = 0;

		if (debug)
			System.out.println("reading data from " + dataFilename);

		try {
			ArrayList<Document> cdocs = new ArrayList<Document>();
			BufferedReader br = new BufferedReader(new FileReader(dataFilename));
			nd = 0;
			nt = 0;
			String line = "";
			parbounds = null;
			boolean parmode = false;
			while ((line = br.readLine()) != null) {
				// one document per line
				String[] fields = line.trim().split("\\s+");
				// empty documents start with a 0 but are used in corpus
				if (fields[0].equals(""))
					continue;
				length = Integer.parseInt(fields[0]);
				// if single paragraph
				if (length == fields.length - 1) {

					Document d = new Document();
					cdocs.add(d);
					d.setNumTerms(length);
					d.setNumWords(0);
					d.setTerms(new int[length]);
					d.setCounts(new int[length]);

					for (n = 0; n < length; n++) {
						// fscanf(fileptr, "%10d:%10d", &word, &count);
						String[] numbers = fields[n + 1].split(":");
						if (numbers[0].equals("") || numbers[0].equals(""))
							continue;
						word = Integer.parseInt(numbers[0]);
						count = (int) Float.parseFloat(numbers[1]);
						nw += count;
						word = word - OFFSET;
						d.setTerm(n, word);
						d.setCount(n, count);
						d.setNumWords(d.getNumWords() + count);
						if (word >= nt) {
							nt = word + 1;
						}
					}
				} else {
					// more than one paragraph
					// TODO: merge with other case
					parmode = true;
					int nextpar = 0;
					int token = 0;
					Document pd = new Document();
					cdocs.add(pd);
					while (nextpar < fields.length) {
						length = Integer.parseInt(fields[nextpar]);
						nextpar += length + 1;
						token++;
						Document d = new Document();
						d.setNumTerms(length);
						d.setNumWords(0);
						d.setTerms(new int[length]);
						d.setCounts(new int[length]);

						for (n = 0; n < length; n++, token++) {
							String[] numbers = fields[token].split(":");
							if (numbers[0].equals("") || numbers[0].equals(""))
								continue;
							word = Integer.parseInt(numbers[0]);
							count = (int) Float.parseFloat(numbers[1]);
							nw += count;
							word = word - OFFSET;
							d.setTerm(n, word);
							d.setCount(n, count);
							d.setNumWords(d.getNumWords() + count);
							if (word >= nt) {
								nt = word + 1;
							}
						}
						pd.addDocument(d);
					}
				}
				// if (nd % 1000 == 0) {
				// System.out.println(nd);
				// }
				nd++;
				// stop if read limit reached
				if (readlimit >= 0 && nd >= readlimit) {
					break;
				}
			}
			numDocs = nd;

			numTerms = nt;
			numWords = nw;
			docs = cdocs.toArray(new Document[] {});
			// if any document in paragraph mode
			if (parmode) {
				parbounds = new int[docs.length][];
				for (int m = 0; m < docs.length; m++) {
					parbounds[m] = docs[m].getParBounds();
				}
			}
			split2corpusDocIds = new int[2][];
			split2corpusDocIds[0] = Vectors.range(0, nd - 1);
			if (debug) {
				System.out.println("number of docs    : " + nd);
				System.out.println("number of terms   : " + nt);
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return
	 */
	public Document[] getDocs() {
		return docs;
	}

	/**
	 * get array of document terms and frequencies
	 * 
	 * @return docs[0 = terms, 1 = frequencies][m][t]
	 */
	public int[][][] getDocTermsFreqs() {
		// words in documents
		int[][][] documents = new int[2][getNumDocs()][];
		for (int i = 0; i < getNumDocs(); i++) {
			documents[0][i] = getDoc(i).getTerms();
			documents[1][i] = getDoc(i).getCounts();
		}
		return documents;
	}

	/**
	 * get matrix with tfidf weighting for corpus
	 * 
	 * @return
	 */
	public double[][] getTfIdf() {
		int[] df = calcDocFreqs();
		double[][] tfidf = new double[numDocs][];
		for (int m = 0; m < numDocs; m++) {
			for (int n = 0; n < docs[m].numTerms; n++) {
				int tf = docs[m].getCount(n);
				double idf = Math
						.log(numDocs / (double) df[docs[m].getTerm(n)]);
				// may add weighting, e.g., 1 + 0.5 * tf / tfmax(m)
				tfidf[m][n] = tf * idf;
			}
		}
		return tfidf;
	}

	/**
	 * get array of paragraph start indices of the documents (term-based)
	 * 
	 * @return
	 */
	public int[][] getDocParBounds() {
		return parbounds;
	}

	/**
	 * get array of paragraph start indices of the documents (word-based)
	 * 
	 * @return
	 */
	public int[][] getDocWordParBounds() {
		if (parbounds == null) {
			return null;
		}
		int[][] psnwords = new int[numDocs][];
		for (int m = 0; m < numDocs; m++) {
			psnwords[m] = getDocWordParBounds(m);
		}
		return psnwords;
	}

	/**
	 * return word-based paragraph starts for doc m
	 * 
	 * @param m
	 * @return
	 */
	private int[] getDocWordParBounds(int m) {
		Document d = docs[m];
		int[] termbounds = d.getParBounds();
		int[] wordbounds = new int[termbounds.length];
		int prevbound = 0;
		int prevwbound = 0;
		for (int j = 0; j < termbounds.length; j++) {
			// sum up frequencies of range corresponding to paragraph
			wordbounds[j] = Vectors.sum(Vectors.sub(d.counts, prevbound,
					termbounds[j] - prevbound)) + prevwbound;
			prevbound = termbounds[j];
			prevwbound = wordbounds[j];
		}
		return wordbounds;
	}

	/**
	 * merge document paragraphs into a single document each.
	 */
	public void mergeDocPars() {
		if (parbounds == null) {
			return;
		}
		for (int i = 0; i < docs.length; i++) {
			docs[i].mergeDocument(null);
		}
		// invalidate paragraph start info
		parbounds = null;
	}

	/**
	 * @param index
	 * @return
	 */
	public Document getDoc(int index) {
		return docs[index];
	}

	/**
	 * Get the documents as vectors of bag of words, i.e., per document, a
	 * scrambled array of term indices is generated.
	 * 
	 * @param rand random number generator or null to use standard generator
	 * @return
	 */
	public int[][] getDocWords(Random rand) {
		// words in documents
		int[][] documents = new int[getNumDocs()][];
		for (int i = 0; i < getNumDocs(); i++) {
			documents[i] = getDocWords(i, rand);
		}
		return documents;
	}

	public int getNumWords() {
		return numWords;
	}

	/**
	 * Get the words of document doc as a scrambled varseq. For paragraph-based
	 * documents, scrambles the paragraphs separately, preserving their
	 * boundaries.
	 * 
	 * @param m
	 * @param rand random number generator or null to omit shuffling
	 * @return
	 */
	public int[] getDocWords(int m, Random rand) {
		if (parbounds == null || parbounds[m] == null
				|| parbounds[m].length == 1) {
			ArrayList<Integer> document = new ArrayList<Integer>();
			for (int i = 0; i < docs[m].getTerms().length; i++) {
				int term = docs[m].getTerms()[i];
				for (int j = 0; j < docs[m].getCount(i); j++) {
					document.add(term);
				}
			}
			// permute words so duplicates aren't juxtaposed
			if (rand != null) {
				Collections.shuffle(document, rand);
			} else {
				// no shuffling
				// Collections.shuffle(document);
			}
			int[] a = (int[]) ArrayUtils.asPrimitiveArray(document, int.class);
			return a;
		} else {
			if (wordparbounds == null) {
				wordparbounds = new int[parbounds.length][];
			}
			wordparbounds[m] = new int[parbounds[m].length];
			int nw = 0;
			int tstart = 0;
			int[] words = new int[Vectors.sum(docs[m].counts)];
			for (int s = 0; s < parbounds[m].length; s++) {
				int tend = parbounds[m][s];
				ArrayList<Integer> par = new ArrayList<Integer>();
				for (int i = tstart; i < tend; i++) {
					int term = docs[m].getTerms()[i];
					for (int j = 0; j < docs[m].getCount(i); j++) {
						par.add(term);
					}
				}
				// permute words so duplicates aren't juxtaposed
				if (rand != null) {
					Collections.shuffle(par, rand);
				} else {
					// no shuffling
					// Collections.shuffle(document);
				}
				for (int i = 0; i < par.size(); i++) {
					words[nw + i] = par.get(i);
				}
				tstart = tend;
				nw += par.size();
			}
			return words;
		}
	}

	/**
	 * @param index
	 * @param doc
	 */
	public void setDoc(int index, Document doc) {
		docs[index] = doc;
	}

	/**
	 * @return
	 */
	public int getNumDocs() {
		return numDocs;
	}

	/**
	 * @return
	 */
	public int getNumTerms() {
		return numTerms;
	}

	public int getNumTerms(int doc) {
		return docs[doc].getNumTerms();
	}

	public int getNumWords(int doc) {
		return docs[doc].getNumWords();
	}

	/**
	 * return the minimum number of words in any document
	 * 
	 * @return
	 */
	public int getMinDocWords() {
		int min = 0;
		if (docs.length > 0) {
			min = docs[0].numWords;
		}
		for (int m = 1; m < numDocs; m++) {
			min = min < docs[m].numWords ? min : docs[m].numWords;
		}
		return min;
	}

	/**
	 * return the maximum number of words in any document
	 * 
	 * @return
	 */
	public int getMaxDocWords() {
		int max = 0;
		for (int m = 0; m < numDocs; m++) {
			max = max > docs[m].numWords ? max : docs[m].numWords;
		}
		return max;
	}

	/**
	 * return the minimum number of terms in any document
	 * 
	 * @return
	 */
	public int getMinDocTerms() {
		int min = 0;
		if (docs.length > 0) {
			min = docs[0].numTerms;
		}
		for (int m = 1; m < numDocs; m++) {
			min = min < docs[m].numTerms ? min : docs[m].numTerms;
		}
		return min;
	}

	/**
	 * return the maximum number of terms in any document
	 * 
	 * @return
	 */
	public int getMaxDocTerms() {
		int max = 0;
		for (int m = 0; m < numDocs; m++) {
			max = max > docs[m].numTerms ? max : docs[m].numTerms;
		}
		return max;
	}

	/**
	 * Get the document ids for a current term. This method uses linear
	 * iteration through the corpus and is intended only for debug purposes.
	 * 
	 * @param term
	 * @return
	 */
	public List<Integer> getDocs(int term) {
		List<Integer> termDocs = new ArrayList<Integer>();
		for (int m = 0; m < numDocs; m++) {
			for (int i = 0; i < docs[m].getTerms().length; i++) {
				if (docs[m].getTerm(i) == term) {
					termDocs.add(m);
				}
			}
		}
		return termDocs;
	}

	/**
	 * @param documents
	 */
	public void setDocs(Document[] documents) {
		docs = documents;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("%s instance:\n", this.getClass()
				.getSimpleName()));
		sb.append(String.format("file base: %s\n", dataFilebase));
		sb.append(String
				.format("docs: M = %d, V = %d, W = %d, N[m] = [%d, %d], T[m] = [%d, %d]\n",
						getNumDocs(), getNumTerms(), getNumWords(),
						getMinDocWords(), getMaxDocWords(), getMinDocTerms(),
						getMaxDocTerms()));

		return sb.toString();
	}

	/**
	 * filter terms by frequency. TODO: paragraph support. The corpus resolver
	 * obtained by getResolver() is updated to the new term mapping.
	 * <p>
	 * If resolver is to be updated, it needs to be loaded prior to calling
	 * filterDocs();
	 * 
	 * @param minDf all more scarce terms are excluded
	 * @param maxDf all more frequent terms are excluded
	 * @return array with new indices in old index elements
	 */
	public int[] filterTermsDf(int minDf, int maxDf) {
		int[] df = calcDocFreqs();
		// rewrite indices
		int[] indices = new int[numTerms];
		int newIndex = 0;
		for (int t = 0; t < numTerms; t++) {
			if (df[t] < minDf || df[t] > maxDf) {
				indices[t] = -1;
			} else {
				indices[t] = newIndex;
				newIndex++;
			}
		}
		// rewrite corpus
		int W = 0;
		for (int m = 0; m < numDocs; m++) {
			List<Integer> tt = new ArrayList<Integer>();
			List<Integer> ff = new ArrayList<Integer>();
			for (int i = 0; i < docs[m].numTerms; i++) {
				int term = docs[m].terms[i];
				if (indices[term] >= 0) {
					tt.add(indices[term]);
					ff.add(docs[m].counts[i]);
					W += docs[m].counts[i];
				}
			}
			docs[m].terms = (int[]) ArrayUtils.asPrimitiveArray(tt, int.class);
			docs[m].counts = (int[]) ArrayUtils.asPrimitiveArray(ff, int.class);
			docs[m].compile();
		}
		numTerms = newIndex;
		numWords = W;
		// map to novel term indices
		if (resolver != null) {
			resolver.filterTerms(indices);
		}
		return indices;
	}

	/**
	 * merge terms by index, for instance to create a stemmed version of the
	 * corpus or to transform indices. TODO: paragraph support. The corpus
	 * resolver obtained by getResolver() must be updated using setTerms() if
	 * second argument null. The frequencies of merged terms add up.
	 * 
	 * @param old2new mapping from old to new indices, numbering must correspond
	 *        to terms, but not all terms need to be represented in old2new.
	 * @param terms new vocabulary, will be updated in resolver if != null
	 */
	public void mergeTerms(int[] old2new, String[] terms) {
		// rewrite corpus
		for (int m = 0; m < numDocs; m++) {
			Map<Integer, Integer> term2freq = new HashMap<Integer, Integer>();
			for (int i = 0; i < docs[m].numTerms; i++) {
				int term = old2new[docs[m].terms[i]];
				Integer freq = term2freq.get(term);
				if (freq == null) {
					freq = docs[m].counts[i];
				} else {
					freq += docs[m].counts[i];
				}
				term2freq.put(term, freq);
			}
			Set<Integer> keys = new TreeSet<Integer>(term2freq.keySet());
			docs[m].terms = new int[keys.size()];
			docs[m].counts = new int[keys.size()];
			int i = 0;
			for (int key : keys) {
				docs[m].terms[i] = key;
				docs[m].counts[i] = term2freq.get(key);
				i++;
			}
			docs[m].compile();
		}
		if (terms != null) {
			numTerms = terms.length;
		} else {
			numTerms = Vectors.max(old2new) + 1;
		}
		if (terms != null) {
			// TODO: source separator as parameter
			getResolver().setTerms(terms, "<");
		}
	}

	/**
	 * predicate to filter the set of documents. Add well-defined random
	 */
	// TODO: also may be applied to terms
	public interface DocPredicate {

		/**
		 * allow access to corpus and current index
		 * 
		 * @param self refers to current corpus
		 * @param m current document index
		 * @return whether to keep in list
		 */
		boolean doesApply(NumCorpus self, int m);
	}

	/**
	 * reduce the size of the corpus to ndocs maximum. This should be called
	 * directly after loading as it only reduces the documents and count.
	 * Consider using split instead.
	 * 
	 * @param ndocs
	 * @param rand scramble documents, if null use the first ndocs
	 * @return old2new indices
	 */
	public int[] reduce(final int ndocs, Random rand) {
		DocPredicate filter = new DocPredicate() {
			int M = ndocs;

			@Override
			public boolean doesApply(NumCorpus self, int m) {
				return M-- > 0;
			}
		};
		return filterDocs(filter, rand);
	}

	/**
	 * filter documents. Also updates the resolver. Vocabulary must be rebuilt
	 * separately because frequencies change: use filterTermsDf().
	 * <p>
	 * If resolver is to be updated, it needs to be loaded prior to calling
	 * filterDocs();
	 * 
	 * @param filter predicate to keep documents in list
	 * @param rand random number generator to be used generate a random
	 *        permutation, null if no random permutation
	 * 
	 * @return old2new indices
	 */
	public int[] filterDocs(DocPredicate filter, Random rand) {
		int[] perm = Vectors.range(0, numDocs - 1);
		if (rand != null) {
			RandomSamplers rs = new RandomSamplers(rand);
			perm = rs.randPerm(perm);
		}
		List<Integer> newDocsList = new ArrayList<Integer>();
		int[] old2new = new int[numDocs];
		for (int m = 0; m < numDocs; m++) {
			int mperm = perm[m];
			if (filter.doesApply(this, mperm)) {
				// System.out.println(String.format("doc %d -> %d", mperm,
				// newDocsList.size()));
				// add to new list
				old2new[mperm] = newDocsList.size();
				newDocsList.add(mperm);
			} else {
				old2new[mperm] = -1;
			}
		}
		int newW = 0;
		Document[] newDocs = new Document[newDocsList.size()];
		for (int m = 0; m < newDocsList.size(); m++) {
			newDocs[m] = docs[newDocsList.get(m)];
			newW += newDocs[m].numWords;
		}
		docs = newDocs;
		numDocs = newDocs.length;
		numWords = newW;

		if (resolver != null) {
			resolver.filterDocs(old2new);
		}

		return old2new;
	}

	/**
	 * calculates the document frequencies of the terms
	 * 
	 * @return
	 */
	public int[] calcDocFreqs() {
		// we construct term frequencies manually even if there may
		// be another source
		int[] df = new int[numTerms];
		for (int m = 0; m < numDocs; m++) {
			for (int t = 0; t < docs[m].numTerms; t++) {
				df[docs[m].terms[t]]++;
			}
		}
		return df;
	}

	/**
	 * create a map from term to documents, where documents are represented by
	 * an id and the respective term frequency
	 * 
	 * @return
	 */
	public HashMap<Integer, Map<Integer, Integer>> getTermDocMap() {
		HashMap<Integer, Map<Integer, Integer>> termDocFreqIndex = new HashMap<Integer, Map<Integer, Integer>>();

		for (int m = 0; m < numDocs; m++) {
			Document document = docs[m];
			// tokenize document
			for (int i = 0; i < document.numTerms; i++) {
				Map<Integer, Integer> doc2freq = termDocFreqIndex.get(document
						.getTerm(i));
				// term still unknown
				if (doc2freq == null) {
					doc2freq = new HashMap<Integer, Integer>();
					termDocFreqIndex.put(document.getTerm(i), doc2freq);
				}
				doc2freq.put(m, document.getCount(i));
			}
		}
		return termDocFreqIndex;
	}

	/**
	 * splits two child corpora of size 1/nsplit off the original corpus, which
	 * itself is left unchanged (except storing the splits). The corpora can be
	 * retrieved using getTrainCorpus and getTestCorpus after using this
	 * function.
	 * <p>
	 * IMPORTANT: If labels are used in the split corpora, the
	 * getDocLabels(kind) method needs to be called before split() to load the
	 * data for splitting.
	 * <p>
	 * IMPORTANT: If resolvers are to be used in the split corpora, the
	 * getResolver() method needs to be called before split() to create the
	 * initial resolver.
	 * 
	 * @param order number of partitions
	 * @param split 0-based split of corpus returned
	 * @param rand random source (null for reusing existing splits)
	 */
	// @Override
	public void split(int order, int split, Random rand) {

		int Mtest;
		int testStart, testEnd;
		int numTestWords;
		if (rand != null) {
			RandomSamplers rs = new RandomSamplers(rand);
			splitperm = rs.randPerm(numDocs);
			splitstarts = new int[order + 1];
		}
		for (int p = 0; p <= order; p++) {
			splitstarts[p] = Math.round(numDocs * (p / (float) order));
		}
		testStart = splitstarts[split];
		testEnd = splitstarts[split + 1];
		Mtest = testEnd - testStart;
		split2corpusDocIds = new int[][] { new int[numDocs - Mtest],
				new int[Mtest] };
		corpus2splitDocIds = new int[numDocs];
		Document[] trainDocs = new Document[numDocs - Mtest];
		Document[] testDocs = new Document[Mtest];

		int mtrain = 0;
		int mtest = 0;
		numTestWords = 0;
		for (int m = 0; m < numDocs; m++) {
			// in test split?
			if (m >= testStart && m < testEnd) {
				testDocs[mtest] = docs[splitperm[m]];
				split2corpusDocIds[1][mtest] = splitperm[m];
				corpus2splitDocIds[splitperm[m]] = -mtest - 1;
				numTestWords += testDocs[mtest].getNumWords();
				mtest++;
			} else {
				trainDocs[mtrain] = docs[splitperm[m]];
				split2corpusDocIds[0][mtrain] = splitperm[m];
				corpus2splitDocIds[splitperm[m]] = mtrain;
				mtrain++;
			}
		}
		trainCorpus = new NumCorpus(trainDocs, numTerms, numWords
				- numTestWords);
		testCorpus = new NumCorpus(testDocs, numTerms, numTestWords);

		if (resolver != null) {
			trainCorpus.resolver = new CorpusResolver(resolver.data);
			trainCorpus.resolver.splitDocRelatedKeys(split2corpusDocIds[0]);
			testCorpus.resolver = new CorpusResolver(resolver.data);
			testCorpus.resolver.splitDocRelatedKeys(split2corpusDocIds[1]);
		}
	}

	/**
	 * return the training corpus split
	 */
	public ICorpus getTrainCorpus() {
		return trainCorpus;
	}

	/**
	 * return the test corpus split
	 */
	public ICorpus getTestCorpus() {
		return testCorpus;
	}

	/**
	 * get the original ids of documents according to the corpus file read in.
	 * If never split, null.
	 * 
	 * @return [training documents, test documents]
	 */
	public int[][] getSplit2corpusDocIds() {
		return split2corpusDocIds;
	}

	/**
	 * get the original ids of documents according to the corpus file read in.
	 * If never split, null.
	 * 
	 * @return [training documents, test documents]
	 */
	public int[] getCorpus2splitDocIds() {
		return corpus2splitDocIds;
	}

	/**
	 * write the corpus to to a file.
	 * 
	 * @param pathbase
	 * @param resolve also write resolver
	 * @throws IOException
	 */
	public void write(String pathbase, boolean resolve) throws IOException {
		BufferedWriter bwcorp = new BufferedWriter(new FileWriter(pathbase
				+ ".corpus"));
		if (docs != null) {
			for (int m = 0; m < docs.length; m++) {
				// if (m % 100 == 0) {
				// System.out.println(m);
				// }
				Document doc = docs[m];
				if (doc.getParBounds() == null) {
					bwcorp.append(Integer.toString(doc.numTerms));
					for (int n = 0; n < doc.numTerms; n++) {
						bwcorp.append(" " + doc.terms[n] + ":" + doc.counts[n]);
					}
					bwcorp.append('\n');
				} else {
					// paragraph mode
					int prevbound = 0;
					for (int s = 0; s < doc.parBounds.length; s++) {
						if (s > 0) {
							bwcorp.append(" ");
						}
						bwcorp.append(Integer.toString(doc.numTerms));
						for (int n = prevbound; n < doc.parBounds[s]; n++) {
							bwcorp.append(" " + doc.terms[n] + ":"
									+ doc.counts[n]);
						}
						prevbound = doc.parBounds[s];
					}
					bwcorp.append('\n');
				}
			}
			bwcorp.close();
		}
		if (resolve) {
			getResolver().write(pathbase);
		}
	}

	/**
	 * get a resolver that acts on this corpus. For this, dataFilebase needs to
	 * be known.
	 * 
	 * @return
	 */
	public CorpusResolver getResolver() {
		if (resolver == null && dataFilebase != null) {
			resolver = new CorpusResolver(dataFilebase);
		}
		return resolver;
	}

	public String getDataFilebase() {
		return dataFilebase;
	}

	public void setDataFilebase(String dataFilebase) {
		this.dataFilebase = dataFilebase;
	}

	/**
	 * check the consistency of the corpus, basically checking for array sizes
	 * in conjunction with the index values contained.
	 * 
	 * @param resolve whether to include the resolver class
	 * @return error report or "" if ok.
	 */
	public String check(boolean resolve, boolean verbose) {
		StringBuffer sb = new StringBuffer();
		int numDocsTf0 = 0;
		int numDocsNull = 0;
		int numTermsDf0 = 0;
		int numDocsStatus = 0;

		if (docs != null) {
			// check terms
			int W = 0;
			for (int m = 0; m < numDocs; m++) {
				if (docs[m] == null) {
					if (verbose)
						sb.append(String.format("docs[%d] = null\n", m));
					numDocsNull++;
					continue;
				} else {
					W += docs[m].numWords;
				}
				String docstatus = docs[m].check();
				if (docstatus != null) {
					if (verbose)
						sb.append(String.format("docs[%d] = null:\n%s", m,
								docstatus));
					numDocsStatus++;
				}
				if (docs[m].numWords == 0) {
					if (verbose)
						sb.append(String.format("docs[%d] = 0 length:\n%s", m));
					numDocsTf0++;
				}
			}
			int[] df = calcDocFreqs();
			int V = df.length;
			if (numTerms != V) {
				// sums of terms equal to what is extracted from docs?
				sb.append(String.format("numTerms = %d != V %d\n", numTerms, V));
			} else {
				// do all terms appear in the corpus
				for (int term = 0; term < numTerms; term++) {
					if (df[term] == 0) {
						if (verbose)
							sb.append(String.format("term = %d df = 0\n", term));
						numTermsDf0++;
					}
				}
			}
			// check documents
			if (numDocs != docs.length) {
				sb.append(String.format("numDocs = %d != docs.length = %d\n",
						numDocs, docs.length));
			}
		} else {
			sb.append(String.format("docs = null, numDocs = %d\n", numDocs));
		}
		// TODO: fix
		// if (numDocsNull > 0)
		// sb.append("null documents" + Vectors.print(numDocsNull) + "\n");
		// if (numDocsTf0 > 0)
		// sb.append("empty document texts" + Vectors.print(numDocsTf0) + "\n");
		// if (numDocsStatus > 0)
		// sb.append("inconsistent documents" + Vectors.print(numDocsStatus)
		// + "\n");
		// if (numTermsDf0 > 0)
		// sb.append("empty terms df=0" + Vectors.print(numTermsDf0) + "\n");

		// check resolver
		if (resolve) {
			sb.append(getResolver().check(this, verbose));
		}
		return sb.toString();
	}
}
