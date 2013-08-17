package experiments;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import data.Email;

import mixedmembership.EdgeScoreLatentSpace;
import mixedmembership.JointTextNetworkModel;
import util.LogRandoms;

public class ConditionalStructureExperimentTest extends ExperimentTest {

	@Override
	public JointTextNetworkModel initializeModel(LogRandoms rng) {

		JointTextNetworkModel model = new JointTextNetworkModel(emails, T, K,
				rng, true, true, true, false, false, false, false, false, false,
				false, false, null);

		model.initialize(null, null, null, null, alpha, beta, null, true, 0, 1,
				null, wordStateFileName + ".true", edgeStateFileName + ".true",
				null, null, null, latentSpaceFileName + ".true",
				interceptFileName + ".true", null, null, null, null,
				missingEdgesFileName + ".true", 0, null, null, null, null,
				null, null, null, null);

		return model;
	}

	@Override
	public String[] getFirstExperimentArgs(int firstNumIter) {
		return new String[] { "-wf=" + wordMatrixFile, "-vf=" + vocabFile,
				"-ef=" + edgeMatrixFile, "-a=" + emails.getNumAuthors() + "",
				"-t=" + T, "-k=" + K, "-n=" + firstNumIter + "", "-p=" + "0",
				"-s=" + "1", "-v", "--alpha=" + alphaBase / T + "",
				"--beta=" + betaBase / emails.getWordDict().size() + "", "-h",
				"-e", "-of=" + "./" };
	}

	@Override
	public String[] getNextExperimentArgs(int nextNumIter, int itersDone) {
		return new String[] { "-wf=" + wordMatrixFile, "-vf=" + vocabFile,
				"-ef=" + edgeMatrixFile, "-a=" + emails.getNumAuthors() + "",
				"-t=" + T + "", "-k=" + K, "-n=" + nextNumIter + "",
				"-p=" + "0", "-s=" + "1", "-v",
				"--alpha=" + alphaBase / T + "",
				"--beta=" + betaBase / emails.getWordDict().size() + "", "-r ",
				"-rf=" + "./", "-i=" + itersDone + "", "-h", "-e",
				"-of=" + "./" };
	}

	@Override
	public JointTextNetworkModel runExperiment(String[] args, LogRandoms rng)
			throws IOException {
		return ConditionalStructureExperiment.main(args, rng);
	}

	@Override
	public void otherAssertions(JointTextNetworkModel model,
			JointTextNetworkModel mainModel) {

		emailAssertion(mainModel);
		wordModelAssertions(model, mainModel);
		edgeModelAssertions(model, mainModel);
		latentSpaceModelAssertions(model, mainModel);

		double logProb = 0;
		double[] logProbVec = new double[emails.size()
				* (emails.getNumAuthors() - 1)];

		EdgeScoreLatentSpace edgeScore = (EdgeScoreLatentSpace) model
				.getEdgeScore();
		int[][] wordAssignments = model.getAssignmentModel()
				.getWordAssignments();
		int[][] edgeAssignments = model.getAssignmentModel()
				.getEdgeAssignments();

		int ind = 0;
		for (int d = 0; d < emails.size(); d++) {
			Email email = emails.getEmail(d);
			int a = email.getAuthor();
			for (int j = 0; j < emails.getNumAuthors() - 1; j++) {
				int r = email.getRecipient(j);
				int t = wordAssignments[d][edgeAssignments[d][j]];
				int y = email.getEdge(r);

				logProb += edgeScore.getLogProb(t, a, r, y);
				logProbVec[ind] = logProb;
				ind++;
			}
		}

		logProb = 0;
		double[] logProbVec2 = new double[emails.size()
				* (emails.getNumAuthors() - 1)];

		edgeScore = (EdgeScoreLatentSpace) mainModel.getEdgeScore();
		wordAssignments = mainModel.getAssignmentModel().getWordAssignments();
		edgeAssignments = mainModel.getAssignmentModel().getEdgeAssignments();

		ind = 0;
		for (int d = 0; d < emails.size(); d++) {
			Email email = emails.getEmail(d);
			int a = email.getAuthor();
			for (int j = 0; j < emails.getNumAuthors() - 1; j++) {
				int r = email.getRecipient(j);
				int t = wordAssignments[d][edgeAssignments[d][j]];
				int y = email.getEdge(r);

				logProb += edgeScore.getLogProb(t, a, r, y);
				logProbVec2[ind] = logProb;
				ind++;
			}
		}

		for (int i = 0; i < logProbVec.length; i++) {
			assertEquals(logProbVec[i], logProbVec2[i], 1e-12);
		}
	}

	@Override
	public boolean usingEdgeModel() {
		return true;
	}
}
