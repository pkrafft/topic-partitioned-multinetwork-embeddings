package mixedmembership;

import java.util.Arrays;

import data.Email;

/**
 * A class that implements the asymmetric version of the Bernoulli edge model
 * discussed in Krafft et al. (2012).
 * 
 * The components of this edge model are each A x A matrices of Bernoulli
 * probabilities giving the probabilities of each pair of actors communicating
 * according to each component. The author of each email affects the
 * probabilities of recipients in that email by selecting the row of each
 * component to use.
 */
public class EdgeScoreBernoulliAsymmetric extends EdgeScoreBernoulli {

	// tracks how many times has each edge (a,r,y) been assigned to each
	// component
	private int[][][][] edgeCounts;

	@Override
	public void initialize(int numFeatures, double[] gamma,
			JointStructure modelStructure) {

		super.initialize(numFeatures, gamma, modelStructure);

		edgeCounts = new int[numFeatures][numActors][numActors][2];
	}

	@Override
	public double getLogProb(int t, int a, int r, int y) {

		int numPos = edgeCounts[t][a][r][1];
		int numAbs = edgeCounts[t][a][r][0];

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
			edgeCounts[t][a][r][1]++;
		} else {
			assert y == 0;
			edgeCounts[t][a][r][0]++;
		}
	}

	@Override
	public void decrementCounts(int t, int a, int r, int y) {
		if (y == 1) {
			edgeCounts[t][a][r][1]--;
		} else {
			assert y == 0;
			edgeCounts[t][a][r][0]--;
		}

		assert edgeCounts[t][a][r][0] >= 0;
		assert edgeCounts[t][a][r][1] >= 0;
	}

	@Override
	public void resetCounts() {

		for (int t = 0; t < numFeatures; t++) {
			for (int i = 0; i < numActors; i++) {
				for (int j = 0; j < numActors; j++) {
					if (i != j) {
						Arrays.fill(edgeCounts[t][i][j], 0);
						Arrays.fill(edgeCounts[t][i][j], 0);
					}
				}
			}
		}
	}

	@Override
	public boolean checkCacheConsistency(JointStructure assignmentModel)
			throws Exception {

		int[][][][] correctConnections = new int[numFeatures][numActors][numActors][2];
		for (int d = 0; d < numDocs; d++) {
			Email email = emails.getDocument(d);
			int a = email.getAuthor();
			for (int j = 0; j < numActors - 1; j++) {
				int r = email.getRecipient(j);
				int t = assignmentModel.getEdgeTopicAssignment(d, j);
				int y = email.getEdge(r);

				correctConnections[t][a][r][y]++;
			}
		}

		for (int t = 0; t < numFeatures; t++) {
			for (int a = 0; a < numActors; a++) {
				for (int r = 0; r < numActors; r++) {
					if (a != r) {
						for (int y = 0; y < 2; y++) {
							if (correctConnections[t][a][r][y] != edgeCounts[t][a][r][y]) {
								throw new Exception();
							}
						}
					}
				}
			}
		}

		return true;
	}
}
