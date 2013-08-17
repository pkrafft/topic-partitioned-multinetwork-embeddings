//package experiments;
//
//import java.io.File;
//import java.util.Arrays;
//
//import mixedmembership.JointTextNetworkModel;
//
////TODO: NOT UP-TO-DATE!
//public class ConditionalStructureSimulationExperiment extends Experiment {
//
//	protected static int iterOffset;
//	protected static String readFolder;
//	private static int repetitions;
//
//	public static void main(String[] args) throws java.io.IOException {
//
//		parseArgs(args);
//		loadData();
//		initializeParameters(true, true, true);
//		runExperiment();
//	}
//
//	protected static void runExperiment() throws java.io.IOException {
//
//		boolean usingBernoulli = false;
//		if (latentDim < 0) {
//			usingBernoulli = true;
//		}
//
//		JointTextNetworkModel model = new JointTextNetworkModel(emails, numFeatures, null,
//				true, true, true, usingBernoulli, false, false, false,
//				latentDim, null);
//
//		model.initialize(null, null, null, null, alpha, beta, gamma, true,
//				printInterval, saveStateInterval, documentTopicsFileName,
//				wordStateFileName, edgeStateFileName, alphaFileName,
//				betaFileName, gammaFileName, latentSpaceFileName,
//				interceptFileName, topicWordsFileName, topicSummaryFileName,
//				logProbFileName, logLikeFileName, missingEdgeFileName);
//
//		String loadedWordStateFileName = readFolder + "/word_state.txt.gz."
//				+ iterOffset;
//		String loadedEdgeStateFileName = readFolder + "/edge_state.txt.gz."
//				+ iterOffset;
//		String loadedLatentSpaceFileName = readFolder + "/latent_spaces.txt."
//				+ iterOffset;
//		String loadedInterceptFileName = readFolder + "/intercepts.txt."
//				+ iterOffset;
//
//		model.readFromFiles(loadedWordStateFileName, loadedEdgeStateFileName,
//				loadedLatentSpaceFileName, loadedInterceptFileName, null,
//				iterOffset);
//
//		String topOutputDir = outputDir;
//
//		outputDir = topOutputDir + "/true-network";
//		new File(outputDir).mkdir();
//		emails.getCoarseNetwork().printNetwork(outputDir, true);
//
//		for (int i = 0; i < repetitions; i++) {
//
//			model.getAssignmentModel().sampleAllEdges();
//
//			outputDir = topOutputDir + "/simulated-network-" + i;
//			new File(outputDir).mkdir();
//			emails.getCoarseNetwork().printNetwork(outputDir, true);
//		}
//	}
//
//	public static void parseArgs(String[] args) {
//
//		if (args.length != 13) {
//			System.out
//					.println("Usage: <word_matrix> <vocab_list> <edge_matrix> "
//							+ "<num_authors> <num_topics> <latent_dim> <repetitions> "
//							+ "<alpha> <beta> <gamma> "
//							+ "<folder> <iter_offset> " + "<output_dir>");
//			System.exit(1);
//		}
//
//		int index = 0;
//
//		wordMatrixFileName = args[index++];
//		vocabListFileName = args[index++];
//		edgeMatrixFileName = args[index++];
//
//		numActors = Integer.parseInt(args[index++]);
//		numFeatures = Integer.parseInt(args[index++]);
//		latentDim = Integer.parseInt(args[index++]);
//
//		repetitions = Integer.parseInt(args[index++]);
//
//		alphaParameter = Double.parseDouble(args[index++]);
//		betaParameter = Double.parseDouble(args[index++]);
//		gammaParameter = Double.parseDouble(args[index++]);
//
//		sampleHypers = new boolean[3];
//		Arrays.fill(sampleHypers, false);
//
//		readFolder = args[index++];
//		iterOffset = Integer.parseInt(args[index++]);
//
//		outputDir = args[index++];
//		new File(outputDir).mkdirs();
//
//		assert index == 13;
//	}
//}
