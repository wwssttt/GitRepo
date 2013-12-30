/*
 * Created on 04.08.2008
 */
package org.knowceans.util;

public class Binomial {

    /**
     * pascal triangle representation of binomial coefficients, omitting outer
     * values (k|0) = 1 and (k|1) = k, and assuming symmetry (k|n) = (k|k-n).
     */
    static final int pascal[] = {6, 10, 15, 20, 21, 35, 28, 56, 70, 36, 84,
        126, // 11
        45, 120, 210, 252, 55, 165, 330, 462, 66, 220, 495, 792, 924, 78, 286, // 26
        715, 1287, 1716, 91, 364, 1001, 2002, 3003, 3432, 105, 455, 1365, 3003, // 39
        5005, 6435, 120, 560, 1820, 4368, 8008, 11440, 12870};

	/**
	 * indices into pascal to retrieve coefficients. For n>=4 obeys
	 * pascalrows = (a + (n % 2)) * (a + 1), a = (int) ((n-4)/2)
	 */
    static final int pascalrows[] = {0, 1, 2, 4, 6, 9, 12, 16, 20, 25, 30, 36,
        42};

    /**
     * binomial coefficient n choose k.
     */
    static long binom(int n, int k) {
        // symmetry
        if (n - k < k) {
            k = n - k;
        }
        // trivial values
        if (n == 0 || k == 0) {
            return 1;
        }
        if (k == 1) {
            return n;
        }
        // k >= 2
        if (n < 16) {
            return pascal[pascalrows[n - 4] + k - 2];
        }
        // all other cases
        return (long) Math.exp(lgamma(n + 1) - lgamma(k + 1) - lgamma(n - k));

    }

    static double lgamma(double x) {
        double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
        double ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1)
            + 24.01409822 / (x + 2) - 1.231739516 / (x + 3) + 0.00120858003
            / (x + 4) - 0.00000536382 / (x + 5);
        return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
    }

    public static void main(String[] args) {
        for (int n = 0; n < 20; n++) {
            for (int k = 0; k <= n; k++) {
                System.out.println("(" + n + "|" + k + ") = " + binom(n, k));
            }
        }
    }
}
