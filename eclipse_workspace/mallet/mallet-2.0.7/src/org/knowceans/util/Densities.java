/*
 * Created on Jul 18, 2005
 */
/*
 * Copyright (c) 2005-2006 Gregor Heinrich. All rights reserved. Redistribution and
 * use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. 2. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knowceans.util;

/**
 * Densities calculates for different density functions the likelihood of a data
 * item given the parameters
 *
 * @author heinrich
 */
public class Densities {

    /**
     * Normal likelihood
     *
     * @param x
     * @param mu
     * @param sigma
     * @return
     */
    public static double pdfNorm(double x, double mu, double sigma) {
        double p = 1 / (Math.sqrt(2 * Math.PI) * sigma)
            / Math.exp((x - mu) * (x - mu) / (2 * sigma * sigma));
        return p;
    }

    /**
     * GMM likelihood
     *
     * @param x
     * @param k
     * @param probs
     * @param mean
     * @param sigma
     * @return
     */
    public static double pdfGmm(double x, double[] probs, double[] mean,
        double[] sigma) {
        double p = 0;
        for (int i = 0; i < probs.length; i++) {
            p += probs[i] * pdfNorm(x, mean[i], sigma[i]);
        }
        return p;
    }

    /**
     * Dirichlet mixture likelihood using mean and precision
     *
     * @param x
     * @param probs
     * @param basemeasure -- multinomial parameter around which the distribution
     *        scatters, m = alpha / s
     * @param precision -- s = sum alpha
     * @return
     */
    public static double pdfDmm(double x[], double[] probs,
        double[][] basemeasure, double[] precision) {
        double p = 0;
        for (int k = 0; k < probs.length; k++) {
            p += probs[k] * pdfDirichlet(x, basemeasure[k], precision[k]);
        }
        return p;
    }

    /**
     * Dirichlet mixture likelihood using Dirichlet parameters and assuming
     * independence between components.
     *
     * @param x
     * @param k
     * @param probs
     * @param mean
     * @param precision
     * @return
     */
    public static double pdfDmm(double xx[], double[] probs,
        double[][] parameters) {
        double p = 0;
        for (int i = 0; i < probs.length; i++) {
            p += probs[i] * pdfDirichlet(xx, parameters[i]);
        }
        return p;
    }

    /**
     * gamma likelihood p(x|a,b) = x^(a-1) * e^(-x/b) / (b^a * Gamma(a))
     *
     * @param x value
     * @param a (shape?)
     * @param b (scale?)
     * @return
     */
    public static double pdfGamma(double x, double a, double b) {
        return Math.pow(x, a - 1) * Math.exp(-x / b)
            / (Math.pow(b, a) * Math.exp(Gamma.lgamma(a)));
    }

    /**
     * beta likelihood
     *
     * @param x data item
     * @param a pseudo counts for success
     * @param b pseudo counts for failure
     * @return
     */
    public static double pdfBeta(double x, double a, double b) {
        return pdfDirichlet(new double[] {x, 1 - x}, new double[] {a, b});
    }

    /**
     * Dirichlet likelihood using logarithmic calculation
     * <p>
     * Dir(xx|alpha) =
     * <p>
     * Gamma(sum_i alpha[i])/(prod_i Gamma(alpha[i])) prod_i xx[i]^(alpha[i]-1)
     *
     * @param xx multivariate convex data item (sum=1)
     * @param alpha Dirichlet parameter vector
     * @return
     */
    public static double pdfDirichlet(double[] xx, double[] alpha) {
        double sumAlpha = 0.;
        double sumLgammaAlpha = 0.;
        double logLik = 0.;
        for (int i = 0; i < xx.length; i++) {
            sumAlpha += alpha[i];
            sumLgammaAlpha += Gamma.lgamma(alpha[i]);
            logLik += Math.log(xx[i]) * (alpha[i] - 1);
        }
        return Math.exp(Gamma.lgamma(sumAlpha) - sumLgammaAlpha + logLik);
    }

    /**
     * Dirichlet likelihood using logarithmic calculation
     * <p>
     * Dir(xx|alpha) =
     * <p>
     * Gamma(sum_i alpha[i])/(prod_i Gamma(alpha[i])) prod_i xx[i]^(alpha[i]-1)
     *
     * @param xx multivariate convex data item (sum=1)
     * @param basemeasure -- normalised coefficients (= mean)
     * @param precision -- central moment ...
     * @return
     */
    public static double pdfDirichlet(double[] xx, double[] basemeasure,
        double precision) {
        double sumAlpha = 0.;
        double sumLgammaAlpha = 0.;
        double logLik = 0.;
        for (int i = 0; i < xx.length; i++) {
            sumAlpha += basemeasure[i] * precision;
            sumLgammaAlpha += Gamma.lgamma(basemeasure[i] * precision);
            logLik += Math.log(xx[i]) * (basemeasure[i] * precision - 1);
        }
        return Math.exp(Gamma.lgamma(sumAlpha) - sumLgammaAlpha + logLik);
    }

    /**
     * Symmetric Dirichlet likelihood:
     * <p>
     * Dir(xx|alpha) = Gamma(k * alpha)/Gamma(alpha)^k prod_i xx[i]^(alpha - 1)
     *
     * @param xx multivariate convex data item (sum=1)
     * @param alpha symmetric parameter
     * @return
     */
    public static double pdfDirichlet(double[] xx, double alpha) {
        double logCoeff = Gamma.lgamma(alpha * xx.length) - Gamma.lgamma(alpha)
            * xx.length;
        double logLik = 0.;
        for (int i = 0; i < xx.length; i++) {
            logLik += Math.log(xx[i]);
        }
        logLik *= alpha - 1;
        return Math.exp(logCoeff + logLik);
    }

    /**
     * Mult(nn|pp) using logarithmic multinomial coefficient
     *
     * @param nn counts for each category
     * @param pp convex probability vector for categories
     * @return
     */
    public static double pdfMultinomial(int[] nn, double[] pp) {
        int N = 0;
        double logCoeff = 0.;
        double logLik = 0.;
        for (int i = 0; i < nn.length; i++) {
            N += nn[i];
            logCoeff -= Gamma.lgamma(nn[i] + 1);
            logLik += Math.log(pp[i]) * nn[i];
        }
        logCoeff += Gamma.lgamma(N + 1);
        return Math.exp(logCoeff + logLik);
    }

    /**
     * Binom(n | N, p) using linear binomial coefficient
     *
     * @param n
     * @param N
     * @param p
     * @return
     */
    public static double pdfBinomial(int n, int N, double p) {
        long binom = Gamma.factorial(N)
            / (Gamma.factorial(N - n) * Gamma.factorial(n));
        double lik = binom * Math.pow(p, N) * Math.pow(1 - p, N - n);
        return lik;
    }


}
