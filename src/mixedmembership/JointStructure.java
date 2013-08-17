package mixedmembership;

import util.LogRandoms;
import util.SliceSampler;
import data.Email;
import data.EmailCorpus;

/**
 * The primary class that determines the structure of the model being used.
 * 
 * This package is roughly organized according to the graphical models of the
 * possible joint text and network models. This class essentially represents the
 * large plate containing both types of data and their assignments to
 * mixture/admixture components, i.e. it represents the general structure of the
 * model. Viewed another way, this class represents the nature of the connection
 * between the model's distributions, the model's latent variables, and the
 * data.
 * 
 * Each specific choice of distribution used in the model (i.e. the prior over
 * the assignments, the generative process for the words, and the generative
 * process for the recipients) is represented by a "Score" class. These classes
 * provide functionality for calculating and caching the necessary likelihood
 * functions, conditional probabilities, and joint probabilities, and for
 * sampling the parameters/hyperparameters associated with those distributions.
 * 
 * For a conceptual example, a score could be a Gaussian distribution. This says
 * nothing about what data the Gaussian is modeling, though. The model structure
 * connects the Gaussian score to the data, perhaps being modeled as drawn from
 * independent Gaussian distributions.
 * 
 * This class thus provides the basic functionality for sampling assignments and
 * computing relevant probabilities such as priors and likelihoods.
 */
public abstract class JointStructure {

	EmailCorpus emails;

	WordScore wordScore;
	AssignmentScore assignmentScore;

	EdgeScore edgeScore;

	// assignments of token to topics
	int[][] tokenAssignments;

	// assignments of edges either to topics or to tokens depending on the model
	int[][] edgeAssignments;

	boolean usingWordModel, usingEdgeModel;

	int numDocs, numActors, numWords, numFeatures;

	LogRandoms rng;

	private SliceSampler alphaSampler;
	private SliceSampler betaSampler;
	private SliceSampler gammaSampler;

	/**
	 * 
	 * @param T
	 *            number of mixture/admixture components to use
	 * @param emails
	 *            the data
	 * @param initWordAssignments
	 *            initial token assignments to use
	 * @param initEdgeAssignments
	 *            initial edge assignments values to use
	 * @param alpha
	 *            initial Dirichlet concentration for topic proportions
	 * @param beta
	 *            initial Dirichlet concentration for word proportions
	 * @param sampleFromPrior
	 *            whether to sample initial assignments from their priors
	 * @param rng
	 *            random number generator
	 * @param edgeScore
	 *            the edge score to use with this model
	 * @param initializeMissingValues
	 *            initial values for held-out edges
	 * @param usingWordModel
	 *            whether this model has a component for describing the word
	 *            data
	 * @param usingEdgeModel
	 *            whether this model has a component for describing the edge
	 *            data
	 */
	public void initialize(int T, EmailCorpus emails,
			int[][] initWordAssignments, int[][] initEdgeAssignments,
			double[] alpha, double[] beta, double[] gamma,
			boolean sampleFromPrior, LogRandoms rng, EdgeScore edgeScore,
			boolean initializeMissingValues, boolean usingWordModel,
			boolean usingEdgeModel) {

		numDocs = emails.size();
		numActors = emails.getNumAuthors();
		if (emails.getWordDict() != null) {
			numWords = emails.getWordDict().size();
		}
		numFeatures = T;

		this.emails = emails;

		assignmentScore = initializeAssignmentScore(alpha);
		alphaSampler = new SliceSampler(assignmentScore, rng);

		if (usingWordModel) {
			wordScore = new WordScore(numWords, numFeatures, beta, this);
			betaSampler = new SliceSampler(wordScore, rng);
		}

		this.edgeScore = edgeScore;
		if (edgeScore instanceof EdgeScoreBernoulli) {
			((EdgeScoreBernoulli) edgeScore).initialize(numFeatures, gamma,
					this);
			gammaSampler = new SliceSampler((EdgeScoreBernoulli) edgeScore, rng);
		}

		this.rng = rng;

		this.usingWordModel = usingWordModel;
		this.usingEdgeModel = usingEdgeModel;

		initializeDataStructures(initWordAssignments, initEdgeAssignments,
				initializeMissingValues, sampleFromPrior);
	}

	// determines what assignment score this model will use
	protected AssignmentScore initializeAssignmentScore(double[] alpha) {
		return new AssignmentScore(numDocs, numFeatures, alpha, this);
	}

	protected void initializeDataStructures(int[][] initWordAssignments,
			int[][] initEdgeAssignments, boolean initializeMissingEdges,
			boolean sampleFromPrior) {

		// Note: order of initialization matters.
		if (initializeMissingEdges) {
			initializeMissingEdges();
		}
		if (usingWordModel || usingMixtureModel()) {
			initializeTokenAssignments(initWordAssignments, sampleFromPrior);
		}
		if (usingEdgeModel && !usingMixtureModel()) {
			initializeEdgeAssignments(initEdgeAssignments, sampleFromPrior);
		}
	}

	/**
	 * Compute the probability of the word data given the current settings of
	 * the model parameters.
	 * 
	 * This method computes P(w | z) from "left-to-right" using the product of
	 * the conditional distributions, P(w_1 | z) P(w_2 | w_1, z) ... P(w_n |
	 * w_1, ..., w_{n-1}, z).
	 * 
	 * @return the likelihood P(w | z)
	 */
	public double logWordDataProb() {

		double logProb = 0;

		wordScore.resetCounts();

		for (int d = 0; d < numDocs; d++) {

			int[] words = emails.getDocument(d).getTokens();
			int nd = emails.getDocument(d).getLength();

			for (int i = 0; i < nd; i++) {

				int w = words[i];
				int t = getTokenTopicAssignment(d, i);

				logProb += wordScore.getLogScore(t, w);

				wordScore.incrementCounts(t, w);
			}
		}

		return logProb;
	}

	// this makes sense to have as a method if you look in JointStructureMixture
	protected int getTokenTopicAssignment(int d, int i) {
		return tokenAssignments[d][i];
	}

	/**
	 * Compute the probability of the edge data given the current settings of
	 * the model parameters.
	 * 
	 * This method computes P(y | x, z, ...) from "left-to-right" using the
	 * product of the conditional distributions. This only actually makes a
	 * difference when edgeScore is Resettable, otherwise the likelihood factors
	 * into independent terms.
	 * 
	 * @return the likelihood P(y | x, z, ...)
	 */
	public double logEdgeDataProb() {

		double logProb = 0;

		if (edgeScore instanceof Resettable) {
			((Resettable) edgeScore).resetCounts();
		}

		for (int d = 0; d < numDocs; d++) {
			logProb += getDocEdgeLogLike(d, edgeScore instanceof Resettable);
		}

		return logProb;
	}

	// computes the likelihood of the edges of one document
	public double getDocEdgeLogLike(int d, boolean countsReset) {

		double logProb = 0;

		Email email = emails.getEmail(d);
		int a = email.getAuthor();

		for (int j = 0; j < numActors - 1; j++) {

			int r = email.getRecipient(j);
			int t = getEdgeTopicAssignment(d, j);
			int y = email.getEdge(r);

			logProb += edgeScore.getLogProb(t, a, r, y);

			if (countsReset) {
				edgeScore.incrementCounts(t, a, r, y);
			}
		}

		return logProb;
	}

	/**
	 * 
	 * @param d
	 *            a document
	 * @param j
	 *            an index (into the length A - 1 representation) of a recipient
	 *            in that document
	 * @return the mixture/admixture component that the edge is assigned to
	 */
	protected abstract int getEdgeTopicAssignment(int d, int j);

	/**
	 * Compute the probability of the current assignment parameters according to
	 * the prior distribution.
	 * 
	 * This method computes P(x, z) from "left-to-right" using the product of
	 * the conditional distributions.
	 * 
	 * We assume an improper uniform prior on the latent spaces and intercepts,
	 * so this method actually calculates the entire prior.
	 * 
	 * @return the prior probability of the model parameters.
	 */
	public double logPriorProb() {

		double logProb = 0;

		assignmentScore.resetCounts();

		if (usingWordModel || usingMixtureModel()) {
			logProb += logPriorProbWordAssignmentComponent();
		}
		if (usingEdgeModel && !usingMixtureModel()) {
			logProb += logPriorProbEdgeAssignmentComponent();
		}

		return logProb;
	}

	private double logPriorProbWordAssignmentComponent() {

		double logProb = 0;

		for (int d = 0; d < numDocs; d++) {

			int nd = getNumTokenAssignments(d);

			for (int i = 0; i < nd; i++) {
				int t = getTokenTopicAssignment(d, i);
				logProb += assignmentScore.getLogScore(d, t);
				assignmentScore.incrementCounts(d, t);
			}
		}
		return logProb;
	}

	protected abstract double logPriorProbEdgeAssignmentComponent();

	/**
	 * Gibbs sample a token assignment from its full conditional distribution.
	 * 
	 * @param d
	 *            an index of a document
	 * @param i
	 *            an index of a token in that document
	 * @param init
	 *            whether this being called by an initialization method
	 * @return 1 if the sample is different from before, 0 otherwise
	 */
	public int sampleTokenAssignment(int d, int i, boolean init) {

		Email email = emails.getEmail(d);

		int a = email.getAuthor();
		int w = -1;
		if (email.getLength() != 0) {
			w = email.getToken(i);
		}

		int oldAssignment = getTokenTopicAssignment(d, i);

		double[] dist = getTokenAssignmentDistribution(d, i, a, w, init,
				oldAssignment);

		int newAssignment = rng.nextDiscreteLogDist(dist);

		// update data structures
		assignToken(d, i, a, w, newAssignment);

		return oldAssignment == newAssignment ? 0 : 1;
	}

	/**
	 * Compute the full conditional distribution for a token assignment
	 * 
	 * @param d
	 *            an index of a document
	 * @param i
	 *            an index of a token in that document
	 * @param a
	 *            the author of that document
	 * @param w
	 *            the word type of that token
	 * @param init
	 *            whether this method is being called from an initialization
	 *            method
	 * @param oldAssignment
	 *            the last value of the token assignment
	 * @return a distribution in log space
	 */
	protected double[] getTokenAssignmentDistribution(int d, int i, int a,
			int w, boolean init, int oldAssignment) {

		if (!init) {
			// keep caches up-to-date
			unassignToken(d, i, a, w, oldAssignment);
		}

		double[] dist = new double[numFeatures];

		for (int t = 0; t < numFeatures; t++) {
			dist[t] = assignmentScore.getLogValue(d, t);

			if (!init) { // if not computing prior distribution
				dist[t] += getTokenAssignmentLogLike(d, i, a, w, t);
			}
			assert dist[t] < Double.POSITIVE_INFINITY;
		}
		return dist;
	}

	protected double getTokenAssignmentLogLike(int d, int i, int a, int w, int t) {
		return wordScore.getLogScore(t, w);
	}

	// for keeping data structures and caches up-to-date
	protected void unassignToken(int d, int i, int a, int w, int t) {
		assignmentScore.decrementCounts(d, t);
		if (emails.getEmail(d).getLength() != 0) {
			wordScore.decrementCounts(t, w);
		}
	}

	// for keeping data structures and caches up-to-date
	protected void assignToken(int d, int i, int a, int w, int t) {

		tokenAssignments[d][i] = t;

		assignmentScore.incrementCounts(d, t);
		if (emails.getEmail(d).getLength() != 0) {
			wordScore.incrementCounts(t, w);
		}
	}

	/**
	 * Gibbs sample an edge assignment from its full conditional distribution.
	 * 
	 * @param d
	 *            an index of a document
	 * @param j
	 *            an index (into the length A - 1 recipient representation) of a
	 *            recipient
	 * @param init
	 *            whether this is sampling from the prior distribution
	 * @return 1 if the sample is different from the last one
	 */
	public int sampleEdgeAssignment(int d, int j, boolean init) {

		Email email = emails.getEmail(d);

		int a = email.getAuthor();
		int r = email.getRecipient(j);
		int y = email.getEdge(r);

		int oldAssignment = edgeAssignments[d][j];

		double[] dist = getEdgeAssignmentDistribution(d, j, a, r, y,
				oldAssignment, init);

		// TODO sample in log space
		// TODO change all discrete samples to use unnormalized CDF method
		int newAssignment = rng.nextDiscreteLogDist(dist);

		// keep data structures up-to-date
		assignEdge(d, j, a, r, y, newAssignment);

		return oldAssignment == newAssignment ? 0 : 1;
	}

	protected double[] getEdgeAssignmentDistribution(int d, int j, int a,
			int r, int y, int oldAssignment, boolean init) {

		if (!init) {
			// keep data structures up-to-date
			unassignEdge(d, j, a, r, y, oldAssignment);
		}

		double[] dist = initializeEdgeAssignmentDistribution(d);

		for (int i = 0; i < dist.length; i++) {
			dist[i] = getEdgeAssignmentScore(d, i, a, r, y, init);
		}

		return dist;
	}

	protected void unassignEdge(int d, int j, int a, int r, int y, int t) {
		edgeScore.decrementCounts(t, a, r, y);
	}

	protected void assignEdge(int d, int j, int a, int r, int y, int t) {
		edgeScore.incrementCounts(t, a, r, y);
	}

	protected abstract double[] initializeEdgeAssignmentDistribution(int d);

	/**
	 * Compute the terms of the likelihood function involving this edge
	 * assignment.
	 * 
	 * @param d
	 *            an index of a document
	 * @param i
	 *            the value of this assignment
	 * @param a
	 *            the id of an actor
	 * @param r
	 *            the id of another actor
	 * @param y
	 *            the value of the edge
	 * @param init
	 *            whether to sample from the prior distribution
	 * @return the relevant log score
	 */
	protected abstract double getEdgeAssignmentScore(int d, int i, int a,
			int r, int y, boolean init);

	private void initializeTokenAssignments(int[][] zInit,
			boolean sampleFromPrior) {

		tokenAssignments = new int[numDocs][];
		for (int d = 0; d < numDocs; d++) {
			int nd = getNumTokenAssignments(d);
			tokenAssignments[d] = new int[nd];
		}

		for (int d = 0; d < numDocs; d++) {

			Email e = emails.getEmail(d);
			int a = e.getAuthor();

			for (int i = 0; i < tokenAssignments[d].length; i++) {

				int w = -1;
				if (e.getLength() != 0) {
					w = e.getToken(i);
				}

				if (zInit == null && sampleFromPrior) {
					// sample and assign
					sampleTokenAssignment(d, i, true);
				} else if (zInit == null) {
					// update data structures with uniformly random value
					assignToken(d, i, a, w, rng.nextInt(numFeatures));
				} else {
					// update data structures with initial value
					assignToken(d, i, a, w, zInit[d][i]);
				}
			}
		}
	}

	/**
	 * 
	 * @param d
	 *            an index of a document
	 * @return the number of topic assignments that document (may be different
	 *         than the number of tokens in the conditional and mixture
	 *         structures)
	 */
	protected abstract int getNumTokenAssignments(int d);

	private void initializeEdgeAssignments(int[][] xInit,
			boolean sampleFromPrior) {

		edgeAssignments = new int[numDocs][numActors - 1];

		for (int d = 0; d < numDocs; d++) {

			Email e = emails.getEmail(d);
			int a = e.getAuthor();

			for (int j = 0; j < numActors - 1; j++) {

				int r = e.getRecipient(j);
				int y = e.getEdge(r);

				if (xInit == null && sampleFromPrior) {
					// sample and assign
					sampleEdgeAssignment(d, j, true);
				} else if (xInit == null) {
					// update data structures with uniformly random value
					assignEdge(d, j, a, r, y, getRandomEdgeAssignment(d, j));
				} else {
					// update data structures with given initial value
					assignEdge(d, j, a, r, y, xInit[d][j]);
				}
			}
		}
	}

	protected abstract int getRandomEdgeAssignment(int d, int j);

	/**
	 * Assign uniformly random values to missing edges.
	 */
	private void initializeMissingEdges() {
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

	/**
	 * Sample missing values from their full conditional distributions.
	 */
	public void sampleMissingEdges() {
		for (int d = 0; d < numDocs; d++) {
			Email e = emails.getEmail(d);
			for (int j : e.getMissingData()) {
				sampleEdge(d, j);
			}
		}
	}

	public void sampleAllEdges() {
		for (int d = 0; d < numDocs; d++) {
			for (int j = 0; j < numActors - 1; j++) {
				sampleEdge(d, j);
			}
		}
	}

	private void sampleEdge(int d, int j) {

		Email e = emails.getEmail(d);

		int a = e.getAuthor();
		int r = e.getRecipient(j);
		int t = getEdgeTopicAssignment(d, j);
		int y = e.getEdge(r);

		double p = getMissingEdgeDistribution(t, a, r, y);

		if (Math.log(rng.nextDouble()) > p) {
			edgeScore.incrementCounts(t, a, r, 1 - y);
			e.setEdge(r, 1 - y);
		} else {
			edgeScore.incrementCounts(t, a, r, y);
		}
	}

	protected double getMissingEdgeDistribution(int t, int a, int r, int y) {
		// decrementing the counts only actually has an effect for the Bernoulli
		// models
		edgeScore.decrementCounts(t, a, r, y);
		return edgeScore.getLogProb(t, a, r, y);
	}

	public void sampleAlpha(int numIterations, double stepSize) {
		alphaSampler.sampleParameters(numIterations, stepSize);
	}

	public void sampleBeta(int numIterations, double stepSize) {
		betaSampler.sampleParameters(numIterations, stepSize);
	}

	public void sampleGamma(int numIterations, double stepSize) {
		gammaSampler.sampleParameters(numIterations, stepSize);
	}

	public EmailCorpus getEmails() {
		return emails;
	}

	public int[][] getWordAssignments() {
		return tokenAssignments;
	}

	public WordScore getWordScore() {
		return wordScore;
	}

	public EdgeScore getEdgeScore() {
		return edgeScore;
	}

	public int[][] getEdgeAssignments() {
		return edgeAssignments;
	}

	public double[] getAlpha() {
		return assignmentScore.getAlpha();
	}

	public double[] getBeta() {
		return wordScore.getAlpha();
	}

	public AssignmentScore getAssignmentScore() {
		return assignmentScore;
	}

	public double[] getGamma() {
		return ((EdgeScoreBernoulli) edgeScore).getGamma();
	}

	public boolean usingMixtureModel() {
		return false;
	}
}
