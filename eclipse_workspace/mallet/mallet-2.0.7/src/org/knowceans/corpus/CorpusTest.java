package org.knowceans.corpus;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.knowceans.util.CokusRandom;
import org.knowceans.util.IndexQuickSort;
import org.knowceans.util.Print;

public class CorpusTest {

	public static void main(String[] args) throws IOException {
		// String filebase = "berry95/berry95";
		String filebase = "corpus-example/nips";
		// String filebase = "cs3k/cs3k";
		// String filebase = "cssub/cssub";
		// String filebase =
		// "/Users/gregor/Documents/diss/datasets/acl-anthology/aan-corpus/aan";
		Random rand = new CokusRandom();
		LabelNumCorpus corpus = new LabelNumCorpus(filebase);
		// needed to load labels before splitting the corpus
		// TODO: adjust label type
		final int cat = LabelNumCorpus.LCATEGORIES;
		final int auth = LabelNumCorpus.LAUTHORS;
		corpus.getDocLabels(cat);
		corpus.getDocLabels(auth);

		int[][] w = corpus.getDocWords(rand);
		int V = corpus.getNumTerms();
		int[][] y = corpus.getDocLabels(LabelNumCorpus.LCATEGORIES);
		int Y = corpus.getLabelsV(LabelNumCorpus.LCATEGORIES);
		int Ymax = Math.max(corpus.getLabelsMaxN(LabelNumCorpus.LCATEGORIES),
				corpus.getLabelsMaxN(LabelNumCorpus.LCATEGORIES));

		// filter terms by document frequency
		int[] index = corpus.filterTermsDf(10, 500);
		corpus.write("testcorpus", true);
		corpus.getResolver().filterTerms(index);
		// corpus.getResolver().filterDocuments(index);
		corpus.getResolver().write("testcorpus", ICorpusResolver.KTERMS);

		displayCorpus(corpus);
	}

	/**
	 * display the corpus, resolving its terms
	 * 
	 * @param corpus
	 */
	private static void displayCorpus(LabelNumCorpus corpus) {
		CorpusResolver cr = corpus.getResolver();
		Print.setOutput(System.out);

		// corpus statistics
		Print.fln("========= corpus =========");
		Print.fln("file base: %s", corpus.dataFilebase);
		Print.fln("stats: M = %d, V = %d, W = %d", corpus.getNumDocs(),
				corpus.getNumTerms(), corpus.getNumWords());
		Print.fln("labels:");
		for (int i = 0; i < LabelNumCorpus.labelExtensions.length; i++) {
			Print.fln(" %s = %d, .keys = %d",
					LabelNumCorpus.labelExtensions[i], corpus.hasLabels(i),
					cr.hasLabelKeys(i + 2));
		}

		Print.fln("file base: ", corpus.dataFilebase);

		// print terms
		Print.fln("========= vocabulary =========");
		Print.f("TERMS:");
		int T = 100;// corpus.getNumTerms()
		int[] df = corpus.calcDocFreqs();
		int[] index = IndexQuickSort.sort(df);
		IndexQuickSort.reverse(index);
		for (int t = 0; t < T; t++) {
			if (t % 10 == 0) {
				Print.f("\n\t");
			}
			Print.f("%d:%d %s ", t, df[index[t]], cr.resolveTerm(index[t]));
		}
		Print.fln("");
		Set<Integer> fields = new HashSet<Integer>();
		fields.add(LabelNumCorpus.LAUTHORS);
		fields.add(LabelNumCorpus.LTERMS);
		fields.add(LabelNumCorpus.LDOCS);
		fields.add(LabelNumCorpus.LVOLS);
		fields.add(LabelNumCorpus.LCATEGORIES);
		fields.add(LabelNumCorpus.LREFERENCES);
		// print documents
		Print.fln("\n========= documents =========");
		int M = corpus.getNumDocs();
		for (int m = 0; m < M; m++) {
			printDocument(corpus, cr, m, fields);
		}

	}

	/**
	 * print one document with title and content
	 * 
	 * @param corpus
	 * @param cr
	 * @param m
	 * @param fields null for just terms or array field numbers in
	 *        LabelNumCorpus to be displayed
	 */
	private static void printDocument(LabelNumCorpus corpus, CorpusResolver cr,
			int m, Set<Integer> fields) {
		if (fields.contains(LabelNumCorpus.LDOCS)) {
			Print.fln("##### document %d %s:", m, cr.resolveDocTitle(m));
		} else {
			Print.fln("%d:", m);
		}
		int[] labelTypes = { LabelNumCorpus.LAUTHORS,
				LabelNumCorpus.LCATEGORIES, LabelNumCorpus.LREFERENCES,
				LabelNumCorpus.LTAGS, LabelNumCorpus.LVOLS,
				LabelNumCorpus.LYEARS };
		String[] labelNames = { "AUTHORS:", "CATEGORIES:", "VOLS:",
				"REFERENCES:", "TAGS:", "YEARS:" };
		for (int kind : labelTypes) {
			if (fields.contains(kind)) {
				Print.f(labelNames[kind]);
				for (int label : corpus.getDocLabels(kind, m)) {
					Print.f(" %d:%s", label, cr.resolveLabel(kind, label));
					if (kind == LabelNumCorpus.LREFERENCES) {
						Print.f("\n\t");
					}
				}
				Print.fln("");
			}
		}

		if (fields.contains(LabelNumCorpus.LTERMS)) {
			Print.f("TERMS:");
			int[] index = IndexQuickSort.sort(corpus.docs[m].getCounts());
			IndexQuickSort.reverse(index);
			for (int t = 0; t < corpus.docs[m].getNumTerms(); t++) {
				int term = corpus.docs[m].getTerm(index[t]);
				int count = corpus.docs[m].getCount(index[t]);
				if (t % 10 == 0) {
					Print.f("\n\t");
				}
				Print.f("%s:%d %s ", count, term, cr.resolveTerm(term));
			}
		}
		Print.fln("\n");
	}
}
