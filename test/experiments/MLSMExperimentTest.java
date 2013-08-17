package experiments;

import java.io.IOException;

import mixedmembership.JointTextNetworkModel;
import util.LogRandoms;

public class MLSMExperimentTest extends ExperimentTest {

	@Override
	public JointTextNetworkModel initializeModel(LogRandoms rng) {

		JointTextNetworkModel model = new JointTextNetworkModel(emails, T, K,
				rng, false, true, true, false, false, false, false, true, false,
				false, false, null);

		model.initialize(null, null, null, null, alpha, null, null, true, 0, 1,
				null, wordStateFileName + ".true", null, null, null, null,
				latentSpaceFileName + ".true", interceptFileName + ".true",
				null, null, null, null, missingEdgesFileName + ".true", 0,
				null, null, null, null, null, null, null, null);

		return model;
	}

	@Override
	public String[] getFirstExperimentArgs(int firstNumIter) {
		return new String[] { "-ef=" + edgeMatrixFile,
				"-a=" + emails.getNumAuthors() + "", "-t=" + T, "-k=" + K,
				"-n=" + firstNumIter + "", "-p=" + "0", "-s=" + "1", "-v",
				"--alpha=" + alphaBase / T + "", "-h", "-e", "-of=" + "./" };
	}

	@Override
	public String[] getNextExperimentArgs(int nextNumIter, int itersDone) {
		return new String[] { "-ef=" + edgeMatrixFile,
				"-a=" + emails.getNumAuthors() + "", "-t=" + T + "", "-k=" + K,
				"-n=" + nextNumIter + "", "-p=" + "0", "-s=" + "1", "-v",
				"--alpha=" + alphaBase / T + "", "-r ", "-rf=" + "./",
				"-i=" + itersDone + "", "-h", "-e", "-of=" + "./" };
	}

	@Override
	public JointTextNetworkModel runExperiment(String[] args, LogRandoms rng)
			throws IOException {
		return MLSMExperiment.main(args, rng);
	}

	@Override
	public void otherAssertions(JointTextNetworkModel model,
			JointTextNetworkModel mainModel) {

		edgeModelAssertions(model, mainModel);
		latentSpaceModelAssertions(model, mainModel);
	}

	@Override
	public boolean usingEdgeModel() {
		return true;
	}
}
