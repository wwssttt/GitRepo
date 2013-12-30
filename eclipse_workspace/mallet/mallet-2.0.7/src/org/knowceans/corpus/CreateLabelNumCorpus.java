package org.knowceans.corpus;

import java.util.Map;
import java.util.TreeMap;

/**
 * As the name implies: create a LabelNumCorpus, in this case from the
 * information fed through the methods to add or set terms or document labels.
 * The corpus has a resolver in the backend that generates unique item ids in
 * the order of addition to the corpus. Both classes operate in tandem to fill
 * the numerical and string representations of the text and meta-data.
 * 
 * @author gregor
 * 
 */
public class CreateLabelNumCorpus extends LabelNumCorpus {

	private CreateCorpusResolver cresolver;

	/**
	 * creates an empty corpus and sets up the file base
	 * 
	 * @param filebase
	 */
	public CreateLabelNumCorpus(String filebase) {
		this.dataFilebase = filebase;
	}

	/**
	 * as we create the corpus, the resolver may be set
	 * 
	 * @param resolver
	 */
	public void setResolver(CorpusResolver resolver) {
		this.resolver = resolver;
		cresolver = (CreateCorpusResolver) resolver;

	}

	/**
	 * allocate labels of the given types with the number of documents
	 * 
	 * @param numDocs
	 * @param types L-types of the labels to be expected
	 */
	public void allocLabels(int numDocs, int[] types) {
		this.numDocs = numDocs;
		labels = new int[labelExtensions.length][][];
		for (int type : types) {
			labels[type] = new int[numDocs][];
		}
		labelsV = new int[labelExtensions.length];
		labelsW = new int[labelExtensions.length];
	}

	/**
	 * allocate the content for the actual corpus essence, the main svmlight
	 * file for most cases. NumDocs is reset.
	 * 
	 * @param numDocs
	 */
	public void allocContent(int numDocs) {
		if (numDocs != this.numDocs) {
			System.out
					.println("warning: labels and corpus have different corpus sizes");
		}
		docs = new Document[numDocs];
		numWords = 0;
	}

	/**
	 * sets the document content to the terms and frequencies. NumDocs is
	 * incremented.
	 * 
	 * @param terms
	 * @param frequencies
	 */
	public void setDocContent(int docId, int[] terms, int[] frequencies) {
		Document doc = new Document();
		doc.setTerms(terms);
		doc.setCounts(frequencies);
		doc.compile();
		docs[docId] = doc;
		numWords += doc.getNumWords();
	}

	/**
	 * sets the document content to the words (after normalisation). NumDocs is
	 * incremented.
	 * 
	 * @param terms
	 * @param frequencies
	 */
	public void setDocContent(int docId, String[] words) {
		Map<Integer, Integer> term2freq = new TreeMap<Integer, Integer>();
		for (String word : words) {
			int term = cresolver.addAndResolve(ICorpusResolver.KTERMS, word);
			Integer freq = term2freq.get(term);
			if (freq == null) {
				freq = 0;
			}
			term2freq.put(term, freq + 1);
		}
		int[] terms = new int[term2freq.size()];
		int[] freqs = new int[term2freq.size()];
		int i = 0;
		// create term and frequency arrays
		for (Integer term : term2freq.keySet()) {
			terms[i] = term;
			freqs[i] = term2freq.get(term);
		}
		setDocContent(docId, terms, freqs);
	}

	/**
	 * set the labels for the given document and label type
	 * 
	 * @param doc
	 * @param labelType
	 * @param values
	 */
	public void setDocLabels(int doc, int labelType, int[] values) {
		labels[labelType][doc] = values;
	}

	/**
	 * add the labels to a particular doc, resolving them first. Resolver needs
	 * to have been set up as using initMapsForLabelTypes() (or ...ForKeyTypes).
	 * 
	 * @param doc document id
	 * @param labelType L-type
	 * @param labelStrings if unknown, the resolver will add them
	 */
	@SuppressWarnings("static-access")
	public void addResolveDocLabels(int doc, int labelType,
			String[] labelStrings) {
		int[] values = cresolver.addAndResolve(
				resolver.labelId2keyExt[labelType], labelStrings);
		labels[labelType][doc] = values;
	}

	/**
	 * add the label to a particular doc, resolving them first. Resolver needs
	 * to have been set up as using initMapsForLabelTypes() (or ...ForKeyTypes).
	 * 
	 * @param doc document id
	 * @param labelType L-type
	 * @param labelString if unknown, the resolver will add it
	 */
	@SuppressWarnings("static-access")
	public void addResolveDocLabel(int doc, int labelType, String labelString) {
		int value = cresolver.addAndResolve(resolver.labelId2keyExt[labelType],
				labelString);
		labels[labelType][doc] = new int[] { value };
		labelsW[labelType]++;
	}

	/**
	 * if the vocabulary is set, the number of terms is updated here
	 * 
	 * @param numTerms
	 */
	public void setNumTerms(int numTerms) {
		this.numTerms = numTerms;
	}

	/**
	 * compile the corpus, so all V and W of labels are up to date. The
	 * vocabulary size is set using setNumTerms because data are higher here.
	 */
	public void compile() {
		for (int type = 0; type < labelExtensions.length; type++) {
			if (labels[type] != null) {
				// set null entries to zero-length arrays
				for (int n = 0; n < numDocs; n++) {
					if (labels[type][n] == null) {
						labels[type][n] = new int[0];
					}
				}
				if (type == LREFERENCES) {
					labelsV[type] = numDocs;
				} else if (type == LMENTIONS) {
					labelsV[type] = LabelNumCorpus
							.getVocabSize(labels[LAUTHORS]);
				} else {
					labelsV[type] = LabelNumCorpus.getVocabSize(labels[type]);
				}
			}
		}
	}

}
