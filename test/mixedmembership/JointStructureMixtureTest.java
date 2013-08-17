package mixedmembership;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import util.LogRandoms;
import cc.mallet.types.Alphabet;
import cc.mallet.util.Randoms;
import data.Email;
import data.EmailCorpus;

public class JointStructureMixtureTest {

	Alphabet wordDict;
	EmailCorpus emails;
	MockLogRandoms rng;

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
		JointTextNetworkModel.DEBUG_OVERRIDE = true;
	}
	
	public void initializeEmails() {
		emails = new EmailCorpus(4, wordDict);
		emails.add(new Email(0, new int[]{0, 4, 2, 2}, new int[]{0, 0, 0, 1}, null));
		emails.add(new Email(2, new int[]{1, 1, 1}, new int[]{1, 1, 0, 1}, null));
		emails.add(new Email(3, new int[]{}, new int[]{1, 0, 0, 0}, null));
		emails.add(new Email(3, new int[]{0}, new int[]{1, 0, 0, 0}, null));
	}

	private static double logProb(double arg) {
		return arg - Math.log(1 + Math.exp(arg));
	}

	private static double logNotProb(double arg) {
		return -Math.log(1 + Math.exp(arg));
	}
	
	private static void normalizeLogProb(double[] values) {
		double norm = 0;
		for(int i = 0; i < values.length; i++) {
			values[i] = Math.exp(values[i]);
			norm += values[i];
		}
		for(int i = 0; i < values.length; i++) {
			values[i] = Math.log(values[i]/norm);
		}
	}
	
	// edge only mixture model after estimation, using multiple calls to estimate method, writing/reading files
	@Test
	public void test10c() {
		int T = 3;
		int[][] initWordAssignments = null;		
		double[][][] initPositions = null;
		double[] initIntercepts = null;
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 3;
		
		String wordStateFileName = "word-state-test.txt.gz";
		String latentSpaceFileName = "latent-space-test.txt";
		String interceptFileName = "intercept-test.txt";
		String predictionsFileName = "predictions-test.txt";
		
		Random temp = new Random();
		int seed =  temp.nextInt();
		LogRandoms rng = new LogRandoms();
		
		rng = new LogRandoms(seed);
		
		emails.obscureRandomEdges(0.5, rng);
		
		JointTextNetworkModel model = new JointTextNetworkModel(emails, T, 2, rng, false, true, true, false, false, false, false, true, false, false, false, null);
		model.initialize(initWordAssignments, null, initPositions, initIntercepts, 
				alpha, beta, null, false, 0, 1, null, 
				wordStateFileName + ".true", null,
				null, null, null, 
				latentSpaceFileName + ".true", interceptFileName + ".true", 
				null, null, null, null, predictionsFileName, 0, null, null, null, null, null, null, null, null);
		
		model.estimate(numIter, false);
		
		double trueValue = model.logProb(model.logLike());
		
		initializeEmails();

		rng = new LogRandoms(seed);
		
		emails.obscureRandomEdges(0.5, rng);
		
		model = new JointTextNetworkModel(emails, T, 2, rng, false, true, true, false, false, false, false, true, false, false, false, null);
		model.initialize(initWordAssignments, null, initPositions, initIntercepts, 
				alpha, beta, null, false, 0, 1, null, 
				wordStateFileName, null, 
				null, null, null, 
				latentSpaceFileName, interceptFileName, 
				null, null, null, null, predictionsFileName, 0, null, null, null, null, null, null, null, null);
		
		model.estimate(1, false);
		
		for (int iterOffset = 1; iterOffset < numIter; iterOffset++) {
			
			// need to initialize these to keep the rng on the right track
			// so draws are taken up initializing them
			initWordAssignments = new int[emails.size()][];
			for(int d = 0; d < initWordAssignments.length; d++) {
				initWordAssignments[d] = new int[Math.max(emails.getEmail(d).getLength(), 1)];
			}
			initPositions = new double[T][emails.getNumAuthors()][2];
			initIntercepts = new double[T];
			
			initializeEmails();
			model = new JointTextNetworkModel(emails, T, 2, rng, false, true, true, false, false, false, false, true, false, false, false, null);
			model.initialize(initWordAssignments, null, initPositions, initIntercepts,
					alpha, beta, null, false, 0, 1, null, 
					wordStateFileName, null, 
					null, null, null,
					latentSpaceFileName, interceptFileName, 
					null, null, null, null, predictionsFileName, iterOffset, wordStateFileName + "." + iterOffset, 
					null,
					latentSpaceFileName + "." + iterOffset, 
					interceptFileName + "." + iterOffset, null, null, null,
					predictionsFileName + "." + iterOffset);
			model.estimate(1, false);
		}
		
		assertEquals(trueValue, model.logProb(model.logLike()), 1e-12);
		
		(new File(wordStateFileName)).delete();
		(new File(latentSpaceFileName)).delete();
		(new File(interceptFileName)).delete();
		(new File(predictionsFileName)).delete();
		(new File(wordStateFileName + ".true")).delete();
		(new File(latentSpaceFileName + ".true")).delete();
		(new File(interceptFileName + ".true")).delete();
		(new File(predictionsFileName + ".true")).delete();
		for(int i = 1; i <= numIter; i++) {
			(new File(wordStateFileName + "." + i)).delete();
			(new File(latentSpaceFileName + "." + i)).delete();
			(new File(interceptFileName + "." + i)).delete();
			(new File(predictionsFileName + "." + i)).delete();
			(new File(wordStateFileName + ".true" + "." + i)).delete();
			(new File(latentSpaceFileName + ".true" + "." + i)).delete();
			(new File(interceptFileName + ".true" + "." + i)).delete();
			(new File(predictionsFileName + ".true" + "." + i)).delete();
		}
	}
	
	// edge only mixture model after estimation
	@Test
	public void test10b() {
		int T = 3;
		int[][] initWordAssignments = null;
		double[][][] initPositions = null;
		double[] initIntercepts = null; // new double[]{-1, 0, 1};
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 3;
		
		double[] gaussianSamples = new double[]{
				0,0,0,0,22,0,0,0,0,0,0,0,
				0,0,0,0,0,0,8,0,0,0,0,4,
				1,5,-10,
				0,0,0,0,-5,0,0,0,0,0,0,0,
				0,0,-3,0,0,0,0,0,0,0,2,1,
				0,0,-10,
				
				0,1,1,2,2,0,0,0,-1,-1,0,0,1,-1,0,0,-1,1,0,0,1,0,0,0,
				-1,10,1
		};
		rng.setGaussian(gaussianSamples);
		
		// 3 samples for initializing missing edges
		
		// for each iteration
		// 12 samples for author position MCMC in each space
		// 3 samples for intercept
		// 3 samples for missing edges
		
		int offset = 12 + 3 + 3;
		int positionStart = 3;
		int interceptStart = 15;
		int missingEdgeStart = 18;

		double[] doubleSamples = new double[1000];

		doubleSamples[positionStart + 11] = Double.POSITIVE_INFINITY;
		doubleSamples[positionStart + offset + 5] = Double.POSITIVE_INFINITY;
		doubleSamples[positionStart + offset + 10] = Double.POSITIVE_INFINITY;
		doubleSamples[interceptStart + 1] = Double.POSITIVE_INFINITY;
		
		// reject last latent position sample, second-to-last intercept
		doubleSamples[positionStart + 2*offset + 11] = Double.POSITIVE_INFINITY;
		doubleSamples[interceptStart + 2*offset + 1] = Double.POSITIVE_INFINITY;
		
		// missing edges are set to 1 by default (double = 0 => edge = 1)
		doubleSamples[0] = 1.0; // initialize incorrect 1st edge
		doubleSamples[2] = 1.0; // initialize incorrect 3rd edge
		
		// after initialization, double = 1 flips the edge
		doubleSamples[missingEdgeStart] = 1.0; // incorrect 1st edge
		doubleSamples[missingEdgeStart + 2] = 1.0; // correct 3rd edge
		doubleSamples[missingEdgeStart + offset + 1] = 1.0; // incorrect 2nd edge
		doubleSamples[missingEdgeStart + 2*offset] = 1.0; // correct 1st edge
		doubleSamples[missingEdgeStart + 2*offset + 1] = 1.0; // correct 2nd edge
		
		rng.setDouble(doubleSamples);
		
		int[] discreteSamples = new int[1000];

		int assignmentIter = 4;
		
		LogRandoms tempRng = new LogRandoms();
		int ind = -1;
		for(int iter = 0; iter < numIter - 1; iter++) {
			for (int i = 0; i < assignmentIter; i++) {
				ind++;
				discreteSamples[ind] = tempRng.nextInt(T);
			}
		}
		
		int[] finalSamples = new int[]{1, 0, 2, 1};
		System.arraycopy(finalSamples, 0, discreteSamples, 2*assignmentIter, finalSamples.length);
		
		rng.setDiscrete(discreteSamples);

		emails.getEmail(0).obscureEdge(1);
		emails.getEmail(0).obscureEdge(3);
		emails.getEmail(1).obscureEdge(1);

		double trueValue = 0;
		
		// assignment score
		double prior = -Math.log(2) - 2 * Math.log(3) - Math.log(7);
		trueValue += prior;
		System.out.println("prior: " + prior);
		
		// edge score
		double edgeLike = logNotProb(-Math.sqrt(2)) + logNotProb(-2) + logProb(-Math.sqrt(2)) + 
				2*logProb(-1 - Math.sqrt(5)) + logProb(-3) + 
				logProb(-2) + logNotProb(1 - Math.sqrt(5)) + logNotProb(1 - Math.sqrt(2)) +
				logProb(-Math.sqrt(2)) + logNotProb(0) + logNotProb(-Math.sqrt(2));
		trueValue += edgeLike;
		System.out.println("like: " + edgeLike);

		JointTextNetworkModel model = 
				new JointTextNetworkModel(emails, T, 2, rng, false, true, true, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		
		double testValue = model.logProb(model.logLike());
		
		assertEquals(trueValue, testValue, 1e-12);
	}
	
	// edge only mixture model lsm gibbs sampling equations
	@Test
	public void test10a() {
		int T = 3;
		int[][] initWordAssignments = new int[][]{new int[]{1}, new int[]{0}, new int[]{2}, new int[]{1}};
		double[][][] initPositions = new double[][][]{
				new double[][]{new double[]{0.0, 1.0}, new double[]{1.0, 2.0}, new double[]{2.0, 0.0}, new double[]{0.0, 0.0}},
				new double[][]{new double[]{-1.0, -1.0}, new double[]{0.0, 0.0}, new double[]{1.0, -1.0}, new double[]{0.0, 0.0}}, 
				new double[][]{new double[]{-1.0, 1.0}, new double[]{0.0, 0.0}, new double[]{1.0, 0.0}, new double[]{2.0, 1.0}}};
		double[] initIntercepts = new double[]{-1, 0, 1};
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 0;

		double[] trueValue = new double[3];
		
		double edgeLike = logNotProb(-1 - Math.sqrt(2)) + logNotProb(-1 - Math.sqrt(5)) + logProb(-2);
		System.out.println("test edge like: " + edgeLike);
		double prior = Math.log(3) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[0] = edgeLike + prior;
		
		edgeLike = logNotProb(-Math.sqrt(2)) + logNotProb(-2) + logProb(-Math.sqrt(2));
		System.out.println("test edge like: " + edgeLike);
		prior = Math.log(3) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[1] = edgeLike + prior;
		
		edgeLike = logNotProb(1 - Math.sqrt(2)) + logNotProb(1 - Math.sqrt(5)) + logProb(-2);
		System.out.println("test edge like: " + edgeLike);
		prior = Math.log(3) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[2] = edgeLike + prior;
		
		normalizeLogProb(trueValue);

		JointTextNetworkModel model = 
				new JointTextNetworkModel(emails, T, 2, rng, false, true, false, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		
		double[] testValue = model.getAssignmentModel().getTokenAssignmentDistribution(0, -1, 0, -1, false, 1);
		normalizeLogProb(testValue);
		
		for(int i = 0; i < T; i++) {
			assertEquals(trueValue[i], testValue[i], 1e-12);
		}
		
		trueValue = new double[3];
		
		edgeLike = logProb(-2) + logNotProb(-1 - Math.sqrt(5)) + logNotProb(-3);
		System.out.println("test edge like: " + edgeLike);
		prior = Math.log(3) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[0] = edgeLike + prior;
		
		edgeLike = logProb(-Math.sqrt(2)) + logNotProb(0) + logNotProb(-Math.sqrt(2));
		System.out.println("test edge like: " + edgeLike);
		prior = Math.log(4) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[1] = edgeLike + prior;
				
		edgeLike = logProb(-2) + logNotProb(1 - Math.sqrt(5)) + logNotProb(1 - Math.sqrt(2));
		System.out.println("test edge like: " + edgeLike);
		prior = Math.log(2) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[2] = edgeLike + prior;
		
		normalizeLogProb(trueValue);
		
		model = 
				new JointTextNetworkModel(emails, T, 2, rng, false, true, false, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		
		testValue = model.getAssignmentModel().getTokenAssignmentDistribution(2, -1, 3, -1, false, 2);
		normalizeLogProb(testValue);
		
		for(int i = 0; i < T; i++) {
			assertEquals(trueValue[i], testValue[i], 1e-12);
		}
		
		double trueLogProb = logProb(-1 - Math.sqrt(5)); 

		model = 
				new JointTextNetworkModel(emails, T, 2, rng, false, true, false, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		
		double testLogProb = model.getAssignmentModel().getMissingEdgeDistribution(0, 2, 1, 1);

		assertEquals(trueLogProb, testLogProb, 1e-12);
		
		trueLogProb = logNotProb(0); 

		model = 
				new JointTextNetworkModel(emails, T, 2, rng, false, true, false, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		
		testLogProb = model.getAssignmentModel().getMissingEdgeDistribution(1, 3, 1, 0);

		assertEquals(trueLogProb, testLogProb, 1e-12);
	}
	
	
	// word mixture model after estimation, using multiple calls to estimate method, writing/reading files
	@Test
	public void test9c() {
		int T = 3;
		int[][] initWordAssignments = null;		
		double[][][] initPositions = null;
		double[] initIntercepts = null;
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 3;
		
		String wordStateFileName = "word-state-test.txt.gz";
		
		Random temp = new Random();
		int seed =  temp.nextInt();
		LogRandoms rng = new LogRandoms();
		
		rng = new LogRandoms(seed);
				
		JointTextNetworkModel model = new JointTextNetworkModel(emails, T, 2, rng, true, false, false, false, false, false, false, true, false, false, false, null);
		model.initialize(initWordAssignments, null, initPositions, initIntercepts, 
				alpha, beta, null, false, 0, 1, null, 
				wordStateFileName + ".true", null,
				null, null, null, 
				null, null,
				null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		
		model.estimate(numIter, false);
		
		double trueValue = model.logProb(model.logLike());
		
		initializeEmails();

		rng = new LogRandoms(seed);
				
		model = new JointTextNetworkModel(emails, T, 2, rng, true, false, false, false, false, false, false, true, false, false, false, null);
		model.initialize(initWordAssignments, null, initPositions, initIntercepts, 
				alpha, beta, null, false, 0, 1, null, 
				wordStateFileName, null, 
				null, null, null, 
				null, null, 
				null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		
		model.estimate(1, false);
		
		for (int iterOffset = 1; iterOffset < numIter; iterOffset++) {
			
			// need to initialize these to keep the rng on the right track
			// so draws are taken up initializing them
			initWordAssignments = new int[emails.size()][];
			for(int d = 0; d < initWordAssignments.length; d++) {
				initWordAssignments[d] = new int[Math.max(emails.getEmail(d).getLength(), 1)];
			}
			initPositions = new double[T][emails.getNumAuthors()][2];
			initIntercepts = new double[T];
			
			initializeEmails();
			model = new JointTextNetworkModel(emails, T, 2, rng, true, false, false, false, false, false, false, true, false, false, false, null);
			model.initialize(initWordAssignments, null, initPositions, initIntercepts,
					alpha, beta, null, false, 0, 1, null, 
					wordStateFileName, null, 
					null, null, null,
					null, null, 
					null, null, null, null, null, iterOffset, wordStateFileName + "." + iterOffset, 
					null,
					null,
					null, null, null, null,
					null);
			model.estimate(1, false);
		}
		
		assertEquals(trueValue, model.logProb(model.logLike()), 1e-12);
		
		(new File(wordStateFileName)).delete();
		(new File(wordStateFileName + ".true")).delete();
		for(int i = 1; i <= numIter; i++) {
			(new File(wordStateFileName + "." + i)).delete();
			(new File(wordStateFileName + ".true" + "." + i)).delete();
		}
	}
	
	
	// edge mixture model after estimation
	@Test
	public void test9b() {
		int T = 3;
		int[][] initWordAssignments = null;
		double[][][] initPositions = null;
		double[] initIntercepts = null; 
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 3;
		
		int[] discreteSamples = new int[1000];

		int assignmentIter = 4;
		
		LogRandoms tempRng = new LogRandoms();
		int ind = -1;
		for(int iter = 0; iter < numIter - 1; iter++) {
			for (int i = 0; i < assignmentIter; i++) {
				ind++;
				discreteSamples[ind] = tempRng.nextInt(T);
			}
		}
		
		int[] finalSamples = new int[]{1, 0, 2, 1};
		System.arraycopy(finalSamples, 0, discreteSamples, 2*assignmentIter, finalSamples.length);
		
		rng.setDiscrete(discreteSamples);
		
		double trueValue = 0;
		
		// assignment score
		double prior = -Math.log(2) - 2 * Math.log(3) - Math.log(7);
		trueValue += prior;
		
		// word score
		double wordLike = -2 * Math.log(2) - 3 * Math.log(3) - 2 * Math.log(5) - 2 * Math.log(7);
		trueValue += wordLike;

		JointTextNetworkModel model = 
				new JointTextNetworkModel(emails, T, 2, rng, true, false, false, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		
		assertEquals(trueValue, model.logProb(model.logLike()), 1e-12);
	}
	
	// text mixture model gibbs sampling equations
	@Test
	public void test9a() {
		int T = 3;
		int[][] initWordAssignments = new int[][]{new int[]{1}, new int[]{0}, new int[]{2}, new int[]{1}};
		double[][][] initPositions = null;
		double[] initIntercepts = null;
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 0;

		double[] trueValue = new double[3];
		
		double wordLike =  - Math.log(8) - Math.log(9) - Math.log(10) + Math.log(2) - Math.log(11);
		System.out.println("test word like: " + wordLike);
		double prior = Math.log(3) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[0] = wordLike + prior;
		
		wordLike = Math.log(2) - Math.log(6) - Math.log(7) - Math.log(8) + Math.log(2) - Math.log(9);
		System.out.println("test word like: " + wordLike);
		prior = Math.log(3) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[1] = wordLike + prior;
		
		wordLike =  - Math.log(5) - Math.log(6) - Math.log(7) + Math.log(2) - Math.log(8);
		System.out.println("test word like: " + wordLike);
		prior = Math.log(3) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[2] = wordLike + prior;
		
		normalizeLogProb(trueValue);

		JointTextNetworkModel model = 
				new JointTextNetworkModel(emails, T, 2, rng, true, false, false, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		
		double[] testValue = model.getAssignmentModel().getTokenAssignmentDistribution(0, -1, 0, -1, false, 1);
		normalizeLogProb(testValue);
		
		for(int i = 0; i < T; i++) {
			assertEquals(trueValue[i], testValue[i], 1e-12);
		}
		
		trueValue = new double[3];
		
		wordLike = 0;
		System.out.println("test word like: " + wordLike);
		prior = Math.log(3) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[0] = wordLike + prior;
		
		wordLike = 0;
		System.out.println("test word like: " + wordLike);
		prior = Math.log(4) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[1] = wordLike + prior;
				
		wordLike = 0;
		System.out.println("test word like: " + wordLike);
		prior = Math.log(2) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[2] = wordLike + prior;
		
		normalizeLogProb(trueValue);
		
		model = 
				new JointTextNetworkModel(emails, T, 2, rng, true, false, false, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		
		testValue = model.getAssignmentModel().getTokenAssignmentDistribution(2, -1, 3, -1, false, 2);
		normalizeLogProb(testValue);
		
		for(int i = 0; i < T; i++) {
			assertEquals(trueValue[i], testValue[i], 1e-12);
		}
	}
	
	// mixture model after estimation, using multiple calls to estimate method, writing/reading files
	@Test
	public void test8() {
		int T = 3;
		int[][] initWordAssignments = null;		
		double[][][] initPositions = null;
		double[] initIntercepts = null;
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 3;
		
		String wordStateFileName = "word-state-test.txt.gz";
		String latentSpaceFileName = "latent-space-test.txt";
		String interceptFileName = "intercept-test.txt";
		String predictionsFileName = "predictions-test.txt";
		
		Random temp = new Random();
		int seed =  temp.nextInt();
		LogRandoms rng = new LogRandoms();
		
		rng = new LogRandoms(seed);
		
		emails.obscureRandomEdges(0.5, rng);
		
		JointTextNetworkModel model = new JointTextNetworkModel(emails, T, 2, rng, true, true, true, false, false, false, false, true, false, false, false, null);
		model.initialize(initWordAssignments, null, initPositions, initIntercepts, 
				alpha, beta, null, false, 0, 1, null, 
				wordStateFileName + ".true", null,
				null, null, null, 
				latentSpaceFileName + ".true", interceptFileName + ".true", 
				null, null, null, null, predictionsFileName, 0, null, null, null, null, null, null, null, null);
		
		model.estimate(numIter, false);
		
		double trueValue = model.logProb(model.logLike());
		
		initializeEmails();

		rng = new LogRandoms(seed);
		
		emails.obscureRandomEdges(0.5, rng);
		
		model = new JointTextNetworkModel(emails, T, 2, rng, true, true, true, false, false, false, false, true, false, false, false, null);
		model.initialize(initWordAssignments, null, initPositions, initIntercepts, 
				alpha, beta, null, false, 0, 1, null, 
				wordStateFileName, null, 
				null, null, null, 
				latentSpaceFileName, interceptFileName, 
				null, null, null, null, predictionsFileName, 0, null, null, null, null, null, null, null, null);
		
		model.estimate(1, false);
		
		for (int iterOffset = 1; iterOffset < numIter; iterOffset++) {
			
			// need to initialize these to keep the rng on the right track
			// so draws are taken up initializing them
			initWordAssignments = new int[emails.size()][];
			for(int d = 0; d < initWordAssignments.length; d++) {
				initWordAssignments[d] = new int[Math.max(emails.getEmail(d).getLength(), 1)];
			}
			initPositions = new double[T][emails.getNumAuthors()][2];
			initIntercepts = new double[T];
			
			initializeEmails();
			model = new JointTextNetworkModel(emails, T, 2, rng, true, true, true, false, false, false, false, true, false, false, false, null);
			model.initialize(initWordAssignments, null, initPositions, initIntercepts,
					alpha, beta, null, false, 0, 1, null, 
					wordStateFileName, null, 
					null, null, null,
					latentSpaceFileName, interceptFileName, 
					null, null, null, null, predictionsFileName, iterOffset, wordStateFileName + "." + iterOffset, 
					null,
					latentSpaceFileName + "." + iterOffset, 
					interceptFileName + "." + iterOffset, null, null, null,
					predictionsFileName + "." + iterOffset);
			model.estimate(1, false);
		}
		
		assertEquals(trueValue, model.logProb(model.logLike()), 1e-12);
		
		(new File(wordStateFileName)).delete();
		(new File(latentSpaceFileName)).delete();
		(new File(interceptFileName)).delete();
		(new File(predictionsFileName)).delete();
		(new File(wordStateFileName + ".true")).delete();
		(new File(latentSpaceFileName + ".true")).delete();
		(new File(interceptFileName + ".true")).delete();
		(new File(predictionsFileName + ".true")).delete();
		for(int i = 1; i <= numIter; i++) {
			(new File(wordStateFileName + "." + i)).delete();
			(new File(latentSpaceFileName + "." + i)).delete();
			(new File(interceptFileName + "." + i)).delete();
			(new File(predictionsFileName + "." + i)).delete();
			(new File(wordStateFileName + ".true" + "." + i)).delete();
			(new File(latentSpaceFileName + ".true" + "." + i)).delete();
			(new File(interceptFileName + ".true" + "." + i)).delete();
			(new File(predictionsFileName + ".true" + "." + i)).delete();
		}
	}
	
	// writing/reading files, mixture model
	@Test
	public void test7() {
		int T = 3;
		int[][] initWordAssignments = new int[][]{new int[]{2, 0, 2, 1}, new int[]{2, 1, 0}, new int[]{1}, new int[]{1}};
		int[][] initEdgeAssignments = new int[][]{new int[]{1, 3, 3}, new int[]{1, 2, 1}, new int[]{0, 0, 0}, new int[]{0, 0, 0}};
		double[][][] initPositions = new double[][][]{
				new double[][]{new double[]{0.0, 1.0}, new double[]{1.0, 2.0}, new double[]{2.0, 0.0}, new double[]{0.0, 0.0}},
				new double[][]{new double[]{-1.0, -1.0}, new double[]{0.0, 0.0}, new double[]{1.0, -1.0}, new double[]{0.0, 0.0}}, 
				new double[][]{new double[]{-1.0, 1.0}, new double[]{0.0, 0.0}, new double[]{1.0, 0.0}, new double[]{2.0, 1.0}}};
		double[] initIntercepts = new double[]{-1, 1, 0};
		double[] gamma = new double[]{2.0};
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 0;
		
		String wordStateFileName = "word-state-test.txt.gz";
		String edgeStateFileName = "edge-state-test.txt.gz";
		String latentSpaceFileName = "latent-space-test.txt";
		String interceptFileName = "intercept-test.txt";
		String predictionsFileName = "predictions-test.txt";
		

		EmailCorpus emailsCopy = emails.copy();
		
		emails.obscureRandomEdges(0.5, new Randoms());
		
		JointTextNetworkModel model = 
				new JointTextNetworkModel(emails, T, 2, null, true, true, false, false, false, false, false, false, false, false, false, null);
		model.initialize(initWordAssignments, initEdgeAssignments, initPositions, initIntercepts, 
				alpha, beta, gamma, false, 0, 0, null, 
				wordStateFileName, edgeStateFileName, 
				null, null, null, 
				latentSpaceFileName, interceptFileName, 
				null, null, null, null, predictionsFileName, 0, null, null, null, null, null, null, null, null); 
		model.estimate(numIter, false);
		
		JointTextNetworkModel modelCopy = 
				new JointTextNetworkModel(emailsCopy, T, 2, null, true, true, false, false, false, false, false, false, false, false, false, null);
		modelCopy.initialize(null, null, null, null, 
				alpha, beta, gamma, false, 0, 0, null, 
				null, null,  null,
				null, null, 
				null, null, 
				null, null, null, null, null, 0, wordStateFileName, edgeStateFileName, latentSpaceFileName, interceptFileName, null, null, null, predictionsFileName);
		modelCopy.estimate(numIter, false);
			
		assertEquals(model.logProb(model.logLike()), modelCopy.logProb(modelCopy.logLike()), 1e-12);
		
		for(int d = 0; d < emails.size(); d++) {
			assertEquals(emails.getEmail(d).getObscured(), emailsCopy.getEmail(d).getObscured());
			for(int r = 0; r < emails.getNumAuthors(); r++) {
				assertEquals(emails.getEmail(d).getEdge(r), emailsCopy.getEmail(d).getEdge(r));
			}
		}
		
		(new File(wordStateFileName)).delete();
		(new File(edgeStateFileName)).delete();
		(new File(latentSpaceFileName)).delete();
		(new File(interceptFileName)).delete();
		(new File(predictionsFileName)).delete();
	}

	// mixture model after estimation, using multiple calls to estimate method
	@Test
	public void test3() {
		int T = 3;
		int[][] initWordAssignments = null;
		double[][][] initPositions = null;
		double[] initIntercepts = null; // new double[]{-1, 1, 0};
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 3;
		
		double[] gaussianSamples = new double[]{
				0,0,0,0,22,0,0,0,0,0,0,0,
				0,0,0,0,0,0,8,0,0,0,0,4,
				1,5,-10,
				0,0,0,0,-5,0,0,0,0,0,0,0,
				0,0,-3,0,0,0,0,0,0,0,2,1,
				0,0,-10,
				
				0,1,1,2,2,0,0,0,-1,-1,0,0,1,-1,0,0,-1,1,0,0,1,0,0,0,
				-1,10,1
		};
		rng.setGaussian(gaussianSamples);
		
		// 3 samples for initializing missing edges
		
		// for each iteration
		// 12 samples for author position MCMC in each space
		// 3 samples for intercept
		// 3 samples for missing edges
		
		int offset = 12 + 3 + 3;
		int positionStart = 3;
		int interceptStart = 15;
		int missingEdgeStart = 18;

		double[] doubleSamples = new double[1000];

		doubleSamples[positionStart + 11] = Double.POSITIVE_INFINITY;
		doubleSamples[positionStart + offset + 5] = Double.POSITIVE_INFINITY;
		doubleSamples[positionStart + offset + 10] = Double.POSITIVE_INFINITY;
		doubleSamples[interceptStart + 1] = Double.POSITIVE_INFINITY;
		
		// reject last latent position sample, second-to-last intercept
		doubleSamples[positionStart + 2*offset + 11] = Double.POSITIVE_INFINITY;
		doubleSamples[interceptStart + 2*offset + 1] = Double.POSITIVE_INFINITY;
		
		// missing edges are set to 1 by default (double = 0 => edge = 1)
		doubleSamples[0] = 1.0; // initialize incorrect 1st edge
		doubleSamples[2] = 1.0; // initialize incorrect 3rd edge
		
		// after initialization, double = 1 flips the edge
		doubleSamples[missingEdgeStart] = 1.0; // incorrect 1st edge
		doubleSamples[missingEdgeStart + 2] = 1.0; // correct 3rd edge
		doubleSamples[missingEdgeStart + offset + 1] = 1.0; // incorrect 2nd edge
		doubleSamples[missingEdgeStart + 2*offset] = 1.0; // correct 1st edge
		doubleSamples[missingEdgeStart + 2*offset + 1] = 1.0; // correct 2nd edge
		
		rng.setDouble(doubleSamples);
		
		int[] discreteSamples = new int[1000];

		int assignmentIter = 4;
		
		LogRandoms tempRng = new LogRandoms();
		int ind = -1;
		for(int iter = 0; iter < numIter - 1; iter++) {
			for (int i = 0; i < assignmentIter; i++) {
				ind++;
				discreteSamples[ind] = tempRng.nextInt(T);
			}
		}
		
		int[] finalSamples = new int[]{1, 0, 2, 1};
		System.arraycopy(finalSamples, 0, discreteSamples, 2*assignmentIter, finalSamples.length);
		
		rng.setDiscrete(discreteSamples);

		
		emails.getEmail(0).obscureEdge(1);
		emails.getEmail(0).obscureEdge(3);
		emails.getEmail(1).obscureEdge(1);

		double trueValue = 0;
		
		// assignment score
		double prior = -Math.log(2) - 2 * Math.log(3) - Math.log(7);
		trueValue += prior;
		
		// word score
		double wordLike = -2 * Math.log(2) - 3 * Math.log(3) - 2 * Math.log(5) - 2 * Math.log(7);
		trueValue += wordLike;
		
		// edge score
		double edgeLike = logNotProb(-Math.sqrt(2)) + logNotProb(-2) + logProb(-Math.sqrt(2)) + 
				2*logProb(-1 - Math.sqrt(5)) + logProb(-3) + 
				logProb(-2) + logNotProb(1 - Math.sqrt(5)) + logNotProb(1 - Math.sqrt(2)) +
				logProb(-Math.sqrt(2)) + logNotProb(0) + logNotProb(-Math.sqrt(2));
		trueValue += edgeLike;
				
		JointTextNetworkModel model = 
				new JointTextNetworkModel(emails, T, 2, rng, true, true, true, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		
		for (int iter = 0; iter < numIter; iter++) {
			model.estimate(1, false);
		}
		assertEquals(trueValue, model.logProb(model.logLike()), 1e-12);
	}
	
	// mixture model after estimation
	@Test
	public void test2() {
		int T = 3;
		int[][] initWordAssignments = null;
		double[][][] initPositions = null;
		double[] initIntercepts = null; // new double[]{-1, 0, 1};
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 3;
		
		double[] gaussianSamples = new double[]{
				0,0,0,0,22,0,0,0,0,0,0,0,
				0,0,0,0,0,0,8,0,0,0,0,4,
				1,5,-10,
				0,0,0,0,-5,0,0,0,0,0,0,0,
				0,0,-3,0,0,0,0,0,0,0,2,1,
				0,0,-10,
				
				0,1,1,2,2,0,0,0,-1,-1,0,0,1,-1,0,0,-1,1,0,0,1,0,0,0,
				-1,10,1
		};
		rng.setGaussian(gaussianSamples);
		
		// 3 samples for initializing missing edges
		
		// for each iteration
		// 12 samples for author position MCMC in each space
		// 3 samples for intercept
		// 3 samples for missing edges
		
		int offset = 12 + 3 + 3;
		int positionStart = 3;
		int interceptStart = 15;
		int missingEdgeStart = 18;

		double[] doubleSamples = new double[1000];

		doubleSamples[positionStart + 11] = Double.POSITIVE_INFINITY;
		doubleSamples[positionStart + offset + 5] = Double.POSITIVE_INFINITY;
		doubleSamples[positionStart + offset + 10] = Double.POSITIVE_INFINITY;
		doubleSamples[interceptStart + 1] = Double.POSITIVE_INFINITY;
		
		// reject last latent position sample, second-to-last intercept
		doubleSamples[positionStart + 2*offset + 11] = Double.POSITIVE_INFINITY;
		doubleSamples[interceptStart + 2*offset + 1] = Double.POSITIVE_INFINITY;
		
		// missing edges are set to 1 by default (double = 0 => edge = 1)
		doubleSamples[0] = 1.0; // initialize incorrect 1st edge
		doubleSamples[2] = 1.0; // initialize incorrect 3rd edge
		
		// after initialization, double = 1 flips the edge
		doubleSamples[missingEdgeStart] = 1.0; // incorrect 1st edge
		doubleSamples[missingEdgeStart + 2] = 1.0; // correct 3rd edge
		doubleSamples[missingEdgeStart + offset + 1] = 1.0; // incorrect 2nd edge
		doubleSamples[missingEdgeStart + 2*offset] = 1.0; // correct 1st edge
		doubleSamples[missingEdgeStart + 2*offset + 1] = 1.0; // correct 2nd edge
		
		rng.setDouble(doubleSamples);
		
		int[] discreteSamples = new int[1000];

		int assignmentIter = 4;
		
		LogRandoms tempRng = new LogRandoms();
		int ind = -1;
		for(int iter = 0; iter < numIter - 1; iter++) {
			for (int i = 0; i < assignmentIter; i++) {
				ind++;
				discreteSamples[ind] = tempRng.nextInt(T);
			}
		}
		
		int[] finalSamples = new int[]{1, 0, 2, 1};
		System.arraycopy(finalSamples, 0, discreteSamples, 2*assignmentIter, finalSamples.length);
		
		rng.setDiscrete(discreteSamples);

		emails.getEmail(0).obscureEdge(1);
		emails.getEmail(0).obscureEdge(3);
		emails.getEmail(1).obscureEdge(1);

		double trueValue = 0;
		
		// assignment score
		double prior = -Math.log(2) - 2 * Math.log(3) - Math.log(7);
		trueValue += prior;
		
		// word score
		double wordLike = -2 * Math.log(2) - 3 * Math.log(3) - 2 * Math.log(5) - 2 * Math.log(7);
		trueValue += wordLike;
		
		// edge score
		double edgeLike = logNotProb(-Math.sqrt(2)) + logNotProb(-2) + logProb(-Math.sqrt(2)) + 
				2*logProb(-1 - Math.sqrt(5)) + logProb(-3) + 
				logProb(-2) + logNotProb(1 - Math.sqrt(5)) + logNotProb(1 - Math.sqrt(2)) +
				logProb(-Math.sqrt(2)) + logNotProb(0) + logNotProb(-Math.sqrt(2));
		trueValue += edgeLike;

		JointTextNetworkModel model = 
				new JointTextNetworkModel(emails, T, 2, rng, true, true, true, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		
		assertEquals(trueValue, model.logProb(model.logLike()), 1e-12);
	}
	
	// mixture model lsm gibbs sampling equations
	@Test
	public void test1a() {
		int T = 3;
		int[][] initWordAssignments = new int[][]{new int[]{1}, new int[]{0}, new int[]{2}, new int[]{1}};
		double[][][] initPositions = new double[][][]{
				new double[][]{new double[]{0.0, 1.0}, new double[]{1.0, 2.0}, new double[]{2.0, 0.0}, new double[]{0.0, 0.0}},
				new double[][]{new double[]{-1.0, -1.0}, new double[]{0.0, 0.0}, new double[]{1.0, -1.0}, new double[]{0.0, 0.0}}, 
				new double[][]{new double[]{-1.0, 1.0}, new double[]{0.0, 0.0}, new double[]{1.0, 0.0}, new double[]{2.0, 1.0}}};
		double[] initIntercepts = new double[]{-1, 0, 1};
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 0;

		double[] trueValue = new double[3];
		
		double edgeLike = logNotProb(-1 - Math.sqrt(2)) + logNotProb(-1 - Math.sqrt(5)) + logProb(-2);
		System.out.println("test edge like: " + edgeLike);
		double wordLike =  - Math.log(8) - Math.log(9) - Math.log(10) + Math.log(2) - Math.log(11);
		System.out.println("test word like: " + wordLike);
		double prior = Math.log(3) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[0] = wordLike + edgeLike + prior;
		
		edgeLike = logNotProb(-Math.sqrt(2)) + logNotProb(-2) + logProb(-Math.sqrt(2));
		System.out.println("test edge like: " + edgeLike);
		wordLike = Math.log(2) - Math.log(6) - Math.log(7) - Math.log(8) + Math.log(2) - Math.log(9);
		System.out.println("test word like: " + wordLike);
		prior = Math.log(3) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[1] = wordLike + edgeLike + prior;
		
		edgeLike = logNotProb(1 - Math.sqrt(2)) + logNotProb(1 - Math.sqrt(5)) + logProb(-2);
		System.out.println("test edge like: " + edgeLike);
		wordLike =  - Math.log(5) - Math.log(6) - Math.log(7) + Math.log(2) - Math.log(8);
		System.out.println("test word like: " + wordLike);
		prior = Math.log(3) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[2] = edgeLike + wordLike + prior;
		
		normalizeLogProb(trueValue);

		JointTextNetworkModel model = 
				new JointTextNetworkModel(emails, T, 2, rng, true, true, false, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		
		double[] testValue = model.getAssignmentModel().getTokenAssignmentDistribution(0, -1, 0, -1, false, 1);
		normalizeLogProb(testValue);
		
		for(int i = 0; i < T; i++) {
			assertEquals(trueValue[i], testValue[i], 1e-12);
		}
		
		trueValue = new double[3];
		
		edgeLike = logProb(-2) + logNotProb(-1 - Math.sqrt(5)) + logNotProb(-3);
		System.out.println("test edge like: " + edgeLike);
		wordLike = 0;
		System.out.println("test word like: " + wordLike);
		prior = Math.log(3) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[0] = wordLike + edgeLike + prior;
		
		edgeLike = logProb(-Math.sqrt(2)) + logNotProb(0) + logNotProb(-Math.sqrt(2));
		System.out.println("test edge like: " + edgeLike);
		wordLike = 0;
		System.out.println("test word like: " + wordLike);
		prior = Math.log(4) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[1] = wordLike + edgeLike + prior;
				
		edgeLike = logProb(-2) + logNotProb(1 - Math.sqrt(5)) + logNotProb(1 - Math.sqrt(2));
		System.out.println("test edge like: " + edgeLike);
		wordLike = 0;
		System.out.println("test word like: " + wordLike);
		prior = Math.log(2) - Math.log(9);
		System.out.println("test prior: " + prior);
		
		trueValue[2] = edgeLike + wordLike + prior;
		
		normalizeLogProb(trueValue);
		
		model = 
				new JointTextNetworkModel(emails, T, 2, rng, true, true, false, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		
		testValue = model.getAssignmentModel().getTokenAssignmentDistribution(2, -1, 3, -1, false, 2);
		normalizeLogProb(testValue);
		
		for(int i = 0; i < T; i++) {
			assertEquals(trueValue[i], testValue[i], 1e-12);
		}
		
		double trueLogProb = logProb(-1 - Math.sqrt(5)); 

		model = 
				new JointTextNetworkModel(emails, T, 2, rng, true, true, false, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		
		double testLogProb = model.getAssignmentModel().getMissingEdgeDistribution(0, 2, 1, 1);

		assertEquals(trueLogProb, testLogProb, 1e-12);
		
		trueLogProb = logNotProb(0); 

		model = 
				new JointTextNetworkModel(emails, T, 2, rng, true, true, false, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		
		testLogProb = model.getAssignmentModel().getMissingEdgeDistribution(1, 3, 1, 0);

		assertEquals(trueLogProb, testLogProb, 1e-12);
	}
	
	// mixture model before estimation
	@Test
	public void test1() {
		int T = 3;
		int[][] initWordAssignments = new int[][]{new int[]{1}, new int[]{0}, new int[]{2}, new int[]{1}};
		double[][][] initPositions = new double[][][]{
				new double[][]{new double[]{0.0, 1.0}, new double[]{1.0, 2.0}, new double[]{2.0, 0.0}, new double[]{0.0, 0.0}},
				new double[][]{new double[]{-1.0, -1.0}, new double[]{0.0, 0.0}, new double[]{1.0, -1.0}, new double[]{0.0, 0.0}}, 
				new double[][]{new double[]{-1.0, 1.0}, new double[]{0.0, 0.0}, new double[]{1.0, 0.0}, new double[]{2.0, 1.0}}};
		double[] initIntercepts = new double[]{-1, 0, 1};
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 0;

		JointTextNetworkModel model = 
				new JointTextNetworkModel(emails, T, 2, rng, true, true, false, false, false, false, false, true, false, false, false, null);
		model.
		initialize(initWordAssignments, null, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		
		double trueValue = 0;
		
		// assignment score
		double prior = -Math.log(2) - 2 * Math.log(3) - Math.log(7);
		trueValue += prior;
		
		// word score
		double wordLike = -2 * Math.log(2) - 3 * Math.log(3) - 2 * Math.log(5) - 2 * Math.log(7);
		trueValue += wordLike;
		
		// edge score
		double edgeLike = logNotProb(-Math.sqrt(2)) + logNotProb(-2) + logProb(-Math.sqrt(2)) + 
				2*logProb(-1 - Math.sqrt(5)) + logProb(-3) + 
				logProb(-2) + logNotProb(1 - Math.sqrt(5)) + logNotProb(1 - Math.sqrt(2)) +
				logProb(-Math.sqrt(2)) + logNotProb(0) + logNotProb(-Math.sqrt(2));
		trueValue += edgeLike;
		
		assertEquals(trueValue, model.logProb(model.logLike()), 1e-12);
	}
}
