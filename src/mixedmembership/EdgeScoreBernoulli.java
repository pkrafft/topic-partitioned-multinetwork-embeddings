package mixedmembership;

import java.io.IOException;
import java.io.PrintWriter;

import util.SliceSamplable;
import data.EmailCorpus;

/**
 * An abstract class for edge scores that use collapsed mixtures/admixtures of
 * Bernoulli distributions
 */
public abstract class EdgeScoreBernoulli implements EdgeScore, Resettable,
		SliceSamplable {

	double[] gamma;

	protected int numActors, numFeatures, numDocs;

	protected EmailCorpus emails;
	protected JointStructure modelStructure;

	/**
	 * 
	 * @param numFeatures
	 *            the number of mixture/admixture components in the model
	 * @param gamma
	 *            initial values for the Beta hyperparameters of each mixture
	 *            component
	 * @param modelStructure
	 *            a back pointer to the joint structure that this score is part
	 *            of
	 */
	public void initialize(int numFeatures, double[] gamma, JointStructure modelStructure) {
		
		this.modelStructure = modelStructure;
		this.emails = modelStructure.getEmails();
		numDocs = emails.size();
		this.numActors = emails.getNumAuthors();
		
		this.numFeatures = numFeatures;

		assert gamma.length == 1;
		this.gamma = gamma;
	}

	public void printGamma(String fileName) {

		try {

			PrintWriter pw = new PrintWriter(fileName);

			for (int i = 0; i < gamma.length; i++)
				pw.println(gamma[i]);

			pw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	@Override
	public double logProb() {
		return modelStructure.logEdgeDataProb();
	}

	@Override
	public double[] getSliceSamplableParameters() {
		return gamma;
	}

	@Override
	public void setSliceSamplableParameters(double[] newValues) {
		System.arraycopy(newValues, 0, gamma, 0, gamma.length);
	}

	public double[] getGamma() {
		return gamma;
	}
}
