package experiments;

import java.io.IOException;

import mixedmembership.JointTextNetworkModel;
import util.LogRandoms;

public class LDAExperimentTest extends ExperimentTest {

	@Override
	public JointTextNetworkModel initializeModel(LogRandoms rng) {

		JointTextNetworkModel model = new JointTextNetworkModel(emails, T, K,
				rng, true, false, false, false, false, true, false, false, false,
				false, false, null);

		model.initialize(null, null, null, null, alpha, beta, null, true, 0, 1,
				null, wordStateFileName + ".true", null, null, null, null,
				null, null, null, null, null, null, null, 0, null, null, null,
				null, null, null, null, null);

		return model;
	}

	@Override
	public String[] getFirstExperimentArgs(int firstNumIter) {
		return new String[] { "-wf=" + wordMatrixFile, "-vf=" + vocabFile,
				"-t=" + T, "-n=" + firstNumIter + "", "-p=" + "0", "-s=" + "1",
				"-v", "--alpha=" + alphaBase / T + "",
				"--beta=" + betaBase / emails.getWordDict().size() + "", "-h",
				"-of=" + "./" };
	}

	@Override
	public String[] getNextExperimentArgs(int nextNumIter, int itersDone) {
		return new String[] { "-wf=" + wordMatrixFile, "-vf=" + vocabFile,
				"-t=" + T, "-n=" + nextNumIter + "", "-p=" + "0", "-s=" + "1",
				"-v", "--alpha=" + alphaBase / T + "",
				"--beta=" + betaBase / emails.getWordDict().size() + "", "-h",
				"-r ", "-rf=" + "./", "-i=" + itersDone, "-of=" + "./" };
	}

	@Override
	public JointTextNetworkModel runExperiment(String[] args, LogRandoms rng)
			throws IOException {
		return LDAExperiment.main(args, rng);
	}

	@Override
	public void otherAssertions(JointTextNetworkModel model,
			JointTextNetworkModel mainModel) {

		wordModelAssertions(model, mainModel);
	}

	@Override
	public boolean usingEdgeModel() {
		return false;
	}
}
