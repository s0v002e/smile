/*******************************************************************************
 * Copyright (c) 2010-2020 Haifeng Li. All rights reserved.
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/

package smile.regression;

import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import smile.clustering.KMeans;
import smile.data.*;
import smile.math.MathEx;
import smile.math.kernel.GaussianKernel;
import smile.math.matrix.Matrix;
import smile.validation.CrossValidation;
import smile.validation.LOOCV;
import smile.validation.metric.RMSE;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Haifeng Li
 */
public class GaussianProcessRegressionTest {
    public GaussianProcessRegressionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test(expected = Test.None.class)
    public void testLongley() throws Exception {
        System.out.println("longley");

        MathEx.setSeed(19650218); // to get repeatable results.

        double[][] longley = MathEx.clone(Longley.x);
        MathEx.standardize(longley);

        double[] prediction = LOOCV.regression(longley, Longley.y, (xi, yi) -> GaussianProcessRegression.fit(xi, yi, new GaussianKernel(8.0), 0.2));
        double rmse = RMSE.of(Longley.y, prediction);

        System.out.println("RMSE = " + rmse);
        assertEquals(2.749193150193674, rmse, 1E-4);

        GaussianProcessRegression<double[]> model = GaussianProcessRegression.fit(longley, Longley.y, new GaussianKernel(8.0), 0.2);
        System.out.println(model);

        GaussianProcessRegression<double[]>.JointPrediction joint = model.eval(Arrays.copyOf(longley, 10));
        System.out.println(joint);

        int n = joint.mu.length;
        double[] musd = new double[2];
        for (int i = 0; i < n; i++) {
            model.predict(longley[i], musd);
            assertEquals(musd[0], joint.mu[i], 1E-7);
            assertEquals(musd[1], joint.sd[i], 1E-7);
        }

        double[][] samples = joint.sample(500);
        System.out.format("samples = %s\n", new Matrix(samples).toString());
        System.out.format("sample cov = %s\n", new Matrix(MathEx.cov(samples)).toString(true));

        java.nio.file.Path temp = smile.data.Serialize.write(model);
        smile.data.Serialize.read(temp);
    }

    @Test
    public void testCPU() {
        System.out.println("CPU");

        MathEx.setSeed(19650218); // to get repeatable results.

        double[][] x = MathEx.clone(CPU.x);
        MathEx.standardize(x);
        CrossValidation cv = new CrossValidation(x.length, 10);

        double[] prediction = cv.regression(x, CPU.y, (xi, yi) -> GaussianProcessRegression.fit(xi, yi, new GaussianKernel(47.02), 0.1));

        double[] sparsePrediction = cv.regression(10, x, CPU.y, (xi, yi) -> {
            KMeans kmeans = KMeans.fit(xi, 30);
            double[][] centers = kmeans.centroids;
            double r0 = 0.0;
            for (int l = 0; l < centers.length; l++) {
                for (int j = 0; j < l; j++) {
                    r0 += MathEx.distance(centers[l], centers[j]);
                }
            }
            r0 /= (2 * centers.length);
            System.out.println("Kernel width = " + r0);
            return GaussianProcessRegression.fit(xi, yi, centers, new GaussianKernel(r0), 0.1);
        });

        double[] nystromPrediction = cv.regression(x, CPU.y, (xi, yi) -> {
            KMeans kmeans = KMeans.fit(xi, 30);
            double[][] centers = kmeans.centroids;
            double r0 = 0.0;
            for (int l = 0; l < centers.length; l++) {
                for (int j = 0; j < l; j++) {
                    r0 += MathEx.distance(centers[l], centers[j]);
                }
            }
            r0 /= (2 * centers.length);
            System.out.println("Kernel width = " + r0);
            return GaussianProcessRegression.nystrom(xi, yi, centers, new GaussianKernel(r0), 0.1);
        });

        double rmse = RMSE.of(CPU.y, prediction);
        double sparseRMSE = RMSE.of(CPU.y, sparsePrediction);
        double nystromRMSE = RMSE.of(CPU.y, nystromPrediction);

        System.out.println("Regular 10-CV RMSE = " + rmse);
        System.out.println("Sparse 10-CV RMSE = " + sparseRMSE);
        System.out.println("Nystrom 10-CV RMSE = " + nystromRMSE);
        assertEquals(76.20610792727746, rmse, 1E-4);
        assertEquals(68.45870366386313, sparseRMSE, 1E-4);
        assertEquals(65.74555877623769, nystromRMSE, 1E-4);
    }

    @Test(expected = Test.None.class)
    public void test2DPlanes() throws Exception {
        System.out.println("2dplanes");

        MathEx.setSeed(19650218); // to get repeatable results.

        double[][] x = MathEx.clone(Planes.x);
        double[] y = Planes.y;

        int[] permutation = MathEx.permutate(x.length);
        double[][] datax = new double[4000][];
        double[] datay = new double[datax.length];
        for (int i = 0; i < datax.length; i++) {
            datax[i] = x[permutation[i]];
            datay[i] = y[permutation[i]];
        }

        CrossValidation cv = new CrossValidation(datax.length, 10);

        double[] prediction = cv.regression(datax, datay, (xi, yi) -> GaussianProcessRegression.fit(xi, yi, new GaussianKernel(34.866), 0.1));

        double[] sparsePrediction = cv.regression(10, datax, datay, (xi, yi) -> {
            KMeans kmeans = KMeans.fit(xi, 30);
            double[][] centers = kmeans.centroids;
            double r0 = 0.0;
            for (int l = 0; l < centers.length; l++) {
                for (int j = 0; j < l; j++) {
                    r0 += MathEx.distance(centers[l], centers[j]);
                }
            }
            r0 /= (2 * centers.length);
            System.out.println("Kernel width = " + r0);
            return GaussianProcessRegression.fit(xi, yi, centers, new GaussianKernel(r0), 0.1);
        });

        double[] nystromPrediction = cv.regression(datax, datay, (xi, yi) -> {
            KMeans kmeans = KMeans.fit(xi, 30);
            double[][] centers = kmeans.centroids;
            double r0 = 0.0;
            for (int l = 0; l < centers.length; l++) {
                for (int j = 0; j < l; j++) {
                    r0 += MathEx.distance(centers[l], centers[j]);
                }
            }
            r0 /= (2 * centers.length);
            System.out.println("Kernel width = " + r0);
            return GaussianProcessRegression.nystrom(xi, yi, centers, new GaussianKernel(r0), 0.1);
        });

        double rmse = RMSE.of(datay, prediction);
        double sparseRMSE = RMSE.of(datay, sparsePrediction);
        double nystromRMSE = RMSE.of(datay, nystromPrediction);

        System.out.println("Regular 10-CV RMSE = " + rmse);
        System.out.println("Sparse 10-CV RMSE = " + sparseRMSE);
        System.out.println("Nystrom 10-CV RMSE = " + nystromRMSE);
        assertEquals(2.398555967342820, rmse, 1E-4);
        assertEquals(2.176467597770095, sparseRMSE, 1E-4);
        assertEquals(2.109326738693354, nystromRMSE, 1E-4);
    }

    @Test(expected = Test.None.class)
    public void testAilerons() throws Exception {
        System.out.println("ailerons");

        MathEx.setSeed(19650218); // to get repeatable results.

        double[][] x = MathEx.clone(Ailerons.x);
        MathEx.standardize(x);
        double[] y = Ailerons.y.clone();
        for (int i = 0; i < y.length; i++) {
            y[i] *= 10000;
        }

        int[] permutation = MathEx.permutate(x.length);
        double[][] datax = new double[4000][];
        double[] datay = new double[datax.length];
        for (int i = 0; i < datax.length; i++) {
            datax[i] = x[permutation[i]];
            datay[i] = y[permutation[i]];
        }

        CrossValidation cv = new CrossValidation(datax.length, 10);

        double[] prediction = cv.regression(datax, datay, (xi, yi) -> GaussianProcessRegression.fit(xi, yi, new GaussianKernel(183.96), 0.1));

        double[] sparsePrediction = cv.regression(10, datax, datay, (xi, yi) -> {
            KMeans kmeans = KMeans.fit(xi, 30);
            double[][] centers = kmeans.centroids;
            double r0 = 0.0;
            for (int l = 0; l < centers.length; l++) {
                for (int j = 0; j < l; j++) {
                    r0 += MathEx.distance(centers[l], centers[j]);
                }
            }
            r0 /= (2 * centers.length);
            System.out.println("Kernel width = " + r0);
            return GaussianProcessRegression.fit(xi, yi, centers, new GaussianKernel(r0), 0.1);
        });

        double[] nystromPrediction = cv.regression(datax, datay, (xi, yi) -> {
            KMeans kmeans = KMeans.fit(xi, 30);
            double[][] centers = kmeans.centroids;
            double r0 = 0.0;
            for (int l = 0; l < centers.length; l++) {
                for (int j = 0; j < l; j++) {
                    r0 += MathEx.distance(centers[l], centers[j]);
                }
            }
            r0 /= (2 * centers.length);
            System.out.println("Kernel width = " + r0);
            return GaussianProcessRegression.nystrom(xi, yi, centers, new GaussianKernel(r0), 0.1);
        });

        double rmse = RMSE.of(datay, prediction);
        double sparseRMSE = RMSE.of(datay, sparsePrediction);
        double nystromRMSE = RMSE.of(datay, nystromPrediction);

        System.out.println("Regular 10-CV RMSE = " + rmse);
        System.out.println("Sparse 10-CV RMSE = " + sparseRMSE);
        System.out.println("Nystrom 10-CV RMSE = " + nystromRMSE);
        assertEquals(2.164701537672616, rmse, 1E-4);
        assertEquals(2.289313739055932, sparseRMSE, 1E-4);
        assertEquals(2.212407035135691, nystromRMSE, 1E-4);
    }

    @Test(expected = Test.None.class)
    public void testBank32nh() throws Exception {
        System.out.println("bank32nh");

        MathEx.setSeed(19650218); // to get repeatable results.

        double[][] x = MathEx.clone(Bank32nh.x);
        double[] y = Bank32nh.y;
        MathEx.standardize(x);

        int[] permutation = MathEx.permutate(x.length);
        double[][] datax = new double[4000][];
        double[] datay = new double[datax.length];
        for (int i = 0; i < datax.length; i++) {
            datax[i] = x[permutation[i]];
            datay[i] = y[permutation[i]];
        }

        CrossValidation cv = new CrossValidation(datax.length, 10);

        double[] prediction = cv.regression(datax, datay, (xi, yi) -> GaussianProcessRegression.fit(xi, yi, new GaussianKernel(55.3), 0.1));

        double[] sparsePrediction = cv.regression(10, datax, datay, (xi, yi) -> {
            KMeans kmeans = KMeans.fit(xi, 30);
            double[][] centers = kmeans.centroids;
            double r0 = 0.0;
            for (int l = 0; l < centers.length; l++) {
                for (int j = 0; j < l; j++) {
                    r0 += MathEx.distance(centers[l], centers[j]);
                }
            }
            r0 /= (2 * centers.length);
            System.out.println("Kernel width = " + r0);
            return GaussianProcessRegression.fit(xi, yi, centers, new GaussianKernel(r0), 0.1);
        });

        double[] nystromPrediction = cv.regression(datax, datay, (xi, yi) -> {
            KMeans kmeans = KMeans.fit(xi, 30);
            double[][] centers = kmeans.centroids;
            double r0 = 0.0;
            for (int l = 0; l < centers.length; l++) {
                for (int j = 0; j < l; j++) {
                    r0 += MathEx.distance(centers[l], centers[j]);
                }
            }
            r0 /= (2 * centers.length);
            System.out.println("Kernel width = " + r0);
            return GaussianProcessRegression.nystrom(xi, yi, centers, new GaussianKernel(r0), 0.1);
        });

        double rmse = RMSE.of(datay, prediction);
        double sparseRMSE = RMSE.of(datay, sparsePrediction);
        double nystromRMSE = RMSE.of(datay, nystromPrediction);

        System.out.println("Regular 10-CV RMSE = " + rmse);
        System.out.println("Sparse 10-CV RMSE = " + sparseRMSE);
        System.out.println("Nystrom 10-CV RMSE = " + nystromRMSE);
        assertEquals(0.08434491755621974, rmse, 1E-4);
        assertEquals(0.08494071211767774, sparseRMSE, 1E-4);
        assertEquals(0.34623422758160893, nystromRMSE, 1E-4);
    }

    @Test(expected = Test.None.class)
    public void testPuma8nh() throws Exception {
        System.out.println("puma8nh");

        MathEx.setSeed(19650218); // to get repeatable results.

        double[][] x = Puma8NH.x;
        double[] y = Puma8NH.y;

        int[] permutation = MathEx.permutate(x.length);
        double[][] datax = new double[4000][];
        double[] datay = new double[datax.length];
        for (int i = 0; i < datax.length; i++) {
            datax[i] = x[permutation[i]];
            datay[i] = y[permutation[i]];
        }

        CrossValidation cv = new CrossValidation(datax.length, 10);

        double[] prediction = cv.regression(datax, datay, (xi, yi) -> GaussianProcessRegression.fit(xi, yi, new GaussianKernel(38.63), 0.1));

        double[] sparsePrediction = cv.regression(10, datax, datay, (xi, yi) -> {
            KMeans kmeans = KMeans.fit(xi, 30);
            double[][] centers = kmeans.centroids;
            double r0 = 0.0;
            for (int l = 0; l < centers.length; l++) {
                for (int j = 0; j < l; j++) {
                    r0 += MathEx.distance(centers[l], centers[j]);
                }
            }
            r0 /= (2 * centers.length);
            System.out.println("Kernel width = " + r0);
            return GaussianProcessRegression.fit(xi, yi, centers, new GaussianKernel(r0), 0.1);
        });

        double[] nystromPrediction = cv.regression(datax, datay, (xi, yi) -> {
            KMeans kmeans = KMeans.fit(xi, 30);
            double[][] centers = kmeans.centroids;
            double r0 = 0.0;
            for (int l = 0; l < centers.length; l++) {
                for (int j = 0; j < l; j++) {
                    r0 += MathEx.distance(centers[l], centers[j]);
                }
            }
            r0 /= (2 * centers.length);
            System.out.println("Kernel width = " + r0);
            return GaussianProcessRegression.nystrom(xi, yi, centers, new GaussianKernel(r0), 0.1);
        });

        double rmse = RMSE.of(datay, prediction);
        double sparseRMSE = RMSE.of(datay, sparsePrediction);
        double nystromRMSE = RMSE.of(datay, nystromPrediction);

        System.out.println("Regular 10-CV RMSE = " + rmse);
        System.out.println("Sparse 10-CV RMSE = " + sparseRMSE);
        System.out.println("Nystrom 10-CV RMSE = " + nystromRMSE);
        assertEquals(4.441690979075472, rmse, 1E-4);
        assertEquals(4.421352271422635, sparseRMSE, 1E-4);
        assertEquals(4.414866025541026, nystromRMSE, 1E-4);
    }

    @Test(expected = Test.None.class)
    public void testKin8nm() throws Exception {
        System.out.println("kin8nm");

        MathEx.setSeed(19650218); // to get repeatable results.

        double[][] x = MathEx.clone(Kin8nm.x);
        double[] y = Kin8nm.y;
        int[] permutation = MathEx.permutate(x.length);
        double[][] datax = new double[4000][];
        double[] datay = new double[datax.length];
        for (int i = 0; i < datax.length; i++) {
            datax[i] = x[permutation[i]];
            datay[i] = y[permutation[i]];
        }

        CrossValidation cv = new CrossValidation(datax.length, 10);

        double[] prediction = cv.regression(datax, datay, (xi, yi) -> GaussianProcessRegression.fit(xi, yi, new GaussianKernel(34.97), 0.1));

        double[] sparsePrediction = cv.regression(10, datax, datay, (xi, yi) -> {
            KMeans kmeans = KMeans.fit(xi, 30);
            double[][] centers = kmeans.centroids;
            double r0 = 0.0;
            for (int l = 0; l < centers.length; l++) {
                for (int j = 0; j < l; j++) {
                    r0 += MathEx.distance(centers[l], centers[j]);
                }
            }
            r0 /= (2 * centers.length);
            System.out.println("Kernel width = " + r0);
            return GaussianProcessRegression.fit(xi, yi, centers, new GaussianKernel(r0), 0.1);
        });

        double[] nystromPrediction = cv.regression(datax, datay, (xi, yi) -> {
            KMeans kmeans = KMeans.fit(xi, 30);
            double[][] centers = kmeans.centroids;
            double r0 = 0.0;
            for (int l = 0; l < centers.length; l++) {
                for (int j = 0; j < l; j++) {
                    r0 += MathEx.distance(centers[l], centers[j]);
                }
            }
            r0 /= (2 * centers.length);
            System.out.println("Kernel width = " + r0);
            return GaussianProcessRegression.nystrom(xi, yi, centers, new GaussianKernel(r0), 0.1);
        });

        double rmse = RMSE.of(datay, prediction);
        double sparseRMSE = RMSE.of(datay, sparsePrediction);
        double nystromRMSE = RMSE.of(datay, nystromPrediction);

        System.out.println("Regular 10-CV RMSE = " + rmse);
        System.out.println("Sparse 10-CV RMSE = " + sparseRMSE);
        System.out.println("Nystrom 10-CV RMSE = " + nystromRMSE);
        assertEquals(0.20205594684848896, rmse, 1E-4);
        assertEquals(0.19840126234796535, sparseRMSE, 1E-4);
        assertEquals(0.19580679837507917, nystromRMSE, 1E-4);
    }
}