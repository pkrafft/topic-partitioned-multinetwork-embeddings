package baselines;

import util.LogRandoms;
import util.Network;
import data.Email;
import data.EmailCorpus;
import experiments.Model;

/**
 * Implementation of the edge-only baseline discussed in Krafft et al. (2012).
 */
public class EdgeFrequencyModel implements Model {

	EmailCorpus emails;
	int[][] presentNetwork;
	int[][] absentNetwork;

	int numActors;
	int numDocs;

	LogRandoms rng;

	public EdgeFrequencyModel(EmailCorpus emails, LogRandoms rng) {
		this.emails = emails;
		numActors = emails.getNumAuthors();
		numDocs = emails.size();
		presentNetwork = new int[numActors][numActors];
		absentNetwork = new int[numActors][numActors];
		if (rng == null) {
			this.rng = new LogRandoms();
		} else {
			this.rng = rng;
		}
	}

	/**
	 * Fill in missing edges using this model.
	 */
	public void estimate() {

		Network network = emails.getCoarseNetwork();

		presentNetwork = network.getPresentEdges();
		absentNetwork = network.getAbsentEdges();

		for (int d = 0; d < numDocs; d++) {

			Email e = emails.getEmail(d);
			int a = e.getAuthor();

			for (int j = 0; j < numActors - 1; j++) {

				if (e.getMissingData().contains(j)) {

					int r = e.getRecipient(j);

					double p = Math.log(presentNetwork[a][r])
							- Math.log(presentNetwork[a][r]
									+ absentNetwork[a][r]);

					int y = Math.log(rng.nextDouble()) < p ? 1 : 0;

					e.setEdge(r, y);
				}
			}
		}
	}

	@Override
	public double getDocEdgeLogLike(int d) {

		double logLike = 0;

		Email e = emails.getEmail(d);
		int a = e.getAuthor();

		for (int j = 0; j < numActors - 1; j++) {

			if (e.getMissingData().contains(j)) {

				int r = e.getRecipient(j);
				int y = e.getEdge(r);

				double p = Math.log(presentNetwork[a][r])
						- Math.log(presentNetwork[a][r] + absentNetwork[a][r]);
				p = y == 1 ? p : Math.log(1 - Math.exp(p));

				logLike += p;
			}
		}

		return logLike;
	}

	@Override
	public EmailCorpus getEmails() {
		return emails;
	}

	@Override
	public void setEmails(EmailCorpus emails) {
		this.emails = emails;
	}
}
