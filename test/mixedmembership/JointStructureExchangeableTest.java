package mixedmembership;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import util.LogRandoms;
import cc.mallet.types.Alphabet;
import data.Email;
import data.EmailCorpus;

public class JointStructureExchangeableTest {

	EmailCorpus emails1;
	EmailCorpus emails2;
	EmailCorpus emails3;
	EmailCorpus emails4;
	EmailCorpus emails5;
	EmailCorpus emails6;
	EmailCorpus emails7;
	MockLogRandoms rng;

	@Before
	public void setUp() throws Exception {

		emails1 = new EmailCorpus(2);
		emails1.add(new Email(0, new int[] { 0, 1 }, null));

		emails2 = new EmailCorpus(4);
		emails2.add(new Email(0, new int[] { 0, 0, 0, 1 }, null));
		emails2.add(new Email(2, new int[] { 1, 1, 0, 1 }, null));

		emails3 = new EmailCorpus(4);
		emails3.add(new Email(0, new int[] { 0, 0, 0, 1 }, null));
		emails3.add(new Email(2, new int[] { 1, 1, 0, 1 }, null));
		emails3.add(new Email(3, new int[] { 1, 0, 0, 0 }, null));
		emails3.add(new Email(3, new int[] { 1, 0, 0, 0 }, null));

		Alphabet wordDict = new Alphabet();
		wordDict.lookupIndex("a");
		wordDict.lookupIndex("b");
		wordDict.lookupIndex("c");
		wordDict.lookupIndex("d");
		wordDict.lookupIndex("e");

		emails4 = new EmailCorpus(1, wordDict);
		emails4.add(new Email(0, new int[] { 0, 4, 2, 2 }, null, null));

		emails5 = new EmailCorpus(2, wordDict);
		emails5.add(new Email(0, new int[] { 0, 4, 2, 2 }, new int[] { 0, 1 },
				null));

		emails6 = new EmailCorpus(4, wordDict);
		emails6.add(new Email(0, new int[] { 0, 4, 2, 2 }, new int[] { 0, 0, 0,
				1 }, null));
		emails6.add(new Email(2, new int[] { 1, 1, 1 },
				new int[] { 1, 1, 0, 1 }, null));
		emails6.add(new Email(3, new int[] {}, new int[] { 1, 0, 0, 0 }, null));
		emails6.add(new Email(3, new int[] { 0 }, new int[] { 1, 0, 0, 0 },
				null));

		emails7 = new EmailCorpus(4, wordDict);
		emails7.add(new Email(0, new int[] { 0, 4, 2, 2, 1, 1 }, new int[] { 0,
				0, 0, 1 }, null));
		emails7.add(new Email(2,
				new int[] { 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, new int[] {
						1, 1, 0, 1 }, null));
		emails7.add(new Email(3, new int[] {}, new int[] { 1, 0, 0, 0 }, null));
		emails7.add(new Email(3, new int[] { 0, 0, 1, 2, 4, 1 }, new int[] { 1,
				0, 0, 0 }, null));

		rng = new MockLogRandoms();

		JointTextNetworkModel.DEBUG = true;
		JointTextNetworkModel.DEBUG_OVERRIDE = true;
	}

	private static double logProb(double arg) {
		return arg - Math.log(1 + Math.exp(arg));
	}

	private static double logNotProb(double arg) {
		return -Math.log(1 + Math.exp(arg));
	}

	private static double prob(double arg) {
		return Math.exp(logProb(arg));
	}

	private static double notProb(double arg) {
		return Math.exp(logNotProb(arg));
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

	// exchangeable joint model with alt. space sampler, with sampling theta,
	// with missing edges, hyperparameter sampling
	@Test
	public void test6() {
		int T;
		int K;
		double[][][] initPositions;
		double[] initIntercepts;
		double[] trueValue;

		int numIterations = 3;

		int[][] initEdgeAssignments;

		T = 3;
		K = 2;
		double[] beta = new double[] { 6.0 };
		double[] alpha = new double[] { 6.0 };
		initEdgeAssignments = null;
		initPositions = null;
		initIntercepts = null;

		double[] doubleSamples = new double[1000];

		// 3 samples for initializing missing edges

		// for each iteration
		// 3 samples for missing edges
		// 15 (alpha) + 15 (beta) samples for slice sampling hyperparameters
		// 12 samples for author position MCMC in each space
		// 3 samples for intercept MCMC in each space

		int offset = 3 + (15 + 15) + 12 + 3;
		int missingEdgeStart = 3;
		int hyperStart = 6;
		int positionStart = 36;
		int interceptStart = 48;

		// reject last latent position sample, last intercept
		doubleSamples[positionStart + 2 * offset + 11] = Double.POSITIVE_INFINITY;

		// reject last intercept sample
		doubleSamples[interceptStart + 2 * offset + 2] = Double.POSITIVE_INFINITY;

		// missing edges are set to 1 by default (double = 0 => edge = 1)
		doubleSamples[0] = 1.0; // initialize incorrect 1st edge
		doubleSamples[2] = 1.0; // initialize incorrect 3rd edge

		// after initialization, double = 1 flips the edge
		doubleSamples[missingEdgeStart] = 1.0; // incorrect 1st edge
		doubleSamples[missingEdgeStart + 2] = 1.0; // correct 3rd edge
		doubleSamples[missingEdgeStart + offset + 1] = 1.0; // incorrect 2nd
															// edge
		doubleSamples[missingEdgeStart + 2 * offset] = 1.0; // correct 1st edge
		doubleSamples[missingEdgeStart + 2 * offset + 1] = 1.0; // correct 2nd
																// edge

		// set lower bound to alpha = 3, then take that sample
		doubleSamples[hyperStart + offset + 1] = Math.log(2);

		rng.setDouble(doubleSamples);

		int assignmentIter = (emails7.getNumAuthors() - 1) * emails7.size();
		for (int i = 0; i < emails7.size(); i++) {
			assignmentIter += emails7.getDocument(i).getLength();
		}
		int[] discreteSamples = new int[1000];
		LogRandoms tempRng = new LogRandoms();
		int ind = -1;
		for (int iter = 0; iter < numIterations - 1; iter++) {
			for (int d = 0; d < emails7.size(); d++) {
				int Nd = emails7.getEmail(d).getLength();
				for (int i = 0; i < Nd; i++) {
					ind++;
					discreteSamples[ind] = tempRng.nextInt(T);
				}
			}
			for (int d = 0; d < emails7.size(); d++) {
				for (int i = 0; i < emails7.getNumAuthors() - 1; i++) {
					ind++;
					discreteSamples[ind] = tempRng.nextInt(T);
				}
			}
		}
		int[] finalSamples = new int[] {
				2,0,2,0,1,1, 
				0,2,1,1,1,1,1,1,1,1,1,1, 
				0,1,1,2,1,2,				
		
				1,0,2,
				0,2,1,
				2,1,0,
				2,2,2};
		System.arraycopy(finalSamples, 0, discreteSamples, 2 * assignmentIter,
				finalSamples.length);

		rng.setDiscrete(discreteSamples);

		emails7.getEmail(0).obscureEdge(1);
		emails7.getEmail(0).obscureEdge(3);
		emails7.getEmail(1).obscureEdge(1);

		JointTextNetworkModel model = new JointTextNetworkModel(emails7, T, K, rng, true,
				true, true, false, false, true, true, false, false, false, false, null);

		model.initialize(null, initEdgeAssignments, initPositions,
				initIntercepts, alpha, beta, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIterations, true);

		// int[][] initWordAssignments = new int[][]{
		// new int[]{2, 0, 2, 0, 1, 1},
		// new int[]{0, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		// new int[]{},
		// new int[]{0, 1, 1, 2, 1, 2}};
		// int[][] initEdgeAssignments = new int[][]{
		// new int[]{1, 0, 2},
		// new int[]{0, 2, 1},
		// new int[]{2, 1, 0},
		// new int[]{2, 2, 2}};
		// initPositions = new double[][][]{
		// new double[][]{
		// new double[]{0.0, 1.0},
		// new double[]{1.0, 2.0},
		// new double[]{2.0, 0.0},
		// new double[]{0.0, 0.0}},
		// new double[][]{
		// new double[]{-1.0, -1.0},
		// new double[]{0.0, 0.0},
		// new double[]{1.0, -1.0},
		// new double[]{0.0, 0.0}},
		// new double[][]{
		// new double[]{-1.0, 1.0},
		// new double[]{0.0, 0.0},
		// new double[]{1.0, 0.0},
		// new double[]{2.0, 1.0}}};
		//
		// initIntercepts = new double[]{-1, 0, 1};
		// featureProportions = new double[][]{
		// new double[]{1/3.0, 1/3.0, 1/3.0},
		// new double[]{1/6.0, 4/6.0, 1/6.0},
		// new double[]{1/3.0, 1/3.0, 1/3.0},
		// new double[]{1/6.0, 2/6.0, 3/6.0}};

		trueValue = new double[4];

		double edgeValue;

		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(2)) + 1 / 3.0
				* notProb(-Math.sqrt(2)) + 1 / 3.0 * notProb(1 - Math.sqrt(2));
		trueValue[0] += Math.log(edgeValue);
		trueValue[1] += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(5)) + 1 / 3.0
				* notProb(-2) + 1 / 3.0 * notProb(1 - Math.sqrt(5));
		trueValue[0] += Math.log(edgeValue);
		trueValue[2] += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * prob(-2) + 1 / 3.0 * prob(-Math.sqrt(2)) + 1
				/ 3.0 * prob(-2);
		trueValue[0] += Math.log(edgeValue);
		trueValue[3] += Math.log(edgeValue);

		edgeValue = 1 / 6.0 * prob(-1 - Math.sqrt(5)) + 4 / 6.0 * prob(-2) + 1
				/ 6.0 * prob(1 - Math.sqrt(5));
		trueValue[2] += Math.log(edgeValue);
		trueValue[0] += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * prob(-1 - Math.sqrt(5)) + 4 / 6.0
				* prob(-Math.sqrt(2)) + 1 / 6.0 * prob(0);
		trueValue[2] += Math.log(edgeValue);
		trueValue[1] += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * prob(-3) + 4 / 6.0 * prob(-Math.sqrt(2)) + 1
				/ 6.0 * prob(1 - Math.sqrt(2));
		trueValue[2] += Math.log(edgeValue);
		trueValue[3] += Math.log(edgeValue);

		edgeValue = 1 / 3.0 * prob(-2) + 1 / 3.0 * prob(-Math.sqrt(2)) + 1
				/ 3.0 * prob(-2);
		trueValue[3] += Math.log(edgeValue);
		trueValue[0] += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(5)) + 1 / 3.0 * notProb(0)
				+ 1 / 3.0 * notProb(1 - Math.sqrt(5));
		trueValue[3] += Math.log(edgeValue);
		trueValue[1] += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-3) + 1 / 3.0 * notProb(-Math.sqrt(2))
				+ 1 / 3.0 * notProb(1 - Math.sqrt(2));
		trueValue[3] += Math.log(edgeValue);
		trueValue[2] += Math.log(edgeValue);

		edgeValue = 1 / 6.0 * prob(-2) + 2 / 6.0 * prob(-Math.sqrt(2)) + 3
				/ 6.0 * prob(-2);
		trueValue[3] += Math.log(edgeValue);
		trueValue[0] += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * notProb(-1 - Math.sqrt(5)) + 2 / 6.0 * notProb(0)
				+ 3 / 6.0 * notProb(1 - Math.sqrt(5));
		trueValue[3] += Math.log(edgeValue);
		trueValue[1] += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * notProb(-3) + 2 / 6.0 * notProb(-Math.sqrt(2))
				+ 3 / 6.0 * notProb(1 - Math.sqrt(2));
		trueValue[3] += Math.log(edgeValue);
		trueValue[2] += Math.log(edgeValue);

		for (int i = 0; i < 4; i++) {
			assertEquals(
					trueValue[i],
					((EdgeScoreLatentSpaceMarginalizedAssignments) model
							.getAlternativeLatentSpaceSampler()).positionLogLikes[i],
					1e-12);
		}
	}
	
	// edge model only with alt. space sampler, with sampling theta, with
	// missing edges, hyperparameter sampling
	@Test
	public void test4() {
		int T;
		int K;
		double[][][] initPositions;
		double[] initIntercepts;
		double[] trueValue;

		int numIterations = 3;

		int[][] initEdgeAssignments;

		T = 3;
		K = 2;
		double[] beta = null;
		double[] alpha = new double[] { 6.0 }; // will be 3.0
		initEdgeAssignments = null;
		initPositions = null;
		initIntercepts = null;

		double[] doubleSamples = new double[1000];

		// 3 samples for initializing missing edges

		// for each iteration
		// 3 samples for missing edges
		// 15 samples for slice sampling hyperparameters
		// 12 samples for author position MCMC in each space
		// 3 samples for intercept MCMC in each space

		int offset = 3 + 15 + 12 + 3;
		int missingEdgeStart = 3;
		int hyperStart = 6;
		int positionStart = 21;
		int interceptStart = 33;
		
		// reject last latent position sample, last intercept
		doubleSamples[positionStart + 2 * offset + 11] = Double.POSITIVE_INFINITY;

		// reject last intercept sample
		doubleSamples[interceptStart + 2 * offset + 2] = Double.POSITIVE_INFINITY;

		// missing edges are set to 1 by default (double = 0 => edge = 1)
		doubleSamples[0] = 1.0; // initialize incorrect 1st edge
		doubleSamples[2] = 1.0; // initialize incorrect 3rd edge
		doubleSamples[missingEdgeStart] = 1.0; // incorrect 1st edge
		doubleSamples[missingEdgeStart + 2] = 1.0; // correct 3rd edge
		doubleSamples[missingEdgeStart + offset + 1] = 1.0; // incorrect 2nd
															// edge
		doubleSamples[missingEdgeStart + 2 * offset] = 1.0; // correct 1st edge
		doubleSamples[missingEdgeStart + 2 * offset + 1] = 1.0; // correct 2nd
																// edge

		// set lower bound to alpha = 3, then take that sample
		doubleSamples[hyperStart + 2 * offset + 1] = Math.log(2);

		rng.setDouble(doubleSamples);

		int assignmentIter = (emails3.getNumAuthors() - 1) * emails3.size();
		int[] discreteSamples = new int[1000];
		LogRandoms tempRng = new LogRandoms();
		int ind = -1;
		for (int iter = 0; iter < numIterations - 1; iter++) {
			for (int d = 0; d < emails3.size(); d++) {
				for (int i = 0; i < emails3.getNumAuthors() - 1; i++) {
					ind++;
					discreteSamples[ind] = tempRng.nextInt(T);
				}
			}
		}
		int[] finalSamples = new int[] { 0, 1, 2, 1, 1, 1, 1, 0, 2, 2, 2, 1 };
		System.arraycopy(finalSamples, 0, discreteSamples, 2 * assignmentIter,
				finalSamples.length);
		rng.setDiscrete(discreteSamples);
		
		emails3.getEmail(0).obscureEdge(1);
		emails3.getEmail(0).obscureEdge(3);
		emails3.getEmail(1).obscureEdge(1);

		JointTextNetworkModel model = new JointTextNetworkModel(emails3, T, K, rng, false,
				true, true, false, false, true, true, false, false, false, false, null);

		model.initialize(null, initEdgeAssignments, initPositions,
				initIntercepts, alpha, beta, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIterations, true);

		// int[][] initEdgeAssignments = new int[][]{new int[]{1, 0, 2}, new
		// int[]{1, 1, 1}, new int[]{2, 1, 0}, new int[]{1, 2, 2}};
		// initPositions = new double[][][]{
		// new double[][]{
		// new double[]{0.0, 1.0},
		// new double[]{1.0, 2.0},
		// new double[]{2.0, 0.0},
		// new double[]{0.0, 0.0}},
		// new double[][]{
		// new double[]{-1.0, -1.0},
		// new double[]{0.0, 0.0},
		// new double[]{1.0, -1.0},
		// new double[]{0.0, 0.0}},
		// new double[][]{
		// new double[]{-1.0, 1.0},
		// new double[]{0.0, 0.0},
		// new double[]{1.0, 0.0},
		// new double[]{2.0, 1.0}}};
		//
		// initIntercepts = new double[]{-1, 0, 1};
		// featureProportions = new double[][]{
		// new double[]{1/3.0, 1/3.0, 1/3.0},
		// new double[]{1/6.0, 4/6.0, 1/6.0},
		// new double[]{1/3.0, 1/3.0, 1/3.0},
		// new double[]{1/6.0, 2/6.0, 3/6.0}};

		trueValue = new double[4];

		double edgeValue;

		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(2)) + 1 / 3.0
				* notProb(-Math.sqrt(2)) + 1 / 3.0 * notProb(1 - Math.sqrt(2));
		trueValue[0] += Math.log(edgeValue);
		trueValue[1] += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(5)) + 1 / 3.0
				* notProb(-2) + 1 / 3.0 * notProb(1 - Math.sqrt(5));
		trueValue[0] += Math.log(edgeValue);
		trueValue[2] += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * prob(-2) + 1 / 3.0 * prob(-Math.sqrt(2)) + 1
				/ 3.0 * prob(-2);
		trueValue[0] += Math.log(edgeValue);
		trueValue[3] += Math.log(edgeValue);

		edgeValue = 1 / 6.0 * prob(-1 - Math.sqrt(5)) + 4 / 6.0 * prob(-2) + 1
				/ 6.0 * prob(1 - Math.sqrt(5));
		trueValue[2] += Math.log(edgeValue);
		trueValue[0] += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * prob(-1 - Math.sqrt(5)) + 4 / 6.0
				* prob(-Math.sqrt(2)) + 1 / 6.0 * prob(0);
		trueValue[2] += Math.log(edgeValue);
		trueValue[1] += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * prob(-3) + 4 / 6.0 * prob(-Math.sqrt(2)) + 1
				/ 6.0 * prob(1 - Math.sqrt(2));
		trueValue[2] += Math.log(edgeValue);
		trueValue[3] += Math.log(edgeValue);

		edgeValue = 1 / 3.0 * prob(-2) + 1 / 3.0 * prob(-Math.sqrt(2)) + 1
				/ 3.0 * prob(-2);
		trueValue[3] += Math.log(edgeValue);
		trueValue[0] += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(5)) + 1 / 3.0 * notProb(0)
				+ 1 / 3.0 * notProb(1 - Math.sqrt(5));
		trueValue[3] += Math.log(edgeValue);
		trueValue[1] += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-3) + 1 / 3.0 * notProb(-Math.sqrt(2))
				+ 1 / 3.0 * notProb(1 - Math.sqrt(2));
		trueValue[3] += Math.log(edgeValue);
		trueValue[2] += Math.log(edgeValue);

		edgeValue = 1 / 6.0 * prob(-2) + 2 / 6.0 * prob(-Math.sqrt(2)) + 3
				/ 6.0 * prob(-2);
		trueValue[3] += Math.log(edgeValue);
		trueValue[0] += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * notProb(-1 - Math.sqrt(5)) + 2 / 6.0 * notProb(0)
				+ 3 / 6.0 * notProb(1 - Math.sqrt(5));
		trueValue[3] += Math.log(edgeValue);
		trueValue[1] += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * notProb(-3) + 2 / 6.0 * notProb(-Math.sqrt(2))
				+ 3 / 6.0 * notProb(1 - Math.sqrt(2));
		trueValue[3] += Math.log(edgeValue);
		trueValue[2] += Math.log(edgeValue);

		for (int i = 0; i < 4; i++) {
			assertEquals(
					trueValue[i],
					((EdgeScoreLatentSpaceMarginalizedAssignments) model
							.getAlternativeLatentSpaceSampler()).positionLogLikes[i],
					1e-12);
		}
	}

	// edge model only with alt. space sampler, with sampling theta, with
	// missing edges, no hyperparameter sampling
	@Test
	public void test3() {
		int T;
		int K;
		double[][][] initPositions;
		double[] initIntercepts;
		double[] trueValue;

		int numIterations = 3;

		int[][] initEdgeAssignments;

		T = 3;
		K = 2;
		double[] beta = null;
		double[] alpha = new double[] { 3.0 };
		initEdgeAssignments = null;
		initPositions = null;
		initIntercepts = null;

		double[] doubleSamples = new double[100];

		// 3 samples for initializing missing edges

		// for each iteration
		// 12 samples for author position MCMC in each space
		// 3 samples for intercept in each space
		// 3 samples for missing edges

		int offset = 3 + 12 + 3;
		int missingEdgeStart = 3;
		int positionStart = 6;
		int interceptStart = 18;

		// reject last latent position sample
		doubleSamples[positionStart + 2 * offset + 11] = Double.POSITIVE_INFINITY;

		// reject last intercept sample
		doubleSamples[interceptStart + 2 * offset + 2] = Double.POSITIVE_INFINITY;

		// missing edges are set to 1 by default (double = 0 => edge = 1)
		doubleSamples[0] = 1.0; // initialize incorrect 1st edge
		doubleSamples[2] = 1.0; // initialize incorrect 3rd edge
		doubleSamples[missingEdgeStart] = 1.0; // incorrect 1st edge
		doubleSamples[missingEdgeStart + 2] = 1.0; // correct 3rd edge
		doubleSamples[missingEdgeStart + offset + 1] = 1.0; // incorrect 2nd
															// edge
		doubleSamples[missingEdgeStart + 2 * offset] = 1.0; // correct 1st edge
		doubleSamples[missingEdgeStart + 2 * offset + 1] = 1.0; // correct 2nd
																// edge

		rng.setDouble(doubleSamples);

		int assignmentIter = (emails3.getNumAuthors() - 1) * emails3.size();
		int[] discreteSamples = new int[1000];
		LogRandoms tempRng = new LogRandoms();
		int ind = -1;
		for (int iter = 0; iter < numIterations - 1; iter++) {
			for (int d = 0; d < emails3.size(); d++) {
				for (int i = 0; i < emails3.getNumAuthors() - 1; i++) {
					ind++;
					discreteSamples[ind] = tempRng.nextInt(T);
				}
			}
		}
		int[] finalSamples = new int[] { 0, 1, 2, 1, 1, 1, 1, 0, 2, 2, 2, 1 };
		System.arraycopy(finalSamples, 0, discreteSamples, 2 * assignmentIter,
				finalSamples.length);
		rng.setDiscrete(discreteSamples);

		emails3.getEmail(0).obscureEdge(1);
		emails3.getEmail(0).obscureEdge(3);
		emails3.getEmail(1).obscureEdge(1);

		JointTextNetworkModel model = new JointTextNetworkModel(emails3, T, K, rng, false,
				true, true, false, false, true, true, false, false, false, false, null);

		model.initialize(null, initEdgeAssignments, initPositions,
				initIntercepts, alpha, beta, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIterations, false);

		// int[][] initEdgeAssignments = new int[][]{new int[]{1, 0, 2}, new
		// int[]{1, 1, 1}, new int[]{2, 1, 0}, new int[]{1, 2, 2}};
		// initPositions = new double[][][]{
		// new double[][]{
		// new double[]{0.0, 1.0},
		// new double[]{1.0, 2.0},
		// new double[]{2.0, 0.0},
		// new double[]{0.0, 0.0}},
		// new double[][]{
		// new double[]{-1.0, -1.0},
		// new double[]{0.0, 0.0},
		// new double[]{1.0, -1.0},
		// new double[]{0.0, 0.0}},
		// new double[][]{
		// new double[]{-1.0, 1.0},
		// new double[]{0.0, 0.0},
		// new double[]{1.0, 0.0},
		// new double[]{2.0, 1.0}}};
		//
		// initIntercepts = new double[]{-1, 0, 1};
		// featureProportions = new double[][]{
		// new double[]{1/3.0, 1/3.0, 1/3.0},
		// new double[]{1/6.0, 4/6.0, 1/6.0},
		// new double[]{1/3.0, 1/3.0, 1/3.0},
		// new double[]{1/6.0, 2/6.0, 3/6.0}};

		trueValue = new double[4];

		double edgeValue;

		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(2)) + 1 / 3.0
				* notProb(-Math.sqrt(2)) + 1 / 3.0 * notProb(1 - Math.sqrt(2));
		trueValue[0] += Math.log(edgeValue);
		trueValue[1] += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(5)) + 1 / 3.0
				* notProb(-2) + 1 / 3.0 * notProb(1 - Math.sqrt(5));
		trueValue[0] += Math.log(edgeValue);
		trueValue[2] += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * prob(-2) + 1 / 3.0 * prob(-Math.sqrt(2)) + 1
				/ 3.0 * prob(-2);
		trueValue[0] += Math.log(edgeValue);
		trueValue[3] += Math.log(edgeValue);

		edgeValue = 1 / 6.0 * prob(-1 - Math.sqrt(5)) + 4 / 6.0 * prob(-2) + 1
				/ 6.0 * prob(1 - Math.sqrt(5));
		trueValue[2] += Math.log(edgeValue);
		trueValue[0] += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * prob(-1 - Math.sqrt(5)) + 4 / 6.0
				* prob(-Math.sqrt(2)) + 1 / 6.0 * prob(0);
		trueValue[2] += Math.log(edgeValue);
		trueValue[1] += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * prob(-3) + 4 / 6.0 * prob(-Math.sqrt(2)) + 1
				/ 6.0 * prob(1 - Math.sqrt(2));
		trueValue[2] += Math.log(edgeValue);
		trueValue[3] += Math.log(edgeValue);

		edgeValue = 1 / 3.0 * prob(-2) + 1 / 3.0 * prob(-Math.sqrt(2)) + 1
				/ 3.0 * prob(-2);
		trueValue[3] += Math.log(edgeValue);
		trueValue[0] += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(5)) + 1 / 3.0 * notProb(0)
				+ 1 / 3.0 * notProb(1 - Math.sqrt(5));
		trueValue[3] += Math.log(edgeValue);
		trueValue[1] += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-3) + 1 / 3.0 * notProb(-Math.sqrt(2))
				+ 1 / 3.0 * notProb(1 - Math.sqrt(2));
		trueValue[3] += Math.log(edgeValue);
		trueValue[2] += Math.log(edgeValue);

		edgeValue = 1 / 6.0 * prob(-2) + 2 / 6.0 * prob(-Math.sqrt(2)) + 3
				/ 6.0 * prob(-2);
		trueValue[3] += Math.log(edgeValue);
		trueValue[0] += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * notProb(-1 - Math.sqrt(5)) + 2 / 6.0 * notProb(0)
				+ 3 / 6.0 * notProb(1 - Math.sqrt(5));
		trueValue[3] += Math.log(edgeValue);
		trueValue[1] += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * notProb(-3) + 2 / 6.0 * notProb(-Math.sqrt(2))
				+ 3 / 6.0 * notProb(1 - Math.sqrt(2));
		trueValue[3] += Math.log(edgeValue);
		trueValue[2] += Math.log(edgeValue);

		for (int i = 0; i < 4; i++) {
			assertEquals(
					trueValue[i],
					((EdgeScoreLatentSpaceMarginalizedAssignments) model
							.getAlternativeLatentSpaceSampler()).positionLogLikes[i],
					1e-12);
		}
	}

	// edge model only using alt. space sampler, with sampling theta, starting
	// from null
	@Test
	public void test1() {
		int T;
		int K;
		double[][][] initPositions;
		double[] initIntercepts;
		double trueValue;

		int numIterations = 3;

		int[][] initEdgeAssignments;

		T = 3;
		K = 2;
		double[] beta = null;
		double[] alpha = new double[] { 3.0 };
		initEdgeAssignments = null;
		initPositions = null;
		initIntercepts = null;

		JointTextNetworkModel model = new JointTextNetworkModel(emails3, T, K, rng, false,
				true, true, false, false, true, true, false, false, false, false, null);

		model.initialize(null, initEdgeAssignments, initPositions,
				initIntercepts, alpha, beta, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIterations, false);

		// featureProportions = new double[][]{
		// new double[]{1/3.0, 1/3.0, 1/3.0},
		// new double[]{1/6.0, 4/6.0, 1/6.0},
		// new double[]{1/3.0, 1/3.0, 1/3.0},
		// new double[]{1/6.0, 2/6.0, 3/6.0}};

		trueValue = 0;

		double edgeValue = 0;
		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(2)) + 1 / 3.0
				* notProb(-Math.sqrt(2)) + 1 / 3.0 * notProb(1 - Math.sqrt(2));
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(5)) + 1 / 3.0
				* notProb(-2) + 1 / 3.0 * notProb(1 - Math.sqrt(5));
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * prob(-2) + 1 / 3.0 * prob(-Math.sqrt(2)) + 1
				/ 3.0 * prob(-2);
		trueValue += Math.log(edgeValue);

		edgeValue = 1 / 6.0 * prob(-1 - Math.sqrt(5)) + 4 / 6.0 * prob(-2) + 1
				/ 6.0 * prob(1 - Math.sqrt(5));
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * prob(-1 - Math.sqrt(5)) + 4 / 6.0
				* prob(-Math.sqrt(2)) + 1 / 6.0 * prob(0);
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * prob(-3) + 4 / 6.0 * prob(-Math.sqrt(2)) + 1
				/ 6.0 * prob(1 - Math.sqrt(2));
		trueValue += Math.log(edgeValue);

		edgeValue = 1 / 3.0 * prob(-2) + 1 / 3.0 * prob(-Math.sqrt(2)) + 1
				/ 3.0 * prob(-2);
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(5)) + 1 / 3.0 * notProb(0)
				+ 1 / 3.0 * notProb(1 - Math.sqrt(5));
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-3) + 1 / 3.0 * notProb(-Math.sqrt(2))
				+ 1 / 3.0 * notProb(1 - Math.sqrt(2));
		trueValue += Math.log(edgeValue);

		edgeValue = 1 / 6.0 * prob(-2) + 2 / 6.0 * prob(-Math.sqrt(2)) + 3
				/ 6.0 * prob(-2);
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * notProb(-1 - Math.sqrt(5)) + 2 / 6.0 * notProb(0)
				+ 3 / 6.0 * notProb(1 - Math.sqrt(5));
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * notProb(-3) + 2 / 6.0 * notProb(-Math.sqrt(2))
				+ 3 / 6.0 * notProb(1 - Math.sqrt(2));
		trueValue += Math.log(edgeValue);

		assertEquals(trueValue, model.logLike(), 1e-12);
	}

	// edge model only using alt. space sampler, with sampling theta
	@Test
	public void test5() {
		int T;
		int K;
		double[][][] initPositions;
		double[] initIntercepts;
		double trueValue;

		int numIterations = 3;

		int[][] initEdgeAssignments;

		T = 3;
		K = 2;
		double[] beta = null;
		double[] alpha = new double[] { 3.0 };
		initEdgeAssignments = new int[][] { new int[] { 0, 0, 0 },
				new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 },
				new int[] { 0, 0, 0 } };
		initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } } };
		initIntercepts = new double[] { 0.0, 0.0, 0.0 };

		int assignmentIter = (emails6.getNumAuthors() - 1) * emails6.size();
		int[] discreteSamples = new int[1000];
		LogRandoms tempRng = new LogRandoms();
		int ind = -1;
		for (int iter = 0; iter < numIterations - 1; iter++) {
			for (int d = 0; d < emails6.size(); d++) {
				for (int i = 0; i < emails6.getNumAuthors() - 1; i++) {
					ind++;
					discreteSamples[ind] = tempRng.nextInt(T);
				}
			}
		}
		int[] finalSamples = new int[] { 0, 1, 2, 1, 1, 1, 1, 0, 2, 2, 2, 1 };
		System.arraycopy(finalSamples, 0, discreteSamples, 2 * assignmentIter,
				finalSamples.length);
		rng.setDiscrete(discreteSamples);

		JointTextNetworkModel model = new JointTextNetworkModel(emails3, T, K, rng, false,
				true, true, false, false, true, true, false, false, false, false, null);

		model.initialize(null, initEdgeAssignments, initPositions,
				initIntercepts, alpha, beta, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIterations, false);

		// featureProportions = new double[][]{
		// new double[]{1/3.0, 1/3.0, 1/3.0},
		// new double[]{1/6.0, 4/6.0, 1/6.0},
		// new double[]{1/3.0, 1/3.0, 1/3.0},
		// new double[]{1/6.0, 2/6.0, 3/6.0}};
		// log: [[-1.0986122886681096, -1.0986122886681096,
		// -1.0986122886681096], [-1.791759469228055, -0.4054651081081644,
		// -1.791759469228055], [-1.0986122886681096, -1.0986122886681096,
		// -1.0986122886681096], [-1.791759469228055, -1.0986122886681096,
		// -0.6931471805599452]]

		trueValue = 0;

		double edgeValue = 0;

		// [-0.255136485909206, -0.13626076326478176, -1.9333779623918637]
		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(2)) + 1 / 3.0
				* notProb(-Math.sqrt(2)) + 1 / 3.0 * notProb(1 - Math.sqrt(2));
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(5)) + 1 / 3.0
				* notProb(-2) + 1 / 3.0 * notProb(1 - Math.sqrt(5));
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * prob(-2) + 1 / 3.0 * prob(-Math.sqrt(2)) + 1
				/ 3.0 * prob(-2);
		trueValue += Math.log(edgeValue);

		// [-2.09318407864016, -1.514042869051154, -1.58669079862885]
		edgeValue = 1 / 6.0 * prob(-1 - Math.sqrt(5)) + 4 / 6.0 * prob(-2) + 1
				/ 6.0 * prob(1 - Math.sqrt(5));
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * prob(-1 - Math.sqrt(5)) + 4 / 6.0
				* prob(-Math.sqrt(2)) + 1 / 6.0 * prob(0);
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * prob(-3) + 4 / 6.0 * prob(-Math.sqrt(2)) + 1
				/ 6.0 * prob(1 - Math.sqrt(2));
		trueValue += Math.log(edgeValue);

		// [-1.9333779623918637, -0.29345510767698524, -0.24033142442212246]
		edgeValue = 1 / 3.0 * prob(-2) + 1 / 3.0 * prob(-Math.sqrt(2)) + 1
				/ 3.0 * prob(-2);
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-1 - Math.sqrt(5)) + 1 / 3.0 * notProb(0)
				+ 1 / 3.0 * notProb(1 - Math.sqrt(5));
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 3.0 * notProb(-3) + 1 / 3.0 * notProb(-Math.sqrt(2))
				+ 1 / 3.0 * notProb(1 - Math.sqrt(2));
		trueValue += Math.log(edgeValue);

		// [-1.933377962391864, -0.3362175853486852, -0.3175167709581459]
		edgeValue = 1 / 6.0 * prob(-2) + 2 / 6.0 * prob(-Math.sqrt(2)) + 3
				/ 6.0 * prob(-2);
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * notProb(-1 - Math.sqrt(5)) + 2 / 6.0 * notProb(0)
				+ 3 / 6.0 * notProb(1 - Math.sqrt(5));
		trueValue += Math.log(edgeValue);
		edgeValue = 1 / 6.0 * notProb(-3) + 2 / 6.0 * notProb(-Math.sqrt(2))
				+ 3 / 6.0 * notProb(1 - Math.sqrt(2));
		trueValue += Math.log(edgeValue);

		try {
			((EdgeScoreLatentSpaceMarginalizedAssignments) model
					.getAlternativeLatentSpaceSampler())
					.checkCacheConsistency();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		assertEquals(trueValue, model.logLike(), 1e-12);
	}

	// TODO: re-implement single intercept option, uncomment test

	// exchangeable joint model (harder test) after estimation procedure, single
	// intercept
	// @Test
	public void testLogProb15() {
		int T = 3;
		int K = 2;
		boolean usingWordModel = true;
		boolean usingEdgeModel = true;
		int[][] initWordAssignments = null;
		int[][] initEdgeAssignments = null;
		double[][][] initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } } };
		double[] initIntercepts = new double[] { 0.0, 0.0, 0.0 };
		double[] beta = new double[] { 5.0 };
		double[] alpha = new double[] { 6.0 };
		int numItns = 3;

		rng = new MockLogRandoms(3);

		double trueValue = 0;

		// word score
		trueValue += Math.log(2) + 3 * Math.log(24) - Math.log(5040)
				- Math.log(120) - Math.log(40320);

		// space 0 score
		trueValue += logNotProb(-1 - Math.sqrt(2)) + logProb(-1 - Math.sqrt(5))
				+ logNotProb(-1 - Math.sqrt(5));

		// space 1 score
		trueValue += logProb(-3) + 2 * logProb(-1 - Math.sqrt(2))
				+ logNotProb(-1) + logNotProb(-1 - Math.sqrt(2));

		// space 2 score
		trueValue += logNotProb(-1 - Math.sqrt(5)) + logProb(-4)
				+ logProb(-1 - Math.sqrt(2)) + logNotProb(-1 - Math.sqrt(2));

		// assignment score
		trueValue += 4 * Math.log(120) - Math.log(479001600)
				- Math.log(39916800) - Math.log(40320) - Math.log(362880) + 4
				* Math.log(2) + 4 * Math.log(6) + Math.log(24) + Math.log(720);

		JointTextNetworkModel model = new JointTextNetworkModel(emails6, T, K,
				rng, usingWordModel, usingEdgeModel, true, false, false,
				true, false, false, false, false, false, null);

		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, null, false, 0, 0,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numItns, false);

		assertEquals(trueValue, model.logProb(), 1e-12);
	}

	// exchangeable joint model (harder test) after estimation procedure
	@Test
	public void testLogProb14() {
		int T = 3;
		int K = 2;
		boolean usingWordModel = true;
		boolean usingEdgeModel = true;
		int[][] initWordAssignments = null;
		int[][] initEdgeAssignments = null;
		double[][][] initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } } };
		double[] initIntercepts = new double[] { 0.0, 0.0, 0.0 };
		double[] beta = new double[] { 5.0 };
		double[] alpha = new double[] { 6.0 };
		int numItns = 3;

		int assignmentIter = (emails6.getNumAuthors() - 1) * emails6.size();
		for (int i = 0; i < emails6.size(); i++) {
			assignmentIter += emails6.getDocument(i).getLength();
		}
		int[] discreteSamples = new int[1000];
		LogRandoms tempRng = new LogRandoms();
		int ind = -1;
		for (int iter = 0; iter < numItns - 1; iter++) {
			for (int d = 0; d < emails6.size(); d++) {
				int Nd = emails6.getEmail(d).getLength();
				for (int i = 0; i < Nd; i++) {
					ind++;
					discreteSamples[ind] = tempRng.nextInt(T);
				}
			}
			for (int d = 0; d < emails6.size(); d++) {
				for (int i = 0; i < emails6.getNumAuthors() - 1; i++) {
					ind++;
					discreteSamples[ind] = tempRng.nextInt(T);
				}
			}
		}
		int[] finalSamples = new int[] { 2, 0, 2, 2, 2, 1, 0, 0, 0, 2, 2, 1, 0,
				2, 1, 0, 2, 1, 1, 1 };
		System.arraycopy(finalSamples, 0, discreteSamples, 2 * assignmentIter,
				finalSamples.length);
		rng.setDiscrete(discreteSamples);

		double trueValue = 0;

		// word score
		trueValue += Math.log(2) + 3 * Math.log(24) - Math.log(5040)
				- Math.log(120) - Math.log(40320);

		// space 0 score
		trueValue += logNotProb(-1 - Math.sqrt(2)) + logProb(-1 - Math.sqrt(5))
				+ logNotProb(-1 - Math.sqrt(5));

		// space 1 score
		trueValue += logProb(-2) + 2 * logProb(-Math.sqrt(2)) + logNotProb(0)
				+ logNotProb(-Math.sqrt(2));

		// space 2 score
		trueValue += logNotProb(1 - Math.sqrt(5)) + logProb(-2)
				+ logProb(1 - Math.sqrt(2)) + logNotProb(1 - Math.sqrt(2));

		// assignment score
		trueValue += 4 * Math.log(120) - Math.log(479001600)
				- Math.log(39916800) - Math.log(40320) - Math.log(362880) + 4
				* Math.log(2) + 4 * Math.log(6) + Math.log(24) + Math.log(720);

		JointTextNetworkModel model = new JointTextNetworkModel(emails6, T, K,
				rng, usingWordModel, usingEdgeModel, true, false, false,
				true, false, false, false, false, false, null);

		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, null, false, 0, 0,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numItns, false);

		assertEquals(trueValue, model.logProb(), 1e-12);
	}
	
	// erosheva (harder test), Gibbs sampling distribution test
	@Test
	public void testLogProb13b() {
		int T = 3;
		int K = 2;
		boolean usingWordModel = true;
		boolean usingEdgeModel = true;
		int[][] initWordAssignments = new int[][] { new int[] { 2, 0, 2, 2 },
				new int[] { 2, 1, 0 }, new int[] {}, new int[] { 0 } };
		int[][] initEdgeAssignments = new int[][] { new int[] { 0, 2, 2 },
				new int[] { 1, 0, 2 }, new int[] { 1, 0, 2 },
				new int[] { 1, 1, 1 } };
		double[][][] initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 1.0 },
						new double[] { 1.0, 2.0 }, new double[] { 2.0, 0.0 },
						new double[] { 0.0, 0.0 } },
						new double[][] { new double[] { -1.0, -1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, -1.0 },
						new double[] { 0.0, 0.0 } },
						new double[][] { new double[] { -1.0, 1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, 0.0 },
						new double[] { 2.0, 1.0 } } };
		double[] initIntercepts = new double[] { -1, 0, 1 };
		double[] gamma = new double[] { 2.0 };
		double[] beta = new double[] { 5.0 };
		double[] alpha = new double[] { 6.0 };
		int numItns = 0;

		double[] trueValue = new double[]{
				-Math.log(2),
				-Math.log(3),
				-Math.log(2) + Math.log(3)
		};
		normalizeLogProb(trueValue);

		JointTextNetworkModel model = new JointTextNetworkModel(emails6, T, K,
				rng, usingWordModel, usingEdgeModel, false, true, true,
				true, false, false, false, false, false, null);

		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, gamma, false, 0, 0,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numItns, false);

		double[] testValue = model.getAssignmentModel().getTokenAssignmentDistribution(0, 3, 0, 2, false, 2);
		normalizeLogProb(testValue);

		for(int i = 0; i < T; i++) {
			assertEquals(trueValue[i], testValue[i], 1e-12);
		}
		
		trueValue = new double[]{
				Math.log(2),
				Math.log(1),
				Math.log(4)
		};
		normalizeLogProb(trueValue);

		model = new JointTextNetworkModel(emails6, T, K,
				rng, usingWordModel, usingEdgeModel, false, true, true,
				true, false, false, false, false, false, null);

		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, gamma, false, 0, 0,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numItns, false);

		testValue = model.getAssignmentModel().getEdgeAssignmentDistribution(0, 2, 0, 3, 1, 2, false);
		normalizeLogProb(testValue);

		for(int i = 0; i < T; i++) {
			assertEquals(trueValue[i], testValue[i], 1e-12);
		}
		
		trueValue = new double[]{
				Math.log(9) - Math.log(5),
				Math.log(2),
				Math.log(1)
		};
		normalizeLogProb(trueValue);

		model = new JointTextNetworkModel(emails6, T, K,
				rng, usingWordModel, usingEdgeModel, false, true, true,
				true, false, false, false, false, false, null);

		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, gamma, false, 0, 0,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numItns, false);

		testValue = model.getAssignmentModel().getEdgeAssignmentDistribution(3, 1, 3, 1, 0, 1, false);
		normalizeLogProb(testValue);

		for(int i = 0; i < T; i++) {
			assertEquals(trueValue[i], testValue[i], 1e-12);
		}
		
		double trueLogProb = -Math.log(4); 

		model = new JointTextNetworkModel(emails6, T, K,
				rng, usingWordModel, usingEdgeModel, false, true, true,
				true, false, false, false, false, false, null);

		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, gamma, false, 0, 0,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numItns, false);
		
		double testLogProb = model.getAssignmentModel().getMissingEdgeDistribution(0, 2, 1, 1);

		assertEquals(trueLogProb, testLogProb, 1e-12);
		
		trueLogProb = -Math.log(2); 

		model = new JointTextNetworkModel(emails6, T, K,
				rng, usingWordModel, usingEdgeModel, false, true, true,
				true, false, false, false, false, false, null);

		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, gamma, false, 0, 0,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numItns, false);
		
		testLogProb = model.getAssignmentModel().getMissingEdgeDistribution(0, 3, 1, 0);

		assertEquals(trueLogProb, testLogProb, 1e-12);
	}
	
	// lda only (harder test), Gibbs sampling distribution test
	@Test
	public void testLogProb13a() {
		int T = 3;
		int K = 2;
		boolean usingWordModel = true;
		boolean usingEdgeModel = false;
		int[][] initWordAssignments = new int[][] { new int[] { 2, 0, 2, 2 },
				new int[] { 2, 1, 0 }, new int[] {}, new int[] { 0 } };
		int[][] initEdgeAssignments = new int[][] { new int[] { 0, 2, 2 },
				new int[] { 1, 0, 2 }, new int[] { 1, 0, 2 },
				new int[] { 1, 1, 1 } };
		double[][][] initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 1.0 },
						new double[] { 1.0, 2.0 }, new double[] { 2.0, 0.0 },
						new double[] { 0.0, 0.0 } },
						new double[][] { new double[] { -1.0, -1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, -1.0 },
						new double[] { 0.0, 0.0 } },
						new double[][] { new double[] { -1.0, 1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, 0.0 },
						new double[] { 2.0, 1.0 } } };
		double[] initIntercepts = new double[] { -1, 0, 1 };
		double[] beta = new double[] { 5.0 };
		double[] alpha = new double[] { 6.0 };
		int numItns = 0;

		double[] trueValue = new double[]{
				-Math.log(8) + Math.log(3),
				-Math.log(6) + Math.log(2),
				Math.log(2) - Math.log(8) + Math.log(4)
		};
		normalizeLogProb(trueValue);

		JointTextNetworkModel model = new JointTextNetworkModel(emails6, T, K,
				rng, usingWordModel, usingEdgeModel, true, false, false,
				true, false, false, false, false, false, null);

		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, null, false, 0, 0,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numItns, false);

		double[] testValue = model.getAssignmentModel().getTokenAssignmentDistribution(0, 3, 0, 2, false, 2);
		normalizeLogProb(testValue);

		for(int i = 0; i < T; i++) {
			assertEquals(trueValue[i], testValue[i], 1e-12);
		}
	}

	// exchangeable joint model (harder test) before estimation procedure
	@Test
	public void testLogProb13() {
		int T = 3;
		int K = 2;
		boolean usingWordModel = true;
		boolean usingEdgeModel = true;
		int[][] initWordAssignments = new int[][] { new int[] { 2, 0, 2, 2 },
				new int[] { 2, 1, 0 }, new int[] {}, new int[] { 0 } };
		int[][] initEdgeAssignments = new int[][] { new int[] { 0, 2, 2 },
				new int[] { 1, 0, 2 }, new int[] { 1, 0, 2 },
				new int[] { 1, 1, 1 } };
		double[][][] initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 1.0 },
						new double[] { 1.0, 2.0 }, new double[] { 2.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { -1.0, -1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, -1.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { -1.0, 1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, 0.0 },
						new double[] { 2.0, 1.0 } } };
		double[] initIntercepts = new double[] { -1, 0, 1 };
		double[] beta = new double[] { 5.0 };
		double[] alpha = new double[] { 6.0 };
		int numItns = 0;

		double trueValue = 0;

		// word score
		trueValue += Math.log(2) + 3 * Math.log(24) - Math.log(5040)
				- Math.log(120) - Math.log(40320);

		// space 0 score
		trueValue += logNotProb(-1 - Math.sqrt(2)) + logProb(-1 - Math.sqrt(5))
				+ logNotProb(-1 - Math.sqrt(5));

		// space 1 score
		trueValue += logProb(-2) + 2 * logProb(-Math.sqrt(2)) + logNotProb(0)
				+ logNotProb(-Math.sqrt(2));

		// space 2 score
		trueValue += logNotProb(1 - Math.sqrt(5)) + logProb(-2)
				+ logProb(1 - Math.sqrt(2)) + logNotProb(1 - Math.sqrt(2));

		// space prior
		// trueValue += 12*Distributions.normalLogDensity(0.0, 0.0, 10000.0) +
		// 12*Distributions.normalLogDensity(1.0, 0.0, 10000.0) +
		// 3*Distributions.normalLogDensity(2.0, 0.0, 10000.0);

		// assignment score
		trueValue += 4 * Math.log(120) - Math.log(479001600)
				- Math.log(39916800) - Math.log(40320) - Math.log(362880) + 4
				* Math.log(2) + 4 * Math.log(6) + Math.log(24) + Math.log(720);

		JointTextNetworkModel model = new JointTextNetworkModel(emails6, T, K,
				rng, usingWordModel, usingEdgeModel, true, false, false,
				true, false, false, false, false, false, null);

		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, null, false, 0, 0,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numItns, false);

		assertEquals(trueValue, model.logProb(), 1e-12);
	}

	// exchangeable joint model (simple test) before estimation procedure
	@Test
	public void testLogProb12() {
		int T = 1;
		int K = 1;
		boolean usingWordModel = true;
		boolean usingEdgeModel = true;
		int[][] initWordAssignments = new int[][] { new int[] { 0, 0, 0, 0 } };
		int[][] initEdgeAssignments = new int[][] { new int[] { 0 } };
		double[][][] initPositions = new double[][][] { new double[][] {
				new double[] { 1 }, new double[] { 0 } } };
		double[] initIntercepts = new double[] { -1 };
		double[] beta = new double[] { 5.0 };
		double[] alpha = new double[] { 1.0 };
		int numItns = 0;

		double trueValue = Math.log(2) - Math.log(1680) - 2
				- Math.log(1 + Math.exp(-2));
		// + Distributions.normalLogDensity(0.0, 0.0, 10000.0) +
		// 2*Distributions.normalLogDensity(-1.0, 0.0, 10000.0);

		JointTextNetworkModel model = new JointTextNetworkModel(emails5, T, K,
				rng, usingWordModel, usingEdgeModel, true, false, false,
				true, false, false, false, false, false, null);

		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, null, false, 0, 0,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numItns, false);

		assertEquals(trueValue, model.logProb(), 1e-12);
	}

	// word model only (LDA), before estimation procedure
	@Test
	public void testLogProb11() {
		int T = 1;
		int K = 0;
		boolean usingWordModel = true;
		boolean usingEdgeModel = false;
		int[][] initWordAssignments = new int[][] { new int[] { 0, 0, 0, 0 } };
		int[][] initEdgeAssignments = null;
		double[][][] initPositions = null;
		double[] initIntercepts = null;
		double[] beta = new double[] { 5.0 };
		double[] alpha = new double[] { 2.0 };
		int numItns = 0;

		double trueValue = Math.log(2) - Math.log(1680);

		JointTextNetworkModel model = new JointTextNetworkModel(emails4, T, K,
				rng, usingWordModel, usingEdgeModel, true, false, false,
				true, false, false, false, false, false, null);

		model.initialize(initWordAssignments, initEdgeAssignments,
				initPositions, initIntercepts, alpha, beta, null, false, 0, 0,
				null, null, null, null, null, null, null, null, null, null,
				null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numItns, false);

		assertEquals(trueValue, model.logProb(), 1e-12);
	}

	// edge model only, before estimation procedure, simplest test
	@Test
	public void testLogProb1() {
		int T;
		int K;
		int[][] initAssignments;
		double[][][] initPositions;
		double[] initIntercepts;
		int iterations;
		double trueValue;

		T = 1;
		K = 1;
		initAssignments = new int[][] { new int[] { 0 } };
		initPositions = new double[][][] { new double[][] { new double[] { 1 },
				new double[] { 0 } } };
		initIntercepts = new double[] { -1 };
		iterations = 0;
		trueValue = -2 - Math.log(1 + Math.exp(-2));

		JointTextNetworkModel model = new JointTextNetworkModel(emails1, T, K, rng, false,
				true, true, false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 1.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		assertEquals(trueValue, model.logProb(), 1e-12);
	}

	// edge model only, before estimation procedure, one topic
	@Test
	public void testLogProb2() {
		int T;
		int K;
		int[][] initAssignments;
		double[][][] initPositions;
		double[] initIntercepts;
		int iterations;
		double trueValue;

		T = 1;
		K = 1;
		initAssignments = new int[][] { new int[] { 0, 0, 0 },
				new int[] { 0, 0, 0 } };
		initPositions = new double[][][] { new double[][] { new double[] { 0 },
				new double[] { 0 }, new double[] { 0 }, new double[] { 0 } } };
		initIntercepts = new double[] { 0 };
		iterations = 0;

		JointTextNetworkModel model = new JointTextNetworkModel(emails2, T, K, rng, false,
				true, true, false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 1.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		trueValue = 0.0;
		assertEquals(trueValue, model.getAssignmentModel().logPriorProb(),
				1e-12);
		trueValue = 6 * Math.log(0.5);
		assertEquals(trueValue, model.getAssignmentModel().logEdgeDataProb(),
				1e-12);
		trueValue = 6 * Math.log(0.5);
		assertEquals(trueValue, model.logProb(), 1e-12);
	}

	// edge model only, before estimation procedure, more topics, simple
	// positions
	@Test
	public void testLogProb3() {
		int T;
		int K;
		int[][] initAssignments;
		double[][][] initPositions;
		double[] initIntercepts;
		int iterations;
		double trueValue;

		T = 3;
		K = 2;
		initAssignments = new int[][] { new int[] { 0, 2, 2 },
				new int[] { 1, 0, 2 } };
		initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } } };
		initIntercepts = new double[] { 0.0, 0.0, 0.0 };
		iterations = 0;

		JointTextNetworkModel model = new JointTextNetworkModel(emails2, T, K, rng, false,
				true, true, false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		trueValue = 6 * Math.log(0.5);
		assertEquals(trueValue, model.getAssignmentModel().logEdgeDataProb(),
				1e-12);
		trueValue = 6 * Math.log(0.5) + Math.log(8) - 2 * Math.log(120);
		assertEquals(trueValue, model.logProb(), 1e-12);
	}

	// edge model only, before estimation procedure, more topics, different
	// positions
	@Test
	public void testLogProb4() {
		int T;
		int K;
		int[][] initAssignments;
		double[][][] initPositions;
		double[] initIntercepts;
		int iterations;
		double trueValue;

		T = 3;
		K = 2;
		initAssignments = new int[][] { new int[] { 0, 2, 2 },
				new int[] { 1, 0, 2 } };
		initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 1.0 },
						new double[] { 1.0, 2.0 }, new double[] { 2.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { -1.0, -1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, -1.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { -1.0, 1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, 0.0 },
						new double[] { 2.0, 1.0 } } };
		initIntercepts = new double[] { -1, 0, 1 };
		iterations = 0;
		trueValue = logNotProb(-1 - Math.sqrt(2))
				+ logNotProb(1 - Math.sqrt(5)) + 2 * logProb(-2)
				+ logProb(-1 - Math.sqrt(5)) + logProb(1 - Math.sqrt(2))
				+ Math.log(8) - 2 * Math.log(120);

		JointTextNetworkModel model = new JointTextNetworkModel(emails2, T, K, rng, false,
				true, true, false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		assertEquals(trueValue, model.logProb(), 1e-12);
	}

	// edge model only, after estimation procedure (more topics, different
	// positions)
	@Test
	public void testLogProb5() {
		int T;
		int K;
		int[][] initAssignments;
		double[][][] initPositions;
		double[] initIntercepts;
		int iterations;
		double trueValue;

		T = 3;
		K = 2;
		initAssignments = new int[][] { new int[] { 0, 0, 0 },
				new int[] { 0, 0, 0 } };
		initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 }, new double[] { 0.0, 0.0 },
						new double[] { 0.0, 0.0 } } };
		initIntercepts = new double[] { 0.0, 0.0, 0.0 };
		iterations = 3;
		trueValue = logNotProb(-1 - Math.sqrt(2))
				+ logNotProb(1 - Math.sqrt(5)) + 2 * logProb(-2)
				+ logProb(-1 - Math.sqrt(5)) + logProb(1 - Math.sqrt(2))
				+ Math.log(8) - 2 * Math.log(120);

		JointTextNetworkModel model = new JointTextNetworkModel(emails2, T, K, rng, false,
				true, true, false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		assertEquals(trueValue, model.logProb(), 1e-12);
	}

	// edge model only, test position MCMC difference (that logProb = SpaceScore difference)
	// before estimation procedure
	@Test
	public void testLogProb6() {
		int T;
		int K;
		int[][] initAssignments;
		double[][][] initPositions;
		double[] initIntercepts;
		int iterations;
		double trueValue;
		double value1;
		double value2;

		T = 1;
		K = 2;
		initAssignments = new int[][] { new int[] { 0, 0, 0 },
				new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 },
				new int[] { 0, 0, 0 } };
		initPositions = new double[][][] { new double[][] {
				new double[] { 0.0, 1.0 }, new double[] { 1.0, 2.0 },
				new double[] { 2.0, 0.0 }, new double[] { 0.0, -1.0 } } };
		initIntercepts = new double[] { -1 };
		iterations = 0;

		JointTextNetworkModel model = new JointTextNetworkModel(emails3, T, K, rng, false,
				true, true, false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		value1 = model.logProb();
		initPositions = new double[][][] { new double[][] {
				new double[] { -1.0, 2.0 }, new double[] { 1.0, 2.0 },
				new double[] { 2.0, 0.0 }, new double[] { 0.0, -1.0 } } };

		model = new JointTextNetworkModel(emails3, T, K, rng, false, true, true,
				false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		value2 = model.logProb();
		trueValue = value2 - value1;
		initPositions = new double[][][] { new double[][] {
				new double[] { 0.0, 1.0 }, new double[] { 1.0, 2.0 },
				new double[] { 2.0, 0.0 }, new double[] { 0.0, -1.0 } } };

		model = new JointTextNetworkModel(emails3, T, K, rng, false, true, true,
				false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		initPositions = new double[][][] { new double[][] {
				new double[] { -1.0, 2.0 }, new double[] { 1.0, 2.0 },
				new double[] { 2.0, 0.0 }, new double[] { 0.0, -1.0 } } };

		double value = ((EdgeScoreLatentSpace) model.getEdgeScore())
				.getMetropolisPositionLogRatio(0, 0, initPositions[0][0]);

		assertEquals(trueValue, value, 1e-12);
	}

	// edge model only, test intercept MCMC difference (that logProb = SpaceScore difference)
	// before estimation procedure
	@Test
	public void testLogProb7() {
		int T;
		int K;
		int[][] initAssignments;
		double[][][] initPositions;
		double[] initIntercepts;
		int iterations;
		double trueValue;
		double value1;
		double value2;

		T = 1;
		K = 2;
		initAssignments = new int[][] { new int[] { 0, 0, 0 },
				new int[] { 0, 0, 0 }, new int[] { 0, 0, 0 },
				new int[] { 0, 0, 0 } };
		initPositions = new double[][][] { new double[][] {
				new double[] { 0.0, 1.0 }, new double[] { 1.0, 2.0 },
				new double[] { 2.0, 0.0 }, new double[] { 0.0, -1.0 } } };
		initIntercepts = new double[] { -1 };
		iterations = 0;

		JointTextNetworkModel model = new JointTextNetworkModel(emails3, T, K, rng, false,
				true, true, false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 1.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		value1 = model.logProb();

		initIntercepts = new double[] { 2 };

		model = new JointTextNetworkModel(emails3, T, K, rng, false, true, true,
				false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		value2 = model.logProb();
		trueValue = value2 - value1;

		initIntercepts = new double[] { -1 };

		model = new JointTextNetworkModel(emails3, T, K, rng, false, true, true,
				false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		initIntercepts = new double[] { 2 };

		double value = ((EdgeScoreLatentSpace) model.getEdgeScore())
				.getMetropolisInterceptLogRatio(0, initIntercepts[0]);

		assertEquals(trueValue, value, 1e-12);
	}

	// edge model only, test position log prob before estimation procedure
	@Test
	public void testPositionLogProb1() {
		int T;
		int K;
		int[][] initAssignments;
		double[][][] initPositions;
		double[] initIntercepts;
		int iterations;
		double trueValue;

		T = 3;
		K = 2;
		initAssignments = new int[][] { new int[] { 0, 2, 2 },
				new int[] { 1, 0, 2 } };
		initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 1.0 },
						new double[] { 1.0, 2.0 }, new double[] { 2.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { -1.0, -1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, -1.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { -1.0, 1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, 0.0 },
						new double[] { 0.0, 0.0 } } };
		initIntercepts = new double[] { -1, 0, 1 };
		iterations = 0;

		JointTextNetworkModel model = new JointTextNetworkModel(emails2, T, K, rng, false,
				true, true, false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		trueValue = 1 - Math.sqrt(2) - Math.log(1 + Math.exp(1 - Math.sqrt(2)))
				- Math.log(2);
		assertEquals(
				trueValue,
				((EdgeScoreLatentSpace) model.getEdgeScore()).getPositionLogProb(2, 3),
				1e-12);
	}

	// edge model only, test position log prob after estimation procedure
	@Test
	public void testPositionLogProb2() {
		int T;
		int K;
		int[][] initAssignments;
		double[][][] initPositions;
		double[] initIntercepts;
		int iterations;
		double trueValue;

		T = 3;
		K = 2;
		initAssignments = new int[][] { new int[] { 0, 2, 2 },
				new int[] { 1, 0, 2 } };
		initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 1.0 },
						new double[] { 1.0, 2.0 }, new double[] { 2.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { -1.0, -1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, -1.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { -1.0, 1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, 0.0 },
						new double[] { 0.0, 0.0 } } };
		initIntercepts = new double[] { -1, 0, 1 };
		iterations = 3;

		JointTextNetworkModel model = new JointTextNetworkModel(emails2, T, K, rng, false,
				true, true, false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		trueValue = -2 - Math.log(1 + Math.exp(-2)) + 1 - Math.sqrt(2)
				- Math.log(1 + Math.exp(1 - Math.sqrt(2)));

		assertEquals(
				trueValue,
				((EdgeScoreLatentSpace) model.getEdgeScore()).getPositionLogProb(2, 3),
				1e-12);
	}

	// edge model only, test MCMC difference (using logProb) after estimation procedure
	@Test
	public void testLogProb9() {
		int T;
		int K;
		int[][] initAssignments;
		double[][][] initPositions;
		double[] initIntercepts;
		int iterations;
		double trueValue;
		double value1;
		double value2;

		T = 3;
		K = 2;
		initAssignments = new int[][] { new int[] { 0, 2, 2 },
				new int[] { 1, 0, 2 } };
		initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 1.0 },
						new double[] { 1.0, 2.0 }, new double[] { 2.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { -1.0, -1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, -1.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { -1.0, 1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, 0.0 },
						new double[] { 0.0, 0.0 } } };
		initIntercepts = new double[] { -1, 0, 1 };
		iterations = 0;

		JointTextNetworkModel model = new JointTextNetworkModel(emails2, T, K, rng, false,
				true, true, false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		value1 = model.logProb();
		iterations = 3;

		model = new JointTextNetworkModel(emails2, T, K, rng, false, true, true,
				false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		value2 = model.logProb();
		trueValue = -Math.log(2) + 2 + Math.log(1 + Math.exp(-2));
		assertEquals(trueValue, value1 - value2, 1e-12);
	}

	// edge model only, test MCMC difference using internal methods after estimation procedure
	@Test
	public void testLogProb10() {
		int T;
		int K;
		int[][] initAssignments;
		double[][][] initPositions;
		double[] initIntercepts;
		int iterations;
		double trueValue;
		double value1;
		double value2;

		T = 3;
		K = 2;
		initAssignments = new int[][] { new int[] { 0, 2, 2 },
				new int[] { 1, 0, 2 } };
		initPositions = new double[][][] {
				new double[][] { new double[] { 0.0, 1.0 },
						new double[] { 1.0, 2.0 }, new double[] { 2.0, 0.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { -1.0, -1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, -1.0 },
						new double[] { 0.0, 0.0 } },
				new double[][] { new double[] { -1.0, 1.0 },
						new double[] { 0.0, 0.0 }, new double[] { 1.0, 0.0 },
						new double[] { 0.0, 0.0 } } };
		initIntercepts = new double[] { -1, 0, 1 };
		iterations = 0;

		JointTextNetworkModel model = new JointTextNetworkModel(emails2, T, K, rng, false,
				true, true, false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		value1 = ((EdgeScoreLatentSpace) model.getEdgeScore())
				.getPositionLogProb(2, 3);

		iterations = 3;

		model = new JointTextNetworkModel(emails2, T, K, rng, false, true, true,
				false, false, true, false, false, false, false, false, null);

		model.initialize(null, initAssignments, initPositions, initIntercepts,
				new double[] { 3.0 }, null, null, false, 0, 0, null, null,
				null, null, null, null, null, null, null, null, null, null,
				null, 0, null, null, null, null, null, null, null, null);
		model.estimate(iterations, false);

		value2 = ((EdgeScoreLatentSpace) model.getEdgeScore())
				.getPositionLogProb(2, 3);

		trueValue = -Math.log(2) + 2 + Math.log(1 + Math.exp(-2));

		assertEquals(trueValue, value1 - value2, 1e-12);
	}

}
