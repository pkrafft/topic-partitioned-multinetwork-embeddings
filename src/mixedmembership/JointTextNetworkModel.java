package mixedmembership;

import java.util.Arrays;

import util.LogRandoms;
import data.EmailCorpus;
import data.InstanceListLoader;
import experiments.Model;

/**
 * This class provides the main functionality for this package. It initializes
 * the model, provides methods for running the estimation procedure, controls
 * output printing, and includes some additional functionality such as enforcing
 * time limits.
 */
public class JointTextNetworkModel implements Model {

	// when DEBUG = true, we check cache consistency and don't print final
	// states (to test that the experiment classes don't rely on those)
	public static boolean DEBUG = false;

	// print final states despite being in DEBUG mode. this is a hack for back
	// compatibility with some old tests
	public static boolean DEBUG_OVERRIDE = false;

	private EmailCorpus emails;

	private int numActors, numDocs, numWords, numFeatures, latentDim;

	private JointStructure model;
	private EdgeScore edgeScore;
	private EdgeScoreLatentSpaceMarginalizedAssignments alternativeLatentSpaceSampler;

	boolean usingWordModel, usingEdgeModel;
	boolean samplingIntercept;
	private boolean usingBernoulli;
	private boolean usingErosheva;
	private boolean usingBasicJointStructure;
	private boolean usingAlternativeSpaceSampler;
	private boolean usingMixtureModel;
	private boolean usingAsymmetric;
	private boolean disjointEdgeModelSampler;
	private boolean disjointLatentSpaceSampler;

	private LogRandoms rng;

	private MCMCPrinter printer;
	private int sample = 0;

	private long timeStart;
	private Long timeLimit;

	// some combinations of these parameters probably will not work, many are
	// not tested. the only ones that are tested are those represented in the
	// experiments classes.
	/**
	 * 
	 * @param emails
	 *            the data
	 * @param numFeatures
	 *            the number of mixture/admixture components to use
	 * @param latentDim
	 *            the dimension of the latent spaces
	 * @param rng
	 *            a random number generator
	 * @param usingWordModel
	 *            whether to model the word data
	 * @param usingEdgeModel
	 *            whether to model the recipient data
	 * @param sampleIntercept
	 *            whether to sample intercepts
	 * @param bernoulliModel
	 *            whether to use a bernoulli edge score
	 * @param eroshevaModel
	 *            whether to use an erosheva edge score
	 * @param exchangeableJointStructure
	 *            whether to use the basic model structure
	 * @param alternativeSpaceSampler
	 *            whether to marginalize edge assignments
	 * @param mixtureModel
	 *            whether to use a mixture instead of an admixture model
	 * @param usingAsymmetricEdgeModel
	 *            whether the probability of A connecting with B should be
	 *            constrained to be equal to B connecting with A (for the
	 *            Bernoulli model)
	 * @param disjointEdgeModelSampler
	 *            whether to sample only the edge model aspects --- the
	 *            assignments and the latent spaces. This is useful for
	 *            visualizing variances and for running MMLSM+LDA. Edge
	 *            assignments and latent spaces are randomly initialized. This
	 *            option is not meant to be used on top of models that already
	 *            had been trained on missing data or on data that could now be
	 *            missing.
	 * @param disjointLatentSpaceSampler
	 *            whether to sample only the latent spaces. This is useful for
	 *            visualizing variance, for running bernoulli+LSM, and for
	 *            running mixture+LSM. Latent spaces are randomly initialized.
	 *            This option is not meant to be used on top of models that
	 *            already had been trained on missing data or on data that could
	 *            now be missing.
	 * @param timeLimit
	 *            the maximum time after the object is created that estimation
	 *            is allowed to run for
	 */
	public JointTextNetworkModel(EmailCorpus emails, int numFeatures,
			int latentDim, LogRandoms rng, boolean usingWordModel,
			boolean usingEdgeModel, boolean sampleIntercept,
			boolean bernoulliModel, boolean eroshevaModel,
			boolean exchangeableJointStructure,
			boolean alternativeSpaceSampler, boolean mixtureModel,
			boolean usingAsymmetricEdgeModel, boolean disjointEdgeModelSampler,
			boolean disjointLatentSpaceSampler, Long timeLimit) {

		// the time starts counting here, not when estimation starts
		timeStart = System.currentTimeMillis();
		this.timeLimit = timeLimit;

		this.emails = emails;

		this.numFeatures = numFeatures;
		numActors = emails.getNumAuthors();
		numDocs = emails.size();
		if (emails.getWordDict() != null) {
			numWords = emails.getWordDict().size();
		}
		this.latentDim = latentDim;

		if (rng != null) {
			this.rng = rng;
		} else {
			this.rng = new LogRandoms();
		}

		this.usingWordModel = usingWordModel;
		this.usingEdgeModel = usingEdgeModel;
		this.samplingIntercept = sampleIntercept;
		this.usingBernoulli = bernoulliModel;
		this.usingErosheva = eroshevaModel;
		this.usingBasicJointStructure = exchangeableJointStructure;
		this.usingAlternativeSpaceSampler = alternativeSpaceSampler;
		this.usingMixtureModel = mixtureModel;
		this.usingAsymmetric = usingAsymmetricEdgeModel;
		this.disjointEdgeModelSampler = disjointEdgeModelSampler;
		this.disjointLatentSpaceSampler = disjointLatentSpaceSampler;
	}

	/**
	 * Initialize the model.
	 * 
	 * Much of the functionality of this method and the options that it
	 * activates depend on what file names are null. Generally, if a file name
	 * is null, the state/values associated with that file will not be printed
	 * or the state of the variable associated with that file will not be
	 * loaded.
	 */
	public void initialize(int[][] initWordAssignments,
			int[][] initEdgeAssignments, double[][][] initLatentSpaces,
			double[] initIntercepts, double[] alpha, double[] beta,
			double[] gamma, boolean sampleFromPrior, int printInterval,
			int saveStateInterval, String documentTopicsFileName,
			String wordStateFileName, String edgeStateFileName,
			String alphaFileName, String betaFileName, String gammaFileName,
			String latentSpaceFileName, String interceptFileName,
			String topicWordsFileName, String topicSummaryFileName,
			String logProbFileName, String logLikeFileName,
			String missingEdgeFileName, int iterOffset,
			String savedWordStateFileName, String savedEdgeStateFileName,
			String savedLatentSpaceFileName, String savedInterceptFileName,
			String savedAlphaFileName, String savedBetaFileName,
			String savedGammaFileName, String savedMissingEdgeFileName) {

		if (savedWordStateFileName != null) {
			initWordAssignments = InstanceListLoader.loadWordAssignments(
					savedWordStateFileName, numDocs);
		}
		if (savedEdgeStateFileName != null) {
			initEdgeAssignments = InstanceListLoader.loadEdgeAssignments(
					savedEdgeStateFileName, numDocs, numActors);
		}
		if (savedLatentSpaceFileName != null) {
			initLatentSpaces = InstanceListLoader
					.loadLatentSpaces(savedLatentSpaceFileName, latentDim,
							numActors, numFeatures);
			initIntercepts = InstanceListLoader.loadIntercepts(
					savedInterceptFileName, numFeatures);
		}
		if (savedAlphaFileName != null) {
			alpha = InstanceListLoader.loadHyper(savedAlphaFileName,
					alpha.length);
		}
		if (savedBetaFileName != null) {
			beta = InstanceListLoader.loadHyper(savedBetaFileName, beta.length);
		}
		if (savedGammaFileName != null) {
			gamma = InstanceListLoader.loadHyper(savedGammaFileName,
					gamma.length);
		}

		boolean initializeMissingValues = true;
		if (savedMissingEdgeFileName != null) {
			InstanceListLoader
					.loadPredictions(emails, savedMissingEdgeFileName);
			initializeMissingValues = false;
		}

		sample = iterOffset;

		// initialize these to 10 because distance between actors can only
		// decrease the probability of connecting
		if (initIntercepts == null) {
			initIntercepts = new double[numFeatures];
			Arrays.fill(initIntercepts, 10.0);
		}

		// these edge scores are constructed here instead of inside the
		// JointStructure constructor to avoid passing lots of unnecessary
		// parameters around
		if (usingBernoulli && !usingErosheva && !usingAsymmetric) {
			edgeScore = new EdgeScoreBernoulliSymmetric();
		} else if (usingBernoulli && !usingErosheva) {
			edgeScore = new EdgeScoreBernoulliAsymmetric();
		} else if (usingErosheva) {
			edgeScore = new EdgeScoreBernoulliErosheva();
		} else if (usingEdgeModel) {
			edgeScore = new EdgeScoreLatentSpace(numFeatures, latentDim,
					emails, initLatentSpaces, initIntercepts, rng);
		}

		if (!usingBasicJointStructure && !usingMixtureModel) {
			model = new JointStructureConditional();
		} else if (!usingMixtureModel) {
			model = new JointStructureExchangeable();
		} else {
			model = new JointStructureMixture();
		}
		model.initialize(numFeatures, emails, initWordAssignments,
				initEdgeAssignments, alpha, beta, gamma, sampleFromPrior, rng,
				edgeScore, initializeMissingValues, usingWordModel,
				usingEdgeModel);

		if (usingAlternativeSpaceSampler) {
			alternativeLatentSpaceSampler = new EdgeScoreLatentSpaceMarginalizedAssignments(
					numFeatures, latentDim, emails,
					((EdgeScoreLatentSpace) edgeScore).getLatentSpaces(),
					((EdgeScoreLatentSpace) edgeScore).getIntercepts(), rng);
			alternativeLatentSpaceSampler.updateFeatureProportions(model
					.getAssignmentScore().getLogDocNorms(), model
					.getAssignmentScore());
		}

		printer = new MCMCPrinter(emails, model, edgeScore, printInterval,
				saveStateInterval, documentTopicsFileName, wordStateFileName,
				edgeStateFileName, alphaFileName, betaFileName, gammaFileName,
				latentSpaceFileName, interceptFileName, missingEdgeFileName,
				topicWordsFileName, topicSummaryFileName, logProbFileName,
				logLikeFileName);

		if (iterOffset == 0) {
			double logLike = logLike();
			double logProb = logProb(logLike);
			printer.printInitial(numActors, numDocs, numWords, numFeatures,
					latentDim, logProb, logLike);
		}

		if (DEBUG) {
			try {
				if (usingEdgeModel) {
					edgeScore.checkCacheConsistency(model);
				}
				if (usingAlternativeSpaceSampler) {
					((EdgeScoreLatentSpaceMarginalizedAssignments) alternativeLatentSpaceSampler)
							.checkCacheConsistency();
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

	}

	// TODO: add back in functionality for single intercept?
	/**
	 * Estimate the parameters of the model and fill in any missing data.
	 * 
	 * @param numIterations
	 *            how many iterations to perform sampling for
	 * @param sampleHypers
	 *            whether to sample the hyperparameters along with the
	 *            parameters
	 * @return the number of iterations that completed before the time limit
	 */
	public int estimate(int numIterations, boolean sampleHypers) {

		double proposalVar = 1.0;

		int iter = 0;
		while (iter < numIterations) {
			iter++;
			sample++;

			// start with high proposal variance to explore the space more in
			// the Metropolis-Hastings samples
			if (sample <= 100) {
				proposalVar = 100.0 / sample;
			}

			try {

				// sample assignments

				if (numFeatures > 1 && !disjointLatentSpaceSampler) {

					if ((usingWordModel || usingMixtureModel)
							&& !disjointEdgeModelSampler) {

						for (int d = 0; d < numDocs; d++) {
							for (int i = 0; i < model.getNumTokenAssignments(d); i++) {
								model.sampleTokenAssignment(d, i, false);
							}
						}
					}

					if (usingEdgeModel && !usingMixtureModel) {

						for (int d = 0; d < numDocs; d++) {
							for (int j = 0; j < numActors - 1; j++) {
								model.sampleEdgeAssignment(d, j, false);
							}
						}
					}
				}

				// sample latent spaces

				// if there are latent spaces and its a standard model
				if (usingEdgeModel && !usingBernoulli
						&& !usingAlternativeSpaceSampler) {

					for (int t = 0; t < numFeatures; t++) {
						for (int a = 0; a < numActors; a++) {

							int accepted = ((EdgeScoreLatentSpace) edgeScore)
									.samplePosition(t, a, proposalVar);

							printer.positionProposal(accepted);
						}
					}

					if (samplingIntercept) {

						for (int t = 0; t < numFeatures; t++) {

							int accepted = ((EdgeScoreLatentSpace) edgeScore)
									.sampleIntercept(t, proposalVar);

							printer.interceptProposal(accepted);
						}
					}
				}

				// sample missing data

				model.sampleMissingEdges();

				// sample hyperparameters

				if (sampleHypers) {

					if (!disjointEdgeModelSampler) {

						model.sampleAlpha(5, 1.0);

						if (usingWordModel) {
							model.sampleBeta(5, 1.0);
						}
					}

					if (usingBernoulli) {
						model.sampleGamma(5, 1.0);
					}
				}

				if (usingEdgeModel && DEBUG) {
					edgeScore.checkCacheConsistency(model);
				}

				// this code is weird because of some legacy code that made the
				// order of the sampling important and abnormal
				if (usingAlternativeSpaceSampler) {

					alternativeLatentSpaceSampler.updateFeatureProportions(
							model.getAssignmentScore().getLogDocNorms(),
							model.getAssignmentScore());

					// TODO: add proposal variance as a parameter

					for (int t = 0; t < numFeatures; t++) {
						for (int a = 0; a < numActors; a++) {
							int accepted = alternativeLatentSpaceSampler
									.samplePosition(t, a);
							printer.positionProposal(accepted);
						}
					}

					if (samplingIntercept) {
						for (int t = 0; t < numFeatures; t++) {
							int accepted = alternativeLatentSpaceSampler
									.sampleIntercept(t);
							printer.interceptProposal(accepted);
						}
					}

					((EdgeScoreLatentSpace) model.getEdgeScore()).clearCache();

					if (DEBUG) {
						((EdgeScoreLatentSpaceMarginalizedAssignments) alternativeLatentSpaceSampler)
								.checkCacheConsistency();
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}

			double logLike = logLike();
			double logProb = logProb(logLike);

			long time = System.currentTimeMillis();

			// if the time limit is exceeded
			if (timeLimit != null && (time - timeStart) > timeLimit) {

				// save state and end the estimation
				printer.printIteration(sample, logProb, logLike, true);
				break;
			} else {
				printer.printIteration(sample, logProb, logLike, false);
			}
		}

		if (!DEBUG || DEBUG_OVERRIDE) {
			printer.printFinal();
		}
		return iter;
	}

	/**
	 * Calculate the log likelihood of the current model parameters.
	 * 
	 * @return log P(w, y | ...)
	 */
	public double logLike() {
		double logLike = 0;
		if (usingWordModel) {
			logLike += model.logWordDataProb();
		}
		if (usingEdgeModel && !usingAlternativeSpaceSampler) {
			logLike += model.logEdgeDataProb();
		}
		if (usingAlternativeSpaceSampler) {
			logLike += alternativeLatentSpaceSampler.logEdgeDataProb();
		}
		return logLike;
	}

	public double logProb() {
		return logProb(logLike());
	}

	/**
	 * Calculate the joint probability of data and the current model parameters.
	 * 
	 * @return P(w, y, z, x, S, b | ...)
	 */
	public double logProb(double logLike) {
		double logProb = logLike;
		if (!usingAlternativeSpaceSampler) {
			logProb += model.logPriorProb();
		} else {
			// TODO: implement prior on doc-feature proportions?
		}
		return logProb;
	}

	public double[] getAlpha() {
		return model.getAssignmentScore().getAlpha();
	}

	public JointStructure getModel() {
		return model;
	}

	public AssignmentScore getAssignmentScore() {
		return model.getAssignmentScore();
	}

	public EdgeScore getEdgeScore() {
		return this.edgeScore;
	}

	public JointStructure getAssignmentModel() {
		return this.model;
	}

	public EmailCorpus getEmails() {
		return emails;
	}

	public void setEmails(EmailCorpus emails) {
		this.emails = emails;
	}

	public EdgeScoreLatentSpaceMarginalizedAssignments getAlternativeLatentSpaceSampler() {
		return alternativeLatentSpaceSampler;
	}

	public double[] getBeta() {
		return model.getBeta();
	}

	public double[] getGamma() {
		return model.getGamma();
	}

	@Override
	public double getDocEdgeLogLike(int d) {
		return model.getDocEdgeLogLike(d, false);
	}
}
