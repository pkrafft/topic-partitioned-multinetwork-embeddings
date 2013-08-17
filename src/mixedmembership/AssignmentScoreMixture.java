package mixedmembership;

public class AssignmentScoreMixture extends AssignmentScore {

	public AssignmentScoreMixture(int numTopics, double[] alpha,
			JointStructure modelStructure) {
		super(1, numTopics, alpha, modelStructure);
	}

	@Override
	public double getLogScore(int d, int t) {
		return super.getLogScore(0, t);
	}

	@Override
	public double getLogValue(int d, int t) {
		return super.getLogValue(0, t);
	}

	@Override
	public double getLogNormalizer(int d) {
		return super.getLogNormalizer(0);
	}

	@Override
	public void incrementCounts(int d, int t) {
		super.incrementCounts(0, t);
	}

	@Override
	public void decrementCounts(int d, int t) {
		super.decrementCounts(0, t);
	}
}
