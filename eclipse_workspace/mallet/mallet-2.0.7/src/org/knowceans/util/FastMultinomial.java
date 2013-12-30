/*
 * Created on Sep 23, 2009
 */
package org.knowceans.util;

import java.util.Random;

/**
 * FastMultinomial implements a fast multinomial sampler that exploits
 * heavy-tail distribution of the probability masses. It assumes that the
 * weights are based on a product term and uses a series of bounds on the
 * normalisation constant of the sampling distribution that allows to calculate
 * the terms iteratively. The algorithm was described for a fast LDA Gibbs
 * sampler in Porteous, Newman and Welling (KDD 2008) and is here used more
 * generically.
 * <p>
 * The core data structure is the weights array abc that contains a multinomial
 * factor distribution in each of its rows. Operation requires to keep an index
 * of sorted elements of abc, for which the dominant element should be chosen
 * with a call to indexsort(abc[dominantindex]). Further, a norm of all of the
 * rows of abc should be calculated using sumcube(abc[i]), not including the
 * root operation. Finally, samples can be taken from the distribution, updating
 * the state of the sampler in all three parts, abc, the norm and the sort
 * index. There are two sampling methods, one that returns the sample as an
 * index of the sorting index array (useful to maintain the sampling order using
 * reorder()) and one that returns the actual sample index in abc.
 * <p>
 * The static functions are proof of concept that does not save much computing
 * time because they don't pre-compute the weights beforehand. Actual speed is
 * gained by subclassing and calculating the weights in getWeights.
 * 
 * @author gregor
 */
@SuppressWarnings("hiding")
public abstract class FastMultinomial {

    public static void main(String[] args) {
        int nsamp = 100000;
        //        double[] a = new double[] {0.4, 0.1, 0.001, 0.01, 0.02, 0.003, 0.15,
        //            0.003, 0.2, 0.1};
        int K = 100;
        Cokus.seed((int) System.currentTimeMillis());
        double[] a = Samplers.randDir(0.001, K);
        double[] b = Samplers.randDir(3, K);
        double[] c = Samplers.randDir(3, K);
        double[][] ww = new double[][] {a, b, c};

        int[][] samples = staticMain(nsamp, ww);

        System.out.println(Vectors.print(idx));

        // check empirical distribution
        for (int i = 0; i < samples.length; i++) {
            System.out.println("***********");
            System.out.println(i);
            Histogram.hist(System.out, samples[i], 10);
        }
        long fast = StopWatch.read("fast");
        long baseline = StopWatch.read("baseline");

        System.out.println("fast: " + StopWatch.format(fast));
        System.out.println("baseline: " + StopWatch.format(baseline));
        System.out.println(String.format(
            "fast vs. baseline: %2.1f%% more time", (fast - baseline)
                / (double) baseline * 100));

        double dist = 0;
        for (int i = 0; i < samples[0].length; i++) {
            dist += samples[0][i] * Math.log(samples[0][i] / (samples[1][i] + 1e-12));
        }
        System.out.println(String.format("KL divergence = %2.5f", dist / nsamp));

    }

    /**
     * static operation of the sampler
     * 
     * @param nsamp
     * @param ww
     * @return
     */
    private static int[][] staticMain(int nsamp, double[][] ww) {
        // order weights
        idx = new int[2][];
        idx[0] = Vectors.range(0, ww[0].length - 1);
        idx[1] = Vectors.range(0, ww[0].length - 1);
        indexsort(ww[0], idx);
        // set up norms
        double[] wwnorm = new double[ww.length];
        for (int i = 0; i < wwnorm.length; i++) {
            wwnorm[i] = sumcube(ww[i]);
        }

        // now sample
        int[][] samples = new int[2][nsamp];

        Random rand = new CokusRandom();
        StopWatch.start("fast");
        for (int i = 0; i < nsamp; i++) {
            samples[0][i] = idx[0][sampleIdx(ww, wwnorm, idx[0], rand)];
        }
        StopWatch.stop("fast");
        rand = new CokusRandom();
        StopWatch.start("baseline");
        for (int i = 0; i < nsamp; i++) {
            samples[1][i] = sample0(ww, rand);
        }
        StopWatch.stop("baseline");
        return samples;

    }

    ////////////// instance members and functions /////////////

    /**
     * number of dimensions
     */
    private int K;

    /**
     * number of factors
     */
    private int I;

    /**
     * power of the norm of abc
     */
    private double[] pownorm;

    /**
     * rng
     */
    private Random rand;

    /**
     * sorting index into abc to the norm
     */
    private static int[][] idx;

    /**
     * set up a fast multinomial using a subclass implementation for the
     * 
     * @param I number of factor distributions
     * @param K number of topics
     * @param pownorm cube norms of factors [I]
     * @param idx descending order of weight factors
     * @param rand random sampler
     */
    public FastMultinomial(int I, int K, Random rand) {
        this.rand = rand;
        this.I = I;
        this.K = K;
    }

    /**
     * Fast sampling from a distribution with weights in factors abc, each
     * element of which is a vector of the size of the sampling space K.
     * Further, the cube norm of each of the factors is given in pownorm
     * (without the inverse exponential operation), as well as the indexes of
     * the values of abc ordered decending (by one of the factors).
     * 
     * @param I number of factor distributions
     * @param K number of topics
     * @param pownorm cube norms of factors [I]
     * @param idx descending order of weight factors
     * @param rand random sampler
     * @return a sample as index of idx, use sample() to get the original index
     *         of abc
     */
    public int sampleIdx() {
        double[] weights = new double[I];
        int korig;
        double Zlprev;
        double Zl = Double.POSITIVE_INFINITY;
        double[] pcum = new double[K];
        // see static version for implementation info
        double[] remainNorms = Vectors.copy(pownorm);
        double u = rand.nextDouble();
        for (int l = 0, lprev = -1; l < K; l++, lprev++) {
            pcum[l] = l == 0 ? 0 : pcum[lprev];
            korig = idx[0][l];
            getWeights(korig, weights);
            double pl = weights[0];
            for (int i = 1; i < I; i++)
                pl *= weights[i];
            pcum[l] += pl;
            Zlprev = Zl;
            Zl = pcum[l] + reducenorm(remainNorms, weights);
            if (u <= pcum[l] / Zl)
                if (l > 0 && u <= pcum[lprev] / Zl) {
                    u = (u * Zlprev - pcum[lprev]) * Zl / (Zlprev - Zl);
                    for (int k = 0; k < l; k++)
                        if (u <= pcum[k])
                            return k;
                } else
                    return l;
        }
        return -1;
    }

    /**
     * This function is supposed to do two things: remove the current element
     * abc from the exponentiated norm and calculate the norm of the remaining
     * elements by the root. This function is implemented using the cube root
     * and should be subclassed for other norms.
     * 
     * @param abcl2kExpnorm [in/out] exponentiated norms from l to K - 1 in each
     *        row
     * @param abcl1 [in] weights to be reduced = abc[][l-1]
     * @return
     */
    public double reducenorm(double[] abcl2kExpnorm, double[] abcl1) {
        double abcl2knorm = 1;
        for (int i = 0; i < I; i++) {
            abcl2kExpnorm[i] -= cube(abcl1[i]);
            if (abcl2kExpnorm[i] < 0) {
                abcl2kExpnorm[i] = 0;
            }
            abcl2knorm *= abcl2kExpnorm[i];
        }
        return cuberoot(abcl2knorm);
    }

    /**
     * Calculate exponentiated norm sum of x for the weights arr. This
     * implementation uses cube norm. Typically, this function is called for
     * each factor once before fast sampling can start. It requires that weights
     * for all k are calculated initially, whereas for the fast sampler this is
     * not necessary.
     * 
     * @param abci factor
     * @return norm (exp)
     */
    public double initnorm(double[] abci) {
        double sum = 0;
        for (int k = 0; k < abci.length; k++) {
            sum += cube(abci[k]);
        }
        return sum;
    }

    /**
     * Get the weight for dimension k for each factor. This is subclassed and
     * contains the actual weights calculations whose number should be reduced
     * by the sorted sampling.
     * 
     * @param k
     * @param weights get the weights
     * @return
     */
    public abstract void getWeights(int k, double[] weights);

    /////////// static functions ///////////

    /**
     * Fast sampling from a distribution with weights in factors abc, each
     * element of which is a vector of the size of the sampling space K.
     * Further, the cube norm of each of the factors is given in pownorm
     * (without the inverse exponential operation), as well as the indexes of
     * the values of abc ordered decending (by one of the factors).
     * 
     * @param abc weight factors of multinomial masses [I x K]
     * @param pownorm cube norms of factors [I]
     * @param idx descending order of weight factors
     * @param rand random sampler
     * @return a sample as index of abc
     */
    public static int sample(double[][] abc, double[] abcnorm, int[] idx,
        Random rand) {
        return idx[sampleIdx(abc, abcnorm, idx, rand)];
    }

    public static int sample2(double[][] abc, double[] abcnorm, int[] idx,
        Random rand) {
        return idx[sampleIdx2(abc, abcnorm, idx, rand)];
    }

    /**
     * Standard sampling from a distribution with weights in factors abc, each
     * element of which is a vector of the size of the sampling space K.
     * 
     * @param weights weight factors of multinomial masses [I x K]
     * @param rand random sampler
     * @return a sample as index of weights
     */
    public static int sample0(double[][] weights, Random rand) {
        int I = weights.length;
        int K = weights[0].length;
        double[] pcum = new double[K];

        double u = rand.nextDouble();
        // for all partitions s_l^k
        for (int k = 0; k < K; k++) {
            double pk = weights[0][k];
            for (int i = 1; i < I; i++) {
                pk *= weights[i][k];
            }
            // get the known weights so far
            // pcumk = pcumk + prod_i abc_ik
            pcum[k] = (k == 0) ? pk : pcum[k - 1] + pk;
        }
        // perform binary search
        int k = Samplers.binarySearch(pcum, u * pcum[K - 1]);
        return k;
    }

    /**
     * Standard sampling from a distribution with weights in factors abc, each
     * element of which is a vector of the size of the sampling space K.
     * 
     * @param weights weight factors of multinomial masses [I x K]
     * @param rand random sampler
     * @return a sample as index of weights
     */
    public static int sample00(double[][] weights, Random rand) {
        int I = weights.length;
        int K = weights[0].length;
        double[] pcum = new double[K];

        // for all partitions s_l^k
        for (int k = 0; k < K; k++) {
            double pk = weights[0][k];
            for (int i = 1; i < I; i++) {
                pk *= weights[i][k];
            }
            // get the known weights so far
            // pcumk = pcumk + prod_i abc_ik
            pcum[k] = (k == 0) ? pk : pcum[k - 1] + pk;
        }
        // perform linear search
        double u = rand.nextDouble() * pcum[K - 1];
        for (int k = 0; k < K; k++) {
            if (u < pcum[k]) {
                return k;
            }
        }
        return -1;
    }

    /**
     * Fast sampling from a distribution with weights in factors abc, each
     * element of which is a vector of the size of the sampling space K.
     * Further, the cube norm of each of the factors is given in pownorm
     * (without the inverse exponential operation), as well as the indexes of
     * the values of abc ordered decending (by one of the factors).
     * 
     * @param weights weight factors of multinomial masses [I x K]
     * @param pownorm cube norms of factors [I]
     * @param idx indices of weights in descending order of weight values
     * @param rand random sampler
     * @return a sample as index of idx, use sample() to get the original index
     *         of abc
     */
    public static int sampleIdx(double[][] weights, double[] abcnorm,
        int[] idx, Random rand) {
        int I = weights.length;
        int K = weights[0].length;
        int korig;
        double Zlprev, pcumprev;
        double Zl = Double.POSITIVE_INFINITY;
        double[] pcum = new double[K];

        // get norms that are decremented
        // by local elements:
        // norm(a_l:K-1) starting with (a_0:K-1)
        double[] remainNorms = new double[I];
        for (int i = 0; i < I; i++) {
            remainNorms[i] = abcnorm[i];
        }

        double u = rand.nextDouble();
        for (int l = 0; l < K; l++) {

            // orig. topic for this partition                    
            korig = idx[l];
            //int lprev = l - 1;

            double pl = weights[0][korig];
            for (int i = 1; i < I; i++) {
                pl *= weights[i][korig];
            }
            // cumulate the known weights so far
            // pcumk = pcumk + prod_i abc_ik
            if (l == 0) {
                pcumprev = 0;
                pcum[l] = pl;
            } else {
                pcumprev = pcum[l - 1];
                pcum[l] = pcumprev + pl;
            }

            // add the estimate of the unknown weights
            double remainNorm = 1;
            for (int i = 0; i < I; i++) {
                // weight korig now known -> remove it
                remainNorms[i] -= cube(weights[i][korig]);
                if (remainNorms[i] < 0) {
                    remainNorms[i] = 0;
                }
                remainNorm *= remainNorms[i];
            }

            // store Zl to calculate Zl diff
            Zlprev = Zl;
            // Zl = (sum_k=1:l pk) + prod_i norm(a_i,l+1:K)
            Zl = pcum[l] + cuberoot(remainNorm);

            // if below s_l^l = p_l / Z_l (if s_l^k, k <= l)
            if (u <= pcum[l] / Zl) {
                // if it's an s_l^k below the "main segment" s_l^l (if k < l)
                // NOTE: s_l^l between pcum[lprev]/Zl and pcum[l]/Zl,
                // s_l^k are between pcum[lprev]/Zlprev and pcum[lprev]/Zl
                if (l > 0 && u <= pcumprev / Zl) {
                    // remove all segments up to s_lprev^lprev
                    // and scale with s_l^k / p_k = (1/Zl - 1/Zlprev)
                    u = (u * Zlprev - pcumprev) * Zl / (Zlprev - Zl);
                    // for each s_l^k
                    for (int k = 0; k < l; k++) {
                        if (u <= pcum[k]) {
                            return k;
                        }
                    } // for each s_l^k
                } else {
                    // it's "main segment" s_l^l
                    return l;
                }
            } // if p too low
        } // for k
        // should never reach this...
        System.out.println("ERROR: sample -1");
        return -1;
    }

    /**
     * Fast sampling from a distribution with weights in factors abc, each
     * element of which is a vector of the size of the sampling space K.
     * Further, the cube norm of each of the factors is given in pownorm
     * (without the inverse exponential operation), as well as the indexes of
     * the values of abc ordered decending (by one of the factors).
     * 
     * @param weights weight factors of multinomial masses [I x K]
     * @param pownorm cube norms of factors [I]
     * @param idx descending order of weight factors
     * @param rand random sampler
     * @return a sample as index of idx, use sample() to get the original index
     *         of abc
     */
    public static int sampleIdx2(double[][] weights, double[] abcnorm,
        int[] idx, Random rand) {
        int I = weights.length;
        int K = weights[0].length;
        int korig;
        double Zlprev, Zl = Double.POSITIVE_INFINITY;
        double[] p = new double[K];

        // get norms that are decremented
        // by local elements:
        // norm(a_l:K-1) starting with (a_0:K-1)
        double[] remainNorms = new double[I];
        for (int i = 0; i < I; i++) {
            remainNorms[i] = abcnorm[i];
        }

        double Zldiff = 0;
        Zlprev = Double.POSITIVE_INFINITY;
        double slk = 0;
        double slkcum = 0;
        double pkcum = 0;

        double u = rand.nextDouble();
        // for all partitions s_l^k
        for (int l = 0; l < K; l++) {
            // for all partitions s_l^k with k < l
            for (int k = l; k >= 0; k--) {
                if (k == l) {
                    // get the original topic for this partition                    
                    korig = idx[l];

                    double pk = weights[0][korig];
                    for (int i = 1; i < I; i++) {
                        pk *= weights[i][korig];
                    }
                    // get the known weights so far
                    // pcumk = pcumk + prod_i abc_ik

                    pkcum += pk;
                    p[k] = pk;

                    // add the estimate of the unknown weights
                    double remainNorm = 1;
                    for (int i = 0; i < I; i++) {
                        // weight korig now known -> remove it
                        remainNorms[i] -= cube(weights[i][korig]);
                        if (remainNorms[i] < 0) {
                            remainNorms[i] = 0;
                        }
                        remainNorm *= remainNorms[i];
                    }
                    Zlprev = Zl;
                    // Zl = (sum_k=1:l pk) + prod_i norm(a_i,l+1:K)
                    Zl = pkcum + cuberoot(remainNorm);

                    // get current partition
                    // s_l^l = p_l / Z_l
                    slk = p[k] / Zl;
                } else {
                    // get current partition
                    // TODO: this could be pre-calculated using shifted/scaled u
                    // s_l^k = p_k ( 1/Z_l - 1/Z_lprev )
                    slk = p[k] * Zldiff;
                }
                // total length of partitions so far
                slkcum += slk;
                if (u <= slkcum) {
                    //System.out.println(l-k);
                    //histLK[l - k]++;
                    //histK[k]++;
                    // if sample in current s_l^k
                    return k;
                }
                if (l == k) {
                    Zldiff = 1 / Zl - 1 / Zlprev;
                }
            } // for k
        } // for l
        // should never reach this...
        return -1;
    }

    /**
     * sort idx according to reverse order of elements of x, leaving x
     * untouched.
     * 
     * @param x [in] unsorted array
     * @param idx [in/out] index array reordered so x[idx[0..K-1]] has
     *        descending order.
     */
    public static void indexsort(double[] x, int[][] idx) {
        IndexQuickSort.sort(x, idx[0]);
        IndexQuickSort.reverse(idx[0]);
        IndexQuickSort.inverse(idx[0], idx[1]);
    }

    /**
     * reorder the index of a sorted array after element kinc had been
     * incremented and kdec decremented (referring to the indices in idx). This
     * is a minimal form of quicksort.
     * 
     * @param x weights array
     * @param idx indices from x to idx
     * @param kinc element in idx just incremented
     * @param kdec element in idx just decremented
     */
    public static void reorder(double[] x, int[] idx, int kinc, int kdec) {

        while (kinc > 0 && x[idx[kinc]] > x[idx[kinc - 1]]) {
            IndexQuickSort.swap(idx, kinc, kinc - 1);
            kinc--;
        }

        while (kdec < x.length - 1 && x[idx[kdec]] < x[idx[kdec + 1]]) {
            IndexQuickSort.swap(idx, kdec, kdec + 1);
            kdec++;
        }
    }

    /**
     * cube of the argument
     * 
     * @param x
     * @return
     */
    public static double cube(double x) {
        return x * x * x;
    }

    /**
     * cube root of argument
     * 
     * @param x
     * @return
     */
    public static double cuberoot(double x) {
        return Math.pow(x, 0.33333333333);
    }

    /**
     * calculate sum of cubes of argument
     * 
     * @param x
     * @return sum x^3
     */
    public static double sumcube(double[] x) {
        int i;
        double sum = 0;
        for (i = 0; i < x.length; i++) {
            sum += cube(x[i]);
        }
        return sum;
    }

}
