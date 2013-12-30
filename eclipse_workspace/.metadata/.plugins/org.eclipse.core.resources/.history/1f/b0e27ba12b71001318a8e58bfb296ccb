package org.knowceans.corpus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.SimpleFSDirectory;
import org.knowceans.map.BijectiveHashMap;
import org.knowceans.map.BijectiveTreeMap;
import org.knowceans.map.IMultiMap;
import org.knowceans.map.InvertibleHashMultiMap;
import org.knowceans.util.Conf;
import org.knowceans.util.StopWatch;
import org.knowceans.util.UnHtml;

/**
 * Driver for ACL Anthology Network extraction as example for Extraction.
 * Content is checked beforehand and gaps in ids directly set up and recognised.
 * <p>
 * Depends on Lucene 3.0
 * <p>
 * Works on 2009 and 2011 version on http://clair.si.umich.edu/clair/anthology/
 * (Caveat: 2011 version has redundant full text content)
 * 
 * @version 1.0 Supersedes AanConverter and AanConverter2. The difference is
 *          that all data are handled in a LabelNumCorpus and its
 *          CreateCorpusResolver.
 * 
 * @author gregor
 */
public class LuceneCorpusExtractor {

	public static void main(String[] args) {
		Conf.setPropFile("conf/aanx.conf");
		// LabelNumCorpus lnc = new LabelNumCorpus();
		// lnc.setDataFilebase(dest);

		LuceneCorpusExtractor a = new LuceneCorpusExtractor();
		// create svmlight-based corpus
		a.run();
	}

	/**
	 * AanDocument represents a document read from the files, optimised for
	 * efficient hashtable lookup via mid.
	 * 
	 * @author gregor
	 */
	class AanDocument {
		int mid;
		String aanid;
		String[] authors;
		String title;
		String content;
		String venue;
		String year;
		int[] citations;
	}

	/**
	 * AAN has string indices, thus additional mapping is needed
	 */
	private BijectiveHashMap<String, Integer> aanid2mid;
	private BijectiveHashMap<Integer, AanDocument> mid2doc;

	private String srcbase;
	private String destbase;
	private String metadataFile;
	private String contentDir;
	private String citationFile;
	private CreateLabelNumCorpus corpus;

	private CreateCorpusResolver resolver;
	private SimpleFSDirectory indexDirectory;
	private int docSize;
	private IndexWriter indexWriter;

	public LuceneCorpusExtractor() {
		srcbase = Conf.get("source.filebase");
		destbase = Conf.get("corpus.filebase");
		metadataFile = Conf.get("source.metadata.file");
		contentDir = Conf.get("source.fulltext.dir");
		citationFile = Conf.get("source.citation.file");
		docSize = Conf.getInt("corpus.abstract.text.chars");

		corpus = new CreateLabelNumCorpus(destbase);
		resolver = new CreateCorpusResolver(destbase);
		corpus.setResolver(resolver);
		try {
			indexDirectory = new SimpleFSDirectory(new File(
					Conf.get("indexer.files")));
		} catch (IOException e) {
			e.printStackTrace();
		}

		mid2doc = new BijectiveHashMap<Integer, AanDocument>();
		aanid2mid = new BijectiveHashMap<String, Integer>();
	}

	/**
	 * run the corpus extractor
	 */
	public void run() {
		try {

			// strategy: we read all metadata, which basically is what we have
			// the relations and authorship for. we then try to match this with
			// content, only considering metadata with given content and vice
			// versa. the basis for comparison is the aan id of the

			StopWatch.start();

			debug("reading metadata");
			aanid2mid = readMetadata();
			debug("reading citations");
			readCitations();
			debug("setting up corpus");
			createMetadata();
			debug("reading and indexing content");
			// save time: index = false
			readAndIndexContent(aanid2mid, false);
			debug("extracting terms from index");
			createVocabulary();
			debug("extracting corpus from index");
			createCorpus();
			debug("checking corpus ");
			debug(corpus.check(true, false));
			debug("writing to " + destbase);
			corpus.write(destbase, true);
			debug("creating document excerpts");
			writeDocs();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * read data for the resolver and authorship information
	 * 
	 * @return
	 * 
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	private BijectiveHashMap<String, Integer> readMetadata()
			throws NumberFormatException, IOException {

		// read the metadata file and set up the respective fields in the
		// resolver

		BufferedReader br;
		String line;
		// read authors + titles + venues + time
		/*
		 * $/release/2008/acl-metadata.txt id = {L08-1260} author = {Vetulani,
		 * Grazyna; Vetulani, Zygmunt; Obrębski, Tomasz} title = {Verb-Noun
		 * Collocation SyntLex Dictionary: Corpus-Based Approach} venue = {LREC}
		 * year = {2008}
		 */
		br = new BufferedReader(new FileReader(srcbase + metadataFile));
		line = null;
		boolean skipdoc = false;
		AanDocument doc = null;
		aanid2mid = new BijectiveHashMap<String, Integer>();

		while ((line = br.readLine()) != null) {
			// replace html entities
			line = UnHtml.getText(line);
			line = line.trim();
			if (line.startsWith("id = ")) {
				String aanid = line.trim().substring(6, line.length() - 1);
				int mid = addAanid(aanid);
				doc = new AanDocument();
				doc.mid = mid;
				doc.aanid = aanid;
				mid2doc.put(mid, doc);
			} else if (!skipdoc) {
				if (line.startsWith("author = ")) {
					line = line.trim().substring(10, line.length() - 1);
					String[] aa = line.split("\\;");
					for (int i = 0; i < aa.length; i++) {
						aa[i] = aa[i].trim();
					}
					doc.authors = aa;
				} else if (line.startsWith("title = ")) {
					line = line.trim().substring(9, line.length() - 1);
					doc.title = line;
				} else if (line.startsWith("venue = ")) {
					line = line.trim().substring(9, line.length() - 1);
					doc.venue = line;
				} else if (line.startsWith("year = ")) {
					line = line.trim().substring(8, line.length() - 1);
					doc.year = line;
				}
			}
		}
		br.close();
		return aanid2mid;
	}

	/**
	 * determine mid for aan id
	 * 
	 * @param aanid
	 * @return
	 */
	private int addAanid(String aanid) {
		Integer mid = aanid2mid.get(aanid);
		if (mid == null) {
			// last index + 1
			mid = aanid2mid.size();
		}
		aanid2mid.put(aanid, mid);
		return mid;
	}

	private void readCitations() throws FileNotFoundException, IOException {
		BufferedReader br;
		String line;
		// read citations
		/*
		 * $/release/2008/networks/paper-citation-network.txt A00-1009 ==>
		 * A92-1006 ordering by first aanid
		 */

		br = new BufferedReader(new FileReader(srcbase + citationFile));
		line = null;
		Pattern p = Pattern.compile("(.+) ==> (.+)");
		IMultiMap<Integer, Integer> mid2mid = new InvertibleHashMultiMap<Integer, Integer>();
		while ((line = br.readLine()) != null) {
			Matcher m = p.matcher(line);
			if (m.find()) {
				String a = m.group(1).trim();
				String b = m.group(2).trim();
				Integer da = aanid2mid.get(a);
				Integer db = aanid2mid.get(b);
				if (da != null && db != null) {
					mid2mid.add(da, db);
				} else {
					// citation for document sans metadata
					// debug("citation for document without metadata " + a +
					// " / "
					// + da + ") -> " + b + " / " + db);
				}
			} else {
				debug("wrong line format: " + line);
			}
		}
		// add citations to documents
		for (Entry<Integer, Set<Integer>> e : mid2mid.entrySet()) {
			AanDocument doc = mid2doc.get(e.getKey());
			if (doc != null) {
				doc.citations = new int[e.getValue().size()];
				int i = 0;
				for (int cit : e.getValue()) {
					doc.citations[i] = cit;
					i++;
				}
			} else {
				debug("citation from document without mid " + e.getKey()
						+ ", aanid for mid = "
						+ aanid2mid.getInverse(e.getKey()));
			}
		}
		br.close();
	}

	/**
	 * reads the full-text information from the raw files and writes it to a
	 * .text file for further processing, as well as a .docs.key file for the
	 * ids
	 * 
	 * @param aanid2mid used to enforce the order of function calls, this one
	 *        after the metadata
	 * @param index
	 * 
	 * @throws IOException
	 */
	private void readAndIndexContent(
			BijectiveHashMap<String, Integer> aanid2mid, boolean index)
			throws Exception {

		// read and index documents
		File docdir = new File(srcbase + contentDir);
		String[] ls = docdir.list();

		// open Lucene
		if (index)
			startIndex();
		StopWatch.lap();

		int m = 0;
		Set<Integer> indexed = new HashSet<Integer>();
		for (String filename : ls) {
			// W09-3334
			if (filename.matches("[A-Z][0-9]+\\-[0-9]+.*\\.txt")) {
				// read content from corpus file
				BufferedReader br = new BufferedReader(new FileReader(
						docdir.getAbsoluteFile() + "/" + filename));
				String line = null;
				StringBuffer sb = new StringBuffer();
				// write content to .text file
				while ((line = br.readLine()) != null) {
					// line format is (\d+:\d+)\s+(.+); we need only content
					line = line.replaceAll("^.+\t", "").trim();
					sb.append(" " + line);
				}

				// get id
				String aanid = filename.substring(0, filename.lastIndexOf('.'))
						.trim();
				if (aanid == null) {
					debug("filename broken? " + filename);
				}
				Integer mid = aanid2mid.get(aanid);
				if (mid == null) {
					// debug(m + " skipping content without metadata: " +
					// aanid);
					// we don't accept content without metadata
					continue;
				}

				AanDocument doc = mid2doc.get(mid);

				if (doc == null) {
					debug("this shouldn't happen: content + metadata with mid but no document: "
							+ aanid);
					// we don't accept content without metadata
					continue;
				}

				indexed.add(mid);

				// don't add full text to document representation to preserve
				if (sb.length() > docSize) {
					doc.content = sb.substring(0, docSize).trim() + "...";
				} else {
					doc.content = sb.toString().trim();
				}
				if (index)
					indexDocument(doc, sb.toString());
				br.close();
				if (m % 500 == 0) {
					debug("lap time = " + StopWatch.lap() + ", m = " + m);
				}
				m++;
			} else {
				debug("invalid file name: " + filename);
			}
		} // for
		debug("documents with content and metadata: " + m);
		m = 0;
		debug("add documents with only metadata...");
		// now index the documents that have only metadata
		for (int mid = 0; mid < mid2doc.size(); mid++) {
			if (!indexed.contains(mid)) {
				AanDocument doc = mid2doc.get(mid);
				if (doc == null) {
					debug("this shouldn't happen: metadata with mid but no document: mid = "
							+ mid);
					continue;
				}
				// index the document with only the title
				if (index)
					indexDocument(doc, null);
				m++;
			}
			if (mid % 500 == 0) {
				debug("m = " + m + ", mid = " + mid);
			}
		}
		debug("documents with only metadata: " + m);
		if (index)
			finishIndex();
	}

	// indexing routines

	/**
	 * create an index of the corpus
	 * 
	 * @throws FileNotFoundException
	 */
	private void startIndex() throws Exception {
		// open Lucene index
		String anaclz = Conf.get("indexer.analyzer.class");
		debug("using analyzer " + anaclz);

		@SuppressWarnings("unchecked")
		Class<Analyzer> cana = (Class<Analyzer>) Class.forName(anaclz);
		Analyzer ana = cana.newInstance();
		indexWriter = new IndexWriter(indexDirectory, ana,
				MaxFieldLength.UNLIMITED);
		// 1GB of buffer
		indexWriter.setRAMBufferSizeMB(1000);
		indexWriter.deleteAll();
	}

	/**
	 * index document in the given document writer
	 * 
	 * @param doc document with all metadata
	 * @param content additional content for the document (not stored in doc to
	 *        preserved overall memory)
	 */
	public void indexDocument(AanDocument aandoc, String content)
			throws Exception {
		// TODO: That's somewhat overkill because in readWriteContent we could
		// just save the file names. However, this is more flexible for other
		// corpora.
		Document doc = new Document();
		Fieldable fmid = new Field("mid", Integer.toString(aandoc.mid),
				Field.Store.YES, Field.Index.NOT_ANALYZED);
		doc.add(fmid);
		Fieldable faanid = new Field("aanid", aandoc.aanid, Field.Store.YES,
				Field.Index.NOT_ANALYZED);
		doc.add(faanid);
		String s = "";
		// only use a single field
		if (content != null) {
			s = content;
		}
		if (aandoc.title != null) {
			// title = escapeDb(title);
			s += " " + aandoc.title;
		}
		Fieldable t = new Field("text", s, Field.Store.NO,
				Field.Index.ANALYZED, Field.TermVector.YES);
		doc.add(t);
		indexWriter.addDocument(doc);
	}

	/**
	 * close and optimise lucene index
	 * 
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	private void finishIndex() throws CorruptIndexException, IOException {
		// finish indexing
		indexWriter.optimize();
		indexWriter.commit();
		indexWriter.close();
	}

	/**
	 * based on the AanDocuments, the corpus is filled. Policy: Any document
	 * with empty metadata is stored with empty values, which may later be added
	 * or removed by filtering with an appropriate DocPredicate in
	 * LabelNumCorpus.
	 */
	@SuppressWarnings("static-access")
	private void createMetadata() {
		int[] labelTypes = { corpus.LAUTHORS, corpus.LVOLS, corpus.LYEARS,
				corpus.LREFERENCES };
		int M = aanid2mid.size();
		corpus.allocLabels(M, labelTypes);

		resolver.initMapsForLabelTypes(labelTypes);
		resolver.allocKeyType(resolver.KDOCS, M);
		resolver.allocKeyType(resolver.KDOCNAME, M);
		resolver.allocKeyType(resolver.KDOCREF, M);

		for (int mid : mid2doc.keySet()) {
			AanDocument doc = mid2doc.get(mid);
			corpus.addResolveDocLabels(mid, corpus.LAUTHORS, doc.authors);
			corpus.addResolveDocLabel(mid, corpus.LVOLS, doc.venue);
			corpus.addResolveDocLabel(mid, corpus.LYEARS, doc.year);
			corpus.setDocLabels(mid, corpus.LREFERENCES, doc.citations);
			resolver.setValue(resolver.KDOCS, mid, doc.title);
			resolver.setValue(resolver.KDOCNAME, mid, doc.aanid);
			resolver.setValue(resolver.KDOCREF, mid, getDocRef(doc));
			if (mid % 500 == 0) {
				// debug("m = " + mid);
			}
		}

		// can remove all maps by now (indexing is done separately)
		resolver.compile(true);
	}

	// index postprocessing routines

	private String getDocRef(AanDocument doc) {
		StringBuffer sb = new StringBuffer();
		String authors = doc.authors[0];
		for (int i = 1; i < doc.authors.length; i++) {
			authors += "; " + doc.authors[i];
		}
		return sb.append(doc.aanid).append(": ").append(authors).append(": ")
				.append(doc.title).append(", ").append(doc.venue).append(", ")
				.append(doc.year).append("\n").toString();
	}

	/**
	 * extracts the vocabulary from the index and creates a mapping to term
	 * indices in the resolver
	 * 
	 * @throws IOException
	 * @throws CorruptIndexException
	 */
	public void createVocabulary() throws IOException, CorruptIndexException {
		BijectiveTreeMap<Integer, String> id2term = new BijectiveTreeMap<Integer, String>();
		int tid = 0;

		// now extract terms from index
		IndexReader ir = IndexReader.open(indexDirectory);
		// filter by minimum document frequency
		int mindf = Conf.getInt("indexer.mindf");
		// that's an alphabetical ordering
		TermEnum terms = ir.terms();
		while (terms.next()) {
			Term term = terms.term();
			int freq = terms.docFreq();
			if (freq >= mindf) {
				String t = term.text();
				id2term.put(tid, t);
				tid++;
				if (tid % 500 == 0) {
					// debug("term id = " + tid + " " + t);
				}
			}
		}

		debug("terms: V = " + tid + " id2term.size() = " + id2term.size());

		// convert this to term list
		String[] termlist = new String[id2term.size()];
		for (int i : id2term.keySet()) {
			termlist[i] = id2term.get(i);
		}
		// resolver is done by now
		resolver.setTerms(termlist, null);
		corpus.setNumTerms(termlist.length);
		corpus.compile();
		ir.close();
	}

	/**
	 * extracts the corpus from the index using the mapping of term indices in
	 * the resolver. Therefore these must be set up prior to calling
	 * createCorpus().
	 * 
	 * @throws IOException
	 * @throws CorruptIndexException
	 */
	public void createCorpus() throws IOException, CorruptIndexException {

		IndexSearcher is = new IndexSearcher(indexDirectory);
		IndexReader ir = is.getIndexReader();

		int M = ir.numDocs();
		System.out.println("index size M = " + M);
		if (aanid2mid.size() != M) {
			debug("warning: index inconsistent: document counts: metadata size = "
					+ aanid2mid.size()
					+ " vs. documents in index (content + metadata) = " + M);
		}
		corpus.allocContent(aanid2mid.size());
		for (int m = 0; m < M; m++) {
			if (m % 500 == 0) {
				debug("m = " + m);
			}

			// Lucene Document
			Document d = is.doc(m);
			int mid = Integer.parseInt(d.get("mid"));
			TermFreqVector[] tv = ir.getTermFreqVectors(m);
			if (tv != null) {
				TreeMap<Integer, Integer> terms = new TreeMap<Integer, Integer>();
				for (int field = 0; field < tv.length; field++) {
					String[] dterms = tv[field].getTerms();
					int[] freqs = tv[field].getTermFrequencies();
					for (int term = 0; term < dterms.length; term++) {
						int tid = resolver.getTermId(dterms[term]);
						if (tid >= 0) {
							Integer freq = terms.get(tid);
							if (freq == null) {
								freq = 0;
							}
							terms.put(tid, freq + freqs[term]);
						} else {
							// probably a low-df term
						}
					}
				}
				int[] tt = new int[terms.size()];
				int[] ff = new int[terms.size()];
				int i = 0;
				for (Entry<Integer, Integer> e : terms.entrySet()) {
					tt[i] = e.getKey();
					ff[i] = e.getValue();
					i++;
				}
				corpus.setDocContent(mid, tt, ff);
			} else {
				debug("document " + m + " no term vector for mid " + mid);
				corpus.setDocContent(mid, new int[0], new int[0]);
			}
		}
	}

	// summary copy routine

	/**
	 * create document excerpts in a file, reading sources and mapping to mid in
	 * corpus. Must be called after readAndIndex
	 * 
	 * @throws Exception
	 */
	private void writeDocs() throws Exception {
		BufferedWriter bw = new BufferedWriter(new FileWriter(destbase
				+ ".text"));
		int mid = 0;
		for (mid = 0; mid < mid2doc.size(); mid++) {
			AanDocument doc = mid2doc.get(mid);
			if (doc == null) {
				debug("no document for mid = " + mid);
			}
			if (doc.content != null) {
				bw.append(doc.content);
			} else {
				bw.append(doc.aanid);
			}
			bw.append("\n");
		} // for
		System.out.println(mid);
		bw.close();
	}

	// debug routine

	/**
	 * print a debug message
	 */
	private void debug(String message) {
		System.out.println(StopWatch.format(StopWatch.read()) + " " + message);

	}

	/**
	 * debug the Lucene index by printing its contents
	 * 
	 * @param ir
	 * @param term2id
	 * @throws CorruptIndexException
	 * @throws IOException
	 */
	protected void debugIndex(IndexReader ir,
			BijectiveHashMap<String, Integer> term2id)
			throws CorruptIndexException, IOException {
		System.out.println("V = " + term2id.size());
		System.out.println(term2id);
		SimpleFSDirectory rd = new SimpleFSDirectory(new File(
				Conf.get("indexer.files")));
		IndexSearcher is = new IndexSearcher(rd);
		int M = ir.numDocs();
		System.out.println("M = " + M);
		for (int m = 0; m < M; m++) {
			Document d = is.doc(m);
			System.out.println(m + ": did = " + d.get("mid")
					+ "*****************************");
			TermFreqVector[] tt = ir.getTermFreqVectors(m);
			for (int field = 0; field < tt.length; field++) {
				System.out.println("field " + field);
				String[] dterms = tt[field].getTerms();
				int[] freqs = tt[field].getTermFrequencies();
				for (int term = 0; term < dterms.length; term++) {
					System.out.println(dterms[term] + " " + freqs[term]);
				}
			}
		}
	}
}
