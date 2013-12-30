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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.knowceans.util.ArrayUtils;
import org.knowceans.util.CokusRandom;
import org.knowceans.util.Vectors;

/**
 * Represents a corpus of documents, using numerical data only.
 * <p>
 * 
 * @author heinrich
 */
public class LabelNumCorpus extends NumCorpus implements ILabelCorpus {

	/**
	 * test corpus reading and splitting
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// LabelNumCorpus nc = new LabelNumCorpus("corpus-example/berry95");
		LabelNumCorpus nc = new LabelNumCorpus("corpus-example/nips");

		boolean dofilter = true;
		boolean doresolve = true;

		nc.getDocLabels(LAUTHORS);
		nc.getDocLabels(LREFERENCES);
		nc.getResolver();
		Random rand = new CokusRandom();

		if (doresolve) {
			nc.getResolver();
		}

		if (dofilter) {
			filterTest(nc, rand);
			nc.reduceUnlinkedDocs();
		}

		System.out.println(nc);

		nc.split(2, 0, rand);

		System.out.println("train");
		LabelNumCorpus ncc = (LabelNumCorpus) nc.getTrainCorpus();
		System.out.println(ncc);
		CorpusResolver ncr = ncc.getResolver();
		int[][] x = ncc.getDocWords(rand);
		for (int m = 0; m < x.length; m++) {
			String ref = ncr.resolveDocRef(m);
			System.out.println(String.format("%s: %s",
					ref != null ? ref : ncr.resolveDocTitle(m),
					Vectors.print(ncr.resolveWords(x[m]))));
		}
		// System.out.println(Vectors.print(x));
		System.out.println("labels");
		int[][] a = ncc.getDocLabels(LAUTHORS);
		System.out.println(Vectors.print(a));
		System.out.println("references");
		int[][] c = ncc.getDocLabels(LREFERENCES);
		System.out.println(Vectors.print(c));

		System.out.println("test");
		ncc = (LabelNumCorpus) nc.getTestCorpus();
		System.out.println(ncc);
		ncr = ncc.getResolver();
		x = ncc.getDocWords(rand);
		for (int m = 0; m < x.length; m++) {
			String ref = ncr.resolveDocRef(m);
			System.out.println(String.format("%s: %s",
					ref != null ? ref : ncr.resolveDocTitle(m),
					Vectors.print(ncr.resolveWords(x[m]))));
		}
		// System.out.println(Vectors.print(x));
		System.out.println("labels");
		a = ncc.getDocLabels(LAUTHORS);
		System.out.println(Vectors.print(a));
		System.out.println("references");
		int[][] cq = ncc.getDocLabels(LREFERENCES);
		System.out.println(Vectors.print(c));

		// create citations for both corpora jointly
		int[][][] cc = LabelNumCorpus.getSparseTransposeDual(c, cq);
		System.out.println("citations train");
		System.out.println(Vectors.print(cc[0]));
		System.out.println("citations test");
		System.out.println(Vectors.print(cc[1]));

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
				if (self.docs[m].numWords <= mint + 100) {
					// if (self.docs[m].numWords <= mint + 1) {
					return false;
				}
				return true;
			}
		}, rand);
		System.out.println("new numdocs = " + corpus.numDocs);

		System.out.println("orig numterms = " + corpus.numTerms);
		corpus.filterTermsDf(20, 100);
		// corpus.filterTermsDf(4, 10);
		System.out.println("new numterms = " + corpus.numTerms);
	}

	/**
	 * the extensions for the label type constants in ILabelCorpus.L*
	 */
	public static final String[] labelExtensions = { ".authors", ".labels",
			".tags", ".vols", ".years", ".cite", ".ment" };

	public static final String[] labelNames = { "authors", "labels", "tags",
			"volumes", "years", "citations", "mentionings" };

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

	// cardinality constraints for documents (how many per document)
	public static final int[] cardinalityOne = { LVOLS, LYEARS };
	public static final int[] cardinalityGeOne = { LAUTHORS };
	public static final int[] cardinalityGeZero = { LREFERENCES, LMENTIONS,
			LTAGS, LCATEGORIES };

	// may there be labels that don't appear (e.g., documents without inlinks,
	// authors without mentions, categories without instance)
	public static final int[] allowEmptyLabels = { LREFERENCES, LMENTIONS,
			LCATEGORIES };

	// these are relational metadata without key information that need to be
	// handled directly after filtering
	public static final int[] relationalLabels = { LREFERENCES, LMENTIONS };

	// labels that contain doc Ids, which therefore need to be re-mapped after
	// document filtering
	public static final int[] docIdLabels = { LREFERENCES };

	/**
	 * there are only documents with links
	 */
	public boolean pureRelational = false;

	/**
	 * array of labels. Elements are filled as soon as readlabels is called.
	 */
	protected int[][][] labels;
	/**
	 * total count of labels
	 */
	protected int[] labelsW;
	/**
	 * total range of labels
	 */
	protected int[] labelsV;

	/**
	 * number of documents of dual corpus after split
	 */
	protected int numDocsDual;

	/**
	 * when splitting the corpus, whether to cut references between test and
	 * training corpus, thus reducing both to independent corpora. If they are
	 * not cut, the index is set -newIndexInDualCorpus - 1.
	 */
	protected boolean cutRefsInSplit = false;

	/**
     * 
     */
	public LabelNumCorpus() {
		super();
		init();
	}

	/**
	 * @param dataFilebase (filename without extension)
	 */
	public LabelNumCorpus(String dataFilebase) {
		super(dataFilebase);
		this.dataFilebase = dataFilebase;
		init();
	}

	/**
	 * @param dataFilebase (filename without extension)
	 * @param parmode if true read paragraph corpus
	 */
	public LabelNumCorpus(String dataFilebase, boolean parmode) {
		super(dataFilebase + (parmode ? ".par" : ""));
		this.dataFilebase = dataFilebase;
		init();
	}

	/**
	 * @param dataFilebase (filename without extension)
	 * @param readlimit number of docs to reduce corpus when reading (-1 =
	 *        unlimited)
	 * @param parmode if true read paragraph corpus
	 */
	public LabelNumCorpus(String dataFilebase, int readlimit, boolean parmode) {
		super(dataFilebase + (parmode ? ".par" : ""), readlimit);
		this.dataFilebase = dataFilebase;
		init();
	}

	/**
	 * create label corpus from standard one, using references to all fields of
	 * the NumCorpus argument and initialising new label data
	 * 
	 * @param corp
	 */
	public LabelNumCorpus(NumCorpus corp) {
		this.docs = corp.docs;
		this.numDocs = corp.numDocs;
		this.numTerms = corp.numTerms;
		this.numWords = corp.numWords;
		this.resolver = corp.resolver;
		this.dataFilebase = corp.dataFilebase;
		this.parbounds = corp.parbounds;
		this.debug = corp.debug;
		this.split2corpusDocIds = corp.split2corpusDocIds;
		this.corpus2splitDocIds = corp.corpus2splitDocIds;
		this.readlimit = corp.readlimit;
		this.splitperm = corp.splitperm;
		this.splitstarts = corp.splitstarts;
		this.testCorpus = corp.testCorpus;
		this.trainCorpus = corp.trainCorpus;
		this.wordparbounds = corp.wordparbounds;
		init();
	}

	protected void init() {
		labels = new int[labelExtensions.length][][];
		labelsW = new int[labelExtensions.length];
		labelsV = new int[labelExtensions.length];
	}

	/**
	 * checks whether the corpus has labels
	 * 
	 * @param kind according to label constants ILabelCorpus.L*
	 * @return 0 for no label values, 1 for yes, 2 for loaded, -1 for illegal
	 */
	public int hasLabels(int kind) {
		if (kind >= labels.length || kind < -2) {
			return -1;
		}
		if (kind < 0) {
			// we have docs and terms loaded
			if (docs != null)
				return 2;
		} else {
			// any metadata
			if (labels[kind] != null) {
				return 2;
			}
			File f = new File(this.dataFilebase + labelExtensions[kind]);
			if (f.exists()) {
				return 1;
			}
		}
		// not loaded
		return 0;
	}

	/**
	 * loads all labels (metadata)
	 */
	public void loadAllLabels() {
		for (int i = 0; i < LabelNumCorpus.labelExtensions.length; i++) {
			if (hasLabels(i) == 1) {
				// System.out.println("loading " +
				// LabelNumCorpus.labelNames[i]);
				getDocLabels(i);
			}
		}
	}

	/**
	 * loads and returns the document labels of given kind
	 */
	// @Override
	public int[][] getDocLabels(int kind) {
		if (hasLabels(kind) <= 0) {
			return null;
		}
		if (labels[kind] == null)
			readLabels(kind);
		return labels[kind];
	}

	/**
	 * get the labels for one document
	 * 
	 * @param m
	 * @param kind
	 * @return
	 */
	public int[] getDocLabels(int kind, int m) {
		if (hasLabels(kind) <= 0) {
			return null;
		}
		if (labels[kind] == null)
			readLabels(kind);
		return labels[kind][m];
	}

	/**
	 * return the minimum number of labels in any document
	 * 
	 * @param kind
	 * @return value or -1 if labels of this type = null
	 */
	public int getLabelsMinN(int kind) {
		if (labels[kind] == null) {
			return -1;
		}
		int min = 0;
		if (labels[kind].length > 0) {
			min = labels[kind][0].length;
		}
		for (int m = 1; m < numDocs; m++) {
			min = min < labels[kind][m].length ? min : labels[kind][m].length;
		}
		return min;
	}

	/**
	 * return the maximum number of labels in any document
	 * 
	 * @param kind
	 * @return value or -1 if labels of this type = null
	 */
	public int getLabelsMaxN(int kind) {
		if (labels[kind] == null) {
			return -1;
		}
		int max = 0;
		for (int m = 0; m < numDocs; m++) {
			max = max < labels[kind][m].length ? labels[kind][m].length : max;
		}
		return max;
	}

	// @Override
	public int getLabelsW(int kind) {
		return labelsW[kind];
	}

	// @Override
	public int getLabelsV(int kind) {
		return labelsV[kind];
	}

	/**
	 * read a label file with one line per document and associated labels
	 * 
	 * @param kind
	 * @return
	 */
	private void readLabels(int kind) {
		ArrayList<int[]> data = new ArrayList<int[]>();
		int W = 0;
		int V = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(dataFilebase
					+ labelExtensions[kind]));
			String line;
			int j = 0;
			while ((line = br.readLine()) != null) {
				// remove additional info
				int c = line.indexOf(" : ");
				if (c > -1) {
					line = line.substring(0, c);
				}
				line = line.trim();
				if (line.length() == 0) {
					data.add(new int[0]);
					continue;
				}
				String[] parts = line.split(" ");
				int[] a = new int[parts.length];
				for (int i = 0; i < parts.length; i++) {
					a[i] = Integer.parseInt(parts[i].trim());
					if (a[i] >= V) {
						V = a[i] + 1;
					}
				}
				W += a.length;
				data.add(a);
				j++;
			}
			br.close();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		labels[kind] = data.toArray(new int[0][0]);
		labelsW[kind] = W;
		labelsV[kind] = V;
		// Print.fln("labels loaded: %s: V = %d, W = %d", labelNames[kind], V,
		// W);
	}

	// document filtering

	/**
	 * filter out documents with empty labels of the type in the set.
	 * 
	 * @param set of L constants
	 * @return old2new indices
	 */
	public int[] reduceEmptyLabels(final Set<Integer> labelTypes) {
		DocPredicate filter = new DocPredicate() {
			@Override
			public boolean doesApply(NumCorpus self, int m) {
				for (int ltype : labelTypes) {
					if (labels[ltype][m].length == 0) {
						return false;
					}
				}
				return true;
			}
		};
		return filterDocs(filter, null);
	}

	/**
	 * filter all documents that don't have links, considering any inlinks or
	 * outlinks.
	 * 
	 * @return
	 */
	// , TODO: boolean usementions
	public int[] reduceUnlinkedDocs() {
		final int[][] references = labels[LREFERENCES];

		// first create a list of incoming links by transposing the relation
		@SuppressWarnings("unchecked")
		final int[][] inlinks = createCitations(references);

		// TODO: add mentions
		// int[][] mentions = labels[LMENTIONS];
		// int[][] authors = labels[LAUTHORS];

		DocPredicate filter = new DocPredicate() {
			@Override
			public boolean doesApply(NumCorpus self, int m) {
				return inlinks[m].length > 0 || references[m].length > 0;
			}
		};
		pureRelational = true;
		return filterDocs(filter, null);
	}

	/**
	 * creates the citations of this corpus as the incoming links. If
	 * cutRefsInSplit is false, the array returned contains the corpus and in
	 * elements numDoc ... numDoc + dualCorpus.numDoc - 1 the incoming links of
	 * the dual corpus (test for train and vice versa). This is the main
	 * difference to using getSparseTransform().
	 * <p>
	 * TODO: difference with getSparseTranspose/getSparseTransposeDual
	 * 
	 * @param references
	 * @return
	 */
	public int[][] createCitations(final int[][] references) {
		@SuppressWarnings("unchecked")
		// original + dual corpus (if exists)
		final List<Integer>[] inlinks = new List[numDocs + numDocsDual];

		for (int m = 0; m < inlinks.length; m++) {
			inlinks[m] = new ArrayList<Integer>();
		}

		for (int m = 0; m < references.length; m++) {
			for (int i = 0; i < references[m].length; i++) {
				int r = references[m][i];
				if (r >= 0) {
					inlinks[r].add(m);
				} else {
					// link to dual corpus
					// r=-1 => m=0 => index numDocs
					// r=-2 => m=1 => index numDocs + 1
					inlinks[numDocs - r - 1].add(m);
				}
			}
		}
		int[][] cites = new int[numDocs + numDocsDual][];
		for (int m = 0; m < inlinks.length; m++) {
			cites[m] = (int[]) ArrayUtils.asPrimitiveArray(inlinks[m],
					int.class);
		}
		return cites;
	}

	/**
	 * filter documents. Also updates the resolver. Vocabulary must be rebuilt
	 * separately because frequencies change: use filterTermsDf(). Because
	 * citations are directly affected, this label type is updated here, as
	 * well. Removes the pureRelational flag because then-outside references are
	 * removed.
	 * 
	 * @param filter predicate to keep documents in list
	 * @param rand random number generator to be used generate a random
	 *        permutation, null if no random permutation
	 * 
	 * @return old2new indices
	 */
	public int[] filterDocs(DocPredicate filter, Random rand) {
		int[] old2new = super.filterDocs(filter, rand);
		// by now, we have filtered documents (even by label predicates) and
		// need to sync the labels to them
		int[][][] newLabels = new int[labelExtensions.length][][];
		int[] newLabelsW = new int[labelExtensions.length];
		for (int type = 0; type < labelExtensions.length; type++) {
			// System.out.println("label type " + labelNames[type] + "...");
			if (labels[type] != null) {
				// numDocs is the new size
				newLabels[type] = new int[numDocs][];
				for (int m = 0; m < labels[type].length; m++) {
					if (old2new[m] >= 0) {
						// System.out.println(String.format(
						// "label m = %d, old2new[] = %d: %s", m,
						// old2new[m], Vectors.print(labels[type][m])));
						newLabels[type][old2new[m]] = labels[type][m];
						newLabelsW[type] += labels[type][m].length;
					}
				}
			}
		}
		labels = newLabels;
		labelsW = newLabelsW;
		if (labels[LREFERENCES] != null) {
			// filter citations here, otherwise old2new is awkward to handle
			labelsW[LREFERENCES] = rewriteLabels(LREFERENCES, old2new);
			// determine number of unique cited documents
			// labelsV[LREFERENCES] = getVocabSize(labels[LREFERENCES]);
			// gaps allowed...
			labelsV[LREFERENCES] = numDocs;
		}
		pureRelational = false;
		return old2new;
	}

	/**
	 * filtering without changing the document sequence
	 * 
	 * @param filter
	 * @return
	 */
	public int[] filterDocs(DocPredicate filter) {
		return filterDocs(filter, null);
	}

	/**
	 * count the number of distinct values in x
	 * 
	 * @param x
	 * @return
	 */
	public static int getVocabSize(int[][] x) {
		Set<Integer> refV = new HashSet<Integer>();
		for (int m = 0; m < x.length; m++) {
			for (int i = 0; i < x[m].length; i++) {
				refV.add(x[m][i]);
			}
		}
		int y = refV.size();
		return y;
	}

	/**
	 * "semantic overload" of getSparseTranspose: References are given as they
	 * are output from the labels[LREFERENCES] of train and test corpora before
	 * a split, or after a split with negative indices ignored, i.e., breaking
	 * references between dual corpora. To preserve dual references, use
	 * getCitesFromRefsDual().
	 * 
	 * @param refs from labels[LREFERENCES]
	 * @return citations
	 */
	public static int[][] getCitesFromRefs(int[][] refs) {
		return getSparseTranspose(refs);
	}

	/**
	 * transpose the sparse matrix with unit elements at positions (m, x[m][n]),
	 * as used to represent an adjacency matrix. Correspondingly the sparse
	 * transpose creates a set of inlinks from outlinks and vice versa.
	 * <p>
	 * TODO: this now assumes a quadratic matrix --> may use for non-quadratic
	 * such as term-document matrices
	 * <p>
	 * this method ignores any negative matrix entries
	 * 
	 * @param x
	 * @return
	 */
	public static int[][] getSparseTranspose(int[][] x) {

		@SuppressWarnings("unchecked")
		List<Integer>[] inrefs = new List[x.length];
		for (int m = 0; m < x.length; m++) {
			inrefs[m] = new ArrayList<Integer>();
		}
		for (int m = 0; m < x.length; m++) {
			for (int i = 0; i < x[m].length; i++) {
				inrefs[x[m][i]].add(m);
			}
		}
		int[][] xtransp = new int[x.length][];
		for (int m = 0; m < x.length; m++) {
			xtransp[m] = (int[]) ArrayUtils.asPrimitiveArray(inrefs[m],
					int.class);
		}
		return xtransp;
	}

	/**
	 * "semantic overload" of getSparseTransposeDual: The training references
	 * and test references are given as arguments as they are output from the
	 * labels[LREFERENCES] of train and test corpora after a split with negative
	 * indices indicating references into other corpus.
	 * 
	 * @param trainRefs from trainCorpus.labels[LREFERENCES]
	 * @param testRefs from testCorpus.labels[LREFERENCES]
	 * @return {citations in trainCorpus, citations in testCorpus}
	 */
	public static int[][][] getCitesFromRefsTrainTest(int[][] trainRefs,
			int[][] testRefs) {
		return getSparseTransposeDual(trainRefs, testRefs);
	}

	/**
	 * transpose the reference labels to citations, optionally using the dual
	 * corpus to transpose negative labels (references in dual split corpora)
	 * 
	 * @param x with negative references into dual y
	 * @param y with negative references into dual x
	 * @return {transpose of x with added values from y, transpose of y
	 *         analogous}
	 */
	public static int[][][] getSparseTransposeDual(int[][] x, int[][] y) {

		@SuppressWarnings("unchecked")
		List<Integer>[][] inrefs = new List[][] { new List[x.length],
				new List[y.length] };
		for (int m = 0; m < x.length; m++) {
			inrefs[0][m] = new ArrayList<Integer>();
		}
		for (int m = 0; m < y.length; m++) {
			inrefs[1][m] = new ArrayList<Integer>();
		}
		for (int m = 0; m < x.length; m++) {
			for (int i = 0; i < x[m].length; i++) {
				if (x[m][i] >= 0) {
					inrefs[0][x[m][i]].add(m);
				} else {
					// index in dual array
					inrefs[1][-x[m][i] - 1].add(-m - 1);
				}
			}
		}
		for (int m = 0; m < y.length; m++) {
			for (int i = 0; i < y[m].length; i++) {
				if (y[m][i] >= 0) {
					inrefs[1][y[m][i]].add(m);
				} else {
					// index in main array (which again is dual to the dual)
					inrefs[0][-y[m][i] - 1].add(-m - 1);
				}
			}
		}
		int[][][] xtransp = new int[][][] { new int[x.length][],
				new int[y.length][] };
		for (int m = 0; m < x.length; m++) {
			xtransp[0][m] = (int[]) ArrayUtils.asPrimitiveArray(inrefs[0][m],
					int.class);
		}
		for (int m = 0; m < y.length; m++) {
			xtransp[1][m] = (int[]) ArrayUtils.asPrimitiveArray(inrefs[1][m],
					int.class);
		}
		return xtransp;
	}

	/**
	 * calculates the document frequencies of the labels
	 * 
	 * @param label type
	 * @return
	 */
	public int[] calcLabelDocFreqs(int type) {
		// we construct term frequencies manually even if there may
		// be another source
		int[] df = new int[labelsV[type]];
		for (int m = 0; m < numDocs; m++) {
			for (int t = 0; t < labels[type][m].length; t++) {
				df[labels[type][m][t]]++;
			}
		}
		return df;
	}

	/**
	 * filter labels (of all types) that do not exist in the corpus
	 */
	public void filterLabels() {
		for (int type = 0; type < labelExtensions.length; type++) {
			if (Arrays.binarySearch(relationalLabels, type) >= 0) {
				// skip relations
				continue;
			}
			// System.out.println("reduce labels type " + labelNames[type]);
			filterLabelsDf(type, 1);
		}
	}

	/**
	 * filter labels by frequency. The corpus resolver obtained by getResolver()
	 * is updated to the new label mapping. Note that this does not apply to the
	 * relational label type references and mentions. Instead, filterDocs
	 * directly updates references and filterLabelsDf(type=LAUTHORS, ... )
	 * updates mentions, so there's no need to manage old2new indices
	 * separately.
	 * 
	 * @param minDf all more scarce terms are excluded
	 * @param maxDf all more frequent terms are excluded
	 * @return array with new indices in old index elements or null if nothing
	 *         was changed.
	 */
	public int[] filterLabelsDf(int type, int minDf) {
		if (labels[type] == null) {
			return null;
		}
		// skip relational labels
		if (Arrays.binarySearch(relationalLabels, type) >= 0) {
			return null;
		}
		int[] df = calcLabelDocFreqs(type);
		// rewrite indices
		int[] old2new = new int[labelsV[type]];
		int newIndex = 0;
		for (int t = 0; t < labelsV[type]; t++) {
			if (df[t] < minDf) {
				old2new[t] = -1;
			} else {
				old2new[t] = newIndex;
				newIndex++;
			}
		}
		// rewrite corpus
		labelsW[type] = rewriteLabels(type, old2new);
		labelsV[type] = newIndex;

		if (type == LAUTHORS && labels[LMENTIONS] != null) {
			// filter mentioned authors
			// if mentionings, we should rewrite with authors' old2new
			labelsW[LMENTIONS] = rewriteLabels(LMENTIONS, old2new);
			// labelsV[LMENTIONS] = getVocabSize(labels[LMENTIONS]);
			// gaps allowed...
			labelsV[LMENTIONS] = labelsV[LAUTHORS];
		}

		// map to novel label indices (need to translate type)
		getResolver()
				.filterLabels(CorpusResolver.labelId2keyExt[type], old2new);
		return old2new;
	}

	/**
	 * rewrite the labels of given type throughout the corpus, using mapping
	 * old2new. If labels are negative, they are simply copied (because negative
	 * labels are reserved to represent document ids in the dual corpus, which
	 * are not filtered at this point.There is currently no method to filter
	 * these labels in the dual corpus, so filtering should be done before
	 * splitting.)
	 * 
	 * @param type
	 * @param old2new
	 * @param return number of label instances (words) in corpus
	 */
	protected int rewriteLabels(int type, int[] old2new) {
		int W = 0;
		for (int m = 0; m < numDocs; m++) {
			List<Integer> tt = new ArrayList<Integer>();
			int[] ll = labels[type][m];
			// System.out.println("*** doc " + m);
			for (int i = 0; i < ll.length; i++) {
				int label = ll[i];
				if (label > 0) {
					if (old2new[label] >= 0) {
						tt.add(old2new[label]);
						W++;
						// System.out.println("add " + label + "->" +
						// old2new[label]);
					} else {
						// System.out.println("dump " + label + "->" +
						// old2new[label]);
					}
				} else {
					// doc id in dual corpus --> keep original (negative id)
					tt.add(label);
					W++;
				}
			}
			labels[type][m] = (int[]) ArrayUtils
					.asPrimitiveArray(tt, int.class);
		}
		return W;
	}

	// end document filtering

	/**
	 * splits the corpus. See superclass method. Note that the references and
	 * other labels with document ids are being rewritten in the label set. By
	 * default, the references to the "dual" corpus, i.e., training for test and
	 * vice versa, are written as -m-1, that is, checking for negative indices
	 * will allow to identify links across corpus boundaries. If these links are
	 * to be filtered, set the cutRefsInSplit property to true.
	 * <p>
	 * IMPORTANT: Use split cautiously, as references to child corpora are
	 * shared. Filtering may produce inpredictable results if the label and word
	 * data are not actually copied, which must be done explicitly. The typical
	 * use case is, however, to filter subcorpora before splitting the corpus.
	 */
	@Override
	public void split(int order, int split, Random rand) {

		// get plain num corpora and split data
		super.split(order, split, rand);

		// convert to subclass
		trainCorpus = new LabelNumCorpus((NumCorpus) getTrainCorpus());
		testCorpus = new LabelNumCorpus((NumCorpus) getTestCorpus());
		LabelNumCorpus train = (LabelNumCorpus) trainCorpus;
		LabelNumCorpus test = (LabelNumCorpus) testCorpus;

		int Mtest = splitstarts[split + 1] - splitstarts[split];
		train.labels = new int[labelExtensions.length][][];
		train.labelsW = new int[labelExtensions.length];
		train.labelsV = labelsV;
		train.pureRelational = false;
		train.numDocsDual = testCorpus.numDocs;
		train.dataFilebase = dataFilebase;
		test.labels = new int[labelExtensions.length][][];
		test.labelsW = new int[labelExtensions.length];
		test.labelsV = labelsV;
		test.pureRelational = false;
		test.numDocsDual = trainCorpus.numDocs;
		test.dataFilebase = dataFilebase;

		for (int type = 0; type < labelExtensions.length; type++) {
			if (labels[type] == null) {
				continue;
			}
			train.labels[type] = new int[numDocs - Mtest][];
			test.labels[type] = new int[Mtest][];
			for (int m = 0; m < numDocs; m++) {
				int msplit = corpus2splitDocIds[m];
				if (msplit >= 0) {
					train.labels[type][msplit] = labels[type][m];
				} else {
					test.labels[type][-msplit - 1] = labels[type][m];
				}
			}

			// replace document ids in split corpora
			if (Arrays.binarySearch(docIdLabels, type) >= 0) {
				int[][] doclabels = train.labels[type];
				for (int m = 0; m < doclabels.length; m++) {
					List<Integer> doc = new ArrayList<Integer>();
					for (int n = 0; n < doclabels[m].length; n++) {
						int msplit = corpus2splitDocIds[doclabels[m][n]];
						// if in train corpus or no cut between train and test
						if (msplit >= 0 || !cutRefsInSplit) {
							doc.add(msplit);
						}
					}
					doclabels[m] = (int[]) ArrayUtils.asPrimitiveArray(doc,
							int.class);
				}
				doclabels = test.labels[type];
				for (int m = 0; m < doclabels.length; m++) {
					List<Integer> doc = new ArrayList<Integer>();
					for (int n = 0; n < doclabels[m].length; n++) {
						// test index complement
						int msplit = -corpus2splitDocIds[doclabels[m][n]] - 1;
						// if in test corpus or no cut between train and test
						if (msplit >= 0 || !cutRefsInSplit) {
							doc.add(msplit);
						}
					}
					doclabels[m] = (int[]) ArrayUtils.asPrimitiveArray(doc,
							int.class);
				}
			}
		}
	}

	@Override
	public void write(String pathbase, boolean resolve) throws IOException {
		super.write(pathbase, resolve);
		// write the stuff that labels add to the plain NumCorpus
		for (int type = 0; type < labelExtensions.length; type++) {
			if (labels[type] == null) {
				continue;
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(pathbase
					+ labelExtensions[type]));
			for (int m = 0; m < numDocs; m++) {
				// NOTE: null and zero-length are treated same, but null may be
				// an error
				if (labels[type][m] != null) {
					for (int n = 0; n < labels[type][m].length; n++) {
						if (n > 0) {
							bw.write(' ');
						}
						bw.write(Integer.toString(labels[type][m][n]));
					}
				}
				bw.append('\n');
			}
			bw.close();
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		// corpus statistics
		sb.append(super.toString());
		sb.append(String
				.format("labels (0 = not available, 1 = available, 2 = loaded, -1 = no resolver):\n"));
		for (int i = 0; i < LabelNumCorpus.labelExtensions.length; i++) {
			sb.append(String.format(" %s = %d, .keys = %d\n",
					LabelNumCorpus.labelExtensions[i], hasLabels(i),
					(resolver != null) ? resolver.hasLabelKeys(i + 2) : -1));
			if (hasLabels(i) >= 2) {
				sb.append(String.format(
						"    V = %d, W = %d, N[m] = [%d, %d]\n", getLabelsV(i),
						getLabelsW(i), getLabelsMinN(i), getLabelsMaxN(i)));
			}
		}
		return sb.toString();
	}

	/**
	 * check the consistency of the corpus, basically checking for array sizes
	 * in conjunction with the index values contained.
	 * 
	 * @param resolve whether to include the resolver class
	 * @param verbose whether to list all instances or count them
	 * @return error report or empty string "" if ok.
	 */
	public String check(boolean resolve, boolean verbose) {
		StringBuffer sb = new StringBuffer();
		// TODO: we have the resolver (including labels) checked before the
		// numerical labels... suboptimal but ok for a consistency check but.
		sb.append(super.check(resolve, verbose));
		int[] numIdsGeV = new int[labelExtensions.length];
		int[] numIdsLblDf0 = new int[labelExtensions.length];
		int[] numDocsLblTf0 = new int[labelExtensions.length];
		int[] numDocsLblNull = new int[labelExtensions.length];
		int[] numDocsRef0 = new int[labelExtensions.length];

		for (int type = 0; type < labelExtensions.length; type++) {
			if (labels[type] != null) {
				if (labels[type].length != numDocs) {
					// each document needs to have a label
					sb.append(String
							.format("label type %s length = %d != document count M = %d\n",
									labelNames[type], labels[type].length,
									numDocs));
				} else {
					// check whether labels array size matches that of the
					// metadata
					int W = 0;
					int V = labelsV[type];
					int[] ll = new int[V];
					boolean needOne = Arrays.binarySearch(cardinalityGeOne,
							type) >= 0;
					boolean exactlyOne = Arrays.binarySearch(cardinalityOne,
							type) >= 0;
					// check W and availability of all labels
					for (int m = 0; m < numDocs; m++) {
						int[] row = labels[type][m];
						if (row == null) {
							if (verbose) {
								sb.append(String.format(
										"label type %s document %d = null\n",
										labelNames[type], m));
							}
							numDocsLblNull[type]++;
							continue;
						}
						// not for document ids as labels
						if (Arrays.binarySearch(docIdLabels, type) < 0) {
							for (int n = 0; n < row.length; n++) {
								if (row[n] < labelsV[type]) {
									ll[row[n]]++;
								} else {
									if (verbose) {
										sb.append(String
												.format("label type %s [%d][%d]  %d > V = %d\n",
														labelNames[type], m, n,
														row[n], V));
									}
									numIdsGeV[type]++;
								}
							}
						}
						if (((exactlyOne || needOne) && labels[type][m].length == 0)
								|| (exactlyOne && labels[type][m].length > 1)) {
							if (verbose) {
								sb.append(String
										.format("label type %s: cardinality constraint broken: m = %d: %d\n",
												labelNames[type], m,
												labels[type][m].length));
							}
							numDocsLblTf0[type]++;
						}
						W += labels[type][m].length;
					}
					boolean cannotBeEmpty = Arrays.binarySearch(
							allowEmptyLabels, type) < 0;
					if (cannotBeEmpty) {
						for (int t = 0; t < ll.length; t++) {
							if (ll[t] == 0) {
								if (verbose) {
									sb.append(String
											.format("label type %s : %d frequency = 0\n",
													labelNames[type], t));
								}
								numIdsLblDf0[type]++;
							}
						}
					}
					if (pureRelational && type == LREFERENCES) {
						int[][] references = labels[type];
						int[][] citations = getSparseTranspose(references);
						for (int m = 0; m < numDocs; m++) {
							if (references[m].length == 0
									&& citations[m].length == 0) {
								if (verbose) {
									sb.append(String
											.format("label type %s : doc %d relations = 0 (reference count = %d)\n",
													labelNames[type], m,
													references[m].length));
								}
								numDocsRef0[type]++;
							}
						}
						// TODO: check split references
					}
					// TODO: fix
					// if (Vectors.sum(numIdsGeV) > 0)
					// sb.append("labels with values > V  (per type): "
					// + Vectors.print(numIdsGeV) + "\n");
					// if (Vectors.sum(numIdsLblDf0) > 0)
					// sb.append("labels with empty values df=0 (per type): "
					// + Vectors.print(numIdsLblDf0) + "\n");
					// if (Vectors.sum(numDocsLblTf0) > 0)
					// sb.append("documents with empty labels (constrained cardinality, per type): "
					// + Vectors.print(numDocsLblTf0) + "\n");
					// if (Vectors.sum(numDocsLblNull) > 0)
					// sb.append("number documents with null labels  (per type): "
					// + Vectors.print(numDocsLblNull) + "\n");
					// if (Vectors.sum(numDocsRef0) > 0)
					// sb.append("number of documents with empty references (per type): "
					// + Vectors.print(numDocsRef0) + "\n");
				}
			}
		}
		return sb.toString();
	}

	public boolean isCutRefsInSplit() {
		return cutRefsInSplit;
	}

	public void setCutRefsInSplit(boolean cutRefsInSplit) {
		this.cutRefsInSplit = cutRefsInSplit;
	}
}
