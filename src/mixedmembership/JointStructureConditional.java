package mixedmembership;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;

/**
 * This class implements the joint structure that is used in the main model
 * described by Krafft et al. (2012).
 * 
 * This structure is based on Corr-LDA (Blei and Jordan, 2003). In this
 * structure recipients are assigned to tokens rather than directly to topics.
 * The recipients are associated with the topics that those tokens are
 * themselves assigned to.
 * 
 * The most notable difference in this structure is thus that the edge
 * assignments are drawn from an N*_d dimensional distribution, where N*_d is
 * the number of "token assignments" in document d. When the number of tokens in
 * a document is zero we use a dummy token assignment that is not associated
 * with any actual tokens so N_d = 1 and we are still able to sample the topic
 * for all of the edges in that document.
 */
public class JointStructureConditional extends JointStructure {

	// an inverted index giving all of the edges that are associated to each
	// token
	TIntHashSet[][] wordEdgeHash;

	@Override
	public void initializeDataStructures(int[][] initWordAssignments,
			int[][] initEdgeAssignments, boolean initializeMissingValues,
			boolean sampleFromPrior) {

		wordEdgeHash = new TIntHashSet[numDocs][];
		for (int d = 0; d < numDocs; d++) {
			int Nd = getNumTokenAssignments(d);
			wordEdgeHash[d] = new TIntHashSet[Nd];
			for (int i = 0; i < Nd; i++) {
				wordEdgeHash[d][i] = new TIntHashSet();
			}
		}

		super.initializeDataStructures(initWordAssignments,
				initEdgeAssignments, initializeMissingValues, sampleFromPrior);
	}

	@Override
	protected double[] initializeEdgeAssignmentDistribution(int d) {
		int Nd = Math.max(1, emails.getDocument(d).getLength());
		return new double[Nd];
	}

	@Override
	protected void unassignEdge(int d, int j, int a, int r, int y,
			int oldAssignment) {
		wordEdgeHash[d][oldAssignment].remove(r);
		super.unassignEdge(d, j, a, r, y, tokenAssignments[d][oldAssignment]);
	}

	@Override
	protected void assignEdge(int d, int j, int a, int r, int y, int assignment) {
		edgeAssignments[d][j] = assignment;
		wordEdgeHash[d][assignment].add(r);
		super.assignEdge(d, j, a, r, y, tokenAssignments[d][assignment]);
	}

	protected double getEdgeAssignmentScore(int d, int i, int a, int r, int y,
			boolean init) {
		// in this model the prior on edge assignments is a uniform distribution
		return init ? 0 : edgeScore.getLogProb(tokenAssignments[d][i], a, r, y);
	}

	@Override
	protected void unassignToken(int d, int i, int a, int w, int t) {

		super.unassignToken(d, i, a, w, t);

		// update the data structures that depend on the edges assigned to this
		// token
		TIntIterator it = wordEdgeHash[d][i].iterator();
		while (it.hasNext()) {
			int r = it.next();
			int y = emails.getEmail(d).getEdge(r);
			edgeScore.decrementCounts(t, a, r, y);
		}
	}

	@Override
	protected double getTokenAssignmentLogLike(int d, int i, int a, int w, int t) {

		double score = 0;

		// if this is not a dummy token assignment
		if (emails.getEmail(d).getLength() != 0) {
			score += super.getTokenAssignmentLogLike(d, i, a, w, t);
		}

		return score + getEdgeLogLike(d, i, t, a);
	}

	// get the likelihood of all the edges associated with this token
	private double getEdgeLogLike(int d, int i, int t, int a) {
		TIntIterator it = wordEdgeHash[d][i].iterator();

		double logProb = 0;
		while (it.hasNext()) {
			int r = it.next();
			int y = emails.getEmail(d).getEdge(r);
			logProb += edgeScore.getLogProb(t, a, r, y);
		}

		return logProb;
	}

	@Override
	protected void assignToken(int d, int i, int a, int w, int t) {

		super.assignToken(d, i, a, w, t);

		// update the data structures that depend on the edges assigned to this
		// token
		TIntIterator it = wordEdgeHash[d][i].iterator();
		while (it.hasNext()) {
			int r = it.next();
			int y = emails.getEmail(d).getEdge(r);
			edgeScore.incrementCounts(t, a, r, y);
		}
	}

	@Override
	protected int getRandomEdgeAssignment(int d, int j) {
		int Nd = getNumTokenAssignments(d);
		return rng.nextInt(Nd);
	}

	@Override
	protected int getNumTokenAssignments(int d) {
		return Math.max(1, emails.getDocument(d).getLength());
	}

	// uniform prior
	@Override
	protected double logPriorProbEdgeAssignmentComponent() {
		double logProb = 0;
		for (int d = 0; d < numDocs; d++) {
			logProb -= (numActors - 1) * Math.log(tokenAssignments[d].length);
		}
		return logProb;
	}

	// get the topic associated with the token that this edge is assigned to
	@Override
	protected int getEdgeTopicAssignment(int d, int j) {
		return tokenAssignments[d][edgeAssignments[d][j]];
	}
}
