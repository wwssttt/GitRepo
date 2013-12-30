package org.knowceans.corpus;

import java.util.Random;

public class TestCorpus {

	public static void main(String[] args) throws Exception {
		String filebase = "corpus-example/berry95";
		String outpath = "corpus-example/berry95-out";
		LabelNumCorpus corpus = new LabelNumCorpus(filebase);
		corpus.loadAllLabels();

		// FIXME: LabelNumCorpus with split. not working, labels are null.
		// test splitting
		// ((LabelNumCorpus0) corpus).cutRefsInSplit = false;
		corpus.split(8, 7, new Random());

		System.out.println(corpus.check(true, true));

		LabelNumCorpus tcorpus = (LabelNumCorpus) corpus.getTrainCorpus();

		System.out.println(tcorpus.check(true, true));

		// ////// start preparing corpus ////////

		// stemming of vocabulary
		System.out.println("stemming");
		ICorpusStemmer cse = new CorpusStemmer.English();
		int V = tcorpus.getNumTerms();
		cse.stem(tcorpus);
		System.out.println(String.format("V = %d -> %d", V,
				tcorpus.getNumTerms()));

		// we want to have 100 linked documents

		// either incoming or outgoing links
		System.out.println("removing unlinked documents");
		int M = tcorpus.getNumDocs();
		tcorpus.reduceUnlinkedDocs();
		System.out.println(String.format("M = %d -> %d", M,
				tcorpus.getNumDocs()));

		// adjust the vocabulary
		System.out.println("filtering terms");
		V = tcorpus.getNumTerms();
		tcorpus.filterTermsDf(2, 2000);
		System.out.println(String.format("V = %d -> %d", V,
				tcorpus.getNumTerms()));

		// require a single instance of each label in the corpus
		System.out.println("filtering labels");
		corpus.filterLabels();
		System.out.print("checking corpus\n" + tcorpus.check(true, false));
		System.out.println("writing to " + outpath);

	}
}
