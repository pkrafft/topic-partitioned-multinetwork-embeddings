package data;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.GZIPOutputStream;

import util.Network;
import cc.mallet.types.Alphabet;
import cc.mallet.util.Randoms;

/**
 * A class for storing, manipulating, and summarizing a corpus of emails.
 */
public class EmailCorpus extends Corpus<Email> implements Serializable {

	private static final long serialVersionUID = 1L;

	private int numActors;

	private TIntObjectHashMap<TIntIntHashMap> coOccurenceStatistics;
	int[] documentFrequencies;

	/**
	 * Create a new email corpus object.
	 * 
	 * @param numActors
	 *            the number of actors in this email network
	 * @param documents
	 *            the individual emails in this corpus
	 * @param wordDict
	 *            the vocabulary of this corpus
	 */
	public EmailCorpus(int numActors, ArrayList<Email> documents,
			Alphabet wordDict) {
		super(wordDict, new TIntIntHashMap());
		this.numActors = numActors;
		this.documents = documents;
	}

	public EmailCorpus(int numActors, Alphabet wordDict) {
		this(numActors, new ArrayList<Email>(), wordDict);
	}

	public EmailCorpus(int numActors) {
		this(numActors, new Alphabet());
	}

	/**
	 * Generate a random email corpus.
	 * 
	 * @param maxNumActors
	 * @param maxVocabSize
	 * @param maxNumEmails
	 * @param maxDocLength
	 * @param rng
	 *            random number generator
	 */
	public EmailCorpus(int maxNumActors, int maxVocabSize, int maxNumEmails,
			int maxDocLength, Randoms rng) {

		this(rng.nextInt(maxNumActors) + 1);

		HashSet<Integer> vocab = new HashSet<Integer>(maxVocabSize);

		int numDocs = rng.nextInt(maxNumEmails) + 1;
		int[][] docs = new int[numDocs][];
		for (int i = 0; i < numDocs; i++) {

			int docLength = rng.nextInt(maxDocLength);
			docs[i] = new int[docLength];
			for (int j = 0; j < docLength; j++) {
				docs[i][j] = rng.nextInt(maxVocabSize);
				vocab.add(docs[i][j]);
			}
			Arrays.sort(docs[i]);
		}

		Alphabet wordDict = new Alphabet();
		for (int i : vocab) {
			wordDict.lookupIndex(i + "");
		}
		setWordDict(wordDict);

		for (int i = 0; i < numDocs; i++) {

			int[] tokens = new int[docs[i].length];
			for (int j = 0; j < docs[i].length; j++) {
				tokens[j] = wordDict.lookupIndex(docs[i][j] + "");
			}
			Arrays.sort(tokens);

			int author = numActors > 0 ? rng.nextInt(numActors) : 0;
			int[] edges = new int[numActors];
			for (int a = 0; a < numActors; a++) {
				if (a != author) {
					edges[a] = rng.nextInt(2);
				}
			}

			add(new Email(author, tokens, edges, null));
		}
	}

	/**
	 * Choose a random set of emails whose recipients will be held-out from the
	 * analysis.
	 * 
	 * This method is useful for link prediction experiments. Held-out emails
	 * are treated as missing data.
	 * 
	 * @param p
	 *            the probability of obscuring any particular email.
	 * @param rng
	 *            a random number generator
	 */
	public void obscureRandomEdges(double p, Randoms rng) {
		if (numActors > 1) { // if there are any recipients to obscure
			for (int d = 0; d < documents.size(); d++) {
				if (p > rng.nextDouble()) {
					documents.get(d).obscure();
				}
			}
		}
	}

	/**
	 * Hold out the edges of a specific set of emails from the analysis.
	 * 
	 * This method is useful for holding out particular stretches of time in a
	 * link prediction task. The set of emails that is obscured is the emails
	 * associated with the indices in the range [start, end).
	 * 
	 * @param start
	 *            the index of the first email to obscure
	 * @param end
	 *            the index after the last email to obscure
	 */
	public void obscureContinuousTestSet(int start, int end) {

		assert start >= 0 && end <= documents.size();

		for (int d = start; d < end; d++) {
			for (int a = 0; a < numActors; a++) {
				documents.get(d).obscureEdge(a);
			}
		}
	}

	public Email getEmail(int d) {
		return documents.get(d);
	}

	public int getNumAuthors() {
		return numActors;
	}

	/**
	 * Count the number of times each actor communicates or doesn't communicate
	 * with each other actor.
	 * 
	 * @return the A x A x 2 multinetwork representation of this email corpus.
	 */
	public Network getCoarseNetwork() {

		int[][] presentEdges = new int[numActors][numActors];
		int[][] absentEdges = new int[numActors][numActors];

		for (int d = 0; d < documents.size(); d++) {

			Email e = documents.get(d);
			int a = e.getAuthor();

			for (int j = 0; j < numActors - 1; j++) {

				if (!e.getMissingData().contains(j)) {

					int r = e.getRecipient(j);
					int y = e.getEdge(r);

					if (y == 1) {
						presentEdges[a][r]++;
					} else {
						absentEdges[a][r]++;
					}
				}
			}
		}

		return new Network(presentEdges, absentEdges);
	}

	/**
	 * Count the number of times each word type co-occurs with each other word
	 * type and the number of documents in which each word type appears.
	 * 
	 * This method is useful for calculating the coherence of topics.
	 */
	public void calculateCoOccurenceStatistics() {

		coOccurenceStatistics = new TIntObjectHashMap<TIntIntHashMap>();
		documentFrequencies = new int[wordDict.size()];

		for (Email e : documents) {

			HashSet<Integer> wordSet = new HashSet<Integer>();
			for (int w : e.tokens) {
				wordSet.add(w);
			}

			for (int w1 : wordSet) {
				if (coOccurenceStatistics.get(w1) == null) {
					coOccurenceStatistics.put(w1, new TIntIntHashMap());
				}
				TIntIntHashMap row = coOccurenceStatistics.get(w1);

				documentFrequencies[w1]++;

				for (int w2 : wordSet) {
					if (!row.containsKey(w2)) {
						row.put(w2, 1);
					} else {
						row.increment(w2);
					}
				}
			}
		}
	}

	/**
	 * Calculate the statistic used to compute topic coherence.
	 * 
	 * @param w1
	 *            a word type
	 * @param w2
	 *            another word type
	 * @return a value
	 */
	public double getCoOccurenceStatistic(int w1, int w2) {
		TIntIntHashMap row = coOccurenceStatistics.get(w1);
		if (row != null) {
			int value = row.get(w2);
			return Math.log(1 + value) - Math.log(documentFrequencies[w2]);
		} else {
			return Double.NaN;
		}
	}

	public TIntObjectHashMap<TIntIntHashMap> getCoOccurenceStatistics() {
		return coOccurenceStatistics;
	}

	public void printMissingEdges(String missingEdgeFileName) {
		PrintWriter pw;
		try {
			pw = new PrintWriter(missingEdgeFileName);
			for (int d = 0; d < size(); d++) {
				for (int j : getEmail(d).getMissingData()) {
					int r = getEmail(d).getRecipient(j);
					int y = getEmail(d).getEdge(r);
					pw.println(d + "," + r + "," + y);
				}
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	public void printEdgeFeatures(int[][] z, String fileName) {

		try {

			PrintStream pw = new PrintStream(new GZIPOutputStream(
					new BufferedOutputStream(new FileOutputStream(new File(
							fileName)))));

			pw.println("#doc,source,index,author,recipient,value,feature");

			for (int d = 0; d < documents.size(); d++) {

				for (int i = 0; i < numActors - 1; i++) {

					int r = getEmail(d).getRecipient(i);
					int y = getEmail(d).getEdge(r);

					pw.print(d);
					pw.print(",");
					pw.print(getEmail(d).getSource());
					pw.print(",");
					pw.print(i);
					pw.print(",");
					pw.print(getEmail(d).getAuthor());
					pw.print(",");
					pw.print(r);
					pw.print(",");
					pw.print(y);
					pw.print(",");
					pw.print(z[d][i]);
					pw.println();
				}
			}

			pw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	public EmailCorpus copy() {
		ArrayList<Email> documentsCopy = new ArrayList<Email>(documents.size());
		for (Email e : documents) {
			documentsCopy.add(e.copy());
		}
		return new EmailCorpus(numActors, documentsCopy, wordDict);
	}

	@Override
	public String toString() {
		String string = "";

		string += "Vocab size: " + wordDict.size() + "\n";
		string += "Num docs: " + documents.size() + "\n\n";

		string += "Vocab:";
		for (int v = 0; v < wordDict.size(); v++) {
			string += " " + wordDict.lookupObject(v);
		}
		string += "\n\n";

		int i = 0;
		for (Email e : documents) {
			string += "Doc: " + i + "\n";
			string += e + "\n\n";
			i++;
		}

		return string;
	}

	public int vocabSize() {
		return wordDict.size();
	}

	public void writeToFiles(String wordMatrixFile, String edgeMatrixFile,
			String vocabFile) {
		try {

			PrintWriter pw = new PrintWriter(new FileWriter(wordMatrixFile));
			PrintWriter pw2 = new PrintWriter(new FileWriter(edgeMatrixFile));
			PrintWriter pw3 = new PrintWriter(new FileWriter(vocabFile));

			for (int d = 0; d < documents.size(); d++) {

				Email e = documents.get(d);
				int[] words = new int[wordDict.size()];
				for (int i : e.getTokens()) {
					words[i]++;
				}

				String source = e.getSource() == null ? "" : e.getSource();
				pw.print(source);
				for (int v = 0; v < wordDict.size(); v++) {
					pw.print("," + words[v]);
				}
				pw.println();

				pw2.print(source);
				pw2.print("," + e.getAuthor());
				for (int r = 0; r < numActors; r++) {
					pw2.print("," + e.getEdge(r));
				}
				pw2.println();
			}

			for (int v = 0; v < wordDict.size(); v++) {
				pw3.println(wordDict.lookupObject(v));
			}

			pw.close();
			pw2.close();
			pw3.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	@Override
	public boolean equals(Object emails) {
		return this.toString().equals(((EmailCorpus) emails).toString());
	}
}
