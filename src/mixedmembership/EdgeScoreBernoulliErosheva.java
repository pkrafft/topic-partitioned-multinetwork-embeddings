package mixedmembership;

import java.util.Arrays;

import data.Email;

/**
 * A class that implements the Bernoulli edge model of Erosheva et al. (2004)
 * 
 * In this model each mixture/admixture component is an A x 1 vector of
 * Bernoulli probabilities. Each component represents the probability of
 * choosing each recipient according to that component. In this model the author
 * of an email does not change the probability of that email having any
 * particular recipient.
 */
public class EdgeScoreBernoulliErosheva extends EdgeScoreBernoulli {

	// tracks how many times each present or absent recipient (r,y) has been
	// assigned to each component
	private int[][][] edgeCounts;

	@Override
	public void initialize(int numFeatures, double[] gamma, JointStructure modelStructure) {

		super.initialize(numFeatures, gamma, modelStructure);

		edgeCounts = new int[numFeatures][numActors][2];
	}

	@Override
	public double getLogProb(int t, int a, int r, int y) {

		int numPos = edgeCounts[t][r][1];
		int numAbs = edgeCounts[t][r][0];

		if (y == 1) {
			return Math.log(numPos + 0.5 * gamma[0])
					- Math.log(numPos + numAbs + gamma[0]);
		} else {
			assert y == 0;
			return Math.log(numAbs + 0.5 * gamma[0])
					- Math.log(numPos + numAbs + gamma[0]);
		}
	}

	@Override
	public void incrementCounts(int t, int a, int r, int y) {

		if (y == 1) {
			edgeCounts[t][r][1]++;
		} else {
			assert y == 0;
			edgeCounts[t][r][0]++;
		}
	}

	@Override
	public void decrementCounts(int t, int a, int r, int y) {

		if (y == 1) {
			edgeCounts[t][r][1]--;
		} else {
			assert y == 0;
			edgeCounts[t][r][0]--;
		}

		assert edgeCounts[t][r][0] >= 0;
		assert edgeCounts[t][r][1] >= 0;
	}

	@Override
	public void resetCounts() {

		for (int t = 0; t < numFeatures; t++) {
			for (int i = 0; i < numActors; i++) {
				Arrays.fill(edgeCounts[t][i], 0);
				Arrays.fill(edgeCounts[t][i], 0);
			}
		}
	}

	@Override
	public boolean checkCacheConsistency(JointStructure assignmentModel)
			throws Exception {

		// calculate edge counts directly
		int[][][] correctConnections = new int[numFeatures][numActors][2];
		for (int d = 0; d < numDocs; d++) {
			Email email = emails.getDocument(d);
			for (int j = 0; j < numActors - 1; j++) {
				int r = email.getRecipient(j);
				int t = assignmentModel.getEdgeTopicAssignment(d, j);
				int y = email.getEdge(r);

				correctConnections[t][r][y]++;
			}
		}

		// check the caches
		for (int t = 0; t < numFeatures; t++) {
			for (int r = 0; r < numActors; r++) {
				for (int y = 0; y < 2; y++) {
					if (correctConnections[t][r][y] != edgeCounts[t][r][y]) {
						throw new Exception();
					}
				}
			}
		}

		return true;
	}

}
