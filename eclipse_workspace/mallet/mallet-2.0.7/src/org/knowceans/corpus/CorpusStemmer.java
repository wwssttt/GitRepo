package org.knowceans.corpus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.knowceans.map.IMultiMap;
import org.knowceans.map.InvertibleHashMultiMap;
import org.tartarus.snowball.SnowballStemmer;

/**
 * Uses Snowball to stem a numeric corpus via its resolver.
 * 
 * @author gregor
 * 
 */
public class CorpusStemmer implements ICorpusStemmer {

	public static void main(String[] args) throws Throwable {
		String filebase = "corpus-example/nips";
		LabelNumCorpus corpus = new LabelNumCorpus(filebase);

		int V = corpus.getNumTerms();
		String[] terms = corpus.getResolver().data[CorpusResolver.KTERMS];
		int[] df = corpus.calcDocFreqs();
		for (int i = 0; i < terms.length; i++) {
			System.out.println(terms[i] + ", df = " + df[i]);
		}

		ICorpusStemmer es = new CorpusStemmer.English();
		es.stem(corpus);

		System.out.println("**************************************");

		terms = corpus.getResolver().data[CorpusResolver.KTERMSOURCE];
		df = corpus.calcDocFreqs();
		for (int i = 0; i < terms.length; i++) {
			System.out.println(terms[i] + ", df = " + df[i]);
		}
		System.out.println(String.format("V = %d --> %d", V,
				corpus.getNumTerms()));

	}

	private SnowballStemmer stemmer;

	// / nested convenience subclasses for different languages
	// NOTE: see the org.tartarus.snowball.ext package for more languages

	/**
	 * "Classical" Porter stemmer, consider English for more recent algorithm
	 * <p>
	 * See http://snowball.tartarus.org/algorithms/porter/stemmer.html
	 */
	public static class Porter extends CorpusStemmer {
		public Porter() throws Exception {
			super("porter");
		}
	}

	/**
	 * English (Porter2) stemmer
	 * <p>
	 * See http://snowball.tartarus.org/algorithms/english/stemmer.html
	 */
	public static class English extends CorpusStemmer {
		public English() throws Exception {
			super("english");
		}
	}

	/**
	 * German stemmer
	 * <p>
	 * See http://snowball.tartarus.org/algorithms/german2/stemmer.html
	 */
	public static class German extends CorpusStemmer {
		public German() throws Exception {
			super("german");
		}
	}

	// / end language stemmers //////

	/**
	 * create the stemmer
	 * 
	 * @param language the language of the stemmer
	 * @throws Exception
	 */
	public CorpusStemmer(String language) throws Exception {
		@SuppressWarnings("rawtypes")
		Class stemClass = Class.forName("org.tartarus.snowball.ext." + language
				+ "Stemmer");
		stemmer = (SnowballStemmer) stemClass.newInstance();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IStemmer#stem(org.knowceans.corpus.NumCorpus)
	 */
	@Override
	public int[] stem(NumCorpus corpus) {
		int[] old2new = null;
		try {
			String[] terms = corpus.getResolver().getStrings(
					CorpusResolver.KTERMS);
			int[] df = corpus.calcDocFreqs();
			String[] stemmed = stemTerms(terms);
			old2new = new int[terms.length];
			String[] newIndex = createTermMapping(terms, df, stemmed, old2new);
			corpus.mergeTerms(old2new, newIndex);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return old2new;
	}

	public class Term {
		String value;
		int id;
		int df;

		Term(String value, int id, int df) {
			this.value = value;
			this.id = id;
			this.df = df;
		}
	}

	/**
	 * create mapping from term index to new term index. The
	 * 
	 * @param terms old terms, unique
	 * @param newTerms new terms, with duplicates
	 * @param old2new [out] mapping of old terms to new ones
	 * @return array of new vocabulary that corresponds to old2new (no
	 *         duplicates, so the length of this array is the size of the new
	 *         vocabulary).
	 */
	public String[] createTermMapping(String[] terms, int[] df,
			String[] newTerms, int[] old2new) {
		InvertibleHashMultiMap<Term, String> old2newMap = new InvertibleHashMultiMap<Term, String>();
		for (int i = 0; i < terms.length; i++) {
			Term oldTerm = new Term(terms[i], i, df[i]);
			old2newMap.add(oldTerm, newTerms[i]);
		}
		IMultiMap<String, Term> new2oldMap = old2newMap.getInverse();

		List<Term> ordered = new ArrayList<Term>();
		int i = 0;
		for (String term : new2oldMap.keySet()) {
			int tdf = 0;
			for (Term oldTerm : new2oldMap.get(term)) {
				tdf += oldTerm.df;
			}
			ordered.add(new Term(term, i, tdf));
			i++;
		}
		// sort terms by reverse df, then forward lexicographically
		Collections.sort(ordered, new Comparator<Term>() {

			@Override
			public int compare(Term o1, Term o2) {
				if (o1.df < o2.df) {
					return 1;
				} else if (o1.df > o2.df) {
					return -1;
				} else
					return o1.value.compareTo(o2.value);
			}
		});

		String[] newIndex = new String[new2oldMap.size()];
		for (int j = 0; j < ordered.size(); j++) {
			newIndex[j] = ordered.get(j).value;
			// create inverse mapping
			Set<Term> oldTerms = new2oldMap.get(newIndex[j]);
			String x = "";
			for (Term oldTerm : oldTerms) {
				// x += " " + oldTerm.toString();
				x += " " + oldTerm.value;
				old2new[oldTerm.id] = j;
			}
			newIndex[j] += /* ":" + j + */" <-" + x;
		}
		return newIndex;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IStemmer#stemTerms(java.lang.String[])
	 */
	@Override
	public String[] stemTerms(String[] terms) {
		String[] newTerms = new String[terms.length];
		for (int i = 0; i < terms.length; i++) {
			newTerms[i] = stem(terms[i]);
		}
		return newTerms;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.knowceans.corpus.IStemmer#stem(java.lang.String)
	 */
	@Override
	public synchronized String stem(String word) {
		stemmer.setCurrent(word);
		stemmer.stem();
		return stemmer.getCurrent();
	}
}
