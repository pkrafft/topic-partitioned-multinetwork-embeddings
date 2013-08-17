package mixedmembership;

import util.DistanceModelProbabilities;
import util.LogRandoms;
import data.Email;
import data.EmailCorpus;

/**
 * A class that implements the latent space mixture/admixture components
 * discussed in Krafft et al. (2012).
 * 
 * In this model each mixture component is a symmetric A x A matrix of Bernoulli
 * probabilities that are entirely determined by a set of A K-dimensional latent
 * positions and an intercept term. The author of each email affects the
 * probabilities of recipients in that email by selecting the row of each
 * component to use.
 * 
 * This class makes extensive use of caching in order to improve computational
 * efficiency. The main cache {@link #logProbCache} holds the log probabilities
 * of each pair of actors communicating in each component.
 */
public class EdgeScoreLatentSpace implements EdgeScore {

	EmailCorpus emails;

	int numActors, numDocs, numFeatures, numLatentDims;

	DistanceModelProbabilities distanceModelProbs;

	// K-dimensional position of each actor in each space
	double[][][] latentSpaces;
	double[] intercepts; // offset for each space

	LogRandoms rng;

	// Caching Variables //

	// the main cache that stores the link function computation
	private double[][][][] logProbCache;
	private boolean[][][][] cacheUpdated;

	// holds the number of times each present or absent connection (a,r,y) is
	// assigned to each component
	private int[][][][] connectionCounter;

	// the following cache is used locally in the sampling methods as temporary
	// space before it is known whether logProbCache should be updated
	double[][][] newLogProbCache;
	boolean[][][] newCacheUpdated;

	/**
	 * 
	 * @param T
	 *            number of mixture/admixture components to use
	 * @param K
	 *            dimension of the latent spaces
	 * @param emails
	 *            the data
	 * @param initPositions
	 *            initial latent space positions
	 * @param initIntercepts
	 *            initial latent space offsets
	 * @param rng
	 *            a random number generator
	 */
	public EdgeScoreLatentSpace(int T, int K, EmailCorpus emails,
			double[][][] initPositions, double[] initIntercepts, LogRandoms rng) {

		this.emails = emails;

		numActors = emails.getNumAuthors();
		numDocs = emails.size();
		numFeatures = T;
		numLatentDims = K;

		distanceModelProbs = new DistanceModelProbabilities(numLatentDims);

		this.rng = rng;

		latentSpaces = initPositions;
		if (initPositions == null) {
			initializePositions();
		}

		intercepts = initIntercepts;
		if (initIntercepts == null) {
			initializeIntercepts();
		}

		logProbCache = new double[numFeatures][numActors][][];
		cacheUpdated = new boolean[numFeatures][numActors][][];
		connectionCounter = new int[numFeatures][numActors][][];

		newLogProbCache = new double[numActors][][];
		newCacheUpdated = new boolean[numActors][][];

		for (int i = 0; i < numActors; i++) {

			newLogProbCache[i] = new double[i][2];
			newCacheUpdated[i] = new boolean[i][2];

			for (int f = 0; f < numFeatures; f++) {
				logProbCache[f][i] = new double[i][2];
				cacheUpdated[f][i] = new boolean[i][2];
				connectionCounter[f][i] = new int[i][2];
			}
		}
	}

	/**
	 * Pretty much what it sounds like.
	 */
	private void initializePositions() {
		latentSpaces = new double[numFeatures][numActors][numLatentDims];
		for (int space = 0; space < numFeatures; space++) {
			for (int a = 0; a < numActors; a++) {
				for (int dim = 0; dim < numLatentDims; dim++) {
					latentSpaces[space][a][dim] = rng.nextGaussian();
				}
			}
		}
	}

	public void initializeIntercepts() {
		intercepts = new double[numFeatures];

		for (int space = 0; space < numFeatures; space++) {
			intercepts[space] = rng.nextGaussian();
		}
	}

	public int samplePosition(int space, int actor, double var) {
		return samplePosition(space, actor, var, false, null);
	}

	/**
	 * Perform a Metropolis step to sample a new position for a particular actor
	 * in a particular space.
	 * 
	 * The proposal distribution samples all dimensions of the position jointly
	 * according to a multivariate normal distribution centered around the last
	 * position with diagonal covariance.
	 * 
	 * @param space
	 *            the index of the space being sampled
	 * @param actor
	 *            the id of the actor being sampled
	 * @param var
	 *            the proposal variance
	 * @param debug
	 *            whether to use the supplied sample rather than draw one from
	 *            the proposal distribution
	 * @param sample
	 *            (when debug == true) the proposal to use
	 * @return 1 if the proposal was accepted, 0 otherwise
	 */
	public int samplePosition(int space, int actor, double var, boolean debug,
			double[] sample) {

		if (!debug) {
			sample = new double[numLatentDims];

			// Metropolis-Hastings proposal
			for (int k = 0; k < numLatentDims; k++) {
				sample[k] = rng
						.nextGaussian(latentSpaces[space][actor][k], var);
			}
		}

		double ratio = getMetropolisPositionLogRatio(space, actor, sample);

		int result;

		if (!debug && ratio < Math.log(rng.nextDouble())) { // reject proposal
			result = 0;
		} else { // accept proposal
			latentSpaces[space][actor] = sample;

			for (int r = 0; r < numActors; r++) {
				if (r == actor)
					continue;

				int[] pair = orderActors(actor, r);

				// update global cache using local cache
				for (int y = 0; y < 2; y++) {
					if (newCacheUpdated[pair[0]][pair[1]][y]) {
						logProbCache[space][pair[0]][pair[1]][y] = newLogProbCache[pair[0]][pair[1]][y];
						cacheUpdated[space][pair[0]][pair[1]][y] = true;
					} else {
						cacheUpdated[space][pair[0]][pair[1]][y] = false;
					}
				}
			}

			result = 1;
		}

		// reset local cache
		for (int r = 0; r < numActors; r++) {
			if (r == actor)
				continue;

			int[] pair = orderActors(actor, r);

			for (int y = 0; y < 2; y++) {
				newCacheUpdated[pair[0]][pair[1]][y] = false;
			}
		}

		return result;
	}

	/**
	 * 
	 * @param space
	 *            the index of the component being sampled
	 * @param actor
	 *            the index of the actor being sampled
	 * @param sample
	 *            the proposed position
	 * @return the log probability of accepting the proposal
	 */
	public double getMetropolisPositionLogRatio(int space, int actor,
			double[] sample) {

		return getTempPositionLogProb(space, actor, sample)
				- getPositionLogProb(space, actor);
	}

	// computes a component of the likelihood function using the global cache
	public double getPositionLogProb(int space, int actor) {

		double logProb = 0;

		for (int r = 0; r < numActors; r++) {
			if (r == actor)
				continue;

			logProb += getLogProduct(space, actor, r);
		}

		return logProb;
	}

	// computes a component of the likelihood function using the local cache
	public double getTempPositionLogProb(int space, int actor, double[] sample) {

		double newLogProb = 0;

		for (int r = 0; r < numActors; r++) {
			if (r == actor)
				continue;

			newLogProb += getTempPositionLogProduct(space, actor, r, sample);
		}

		return newLogProb;
	}

	public int sampleIntercept(int space, double var) {
		return sampleIntercept(space, var, false, 0);
	}

	/**
	 * Perform a Metropolis step to sample a new intercept for a particular
	 * space
	 * 
	 * The proposal distribution is a normal distribution centered around the
	 * last intercept with a given variance.
	 * 
	 * @param space
	 *            the index of the space being sampled
	 * @param var
	 *            the proposal variance
	 * @param debug
	 *            whether to use the given sample as a proposal
	 * @param sample
	 *            (when debug == true) the sample to use as a proposal
	 * @return 1 if the proposal is accepted, 0 otherwise
	 */
	public int sampleIntercept(int space, double var, boolean debug,
			double sample) {

		if (!debug) {
			// draw a new proposal
			sample = rng.nextGaussian(intercepts[space], var);
		}

		double ratio = getMetropolisInterceptLogRatio(space, sample);

		int result;

		if (!debug && ratio < Math.log(rng.nextDouble())) { // reject proposal
			result = 0;
		} else { // accept proposal

			intercepts[space] = sample;

			// update global cache using local cache
			for (int actor = 1; actor < numActors; actor++) {
				for (int r = 0; r < actor; r++) {
					for (int y = 0; y < 2; y++) {
						if (newCacheUpdated[actor][r][y]) {
							logProbCache[space][actor][r][y] = newLogProbCache[actor][r][y];
							cacheUpdated[space][actor][r][y] = true;
						} else {
							cacheUpdated[space][actor][r][y] = false;
						}
					}
				}
			}

			result = 1;
		}

		// reset local cache
		for (int actor = 1; actor < numActors; actor++) {
			for (int r = 0; r < actor; r++) {
				for (int y = 0; y < 2; y++) {
					newCacheUpdated[actor][r][y] = false;
				}
			}
		}

		return result;
	}

	/**
	 * 
	 * @param space
	 *            the index of the component being sampled
	 * @param sample
	 *            the proposed intercept
	 * @return the log probability of accepting the proposal
	 */
	public double getMetropolisInterceptLogRatio(int space, double sample) {

		double logProb = 0;

		for (int actor = 1; actor < numActors; actor++) {
			for (int r = 0; r < actor; r++) {
				logProb += getLogProduct(space, actor, r);
			}
		}

		double newLogProb = 0;

		for (int actor = 1; actor < numActors; actor++) {
			for (int r = 0; r < actor; r++) {
				newLogProb += getTempInterceptLogProduct(space, actor, r,
						sample);
			}
		}

		return newLogProb - logProb;
	}

	// computes a component of the likelihood function using the global cache
	private double getLogProduct(int space, int a, int r) {

		int[] pair = orderActors(a, r);

		double p = 0.0;

		if (connectionCounter[space][pair[0]][pair[1]][1] > 0) {
			p += connectionCounter[space][pair[0]][pair[1]][1]
					* getLogProb(space, a, r, 1);
		}
		if (connectionCounter[space][pair[0]][pair[1]][0] > 0) {
			p += connectionCounter[space][pair[0]][pair[1]][0]
					* getLogProb(space, a, r, 0);
		}

		return p;
	}

	// computes a component of the likelihood function using the local cache
	private double getTempPositionLogProduct(int space, int a, int r,
			double[] sample) {

		int[] pair = orderActors(a, r);

		double p = 0.0;

		if (connectionCounter[space][pair[0]][pair[1]][1] > 0) {
			p += connectionCounter[space][pair[0]][pair[1]][1]
					* getTempPositionLogProb(space, a, r, 1, sample);
		}
		if (connectionCounter[space][pair[0]][pair[1]][0] > 0) {
			p += connectionCounter[space][pair[0]][pair[1]][0]
					* getTempPositionLogProb(space, a, r, 0, sample);
		}

		return p;
	}

	// computes a component of the likelihood function using the local cache
	private double getTempInterceptLogProduct(int space, int a, int r,
			double sample) {

		int[] pair = orderActors(a, r);

		double p = 0.0;

		if (connectionCounter[space][pair[0]][pair[1]][1] > 0) {
			p += connectionCounter[space][pair[0]][pair[1]][1]
					* getTempInterceptLogProb(space, a, r, 1, sample);
		}
		if (connectionCounter[space][pair[0]][pair[1]][0] > 0) {
			p += connectionCounter[space][pair[0]][pair[1]][0]
					* getTempInterceptLogProb(space, a, r, 0, sample);
		}

		return p;
	}

	/**
	 * Calculate the log probability of two actor communicating in a particular
	 * space.
	 * 
	 * Computes P(y | x, z, s, b).
	 * 
	 * @param t
	 *            the space to use
	 * @param a
	 *            the first actor
	 * @param r
	 *            the second actor
	 * @param y
	 *            the value of the edge
	 * @return the log probability
	 */
	public double calculateLogProb(int t, int a, int r, int y) {
		int[] pair = orderActors(a, r);
		return distanceModelProbs.getLogScore(t, pair[0], pair[1], y,
				latentSpaces, intercepts);
	}

	@Override
	public double getLogProb(int t, int a, int r, int y) {

		int[] pair = orderActors(a, r);

		// if the value is not already cached
		if (!cacheUpdated[t][pair[0]][pair[1]][y]) {

			// if the probability of 1 - y is not available
			if (!cacheUpdated[t][pair[0]][pair[1]][1 - y]) {

				// calculate and cache
				logProbCache[t][pair[0]][pair[1]][y] = calculateLogProb(t, a,
						r, y);
				cacheUpdated[t][pair[0]][pair[1]][y] = true;

			} else { // else flip the cached probability

				logProbCache[t][pair[0]][pair[1]][y] = Math.log(1 - Math
						.exp(logProbCache[t][pair[0]][pair[1]][1 - y]));
				cacheUpdated[t][pair[0]][pair[1]][y] = true;

			}
		}

		// use the (newly?) cached value
		return logProbCache[t][pair[0]][pair[1]][y];
	}

	/**
	 * As {@link getLogProb} but with the given sample as the position for the
	 * first actor.
	 * 
	 * This method also uses the local cache instead of the global cache.
	 */
	public double getTempPositionLogProb(int t, int a, int r, int y,
			double[] sample) {

		int[] pair = orderActors(a, r);

		// if the value is not already cached
		if (!newCacheUpdated[pair[0]][pair[1]][y]) {

			// if the probability of 1 - y isn't already cached
			if (!newCacheUpdated[pair[0]][pair[1]][1 - y]) {

				// calculate and cache
				newLogProbCache[pair[0]][pair[1]][y] = distanceModelProbs
						.getLogScore(sample, latentSpaces[t][r], y,
								intercepts[t]);
				newCacheUpdated[pair[0]][pair[1]][y] = true;

			} else { // flip the cached value
				newLogProbCache[pair[0]][pair[1]][y] = Math.log(1 - Math
						.exp(newLogProbCache[pair[0]][pair[1]][1 - y]));
				newCacheUpdated[pair[0]][pair[1]][y] = true;
			}
		}

		return newLogProbCache[pair[0]][pair[1]][y];
	}

	/**
	 * As {@link getLogProb} but with the given sample as the intercept for the
	 * given space.
	 * 
	 * This method also uses the local cache instead of the global cache.
	 */
	public double getTempInterceptLogProb(int t, int a, int r, int y,
			double sample) {

		int[] pair = orderActors(a, r);

		// if the value is not cached already
		if (!newCacheUpdated[pair[0]][pair[1]][y]) {
			
			// if the probability of 1 - y isn't cached
			if (!newCacheUpdated[pair[0]][pair[1]][1 - y]) {
				
				// calculate and cache
				newLogProbCache[pair[0]][pair[1]][y] = distanceModelProbs
						.getLogScore(latentSpaces[t][pair[0]],
								latentSpaces[t][pair[1]], y, sample);
				newCacheUpdated[pair[0]][pair[1]][y] = true;
				
			} else { // flip the cached value
				newLogProbCache[pair[0]][pair[1]][y] = Math.log(1 - Math
						.exp(newLogProbCache[pair[0]][pair[1]][1 - y]));
				newCacheUpdated[pair[0]][pair[1]][y] = true;
			}
		}

		return newLogProbCache[pair[0]][pair[1]][y];
	}

	@Override
	public void incrementCounts(int t, int a, int r, int y) {

		int[] pair = orderActors(a, r);

		connectionCounter[t][pair[0]][pair[1]][y]++;
	}

	@Override
	public void decrementCounts(int t, int a, int r, int y) {

		int[] pair = orderActors(a, r);

		connectionCounter[t][pair[0]][pair[1]][y]--;
		assert connectionCounter[t][pair[0]][pair[1]][y] >= 0;
	}

	public double[][][] getLatentSpaces() {
		return latentSpaces;
	}

	/**
	 * Resets caches.
	 */
	public void clearCache() {
		for (int i = 0; i < numActors; i++) {
			for (int f = 0; f < numFeatures; f++) {
				cacheUpdated[f][i] = new boolean[i][2];
			}
		}
	}

	@Override
	public boolean checkCacheConsistency(JointStructure assignmentModel)
			throws Exception {

		// compute connectionCounter directly
		int[][][][] correctConnections = new int[numFeatures][numActors][numActors][2];
		for (int d = 0; d < numDocs; d++) {
			Email email = emails.getDocument(d);
			int a = email.getAuthor();
			for (int j = 0; j < numActors - 1; j++) {
				int r = email.getRecipient(j);
				int t = assignmentModel.getEdgeTopicAssignment(d, j);
				int y = email.getEdge(r);

				int[] pair = orderActors(a, r);

				correctConnections[t][pair[0]][pair[1]][y]++;
			}
		}

		for (int t = 0; t < numFeatures; t++) {
			for (int a = 1; a < numActors; a++) {
				for (int r = 0; r < a; r++) {
					for (int y = 0; y < 2; y++) {

						// check globally cached log probs are correct
						if (cacheUpdated[t][a][r][y]) {
							double logProb = calculateLogProb(t, a, r, y);
							double cachedLogProb = logProbCache[t][a][r][y];
							if (Math.abs(Math.exp(cachedLogProb)
									- Math.exp(logProb)) > 1e-12) {
								throw new Exception();
							}
						}

						// check local cache is reset
						if (newCacheUpdated[a][r][y]) {
							throw new Exception();
						}

						// check edge assignment counts are correct
						if (correctConnections[t][a][r][y] != connectionCounter[t][a][r][y]) {
							throw new Exception();
						}
					}
				}
			}
		}

		return true;
	}

	/**
	 * Returns a canonical ordering of two actors.
	 * 
	 * Since the mixture components in this model are symmetric, we only use the
	 * lower triangle of each component.
	 * 
	 * @param a
	 *            an actor id
	 * @param r
	 *            another actor id
	 * @return a canonical ordering of the actors
	 */
	int[] orderActors(int a, int r) {
		if (a < r) {
			return new int[] { r, a };
		} else {
			return new int[] { a, r };
		}
	}

	public double[] getIntercepts() {
		return intercepts;
	}
}
