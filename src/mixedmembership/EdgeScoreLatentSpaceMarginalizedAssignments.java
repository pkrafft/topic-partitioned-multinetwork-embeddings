package mixedmembership;

import java.util.Arrays;

import util.DistanceModelProbabilities;
import util.LogRandoms;
import cc.mallet.util.Maths;
import data.Email;
import data.EmailCorpus;

/**
 * A class that implements the latent space mixture/admixture components
 * discussed in Krafft et al. (2012) with the edge assignments marginalized out.
 * 
 * In this model each mixture component is a symmetric A x A matrix of Bernoulli
 * probabilities that are entirely determined by a set of A K-dimensional latent
 * positions and an intercept term. The author of each email affects the
 * probabilities of recipients in that email by selecting the row of each
 * component to use.
 * 
 * Furthermore, since edge assignments are marginalized out, the probability of
 * each recipient is the weighted sum of the probabilities of that recipient in
 * each mixture component with the weights determined by the mixture proportions
 * of the email.
 * 
 * This class makes extensive use of caching in order to improve computational
 * efficiency.
 * 
 * NOTE THAT THIS CLASS IS NOT EXTENSIVELY TESTED AND PROBABLY DOES NOT WORK
 * WITH HYPERPARAMETER SAMPLING
 */
public class EdgeScoreLatentSpaceMarginalizedAssignments {

	// this code is complicated and not super important

	EmailCorpus emails;

	int numActors, numDocs, numFeatures, numLatentDims;

	DistanceModelProbabilities edgeScore;

	double[][][] latentSpaces;
	double[] intercepts;

	LogRandoms rng;

	boolean usingSingleIntercept;

	double[][] logFeatureProportions;

	double[][][] spaceEdgeLogProbs;
	boolean[][] spaceEdgeLogProbsCached;
	double[][] edgeLogProbs;

	double[] positionLogLikes;
	double interceptLogLike;

	double[][][] cachedSpaceEdgeLogProbs;
	double[][] cachedEdgeLogProbs;

	public EdgeScoreLatentSpaceMarginalizedAssignments(int T, int K,
			EmailCorpus emails, double[][][] initPositions,
			double[] initIntercepts, boolean initializePositions,
			boolean initializeIntercepts, boolean initializeMissingData,
			LogRandoms rng, boolean usingSingleIntercept) {

		System.out.println("WARNING: THIS CLASS IS NOT EXTENSIVELY "
				+ "TESTED AND PROBABLY DOES NOT WORK WITH "
				+ "HYPERPARAMETER SAMPLING!");

		this.emails = emails;

		numActors = emails.getNumAuthors();
		numDocs = emails.size();
		numFeatures = T;
		numLatentDims = K;

		edgeScore = new DistanceModelProbabilities(numLatentDims);

		this.rng = rng;

		this.usingSingleIntercept = usingSingleIntercept;

		if (initializeMissingData) {
			initializeMissingValues();
		}

		latentSpaces = initPositions;
		if (initializePositions) {
			initializePositions();
		}

		intercepts = initIntercepts;
		if (initializeIntercepts) {
			initializeIntercepts();
		}

		logFeatureProportions = new double[numDocs][numFeatures];
	}

	public EdgeScoreLatentSpaceMarginalizedAssignments(int T, int K,
			EmailCorpus emails, double[][][] initPositions,
			double[] initIntercepts, boolean initializePositions,
			boolean initializeIntercepts, LogRandoms rng) {
		this(T, K, emails, initPositions, initIntercepts, initializePositions,
				initializeIntercepts, true, rng, false);
	}

	public EdgeScoreLatentSpaceMarginalizedAssignments(int numFeatures,
			int latentDim, EmailCorpus emails, double[][][] initPositions,
			double[] initIntercepts, LogRandoms rng) {
		this(numFeatures, latentDim, emails, initPositions, initIntercepts,
				false, false, false, rng, false);
	}

	public void generateEdges(double[][] logFeatureProportions) {

		for (int d = 0; d < numDocs; d++) {

			Email e = emails.getDocument(d);

			for (int j = 0; j < numActors - 1; j++) {

				int a = e.getAuthor();
				int r = e.getRecipient(j);

				double[] components = new double[numFeatures];
				for (int f = 0; f < numFeatures; f++) {
					components[f] = logFeatureProportions[d][f]
							+ edgeScore.getLogScore(f, a, r, 1, latentSpaces,
									intercepts);
				}

				double logP = Maths.sumLogProb(components);

				int y = Math.log(rng.nextDouble()) < logP ? 1 : 0;
				e.setEdge(r, y);
			}
		}
	}

	public void initializeCaches() {

		spaceEdgeLogProbs = new double[numFeatures][numActors][numActors];
		edgeLogProbs = new double[numDocs][numActors - 1];
		positionLogLikes = new double[numActors];
		interceptLogLike = 0;

		cachedSpaceEdgeLogProbs = new double[numFeatures][numActors][numActors];
		cachedEdgeLogProbs = new double[numDocs][numActors - 1];

		double[] components = new double[numFeatures];

		for (int d = 0; d < numDocs; d++) {

			Email e = emails.getEmail(d);

			int a = e.getAuthor();

			for (int j = 0; j < numActors - 1; j++) {

				int r = e.getRecipient(j);
				int y = e.getEdge(r);

				for (int f = 0; f < numFeatures; f++) {

					spaceEdgeLogProbs[f][a][r] = edgeScore.getLogScore(f, a, r,
							1, latentSpaces, intercepts);
					spaceEdgeLogProbs[f][r][a] = spaceEdgeLogProbs[f][a][r];

					components[f] = logFeatureProportions[d][f]
							+ spaceEdgeLogProbs[f][a][r];
				}

				double logP = Maths.sumLogProb(components);
				logP = y == 1 ? logP : Math.log(1 - Math.exp(logP));

				edgeLogProbs[d][j] = logP;
				interceptLogLike += logP;
				positionLogLikes[a] += logP;
				positionLogLikes[r] += logP;
			}
		}
	}

	public double logEdgeDataProb() {
		return interceptLogLike;
	}

	public int samplePosition(int space, int actor) {

		spaceEdgeLogProbsCached = new boolean[numActors][numActors];

		double lastLogPosteriorLike = positionLogLikes[actor];

		double[] lastSpaceSample = new double[numLatentDims];
		System.arraycopy(latentSpaces[space][actor], 0, lastSpaceSample, 0,
				numLatentDims);

		for (int k = 0; k < numLatentDims; k++) {
			latentSpaces[space][actor][k] = rng.nextGaussian(
					latentSpaces[space][actor][k], 1.0);
		}

		double logPosteriorLike = 0;

		for (int d = 0; d < numDocs; d++) {

			Email e = emails.getEmail(d);

			int author = e.getAuthor();

			if (author == actor) {

				for (int j = 0; j < numActors - 1; j++) {

					int r = e.getRecipient(j);
					int y = e.getEdge(r);

					if (!spaceEdgeLogProbsCached[actor][r]) {
						recalculateSpaceEdgeProb(space, actor, r);
					}
					recalculatEdgeProb(space, d, j, y, actor, r);

					logPosteriorLike += edgeLogProbs[d][j];

				}
			} else {

				int j = e.getIndex(actor);
				int y = e.getEdge(actor);

				if (!spaceEdgeLogProbsCached[author][actor]) {
					recalculateSpaceEdgeProb(space, author, actor);
				}
				recalculatEdgeProb(space, d, j, y, author, actor);

				logPosteriorLike += edgeLogProbs[d][j];
			}
		}

		double changeInLogLike = logPosteriorLike - lastLogPosteriorLike;

		if (changeInLogLike < Math.log(rng.nextDouble())) {

			latentSpaces[space][actor] = lastSpaceSample;

			for (int d = 0; d < numDocs; d++) {

				Email e = emails.getEmail(d);

				int author = e.getAuthor();

				if (author == actor) {

					for (int j = 0; j < numActors - 1; j++) {

						int r = e.getRecipient(j);
						resetToCache(space, d, j, actor, r);
					}
				} else {

					int j = e.getIndex(actor);
					resetToCache(space, d, j, author, actor);
				}
			}
			return 0;
		} else {

			interceptLogLike += changeInLogLike;
			positionLogLikes[actor] = logPosteriorLike;

			for (int d = 0; d < numDocs; d++) {

				Email e = emails.getEmail(d);

				int author = e.getAuthor();

				if (author == actor) {

					for (int j = 0; j < numActors - 1; j++) {

						int r = e.getRecipient(j);

						positionLogLikes[r] -= cachedEdgeLogProbs[d][j];
						positionLogLikes[r] += edgeLogProbs[d][j];

					}
				} else {

					int j = e.getIndex(actor);

					positionLogLikes[author] -= cachedEdgeLogProbs[d][j];
					positionLogLikes[author] += edgeLogProbs[d][j];
				}
			}

			return 1;
		}
	}

	public int sampleIntercept(int space) {

		spaceEdgeLogProbsCached = new boolean[numActors][numActors];

		double lastLogPosteriorLike = interceptLogLike;

		double lastIntercept = intercepts[space];

		double logPosteriorLike = 0;

		intercepts[space] = rng.nextGaussian(intercepts[space], 1.0);

		for (int d = 0; d < numDocs; d++) {

			Email e = emails.getEmail(d);

			int a = e.getAuthor();

			for (int j = 0; j < numActors - 1; j++) {

				int r = e.getRecipient(j);
				int y = e.getEdge(r);

				if (!spaceEdgeLogProbsCached[a][r]) {
					recalculateSpaceEdgeProb(space, a, r);
				}
				recalculatEdgeProb(space, d, j, y, a, r);

				logPosteriorLike += edgeLogProbs[d][j];
			}
		}

		double changeInLogLike = logPosteriorLike - lastLogPosteriorLike;

		if (changeInLogLike < Math.log(rng.nextDouble())) {

			intercepts[space] = lastIntercept;

			for (int d = 0; d < numDocs; d++) {
				Email e = emails.getEmail(d);
				int a = e.getAuthor();
				for (int j = 0; j < numActors - 1; j++) {
					int r = e.getRecipient(j);

					resetToCache(space, d, j, a, r);
				}
			}
			return 0;
		} else {

			positionLogLikes = new double[numActors];
			for (int d = 0; d < numDocs; d++) {

				Email e = emails.getEmail(d);

				int a = e.getAuthor();

				for (int j = 0; j < numActors - 1; j++) {

					int r = e.getRecipient(j);

					double edgeLogProb = edgeLogProbs[d][j];
					positionLogLikes[a] += edgeLogProb;
					positionLogLikes[r] += edgeLogProb;
				}
			}

			interceptLogLike = logPosteriorLike;
			return 1;
		}
	}

	private void recalculateSpaceEdgeProb(int space, int a, int r) {

		cachedSpaceEdgeLogProbs[space][a][r] = spaceEdgeLogProbs[space][a][r];
		cachedSpaceEdgeLogProbs[space][r][a] = spaceEdgeLogProbs[space][a][r];

		spaceEdgeLogProbs[space][a][r] = edgeScore.getLogScore(space, a, r, 1,
				latentSpaces, intercepts);
		spaceEdgeLogProbs[space][r][a] = spaceEdgeLogProbs[space][a][r];

		spaceEdgeLogProbsCached[a][r] = true;
		spaceEdgeLogProbsCached[r][a] = true;
	}

	private void recalculatEdgeProb(int space, int d, int j, int y, int a, int r) {

		double logP = edgeLogProbs[d][j];

		cachedEdgeLogProbs[d][j] = logP;

		double newComponent = logFeatureProportions[d][space]
				+ spaceEdgeLogProbs[space][a][r];
		double oldComponent = logFeatureProportions[d][space]
				+ cachedSpaceEdgeLogProbs[space][a][r];

		if (y == 1) {
			logP = Maths.sumLogProb(logP, newComponent);
			logP = Maths.subtractLogProb(logP, oldComponent);
		} else {
			logP = Maths.sumLogProb(logP, oldComponent);
			logP = Maths.subtractLogProb(logP, newComponent);
		}

		edgeLogProbs[d][j] = logP;
	}

	private void resetToCache(int space, int d, int j, int a, int r) {

		spaceEdgeLogProbs[space][a][r] = cachedSpaceEdgeLogProbs[space][a][r];
		spaceEdgeLogProbs[space][r][a] = spaceEdgeLogProbs[space][a][r];
		edgeLogProbs[d][j] = cachedEdgeLogProbs[d][j];
	}

	public void sampleMissingValue(int d, int j) {

		int a = emails.getEmail(d).getAuthor();
		int r = emails.getEmail(d).getRecipient(j);
		int y = emails.getEmail(d).getEdge(r);

		double logP = edgeLogProbs[d][j];

		int newValue = Math.log(rng.nextDouble()) < logP ? y : 1 - y;

		emails.getEmail(d).setEdge(r, newValue);

		if (newValue != y) {

			double oldLogProb = logP;
			double newLogProb = Math.log(1 - Math.exp(logP));

			positionLogLikes[a] -= oldLogProb;
			positionLogLikes[a] += newLogProb;

			positionLogLikes[r] -= oldLogProb;
			positionLogLikes[r] += newLogProb;

			interceptLogLike -= oldLogProb;
			interceptLogLike += newLogProb;

			edgeLogProbs[d][j] = newLogProb;
		}
	}

	public void generateEdges() {
		generateEdges(logFeatureProportions);
	}

	public double[] getPositionLogLikes() {
		return positionLogLikes;
	}

	public double calculateSpaceEdgeLogProb(int t, int a, int r) {
		return edgeScore.getLogScore(t, a, r, 1, latentSpaces, intercepts);
	}

	public double calculateEdgeLogProb(int d, int j) {
		double prob = 0;
		int a = emails.getDocument(d).getAuthor();
		int r = emails.getDocument(d).getRecipient(j);
		for (int t = 0; t < numFeatures; t++) {
			prob += Math.exp(logFeatureProportions[d][t])
					* Math.exp(calculateSpaceEdgeLogProb(t, a, r));
		}
		return Math.log(prob);
	}

	public boolean checkCacheConsistency() throws Exception {

		for (int t = 0; t < numFeatures; t++) {
			for (int a = 0; a < numActors; a++) {
				for (int r = 0; r < numActors; r++) {
					if (a != r) {
						double logProb = calculateSpaceEdgeLogProb(t, a, r);
						double cachedLogProb = spaceEdgeLogProbs[t][a][r];
						if (Math.abs(Math.exp(cachedLogProb)
								- Math.exp(logProb)) > 1e-12) {
							throw new Exception();
						}
					}
				}
			}
		}

		double[] tempPositionLogLikes = new double[numActors];
		double tempInterceptLogLike = 0;

		for (int d = 0; d < numDocs; d++) {
			for (int j = 0; j < numActors - 1; j++) {

				int a = emails.getDocument(d).getAuthor();
				int r = emails.getDocument(d).getRecipient(j);
				int y = emails.getDocument(d).getEdge(r);
				double logProb = calculateEdgeLogProb(d, j);
				logProb = y == 1 ? logProb : Math.log(1 - Math.exp(logProb));

				double cachedLogProb = edgeLogProbs[d][j];
				if (Math.abs(Math.exp(cachedLogProb) - Math.exp(logProb)) > 1e-12) {
					throw new Exception();
				}

				tempInterceptLogLike += logProb;
				tempPositionLogLikes[a] += logProb;
				tempPositionLogLikes[r] += logProb;
			}
		}

		for (int a = 0; a < numActors; a++) {
			if (Math.abs(Math.exp(positionLogLikes[a])
					- Math.exp(tempPositionLogLikes[a])) > 1e-12) {
				throw new Exception();
			}
		}

		if (Math.abs(Math.exp(interceptLogLike)
				- Math.exp(tempInterceptLogLike)) > 1e-12) {
			throw new Exception();
		}

		return true;
	}

	private void initializePositions() {

		for (int space = 0; space < numFeatures; space++) {
			for (int a = 0; a < numActors; a++) {
				for (int dim = 0; dim < numLatentDims; dim++) {
					latentSpaces[space][a][dim] = rng.nextGaussian();
				}
			}
		}
	}

	public void initializeIntercepts() {

		if (usingSingleIntercept) {
			Arrays.fill(intercepts, rng.nextGaussian());
		} else {
			for (int space = 0; space < numFeatures; space++) {
				intercepts[space] = rng.nextGaussian();
			}
		}
	}

	private void initializeMissingValues() {
		for (int d = 0; d < numDocs; d++) {
			Email e = emails.getEmail(d);
			for (int j : e.getMissingData()) {
				int r = e.getRecipient(j);
				if (rng.nextDouble() > 0.5) {
					e.setEdge(r, 0);
				} else {
					e.setEdge(r, 1);
				}
			}
		}
	}

	public double[] getIntercepts() {
		return intercepts;
	}

	public double[][][] getLatentSpaces() {
		return latentSpaces;
	}

	public void updateFeatureProportions(double[] docNorms,
			AssignmentScore assignmentScore) {

		for (int doc = 0; doc < numDocs; doc++) {
			for (int f = 0; f < numFeatures; f++) {
				logFeatureProportions[doc][f] = assignmentScore.getLogScore(
						doc, f);
			}
		}
		initializeCaches();
	}

	public void setFeatureProportions(double[][] logFeatureProportions) {
		this.logFeatureProportions = logFeatureProportions;
		initializeCaches();
	}
}
