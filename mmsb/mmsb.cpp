#include "mmsb.hpp"
#include "categorical_sample.hpp"

vector<double> cdf2pdf(vector<double> cdf);
void print_unnormalized_cdf(vector<double> cdf);

double log_dir_mult_likelihood(std::vector<int> &n, std::vector<double> &a) {
	double A = 0;
	double N = 0;
	double result = 0;

	if (n.size() != a.size()) {
		throw 0;
	}

	for (int i = 0; i < n.size(); i++) {
		A += a[i];
		N += n[i];

		result += lgamma(n[i] + a[i]) - lgamma(a[i]);
	}

	result += lgamma(A) - lgamma(A + N);

	return result;
}

mmsb::mmsb(Corpus &corpus)
		: corpus(corpus) {
	this->y = corpus.getEdges();
}

mmsb::mmsb(Corpus &corpus, const vector<double> &alpha, const vector<double> &betaDiagonal, const vector<double> &betaOffDiagonal)
		: corpus(corpus) {
	this->y = corpus.getEdges();
	this->alpha = alpha;
	this->betaDiagonal = betaDiagonal;
	this->betaOffDiagonal = betaOffDiagonal;
	this->A = y.size();
	this->K = alpha.size();

	cout << "A = " << this->A << ", K = " << this->K << "\n";

	// Resize count vectors
	this->N_author_group.resize(A);
	for (int a = 0; a < A; a++) {
		this->N_author_group[a].resize(K);
	}
	this->N_group_edge.resize(K);
	this->N_group.resize(K);
	for (int p = 0; p < K; p++) {
		this->N_group_edge[p].resize(K);
		for (int q = 0; q < K; q++) {
			this->N_group_edge[p][q].resize(2);
		}
		this->N_group[p].resize(K);
	}

	this->Z_to.resize(A);
	this->Z_from.resize(A);
	for (int a = 0; a < A; a++) {
		int D = y[a].size();
		this->Z_to[a].resize(D);
		this->Z_from[a].resize(D);

		for (int d = 0; d < D; d++) {
			this->Z_to[a][d].resize(A);
			this->Z_from[a][d].resize(A);

			// Randomly initialize all obscured edges
			if (corpus.isObscured(a, d)) {
				this->y[a][d].resize(A);
				for (int r = 0; r < A; r++) {
					if (a != r) {
						y[a][d][r] = rand() > RAND_MAX / 2;
					}
				}
			}
		}
	}

	sample_number = 0;
}

void mmsb::sample(int S) {
	for (int s = 0; s < S; s++) {
		for (int a = 0; a < A; a++) {
			int D = y[a].size();
			for (int d = 0; d < D; d++) {
				for (int r = 0; r < A; r++) {
					if (a != r) {
						int z_to = Z_to[a][d][r];
						int z_from = Z_from[a][d][r];
						if (sample_number != 0) {
							N_group_edge[z_to][z_from][y[a][d][r] ? 1 : 0]--;
							N_group[z_to][z_from]--;

							N_author_group[a][z_to]--;
							N_author_group[r][z_from]--;
						}

						int sample = sample_unnormalized_cdf(this->Z_joint_cdf(a, r, y[a][d][r] ? 1 : 0));

						z_to = sample / this->K;
						z_from = sample % this->K;

						Z_to[a][d][r] = z_to;
						Z_from[a][d][r] = z_from;

						N_author_group[a][z_to]++;
						N_author_group[r][z_from]++;

						if (corpus.isObscured(a, d)) {
							y[a][d][r] = sample_unnormalized_cdf(this->y_cdf(z_to, z_from));
						}

						N_group_edge[z_to][z_from][y[a][d][r] ? 1 : 0]++;
						N_group[z_to][z_from]++;
					}
				}
			}
		}

		sample_number++;

		cout << "Sample " << sample_number << " log-prob: " << log_prob() << "\n";
	}
}

double mmsb::log_like() {
	double ll;

	for (int p = 0; p < K; p++) {
		for (int q = 0; q < K; q++) {
			if (p == q) {
				ll += log_dir_mult_likelihood(N_group_edge[p][q], betaDiagonal);
			} else {
				ll += log_dir_mult_likelihood(N_group_edge[p][q], betaOffDiagonal);
			}
		}
	}

	return ll;
}

double mmsb::log_prob() {
	double lp = log_like();

	for (int a = 0; a < A; a++) {
		lp += log_dir_mult_likelihood(N_author_group[a], alpha);
	}

	return lp;
}

vector<double> mmsb::y_cdf(int z_to, int z_from) {
	vector<double> cdf(2);

	vector<double> &beta = (z_to == z_from) ? betaDiagonal : betaOffDiagonal;

	cdf[0] = (N_group_edge[z_to][z_from][0] + beta[0]) / (N_group[z_to][z_from] + beta[0] + beta[1]);
	cdf[1] = 1;

	return cdf;
}

vector<double> mmsb::Z_prior_cdf(int a) {
	vector<double> cdf(K);

	double sum = 0;
	for (int p = 0; p < K; p++) {
		cdf[p] = sum + N_author_group[a][p] + alpha[p];
		sum = cdf[p];
	}

	return cdf;
}

vector<double> mmsb::y_marginal_cdf(int a, int r) {
	vector<double> cdf(2);

	double sum = 0;
	vector<double> z_a_prior_pdf = cdf2pdf(this->Z_prior_cdf(a));
	vector<double> z_r_prior_pdf = cdf2pdf(this->Z_prior_cdf(r));

	int i = 0;
	for (int z_to = 0; z_to < K; z_to++) {
		for (int z_from = 0; z_from < K; z_from++) {
			vector<double> y_pdf = cdf2pdf(y_cdf(z_to, z_from));

			cdf[0] += y_pdf[0] * z_a_prior_pdf[i] * z_r_prior_pdf[i];
			cdf[1] += y_pdf[1] * z_a_prior_pdf[i] * z_r_prior_pdf[i];

			i++;
		}
	}

	cdf[1] += cdf[0];

	return cdf;
}

vector<double> mmsb::Z_joint_cdf(int a, int r, int edge) {
	vector<double> cdf(K * K);

	int i = 0;
	double sum = 0;
	for (int z_to = 0; z_to < K; z_to++) {
		for (int z_from = 0; z_from < K; z_from++) {
			vector<double> &beta = (z_to == z_from) ? betaDiagonal : betaOffDiagonal;

			cdf[i] = sum + (N_author_group[a][z_to] + alpha[z_to]) * (N_author_group[r][z_from] + alpha[z_from])
				* (N_group_edge[z_to][z_from][edge] + beta[edge]) / (N_group[z_to][z_from] + beta[0] + beta[1]);
			sum = cdf[i];

			i++;
		}
	}

	return cdf;
}

bool mmsb::checkConsistency() {
	vector<vector<int> > T_author_group(A);
	vector<vector<vector<int> > > T_group_edge(K);
	vector<vector<int> > T_group(K);

	for (int p = 0; p < K; p++) {
		T_group_edge[p].resize(K);
		T_group[p].resize(K);
		for (int q = 0; q < K; q++) {
			T_group_edge[p][q].resize(2);
		}
	}

	for (int a = 0; a < A; a++) {
		T_author_group[a].resize(K);
	}

	for (int a = 0; a < A; a++) {
		int D = Z_to[a].size();
		for (int d = 0; d < D; d++) {
//			if (!corpus.isObscured(a, d)) {
				for (int r = 0; r < A; r++) {
					if (a != r) {
						int z_to = Z_to[a][d][r];
						int z_from = Z_from[a][d][r];
						int edge = y[a][d][r] ? 1 : 0;
						T_author_group[a][z_to]++;
						T_author_group[r][z_from]++;
	
						T_group_edge[z_to][z_from][edge]++;
						T_group[z_to][z_from]++;
					}
//				}
			}
		}
	}

	for (int a = 0; a < A; a++) {
		for (int p = 0; p < K; p++) {
			if (T_author_group[a][p] != N_author_group[a][p]) {
				throw 2;
			}
		}
	}

	for (int p = 0; p < K; p++) {
		for (int q = 0; q < K; q++) {
			for (int o = 0; o < 2; o++) {
				if (T_group_edge[p][q][o] != N_group_edge[p][q][o]) {
					throw 3;
				}
			}

			if (T_group[p][q] != N_group[p][q]) {
				throw 4;
			}
		}
	}

	return true;
}

void mmsb::makeConsistent() {
	vector<vector<int> > T_author_group(A);
	vector<vector<vector<int> > > T_group_edge(K);
	vector<vector<int> > T_group(K);

	for (int p = 0; p < K; p++) {
		T_group_edge[p].resize(K);
		T_group[p].resize(K);
		for (int q = 0; q < K; q++) {
			T_group_edge[p][q].resize(2);
		}
	}

	for (int a = 0; a < A; a++) {
		T_author_group[a].resize(K);
	}

	for (int a = 0; a < A; a++) {
		int D = Z_to[a].size();
		for (int d = 0; d < D; d++) {
			if (!corpus.isObscured(a, d)) {
				for (int r = 0; r < A; r++) {
					if (a != r) {
						int z_to = Z_to[a][d][r];
						int z_from = Z_from[a][d][r];
						int edge = y[a][d][r] ? 1 : 0;
						T_author_group[a][z_to]++;
						T_author_group[r][z_from]++;

						T_group_edge[z_to][z_from][edge]++;
						T_group[z_to][z_from]++;
					}
				}
			}
		}
	}

	N_author_group = T_author_group;
	N_group_edge = T_group_edge;
	N_group = T_group;
}

/*
vector<vector<vector<bool> > > mmsb::getSample_y() {
	return y;
}
*/

vector<vector<double> > mmsb::getMembershipVectors() {
	vector<vector<double> > memberships(A);

	for (int a = 0; a < A; a++) {
		memberships[a].resize(K);
		double sum = 0;
		for (int p = 0; p < K; p++) {
			memberships[a][p] = N_author_group[a][p] + alpha[p];
			sum += memberships[a][p];
		}
		for (int p = 0; p < K; p++) {
			memberships[a][p] /= sum;
		}
	}

	return memberships;
}

vector<vector<double> > mmsb::getBlockmodel() {
	vector<vector<double> > blockmodel(K);

	for (int z_to = 0; z_to < K; z_to++) {
		blockmodel[z_to].resize(K);
		for (int z_from = 0; z_from < K; z_from++) {
	vector<double> &beta = (z_to == z_from) ? betaDiagonal : betaOffDiagonal;
			blockmodel[z_to][z_from] = (N_group_edge[z_to][z_from][1] + beta[1]) / (N_group[z_to][z_from] + beta[0] + beta[1]);
		}
	}

	return blockmodel;
}

/*
vector<vector<vector<bool> > > mmsb::getMarginalPredictions(vector<vector<vector<bool> > > true_values) {
	vector<vector<vector<bool> > > predictions(A);
	for (int a = 0; a < A; a++) {
		int D = y[a].size();
		predictions[a].resize(D);
		for (int d = 0; d < D; d++) {
			predictions[a][d].resize(A);
			for (int r = 0; r < A; r++) {
				if (a != r and corpus.isObscured(a, d, r)) {
//					cout << (double)y_marginal[a][d][r] / sample_number << " : " << true_values[a][d][r] << "\n";
//					predictions[a][d][r] = (double)y_marginal[a][d][r] / sample_number > 0.5;
				}
			}
		}
	}

	return predictions;
}
*/

vector<double> cdf2pdf(vector<double> cdf) {
	vector<double> pdf(cdf.size());

	pdf[0] = cdf[0];
	for (int i = 1; i < cdf.size(); i++) {
		pdf[i] = cdf[i] - cdf[i-1];
	}

	return pdf;
}

vector<vector<vector<bool> > > mmsb::getPredictions() {
	return y;
}

vector<vector<vector<bool> > > mmsb::getMarginalPredictions() {
	vector<vector<vector<bool> > > y = this->y;

	for (int a = 0; a < A; a++) {
		int D = y[a].size();
		for (int r = 0; r < A; r++) {
			if (a != r) {
				vector<double> cdf = y_marginal_cdf(a, r);
				bool prediction = cdf[0] < 0.5 * cdf[1];
//				cout << "A: " << a << ", R: " << r << " :: " << 1 - cdf[0] / cdf[1] << "\n";
				for (int d = 0; d < D; d++) {
					if (corpus.isObscured(a, d)) {
						y[a][d][r] = prediction;
					}
				}
			}
		}
	}

	return y;
}

void print_unnormalized_cdf(vector<double> cdf) {
	cout << "Printing CDF: [[ ";
	for (int i = 0; i < cdf.size(); i++) {
		cout << cdf[i] << "\t";
	}
	cout << " ]]\n";
}

/*
template<class Archive>
void mmsb::serialize(Archive & ar, const unsigned int version) {
	ar & y;
	ar & alpha;
	ar & betaDiagonal;
	ar & betaOffDiagonal;
	ar & A;
	ar & K;
	ar & Z_to;
	ar & Z_from;
	ar & N_author_group;
	ar & N_group_edge;
	ar & N_group;
	ar & sample_number;
}
*/

