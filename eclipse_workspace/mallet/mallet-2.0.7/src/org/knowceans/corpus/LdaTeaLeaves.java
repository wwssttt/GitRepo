/*
 * Created on Jan 24, 2010
 */
package org.knowceans.corpus;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;

import org.knowceans.util.IndexQuickSort;
import org.knowceans.util.RandomSamplers;
import org.knowceans.util.Vectors;

/**
 * LdaTeaLeaves presents documents and topics for evaluation of the subjective
 * topic coherence measures put forward by Chang et al. Reading Tea Leaves --
 * How Humans Interpret Topic Models (NIPS 2009).
 * 
 * @author gregor
 */
public class LdaTeaLeaves {

	CorpusResolver resolver;

	private double[][] phi;
	private double[][] theta;
	private LabelNumCorpus corpus;
	private RandomSamplers rand;

	/**
	 * topic weight over corpus, k -> p_k
	 */
	private double[] pk;

	/**
	 * topic ranks k -> rank
	 */
	private int[] rk;

	/**
	 * unshuffle cross-validation: indices of theta --> document ids in corpus
	 */
	private int[] theta2docId;

	/**
	 * inversely ranked document frequencies: dfrank --> term id
	 */
	private int[] term2dfRank;

	private int[][] rank2bestTerms;

	private HashSet<Integer> lowTerms;

	/**
	 * initialise with filebase
	 * 
	 * @param filebase
	 * @param theta
	 * @param phi
	 * @param rand
	 */
	public LdaTeaLeaves(String filebase, double[][] theta, double[][] phi,
			Random rand) {
		this(new LabelNumCorpus(filebase), theta, phi, rand);
	}

	/**
	 * initialise with existing corpus
	 * 
	 * @param corpus
	 * @param theta
	 * @param phi
	 * @param rand
	 */
	public LdaTeaLeaves(LabelNumCorpus corpus, double[][] theta,
			double[][] phi, Random rand) {
		this.resolver = corpus.getResolver();
		this.corpus = corpus;
		this.theta = theta;
		this.phi = phi;
		this.rand = new RandomSamplers(rand);
		// precalculate topic absolute weights and ranke
		getTopicWeights();
		getTopicRanks();
		// these are the ids of the original corpus
		theta2docId = corpus.getSplit2corpusDocIds()[0];
		term2dfRank = IndexQuickSort.inverse(IndexQuickSort.revsort(corpus
				.calcDocFreqs()));

		int K = phi.length;
		int limit = 30;
		rank2bestTerms = new int[K][];
		for (int k = 0; k < phi.length; k++) {
			int[] rank = IndexQuickSort.revsort(phi[k]);
			rank2bestTerms[k] = Vectors.sub(rank, 0, limit);
		}

	}

	/**
	 * create a pair of test documents, one with questions and the second with
	 * the solutions.
	 * 
	 * @param pathbase extended by .lda.teq and .lda.tea
	 * @param numdocs
	 * @param numtopics
	 * @throws IOException
	 */
	public void createTestDocs(String pathbase, int numdocs, int numtopics)
			throws IOException {
		BufferedWriter bwq = new BufferedWriter(new FileWriter(pathbase
				+ ".lda.teq"));
		BufferedWriter bwa = new BufferedWriter(new FileWriter(pathbase
				+ ".lda.tea"));

		// tea leaves for random documents
		int[] a = rand.randPerm(theta2docId.length);
		numdocs = Math.min(numdocs, theta.length);
		for (int m = 0; m < numdocs; m++) {
			String[] doctest = printDocument(a[m], 3, 1, 20, 50);
			String head = "*** Document " + (m + 1) + " ***\n";
			bwq.append(head);
			bwq.append(doctest[0]).append("\n");
			bwa.append(head);
			bwa.append(doctest[1]).append("\n");
		}

		// tea leaves for random topics
		a = rand.randPerm(phi.length);
		numtopics = Math.min(numtopics, phi.length);
		for (int k = 0; k < numtopics; k++) {
			String[] topictest = printTopic(a[k], 5, 1);
			String head = "*** Topic " + (k + 1) + " ***\n";
			bwq.append(head);
			bwq.append(topictest[0]).append("\n");
			bwa.append(head);
			bwa.append(topictest[1]).append("\n");
		}
		bwq.close();
		bwa.close();
	}

	// high-level methods

	/**
	 * create string with document topic information
	 * 
	 * @param m doc id in model
	 * @param topics how many of the most likely topics to print
	 * @param intruders how many intruders (call with 0 to simply print topic)
	 * @param terms number of terms per topic
	 * @param terms number of terms for document
	 * @return strings for test and solution files
	 */
	public String[] printDocument(int m, int topics, int intruders,
			int ntopicTerms, int ndocTerms) {
		ntopicTerms = Math.min(phi[0].length, ntopicTerms);
		int K = phi.length;
		int morig = theta2docId[m];
		StringBuffer btest = new StringBuffer();
		StringBuffer bsolv = new StringBuffer();
		btest.append(String.format("%d: %s (theta[%d])\n", morig,
				resolver.resolveDocTitle(morig), m));
		int[] x = corpus.getDocLabels(ILabelCorpus.LAUTHORS, morig);
		btest.append("authors:");
		for (int aa : x) {
			btest.append(" " + resolver.resolveAuthor(aa));
		}
		btest.append("\n");
		bsolv.append(btest);
		// add doc content to test
		btest.append("content:\n");
		int[] tt = corpus.getDoc(morig).getTerms();
		int[] ff = corpus.getDoc(morig).getCounts();
		int[] ranks = IndexQuickSort.reverse(IndexQuickSort.sort(ff));
		for (int t = 0; t < Math.min(ndocTerms, tt.length); t++) {
			if (t % 4 == 0 && t > 0) {
				btest.append("\n");
			}
			btest.append(String.format("\t%25s:%d", corpus.getResolver()
					.resolveTerm(tt[ranks[t]]), ff[ranks[t]]));
		}
		btest.append("\n");

		// sort topics by weight
		int[] a = IndexQuickSort.revsort(theta[m]);
		int[] k2rank = IndexQuickSort.inverse(a);
		int[] shortlist = new int[topics + intruders];

		// wanting too much?
		if (a.length < topics + intruders) {
			return new String[] {
					"error: topics must exceed number of positive and negative examples)",
					"" };
		}
		// best topics
		for (int k = 0; k < topics; k++) {
			shortlist[k] = a[k];
		}
		// worst topics
		for (int k = 0; k < intruders; k++) {
			shortlist[topics + k] = a[a.length - k - 1];
		}
		// scramble list
		int[] seq = rand.randPerm(topics + intruders);

		btest.append("\ntopics in document (choose 1-" + shortlist.length
				+ ", which matches worst):\n");
		bsolv.append("\ntopics in document (* = intruder):\n");
		for (int idxseq = 0; idxseq < seq.length; idxseq++) {
			int idxshortlist = seq[idxseq];
			int k = shortlist[idxshortlist];
			// sort and truncate topic terms
			int[] terms = Vectors.sub(IndexQuickSort.revsort(phi[k]), 0,
					ntopicTerms);
			// write query, test option has 1-based index
			btest.append(String.format("    %3d. %s", idxseq + 1,
					printTopic(k, terms, false, true)));
			// write solution, test option has 1-based index
			bsolv.append(String.format(
					"  %s %2d. k = %3d. p(k|m) = %6.3f/%d, rank %2d: %s",
					idxshortlist >= topics ? "*" : " ", idxseq + 1, k,
					theta[m][k] * K, K, k2rank[k],
					printTopic(k, terms, true, true)));
		}
		return new String[] { btest.toString(), bsolv.toString() };
	}

	/**
	 * print topic for testing
	 * 
	 * @param k
	 * @param nterms per topic
	 * @param nintruders
	 * @return
	 */
	public String[] printTopic(int k, int nterms, int nintruders) {

		nterms = Math.min(phi[0].length, nterms);
		int V = phi[0].length;
		StringBuffer btest = new StringBuffer();
		StringBuffer bsolv = new StringBuffer();

		// sort topics by weight
		int[] rank2t = IndexQuickSort.revsort(phi[k]);
		int[] t2rank = IndexQuickSort.inverse(rank2t);
		int[] shortlist = new int[nterms + nintruders];

		// wanting too much?
		if (rank2t.length < nterms + nintruders) {
			return new String[] {
					"error: topics must exceed number of positive and negative examples)",
					"" };
		}
		// best topics
		for (int t = 0; t < nterms; t++) {
			shortlist[t] = rank2t[t];
		}
		for (int t = 0; t < nintruders; t++) {
			// worst terms from last quarter
			shortlist[nterms + t] = getLowRankCommonTerm(t2rank, k);
		}
		// scramble list
		int[] seq = rand.randPerm(nterms + nintruders);

		btest.append("\nterms in topic (choose 1-" + shortlist.length
				+ ", which matches worst):\n");
		bsolv.append(String.format(
				"topic %3d (pk = %6.3f/V, V = %d, rank = %3d):\n", k,
				getTopicWeights()[k] * phi.length, phi.length,
				getTopicRanks()[k]));

		bsolv.append("\nterms in topic (* = intruder):\n");
		for (int idxseq = 0; idxseq < seq.length; idxseq++) {
			int t = shortlist[seq[idxseq]];
			String term = resolver.resolveTerm(t);
			term = stripTerm(term);
			// write query, test option has 1-based index
			btest.append(String.format("    %3d. %s\n", idxseq + 1, term));
			// write solution, test option has 1-based index
			bsolv.append(String
					.format("  %s %2d. t = %3d. p(t|k) = %6.3f/V, local rank %2d, global rank: %2d: %s\n",
							seq[idxseq] >= nterms ? "*" : " ", idxseq + 1, t,
							phi[k][t] * V, t2rank[t], term2dfRank[t], term));
		}
		return new String[] { btest.toString(), bsolv.toString() };
	}

	/**
	 * sample from other topics (first 30 or so positions) a term that's
	 * sufficiently low ranked in topic k. Side-effect: Fills lowTerms with the
	 * chosen terms to avoid duplicate intruders.
	 * 
	 * @param t2rank
	 * @param k
	 * @return
	 */
	private int getLowRankCommonTerm(int[] t2rank, int k) {
		/*
		 * Samples about nsamp times over all foreign topics according to their
		 * global importance p(k), with binomial distribution of their ranks
		 * Binom(30, 0.1). After the samples, the candidate with lowest rank
		 * (highest number in t2rank), or the first one that is in the lower
		 * half of t2rank, is taken.
		 */
		int nsamp = 50;
		int term = 0;
		int bestterm = 0;
		int lowestrank = 0;
		if (lowTerms == null) {
			lowTerms = new HashSet<Integer>();
		}
		double pbinom = 0.1;
		int nranks = rank2bestTerms[0].length;
		for (int i = 0; i < nsamp; i++) {
			// sample k with its weight
			int kk = rand.randMult(pk);
			if (kk == k) {
				continue;
			}
			// highly ranked in foreign k
			int rank = rand.randBinom(nranks, pbinom);
			term = rank2bestTerms[kk][rank];
			// System.out.println(String.format(
			// "sampling k = %d rank = %d term = %d %s -> %d", kk, rank,
			// term, resolver.resolveTerm(term), t2rank[term]));
			// low ranked in k
			if (lowestrank < t2rank[term] && !lowTerms.contains(term)) {
				bestterm = term;
				lowestrank = t2rank[term];
				// System.out.println(String.format(
				// "best term = %s, lowest rank = %d",
				// resolver.resolveTerm(term), lowestrank));
			}
			if (lowestrank > phi[0].length / 2) {
				// System.out.println("good: " + resolver.resolveTerm(bestterm)
				// + " -> " + lowestrank);
				break;
			}
		}
		// System.out.println("using " + resolver.resolveTerm(bestterm) + " -> "
		// + lowestrank);
		lowTerms.add(bestterm);
		return bestterm;
	}

	/**
	 * strip term of any space or number stuff (sometimes df values and indices
	 * are in the vocabulary)
	 * 
	 * @param term
	 * @return
	 */
	private String stripTerm(String term) {
		return term.replaceAll("[\\s0-9]+", "");
	}

	// auxiliaries

	/**
	 * print topic with number of terms
	 * 
	 * @param k
	 * @param terms
	 * @param format
	 * @return
	 */
	private String printTopic(int k, int[] terms, boolean withWeights,
			boolean singleLine) {
		int K = phi.length;
		int V = phi[0].length;
		String newLine = singleLine ? " " : "\n\t";
		String u = "";
		if (withWeights) {
			u += String.format("topic %3d (pk = %6.3f/%d, rank = %3d):%s", k,
					getTopicWeights()[k] * K, K, getTopicRanks()[k], newLine);
		}

		for (int i = 0; i < terms.length; i++) {
			int t = terms[i];
			String term = stripTerm(corpus.getResolver().resolveTerm(t));
			if (withWeights) {
				u += String.format("%s (%5.3f/V)%s", term, phi[k][t] * V,
						newLine);
			} else {
				u += String.format("%s%s", term, newLine);
			}
		}
		u += "\n";
		return u;
	}

	/**
	 * get the topic ranks based on the topic weights
	 * 
	 * @return
	 */
	public int[] getTopicRanks() {
		getTopicWeights();
		if (rk == null) {
			// revsort = indices of topics, ranks = index of k in revsort
			rk = IndexQuickSort.inverse(IndexQuickSort.revsort(pk));
		}
		return rk;
	}

	/**
	 * get the relative weights of the topics according to theta. Weights are
	 * cached for repeated calls.
	 * 
	 * @return
	 */
	public double[] getTopicWeights() {
		if (pk == null) {
			pk = new double[phi.length];
			for (int k = 0; k < pk.length; k++) {
				for (int m = 0; m < theta.length; m++) {
					pk[k] += theta[m][k];
				}
			}
			double sum = Vectors.sum(pk);
			Vectors.mult(pk, 1 / sum);
		}
		return pk;
	}
}
