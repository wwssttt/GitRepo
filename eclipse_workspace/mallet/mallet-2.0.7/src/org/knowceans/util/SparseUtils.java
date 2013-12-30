package org.knowceans.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Sparse utilities used to manipulate sparse (i.e., svmlight) data, represented
 * by two matrices: int[][][] {int[][] {[indices]}, int[][] {[frequencies]}}
 * 
 * @author gregor
 * 
 */
@SuppressWarnings("unchecked")
public class SparseUtils {
	/**
	 * sparse multiplication of two matrices, c = a * b' or c = a' * b (if
	 * transpose).
	 * 
	 * @param as sparse indices + weights [2 x M x A]
	 * @param A
	 * @param bs sparse indices + weights [2 x N x B]
	 * @param B
	 * @param cs sparse indices + weights [2 x A x B] or [2 x M x N], must be
	 *        allocated as int[2][A][] or int[2][M][], respectively
	 */
	// TODO: * @param transp use transposes
	public static double multiplySparse(int[][][] as, int A, int[][][] bs,
			int B, int[][][] cs) {

		int[][] ax = as[0], aw = as[1], bx = bs[0], bw = bs[1];
		int M = ax.length;
		Map<Integer, Integer>[] x = new Map[A];
		long elements = 0;

		for (int m = 0; m < M; m++) {
			for (int i = 0; i < ax[m].length; i++) {
				int ii = ax[m][i];
				if (x[ii] == null) {
					x[ii] = new HashMap<Integer, Integer>();
				}
				// we are in sparse row a (which iterates differently
				// with m), now iterate sparse colume b (also different per m)
				for (int j = 0; j < bx[m].length; j++) {
					// product c_ij = a_mi * b_mj
					int prod = aw[m][i] * bw[m][j];
					// get element in c_ij
					Integer ij = x[ii].get(bx[m][j]);
					if (ij == null) {
						x[ii].put(bx[m][j], prod);
						// Print.fln(
						// "%d sparse m = %d, a = %d, b = %d, full a = %d, b = %d, prod = %d",
						// (elements++), m, i, j, ax[m][i], bx[m][j], prod);
					} else {
						// add new value to element
						x[ii].put(bx[m][j], prod + x[ii].get(bx[m][j]));
					}
				}
			}
		}
		// now fill the sparse array
		for (int i = 0; i < A; i++) {
			if (x[i] != null) {
				cs[0][i] = new int[x[i].size()];
				cs[1][i] = new int[x[i].size()];
				int j = 0;
				for (Entry<Integer, Integer> e : x[i].entrySet()) {
					// Print.fln("i = %d, s = %d, val = %d", i, e.getKey(),
					// e.getValue());
					cs[0][i][j] = e.getKey();
					cs[1][i][j] = e.getValue();
					j++;
				}
			} else {
				cs[0][i] = new int[0];
				cs[1][i] = new int[0];
			}
		}
		return elements / (double) A / (double) B;
	}

	/**
	 * dense version of function above
	 * 
	 * @param as (if second component null, all frequencies assumed 1)
	 * @param A
	 * @param bs
	 * @param B
	 * @param c dense array A x A
	 * @return
	 */
	public static void multiplySparse2dense(int[][][] as, int A, int[][][] bs,
			int B, int[][] c) {

		int[][] ax = as[0], aw = as[1], bx = bs[0], bw = bs[1];
		int M = ax.length;

		for (int m = 0; m < M; m++) {
			for (int i = 0; i < ax[m].length; i++) {
				int ii = ax[m][i];
				// we are in sparse row a (which iterates differently
				// with m), now iterate sparse colume b (also different per m)
				for (int j = 0; j < bx[m].length; j++) {
					// add new value to element
					int a = aw != null ? aw[m][i] : 1;
					int b = bw != null ? bw[m][j] : 1;
					if (ii >= c.length || bx[m][j] >= c[ii].length) {
						// Print.fln("m = %d i = %d j = %d ii = %d bxmj = %d",
						// m,
						// i, j, ii, bx[m][j]);
					} else {
						c[ii][bx[m][j]] += a * b;
					}
				}
			}
		}
	}

	/**
	 * create transpose of sparse matrix
	 * 
	 * @param as
	 * @param [out] ast transpose
	 */

	public static void transpose(int[][][] as, int A, int[][][] ast) {
		int M = as[0].length;
		Map<Integer, Integer>[] x = new Map[A];
		for (int m = 0; m < M; m++) {
			for (int n = 0; n < as[0][m].length; n++) {
				int ii = as[0][m][n];
				if (x[ii] == null) {
					x[ii] = new HashMap<Integer, Integer>();
				}
				x[ii].put(m, as[1][m][n]);
			}
		}
		map2sparse(x, ast);
	}

	/**
	 * create a sparse array from the map
	 * 
	 * @param x
	 * @param [out] xs
	 */
	public static void map2sparse(Map<Integer, Integer>[] x, int[][][] xs) {
		// now fill the sparse array
		for (int i = 0; i < x.length; i++) {
			if (x[i] != null) {
				xs[0][i] = new int[x[i].size()];
				xs[1][i] = new int[x[i].size()];
				int j = 0;
				for (Entry<Integer, Integer> e : x[i].entrySet()) {
					xs[0][i][j] = e.getKey();
					xs[1][i][j] = e.getValue();
					j++;
				}
			} else {
				xs[0][i] = new int[0];
			}
		}
	}

	/**
	 * sparse array to dense array
	 * 
	 * @param ax
	 * @param aw
	 * @param A
	 * @return
	 */
	public static int[][] sparse2dense(int[][][] as, int A) {
		int[][] ax = as[0];
		int[][] aw = as[1];
		int M = ax.length;
		int[][] a = new int[M][A];

		for (int m = 0; m < M; m++) {
			for (int i = 0; i < ax[m].length; i++) {
				a[m][ax[m][i]] = aw[m][i];
			}
		}
		return a;
	}

	/**
	 * dense array to sparse array
	 * 
	 * @param a
	 * @return
	 */
	public static int[][][] dense2sparse(int[][] a) {
		int M = a.length;

		Map<Integer, Integer>[] x = new Map[M];

		for (int i = 0; i < M; i++) {
			x[i] = new HashMap<Integer, Integer>();
			for (int j = 0; j < a[i].length; j++) {
				if (a[i][j] != 0) {
					x[i].put(j, a[i][j]);
				}
			}
		}
		int[][][] as = new int[2][M][];
		// now fill the sparse array
		map2sparse(x, as);
		return as;
	}

	/**
	 * sort sparse matrix
	 * 
	 * @param as
	 * @param sort 0 no, 1 by index, 2 by frequency, negative = reverse
	 */
	public static void sort(int[][][] as, int sort) {
		int asort = sort > 0 ? sort : -sort;
		for (int m = 0; m < as[0].length; m++) {

			if (as[1][m] == null) {
				System.out.println("0\n");
				continue;
			}

			int[] s = null;
			// sort by index/frequency
			if (as[1][m].length > 0) {
				if (asort == 1 || asort <= 2) {
					s = IndexQuickSort.sort(as[asort - 1][m]);
					if (sort < 0) {
						IndexQuickSort.reverse(s);
					}
					IndexQuickSort.reorder(as[0][m], s);
					IndexQuickSort.reorder(as[1][m], s);
				}
			}
		}
	}

	/**
	 * write array to file in svmlight format
	 * 
	 * @param as
	 * @param filename
	 * @throws IOException
	 */
	public static void write(int[][][] as, String filename) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename));

		for (int m = 0; m < as[0].length; m++) {

			if (as[1][m] == null) {
				bw.append("0").append("\n");
				continue;
			}

			int[] s = null;
			// sort by index/frequency
			if (as[1][m].length > 0) {
				s = IndexQuickSort.sort(as[1][m]);
				IndexQuickSort.reverse(s);
			}

			bw.append(Integer.toString(as[0][m].length));
			for (int i = 0; i < as[0][m].length; i++) {
				bw.append(" ").append(Integer.toString(as[0][m][s[i]]))
						.append(":").append(Integer.toString(as[1][m][s[i]]));
			}
			bw.append('\n');
		}
		bw.close();
	}

	/**
	 * read array from file in svmlight format
	 * 
	 * @param filename
	 * @return sparse array
	 * @throws IOException
	 */
	public static int[][][] read(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		List<int[]> x = new ArrayList<int[]>();
		List<int[]> w = new ArrayList<int[]>();
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] ss = line.split("\\s+");
			int num = Integer.parseInt(ss[0].trim());
			int[] xm = new int[num];
			int[] wm = new int[num];
			x.add(xm);
			w.add(wm);
			for (int i = 0; i < num; i++) {
				String[] xw = ss[i + 1].split("\\:");
				xm[i] = Integer.parseInt(xw[0]);
				wm[i] = Integer.parseInt(xw[1]);
			}
		}
		br.close();
		int[][][] xs = new int[2][][];
		xs[0] = (int[][]) x.toArray(new int[0][0]);
		xs[1] = (int[][]) w.toArray(new int[0][0]);
		return xs;
	}

	/**
	 * 
	 * @param as
	 * @param sort 0 no, 1 by index, 2 by frequency, negative = reverse
	 */
	public static void print(int[][][] as, int sort) {
		int asort = sort > 0 ? sort : -sort;
		for (int m = 0; m < as[0].length; m++) {

			if (as[1][m] == null) {
				System.out.println("0\n");
				continue;
			}

			int[] s = null;
			// sort by index/frequency
			if (as[1][m].length > 0) {
				if (asort == 1 || asort <= 2) {
					s = IndexQuickSort.sort(as[asort - 1][m]);
					if (sort < 0) {
						IndexQuickSort.reverse(s);
					}
				} else {
					s = Vectors.range(0, as[1][m].length);
				}
			}

			System.out.print(as[0][m].length);
			for (int i = 0; i < as[0][m].length; i++) {
				System.out.print(" " + as[0][m][s[i]] + ":" + as[1][m][s[i]]);
			}
			System.out.print("\n");
		}
	}

	/**
	 * threshold the matrix
	 * 
	 * @param xs
	 * @param bound
	 */
	public static void threshold(int[][][] xs, int bound) {
		for (int i = 0; i < xs[0].length; i++) {
			List<Integer> rx = new ArrayList<Integer>();
			List<Integer> rw = new ArrayList<Integer>();
			for (int j = 0; j < xs[0][i].length; j++) {
				if (xs[1][i][j] >= bound) {
					rx.add(xs[0][i][j]);
					rw.add(xs[1][i][j]);
				}
			}
			// replace row
			xs[0][i] = (int[]) ArrayUtils.asPrimitiveArray(rx);
			xs[1][i] = (int[]) ArrayUtils.asPrimitiveArray(rw);
			if (xs[0][i] == null) {
				xs[0][i] = new int[0];
				xs[1][i] = new int[0];
			}
		}
	}
}
