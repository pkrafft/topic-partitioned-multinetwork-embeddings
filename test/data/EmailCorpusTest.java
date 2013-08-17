package data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cc.mallet.types.Alphabet;
import cc.mallet.util.Randoms;

public class EmailCorpusTest {

	@Test
	public void testCalculateCoOccurenceStatistics() {
		Alphabet wordDict = new Alphabet();
		wordDict.lookupIndex("a");
		wordDict.lookupIndex("b");
		wordDict.lookupIndex("c");
		wordDict.lookupIndex("d");
		wordDict.lookupIndex("e");

		EmailCorpus emails = new EmailCorpus(4, wordDict);
		emails.add(new Email(0, new int[] { 0, 4, 2, 2 }, new int[] { 0, 0, 0,
				1 }, null));
		emails.add(new Email(2, new int[] { 1, 1, 3 },
				new int[] { 1, 1, 0, 1 }, null));
		emails.add(new Email(3, new int[] {}, new int[] { 1, 0, 0, 0 }, null));
		emails.add(new Email(3, new int[] { 0 }, new int[] { 1, 0, 0, 0 }, null));

		double[][] trueValue = new double[][] {
				{ Math.log(3.0 / 2.0), Math.log(1.0 / 1.0),
						Math.log(2.0 / 1.0), Math.log(1.0 / 1.0),
						Math.log(2.0 / 1.0) },
				{ Math.log(1.0 / 2.0), Math.log(2.0 / 1.0),
						Math.log(1.0 / 1.0), Math.log(2.0 / 1.0),
						Math.log(1.0 / 1.0) },
				{ Math.log(2.0 / 2.0), Math.log(1.0 / 1.0),
						Math.log(2.0 / 1.0), Math.log(1.0 / 1.0),
						Math.log(2.0 / 1.0) },
				{ Math.log(1.0 / 2.0), Math.log(2.0 / 1.0),
						Math.log(1.0 / 1.0), Math.log(2.0 / 1.0),
						Math.log(1.0 / 1.0) },
				{ Math.log(2.0 / 2.0), Math.log(1.0 / 1.0),
						Math.log(2.0 / 1.0), Math.log(1.0 / 1.0),
						Math.log(2.0 / 1.0) } };

		emails.calculateCoOccurenceStatistics();
		for (int i = 0; i < trueValue.length; i++) {
			for (int j = 0; j < trueValue[i].length; j++) {
				assertEquals(trueValue[i][j],
						emails.getCoOccurenceStatistic(i, j), 1e-12);
			}
		}
	}

	@Test
	public void testWriteToFiles() {

		Randoms rng = new Randoms();
		EmailCorpus emails = new EmailCorpus(10, 100, 20, 10, rng);

		String wordMatrixFile = "word-matrix.csv";
		String edgeMatrixFile = "edge-matrix.csv";
		String vocabFile = "vocab.txt";

		emails.writeToFiles(wordMatrixFile, edgeMatrixFile, vocabFile);
		
		EmailCorpus emailsTest = new EmailCorpus(emails.getNumAuthors());
		InstanceListLoader.load(wordMatrixFile, vocabFile, edgeMatrixFile,
				emailsTest);
		
		System.out.println(emails);
		System.out.println(emailsTest);
		assertEquals(emails, emailsTest);
	}
	
	@Test
	public void testCopy() {

		Randoms rng = new Randoms();
		EmailCorpus emails = new EmailCorpus(10, 100, 20, 10, rng);
		
		EmailCorpus emailsTest = emails.copy();
		
		System.out.println(emails);
		System.out.println(emailsTest);
		
		assertEquals(emails, emailsTest);
		
		// make sure they are not the same object
		assertEquals(false, emails == emailsTest); 

	}

}
