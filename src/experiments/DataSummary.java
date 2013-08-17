package experiments;

import java.io.File;

import data.EmailCorpus;
import data.InstanceListLoader;

/**
 * Print various email network statistics to files.
 */
public class DataSummary {

	public static void main(String[] args) {

		if (args.length != 5) {
			System.out
					.println("Usage: <word_matrix> <vocab_list> <edge_matrix> "
							+ "<num_authors> <output_dir>");
			System.exit(1);
		}

		int index = 0;

		String wordMatrixFileName = args[index++];
		String vocabListFileName = args[index++];
		String edgeMatrixFileName = args[index++];

		int numActors = Integer.parseInt(args[index++]);

		String outputDir = args[index++];
		new File(outputDir).mkdirs();

		assert index == 5;

		EmailCorpus emails = new EmailCorpus(numActors);
		InstanceListLoader.load(wordMatrixFileName, vocabListFileName,
				edgeMatrixFileName, emails);
		
		emails.getCoarseNetwork().printNetwork(outputDir, true);
	}
}
