package experiments;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import mixedmembership.EdgeScoreLatentSpace;
import mixedmembership.JointTextNetworkModel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import util.LogRandoms;
import cc.mallet.util.Randoms;
import data.EmailCorpus;

/**
 * The idea behind these tests is simply to make sure the experiment classes are
 * actually running the models we think they are running (which have been tested
 * in the mixedmembership class). All these tests do is run the experiments
 * directly and separately explicitly create the models that the experiments
 * should be making. The difficult part about these tests is getting the random
 * number generators to sync up, but all of that is taken care of in this
 * abstract class, so writing tests for new experiment classes is pretty easy as
 * long as their model structure is relatively similar to the existing model
 * structures.
 */
public abstract class ExperimentTest {

	EmailCorpus emails;

	Random temp;
	int seed1;
	int seed2;

	int T;
	int K;
	double alphaBase;
	double betaBase;
	double gammaBase;
	double[] alpha;
	double[] beta;
	double[] gamma;
	int numIter;
	String wordMatrixFile;
	String edgeMatrixFile;
	String vocabFile;
	String wordStateFileName;
	String edgeStateFileName;
	String alphaFileName;
	String betaFileName;
	String gammaFileName;
	String missingEdgesFileName;
	String latentSpaceFileName;
	String interceptFileName;

	@Before
	public void setUp() {

		JointTextNetworkModel.DEBUG = true;
		JointTextNetworkModel.DEBUG_OVERRIDE = false;
		
		temp = new Random();

		T = temp.nextInt(10) + 1;
		K = temp.nextInt(10);
		alphaBase = temp.nextDouble() * 10;
		betaBase = temp.nextDouble() * 10;
		gammaBase = temp.nextDouble() * 10;
		alpha = new double[] { alphaBase };
		beta = new double[] { betaBase };
		gamma = new double[] { gammaBase };

		wordMatrixFile = "word-matrix.csv";
		edgeMatrixFile = "edge-matrix.csv";
		vocabFile = "vocab.txt";

		wordStateFileName = "word_state.txt.gz";
		edgeStateFileName = "edge_state.txt.gz";
		alphaFileName = "alpha.txt";
		betaFileName = "beta.txt";
		gammaFileName = "gamma.txt";
		latentSpaceFileName = "latent_spaces.txt";
		interceptFileName = "intercepts.txt";
		missingEdgesFileName = "missing-edges.txt";

		seed1 = temp.nextInt();
		seed2 = temp.nextInt();

		// create and save a random dataset
		initializeEmails(seed1);
		emails.writeToFiles(wordMatrixFile, edgeMatrixFile, vocabFile);
	}

	@After
	public void tearDown() {

		System.out.println(emails);

		(new File("predictions.txt")).delete();
		(new File("doc_topics.txt.gz")).delete();
		(new File("topic_words.txt.gz")).delete();
		(new File("options.txt")).delete();
		(new File("topic_summary.txt.gz")).delete();
		(new File(wordMatrixFile)).delete();
		(new File(edgeMatrixFile)).delete();
		(new File(vocabFile)).delete();
		(new File(wordStateFileName)).delete();
		(new File(edgeStateFileName)).delete();
		(new File(latentSpaceFileName)).delete();
		(new File(interceptFileName)).delete();
		(new File(alphaFileName)).delete();
		(new File(betaFileName)).delete();
		(new File(gammaFileName)).delete();
		(new File(alphaFileName + ".true")).delete();
		(new File(betaFileName + ".true")).delete();
		(new File(gammaFileName + ".true")).delete();
		(new File(missingEdgesFileName)).delete();
		(new File(wordStateFileName + ".true")).delete();
		(new File(edgeStateFileName + ".true")).delete();
		(new File(latentSpaceFileName + ".true")).delete();
		(new File(interceptFileName + ".true")).delete();
		(new File(missingEdgesFileName + ".true")).delete();
		for (int i = 1; i <= numIter; i++) {
			(new File("options." + (i - 1) + ".txt")).delete();
			(new File("topic_summary.txt.gz." + i)).delete();
			(new File(wordStateFileName + "." + i)).delete();
			(new File(edgeStateFileName + "." + i)).delete();
			(new File(latentSpaceFileName + "." + i)).delete();
			(new File(interceptFileName + "." + i)).delete();
			(new File(alphaFileName + "." + i)).delete();
			(new File(betaFileName + "." + i)).delete();
			(new File(gammaFileName + "." + i)).delete();
			(new File(alphaFileName + ".true." + i)).delete();
			(new File(betaFileName + ".true." + i)).delete();
			(new File(gammaFileName + ".true." + i)).delete();
			(new File(missingEdgesFileName + "." + i)).delete();
			(new File(wordStateFileName + ".true" + "." + i)).delete();
			(new File(edgeStateFileName + ".true" + "." + i)).delete();
			(new File(latentSpaceFileName + ".true" + "." + i)).delete();
			(new File(interceptFileName + ".true" + "." + i)).delete();
			(new File(missingEdgesFileName + ".true" + "." + i)).delete();
		}
	}

	public void initializeEmails(int seed) {
		Randoms temp = new Randoms(seed);
		emails = new EmailCorpus(10, 100, 20, 10, temp);
	}

	public abstract JointTextNetworkModel initializeModel(LogRandoms rng);

	// define command line arguments
	public abstract String[] getFirstExperimentArgs(int iter);

	// define command line arguments
	public abstract String[] getNextExperimentArgs(int iter, int offset);

	public abstract JointTextNetworkModel runExperiment(String[] args,
			LogRandoms rng) throws IOException;

	// TODO: sample hypers
	// testing experiment class, using multiple calls for estimation,
	// reading/writing from files
	@Test
	public void test() {

		Experiment.batchSize = 2;
		numIter = temp.nextInt(10);

		JointTextNetworkModel model = trainTrueModel();

		JointTextNetworkModel mainModel = trainTestModel();

		double truePrior = model.getAssignmentModel().logPriorProb();
		double testPrior = mainModel.getAssignmentModel().logPriorProb();
		assertEquals(truePrior, testPrior, 1e-12);

		double trueValue = model.logProb(model.logLike());
		double testValue = mainModel.logProb(mainModel.logLike());
		assertEquals(trueValue, testValue, 1e-12);

		otherAssertions(model, mainModel);
	}

	// run experiment directly, using multiple calls to the experiment procedure
	private JointTextNetworkModel trainTestModel() {

		LogRandoms rng = new LogRandoms(seed2);

		int firstNumIter = numIter > 0 ? temp.nextInt(numIter) + 1 : 0;

		String[] args = getFirstExperimentArgs(firstNumIter);

		JointTextNetworkModel mainModel = null;
		try {
			mainModel = runExperiment(args, rng);
		} catch (IOException e) {
			System.out.println(emails);
			e.printStackTrace();
			System.exit(-1);
		}

		if (numIter > 0) {

			int itersDone = firstNumIter;

			while (itersDone < numIter) {

				int nextNumIter = temp.nextInt(numIter - itersDone + 1);

				args = getNextExperimentArgs(nextNumIter, itersDone);

				try {
					mainModel = runExperiment(args, rng);
				} catch (IOException e) {
					System.out.println(emails);
					e.printStackTrace();
					System.exit(-1);
				}

				itersDone += nextNumIter;
			}
		}
		return mainModel;
	}

	// explicitly create the right model
	private JointTextNetworkModel trainTrueModel() {

		LogRandoms rng = new LogRandoms(seed2);

		if (usingEdgeModel()) {
			emails.obscureRandomEdges(0.1, rng);
		}

		JointTextNetworkModel model = initializeModel(rng);

		model.estimate(numIter, true);

		return model;
	}

	// various assertions that are useful for different experiments:
	
	public abstract void otherAssertions(JointTextNetworkModel model,
			JointTextNetworkModel mainModel);

	public void emailAssertion(JointTextNetworkModel mainModel) {
		assertEquals(emails, mainModel.getEmails());
	}

	public void wordModelAssertions(JointTextNetworkModel model,
			JointTextNetworkModel mainModel) {

		for (int d = 0; d < emails.size(); d++) {
			for (int i = 0; i < model.getAssignmentModel().getWordAssignments()[d].length; i++) {
				assertEquals(
						model.getAssignmentModel().getWordAssignments()[d][i],
						mainModel.getAssignmentModel().getWordAssignments()[d][i]);
			}
		}

		double trueWordLike = model.getAssignmentModel().logWordDataProb();
		double testWordLike = mainModel.getAssignmentModel().logWordDataProb();
		assertEquals(trueWordLike, testWordLike, 1e-12);
	}

	public void edgeModelAssertions(JointTextNetworkModel model,
			JointTextNetworkModel mainModel) {

		if (model.getAssignmentModel().getEdgeAssignments() != null) {
			for (int d = 0; d < emails.size(); d++) {
				for (int j = 0; j < model.getAssignmentModel()
						.getEdgeAssignments()[d].length; j++) {
					assertEquals(model.getAssignmentModel()
							.getEdgeAssignments()[d][j], mainModel
							.getAssignmentModel().getEdgeAssignments()[d][j]);
				}
			}
		} else {
			assertEquals(true, mainModel.getAssignmentModel()
					.getEdgeAssignments() == null);
		}

		double trueEdgeLike = model.getAssignmentModel().logEdgeDataProb();
		double testEdgeLike = mainModel.getAssignmentModel().logEdgeDataProb();
		assertEquals(trueEdgeLike, testEdgeLike, 1e-12);
	}

	public void latentSpaceModelAssertions(JointTextNetworkModel model,
			JointTextNetworkModel mainModel) {

		for (int t = 0; t < T; t++) {
			for (int a = 0; a < emails.getNumAuthors(); a++) {
				for (int k = 0; k < K; k++) {
					assertEquals(
							((EdgeScoreLatentSpace) model.getEdgeScore())
									.getLatentSpaces()[t][a][k],
							((EdgeScoreLatentSpace) mainModel.getEdgeScore())
									.getLatentSpaces()[t][a][k], 1e-12);
				}
			}
		}

		for (int t = 0; t < T; t++) {
			assertEquals(
					((EdgeScoreLatentSpace) model.getEdgeScore())
							.getIntercepts()[t],
					((EdgeScoreLatentSpace) mainModel.getEdgeScore())
							.getIntercepts()[t], 1e-12);
		}
	}

	public abstract boolean usingEdgeModel();

}
