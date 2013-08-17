package mixedmembership;

import data.Email;

/**
 * This joint structure implements a mixture model rather than an admixture
 * model.
 * 
 * Each document has only one token assignment which determines the topic
 * assignments for all tokens and edges in that document. These assignments are
 * drawn from a single shared multinomial distribution whose parameters are
 * marginalized out.
 * 
 * The way this is implemented means that there are simply no edge assignments
 * (they all point to the one token assignment), so all of the parts of the code
 * that deal with the edge assignments are not applicable.
 */
public class JointStructureMixture extends JointStructure {

	@Override
	protected AssignmentScore initializeAssignmentScore(double[] alpha) {
		return new AssignmentScoreMixture(numFeatures, alpha, this);
	}

	@Override
	protected int getTokenTopicAssignment(int d, int j) {
		return tokenAssignments[d][0];
	}

	@Override
	protected int getEdgeTopicAssignment(int d, int j) {
		return tokenAssignments[d][0];
	}

	@Override
	protected void unassignToken(int d, int i, int a, int w, int t) {

		assignmentScore.decrementCounts(d, t);

		if (usingWordModel) {
			// decrement counts for all tokens
			int[] words = emails.getDocument(d).getTokens();
			int nd = emails.getDocument(d).getLength();
			for (i = 0; i < nd; i++) {
				w = words[i];
				wordScore.decrementCounts(t, w);
			}
		}

		if (usingEdgeModel) {
			// decrement counts for all recipients
			Email email = emails.getEmail(d);
			for (int j = 0; j < numActors - 1; j++) {
				int r = email.getRecipient(j);
				int y = email.getEdge(r);
				edgeScore.decrementCounts(t, a, r, y);
			}
		}
	}

	@Override
	protected void assignToken(int d, int i, int a, int w, int t) {

		tokenAssignments[d][0] = t;

		assignmentScore.incrementCounts(d, t);

		if (usingWordModel) {
			// increment counts for all tokens
			int[] words = emails.getDocument(d).getTokens();
			int nd = emails.getDocument(d).getLength();
			for (i = 0; i < nd; i++) {
				w = words[i];
				wordScore.incrementCounts(t, w);
			}
		}

		if (usingEdgeModel) {
			// increment counts for all recipients
			Email email = emails.getEmail(d);
			for (int j = 0; j < numActors - 1; j++) {
				int r = email.getRecipient(j);
				int y = email.getEdge(r);
				edgeScore.incrementCounts(t, a, r, y);
			}
		}
	}

	// a single assignment affects the whole document, so this method is just
	// the likelihood of all the data (word and edge) in document d
	@Override
	protected double getTokenAssignmentLogLike(int d, int i, int a, int w, int t) {

		double score = 0;

		if (usingWordModel) {

			int[] words = emails.getDocument(d).getTokens();
			int nd = emails.getDocument(d).getLength();

			for (i = 0; i < nd; i++) {
				w = words[i];
				score += wordScore.getLogScore(t, w);

				// temporarily increment counts to get the right joint
				// likelihood
				wordScore.incrementCounts(t, w);
			}

			// remove the counts that were temporarily incremented
			for (i = 0; i < nd; i++) {
				w = words[i];
				wordScore.decrementCounts(t, w);
			}
		}

		if (usingEdgeModel) {

			Email email = emails.getEmail(d);

			// TODO: there is an edge case where we would need to
			// increment/decrement edgeScore here---if we implement Erosheva's
			// primary model with Gibbs sampling of held-out recipients
			for (int j = 0; j < numActors - 1; j++) {
				int r = email.getRecipient(j);
				int y = email.getEdge(r);

				// currently no need to increment here because there is at most
				// one instance of each recipient per document (see TODO above)
				score += edgeScore.getLogProb(t, a, r, y);
			}
		}

		return score;
	}

	// NA - no edge assignments
	@Override
	protected double logPriorProbEdgeAssignmentComponent() {
		return 0;
	}

	// NA - no edge assignments
	@Override
	protected double[] initializeEdgeAssignmentDistribution(int d) {
		return null;
	}

	// NA - no edge assignments
	@Override
	protected double getEdgeAssignmentScore(int d, int i, int a, int r, int y,
			boolean init) {
		return -1;
	}

	@Override
	protected int getNumTokenAssignments(int d) {
		return 1;
	}

	// NA - no edge assignments
	@Override
	protected int getRandomEdgeAssignment(int d, int j) {
		return 0;
	}

	@Override
	public boolean usingMixtureModel() {
		return true;
	}
}
