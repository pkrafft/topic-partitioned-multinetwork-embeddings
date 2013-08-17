package util;

import cc.mallet.util.Maths;
import cc.mallet.util.Randoms;

/**
 * A class for dealing with discrete distributions represented in log space.
 */
public class LogRandoms extends Randoms {

	private static final long serialVersionUID = 1L;

	public LogRandoms(int seed) {

		super(seed);
	}

	public LogRandoms() {

		super();
	}

	/**
	 * Return the next draw from the given discrete log distribution.
	 * 
	 * @param logDist
	 *            an unnnormalized discrete distribution represented in log
	 *            space
	 * @param logDistSum
	 *            the log normalizing constant for the distribution
	 * @return a sample from the given distribution
	 */
	public int nextDiscreteLogDistSlow(double[] logDist, double logDistSum) {

		double r = Math.log(nextUniform()) + logDistSum;
		double acc = Double.NEGATIVE_INFINITY;

		int m = -1;

		for (int i = 0; i < logDist.length; i++) {
			acc = Maths.sumLogProb(acc, logDist[i]);

			if (acc > r) {
				m = i;
				break;
			}
		}

		assert m > -1;

		return m;
	}

	/**
	 * Return the next draw from the given unnormalized discrete log
	 * distribution.
	 * 
	 * @param logDist
	 *            an unnnormalized discrete distribution represented in log
	 *            space
	 * @return a sample from the given distribution
	 */
	public int nextDiscreteLogDist(double[] logDist) {

		double max = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < logDist.length; i++)
			if (logDist[i] > max)
				max = logDist[i];

		double[] dist = new double[logDist.length];
		double distSum = 0.0;

		for (int i = 0; i < logDist.length; i++) {
			dist[i] = Math.exp(logDist[i] - max);
			distSum += dist[i];
		}

		return nextDiscrete(dist, distSum);
	}

	/**
	 * Return the next draw from the given discrete distribution.
	 * 
	 * @param dist
	 *            an unnnormalized discrete distribution
	 * @param distSum
	 *            the normalizing constant for the distribution
	 * @return a sample from the given distribution
	 */
	public int nextDiscrete(double[] dist, double distSum) {

		if (dist.length == 1) {
			return 0;
		}

		double r = nextUniform() * distSum;
		double acc = 0.0;

		int m = -1;

		for (int i = 0; i < dist.length; i++) {
			acc += dist[i];

			if (acc > r) {
				m = i;
				break;
			}
		}

		assert m > -1;

		return m;
	}
}
