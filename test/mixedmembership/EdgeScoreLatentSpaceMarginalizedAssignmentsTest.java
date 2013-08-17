package mixedmembership;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import data.Email;
import data.EmailCorpus;

public class EdgeScoreLatentSpaceMarginalizedAssignmentsTest {

	EmailCorpus emails;
	MockLogRandoms rng;

	@Before
	public void setUp() throws Exception {

		emails = new EmailCorpus(4);
		emails.add(new Email(0, new int[]{0, 0, 0, 1}, null));
		emails.add(new Email(2, new int[]{1, 1, 0, 1}, null));
		emails.add(new Email(3, new int[]{1, 0, 0, 0}, null));
		emails.add(new Email(3, new int[]{1, 0, 0, 0}, null));

		rng = new MockLogRandoms();
	}

	private static double prob(double arg) {
		return Math.exp(logProb(arg));
	}

	private static double notProb(double arg) {
		return Math.exp(logNotProb(arg));
	}

	private static double logProb(double arg) {
		return arg - Math.log(1 + Math.exp(arg));
	}

	private static double logNotProb(double arg) {
		return -Math.log(1 + Math.exp(arg));
	}
	
	// after doing some sampling
	@Test
	public void testLogEdgeDataProb5() {
		int T;
		int K;
		double[][] featureProportions;
		double[][][] initPositions;
		double[] initIntercepts;
		double[] trueValue;

		T = 3;
		K = 2;
		featureProportions = new double[][]{
				new double[]{1, 0, 0}, 
				new double[]{0.25, 0.25, 0.5}, 
				new double[]{0.5, 0.25, 0.25},
				new double[]{0, 0.5, 0.5}};
		initPositions = new double[][][]{
				new double[][]{
						new double[]{0.0, 0.0}, 
						new double[]{0.0, 0.0}, 
						new double[]{2.0, 0.0}, 
						new double[]{0.0, 0.0}},
						new double[][]{
						new double[]{-1.0, -1.0}, 
						new double[]{0.0, 0.0}, 
						new double[]{1.0, -1.0}, 
						new double[]{0.0, 0.0}}, 
						new double[][]{
						new double[]{0.0, 0.0}, 
						new double[]{0.0, 0.0}, 
						new double[]{1.0, 0.0}, 
						new double[]{2.0, 1.0}}};

		initIntercepts = new double[]{-1, 0, 1};

//		initPositions = new double[][][]{
//				new double[][]{
//						new double[]{0.0, 1.0}, 
//						new double[]{1.0, 2.0}, 
//						new double[]{2.0, 0.0}, 
//						new double[]{0.0, 0.0}},
//						new double[][]{
//						new double[]{-1.0, -1.0}, 
//						new double[]{0.0, 0.0}, 
//						new double[]{1.0, -1.0}, 
//						new double[]{0.0, 0.0}}, 
//						new double[][]{
//						new double[]{-1.0, 1.0}, 
//						new double[]{0.0, 0.0}, 
//						new double[]{1.0, 0.0}, 
//						new double[]{2.0, 1.0}}};

		double in = Double.POSITIVE_INFINITY;
		rng.setDouble(new double[]{0, 0, in, 0, in, 0, in, in});
		rng.setGaussian(new double[]{
				-1, 1,
				8, 8,
				8, 8,
				1, 2,
				8, 8,
				0, 1,
				8, 8,
				8, 8});
		
		for(int i = 0; i < 4; i++) {
			for(int j = 0; j < T; j++) {
				featureProportions[i][j] = Math.log(featureProportions[i][j]);
			}
		}
		
		EdgeScoreLatentSpaceMarginalizedAssignments model = new EdgeScoreLatentSpaceMarginalizedAssignments(
				T, K, emails, initPositions, initIntercepts, false, false, rng);
		model.setFeatureProportions(featureProportions);

		model.samplePosition(2, 0); // accept
		model.samplePosition(0, 1); // accept
		model.samplePosition(0, 2); // reject
		model.samplePosition(0, 1); // accept
		model.samplePosition(0, 0); // reject
		model.samplePosition(0, 0); // accept
		model.samplePosition(0, 1); // reject
		model.samplePosition(0, 1); // reject
		
		trueValue = new double[4];
		trueValue[0] += logNotProb(-1 - Math.sqrt(2)); 
		trueValue[1] += logNotProb(-1 - Math.sqrt(2)); 
		trueValue[0] += logNotProb(-1 - Math.sqrt(5));
		trueValue[2] += logNotProb(-1 - Math.sqrt(5));
		trueValue[0] += logProb(-2);
		trueValue[3] += logProb(-2);

		trueValue[2] += Math.log(.25*prob(-1 - Math.sqrt(5)) + .25*prob(-2) + .5*prob(1 - Math.sqrt(5)));
		trueValue[0] += Math.log(.25*prob(-1 - Math.sqrt(5)) + .25*prob(-2) + .5*prob(1 - Math.sqrt(5)));
		trueValue[2] += Math.log(.25*prob(-1 - Math.sqrt(5)) + .25*prob(-Math.sqrt(2)) + .5*prob(0));
		trueValue[1] += Math.log(.25*prob(-1 - Math.sqrt(5)) + .25*prob(-Math.sqrt(2)) + .5*prob(0));
		trueValue[2] += Math.log(.25*prob(-3) + .25*prob(-Math.sqrt(2)) + 0.5*prob(1 - Math.sqrt(2)));
		trueValue[3] += Math.log(.25*prob(-3) + .25*prob(-Math.sqrt(2)) + 0.5*prob(1 - Math.sqrt(2)));

		trueValue[3] += Math.log(.5*prob(-2) + .25*prob(-Math.sqrt(2)) + .25*prob(-2));
		trueValue[0] += Math.log(.5*prob(-2) + .25*prob(-Math.sqrt(2)) + .25*prob(-2));
		trueValue[3] += Math.log(.5*notProb(-1 - Math.sqrt(5)) + .25*notProb(0) + .25*notProb(1 - Math.sqrt(5)));
		trueValue[1] += Math.log(.5*notProb(-1 - Math.sqrt(5)) + .25*notProb(0) + .25*notProb(1 - Math.sqrt(5)));
		trueValue[3] += Math.log(.5*notProb(-3) + .25*notProb(-Math.sqrt(2)) + .25*notProb(1 - Math.sqrt(2)));
		trueValue[2] += Math.log(.5*notProb(-3) + .25*notProb(-Math.sqrt(2)) + .25*notProb(1 - Math.sqrt(2)));

		trueValue[3] += Math.log(.5*prob(-Math.sqrt(2)) + .5*prob(-2));
		trueValue[0] += Math.log(.5*prob(-Math.sqrt(2)) + .5*prob(-2));
		trueValue[3] += Math.log(.5*notProb(0) + .5*notProb(1 - Math.sqrt(5)));
		trueValue[1] += Math.log(.5*notProb(0) + .5*notProb(1 - Math.sqrt(5)));
		trueValue[3] += Math.log(.5*notProb(-Math.sqrt(2)) + .5*notProb(1 - Math.sqrt(2)));
		trueValue[2] += Math.log(.5*notProb(-Math.sqrt(2)) + .5*notProb(1 - Math.sqrt(2)));
		
		for(int i = 0; i < 4; i++) {
			assertEquals(trueValue[i], model.positionLogLikes[i], 1e-12);
		}
	}

	// test position likelihoods after sampling all values
	@Test
	public void testLogEdgeDataProb4() {
		int T;
		int K;
		double[][] featureProportions;
		double[][][] initPositions;
		double[] initIntercepts;
		double trueValue;

		T = 3;
		K = 2;
		featureProportions = new double[][]{
				new double[]{1, 0, 0}, 
				new double[]{0.25, 0.25, 0.5}, 
				new double[]{0.5, 0.25, 0.25},
				new double[]{0, 0.5, 0.5}};
		initPositions = new double[][][]{
				new double[][]{new double[]{0.0, 0.0}, new double[]{0.0, 0.0}, new double[]{0.0, 0.0}, new double[]{0.0, 0.0}},
				new double[][]{new double[]{0.0, 0.0}, new double[]{0.0, 0.0}, new double[]{0.0, 0.0}, new double[]{0.0, 0.0}},
				new double[][]{new double[]{0.0, 0.0}, new double[]{0.0, 0.0}, new double[]{0.0, 0.0}, new double[]{0.0, 0.0}}};
		initIntercepts = new double[]{0.0, 0.0, 0.0};

		for(int i = 0; i < 4; i++) {
			for(int j = 0; j < T; j++) {
				featureProportions[i][j] = Math.log(featureProportions[i][j]);
			}
		}
		
		EdgeScoreLatentSpaceMarginalizedAssignments model = new EdgeScoreLatentSpaceMarginalizedAssignments(
				T, K, emails, initPositions, initIntercepts, false, false, rng);
		model.setFeatureProportions(featureProportions);
		
		for(int i = 0; i < 3; i++) {
			for(int f = 0; f < 3; f++) {
				for(int a = 0; a < 4; a++) {
					model.samplePosition(f, a);
				}
			}
			for(int f = 0; f < 3; f++) {
				model.sampleIntercept(f);
			}
		}

		trueValue = 0;
		
		trueValue += logNotProb(-1 - Math.sqrt(2)) + logNotProb(-1 - Math.sqrt(5)) + logProb(-2);

		trueValue += Math.log(.25*prob(-1 - Math.sqrt(5)) + .25*prob(-2) + .5*prob(1 - Math.sqrt(5)));
		
		trueValue += Math.log(.5*prob(-2) + .25*prob(-Math.sqrt(2)) + .25*prob(-2));
		
		trueValue += Math.log(.5*prob(-Math.sqrt(2)) + .5*prob(-2));
		
		assertEquals(trueValue, model.positionLogLikes[0], 1e-12);
	}


	// after sampling all values
	@Test
	public void testLogEdgeDataProb3() {
		int T;
		int K;
		double[][] featureProportions;
		double[][][] initPositions;
		double[] initIntercepts;
		double trueValue;

		T = 3;
		K = 2;
		featureProportions = new double[][]{
				new double[]{1, 0, 0}, 
				new double[]{0.25, 0.25, 0.5}, 
				new double[]{0.5, 0.25, 0.25},
				new double[]{0, 0.5, 0.5}};
		initPositions = new double[][][]{
				new double[][]{new double[]{0.0, 0.0}, new double[]{0.0, 0.0}, new double[]{0.0, 0.0}, new double[]{0.0, 0.0}},
				new double[][]{new double[]{0.0, 0.0}, new double[]{0.0, 0.0}, new double[]{0.0, 0.0}, new double[]{0.0, 0.0}},
				new double[][]{new double[]{0.0, 0.0}, new double[]{0.0, 0.0}, new double[]{0.0, 0.0}, new double[]{0.0, 0.0}}};
		initIntercepts = new double[]{0.0, 0.0, 0.0};
		
		for(int i = 0; i < 4; i++) {
			for(int j = 0; j < T; j++) {
				featureProportions[i][j] = Math.log(featureProportions[i][j]);
			}
		}

		EdgeScoreLatentSpaceMarginalizedAssignments model = new EdgeScoreLatentSpaceMarginalizedAssignments(
				T, K, emails, initPositions, initIntercepts, false, false, rng);
		model.setFeatureProportions(featureProportions);
		
		for(int i = 0; i < 3; i++) {
			for(int f = 0; f < 3; f++) {
				for(int a = 0; a < 4; a++) {
					model.samplePosition(f, a);
				}
			}
			for(int f = 0; f < 3; f++) {
				model.sampleIntercept(f);
			}
		}

		trueValue = logNotProb(-1 - Math.sqrt(2)) + logNotProb(-1 - Math.sqrt(5)) + logProb(-2);

		trueValue += Math.log(.25*prob(-1 - Math.sqrt(5)) + .25*prob(-2) + .5*prob(1 - Math.sqrt(5)));
		trueValue += Math.log(.25*prob(-1 - Math.sqrt(5)) + .25*prob(-Math.sqrt(2)) + .5*prob(0));
		trueValue += Math.log(.25*prob(-3) + .25*prob(-Math.sqrt(2)) + 0.5*prob(1 - Math.sqrt(2)));

		trueValue += Math.log(.5*prob(-2) + .25*prob(-Math.sqrt(2)) + .25*prob(-2));
		trueValue += Math.log(.5*notProb(-1 - Math.sqrt(5)) + .25*notProb(0) + .25*notProb(1 - Math.sqrt(5)));
		trueValue += Math.log(.5*notProb(-3) + .25*notProb(-Math.sqrt(2)) + .25*notProb(1 - Math.sqrt(2)));

		trueValue += Math.log(.5*prob(-Math.sqrt(2)) + .5*prob(-2));
		trueValue += Math.log(.5*notProb(0) + .5*notProb(1 - Math.sqrt(5)));
		trueValue += Math.log(.5*notProb(-Math.sqrt(2)) + .5*notProb(1 - Math.sqrt(2)));

		assertEquals(trueValue, model.logEdgeDataProb(), 1e-12);
	}

	// after doing some sampling
	@Test
	public void testLogEdgeDataProb2() {
		int T;
		int K;
		double[][] featureProportions;
		double[][][] initPositions;
		double[] initIntercepts;
		double trueValue;

		T = 3;
		K = 2;
		featureProportions = new double[][]{
				new double[]{1, 0, 0}, 
				new double[]{0.25, 0.25, 0.5}, 
				new double[]{0.5, 0.25, 0.25},
				new double[]{0, 0.5, 0.5}};
		initPositions = new double[][][]{
				new double[][]{
						new double[]{0.0, 0.0}, 
						new double[]{0.0, 0.0}, 
						new double[]{2.0, 0.0}, 
						new double[]{0.0, 0.0}},
						new double[][]{
						new double[]{-1.0, -1.0}, 
						new double[]{0.0, 0.0}, 
						new double[]{1.0, -1.0}, 
						new double[]{0.0, 0.0}}, 
						new double[][]{
						new double[]{0.0, 0.0}, 
						new double[]{0.0, 0.0}, 
						new double[]{1.0, 0.0}, 
						new double[]{2.0, 1.0}}};

		initIntercepts = new double[]{0, 0, 1};
		
		for(int i = 0; i < 4; i++) {
			for(int j = 0; j < T; j++) {
				featureProportions[i][j] = Math.log(featureProportions[i][j]);
			}
		}

		emails.getEmail(0).obscureEdge(1);
		emails.getEmail(3).obscureEdge(2);

		EdgeScoreLatentSpaceMarginalizedAssignments model = new EdgeScoreLatentSpaceMarginalizedAssignments(
				T, K, emails, initPositions, initIntercepts, false, false, rng);
		model.setFeatureProportions(featureProportions);
		
		model.sampleIntercept(0); // accept
		model.sampleMissingValue(3, 2); // correct
		model.samplePosition(2, 0); // accept
		model.samplePosition(0, 1); // accept
		model.samplePosition(0, 1); // accept
		model.samplePosition(0, 0); // accept
		model.sampleIntercept(2); // accept
		model.sampleIntercept(2); // reject
		model.sampleIntercept(2); // reject
		model.samplePosition(0, 1); // reject
		model.samplePosition(0, 1); // reject
		model.sampleIntercept(2); //accept
		model.sampleMissingValue(0, 0); // correct

		trueValue = logNotProb(-1 - Math.sqrt(2)) + logNotProb(-1 - Math.sqrt(5)) + logProb(-2);

		trueValue += Math.log(.25*prob(-1 - Math.sqrt(5)) + .25*prob(-2) + .5*prob(1 - Math.sqrt(5)));
		trueValue += Math.log(.25*prob(-1 - Math.sqrt(5)) + .25*prob(-Math.sqrt(2)) + .5*prob(0));
		trueValue += Math.log(.25*prob(-3) + .25*prob(-Math.sqrt(2)) + 0.5*prob(1 - Math.sqrt(2)));

		trueValue += Math.log(.5*prob(-2) + .25*prob(-Math.sqrt(2)) + .25*prob(-2));
		trueValue += Math.log(.5*notProb(-1 - Math.sqrt(5)) + .25*notProb(0) + .25*notProb(1 - Math.sqrt(5)));
		trueValue += Math.log(.5*notProb(-3) + .25*notProb(-Math.sqrt(2)) + .25*notProb(1 - Math.sqrt(2)));

		trueValue += Math.log(.5*prob(-Math.sqrt(2)) + .5*prob(-2));
		trueValue += Math.log(.5*notProb(0) + .5*notProb(1 - Math.sqrt(5)));
		trueValue += Math.log(.5*notProb(-Math.sqrt(2)) + .5*notProb(1 - Math.sqrt(2)));

		assertEquals(trueValue, model.logEdgeDataProb(), 1e-12);
	}

	// before any sampling
	@Test
	public void testLogEdgeDataProb() {
		int T;
		int K;
		double[][] featureProportions;
		double[][][] initPositions;
		double[] initIntercepts;
		double trueValue;

		T = 3;
		K = 2;
		featureProportions = new double[][]{
				new double[]{1, 0, 0}, 
				new double[]{0.25, 0.25, 0.5}, 
				new double[]{0.5, 0.25, 0.25},
				new double[]{0, 0.5, 0.5}};
		initPositions = new double[][][]{
				new double[][]{
						new double[]{0.0, 1.0}, 
						new double[]{1.0, 2.0}, 
						new double[]{2.0, 0.0}, 
						new double[]{0.0, 0.0}},
						new double[][]{
						new double[]{-1.0, -1.0}, 
						new double[]{0.0, 0.0}, 
						new double[]{1.0, -1.0}, 
						new double[]{0.0, 0.0}}, 
						new double[][]{
						new double[]{-1.0, 1.0}, 
						new double[]{0.0, 0.0}, 
						new double[]{1.0, 0.0}, 
						new double[]{2.0, 1.0}}};

		initIntercepts = new double[]{-1, 0, 1};

		for(int i = 0; i < 4; i++) {
			for(int j = 0; j < T; j++) {
				featureProportions[i][j] = Math.log(featureProportions[i][j]);
			}
		}
		EdgeScoreLatentSpaceMarginalizedAssignments model = new EdgeScoreLatentSpaceMarginalizedAssignments(
						T, K, emails, initPositions, initIntercepts, false, false, rng);
		model.setFeatureProportions(featureProportions);
		
		// doc 1
		trueValue = logNotProb(-1 - Math.sqrt(2)) + logNotProb(-1 - Math.sqrt(5)) + logProb(-2);

		// doc 2
		trueValue += Math.log(.25*prob(-1 - Math.sqrt(5)) + .25*prob(-2) + .5*prob(1 - Math.sqrt(5)));
		trueValue += Math.log(.25*prob(-1 - Math.sqrt(5)) + .25*prob(-Math.sqrt(2)) + .5*prob(0));
		trueValue += Math.log(.25*prob(-3) + .25*prob(-Math.sqrt(2)) + 0.5*prob(1 - Math.sqrt(2)));

		// doc 3
		trueValue += Math.log(.5*prob(-2) + .25*prob(-Math.sqrt(2)) + .25*prob(-2));
		trueValue += Math.log(.5*notProb(-1 - Math.sqrt(5)) + .25*notProb(0) + .25*notProb(1 - Math.sqrt(5)));
		trueValue += Math.log(.5*notProb(-3) + .25*notProb(-Math.sqrt(2)) + .25*notProb(1 - Math.sqrt(2)));

		// doc 4
		trueValue += Math.log(.5*prob(-Math.sqrt(2)) + .5*prob(-2));
		trueValue += Math.log(.5*notProb(0) + .5*notProb(1 - Math.sqrt(5)));
		trueValue += Math.log(.5*notProb(-Math.sqrt(2)) + .5*notProb(1 - Math.sqrt(2)));

		assertEquals(trueValue, model.logEdgeDataProb(), 1e-12);
	}

}
