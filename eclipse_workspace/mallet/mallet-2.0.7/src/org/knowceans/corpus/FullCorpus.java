package org.knowceans.corpus;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import org.knowceans.util.IndexQuickSort;
import org.knowceans.util.Print;
import org.knowceans.util.Vectors;

/**
 * a full corpus is a label numeric corpus that uses a resolver to allow
 * consistent access especially to filtered data. Draft stage but usable. <br>
 * <p>
 * TODO: harmonise the usage of label keys.
 * 
 * @deprecated use LabelNumCorpus (more thoroughly debugged)
 * 
 * @author gregor
 * 
 */
public class FullCorpus extends LabelNumCorpus implements ICorpusResolver {

	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		String filebase = "corpus-example/nips";
		filebase = "/data/diss/datasets/acl-anthology/aan-corpus/aan";
		filebase = "/data/diss/mypapers/research-notes/ecml11challenge/whole_dataset/vlex";
		FullCorpus fc = new FullCorpus(filebase);
		fc.loadAllLabels();

		Print.newString();
		Print.fln("%s", fc);
		for (int m = 0; m < fc.numDocs; m++) {

			Print.fln("***\ndocname: %s", fc.resolveDocTitle(m));
			if (fc.hasLabelKeys(fc.KDOCREF) > 0) {
				Print.fln("docref: %s", fc.resolveDocRef(m));
			} else {
				printDocRef(fc, m);
			}
			int[] x = fc.getDoc(m).getTerms();
			int[] f = fc.getDoc(m).getCounts();
			Print.f("%d terms, %d words:", x.length, Vectors.sum(f));
			int[] ranks = IndexQuickSort.reverse(IndexQuickSort.sort(f));
			for (int j = 0; j < x.length; j++) {
				Print.f(" %d:'%s':%d", x[ranks[j]],
						fc.resolveTerm(x[ranks[j]]), f[ranks[j]]);
			}
			Print.fln("");
			x = fc.getDocLabels(fc.LAUTHORS, m);
			if (x != null) {
				Print.f("%d authors:", x.length);
				for (int j = 0; j < x.length; j++) {
					Print.f(" %d:'%s'", x[j], fc.resolveAuthor(x[j]));
				}
				Print.fln("");
			}
			x = fc.getDocLabels(fc.LCATEGORIES, m);
			if (x != null) {
				Print.f("%d categories:", x.length);
				for (int j = 0; j < x.length; j++) {
					Print.fln(" %d:'%s'", x[j], fc.resolveCategory(x[j]));
				}
			}
			x = fc.getDocLabels(fc.LVOLS, m);
			if (x != null) {
				Print.fln("volume: %d:'%s'", x[0], fc.resolveVolume(x[0]));
			}
			x = fc.getDocLabels(fc.LYEARS, m);
			if (x != null) {
				Print.fln("year: '%d'", x[0]);
			}
			x = fc.getDocLabels(fc.LREFERENCES, m);
			if (x != null) {
				Print.fln("%d references:", x.length);
				for (int j = 0; j < x.length; j++) {
					// do we have the full document refs
					if (fc.hasLabelKeys(fc.KDOCREF) > 0) {
						Print.fln(" %d:'%s'", x[j], fc.resolveDocRef(x[j]));
					} else {
						int ref = x[j];
						printDocRef(fc, ref);
					}
				}
			}
			x = fc.getDocLabels(fc.LMENTIONS, m);
			if (x != null) {
				Print.f("%d mentioned authors:", x.length);
				for (int j = 0; j < x.length; j++) {
					Print.f(" %d:%s", x[j], fc.resolveAuthor(x[j]));
				}
				Print.fln("");
			}
		}
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(filebase
					+ ".all.txt"));
			bw.append(Print.getString());
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void printDocRef(FullCorpus fc, int did) {
		// create document ref
		Print.f(" %d:", did);
		if (fc.hasLabels(fc.LAUTHORS) > 0) {
			int[] y = fc.getDocLabels(fc.LAUTHORS, did);
			for (int k = 0; k < y.length; k++) {
				Print.f(" %d", y[k]);
				if (fc.hasLabelKeys(fc.KAUTHORS) > 0) {
					Print.f(":'%s'", fc.resolveAuthor(y[k]));
				}
			}
		}
		if (fc.hasLabelKeys(fc.KDOCS) > 0) {
			Print.f(": %s", fc.resolveDocTitle(did));
		}
		if (fc.hasLabels(fc.LVOLS) > 0) {
			int[] y = fc.getDocLabels(fc.LVOLS, did);
			Print.f(" In: %d", y[0]);
			if (fc.hasLabelKeys(fc.KVOLS) > 0) {
				Print.f(": %s", fc.resolveVolume(y[0]));
			}
		}
		Print.fln("");
	}

	public FullCorpus() {
		super();
		initResolver();
	}

	public FullCorpus(NumCorpus corp) {
		super(corp);
		initResolver();
	}

	public FullCorpus(String dataFilebase, boolean parmode) {
		super(dataFilebase, parmode);
		initResolver();
	}

	public FullCorpus(String dataFilebase, int readlimit, boolean parmode) {
		super(dataFilebase, readlimit, parmode);
		initResolver();
	}

	public FullCorpus(String dataFilebase) {
		super(dataFilebase);
		initResolver();
	}

	private void initResolver() {
		// load resolver
		getResolver();
	}

	/**
	 * loads all labels (metadata)
	 */
	public void loadAllLabels() {
		for (int i = 0; i < LabelNumCorpus.labelExtensions.length; i++) {
			if (hasLabels(i) == 1) {
				System.out.println("loading " + LabelNumCorpus.labelNames[i]);
				getDocLabels(i);
			}
		}
	}

	/**
	 * filters the terms and updates the resolver accordingly
	 */
	@Override
	public int[] filterTermsDf(int minDf, int maxDf) {
		int[] old2new = super.filterTermsDf(minDf, maxDf);
		getResolver().filterTerms(old2new);
		return old2new;
	}

	/**
	 * reduce corpus and. Note: rand is currently ignored!
	 */
	@Override
	public int[] reduce(int ndocs, Random rand) {
		// just reduces the document list
		return super.reduce(ndocs, rand);
		// now reduce the resolver fields

	}

	@Override
	public int hasLabelKeys(int i) {
		return resolver.hasLabelKeys(i);
	}

	public void writeTerms(String file) throws IOException {
		resolver.write(file, resolver.KTERMS);
	}

	@Override
	public String resolveTerm(int t) {
		return resolver.resolveTerm(t);
	}

	@Override
	public int getTermId(String term) {
		return resolver.getTermId(term);
	}

	@Override
	public String resolveCategory(int i) {
		return resolver.resolveCategory(i);
	}

	@Override
	public String resolveAuthor(int i) {
		return resolver.resolveAuthor(i);
	}

	@Override
	public String resolveDocTitle(int i) {
		return resolver.resolveDocTitle(i);
	}

	@Override
	public String resolveDocName(int i) {
		return resolver.resolveDocName(i);
	}

	@Override
	public String resolveDocRef(int i) {
		return resolver.resolveDocRef(i);
	}

	@Override
	public String resolveDocContent(int i) {
		return resolver.resolveDocContent(i);
	}

	@Override
	public String resolveVolume(int i) {
		return resolver.resolveVolume(i);
	}

	@Override
	public String resolveYear(int i) {
		return resolver.resolveYear(i);
	}

	@Override
	public String resolveLabel(int type, int id) {
		return resolver.resolveLabel(type, id);
	}

	@Override
	public int getId(int type, String label) {
		return resolver.getId(type, label);
	}

	/**
	 * write corpus stats to string
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		// corpus statistics
		sb.append(String.format("FullCorpus instance:\n"));
		sb.append(String.format("file base: %s\n", dataFilebase));
		sb.append(String.format("docs: M = %d, V = %d, W = %d\n", getNumDocs(),
				getNumTerms(), getNumWords()));
		sb.append(String.format("labels:\n"));
		for (int i = 0; i < LabelNumCorpus.labelExtensions.length; i++) {
			sb.append(String.format(" %s = %d, .keys = %d\n",
					LabelNumCorpus.labelExtensions[i], hasLabels(i),
					resolver.hasLabelKeys(i + 2)));
			if (hasLabels(i) >= 2) {
				sb.append(String.format("    V = %d, W = %d, max N[m] = %d\n",
						getLabelsV(i), getLabelsW(i), getLabelsMaxN(i)));
			}
		}
		return sb.toString();

	}
}
