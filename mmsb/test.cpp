#include "corpus.hpp"
#include "mmsb.hpp"

class mmsbTest {
public:
	bool testLikelihood() {
		// Construct corpus
		Corpus corpus(3, 0);
		vector<vector<vector<bool> > > edges(3);
		edges[0].push_back(vector<bool>(3));
		edges[0][0][0] = false;
		edges[0][0][1] = true;
		edges[0][0][2] = true;
		edges[1].push_back(vector<bool>(3));
		edges[1][0][0] = false;
		edges[1][0][1] = false;
		edges[1][0][2] = false;
		edges[2].push_back(vector<bool>(3));
		edges[2][0][0] = true;
		edges[2][0][1] = false;
		edges[2][0][2] = false;

		corpus.edges = edges;

		vector<vector<vector<bool> > > edgeObserved(3);
		edgeObserved[0].push_back(vector<bool>(3));
		edgeObserved[0][0][0] = false;
		edgeObserved[0][0][1] = false;
		edgeObserved[0][0][2] = false;
		edgeObserved[1].push_back(vector<bool>(3));
		edgeObserved[1][0][0] = false;
		edgeObserved[1][0][1] = false;
		edgeObserved[1][0][2] = false;
		edgeObserved[2].push_back(vector<bool>(3));
		edgeObserved[2][0][0] = false;
		edgeObserved[2][0][1] = false;
		edgeObserved[2][0][2] = false;

		corpus.edgeObscured = edgeObserved;

		// Construct model
		std::vector<double> alpha(2);
		alpha[0] = 1;
		alpha[1] = 1;
		std::vector<double> beta(2);
		beta[0] = 2;
		beta[1] = 2;

		mmsb model(corpus, alpha, beta);

		vector<vector<vector<int> > > Z_to(3), Z_from(3);
		Z_to[0].push_back(vector<int>(3));
		Z_to[0][0][0] = -1;
		Z_to[0][0][1] = 0;
		Z_to[0][0][2] = 1;
		Z_to[1].push_back(vector<int>(3));
		Z_to[1][0][0] = 1;
		Z_to[1][0][1] = -1;
		Z_to[1][0][2] = 1;
		Z_to[2].push_back(vector<int>(3));
		Z_to[2][0][0] = 0;
		Z_to[2][0][1] = 0;
		Z_to[2][0][2] = -1;

		model.Z_to = Z_to;

		Z_from[0].push_back(vector<int>(3));
		Z_from[0][0][0] = -1;
		Z_from[0][0][1] = 1;
		Z_from[0][0][2] = 1;
		Z_from[1].push_back(vector<int>(3));
		Z_from[1][0][0] = 1;
		Z_from[1][0][1] = -1;
		Z_from[1][0][2] = 1;
		Z_from[2].push_back(vector<int>(3));
		Z_from[2][0][0] = 0;
		Z_from[2][0][1] = 0;
		Z_from[2][0][2] = -1;

		model.Z_from = Z_from;

		model.makeConsistent();

		cout << "N_group_edge\n";
		for (int p = 0; p < 2; p++) {
			for (int q = 0; q < 2; q++) {
				cout << p << "\t" << q << "\t" << model.N_group_edge[p][q][0] << "\t" << model.N_group_edge[p][q][1] << "\n";
			}
		}

		cout << "N_author_group\n";
		for (int a = 0; a < 3; a++) {
			for (int p = 0; p < 2; p++) {
				cout << a << "\t" << p << "\t" << model.N_author_group[a][p] << "\n";
			}
		}

		cout << "Likelihood: " << exp(model.log_like()) << "\n";
		cout << "Joint Prob: " << exp(model.log_prob()) << "\n";
	}

	bool testGibbsSampler() {
		// Construct corpus
		Corpus corpus(3, 0);
		vector<vector<vector<bool> > > edges(3);
		edges[0].push_back(vector<bool>(3));
		edges[0][0][0] = false;
		edges[0][0][1] = true;
		edges[0][0][2] = true;
		edges[1].push_back(vector<bool>(3));
		edges[1][0][0] = false;
		edges[1][0][1] = false;
		edges[1][0][2] = false;
		edges[2].push_back(vector<bool>(3));
		edges[2][0][0] = true;
		edges[2][0][1] = false;
		edges[2][0][2] = false;

		corpus.edges = edges;

		vector<vector<vector<bool> > > edgeObserved(3);
		edgeObserved[0].push_back(vector<bool>(3));
		edgeObserved[0][0][0] = false;
		edgeObserved[0][0][1] = false;
		edgeObserved[0][0][2] = false;
		edgeObserved[1].push_back(vector<bool>(3));
		edgeObserved[1][0][0] = false;
		edgeObserved[1][0][1] = false;
		edgeObserved[1][0][2] = false;
		edgeObserved[2].push_back(vector<bool>(3));
		edgeObserved[2][0][0] = false;
		edgeObserved[2][0][1] = false;
		edgeObserved[2][0][2] = false;

		corpus.edgeObscured = edgeObserved;

		// Construct model
		std::vector<double> alpha(2);
		alpha[0] = 1;
		alpha[1] = 1;
		std::vector<double> beta(2);
		beta[0] = 2;
		beta[1] = 2;

		mmsb model(corpus, alpha, beta);

		vector<vector<vector<int> > > Z_to(3), Z_from(3);
		Z_to[0].push_back(vector<int>(3));
		Z_to[0][0][0] = -1;
		Z_to[0][0][1] = 0;
		Z_to[0][0][2] = 1;
		Z_to[1].push_back(vector<int>(3));
		Z_to[1][0][0] = 1;
		Z_to[1][0][1] = -1;
		Z_to[1][0][2] = 1;
		Z_to[2].push_back(vector<int>(3));
		Z_to[2][0][0] = 0;
		Z_to[2][0][1] = 0;
		Z_to[2][0][2] = -1;

		model.Z_to = Z_to;

		Z_from[0].push_back(vector<int>(3));
		Z_from[0][0][0] = -1;
		Z_from[0][0][1] = 1;
		Z_from[0][0][2] = 1;
		Z_from[1].push_back(vector<int>(3));
		Z_from[1][0][0] = 1;
		Z_from[1][0][1] = -1;
		Z_from[1][0][2] = 1;
		Z_from[2].push_back(vector<int>(3));
		Z_from[2][0][0] = 0;
		Z_from[2][0][1] = 0;
		Z_from[2][0][2] = -1;

		model.Z_from = Z_from;

		model.makeConsistent();

		model.N_author_group[0][0]--;
		model.N_author_group[1][1]--;
		model.N_group_edge[0][1][1]--;
		model.N_group[0][1]--;
		vector<double> Z_cdf = model.Z_joint_cdf(0, 1, 1);
		model.N_author_group[0][0]++;
		model.N_author_group[1][1]++;
		model.N_group_edge[0][1][1]++;
		model.N_group[0][1]++;

		cout << "Z CDF(0, 1, 1):\n";
		int i = 0;
		for (int p = 0; p < 2; p++) {
			for (int q = 0; q < 2; q++) {
				double density = Z_cdf[i];
				if (i > 0) {
					density -= Z_cdf[i-1];
				}
				cout << density / Z_cdf[3] << "\t";
				i++;
			}
			cout << "\n";
		}
	}
};

int main(void) {
	mmsbTest tester;

	cout << "==================\n";
	cout << "Testing Likelihood\n";
	cout << "==================\n";
	tester.testLikelihood();
	// True Value: 5.55556e-07

	cout << "\n=====================\n";
	cout << "Testing Gibbs Sampler\n";
	cout << "=====================\n";
	tester.testGibbsSampler();
}

