package mixedmembership;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cc.mallet.types.Alphabet;
import data.Email;
import data.EmailCorpus;

public class WordScoreTest {

	@Test
	public void testTopicCoherence() {
		Alphabet wordDict = new Alphabet();
		wordDict.lookupIndex("a"); 
		wordDict.lookupIndex("b");
		wordDict.lookupIndex("c");
		wordDict.lookupIndex("d");
		wordDict.lookupIndex("e");
		
		EmailCorpus emails = new EmailCorpus(4, wordDict);
		emails.add(new Email(0, new int[]{0, 4, 2, 2}, new int[]{0, 0, 0, 1}, null));
		emails.add(new Email(2, new int[]{0, 1, 1, 3}, new int[]{1, 1, 0, 1}, null));
		emails.add(new Email(3, new int[]{}, new int[]{1, 0, 0, 0}, null));
		emails.add(new Email(3, new int[]{0, 1}, new int[]{1, 0, 0, 0}, null));
		emails.calculateCoOccurenceStatistics();
		
		WordScore wordScore = new WordScore(wordDict.size(), 3, new double[]{1.0}, null);
		wordScore.setTopic(0, new int[]{3, 2, 5, 1, 4});
		wordScore.setTopic(1, new int[]{3, 4, 5, 0, 0});
		wordScore.setTopic(2, new int[]{5, 4, 3, 2, 1});
				
		double trueValue;
		
		// [4|2 0|2] 0|4
		// [log(2/1) + log(2/1)] + log(2/1)
		trueValue = 3*Math.log(2.0/1.0);
		assertEquals(trueValue, wordScore.getTopicCoherence(wordScore.getTopic(0), 3, emails), 1e-12);
		
		// [1|2 0|2] 0|1
		// [log(1/1) + log(2/1)] + log(3/2)
		trueValue = Math.log(2.0/1.0) + Math.log(3.0/2.0);
		assertEquals(trueValue, wordScore.getTopicCoherence(wordScore.getTopic(1), 3, emails), 1e-12);
		
		// [1|0 2|0 3|0 4|0] [2|1 3|1 4|1] [3|2 4|2] [4|3]
		// [log(3/3) + log(2/3) + log(2/3) + log(2/3)] + [log(1/2) + log(2/2) + log(1/2)] + [log(1/1) + log(2/1)] + [log(1/1)]
		trueValue = 3*Math.log(2.0/3.0) + Math.log(1.0/2.0);
		assertEquals(trueValue, wordScore.getTopicCoherence(wordScore.getTopic(2), 5, emails), 1e-12);
	
		// [1|0 2|0] 2|1
		// [log(3/3) + log(2/3)] + log(1/2)
		trueValue = Math.log(2.0/3.0) + Math.log(1.0/2.0);
		assertEquals(trueValue, wordScore.getTopicCoherence(wordScore.getTopic(2), 3, emails), 1e-12);
		
		trueValue = (3*Math.log(2.0/1.0) + Math.log(2.0/1.0) + Math.log(3.0/2.0) + Math.log(2.0/3.0) + Math.log(1.0/2.0))/3;
		assertEquals(trueValue, wordScore.printCoherence(3, emails), 1e-12);
	}

}