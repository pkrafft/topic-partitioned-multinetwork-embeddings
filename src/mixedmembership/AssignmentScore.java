package mixedmembership;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;

import util.Probability;
import data.EmailCorpus;

/**
 * A class for tracking the document-specific topic distributions (for an
 * admixture model) or the corpus-wide topic distribution (for a mixture model)
 * for models in which the topic proportions have been marginalized out.
 */
public class AssignmentScore extends DirichletMultinomialScore {

	public AssignmentScore(int numDocuments, int numTopics, double[] alpha,
			JointStructure modelStructure) {
		super(numDocuments, numTopics, alpha, modelStructure);
	}

	public void print(EmailCorpus emails, String fileName) {
		print(emails, 0.0, -1, fileName);
	}

	public void print(EmailCorpus emails, double threshold, int numTopics,
			String fileName) {

		try {

			PrintStream pw = new PrintStream(new GZIPOutputStream(
					new BufferedOutputStream(new FileOutputStream(new File(
							fileName)))));

			pw.println("#doc source topic proportion ...");

			Probability[] probs = new Probability[numElements];

			for (int d = 0; d < numComponents; d++) {

				pw.print(d);
				pw.print(" ");
				if (numComponents == 1) {
					pw.print("corpus");
				} else {
					pw.print(emails.getEmail(d).getSource());
				}
				pw.print(" ");

				for (int t = 0; t < numElements; t++)
					probs[t] = new Probability(t, Math.exp(getLogScore(d, t)));

				Arrays.sort(probs);

				if ((numTopics > numElements) || (numTopics < 0))
					numTopics = numElements;

				for (int i = 0; i < numTopics; i++) {

					// break if there are no more topics whose proportion is
					// greater than zero or threshold...

					if ((probs[i].prob == 0) || (probs[i].prob < threshold))
						break;

					pw.print(probs[i].index);
					pw.print(" ");
					pw.print(probs[i].prob);
					pw.print(" ");
				}

				pw.println();
			}

			pw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	@Override
	public double logProb() {
		return modelStructure.logPriorProb();
	}

}
