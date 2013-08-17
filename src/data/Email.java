package data;

import java.util.Arrays;
import java.util.HashSet;

/**
 * A class for representing particular emails.
 * 
 * Each object includes the text and recipients of some email. The
 * representation of the recipients is a little counterintuitive because the
 * information about recipients is stored in a binary array of length A (where A
 * is the number of actors), but whenever the recipients are used in the code
 * such as with the {@link getIndex} or {@link getRecipient} methods or in the
 * {@link #missingEdges} object it is as if the recipients are stored in a
 * vector of length A - 1 in which the author of the email is excluded.
 */
public class Email extends Document {

	int author;
	int[] edges;
	HashSet<Integer> missingEdges;
	boolean obscured;

	/**
	 * 
	 * @param author
	 *            the author this email
	 * @param tokens
	 *            an array of integers representing the tokens in the email. the
	 *            array is as long as the length of the email and each entry is
	 *            the index of the type of that word in the vocabulary.
	 * @param edges
	 *            a length A binary array indicating the recipients of this
	 *            email
	 * @param missingEdges
	 *            a hashset containing the indices to the A - 1 length
	 *            representation of the actors that are not known to be or not
	 *            be recipients of this email
	 * @param source
	 *            the file that the raw data for this email came from
	 */
	public Email(int author, int[] tokens, int[] edges,
			HashSet<Integer> missingEdges, String source) {
		super(source);
		this.author = author;
		this.edges = edges;
		this.source = source;
		this.tokens = tokens;
		this.missingEdges = missingEdges;
	}

	public Email(int author, int[] tokens, int[] edges, String source) {
		this(author, tokens, edges, new HashSet<Integer>(), source);
	}

	public Email(int author, int[] edges, String source) {
		this(author, new int[] {}, edges, source);
	}

	public int getAuthor() {
		return author;
	}

	public void setEdge(int r, int y) {
		edges[r] = y;
	}

	/**
	 * Return whether a particular actor is a recipient of this email.
	 * 
	 * @param r
	 *            the id of a particular actor
	 * @return 1 if r is a recipient of this email, 0 if not, or a value < 0 if
	 *         the binary value is unknown
	 */
	public int getEdge(int r) {
		return edges[r];
	}

	/**
	 * Return the actor associated with an index to the A - 1 length array
	 * representation of recipients that excludes the author of this email.
	 * 
	 * @param j
	 *            the index of an a recipient of this email
	 * @return the actor id of the jth possible recipient
	 */
	public int getRecipient(int j) {
		return j < author ? j : j + 1;
	}

	/**
	 * Return the index to the A - 1 length array representation of recipients
	 * that excludes the author of this email.
	 * 
	 * @param r
	 *            the id of a particular actor
	 * @return the index of actor r in the A - 1 length representation
	 */
	public int getIndex(int r) {
		assert r != author;
		return r < author ? r : r - 1;
	}

	public void setMissingData(HashSet<Integer> missingEdges) {
		this.missingEdges = missingEdges;
	}

	/**
	 * 
	 * @return the indices to the A - 1 length representation of the actors that
	 *         are not known to be or not to be recipients of this email.
	 */
	public HashSet<Integer> getMissingData() {
		return missingEdges;
	}

	public void obscureEdge(int r) {
		if (r != author && !missingEdges.contains(r)) {
			edges[r] = Integer.MIN_VALUE;
			int j = getIndex(r);
			missingEdges.add(j);
		}
	}

	public void obscure() {
		setObscured();
		for (int r = 0; r < edges.length; r++) {
			obscureEdge(r);
		}
	}

	public void setObscured() {
		obscured = true;
	}

	public boolean getObscured() {
		return obscured;
	}

	public Email copy() {
		int[] tokenCopy = Arrays.copyOf(tokens, tokens.length);
		int[] edgesCopy = Arrays.copyOf(edges, edges.length);
		HashSet<Integer> missingEdgeCopy = new HashSet<Integer>(
				missingEdges.size());
		for (int j : missingEdges) {
			missingEdgeCopy.add(j);
		}
		Email copy = new Email(author, tokenCopy, edgesCopy, missingEdgeCopy,
				source);
		if (obscured) {
			copy.setObscured();
		}
		return (copy);
	}

	@Override
	public String toString() {
		String string = "";

		int[] tokenCopy = Arrays.copyOf(tokens, tokens.length);
		Arrays.sort(tokenCopy);
		string += "Author: " + author + "\n";
		string += "Tokens: " + Arrays.toString(tokenCopy) + "\n";
		string += "Edges: " + Arrays.toString(edges) + "\n";
		string += "Obscured: " + obscured + "\n";
		string += "Missing Edges: " + missingEdges;

		return string;
	}
}
