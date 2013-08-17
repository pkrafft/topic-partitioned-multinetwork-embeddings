package data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import cc.mallet.types.Alphabet;

/**
 * A class for reading data sets and state files
 */
public class InstanceListLoader {

	/**
	 * Load a network file only
	 * 
	 * @param inputFile
	 *            edge matrix file name
	 * @param emails
	 *            a shell for the email corpus
	 */
	public static void loadNetwork(String inputFile, EmailCorpus emails) {
		load(null, null, inputFile, emails);
	}

	/**
	 * Load the word model state file
	 * 
	 * @param wordStateFile
	 *            word state file name
	 * @param numDocs
	 *            number of documents in the corpus
	 * @return the loaded token assignment object
	 */
	public static int[][] loadWordAssignments(String wordStateFile, int numDocs) {

		int[][] wordAssignmentsArray = new int[numDocs][];

		Scanner scan = null;

		try {

			ArrayList<ArrayList<Integer>> wordAssignments = new ArrayList<ArrayList<Integer>>(
					numDocs);

			for (int d = 0; d < numDocs; d++) {
				wordAssignments.add(new ArrayList<Integer>());
			}

			scan = new Scanner(new GZIPInputStream(new BufferedInputStream(
					new FileInputStream(new File(wordStateFile)))));
			String line;

			int d, z;
			String[] values;

			scan.nextLine();
			while (scan.hasNextLine()) {
				if (scan != null) {

					line = scan.nextLine();
					values = line.split(",");

					d = Integer.parseInt(values[0]);
					z = Integer.parseInt(values[5]);

					wordAssignments.get(d).add(z);
				}
			}

			for (d = 0; d < numDocs; d++) {
				wordAssignmentsArray[d] = new int[wordAssignments.get(d).size()];
				for (int i = 0; i < wordAssignments.get(d).size(); i++) {
					wordAssignmentsArray[d][i] = wordAssignments.get(d).get(i);
				}
			}

			return wordAssignmentsArray;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return wordAssignmentsArray;
	}

	/**
	 * Load the edge model state file
	 * 
	 * @param edgeStateFile
	 *            edge state file name
	 * @param numDocs
	 *            number of documents in the corpus
	 * @param numActors
	 *            number of actors in the email network
	 * @return the loaded edge assignment object
	 */
	public static int[][] loadEdgeAssignments(String edgeStateFile,
			int numDocs, int numActors) {

		int[][] edgeAssignments = new int[numDocs][numActors - 1];

		Scanner scan = null;

		try {

			scan = new Scanner(new GZIPInputStream(new BufferedInputStream(
					new FileInputStream(new File(edgeStateFile)))));
			String line;

			int d, i, z;
			String[] values;

			scan.nextLine();
			while (scan.hasNextLine()) {
				if (scan != null) {

					line = scan.nextLine();
					values = line.split(",");

					d = Integer.parseInt(values[0]);
					i = Integer.parseInt(values[2]);
					z = Integer.parseInt(values[6]);

					edgeAssignments[d][i] = z;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return edgeAssignments;
	}

	/**
	 * Load a set of latent spaces from file
	 * 
	 * @param latentSpacesFileName
	 *            location of the file
	 * @param numLatentDims
	 *            dimension of the latent spaces
	 * @param numActors
	 *            number of actors in the email network
	 * @param numFeatures
	 *            number of spaces
	 * @return the loaded latent spaces object
	 */
	public static double[][][] loadLatentSpaces(String latentSpacesFileName,
			int numLatentDims, int numActors, int numFeatures) {

		double[][][] latentSpaces = new double[numFeatures][numActors][numLatentDims];

		Scanner scan = null;

		try {

			scan = new Scanner(new File(latentSpacesFileName));
			String line;

			String[] values;

			int t = 0;
			while (scan.hasNextLine()) {

				line = scan.nextLine();
				values = line.split(",");

				int i = 0;
				for (int a = 0; a < numActors; a++) {
					for (int k = 0; k < numLatentDims; k++) {
						latentSpaces[t][a][k] = Double.parseDouble(values[i]);
						i++;
					}
				}
				t++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return latentSpaces;
	}

	/**
	 * Load the intercepts for a model's latent spaces from file
	 * 
	 * @param interceptsFileName
	 *            the location of the file
	 * @param numFeatures
	 *            the number of spaces
	 * @return the loaded intercept object
	 */
	public static double[] loadIntercepts(String interceptsFileName,
			int numFeatures) {

		double[] intercepts = new double[numFeatures];

		Scanner scan = null;

		try {

			scan = new Scanner(new File(interceptsFileName));
			String line;

			String values;

			int t = 0;
			while (scan.hasNextLine()) {

				line = scan.nextLine();
				values = line.trim();
				intercepts[t] = Double.parseDouble(values);
				t++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return intercepts;
	}

	/**
	 * Load the state of the missing edges of a particular corpus from file.
	 * 
	 * This method directly fills in the edges of a pre-existing email corpus
	 * object, so nothing is returned.
	 * 
	 * @param emails
	 *            a model's email corpus representation
	 * @param predictionsFile
	 *            the location of the state file
	 */
	public static void loadPredictions(EmailCorpus emails,
			String predictionsFile) {

		Scanner scan = null;

		try {

			scan = new Scanner(new File(predictionsFile));
			String line;

			int d, r, y;
			Email e;
			String[] values;

			while (scan.hasNextLine()) {

				line = scan.nextLine();
				values = line.split(",");

				d = Integer.parseInt(values[0]);
				e = emails.getEmail(d);
				if (!e.getObscured()) {
					e.obscure();
				}

				r = Integer.parseInt(values[1]);
				y = Integer.parseInt(values[2]);
				e.setEdge(r, y);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Load an email data set from three data files.
	 * 
	 * This method fills a pre-existing email object, so nothing is returned.
	 * 
	 * Further details of the data format are given in this package's README.
	 * 
	 * @param wordInputFile
	 *            the location of a matrix of word counts
	 * @param vocabFile
	 *            the location of a vocabulary file
	 * @param edgeInputFile
	 *            the location of a matrix of email recipients
	 * @param emails
	 *            an email object to fill
	 */
	public static void load(String wordInputFile, String vocabFile,
			String edgeInputFile, EmailCorpus emails) {

		Scanner wordScan = null;
		Scanner vocabScan = null;
		Scanner edgeScan = null;
		try {
			if (wordInputFile != null) {
				wordScan = new Scanner(new File(wordInputFile));
				vocabScan = new Scanner(new File(vocabFile));
				Alphabet wordDict = emails.getWordDict();
				while (vocabScan.hasNext()) {
					wordDict.lookupIndex(vocabScan.nextLine());
				}
			}
			if (edgeInputFile != null) {
				edgeScan = new Scanner(new File(edgeInputFile));
			}
			String wordLine;
			String edgeLine;
			String[] wordValues;
			String[] edgeValues;
			String source;
			int author;
			int[] words;
			int[] edges;
			boolean moreLinesToGo = false;
			if (wordScan != null) {
				moreLinesToGo |= wordScan.hasNextLine();
			}
			if (edgeScan != null) {
				moreLinesToGo |= edgeScan.hasNextLine();
			}
			while (moreLinesToGo) {

				source = null;

				words = new int[0];
				if (wordScan != null) {
					wordLine = wordScan.nextLine();
					wordValues = wordLine.split(",");
					source = wordValues[0];
					LinkedList<Integer> docWords = new LinkedList<Integer>();
					for (int w = 1; w < wordValues.length; w++) {
						String n = wordValues[w];
						for (int i = 0; i < Integer.parseInt(n); i++) {
							docWords.add(w - 1);
						}
					}
					words = new int[docWords.size()];
					int i = 0;
					for (int w : docWords) {
						words[i] = w;
						i++;
					}
					moreLinesToGo &= wordScan.hasNextLine();
				}

				author = 0;
				edges = null;
				if (edgeScan != null) {
					edgeLine = edgeScan.nextLine();
					edgeValues = edgeLine.split(",");
					if (wordScan != null) {
						assert source.equals(edgeValues[0]);
					} else {
						source = edgeValues[0];
					}
					author = Integer.parseInt(edgeValues[1]);
					edges = new int[emails.getNumAuthors()];
					for (int i = 0; i < emails.getNumAuthors(); i++) {
						edges[i] = Integer.parseInt(edgeValues[i + 2]);
					}
					moreLinesToGo &= edgeScan.hasNextLine();
				}

				emails.add(new Email(author, words, edges, source));
			}
			if (wordScan != null) {
				assert !wordScan.hasNextLine();
			}
			if (edgeScan != null) {
				assert !edgeScan.hasNextLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static double[] loadHyper(String hyperFileName, int dimension) {
		
		double[] hypers = new double[dimension];

		Scanner scan = null;

		try {

			scan = new Scanner(new File(hyperFileName));
			String line;

			String values;

			int t = 0;
			while (scan.hasNextLine()) {

				line = scan.nextLine();
				values = line.trim();
				hypers[t] = Double.parseDouble(values);
				t++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return hypers;
	}
}