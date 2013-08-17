package mixedmembership;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import util.LogRandoms;
import cc.mallet.types.Alphabet;
import data.Email;
import data.EmailCorpus;

public class EdgeScoreBernoulliTest {

	EmailCorpus emails1;
	EmailCorpus emails;
	MockLogRandoms rng;

	@Before
	public void setUp() throws Exception {

		JointTextNetworkModel.DEBUG = true;
		JointTextNetworkModel.DEBUG_OVERRIDE = true;
		
		Alphabet wordDict = new Alphabet();
		wordDict.lookupIndex("a");
		wordDict.lookupIndex("b");
		wordDict.lookupIndex("c");
		wordDict.lookupIndex("d");
		wordDict.lookupIndex("e");

		emails1 = new EmailCorpus(4, wordDict);
		emails1.add(new Email(0, new int[] { 0, 4, 2, 2 }, new int[] { 0, 0, 0,
				1 }, null));
		emails1.add(new Email(2, new int[] { 1, 1, 1 },
				new int[] { 1, 1, 0, 1 }, null));
		emails1.add(new Email(3, new int[] {}, new int[] { 1, 0, 0, 0 }, null));
		emails1.add(new Email(3, new int[] { 0 }, new int[] { 1, 0, 0, 0 },
				null));

		emails1.getEmail(0).obscureEdge(1);
		emails1.getEmail(0).obscureEdge(3);
		emails1.getEmail(1).obscureEdge(1);

		emails = new EmailCorpus(4, wordDict);
		emails.add(new Email(0, new int[] { 0, 4, 2, 2 }, new int[] { 0, 0, 0,
				1 }, null));
		emails.add(new Email(2, new int[] { 1, 1, 1 },
				new int[] { 1, 1, 0, 1 }, null));
		emails.add(new Email(3, new int[] {}, new int[] { 1, 0, 0, 0 }, null));
		emails.add(new Email(3, new int[] { 0 }, new int[] { 1, 1, 0, 0 },
				null));

		emails.getEmail(0).obscureEdge(1);
		emails.getEmail(0).obscureEdge(3);
		emails.getEmail(1).obscureEdge(1);

		rng = new MockLogRandoms();
	}
	
	// asymmetric Bernoulli conditional model after estimation
	@Test
	public void test3() {
		int T = 3;
		int[][] initWordAssignments = null;
		int[][] initEdgeAssignments = null;
		double[][][] initPositions = null;
		double[] initIntercepts = null; 
		double[] gamma = new double[]{4.0};
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 3;
		
		// 3 samples for initializing missing edges
		
		// for each iteration
		// 3 samples for missing edges
		
		int offset = 3;
		int missingEdgeStart = 3;

		double[] doubleSamples = new double[1000];
		
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

		// # words in each doc + # edges in each doc
		int assignmentIter = 4+3+1+1 + 3*4;
		
		LogRandoms tempRng = new LogRandoms();
		int ind = -1;
		for(int iter = 0; iter < numIter - 1; iter++) {
			for(int d = 0; d < emails.size(); d++) {
				int Nd = Math.max(emails.getEmail(d).getLength(), 1);
				for(int i = 0; i < Nd; i++) {
					ind++;
					discreteSamples[ind] = tempRng.nextInt(T);
				}
			}
			for(int d = 0; d < emails.size(); d++) {
				int Nd = Math.max(emails.getEmail(d).getLength(), 1);
				for(int i = 0; i < emails.getNumAuthors()-1; i++) {
					ind++;
					discreteSamples[ind] = tempRng.nextInt(Nd);
				}
			}
		}
		
		int[] finalSamples = new int[]{2,0,2,1, 2,1,0, 1, 1, 1,3,3, 1,2,1, 0,0,0, 0,0,0};
		System.arraycopy(finalSamples, 0, discreteSamples, 2*assignmentIter, finalSamples.length);
		
		rng.setDiscrete(discreteSamples);

		double wordLike = -3 * Math.log(5) - 3 * Math.log(6) - 2 * Math.log(7);
		
		double assignmentScore = 7 * Math.log(2) - Math.log(9) - 3 * Math.log(6) - 2 * Math.log(7) - 2 * Math.log(8);
		
		double edgePrior = -3 * Math.log(4) - 3 * Math.log(3);
		
		double edgeLike = 2*Math.log(3) - 8*Math.log(2) - 3*Math.log(5); 

		double trueValue = wordLike + assignmentScore + edgePrior + edgeLike;
		
		JointTextNetworkModel model = 
				new JointTextNetworkModel(emails, T, -1, rng, true, true, false, true, false, false, false, false, true, false, false, null);
		model.
		initialize(initWordAssignments, initEdgeAssignments, initPositions, initIntercepts, alpha, beta, gamma, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		assertEquals(trueValue, model.logProb(model.logLike()), 1e-12);
	}
	
	// test Erosheva model
	@Test
	public void test2() {
		int T = 3;
		int[][] initWordAssignments = null;
		int[][] initEdgeAssignments = null;
		// True values at end:
		// int[][] initWordAssignments = new int[][]{
		// new int[]{2, 0, 2, 2},
		// new int[]{2, 1, 0},
		// new int[]{},
		// new int[]{0}};
		// int[][] initEdgeAssignments = new int[][]{
		// new int[]{0, 2, 2},
		// new int[]{1, 0, 2},
		// new int[]{1, 0, 2},
		// new int[]{1, 1, 1}};
		double[] alpha = new double[] { 12.0 }; // alpha/T = 2
		double[] beta = new double[] { 10.0 }; // beta/W = 1
		double[] gamma = new double[] { 6.0 }; // gamma/2 = 1
		int numIterations = 3;
		boolean sampleHypers = true;
		int printInterval = 0;
		int saveStateInterval = 0;

		// //////////////////// Set up random number generator
		// //////////////////////

		double[] doubleSamples = new double[1000];

		// 3 samples for initializing missing edges

		// for each iteration
		// 3 samples for missing edges
		// 45 samples for slice sampling hyperparameters (3 hyperparameters, 5
		// iterations, 3 samples in each iteration)

		int discreteIterOffset = 3 + 45;
		int hyperOffset = 15;
		int missingEdgeStart = 3;
		int hyperStart = 6;

		// missing edges are set to 1 by default (double = 0 => edge = 1)
		doubleSamples[0] = 1.0; // initialize incorrect 1st edge
		doubleSamples[2] = 1.0; // initialize incorrect 3rd edge

		// after initialization, double = 1 flips the edge
		doubleSamples[missingEdgeStart] = 1.0; // incorrect 1st edge
		doubleSamples[missingEdgeStart + 2] = 1.0; // correct 3rd edge
		doubleSamples[missingEdgeStart + discreteIterOffset + 1] = 1.0; // incorrect
																		// 2nd
																		// edge
		doubleSamples[missingEdgeStart + 2 * discreteIterOffset] = 1.0; // correct
																		// 1st
																		// edge
		doubleSamples[missingEdgeStart + 2 * discreteIterOffset + 1] = 1.0; // correct
																			// 2nd
																			// edge

		// set lower bound to alpha = 6, then take that sample
		doubleSamples[hyperStart + discreteIterOffset + 1] = Math.log(2);
		// set lower bound to beta = 5, then take that sample
		doubleSamples[hyperStart + hyperOffset + discreteIterOffset + 1] = Math
				.log(2);
		// set lower bound to gamma = 2, then take that sample
		doubleSamples[hyperStart + 2 * hyperOffset + discreteIterOffset + 1] = Math
				.log(3);

		rng.setDouble(doubleSamples);

		int[] discreteSamples = new int[1000];

		int assignmentIterOffset = 3 * 4 + 4 + 3 + 0 + 1;

		LogRandoms tempRng = new LogRandoms();
		for (int i = 0; i < 2 * assignmentIterOffset; i++) {
			discreteSamples[i] = tempRng.nextInt(T);
		}

		int[] finalSamples = new int[] { 2, 0, 2, 2, 2, 1, 0, 0, 0, 2, 2, 1, 0,
				2, 1, 0, 2, 1, 1, 1 };
		System.arraycopy(finalSamples, 0, discreteSamples,
				2 * assignmentIterOffset, finalSamples.length);

		rng.setDiscrete(discreteSamples);

		// //////////////////// Compute true value //////////////////////

		// word score
		double wordValue = Math.log(2) + 3 * Math.log(24) - Math.log(5040)
				- Math.log(120) - Math.log(40320);

		// edge score
		double edgeValue = -6 * Math.log(2) - 3 * Math.log(3);

		// assignment score
		double assignmentValue = 4 * Math.log(120) - Math.log(479001600)
				- Math.log(39916800) - Math.log(40320) - Math.log(362880) + 4
				* Math.log(2) + 4 * Math.log(6) + Math.log(24) + Math.log(720);

		double trueValue = wordValue + edgeValue + assignmentValue;

		// //////////////////// Run test //////////////////////

		JointTextNetworkModel mmbm = new JointTextNetworkModel(emails1, T, -1,
				rng, true, true, false, true, true, true, false, false, false,
				false, false, null);
		mmbm.initialize(initWordAssignments, initEdgeAssignments, null, null,
				alpha, beta, gamma, false, printInterval, saveStateInterval,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		mmbm.estimate(numIterations, sampleHypers);

		assertEquals(trueValue, mmbm.logProb(), 1e-12);
	}

	// test joint (exchangeable) symmetric Bernoulli model
	@Test
	public void test1() {
		int T = 3;
		int[][] initWordAssignments = null;
		int[][] initEdgeAssignments = null;
		// True values at end:
		// int[][] initWordAssignments = new int[][]{
		// new int[]{2, 0, 2, 2},
		// new int[]{2, 1, 0},
		// new int[]{},
		// new int[]{0}};
		// int[][] initEdgeAssignments = new int[][]{
		// new int[]{0, 2, 2},
		// new int[]{1, 0, 2},
		// new int[]{1, 0, 2},
		// new int[]{1, 1, 1}};
		double[] alpha = new double[] { 12.0 }; // alpha/T = 2
		double[] beta = new double[] { 10.0 }; // beta/W = 1
		double[] gamma = new double[] { 6.0 }; // gamma/2 = 1
		int numIterations = 3;
		boolean sampleHypers = true;
		int printInterval = 0;
		int saveStateInterval = 0;

		// //////////////////// Set up random number generator
		// //////////////////////

		double[] doubleSamples = new double[1000];

		// 3 samples for initializing missing edges

		// for each iteration
		// 3 samples for missing edges
		// 45 samples for slice sampling hyperparameters (3 hyperparameters, 5
		// iterations, 3 samples in each iteration)

		int discreteIterOffset = 3 + 45;
		int hyperOffset = 15;
		int missingEdgeStart = 3;
		int hyperStart = 6;

		// missing edges are set to 1 by default (double = 0 => edge = 1)
		doubleSamples[0] = 1.0; // initialize incorrect 1st edge
		doubleSamples[2] = 1.0; // initialize incorrect 3rd edge

		// after initialization, double = 1 flips the edge
		doubleSamples[missingEdgeStart] = 1.0; // incorrect 1st edge
		doubleSamples[missingEdgeStart + 2] = 1.0; // correct 3rd edge
		doubleSamples[missingEdgeStart + discreteIterOffset + 1] = 1.0; // incorrect
																		// 2nd
																		// edge
		doubleSamples[missingEdgeStart + 2 * discreteIterOffset] = 1.0; // correct
																		// 1st
																		// edge
		doubleSamples[missingEdgeStart + 2 * discreteIterOffset + 1] = 1.0; // correct
																			// 2nd
																			// edge

		// set lower bound to alpha = 6, then take that sample
		doubleSamples[hyperStart + discreteIterOffset + 1] = Math.log(2);
		// set lower bound to beta = 5, then take that sample
		doubleSamples[hyperStart + hyperOffset + discreteIterOffset + 1] = Math
				.log(2);
		// set lower bound to gamma = 2, then take that sample
		doubleSamples[hyperStart + 2 * hyperOffset + discreteIterOffset + 1] = Math
				.log(3);

		rng.setDouble(doubleSamples);

		int[] discreteSamples = new int[1000];

		int assignmentIterOffset = 3 * 4 + 4 + 3 + 0 + 1;

		LogRandoms tempRng = new LogRandoms();
		for (int i = 0; i < 2 * assignmentIterOffset; i++) {
			discreteSamples[i] = tempRng.nextInt(T);
		}

		int[] finalSamples = new int[] { 2, 0, 2, 2, 2, 1, 0, 0, 0, 2, 2, 1, 0,
				2, 1, 0, 2, 1, 1, 1 };
		System.arraycopy(finalSamples, 0, discreteSamples,
				2 * assignmentIterOffset, finalSamples.length);

		rng.setDiscrete(discreteSamples);

		// //////////////////// Compute true value //////////////////////

		// word score
		double wordValue = Math.log(2) + 3 * Math.log(24) - Math.log(5040)
				- Math.log(120) - Math.log(40320);

		// edge score
		double edgeValue = -8 * Math.log(2) - Math.log(3) - Math.log(6);

		// assignment score
		double assignmentValue = 4 * Math.log(120) - Math.log(479001600)
				- Math.log(39916800) - Math.log(40320) - Math.log(362880) + 4
				* Math.log(2) + 4 * Math.log(6) + Math.log(24) + Math.log(720);

		double trueValue = wordValue + edgeValue + assignmentValue;

		// //////////////////// Run test //////////////////////

		JointTextNetworkModel mmbm = new JointTextNetworkModel(emails1, T, -1,
				rng, true, true, false, true, false, true, false, false, false,
				false, false, null);
		mmbm.initialize(initWordAssignments, initEdgeAssignments, null, null,
				alpha, beta, gamma, false, printInterval, saveStateInterval,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		mmbm.estimate(numIterations, sampleHypers);

		assertEquals(trueValue, mmbm.logProb(), 1e-12);
	}
}
