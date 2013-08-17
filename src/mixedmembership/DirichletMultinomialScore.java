package mixedmembership;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import util.SliceSamplable;
import data.EmailCorpus;

/**
 * A class for dealing with compound Dirichlet-Multinomial distributions,
 * notably tracking counts and sampling hyperparameters. The number of
 * components corresponds to the number of multinomial distributions. The number
 * of elements corresponds to the number of parameters in each multinomial
 * distribution. An item is a data point drawn from a multinomial component.
 * 
 * This class is abstract so that its subclasses can implement the log prob
 * method from the SliceSamplable interface as appropriate.
 */
public abstract class DirichletMultinomialScore implements SliceSamplable {

	protected double[] alpha;
	protected int numComponents;
	protected int numElements;

	// tracks how many items of each type of element are assigned in each
	// component
	protected int[][] componentElementCounts;

	// tracks how many items total are assigned in each component
	protected int[] componentCountsNorm;

	JointStructure modelStructure;

	/**
	 * 
	 * @param numComponents
	 *            number of mixture components
	 * @param numElements
	 *            number of element types
	 * @param alpha
	 *            Dirichlet concentration parameter
	 * @param modelStructure
	 *            a back pointer to the model structure that uses this score
	 */
	public DirichletMultinomialScore(int numComponents, int numElements,
			double[] alpha, JointStructure modelStructure) {

		this.numElements = numElements;
		this.numComponents = numComponents;

		assert alpha.length == 1;
		this.alpha = alpha;

		componentElementCounts = new int[numComponents][numElements];
		componentCountsNorm = new int[numComponents];

		this.modelStructure = modelStructure;
	}

	/**
	 * Calculate the probability of a given element type in a given component.
	 * 
	 * @param c
	 *            an index of a component
	 * @param i
	 *            an index of an element type
	 * 
	 * @return the smoothed proportion of times that type occurs in that topic
	 */
	public double getLogScore(int c, int i) {
		return getLogValue(c, i) - getLogNormalizer(c);
	}

	public double getLogValue(int c, int i) {
		return Math.log(componentElementCounts[c][i] + 1.0 / numElements
				* alpha[0]);
	}

	public double getLogNormalizer(int c) {
		return Math.log(componentCountsNorm[c] + alpha[0]);
	}

	// for keeping data structures updated
	public void incrementCounts(int c, int i) {
		componentElementCounts[c][i]++;
		componentCountsNorm[c]++;
	}

	// for keeping data structures updated
	public void decrementCounts(int c, int i) {
		componentElementCounts[c][i]--;
		componentCountsNorm[c]--;
		assert componentElementCounts[c][i] >= 0;
	}

	// clear data structures
	public void resetCounts() {

		for (int c = 0; c < numComponents; c++)
			Arrays.fill(componentElementCounts[c], 0);

		Arrays.fill(componentCountsNorm, 0);
	}

	public double[] getLogDocNorms() {
		double[] norms = new double[numComponents];
		for (int c = 0; c < numComponents; c++) {
			norms[c] = getLogNormalizer(c);
		}
		return norms;
	}

	public double[][] getLogDocFeatureProportions(EmailCorpus emails) {

		double[][] probs = new double[numComponents][numElements];

		for (int c = 0; c < numComponents; c++) {

			for (int i = 0; i < numElements; i++) {
				probs[c][i] = getLogScore(c, i);
			}
		}

		return probs;
	}

	public double[] getAlpha() {
		return alpha;
	}

	public void printAlpha(String fileName) {

		try {

			PrintWriter pw = new PrintWriter(fileName);

			for (int i = 0; i < alpha.length; i++) {
				pw.println(alpha[i]);
			}

			pw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	@Override
	public double[] getSliceSamplableParameters() {
		return alpha;
	}

	@Override
	public void setSliceSamplableParameters(double[] newValues) {
		System.arraycopy(newValues, 0, alpha, 0, alpha.length);
	}
}
