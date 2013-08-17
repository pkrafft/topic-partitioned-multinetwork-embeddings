package experiments;

import java.util.Arrays;
import java.util.HashSet;

import util.LogRandoms;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Dirichlet;
import data.EmailCorpus;

/**
 * A class for generating purely synthetic data according to LDA's generative
 * process. The number of documents in the synthetic corpus, the number of
 * tokens in each document, and the number of types in the vocabulary are all
 * distributed uniformly with upper bounds specified in the command line
 * arguments.
 * 
 * This class is useful for unit testing slice sampling. It generates all the
 * LDA parameters given your input hyperparameters, then generates data from
 * that LDA model. It writes the files to disk, and you can use those files
 * as input into the regular LDAExperiment class with the -h to check that
 * the hyperparameters are correctly inferred.
 */
public class LDASyntheticData {

	public static void main(String[] args) throws java.io.IOException {

		if (args.length != 7) {
			System.out
					.println("Usage: <output_dir> <alpha> <beta> <num_topics> "
							+ "<max_vocab_size> <max_n_docs> <max_doc_length>");
			System.exit(1);
		}

		int ind = 0;
		String outputDir = args[ind++];
		double alpha = Double.parseDouble(args[ind++]);
		double beta = Double.parseDouble(args[ind++]);
		int numTopics = Integer.parseInt(args[ind++]);
		int maxVocabSize = Integer.parseInt(args[ind++]);
		int maxNumDocs = Integer.parseInt(args[ind++]);
		int maxDocLength = Integer.parseInt(args[ind++]);

		LogRandoms rng = new LogRandoms();
		EmailCorpus emails = new EmailCorpus(1, maxVocabSize, maxNumDocs,
				maxDocLength, rng);

		double[][] phi = new double[numTopics][emails.vocabSize()];
		double[][] theta = new double[emails.size()][];

		double[] par = new double[emails.vocabSize()];
		Arrays.fill(par, beta);
		Dirichlet phiPrior = new Dirichlet(par);

		par = new double[numTopics];
		Arrays.fill(par, alpha);
		Dirichlet thetaPrior = new Dirichlet(par);

		for (int d = 0; d < emails.size(); d++) {
			theta[d] = thetaPrior.nextDistribution();
		}

		for (int t = 0; t < numTopics; t++) {
			phi[t] = phiPrior.nextDistribution();
		}

		HashSet<Integer> vocab = new HashSet<Integer>(maxVocabSize);

		for (int d = 0; d < emails.size(); d++) {

			int[] tokens = emails.getDocument(d).getTokens();

			for (int i = 0; i < tokens.length; i++) {

				int t = rng.nextDiscrete(theta[d]);

				tokens[i] = rng.nextDiscrete(phi[t]);

				vocab.add(tokens[i]);
			}
		}

		Alphabet wordDict = new Alphabet();
		for (int i : vocab) {
			wordDict.lookupIndex(i + "");
		}
		emails.setWordDict(wordDict);

		for (int i = 0; i < emails.size(); i++) {

			int[] tokens = new int[emails.getDocument(i).getLength()];

			for (int j = 0; j < tokens.length; j++) {
				tokens[j] = wordDict.lookupIndex(emails.getDocument(i)
						.getToken(j) + "");
			}

			emails.getDocument(i).setTokens(tokens);
		}

		emails.writeToFiles(outputDir + "/word-matrix.csv", outputDir
				+ "/edge-matrix.csv", outputDir + "/vocab.txt");
	}

}
