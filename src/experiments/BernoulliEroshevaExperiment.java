package experiments;

import java.io.IOException;

import mixedmembership.JointTextNetworkModel;
import util.LogRandoms;

/**
 * Run an experiment with the Bernoulli model formulation presented in Erosheva
 * et al. (2004) using a collapsed Gibbs sampler (Krafft et al., 2012).
 */
public class BernoulliEroshevaExperiment extends Experiment {

	// these static blocks don't actually seem to do anything at this point, but
	// they at least indicate in the code at which command line arguments are
	// not needed
	{
		options.getOption("num-latent-dims").setRequired(false);
	}

	public BernoulliEroshevaExperiment(String[] args) {
		super(args);
	}

	public static void main(String[] args) throws java.io.IOException {
		main(args, null);
	}

	public static JointTextNetworkModel main(String[] args, LogRandoms rng)
			throws IOException {

		// I can't figure out a better way to instantiate an object of the
		// extending class. In principle this main method should be able to be
		// moved up to Experiment in all of these subclasses.
		Experiment experiment = new BernoulliEroshevaExperiment(args);
		JointTextNetworkModel model = experiment.estimateModel(rng);

		return model;
	}

	@Override
	public boolean usingMarginalizedAssignments() {
		return false;
	}

	@Override
	public boolean usingExchangeableJointStructure() {
		return true;
	}

	@Override
	public boolean usingErosheva() {
		return true;
	}

	@Override
	public boolean usingEdgeModel() {
		return true;
	}

	@Override
	public boolean usingBernoulli() {
		return true;
	}

	@Override
	public boolean usingWordModel() {
		return true;
	}

	@Override
	public boolean usingMixtureModel() {
		return false;
	}

	@Override
	public boolean usingAsymmetric() {
		return false;
	}
}
