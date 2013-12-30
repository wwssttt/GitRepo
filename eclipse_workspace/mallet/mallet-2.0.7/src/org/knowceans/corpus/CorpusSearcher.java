package org.knowceans.corpus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knowceans.util.IndexQuickSort;
import org.knowceans.util.Vectors;

/**
 * Searches an ICorpus using its resolver using inverted indices. This class is
 * especially useful to debug corpora using the statistics feature before and
 * after filtering and performing random sanity checks via queries. The class
 * provides searching of terms (full text) but also to list terms, authors,
 * categories etc.
 * 
 * @author gregor
 * 
 */
public class CorpusSearcher {

	/**
	 * driver for search engine
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// String filebase = "corpus-example/berry95";
		String filebase = "corpus-example/nips";
		String outpath = "corpus-example/nips-out";
		LabelNumCorpus corpus = new LabelNumCorpus(filebase);
		corpus.loadAllLabels();

		// ////// start preparing corpus ////////

		// stemming of vocabulary
		System.out.println("stemming");
		ICorpusStemmer cse = new CorpusStemmer.English();
		int V = corpus.getNumTerms();
		cse.stem(corpus);
		System.out.println(String.format("V = %d -> %d", V,
				corpus.getNumTerms()));

		// we want to have 100 linked documents

		// either incoming or outgoing links
		System.out.println("removing unlinked documents");
		int M = corpus.getNumDocs();
		corpus.reduceUnlinkedDocs();
		System.out
				.println(String.format("M = %d -> %d", M, corpus.getNumDocs()));

		// choose a random subset of 100 documents (removes then-outside
		// references)
		// System.out.println("reducing documents");
		// M = corpus.getNumDocs();
		// corpus.reduce(100, new Random());
		// System.out
		// .println(String.format("M = %d -> %d", M, corpus.getNumDocs()));

		// adjust the vocabulary
		System.out.println("filtering terms");
		V = corpus.getNumTerms();
		corpus.filterTermsDf(2, 2000);
		System.out.println(String.format("V = %d -> %d", V,
				corpus.getNumTerms()));

		// require a single instance of each label in the corpus
		System.out.println("filtering labels");
		corpus.filterLabels();
		System.out.print("checking corpus\n" + corpus.check(true, false));
		System.out.println("writing to " + outpath);

		// /////// end preparing corpus /////////

		// corpus.write(outpath, true);
		CorpusSearcher cs = new CorpusSearcher(corpus, true);
		cs.setStemmer(cse);
		cs.interact();
	}

	private LabelNumCorpus corpus;
	private CorpusResolver resolver;
	private ICorpusStemmer stemmer;
	private String help = "Query or .q to quit, .h, ? for this message, .s for stats, Enter to page results, .d<rank> or .m<id> to view doc, \n"
			+ "       .t, .a, .c<prefix> to view terms, authors, categories list, .T, .A, .C<prefix> to view particular item:";

	/**
	 * inverted index
	 */
	private Map<Integer, Map<Integer, Integer>> termDocFreqIndex;
	private int[] docFreqs;
	private Map<Integer, Set<Integer>> authorIndex;
	private Map<Integer, Set<Integer>> labelIndex;
	private String[][] sortedKeyLists;
	private int[][] keyList2id;
	// citations are the (sparse) transpose of references
	private int[][] citations;

	/**
	 * inits the searcher with the given corpus, which needs to have a resolver
	 * to be used with human-readable queries. Loads any index found for the
	 * corpus.
	 * 
	 * @param corpus
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public CorpusSearcher(LabelNumCorpus corpus) throws IOException,
			ClassNotFoundException {
		this(corpus, false);
	}

	/**
	 * create corpus but reindex
	 * 
	 * @param corpus
	 * @param reindex false to load index or index and save, true to create
	 *        index temporarily
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public CorpusSearcher(LabelNumCorpus corpus, boolean reindex)
			throws IOException, ClassNotFoundException {
		this.corpus = corpus;
		this.resolver = corpus.getResolver();
		sortedKeyLists = new String[CorpusResolver.keyExtensions.length][];
		keyList2id = new int[CorpusResolver.keyExtensions.length][];
		if (!reindex && !loadIndex()) {
			System.out.println("indexing");
			createIndex();
			System.out.println("saving to " + corpus.dataFilebase + ".idx");
			saveIndex();
		} else {
			// load a fresh index
			System.out.println("indexing");
			createIndex();
		}
	}

	/**
	 * set the stemmer for querying
	 * 
	 * @param es
	 */
	public void setStemmer(ICorpusStemmer stemmer) {
		this.stemmer = stemmer;
	}

	/**
	 * opens the inverted index, if the corpus has file information and the
	 * respective file exists.
	 * 
	 * @return true if index was loaded
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked")
	private boolean loadIndex() throws IOException, ClassNotFoundException {
		File index = new File(corpus.dataFilebase + ".idx");
		if (index.exists()) {
			ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(
					new FileInputStream(index)));
			// get directory (this may be also done by fixed sequence and null
			// objects)
			String directory = ois.readUTF();
			String[] objects = directory.split(" ");
			for (String object : objects) {
				if (object.equals("terms")) {
					termDocFreqIndex = (Map<Integer, Map<Integer, Integer>>) ois
							.readObject();
					docFreqs = (int[]) ois.readObject();
				} else if (object.equals("authors")) {
					authorIndex = (Map<Integer, Set<Integer>>) ois.readObject();
				} else if (object.equals("labels")) {
					labelIndex = (Map<Integer, Set<Integer>>) ois.readObject();
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * save the index
	 * 
	 * @throws IOException
	 */
	private void saveIndex() throws IOException {
		// INFO: inefficient but avoids deps. to e.g. Prevayler
		File index = new File(corpus.dataFilebase + ".idx");
		ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(
				new FileOutputStream(index)));
		String directory = "";
		if (termDocFreqIndex != null) {
			directory += " " + "terms";
		}
		if (authorIndex != null) {
			directory += " " + "authors";
		}
		if (labelIndex != null) {
			directory += " " + "labels";
		}
		oos.writeUTF(directory);
		if (termDocFreqIndex != null) {
			oos.writeObject(termDocFreqIndex);
			oos.writeObject(docFreqs);
		}
		if (authorIndex != null) {
			oos.writeObject(authorIndex);
		}
		if (labelIndex != null) {
			oos.writeObject(labelIndex);
		}
		oos.flush();
		oos.close();
	}

	/**
	 * represents a preprocessed query
	 */
	public class Query {
		String[] terms;
		String raw;
	}

	/**
	 * interactively search the index
	 */
	public void interact() {
		try {
			System.out.println(help);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			Query lastQuery = null;
			List<Result> results = null;
			int listtype = -1;
			int listpos = -1;
			int resultsPage = 0;
			int pageSize = 10;
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.equals(".q")) {
					break;
				} else if (line.equals("")) {
					// continue paging
					if (results != null) {
						resultsPage++;
						printResults(lastQuery, results,
								resultsPage * pageSize, pageSize);
					} else if (listpos >= 0) {
						resultsPage++;
						printListPage(listtype, listpos + resultsPage
								* pageSize, pageSize);
					}
				} else if (line.startsWith(".d") && line.length() > 2) {
					String arg = line.substring(2);
					if (!arg.matches("[0-9]+")) {
						continue;
					}
					int rank = Integer.parseInt(arg);
					if (results != null && results.size() > rank) {
						System.out.println("result rank " + rank + ":");
						int id = results.get(rank).id;
						printDoc(lastQuery, id);
						System.out
								.println("***********************************");
					}
				} else if (line.startsWith(".m") && line.length() > 2) {
					String arg = line.substring(2);
					if (!arg.matches("[0-9]+")) {
						continue;
					}
					lastQuery = null;
					int m = Integer.parseInt(arg);
					System.out.println("document id = " + m + ":");
					printDoc(lastQuery, m);
					System.out.println("***********************************");
				} else if ((line.startsWith(".A") || line.startsWith(".C") || line
						.startsWith(".T")) && line.length() > 2) {
					String prefix = line.substring(2).trim();
					System.out.println("prefix " + prefix + ":");
					if (line.charAt(1) == 'A') {
						printAuthor(searchList(ICorpusResolver.KAUTHORS, prefix));
					} else if (line.charAt(1) == 'C') {
						printCategory(searchList(ICorpusResolver.KCATEGORIES,
								prefix));
					} else if (line.charAt(1) == 'T') {
						printTerm(searchList(ICorpusResolver.KTERMS, prefix));
					}

					System.out.println("***********************************");
				} else if (line.startsWith(".t") || line.startsWith(".a")
						|| line.startsWith(".c") || line.startsWith(".d")
						|| line.startsWith(".m")) {
					results = null;
					resultsPage = 0;
					listpos = 0;
					String prefix = line.substring(2).trim();
					if (line.charAt(1) == 't') {
						listtype = ICorpusResolver.KTERMS;
					} else if (line.charAt(1) == 'a') {
						listtype = ICorpusResolver.KAUTHORS;
					} else if (line.charAt(1) == 'c') {
						listtype = ICorpusResolver.KCATEGORIES;
					} else if (line.charAt(1) == 'd') {
						listtype = ICorpusResolver.KDOCREF;
					} else if (line.charAt(1) == 'm') {
						listtype = ICorpusResolver.KDOCREF;
					} else {
						// error
					}
					listpos = searchList(listtype, prefix);
					printListPage(listtype, listpos, pageSize);
				} else if (line.startsWith(".h") || line.startsWith("?")) {
					System.out.println(help);
				} else if (line.startsWith(".s")) {
					// print statistics
					System.out.println(corpus);
				} else {
					Query q = parseQuery(line);
					System.out.println("Query parsed: "
							+ Vectors.print(q.terms));
					results = search(q);
					lastQuery = q;
					resultsPage = 0;
					listpos = -1;
					System.out.println(results.size() + " results: ");
					printResults(q, results, 0, pageSize);
					System.out.println("***********************************");
					System.out.println("query:");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * load list of a given type
	 * 
	 * @param type
	 * @returns true if list was loadedF
	 */
	protected boolean loadList(int type) {
		if (sortedKeyLists[type] == null) {
			String[] aa = resolver.getStrings(type);
			if (aa != null) {
				sortedKeyLists[type] = new String[aa.length];
				for (int i = 0; i < aa.length; i++) {
					if (aa[i] == null) {
						aa[i] = CorpusResolver.keyNames[type] + i;
					} else {
						sortedKeyLists[type][i] = aa[i];
					}
				}
				// Print.arraysRowSep("\n", sortedKeyLists[type]);
				keyList2id[type] = IndexQuickSort.sort(sortedKeyLists[type]);
				IndexQuickSort.reorder(sortedKeyLists[type], keyList2id[type]);
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * search the list of the type, printing pageSize results
	 * 
	 * @param type
	 * @param prefix
	 * @return
	 */
	protected int searchList(int type, String prefix) {
		if (!loadList(type)) {
			return 0;
		}
		// search for entry point
		int pos = Arrays.binarySearch(sortedKeyLists[type], prefix);
		if (pos < 0) {
			pos = -pos - 1;
		}
		return pos;
	}

	/**
	 * print one page of the list starting at start
	 * 
	 * @param type
	 * @param start
	 * @param pageSize
	 * @return new start position
	 */
	protected int printListPage(int type, int start, int pageSize) {
		if (sortedKeyLists[type] == null) {
			return 0;
		}
		int listpos;
		for (listpos = start; listpos < start + pageSize; listpos++) {
			if (listpos < sortedKeyLists[type].length) {
				int id = keyList2id[type][listpos];
				int df = 0;
				if (type == ICorpusResolver.KTERMS) {
					df = docFreqs[id];
					String source = "";
					if (stemmer != null) {
						source = resolver.resolveTermSource(id);
						source = " < "
								+ source.substring(source.indexOf("<-") + 2);
					}
					System.out.println(sortedKeyLists[type][listpos]
							+ ", id = " + id + ", df = " + df + " " + source);
				} else {
					if (type == ICorpusResolver.KAUTHORS) {
						df = authorIndex.get(id).size();
					} else if (type == ICorpusResolver.KCATEGORIES) {
						df = labelIndex.get(id).size();
					}
					System.out.println(sortedKeyLists[type][listpos]
							+ ", id = " + id + ", df = " + df);
				}
			}
		}
		return listpos;
	}

	/**
	 * print the document
	 * 
	 * @param id
	 */
	protected void printDoc(Query query, int id) {
		boolean printStats = true;
		int statsTerms = 32;
		if (id >= corpus.numDocs) {
			System.out.println("invalid id");
			return;
		}
		System.out.println("Document id = " + id);
		String title = resolver.resolveDocRef(id);
		if (title == null) {
			String auth = "";
			for (int a : corpus.getDocLabels(LabelNumCorpus.LAUTHORS, id)) {
				String author = resolver.resolveAuthor(a);
				auth += " " + author != null ? author : "author" + a;
			}
			String name = resolver.resolveDocTitle(id);
			if (auth != null && name != null) {
				title = auth + ": " + name;
			}
		}
		String content = resolver.resolveDocContent(id);
		if (title != null) {
			title = highlight(title, query);
			System.out.println(wordWrap(title, 120));
		}
		if (content != null) {
			content = highlight(content, query);
			System.out.println(wordWrap(content, 120));
		}
		if (content == null || printStats) {
			int cols = 4;
			corpus.getDoc(id);

			int[] tt = corpus.getDoc(id).getTerms();
			int[] ff = corpus.getDoc(id).getCounts();
			System.out.println(resolver.termStats(tt, ff, statsTerms, cols));
		}
		if (corpus.hasLabels(LabelNumCorpus.LREFERENCES) == 2) {
			int[] refs = corpus.getDocLabels(LabelNumCorpus.LREFERENCES, id);
			if (refs.length > 0) {
				System.out.println("References:");
				int i = 0;
				for (int ref : refs) {
					i++;
					System.out.println(i + ". id = " + ref + ": "
							+ getDocName(ref));
				}
			}
		}
		int[][] allrefs = corpus.getDocLabels(LabelNumCorpus.LREFERENCES);
		if (allrefs != null) {
			if (citations == null) {
				citations = LabelNumCorpus.getCitesFromRefs(allrefs);
			}
			if (citations[id].length > 0) {
				System.out.println("Citations:");
				int i = 0;
				for (int m : citations[id]) {
					i++;
					System.out
							.println(i + ". id = " + m + ": " + getDocName(m));
				}
			}
		}
		if (query != null) {
			for (String term : query.terms) {
				int termid = resolver.getTermId(term);
				if (termid >= 0) {
					System.out.println(term + ", id = " + termid + ", df = "
							+ docFreqs[termid] + ", tf = "
							+ termDocFreqIndex.get(termid).get(id));
				}
			}
		}
	}

	protected String getDocName(int m) {
		String refname = resolver.resolveDocRef(m);
		if (refname == null) {
			String auth = "";
			for (int a : corpus.getDocLabels(LabelNumCorpus.LAUTHORS, m)) {
				String author = resolver.resolveAuthor(a);
				auth += " " + author != null ? author : "author" + a;
			}
			String name = resolver.resolveDocTitle(m);
			if (auth != null && name != null) {
				refname = auth + ": " + name;
			} else {
				refname = "document" + m;
			}
		}
		return refname;
	}

	/**
	 * prints the given author
	 * 
	 * @param pos position in author list
	 */
	private void printAuthor(int pos) {
		int id = keyList2id[ICorpusResolver.KAUTHORS][pos];
		System.out.println("Author #" + pos + ", id = " + id + ": "
				+ resolver.resolveAuthor(id) + ":");
		System.out.println("Documents: ");
		Set<Integer> docs = authorIndex.get(id);

		int i = 0;
		for (int doc : docs) {
			i++;
			System.out.println(i + ". id = " + doc + ": " + getDocName(doc));
		}
		if (corpus.hasLabels(LabelNumCorpus.LMENTIONS) == 2) {
			int[][] ment = corpus.getDocLabels(LabelNumCorpus.LMENTIONS);
			Set<Integer> ments = new HashSet<Integer>();
			for (int m = 0; m < ment.length; m++) {
				for (i = 0; i < ment[m].length; i++) {
					if (ment[m][i] == id && !authorIndex.get(id).contains(m)) {
						ments.add(m);
					}
				}
			}
			if (ments.size() > 0) {
				System.out.println("Mentions:");
				int j = 0;
				for (int m : ments) {
					j++;
					System.out
							.println(j + ". id = " + m + ": " + getDocName(m));
				}
			}
		}
	}

	/**
	 * prints the given category
	 * 
	 * @param pos position in label list displayed
	 */
	private void printCategory(int pos) {
		int id = keyList2id[ICorpusResolver.KCATEGORIES][pos];
		System.out.println("Category #" + pos + ", id = " + id + ": "
				+ resolver.resolveCategory(id));
		System.out.println("Documents: ");
		Set<Integer> docs = labelIndex.get(id);
		if (docs == null) {
			System.out.println("[empty]");
			return;
		}
		int i = 0;
		for (int doc : docs) {
			i++;
			System.out.println(i + ". " + resolver.resolveDocRef(doc));
		}
	}

	/**
	 * print the term information
	 * 
	 * @param pos
	 */
	private void printTerm(int pos) {
		loadList(ICorpusResolver.KTERMS);
		int id = keyList2id[ICorpusResolver.KTERMS][pos];
		String source = resolver.resolveTermSource(id);
		System.out.print("Term id = " + id + ": ");
		System.out.println(source != null ? source
				: sortedKeyLists[ICorpusResolver.KTERMS][pos]);
		Map<Integer, Integer> termDocs = termDocFreqIndex.get(id);
		// calculate sum
		int tf = 0;
		for (int i : termDocs.values()) {
			tf += i;
		}
		System.out.println("Global frequencies: df = " + docFreqs[id]
				+ ", tf = " + tf);
		System.out.println("Documents (id=tf):\n"
				+ wordWrap(new TreeMap<Integer, Integer>(termDocs).toString(),
						120));
	}

	/**
	 * highlight query terms in content
	 * 
	 * @param content
	 * @param query
	 * @return
	 */
	private String highlight(String content, Query query) {
		if (query == null || query.equals("")) {
			return content;
		}
		String[] terms = query.raw.split(" ");
		Arrays.sort(terms);
		if (stemmer != null) {
			// in the stemming case, we search for words whose stemmed version
			// is in the set of stemmed query terms
			StringBuffer sb = new StringBuffer();
			String[] words = content.split(" ");
			for (int i = 0; i < words.length; i++) {
				if (Arrays.binarySearch(terms,
						stemmer.stem(words[i].toLowerCase())) > -1) {
					words[i] = "***" + words[i] + "***";
				}
				if (i > 0) {
					sb.append(' ');
				}
				sb.append(words[i]);
			}
			content = sb.toString();
		} else {
			for (String term : terms) {
				content = content.replaceAll("\\s(?i)" + term + "\\s", " ***"
						+ term + "*** ");
			}
		}

		return content;
	}

	/**
	 * word wrap the document before column
	 * 
	 * @param content
	 * @param i
	 * @return
	 */
	public static String wordWrap(String content, int columns) {
		if (content == null) {
			return null;
		}
		// word wrap line
		StringBuffer sb = new StringBuffer();
		int prevword = 0;
		int curword = 0;
		int curline = 0;
		int lastspace = content.lastIndexOf(' ');
		if (lastspace == -1) {
			return content;
		}
		while (curword < lastspace) {
			prevword = curword;
			curword = content.indexOf(' ', curword + 1);
			if (curword - curline > columns) {
				sb.append(content.substring(curline, prevword).trim()).append(
						'\n');
				curline = prevword;
			}
		}
		sb.append(content.substring(curline).trim());
		return sb.toString();
	}

	/**
	 * print query and search result
	 * 
	 * @param query
	 * @param results
	 * @param start
	 * @param count
	 */
	private void printResults(Query query, List<Result> results, int start,
			int count) {
		System.out.println("results for query \"" + query.raw + "\":");
		for (int i = start; i < start + count; i++) {
			if (i >= results.size()) {
				return;
			}
			Result result = results.get(i);
			System.out.println(i + ". score = " + result.score + ", id = "
					+ result.id);
			System.out.println("\t" + result.id + ". " + getDocName(result.id));
		}
	}

	/**
	 * result item
	 */
	class Result implements Comparable<Result> {
		int id;
		double score;

		Result(int id, double score) {
			this.id = id;
			this.score = score;
		}

		@Override
		public int compareTo(Result that) {
			// default ranking order is reverse
			return this.score == that.score ? 0 : this.score < that.score ? 1
					: -1;
		}
	}

	// /////////////////////////

	/**
	 * index a set of strings
	 * 
	 * @param corpus
	 */
	private void createIndex() {
		termDocFreqIndex = corpus.getTermDocMap();
		docFreqs = corpus.calcDocFreqs();

		if (corpus.hasLabels(LabelNumCorpus.LAUTHORS) == 2) {
			authorIndex = indexLabels(LabelNumCorpus.LAUTHORS);
		}
		if (corpus.hasLabels(LabelNumCorpus.LCATEGORIES) == 2) {
			labelIndex = indexLabels(LabelNumCorpus.LCATEGORIES);
		}
	}

	/**
	 * index the label type by creating an inverted index to lookup documents
	 * 
	 * @param type
	 * @return
	 */
	protected HashMap<Integer, Set<Integer>> indexLabels(int type) {
		HashMap<Integer, Set<Integer>> index = new HashMap<Integer, Set<Integer>>();
		for (int m = 0; m < corpus.getNumDocs(); m++) {
			int[] lab = corpus.labels[type][m];
			for (int i = 0; i < lab.length; i++) {
				Set<Integer> docs = index.get(lab[i]);
				if (docs == null) {
					docs = new HashSet<Integer>();
					index.put(lab[i], docs);
				}
				docs.add(m);
			}
		}
		return index;
	}

	// ///// query stuff //////

	/**
	 * parse the query. Currently this tokenises the string and creates stemmed
	 * versions of the elements if the stemmer is used for the corpus
	 * 
	 * @param string
	 */
	public Query parseQuery(String string) {
		// tokenise query
		String[] terms = string.split(" ");
		Query q = new Query();
		q.raw = string;
		q.terms = new String[terms.length];
		if (stemmer != null) {
			for (int i = 0; i < terms.length; i++) {
				q.terms[i] = stemmer.stem(terms[i]);
			}
		} else {
			q.terms = string.split(" +");
		}
		return q;
	}

	/**
	 * search for a single term in the corpus
	 * 
	 * @param term (stemmed if stemmer is used)
	 * @return
	 */
	public Map<Integer, Double> findTerm(String term) {
		int termid = resolver.getTermId(term);
		Map<Integer, Double> results = new HashMap<Integer, Double>();
		Map<Integer, Integer> docs2freqs = termDocFreqIndex.get(termid);
		if (docs2freqs != null) {
			for (Entry<Integer, Integer> doc : docs2freqs.entrySet()) {
				// each match is scored 1
				results.put(doc.getKey(), (double) doc.getValue());
			}
		}
		return results;
	}

	/**
	 * search for a set of words in the corpus
	 * 
	 * @param query
	 * @return
	 */
	public List<Result> search(Query query) {
		Map<Integer, Double> scoreMap = new HashMap<Integer, Double>();
		// get individual results and merge scores
		for (int i = 0; i < query.terms.length; i++) {
			Map<Integer, Double> termRes = findTerm(query.terms[i]);
			// intersection
			scoreMap = (i == 0) ? termRes : mergeResults(scoreMap, termRes,
					true);
		}
		// create results list from merged results
		List<Result> results = new ArrayList<Result>();
		for (Entry<Integer, Double> result : scoreMap.entrySet()) {
			results.add(new Result(//
					result.getKey(), result.getValue()));
		}
		Collections.sort(results);
		return results;
	}

	/**
	 * merge the scores the maps
	 * 
	 * @param map1
	 * @param map2
	 * @param intersection intersect (true) or unite (false)
	 * @param result
	 */
	private Map<Integer, Double> mergeResults(Map<Integer, Double> map1,
			Map<Integer, Double> map2, boolean intersection) {
		HashMap<Integer, Double> result = new HashMap<Integer, Double>();
		Set<Integer> mergedKeys = map1.keySet();
		if (!intersection) {
			mergedKeys.addAll(map2.keySet());
		} else {
			mergedKeys.retainAll(map2.keySet());
		}
		// now add the values for the merged keys
		for (int key : mergedKeys) {
			Double val1 = map1.get(key);
			Double val2 = map2.get(key);

			if (val1 == null) {
				val1 = 0.;
			}
			if (val2 == null) {
				val2 = 0.;
			}
			result.put(key, val1 + val2);
		}
		return result;
	}
}
