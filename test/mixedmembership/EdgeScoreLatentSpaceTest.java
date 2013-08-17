package mixedmembership;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import cc.mallet.types.Alphabet;
import data.Email;
import data.EmailCorpus;

public class EdgeScoreLatentSpaceTest {

	EmailCorpus emails;
	MockLogRandoms rng;

	@Before
	public void setUp() throws Exception {

		Alphabet wordDict = new Alphabet();
		wordDict.lookupIndex("a"); 
		wordDict.lookupIndex("b");
		wordDict.lookupIndex("c");
		wordDict.lookupIndex("d");
		wordDict.lookupIndex("e");

		emails = new EmailCorpus(4, wordDict);
		emails.add(new Email(0, new int[]{0, 4, 2, 2}, new int[]{0, 0, 0, 1}, null));
		emails.add(new Email(2, new int[]{1, 1, 1}, new int[]{1, 1, 0, 1}, null));
		emails.add(new Email(3, new int[]{}, new int[]{1, 0, 0, 0}, null));
		emails.add(new Email(3, new int[]{0}, new int[]{1, 0, 0, 0}, null));

		rng = new MockLogRandoms();
	}
	
	// test Metropolis Hastings ratio with conditional model before estimation
	@Test
	public void test1() {
		int T = 3;
		int[][] initWordAssignments = new int[][]{new int[]{2, 0, 2, 1}, new int[]{2, 1, 0}, new int[]{1}, new int[]{1}};
		int[][] initEdgeAssignments = new int[][]{new int[]{1, 3, 3}, new int[]{1, 2, 1}, new int[]{0, 0, 0}, new int[]{0, 0, 0}};
		double[][][] initPositions = new double[][][]{
				new double[][]{new double[]{0.0, 1.0}, new double[]{1.0, 2.0}, new double[]{2.0, 0.0}, new double[]{0.0, 0.0}},
				new double[][]{new double[]{-1.0, -1.0}, new double[]{0.0, 0.0}, new double[]{1.0, -1.0}, new double[]{0.0, 0.0}}, 
				new double[][]{new double[]{-1.0, 1.0}, new double[]{0.0, 0.0}, new double[]{1.0, 0.0}, new double[]{2.0, 1.0}}};
		double[] initIntercepts = new double[]{1, 1, 1};
		double[] beta = new double[]{5.0};
		double[] alpha = new double[]{6.0};
		int numIter = 0;

		JointTextNetworkModel model = 
				new JointTextNetworkModel(emails, T, 2, rng, true, true, false, false, false, false, false, false, false, false, false, null);
		model.
		initialize(initWordAssignments, initEdgeAssignments, initPositions, initIntercepts, alpha, beta, null, false, 0,
				0, null, null, null, null, null, null, null, null, null, null, null, null, null, 0, null, null, null, null, null, null, null, null);
		model.estimate(numIter, false);
		

		EdgeScoreLatentSpace edgeModel = (EdgeScoreLatentSpace) model.getEdgeScore();
		for (int iter = 0; iter < 5; iter++) {
			for (int t = 0; t < T; t++) {
				for (int a = 0; a < emails.getNumAuthors(); a++) {
					double[] sample = new double[2];
					for (int k = 0; k < 2; k++) {
						sample[k] = rng.nextGaussian(0, 10);
					}
					double ratio = edgeModel.getMetropolisPositionLogRatio(t, a, sample);
					double logProb = model.logProb(model.logLike());
					edgeModel.samplePosition(t, a, -1, true, sample);
					
					assertEquals(ratio, model.logProb(model.logLike()) - logProb, 1e-12);
				}

				double sample = rng.nextGaussian(0, 10);
				double ratio = edgeModel.getMetropolisInterceptLogRatio(t, sample);
				double logProb = model.logProb(model.logLike());
				edgeModel.sampleIntercept(t, -1, true, sample);
				
				assertEquals(ratio, model.logProb(model.logLike()) - logProb, 1e-12);
			}
		}
	}
}
