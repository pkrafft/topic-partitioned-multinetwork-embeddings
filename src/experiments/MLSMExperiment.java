package experiments;

import java.io.IOException;

import mixedmembership.JointTextNetworkModel;
import util.LogRandoms;

/**
 * Run an experiment with the edge-only mixture model formulation presented in
 * Krafft et al. (2012).
 */
public class MLSMExperiment extends Experiment {

	{
		options.getOption("word-matrix").setRequired(false);
		options.getOption("vocab").setRequired(false);
		options.getOption("beta").setRequired(false);
		options.getOption("gamma").setRequired(false);
	}

	public MLSMExperiment(String[] args) {
		super(args);
	}

	public static void main(String[] args) throws java.io.IOException {
		main(args, null);
	}

	public static JointTextNetworkModel main(String[] args, LogRandoms rng)
			throws IOException {

		Experiment experiment = new MLSMExperiment(args);
		JointTextNetworkModel model = experiment.estimateModel(rng);

		return model;
	}

	@Override
	public boolean usingMarginalizedAssignments() {
		return false;
	}

	@Override
	public boolean usingExchangeableJointStructure() {
		return false;
	}

	@Override
	public boolean usingErosheva() {
		return false;
	}

	@Override
	public boolean usingEdgeModel() {
		return true;
	}

	@Override
	public boolean usingBernoulli() {
		return false;
	}

	@Override
	public boolean usingWordModel() {
		return false;
	}

	@Override
	public boolean usingMixtureModel() {
		return true;
	}

	@Override
	public boolean usingAsymmetric() {
		return false;
	}
}
