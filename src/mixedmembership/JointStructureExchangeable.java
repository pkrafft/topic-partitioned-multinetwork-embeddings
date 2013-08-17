package mixedmembership;


/**
 * This joint structure uses an admixture model and treats the generative
 * process for the words as exchangeable with the generative process for the
 * recipients.
 * 
 * In this joint structure the prior distribution on the edge assignments is the
 * same as the prior distribution on the word assignments, and the recipients
 * are conditionally independent of the words given the topic proportions of
 * each email.
 * 
 * This joint structure is also the one used by the word-only (LDA) and
 * edge-only (MMLSM) models, so it is kind of the default structure.
 */
public class JointStructureExchangeable extends JointStructure {

	@Override
	protected int getEdgeTopicAssignment(int d, int j) {
		return edgeAssignments[d][j];
	}

	@Override
	protected void unassignEdge(int d, int j, int a, int r, int y, int t) {
		assignmentScore.decrementCounts(d, t);
		super.unassignEdge(d, j, a, r, y, t);
	}

	@Override
	protected void assignEdge(int d, int j, int a, int r, int y, int t) {
		edgeAssignments[d][j] = t;
		assignmentScore.incrementCounts(d, t);
		super.assignEdge(d, j, a, r, y, t);
	}

	@Override
	protected double[] initializeEdgeAssignmentDistribution(int d) {
		return new double[numFeatures];
	}

	@Override
	protected double getEdgeAssignmentScore(int d, int t, int a, int r, int y,
			boolean init) {

		double score = assignmentScore.getLogScore(d, t);
		if (!init) {
			score += edgeScore.getLogProb(t, a, r, y);
		}
		return score;
	}

	@Override
	protected int getRandomEdgeAssignment(int d, int j) {
		return rng.nextInt(numFeatures);
	}

	// the prior here is a left-to-right computation exactly like the prior on
	// the word assignments
	@Override
	protected double logPriorProbEdgeAssignmentComponent() {

		double logProb = 0;

		for (int d = 0; d < numDocs; d++) {

			for (int j = 0; j < numActors - 1; j++) {

				int t = edgeAssignments[d][j];

				logProb += assignmentScore.getLogScore(d, t);
				assignmentScore.incrementCounts(d, t);
			}
		}
		return logProb;
	}

	@Override
	protected int getNumTokenAssignments(int d) {
		return emails.getEmail(d).getLength();
	}
}
