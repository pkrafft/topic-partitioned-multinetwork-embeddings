package util;

/**
 * A class for calculating the probability of actors connecting according to the
 * latent space distance model of Hoff et al. (2002).
 */
public class DistanceModelProbabilities {

	private int latentDim;

	/**
	 * Create an object for evaluating latent spaces.
	 * 
	 * @param K
	 *            Dimension of the latent space
	 */
	public DistanceModelProbabilities(int K) {
		this.latentDim = K;
	}

	/**
	 * Calculate the probability of two actors communicating according to a
	 * given space.
	 * 
	 * @param space
	 *            which space to use
	 * @param a
	 *            the id of the first actor
	 * @param r
	 *            the id of the second actor
	 * @param y
	 *            whether to calculate the probability of a present connection
	 *            (y = 1) or an absent connection (y = 0)
	 * @param latentSpaces
	 *            a set of latent spaces to use
	 * @param intercepts
	 *            a set of intercepts (offsets) to use for each space
	 * @return the logistic sigmoid of the offset Euclidean distance between
	 *         actors a and r
	 */
	public double getScore(int space, int a, int r, int y,
			double[][][] latentSpaces, double[] intercepts) {

		double eta = 0.0;
		for (int k = 0; k < latentDim; k++) {
			eta += Math.pow(latentSpaces[space][a][k]
					- latentSpaces[space][r][k], 2);
		}
		eta = intercepts[space] - Math.sqrt(eta);

		return y == 1 ? Math.exp(eta) / (1 + Math.exp(eta)) : 1 / (1 + Math
				.exp(eta));
	}

	public double getLogScore(int space, int a, int r, int y,
			double[][][] latentSpaces, double[] intercepts) {

		return getLogScore(latentSpaces[space][a], latentSpaces[space][r], y,
				intercepts[space]);
	}

	/**
	 * Calculate the log probability of two actors communicating according to
	 * two given positions and a given offset.
	 * 
	 * @param a
	 *            the position of the first actor
	 * @param r
	 *            the position of the second actor
	 * @param y
	 *            whether to calculate the probability of a present connection
	 *            (y = 1) or an absent connection (y = 0)
	 * @param intercept
	 *            the offset to use
	 * @return the log of the logistic sigmoid of the offset Euclidean distance
	 *         between the given positions
	 */
	public double getLogScore(double[] a, double[] r, int y, double intercept) {

		assert a.length == r.length;

		double eta = 0.0;
		for (int k = 0; k < latentDim; k++) {
			eta += Math.pow(a[k] - r[k], 2);
		}
		eta = intercept - Math.sqrt(eta);

		if (eta < 0) {
			return y == 1 ? eta - Math.log(1 + Math.exp(eta)) : -Math
					.log(1 + Math.exp(eta));
		} else {
			return y == 1 ? -Math.log(1 + Math.exp(-eta)) : -eta
					- Math.log(1 + Math.exp(-eta));
		}
	}

}
