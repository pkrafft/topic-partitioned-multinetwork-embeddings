package experiments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;

import mixedmembership.JointTextNetworkModel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import util.LogRandoms;
import data.EmailCorpus;
import data.InstanceListLoader;

/**
 * The primary class for model-specific experiment classes to extend.
 * 
 * This class provides a common command line argument parser and methods for
 * setting up, running, and evaluating experiments.
 */
public abstract class Experiment {

	String[] args;

	double[] alpha;
	double[] beta;
	double[] gamma;

	String optionsFileName = null;
	String documentTopicsFileName = null;
	String wordStateFileName = null;
	String edgeStateFileName = null;
	String alphaFileName = null;
	String betaFileName = null;
	String gammaFileName = null;
	String latentSpaceFileName = null;
	String interceptFileName = null;
	String topicWordsFileName = null;
	String topicSummaryFileName = null;
	String logProbFileName = null;
	String logLikeFileName = null;
	String predictionsFileName = null;
	String missingEdgeFileName = null;

	static int batchSize = 1000;

	private int numActors;
	private int numFeatures;
	private int latentDim;

	private String wordMatrixFileName;
	private String vocabListFileName;
	private String edgeMatrixFileName;

	private String outputDir;
	private String sourceDir;

	private boolean sampleHypers;
	private double alphaParameter;
	private double betaParameter;
	private double gammaParameter;

	private int numIterations;
	private int printInterval;
	private int saveStateInterval;

	private boolean readFromFolder;
	private boolean obscureRandom;
	private int startTest;
	private int endTest;
	private String readFolder;
	private int iterOffset;

	private boolean missingEdgeExperiment;

	private boolean verbose;
	private boolean clusterJob;
	private boolean bigJob;
	private boolean longJob;
	private Long timeLimit;

	private boolean sampleEdgeModelOnly;
	private boolean sampleLatentSpacesOnly;
	private String disjointReadFolder;
	private String disjointIterOffset;

	// not all of these arguments are used by all experiments
	static String numIterOpt = "n";
	static String iterOffsetOpt = "i";
	static String alphaOpt = "alpha";
	static String betaOpt = "beta";
	static String gammaOpt = "gamma";
	static String readFromFolderOpt = "r";
	static String readFolderOpt = "rf";
	static Options options;
	{

		options = new Options();
		options.addOption("h", "help", false, "print this message");
		options.addOption("wf", "word-matrix", true, "text data file name");
		options.addOption("vf", "vocab", true, "vocab list file name");
		options.addOption("ef", "edge-matrix", true, "network data file name");
		options.addOption("a", "num-actors", true,
				"number of actors in network");
		options.addOption("t", "num-topics", true, "number of topics to use");
		options.addOption("k", "num-latent-dims", true,
				"dimension of latent spaces");
		options.addOption(numIterOpt, "num-iter", true,
				"number of MCMC iterations");
		options.addOption("p", "print-interval", true,
				"how often to print log prob");
		options.addOption("s", "save-state-interval", true,
				"how often to save state");
		options.addOption("v", "verbose", false, "print verbose output");
		options.addOption("h", "sample-hypers", false,
				"whether to sample hyperparameters");
		options.addOption(alphaOpt, true, "initial alpha hyperparameter value");
		options.addOption(betaOpt, true, "initial beta hyperparameter value");
		options.addOption(gammaOpt, true, "initial gamma hyperparameter value");
		options.addOption(readFromFolderOpt, "read-from-folder", false,
				"read state from folder");
		options.addOption(readFolderOpt, "read-folder", true,
				"folder to read state from");
		options.addOption(iterOffsetOpt, "iter-offset", true,
				"iteration to read from");
		options.addOption(readFromFolderOpt, "read-from-folder", false,
				"read state from folder");
		options.addOption("dje", "disjoint-edge-model-sampler", false,
				"sample only the edge model");
		options.addOption("djl", "disjoint-latent-space-sampler", false,
				"sample only the latent spaces");
		options.addOption("djrf", "disjoint-read-folder", true,
				"folder to read word model state from");
		options.addOption("dji", "disjoint-iter-offset", true,
				"iteration to read from for word model state");
		options.addOption("e", "hold-out-edges", false,
				"whether to hold out random edges");
		options.addOption("of", "out-folder", true, "output folder");
		options.addOption("sd", "source-directory", true,
				"folder containing source");
		options.addOption("c", "cluster-job", false,
				"is this job running on swarm?"
						+ "(primarily relevant at UMass CS)");
		options.addOption("b", "big-job", false,
				"is this a large memory cluster job?"
						+ "(primarily relevant at UMass CS)");
		options.addOption("l", "long-job", false,
				"is this cluster job longer than 5 hours?"
						+ "(primarily relevant at UMass CS)");
		options.addOption("m", "time-limit", true, "when to restart job"
				+ "(primarily relevant at UMass CS)");

		options.getOption("word-matrix").setRequired(true);
		options.getOption("vocab").setRequired(true);
		options.getOption("edge-matrix").setRequired(true);

		options.getOption("num-actors").setRequired(true);
		options.getOption("num-topics").setRequired(true);
		options.getOption("num-latent-dims").setRequired(true);

		options.getOption("num-iter").setRequired(true);
		options.getOption("print-interval").setRequired(true);
		options.getOption("save-state-interval").setRequired(true);

		options.getOption("alpha").setRequired(true);
		options.getOption("beta").setRequired(true);
		options.getOption("gamma").setRequired(true);

		options.getOption("out-folder").setRequired(true);
	}

	public Experiment(String[] args) {
		this.args = args;
		parseArgs(args);
	}

	/**
	 * Read in parameters from the command line and set up experiment.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public void parseArgs(String[] args) {

		CommandLineParser parser = new GnuParser();

		try {
			CommandLine line = parser.parse(options, args);

			if (line.hasOption("help")) {
				(new HelpFormatter()).printHelp(this.getClass()
						.getCanonicalName(), options);
				System.exit(-1);
			}

			numFeatures = Integer.parseInt(line.getOptionValue("num-topics"));
			if (line.hasOption("num-latent-dims")) {
				latentDim = Integer.parseInt(line
						.getOptionValue("num-latent-dims"));
			}
			if (line.hasOption("num-actors")) {
				numActors = Integer.parseInt(line.getOptionValue("num-actors"));
			}

			if (line.hasOption("word-matrix")) {
				wordMatrixFileName = line.getOptionValue("word-matrix");
			}
			if (line.hasOption("vocab")) {
				vocabListFileName = line.getOptionValue("vocab");
			}
			if (line.hasOption("edge-matrix")) {
				edgeMatrixFileName = line.getOptionValue("edge-matrix");
			}

			outputDir = line.getOptionValue("out-folder");
			sourceDir = line.getOptionValue("source-directory");

			sampleHypers = line.hasOption("sample-hypers");
			alphaParameter = Double.parseDouble(line.getOptionValue("alpha"));
			if (line.hasOption("beta")) {
				betaParameter = Double.parseDouble(line.getOptionValue("beta"));
			}
			if (line.hasOption("gamma")) {
				gammaParameter = Double.parseDouble(line
						.getOptionValue("gamma"));
			}

			numIterations = Integer.parseInt(line.getOptionValue("num-iter"));

			printInterval = Integer.parseInt(line
					.getOptionValue("print-interval"));
			saveStateInterval = Integer.parseInt(line
					.getOptionValue("save-state-interval"));

			if (line.hasOption("read-from-folder")) {
				readFromFolder = true;
				readFolder = line.getOptionValue("read-folder");
				iterOffset = Integer.parseInt(line
						.getOptionValue("iter-offset"));
			}

			sampleEdgeModelOnly = line.hasOption("disjoint-edge-model-sampler");
			sampleLatentSpacesOnly = line
					.hasOption("disjoint-latent-space-sampler");
			if (sampleEdgeModelOnly || sampleLatentSpacesOnly) {
				disjointReadFolder = line
						.getOptionValue("disjoint-read-folder");
				disjointIterOffset = line
						.getOptionValue("disjoint-iter-offset");
			}

			missingEdgeExperiment = line.hasOption("hold-out-edges");

			obscureRandom = true;
			startTest = -1;
			endTest = -1;

			verbose = line.hasOption("verbose");

			clusterJob = line.hasOption("cluster-job");
			bigJob = line.hasOption("big-job");
			longJob = line.hasOption("long-job");
			if (line.hasOption("time-limit")) {
				timeLimit = Long.parseLong(line.getOptionValue("time-limit"));
			} else {
				timeLimit = null;
			}

		} catch (ParseException exp) {
			System.out.println("Unexpected exception: " + exp.getMessage());
			System.exit(0);
		}
	}

	/**
	 * Train the model.
	 * 
	 * This method runs the estimation procedure for the model associated with
	 * this experiment class for the number of iterations specified in the
	 * command line arguments. The estimation is run in batches of
	 * {@link #batchSize} iterations and prints out intermediate results at the
	 * end of each batch.
	 * 
	 * @param rng
	 *            random number generator (optional)
	 * @return the trained model
	 * @throws IOException
	 */
	public JointTextNetworkModel estimateModel(LogRandoms rng)
			throws IOException {

		if (rng == null) {
			rng = new LogRandoms();
		}

		int numDocs;
		int numWords;

		EmailCorpus emails = new EmailCorpus(numActors);
		InstanceListLoader.load(wordMatrixFileName, vocabListFileName,
				edgeMatrixFileName, emails);

		emails.calculateCoOccurenceStatistics();
		numDocs = emails.size();
		numWords = emails.getWordDict().size();

		new File(outputDir).mkdirs();

		alpha = new double[1];
		Arrays.fill(alpha, alphaParameter * numFeatures);

		if (usingWordModel()) {
			beta = new double[1];
			Arrays.fill(beta, betaParameter * numWords);
		}

		if (usingBernoulli()) {
			gamma = new double[1];
			Arrays.fill(gamma, gammaParameter * 2);
		}

		// which file names are null determines what is printed to the output
		// folder

		if (verbose) {
			optionsFileName = outputDir + "/options." + iterOffset + ".txt";
		}

		if (verbose) {
			logProbFileName = outputDir + "/log_prob.txt";
			logLikeFileName = outputDir + "/log_like.txt";
			predictionsFileName = outputDir + "/predictions.txt";
		} else {
			logProbFileName = null;
			logLikeFileName = null;
			predictionsFileName = null;
		}

		if (sampleHypers && usingWordModel() && verbose) {
			betaFileName = outputDir + "/beta.txt";
		} else {
			betaFileName = null;
		}

		if (sampleHypers && usingBernoulli() && verbose) {
			gammaFileName = outputDir + "/gamma.txt";
		} else {
			gammaFileName = null;
		}

		if ((usingWordModel() || usingMixtureModel()) && verbose) {
			wordStateFileName = outputDir + "/word_state.txt.gz";
		} else {
			wordStateFileName = null;
		}

		if (usingWordModel() && verbose) {
			documentTopicsFileName = outputDir + "/doc_topics.txt.gz";
			topicWordsFileName = outputDir + "/topic_words.txt.gz";
			topicSummaryFileName = outputDir + "/topic_summary.txt.gz";
		} else {
			documentTopicsFileName = null;
			topicWordsFileName = null;
			topicSummaryFileName = null;
		}

		if (usingEdgeModel() && !usingMixtureModel() && verbose) {
			edgeStateFileName = outputDir + "/edge_state.txt.gz";
		} else {
			edgeStateFileName = null;
		}

		if ((usingEdgeModel() || usingWordModel()) && sampleHypers && verbose) {
			alphaFileName = outputDir + "/alpha.txt";
		} else {
			alphaFileName = null;
		}

		if (usingEdgeModel() && verbose && !usingBernoulli()) {
			latentSpaceFileName = outputDir + "/latent_spaces.txt";
			interceptFileName = outputDir + "/intercepts.txt";
		} else {
			latentSpaceFileName = null;
			interceptFileName = null;
		}

		if (optionsFileName != null) {
			PrintWriter pw = new PrintWriter(optionsFileName);
			pw.println("word matrix file = " + wordMatrixFileName);
			pw.println("vocab list file = " + vocabListFileName);
			pw.println("edge matrix file = " + edgeMatrixFileName);
			pw.println("# docs = " + numDocs);
			pw.println("# edges = " + (numActors - 1) * numDocs);
			pw.println("# words = " + numWords);
			pw.println("# features = " + numFeatures);
			pw.println("dimension of latent spaces = " + latentDim);
			pw.println("alpha = " + alphaParameter);
			if (usingWordModel()) {
				pw.println("beta = " + betaParameter);
			}
			pw.println("# iterations = " + numIterations);
			pw.println("iter offset = " + iterOffset);
			pw.println("Print interval = " + printInterval);
			pw.println("Save state interval = " + saveStateInterval);
			pw.println("Sample hypers = " + sampleHypers);
			pw.println("Date = " + (new Date()));
			for (String a : args) {
				pw.println(a);
			}
			pw.close();
		}

		EmailCorpus trueEmails = null;
		if (missingEdgeExperiment) {

			trueEmails = emails.copy();

			if (!readFromFolder) {
				if (obscureRandom) {
					double p = 0.1;
					emails.obscureRandomEdges(p, rng);
				} else {
					emails.obscureContinuousTestSet(startTest, endTest);
				}
			}
			if (verbose) {
				missingEdgeFileName = outputDir + "/missing-edges.txt";
			}
		}

		JointTextNetworkModel model = new JointTextNetworkModel(emails,
				numFeatures, latentDim, rng, usingWordModel(),
				usingEdgeModel(), true, usingBernoulli(), usingErosheva(),
				usingExchangeableJointStructure(),
				usingMarginalizedAssignments(), usingMixtureModel(),
				usingAsymmetric(), sampleEdgeModelOnly, sampleLatentSpacesOnly,
				timeLimit);

		String loadedWordStateFileName = null;
		String loadedEdgeStateFileName = null;
		String loadedLatentSpaceFileName = null;
		String loadedInterceptFileName = null;
		String loadedAlphaFileName = null;
		String loadedBetaFileName = null;
		String loadedGammaFileName = null;
		String loadedMissingEdgeFileName = null;

		if (readFromFolder) {

			if (usingWordModel() || usingMixtureModel()) {
				loadedWordStateFileName = readFolder + "/word_state.txt.gz."
						+ iterOffset;
			}

			if (usingEdgeModel() && !usingMixtureModel()) {
				loadedEdgeStateFileName = readFolder + "/edge_state.txt.gz."
						+ iterOffset;
			}

			if (usingEdgeModel() && !usingBernoulli()) {
				loadedLatentSpaceFileName = readFolder + "/latent_spaces.txt."
						+ iterOffset;
				loadedInterceptFileName = readFolder + "/intercepts.txt."
						+ iterOffset;
			}

			if (sampleHypers) {

				loadedAlphaFileName = readFolder + "/alpha.txt." + iterOffset;

				if (usingWordModel()) {
					loadedBetaFileName = readFolder + "/beta.txt." + iterOffset;
				}
				if (usingEdgeModel() && usingBernoulli()) {
					loadedGammaFileName = readFolder + "/gamma.txt."
							+ iterOffset;
				}
			}

			if (missingEdgeExperiment) {
				loadedMissingEdgeFileName = readFolder + "/missing-edges.txt."
						+ iterOffset;
			}
		} else if (sampleEdgeModelOnly || sampleLatentSpacesOnly) {

			if (usingWordModel() || usingMixtureModel()) {
				loadedWordStateFileName = disjointReadFolder
						+ "/word_state.txt.gz." + disjointIterOffset;
			}

			if (usingEdgeModel() && !usingMixtureModel()
					&& !sampleEdgeModelOnly) {
				loadedEdgeStateFileName = disjointReadFolder
						+ "/edge_state.txt.gz." + disjointIterOffset;
			}

			if (sampleHypers) {

				System.out.println("WARNING: HYPERPARAMETER SAMPLING IN"
						+ " DISJOINT MODELS IS UNTESTED CODE");

				loadedAlphaFileName = disjointReadFolder + "/alpha.txt."
						+ disjointIterOffset;

				if (usingWordModel()) {
					loadedBetaFileName = disjointReadFolder + "/beta.txt."
							+ disjointIterOffset;
				}
			}

			// leaving the other loaded___FileName's null will force the model
			// to sample those values: the latent spaces and possibly the edge
			// assignments. gamma will be set to the initial value specified on
			// the command line
		}

		model.initialize(null, null, null, null, alpha, beta, gamma, true,
				printInterval, saveStateInterval, documentTopicsFileName,
				wordStateFileName, edgeStateFileName, alphaFileName,
				betaFileName, gammaFileName, latentSpaceFileName,
				interceptFileName, topicWordsFileName, topicSummaryFileName,
				logProbFileName, logLikeFileName, missingEdgeFileName,
				iterOffset, loadedWordStateFileName, loadedEdgeStateFileName,
				loadedLatentSpaceFileName, loadedInterceptFileName,
				loadedAlphaFileName, loadedBetaFileName, loadedGammaFileName,
				loadedMissingEdgeFileName);

		int iters = 0;
		for (int batch = 0; batch <= numIterations; batch += batchSize) {

			int itersScheduled = Math.min(numIterations - batch, batchSize);
			int itersDone = model.estimate(itersScheduled, sampleHypers);
			iters += itersDone;

			if (itersDone < itersScheduled) {
				// TODO this won't work for hierarchical prior
				alphaParameter = model.getAlpha()[0] / numFeatures;
				if (usingWordModel()) {
					betaParameter = model.getBeta()[0] / numWords;
				}
				if (usingBernoulli()) {
					gammaParameter = model.getGamma()[0] / 2;
				}
				numIterations -= iters;
				iterOffset += iters;

				if (missingEdgeExperiment) {
					printEdgeConfusion(trueEmails, emails, verbose,
							predictionsFileName);
					printHeldOutEdgeLike(trueEmails, model);
				}
				rerunJob();
				return model;
			}
			if (missingEdgeExperiment) {
				printEdgeConfusion(trueEmails, emails, verbose,
						predictionsFileName);
				printHeldOutEdgeLike(trueEmails, model);
			}
		}

		return model;
	}

	/**
	 * Restart a job that was submitted to a linux cluster.
	 * 
	 * This code has not been tested on any platforms besides the cluster at
	 * UMass's Department of Computer Science.
	 * 
	 * @throws IOException
	 */
	private void rerunJob() throws IOException {
		String command = "";
		if (clusterJob) {
			command += "ssh swarm qsub -cwd -o " + outputDir + "/stdout."
					+ iterOffset + ".txt -e " + outputDir + "/stderr."
					+ iterOffset + ".txt";
			command += bigJob ? " -l mem_free=2G -l mem_token=2G"
					: " -l mem_free=1G -l mem_token=1G";
			command += longJob ? " -l long=TRUE" : "";
			command += bigJob ? " " + sourceDir + "/scripts/run-big-job.sh "
					: " " + sourceDir + "/scripts/run-job.sh ";
		} else {
			command += "java";
			command += " -Xmx";
			command += bigJob ? "2G" : "1G";
			command += " -cp build/jar/NetworkModels.jar ";
		}
		command += getMainCommand();
		System.out.println();
		System.out.println(command);
		Runtime.getRuntime().exec(command);
	}

	/**
	 * Get the arguments for resubmitting a job.
	 * 
	 * This method is used by {@link rerunJob}. The method is designed to copy
	 * all of the original parameters of the last job.
	 * 
	 * @return the parameters passed to the java command
	 */
	public String getMainCommand() {

		String command = this.getClass().getCanonicalName();

		CommandLineParser parser = new GnuParser();

		try {
			CommandLine line = parser.parse(options, args);

			for (Option o : line.getOptions()) {
				String name = o.getOpt();
				if (name != numIterOpt && name != iterOffsetOpt
						&& name != alphaOpt && name != betaOpt
						&& name != gammaOpt && name != readFolderOpt
						&& name != readFromFolderOpt) {
					if (o.hasArg()) {
						command += " --" + name + "=" + o.getValue();
					} else {
						command += " --" + name;
					}
				}
			}

			command += " --alpha=" + alphaParameter;
			if (line.hasOption("beta")) {
				command += " --beta=" + betaParameter;
			}
			if (line.hasOption("gamma")) {
				command += " --gamma=" + gammaParameter;
			}
			command += " --num-iter=" + numIterations;

			command += " --read-from-folder";
			command += " --read-folder=" + outputDir;
			command += " --iter-offset=" + iterOffset;

		} catch (ParseException exp) {
			System.out.println("Unexpected exception: " + exp.getMessage());
		}
		return command;

	}

	/**
	 * 
	 * @return a boolean value indicating whether the mixture model represented
	 *         by this experiment class includes the edge assignment variables
	 *         in the sampling scheme or marginalizes them out
	 */
	public abstract boolean usingMarginalizedAssignments();

	/**
	 * 
	 * @return a boolean taking the value true if the edge model is exchangeable
	 *         with the word model or only one of the edge or word models are
	 *         being used in an admixture model.
	 */
	public abstract boolean usingExchangeableJointStructure();

	/**
	 * 
	 * @return a boolean taking the value true if the edge model uses the
	 *         admixture components described by Erosheva et al. (2004)
	 */
	public abstract boolean usingErosheva();

	/**
	 * 
	 * @return a boolean indicating whether the model includes a component for
	 *         describing the edge data
	 */
	public abstract boolean usingEdgeModel();

	/**
	 * 
	 * @return a boolean taking the value true if the edge model uses the
	 *         bernoulli admixture components described by Krafft et al. (2012)
	 */
	public abstract boolean usingBernoulli();

	/**
	 * 
	 * @return a boolean indicating whether the model includes a component for
	 *         describing the word data
	 */
	public abstract boolean usingWordModel();

	/**
	 * 
	 * @return a boolean indicating whether a mixture model should be used
	 *         rather than an admixture model
	 */
	public abstract boolean usingMixtureModel();

	/**
	 * 
	 * @return a boolean indicating whether the edge model used should be
	 *         asymmetric
	 */
	public abstract boolean usingAsymmetric();

	/**
	 * Calculate and print the F-score for held-out edges.
	 * 
	 * This method also prints the true positives, false positives, true
	 * negatives, and false negatives.
	 * 
	 * @param trueEmails
	 *            the original email dataset
	 * @param testEmails
	 *            the EmailCorpus with held-out edges filled in by a model
	 * @param verbose
	 *            whether to print the edge predictions to a file
	 * @param predictionsFileName
	 *            the name of the file to print to, if any
	 * @throws FileNotFoundException
	 */
	public static void printEdgeConfusion(EmailCorpus trueEmails,
			EmailCorpus testEmails, boolean verbose, String predictionsFileName)
			throws FileNotFoundException {

		PrintWriter pw = null;
		if (verbose) {
			pw = new PrintWriter(predictionsFileName);
		}

		int truePositives = 0;
		int falsePositives = 0;
		int trueNegatives = 0;
		int falseNegatives = 0;

		for (int d = 0; d < testEmails.size(); d++) {

			for (int j : testEmails.getEmail(d).getMissingData()) {

				int r = testEmails.getEmail(d).getRecipient(j);
				int actual = trueEmails.getEmail(d).getEdge(r);

				int predicted = testEmails.getEmail(d).getEdge(r);

				if (verbose) {
					pw.println(d + "," + j + "," + r + "," + actual + ","
							+ predicted + ","
							+ testEmails.getEmail(d).getSource());
				}

				if (actual == 1) {

					if (predicted == 1) {
						truePositives++;

					} else {
						falseNegatives++;
					}
				} else {

					if (predicted == 0) {
						trueNegatives++;
					} else {
						falsePositives++;
					}
				}
			}
		}

		if (verbose) {
			pw.close();
		}

		System.out.println("True Positives: " + truePositives);
		System.out.println("False Positives: " + falsePositives);
		System.out.println("True Negatives: " + trueNegatives);
		System.out.println("False Negatives: " + falseNegatives);

		double precision = (double) truePositives
				/ (truePositives + falsePositives);
		double recall = (double) truePositives
				/ (truePositives + falseNegatives);
		double fscore = 2 * precision * recall / (precision + recall);

		System.out.println("F-Score: " + fscore);
	}

	/**
	 * Print held-out edge log likelihood.
	 * 
	 * This method calcuates the held-out edge log likelihood as the sum of the
	 * log likelihoods of individual held-out emails.
	 * 
	 * @param trueEmails
	 *            the original email dataset
	 * @param model
	 *            a trained model with filled-in edges
	 */
	public static void printHeldOutEdgeLike(EmailCorpus trueEmails, Model model) {

		double logLike = 0;

		EmailCorpus testEmails = model.getEmails();
		model.setEmails(trueEmails);

		for (int d = 0; d < testEmails.size(); d++) {

			if (testEmails.getDocument(d).getObscured()) {
				logLike += model.getDocEdgeLogLike(d);
			}
		}

		System.out.println("Held-out edge log likelihood: " + logLike);

		model.setEmails(testEmails);
	}
}
