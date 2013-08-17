package mixedmembership;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import util.Timer;
import cc.mallet.types.Alphabet;
import data.EmailCorpus;

/**
 * This class consolidates functionality for printing out information about the
 * estimation (such as log like, number of iterations complete, running time,
 * etc.) and for saving state at regular intervals.
 * 
 * What files are printed depends on what file names are passed in as null into
 * the constructor.
 */
public class MCMCPrinter {

	private EmailCorpus emails;

	private Alphabet wordDict;
	private int[][] wordAssignments;
	private WordScore wordScore;
	private EdgeScore edgeScore;
	private int[][] edgeAssignments;
	private AssignmentScore assignmentScore;
	private double[] intercepts;
	private double[][][] latentSpaces;

	private PrintWriter logLikeWriter;
	private PrintWriter logProbWriter;

	private int printInterval;
	private int saveStateInterval;

	private String documentTopicsFileName;
	private String wordStateFileName;
	private String edgeStateFileName;
	private String alphaFileName;
	private String betaFileName;
	private String gammaFileName;
	private String latentSpaceFileName;
	private String interceptFileName;
	private String missingEdgeFileName;
	private String topicWordsFileName;
	private String topicSummaryFileName;

	private long start;

	private int positionAccepts;
	private int positionProposals;

	private int interceptAccepts;
	private int interceptProposals;

	public MCMCPrinter(EmailCorpus emails, JointStructure assignmentModel,
			EdgeScore edgeScore, int printInterval, int saveStateInterval,
			String documentTopicsFileName, String wordStateFileName,
			String edgeStateFileName, String alphaFileName,
			String betaFileName, String gammaFileName,
			String latentSpaceFileName, String interceptFileName,
			String missingEdgeFileName, String topicWordsFileName,
			String topicSummaryFileName, String logProbFileName,
			String logLikeFileName) {

		this.emails = emails;

		this.printInterval = printInterval;
		this.saveStateInterval = saveStateInterval;
		this.documentTopicsFileName = documentTopicsFileName;
		this.wordStateFileName = wordStateFileName;
		this.edgeStateFileName = edgeStateFileName;
		this.alphaFileName = alphaFileName;
		this.betaFileName = betaFileName;
		this.gammaFileName = gammaFileName;
		this.latentSpaceFileName = latentSpaceFileName;
		this.interceptFileName = interceptFileName;
		this.missingEdgeFileName = missingEdgeFileName;
		this.topicWordsFileName = topicWordsFileName;
		this.topicSummaryFileName = topicSummaryFileName;

		wordDict = emails.getWordDict();

		setModelObjects(assignmentModel, edgeScore);

		positionAccepts = 0;
		positionProposals = 0;

		logProbWriter = null;
		if (printInterval != 0 && logProbFileName != null) {
			try {
				logProbWriter = new PrintWriter(new FileWriter(logProbFileName,
						true));
			} catch (IOException e) {
				System.out.println(e);
			}
		}

		logLikeWriter = null;
		if (printInterval != 0 && logProbFileName != null) {
			try {
				logLikeWriter = new PrintWriter(new FileWriter(logLikeFileName,
						true));
			} catch (IOException e) {
				System.out.println(e);
			}
		}

		start = System.currentTimeMillis();
	}

	public void setModelObjects(JointStructure assignmentModel,
			EdgeScore edgeScore) {

		assignmentScore = assignmentModel.getAssignmentScore();

		wordAssignments = assignmentModel.getWordAssignments();
		wordScore = assignmentModel.getWordScore();

		this.edgeScore = edgeScore;

		edgeAssignments = assignmentModel.getEdgeAssignments();
		if (edgeScore instanceof EdgeScoreLatentSpace) {
			latentSpaces = ((EdgeScoreLatentSpace) edgeScore).getLatentSpaces();
			intercepts = ((EdgeScoreLatentSpace) edgeScore).getIntercepts();
		}
	}

	public void printInitial(int A, int D, int W, int T, int K, double logProb,
			double logLike) {
		System.out.println("Num authors: " + A);
		System.out.println("Num docs: " + D);
		System.out.println("Num words in vocab: " + W);

		int N = 0;
		for (int d = 0; d < D; d++) {
			N += emails.getEmail(d).getLength();
		}

		System.out.println("Num tokens: " + N);
		if (A > 0) {
			System.out.println("Num edges: " + D * (A - 1));
		}
		System.out.println("Num topics: " + T);
		System.out.println("Latent space dimension: " + K);

		if (logProbWriter != null) {
			logProbWriter.println("0," + logProb);
			logProbWriter.flush();
		}
		if (logLikeWriter != null) {
			logLikeWriter.println("0," + logLike);
			logLikeWriter.flush();
		}
	}

	public void printIteration(int iter, double logProb, double logLike,
			boolean saveState) {

		System.out.flush();

		if (printInterval != 0) {
			if (iter % printInterval == 0) {

				System.out.print(".");

				if (logProbWriter != null) {
					logProbWriter.println(iter + "," + logProb);
					logProbWriter.flush();
				}

				if (logProbWriter != null) {
					logLikeWriter.println(iter + "," + logLike);
					logLikeWriter.flush();
				}
			}
		}

		if (((saveStateInterval != 0) && (iter % saveStateInterval == 0))
				|| saveState) {
			if (wordStateFileName != null) {
				emails.printWordFeatures(wordAssignments, wordStateFileName
						+ "." + iter);
			}
			if (edgeStateFileName != null) {
				emails.printEdgeFeatures(edgeAssignments, edgeStateFileName
						+ "." + iter);
			}
			if (latentSpaceFileName != null) {
				printLatentSpaces(latentSpaceFileName + "." + iter);
			}
			if (interceptFileName != null) {
				printIntercepts(interceptFileName + "." + iter);
			}
			if (missingEdgeFileName != null) {
				emails.printMissingEdges(missingEdgeFileName + "." + iter);
			}
			if (alphaFileName != null) {
				assignmentScore.printAlpha(alphaFileName + "." + iter);
			}
			if (betaFileName != null) {
				wordScore.printAlpha(betaFileName + "." + iter);
			}
			if (gammaFileName != null) {
				((EdgeScoreBernoulli) edgeScore).printGamma(gammaFileName + "."
						+ iter);
			}
			if (topicSummaryFileName != null) {
				wordScore.print(wordDict, 0.0, 10, true, topicSummaryFileName
						+ "." + iter, null);
			}
			if (saveState && wordScore != null
					&& emails.getCoOccurenceStatistics() != null) {
				wordScore.printCoherence(10, emails);
			}
		}
	}

	public void printFinal() {
		System.out.println();
		System.out.println("Position mcmc accept proportion: "
				+ positionAccepts / (double) positionProposals);
		System.out.println("Intercept mcmc accept proportion: "
				+ interceptAccepts / (double) interceptProposals);

		Timer.printTimingInfo(start, System.currentTimeMillis());

		if (wordStateFileName != null) {
			emails.printWordFeatures(wordAssignments, wordStateFileName);
		}
		if (edgeStateFileName != null) {
			emails.printEdgeFeatures(edgeAssignments, edgeStateFileName);
		}
		if (latentSpaceFileName != null) {
			printLatentSpaces(latentSpaceFileName);
		}
		if (interceptFileName != null) {
			printIntercepts(interceptFileName);
		}
		if (missingEdgeFileName != null) {
			emails.printMissingEdges(missingEdgeFileName);
		}
		if (alphaFileName != null) {
			assignmentScore.printAlpha(alphaFileName);
		}
		if (betaFileName != null) {
			wordScore.printAlpha(betaFileName);
		}
		if (gammaFileName != null) {
			((EdgeScoreBernoulli) edgeScore).printGamma(gammaFileName);
		}
		if (documentTopicsFileName != null) {
			assignmentScore.print(emails, documentTopicsFileName);
		}
		if (topicWordsFileName != null) {
			wordScore.print(wordDict, topicWordsFileName, emails);
		}
		if (topicSummaryFileName != null) {
			wordScore.print(wordDict, 0.0, 10, true, topicSummaryFileName,
					emails);
		}
		if (wordScore != null && emails.getCoOccurenceStatistics() != null) {
			wordScore.printCoherence(10, emails);
		}
	}

	public void positionProposal(int accepted) {
		positionProposals++;
		positionAccepts += accepted;
	}

	public void interceptProposal(int accepted) {
		interceptProposals++;
		interceptAccepts += accepted;
	}

	public void printIntercepts(String fileName) {
		try {
			PrintWriter pw = new PrintWriter(fileName);
			for (int t = 0; t < intercepts.length; t++) {
				pw.println(intercepts[t]);
			}
			pw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	public void printLatentSpaces(String fileName) {
		try {
			PrintWriter pw = new PrintWriter(fileName);
			for (int t = 0; t < latentSpaces.length; t++) {
				String space = "";
				if (latentSpaces[t][0].length > 0) {
					int a;
					int k;
					for (a = 0; a < latentSpaces[t].length - 1; a++) {
						for (k = 0; k < latentSpaces[t][a].length; k++) {
							space += latentSpaces[t][a][k] + ",";
						}
					}
					for (k = 0; k < latentSpaces[t][a].length - 1; k++) {
						space += latentSpaces[t][a][k] + ",";
					}
					space += latentSpaces[t][a][k] + "\n";
				}
				pw.print(space);
			}
			pw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}
