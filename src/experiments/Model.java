package experiments;

import data.EmailCorpus;

/**
 * An interface for specifying basic email modeling functionality.
 */
public interface Model {

	EmailCorpus getEmails();

	void setEmails(EmailCorpus emails);

	double getDocEdgeLogLike(int d);
}
