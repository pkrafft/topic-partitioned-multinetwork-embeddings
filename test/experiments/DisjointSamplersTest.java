package experiments;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import mixedmembership.JointTextNetworkModel;
import mixedmembership.MockLogRandoms;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cc.mallet.types.Alphabet;
import data.Email;
import data.EmailCorpus;

public class DisjointSamplersTest {

	Alphabet wordDict;
	EmailCorpus emails;
	MockLogRandoms rng;

	String wordMatrixFile;
	String edgeMatrixFile;
	String vocabFile;

	@Before
	public void setUp() throws Exception {

		wordDict = new Alphabet();
		wordDict.lookupIndex("a");
		wordDict.lookupIndex("b");
		wordDict.lookupIndex("c");
		wordDict.lookupIndex("d");
		wordDict.lookupIndex("e");

		initializeEmails();

		rng = new MockLogRandoms();

		JointTextNetworkModel.DEBUG = true;
		JointTextNetworkModel.DEBUG_OVERRIDE = false;
		
		wordMatrixFile = "word-matrix.csv";
		edgeMatrixFile = "edge-matrix.csv";
		vocabFile = "vocab.txt";
		emails.writeToFiles(wordMatrixFile, edgeMatrixFile, vocabFile);
	}

	@After
	public void tearDown() {

		(new File(wordMatrixFile)).delete();
		(new File(edgeMatrixFile)).delete();
		(new File(vocabFile)).delete();

		String outputDir = "./temp";
		String wordStateFileName = outputDir + "/word_state.txt.gz.1";
		String edgeStateFileName = outputDir + "/edge_state.txt.gz.1";
		String latentSpaceFileName = outputDir + "/latent_spaces.txt.1";
		String interceptFileName = outputDir + "/intercepts.txt.1";

		(new File(wordStateFileName)).delete();
		(new File(edgeStateFileName)).delete();
		(new File(latentSpaceFileName)).delete();
		(new File(interceptFileName)).delete();
		(new File(outputDir)).delete();

		outputDir = ".";
		wordStateFileName = outputDir + "/word_state.txt.gz";
		edgeStateFileName = outputDir + "/edge_state.txt.gz";
		latentSpaceFileName = outputDir + "/latent_spaces.txt";
		interceptFileName = outputDir + "/intercepts.txt";

		for (int i = 1; i <= 3; i++) {
			(new File("options." + (i - 1) + ".txt")).delete();
			(new File("topic_summary.txt.gz." + i)).delete();
			(new File(wordStateFileName + "." + i)).delete();
			(new File(edgeStateFileName + "." + i)).delete();
			(new File(latentSpaceFileName + "." + i)).delete();
			(new File(interceptFileName + "." + i)).delete();
		}
	}

	public void initializeEmails() {
		emails = new EmailCorpus(4, wordDict);
		emails.add(new Email(0, new int[] { 0, 4, 2, 2 }, new int[] { 0, 0, 0,
				1 }, null));
		emails.add(new Email(2, new int[] { 1, 1, 1 },
				new int[] { 1, 1, 0, 1 }, null));
		emails.add(new Email(3, new int[] {}, new int[] { 1, 0, 0, 0 }, null));
		emails.add(new Email(3, new int[] { 0 }, new int[] { 1, 0, 0, 0 }, null));
	}

	// for latent space distance model likelihoods
	private static double logProb(double arg) {
		return arg - Math.log(1 + Math.exp(arg));
	}

	// for latent space distance model likelihoods
	private static double logNotProb(double arg) {
		return -Math.log(1 + Math.exp(arg));
	}
	
	// TODO: test hyperparameter sampling in disjoint model

	// test disjoint latent space sampling with conditional structure lsm model
	@Test
	public void test2() {

		int T = 3;
		int[][] initWordAssignments = null;
		int[][] initEdgeAssignments = null;
		double[][][] initPositions = null;
		double[] initIntercepts = null;
		double[] beta = new double[] { 5.0 };
		double[] alpha = new double[] { 6.0 };

		String outputDir = "./temp";
		new File(outputDir).mkdirs();
		String wordStateFileName = outputDir + "/word_state.txt.gz";
		String edgeStateFileName = outputDir + "/edge_state.txt.gz";

		// create the rng and model to write the necessary state files to disk

		rng = new MockLogRandoms();

		double[] gaussianSamples = new double[] { 0, 1, 1, 2, 2, 0, 0, 0, -1,
				-1, 0, 0, 1, -1, 0, 0, -1, 1, 0, 0, 1, 0, 2, 1, -1, 1, 0 };
		rng.setGaussian(gaussianSamples);

		double[] doubleSamples = new double[1000];
		rng.setDouble(doubleSamples);

		int[] discreteSamples = new int[1000];

		int[] finalSamples = new int[] { 2, 0, 2, 1, 2, 1, 0, 1, 1, 1, 3, 3, 1,
				2, 1, 0, 0, 0, 0, 0, 0 };
		System.arraycopy(finalSamples, 0, discreteSamples, 0,
				finalSamples.length);

		rng.setDiscrete(discreteSamples);

		JointTextNetworkModel model = new JointTextNetworkModel(emails, T, 2,
				rng, true, true, true, false, false, false, false, false,
				false, false, false, null);
		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, null, false, 0, 1,
				null, wordStateFileName, edgeStateFileName, null, null, null,
				null, null, null, null, null, null, null, 0, null, null, null,
				null, null, null, null, null);
		model.estimate(1, false);

		// set up rng for disjoint experiment

		rng = new MockLogRandoms();

		gaussianSamples = new double[1000];
		double[] finalGaussianSamples = new double[] { 0, 1, 1, 2, 2, 0, 0, 0,
				-1, -1, 0, 0, 1, -1, 0, 0, -1, 1, 0, 0, 1, 0, 2, 1, -1, 1, 0 };
		System.arraycopy(finalGaussianSamples, 0, gaussianSamples,
				finalGaussianSamples.length, finalGaussianSamples.length);
		rng.setGaussian(gaussianSamples);

		doubleSamples = new double[1000];
		rng.setDouble(doubleSamples);

		// run first iteration of disjoint experiment

		int firstNumIter = 1;
		String[] args = new String[] { "-wf=" + wordMatrixFile,
				"-vf=" + vocabFile, "-ef=" + edgeMatrixFile,
				"-a=" + emails.getNumAuthors() + "", "-t=" + T, "-k=" + 2,
				"-n=" + firstNumIter + "", "-p=" + "0", "-s=" + "1", "-v",
				"--alpha=" + alpha[0] / T + "",
				"--beta=" + beta[0] / emails.getWordDict().size() + "", "-djl",
				"-djrf=" + outputDir, "-dji=1", "-of=" + "./" };

		try {
			model = ConditionalStructureExperiment.main(args, rng);
		} catch (IOException e) {
			System.out.println(emails);
			e.printStackTrace();
			System.exit(-1);
		}

		// run next iteration to test reading from file

		args = new String[] { "-wf=" + wordMatrixFile, "-vf=" + vocabFile,
				"-ef=" + edgeMatrixFile, "-a=" + emails.getNumAuthors() + "",
				"-t=" + T + "", "-k=" + 2, "-n=" + 1 + "", "-p=" + "0",
				"-s=" + "1", "-v", "--alpha=" + alpha[0] / T + "",
				"--beta=" + beta[0] / emails.getWordDict().size() + "", "-djl",
				"-djrf=" + outputDir, "-dji=1", "-r ", "-rf=" + "./",
				"-i=" + firstNumIter + "", "-of=" + "./" };

		try {
			model = ConditionalStructureExperiment.main(args, rng);
		} catch (IOException e) {
			System.out.println(emails);
			e.printStackTrace();
			System.exit(-1);
		}

		double trueValue = 0;

		// word score
		trueValue += -3 * Math.log(5) - 3 * Math.log(6) - 2 * Math.log(7);

		// assignment score
		trueValue += 7 * Math.log(2) - Math.log(9) - 3 * Math.log(6) - 2
				* Math.log(7) - 2 * Math.log(8);

		// edge prior
		trueValue += -3 * Math.log(4) - 3 * Math.log(3);

		// edge score
		trueValue += 2 * logNotProb(1 - Math.sqrt(2))
				+ logProb(-1 - Math.sqrt(5)) + logNotProb(-1) + 4
				* logProb(1 - Math.sqrt(2)) + logProb(-1) + 2 * logNotProb(1)
				+ logNotProb(-1 - Math.sqrt(2));

		assertEquals(trueValue, model.logProb(model.logLike()), 1e-12);
	}

	// test disjoint edge model sampling with conditional structure lsm model
	@Test
	public void test1() {

		int T = 3;
		int[][] initWordAssignments = null;
		int[][] initEdgeAssignments = null;
		double[][][] initPositions = null;
		double[] initIntercepts = null;
		double[] beta = new double[] { 5.0 };
		double[] alpha = new double[] { 6.0 };

		String outputDir = "./temp";
		new File(outputDir).mkdirs();
		String wordStateFileName = outputDir + "/word_state.txt.gz";

		// create the rng and model to write the necessary state files to disk

		rng = new MockLogRandoms();

		double[] gaussianSamples = new double[] { 0, 1, 1, 2, 2, 0, 0, 0, -1,
				-1, 0, 0, 1, -1, 0, 0, -1, 1, 0, 0, 1, 0, 2, 1, -1, 1, 0 };
		rng.setGaussian(gaussianSamples);

		double[] doubleSamples = new double[1000];
		rng.setDouble(doubleSamples);

		int[] discreteSamples = new int[1000];

		int[] finalSamples = new int[] { 2, 0, 2, 1, 2, 1, 0, 1, 1, 1, 3, 3, 1,
				2, 1, 0, 0, 0, 0, 0, 0 };
		System.arraycopy(finalSamples, 0, discreteSamples, 0,
				finalSamples.length);

		rng.setDiscrete(discreteSamples);

		JointTextNetworkModel model = new JointTextNetworkModel(emails, T, 2,
				rng, true, true, true, false, false, false, false, false,
				false, false, false, null);
		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, null, false, 0, 1,
				null, wordStateFileName, null, null, null, null, null, null,
				null, null, null, null, null, 0, null, null, null, null, null,
				null, null, null);
		model.estimate(1, false);

		// set up rng for disjoint experiment

		rng = new MockLogRandoms();

		gaussianSamples = new double[1000];
		double[] finalGaussianSamples = new double[] { 0, 1, 1, 2, 2, 0, 0, 0,
				-1, -1, 0, 0, 1, -1, 0, 0, -1, 1, 0, 0, 1, 0, 2, 1, -1, 1, 0 };
		System.arraycopy(finalGaussianSamples, 0, gaussianSamples,
				finalGaussianSamples.length, finalGaussianSamples.length);
		rng.setGaussian(gaussianSamples);

		doubleSamples = new double[1000];
		rng.setDouble(doubleSamples);

		discreteSamples = new int[1000];
		finalSamples = new int[] { 1, 3, 3, 1, 2, 1, 0, 0, 0, 0, 0, 0 };
		System.arraycopy(finalSamples, 0, discreteSamples,
				2 * finalSamples.length, finalSamples.length);
		rng.setDiscrete(discreteSamples);

		// run first iteration of disjoint experiment

		int firstNumIter = 1;
		String[] args = new String[] { "-wf=" + wordMatrixFile,
				"-vf=" + vocabFile, "-ef=" + edgeMatrixFile,
				"-a=" + emails.getNumAuthors() + "", "-t=" + T, "-k=" + 2,
				"-n=" + firstNumIter + "", "-p=" + "0", "-s=" + "1", "-v",
				"--alpha=" + alpha[0] / T + "",
				"--beta=" + beta[0] / emails.getWordDict().size() + "", "-dje",
				"-djrf=" + outputDir, "-dji=1", "-of=" + "./" };

		try {
			model = ConditionalStructureExperiment.main(args, rng);
		} catch (IOException e) {
			System.out.println(emails);
			e.printStackTrace();
			System.exit(-1);
		}

		// run next iteration to test reading from file

		args = new String[] { "-wf=" + wordMatrixFile, "-vf=" + vocabFile,
				"-ef=" + edgeMatrixFile, "-a=" + emails.getNumAuthors() + "",
				"-t=" + T + "", "-k=" + 2, "-n=" + 1 + "", "-p=" + "0",
				"-s=" + "1", "-v", "--alpha=" + alpha[0] / T + "",
				"--beta=" + beta[0] / emails.getWordDict().size() + "", "-dje",
				"-djrf=" + outputDir, "-dji=1", "-r ", "-rf=" + "./",
				"-i=" + firstNumIter + "", "-of=" + "./" };

		try {
			model = ConditionalStructureExperiment.main(args, rng);
		} catch (IOException e) {
			System.out.println(emails);
			e.printStackTrace();
			System.exit(-1);
		}

		double trueValue = 0;

		// word score
		trueValue += -3 * Math.log(5) - 3 * Math.log(6) - 2 * Math.log(7);

		// assignment score
		trueValue += 7 * Math.log(2) - Math.log(9) - 3 * Math.log(6) - 2
				* Math.log(7) - 2 * Math.log(8);

		// edge prior
		trueValue += -3 * Math.log(4) - 3 * Math.log(3);

		// edge score
		trueValue += 2 * logNotProb(1 - Math.sqrt(2))
				+ logProb(-1 - Math.sqrt(5)) + logNotProb(-1) + 4
				* logProb(1 - Math.sqrt(2)) + logProb(-1) + 2 * logNotProb(1)
				+ logNotProb(-1 - Math.sqrt(2));

		assertEquals(trueValue, model.logProb(model.logLike()), 1e-12);
	}
}
