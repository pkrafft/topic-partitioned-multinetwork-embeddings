package mixedmembership;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import util.Probability;
import cc.mallet.types.Alphabet;
import data.EmailCorpus;

/**
 * A class for manipulating and printing the topics from LDA.
 */
public class WordScore extends DirichletMultinomialScore {

	public WordScore(int numElements, int numComponents, double[] alpha,
			JointStructure modelStructure) {
		super(numComponents, numElements, alpha, modelStructure);
	}

	public void print(Alphabet dict, String fileName, EmailCorpus emails) {

		print(dict, 0.0, -1, false, fileName, emails);
	}

	public void print(Alphabet dict, double threshold, int numWordsToPrint,
			boolean summary, String fileName, EmailCorpus emails) {

		assert dict.size() == numElements;

		try {

			PrintStream pw = null;

			if (fileName == null)
				pw = new PrintStream(System.out, true);
			else {

				pw = new PrintStream(new GZIPOutputStream(
						new BufferedOutputStream(new FileOutputStream(new File(
								fileName)))));

				if (!summary)
					pw.println("#topic typeindex type proportion");
			}

			Probability[] probs = new Probability[numElements];

			for (int t = 0; t < numComponents; t++) {

				probs = getTopic(t);

				if ((numWordsToPrint > numElements) || (numWordsToPrint < 0)) {
					numWordsToPrint = numElements;
				}

				StringBuffer line = new StringBuffer();

				for (int i = 0; i < numWordsToPrint; i++) {

					if ((probs[i].prob == 0) || (probs[i].prob < threshold)) {
						break;
					}

					if ((fileName == null) || summary) {
						line.append(dict.lookupObject(probs[i].index));
						line.append(" ");
					} else {
						pw.print(t);
						pw.print(" ");
						pw.print(probs[i].index);
						pw.print(" ");
						pw.print(dict.lookupObject(probs[i].index));
						pw.print(" ");
						pw.print(probs[i].prob);
						pw.println();
					}
				}

				String string = line.toString();

				if ((fileName == null) || summary) {

					if (emails != null) {

						double coherence = getTopicCoherence(probs,
								numWordsToPrint, emails);

						if (!string.equals("")) {
							pw.println(coherence + " Topic " + t + ": "
									+ string);
						}
					} else {
						pw.println("Topic " + t + ": " + string);
					}
				}
			}

			if (fileName != null)
				pw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	public double printCoherence(int numWordsToPrint, EmailCorpus emails) {

		Probability[] probs = new Probability[numElements];
		double averageCoherence = 0;

		for (int t = 0; t < numComponents; t++) {

			probs = getTopic(t);

			double coherence = getTopicCoherence(probs, numWordsToPrint, emails);
			averageCoherence += coherence;

		}

		averageCoherence /= numComponents;
		System.out.println("\nAverage coherence: " + averageCoherence + "\n");
		return averageCoherence;
	}

	/**
	 * Calculate the topic coherence of a particular topic.
	 * 
	 * Topic coherence is described by Mimno et al. (2011).
	 * 
	 * @param topic
	 *            the probabilities of each word in the topic
	 * @param numTopWords
	 *            the number of words to consider in computing the coherence
	 * @param emails
	 *            the data
	 * @return the coherence of the given topic
	 */
	public double getTopicCoherence(Probability[] topic, int numTopWords,
			EmailCorpus emails) {

		double coherence = 0;

		for (int m = 1; m < Math.min(emails.vocabSize(), numTopWords); m++) {
			for (int l = 0; l < m; l++) {
				coherence += emails.getCoOccurenceStatistic(topic[m].index,
						topic[l].index);
			}
		}

		return coherence;
	}

	/**
	 * @param feature
	 *            an index of a topic
	 * @return the vector of probabilities defining a topic.
	 */
	public Probability[] getTopic(int feature) {

		Probability[] probs = new Probability[numElements];

		for (int w = 0; w < numElements; w++) {
			probs[w] = new Probability(w, Math.exp(getLogScore(feature, w)));
		}

		Arrays.sort(probs);

		return probs;
	}

	public void setTopic(int t, int[] wordCounts) {
		for (int i = 0; i < wordCounts.length; i++) {
			componentElementCounts[t][i] = wordCounts[i];
			componentCountsNorm[t] = wordCounts[i];
		}
	}

	@Override
	public double logProb() {
		return modelStructure.logWordDataProb();
	}
}
