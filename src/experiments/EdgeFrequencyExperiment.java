package experiments;

import util.LogRandoms;
import baselines.EdgeFrequencyModel;
import data.EmailCorpus;
import data.InstanceListLoader;

/**
 * Run an experiment with the edge-only baseline described in Krafft et al.
 * (2012).
 */
public class EdgeFrequencyExperiment {

	public static void main(String[] args) throws java.io.IOException {

		if (args.length != 5) {
			System.out
					.println("Usage: <edge_matrix> <num_actors> <obscure_random> "
							+ "<test_set_start> <test_set_end>");
			System.exit(1);
		}

		int index = 0;

		String edgeMatrixFileName = args[index++];

		int numActors = Integer.parseInt(args[index++]);

		boolean obscureRandom = Boolean.parseBoolean(args[index++]);

		int startTest = Integer.parseInt(args[index++]);
		int endTest = Integer.parseInt(args[index++]);

		assert index == 5;

		EmailCorpus trueEmails = new EmailCorpus(numActors);
		InstanceListLoader.loadNetwork(edgeMatrixFileName, trueEmails);

		EmailCorpus testEmails = new EmailCorpus(numActors);
		InstanceListLoader.loadNetwork(edgeMatrixFileName, testEmails);
		LogRandoms rng = new LogRandoms();

		if (obscureRandom) {
			double p = 0.1;
			testEmails.obscureRandomEdges(p, rng);
		} else {
			testEmails.obscureContinuousTestSet(startTest, endTest);
		}

		EdgeFrequencyModel model = new EdgeFrequencyModel(testEmails, rng);
		model.estimate();

		Experiment.printEdgeConfusion(trueEmails, testEmails, false, null);
		Experiment.printHeldOutEdgeLike(trueEmails, model);
	}
}