package data;

import gnu.trove.TIntIntHashMap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.GZIPOutputStream;

import cc.mallet.types.Alphabet;

/**
 * A class for keeping track of the documents in a corpus. 
 * @param <E>
 */
public class Corpus<E extends Document> {

	protected Alphabet wordDict;
	protected TIntIntHashMap unseenCounts;
	protected ArrayList<E> documents;

	public Corpus(Alphabet wordDict, TIntIntHashMap unseenCounts) {

		this.wordDict = wordDict;

		this.unseenCounts = unseenCounts;

		this.documents = new ArrayList<E>();
	}

	public void permute() {

		Collections.shuffle(documents);
	}

	public void add(E d) {

		documents.add(d);
	}

	public int size() {

		return documents.size();
	}

	public E getDocument(int d) {

		return documents.get(d);
	}

	public ArrayList<E> getDocuments() {

		return documents;
	}

	public Alphabet getWordDict() {

		return wordDict;
	}

	public void setWordDict(Alphabet wordDict) {

		this.wordDict = wordDict;
	}

	public TIntIntHashMap getUnseenCounts() {

		return this.unseenCounts;
	}

	public int getUnseenCount(int index) {

		return this.unseenCounts.get(index);
	}

	public void printWordFeatures(int[][] z, String fileName) {

		try {

			PrintStream pw = new PrintStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(new File(fileName)))));

			pw.println("#doc,source,pos,typeindex,type,feature");

			for (int d=0; d<documents.size(); d++) {

				int[] fs = documents.get(d).getTokens();

				int nd = fs.length;

				for (int i = 0; i < z[d].length; i++) {

					int w;
					String word;
					if(nd > 0) {
						w = fs[i];
						word = wordDict.lookupObject(w).toString(); 
					} else {
						w = -1;
						word = "";
					}
					pw.print(d); pw.print(",");
					pw.print(documents.get(d).getSource()); pw.print(",");
					pw.print(i); pw.print(",");
					pw.print(w); pw.print(",");
					pw.print(word); pw.print(",");
					pw.print(z[d][i]); pw.println();
				}
			}

			pw.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}
}
