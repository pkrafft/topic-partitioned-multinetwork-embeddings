package util;

/**
 * An interface for distributions whose parameters can be slice sampled, or who
 * have a subset of parameters that can be slice sampled.
 */
public interface SliceSamplable {

	/**
	 * 
	 * @return the parameters that can be slice sampled
	 */
	double[] getSliceSamplableParameters();

	/**
	 * Set the slice samplable parameters some new values. This method is
	 * typically just copying the new values into the parameter object, and this
	 * could be done from the SliceSampler class itself, but we have made a
	 * separate method here in case any caches need to be updated when the
	 * values of the parameters change.
	 * 
	 * @param newValues
	 *            new values for the parameters
	 */
	void setSliceSamplableParameters(double[] newValues);

	/**
	 * Get the components of the joint probability of the entire model that
	 * involve the parameters being sampled.
	 * 
	 * @return the relevant probability
	 */
	double logProb();

}
