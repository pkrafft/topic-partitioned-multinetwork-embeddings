package mixedmembership;

/**
 * A class for the mixture/admixture components for the edge model.
 * 
 * This package is roughly organized according to the graphical models of the
 * possible joint text and network models. This class essentially represents the
 * plate surrounding the mixture components that describe the generative process
 * for the recipients.
 */
public interface EdgeScore {

	/**
	 * Test the self-consistency of the fields in this edge model.
	 * 
	 * @param assignmentModel
	 *            a model that provides the edge assignments to mixture
	 *            components
	 * @return whether the test passes
	 * @throws Exception
	 */
	public boolean checkCacheConsistency(JointStructure assignmentModel)
			throws Exception;

	/**
	 * A method for keeping track of how many edges are assigned to each
	 * mixture/admixture component.
	 * 
	 * @param i
	 *            an index of a mixture/admixture component
	 * @param a
	 *            the first actor of the edge
	 * @param r
	 *            the second actor of the edge
	 * @param y
	 *            1 if the edge is present, 0 if absent
	 */
	public void decrementCounts(int i, int a, int r, int y);

	public void incrementCounts(int i, int a, int r, int y);

	/**
	 * Calculate the probability of a particular edge according to a particular
	 * mixture/admixture component of this edge model.
	 * 
	 * @param t
	 *            what mixture/admixture component to use
	 * @param a
	 *            the id of the first actor in the edge
	 * @param r
	 *            the id of the second actor in the edge
	 * @param y
	 *            whether to calculate the probability of a present connection
	 *            (y = 1) or an absent connection (y = 0)
	 * @return the log probability
	 */
	public double getLogProb(int t, int a, int r, int y);
}