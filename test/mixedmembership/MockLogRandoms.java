package mixedmembership;

import util.LogRandoms;

/**
 * The main difficult part of unit testing these methods is that the estimation
 * procedures are stochastic. In order to predict the output we need to be able
 * to control what random numbers are chosen. There are different ways to do
 * this, but we have chosen to create a mock random number generator that allows
 * us to completely specify every random number that is drawn. We can then work
 * through examples using those known values to get the expected output. This
 * essentially just means that we are making sure all of the caches and data
 * structures are updated correctly. We typically just test that the ultimate
 * likelihood is right, which ends up being pretty close to meaning that all of
 * the data structures have the right values.
 * 
 * However, these tests do not cover whether the distribution we generate are
 * correct since the random numbers are totally specified. We designed the code
 * so that we could explicitly test that the full conditional distributions for
 * Gibbs sampling and Metropolis ratios for Metropolis-Hastings are what we
 * would expect them to be.
 * 
 * The only major part of this code is not formally tested in these unit tests
 * is the initialization distributions when sampling from priors. It is not
 * clear to me at all how to verify that these distributions are correct without
 * major refactoring to the code. The hyperparameter sampling code has the same
 * basic problem of not being easily testable.
 * 
 * Because of the difficulty of writing unit tests with these mock objects,
 * there is a lot of legacy code in these tests that is not very nice or
 * readable at all. This class is a perfect example.
 */
public class MockLogRandoms extends LogRandoms {

	private static final long serialVersionUID = 1L;
	int countGaussian;
	int countGaussianMeanVar;
	int countDiscrete;
	int countDouble;

	double[] gaussianSamples;
	int[] discreteSamples;
	double[] doubleSamples;

	public MockLogRandoms() {
		this(-1);
	}

	public MockLogRandoms(int initCount) {
		countGaussian = initCount;
		countDiscrete = -1;
		countDouble = initCount;

		// for all tests, three iterations of latent space samples
		gaussianSamples = new double[] {
				// for edge test
				-1, // accept
				-1,
				1, // accept
				8,
				8, // accept (to be replaced)
				1,
				2, // accept
				0,
				1, // accept
				8, // accept
				0, // reject
				0, // reject
				0,
				0, // reject
				0,
				0, // reject
				1, // accept

				// burn samples for first iteration of mmlsm (multi-intercept)
				// tests
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 2, 1,
				2,
				1,
				1, // for the single intercept test

				// for mmlsm tests,
				// final positions besides the two MCMC fails (each position
				// sample is a pair)
				0, 1, 1, 2, 2, 0, 0, 0, -1, -1, 0, 0, 1, -1, 0, 0, -1, 1, 0, 0,
				1, 0, 0, 0, -1, 0, 0 };

		discreteSamples = new int[] {
				// for mmlsm-only tests (three iterations of edge assignments)
				1, 2, 0, 0, 0, 1, 2, 2, 2, 1, 0, 1, 0, 2, 2, 1, 0, 2,

				// for joint model tests
				1, 1,

				1, 2, 2, 1,

				1, 0, 2, 1, 1, 1, 2, 1, 0, 1, 2, 2, // final assignments for
													// mmlsm marginalized test

				2, 1, 1, 0,

				0, 2, 2, 1, 0, 2, 1, 0, 2, 1, 1, 1, 2, 0, 2, 2, 2, 1, 0, 0

		};

		// for all tests - accept mcmc proposals by default
		doubleSamples = new double[100];

		// for edge model test, includes two samples for initializing missing
		// values
		doubleSamples[3] = 1.0;
		doubleSamples[14] = 1.0;
		doubleSamples[9] = Double.POSITIVE_INFINITY;
		doubleSamples[10] = Double.POSITIVE_INFINITY;
		doubleSamples[11] = Double.POSITIVE_INFINITY;
		doubleSamples[12] = Double.POSITIVE_INFINITY;

		// in mmlsem tests, reject last latent position sample, last intercept
		doubleSamples[41] = Double.POSITIVE_INFINITY;
		doubleSamples[44] = Double.POSITIVE_INFINITY;
	}

	public double nextGaussian(double mean, double var) {
		countGaussian++;
		if (countGaussian >= gaussianSamples.length) {
			countGaussian = 0;
		}
		return gaussianSamples[countGaussian];
	}

	public int nextDiscrete(double[] a, double sum) {
		countDiscrete++;
		if (countDiscrete >= discreteSamples.length) {
			countDiscrete = 0;
		}
		return discreteSamples[countDiscrete];
	}

	public int nextDiscreteLogDist(double[] a) {
		return nextDiscrete(a, 1.0);
	}

	public double nextDouble() {
		countDouble++;
		if (countDouble >= doubleSamples.length) {
			countDouble = 0;
		}
		return doubleSamples[countDouble];
	}

	public void setDiscrete(int[] values) {
		discreteSamples = values;
	}

	public void setDouble(double[] values) {
		doubleSamples = values;
	}

	public void setGaussian(double[] values) {
		gaussianSamples = values;
	}
}
