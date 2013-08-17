#include<iostream>
#include<fstream>
#include<vector>
#include<sstream>

#include <sys/time.h>
#include <boost/serialization/vector.hpp>
#include <boost/archive/text_oarchive.hpp>
#include <boost/archive/text_iarchive.hpp>

#define DEBUG

#include "mmsb.hpp"
#include "corpus.hpp"

#define MIN(a,b) (a < b ? a : b)

using namespace std;

double fscore(vector<vector<vector<bool> > > sample, Corpus &corpus);
void printMembershipVectors(vector<vector<double> > memberships);
void printBlockmodel(vector<vector<double> > blockmodel);

int main(int argc, char* argv[]) {
	if (argc != 7) {
		cerr << "Usage: " << argv[0] << " corpus_state model_state iterations batch_size save_state_interval save_state_path" << "\n\n";
		exit(1);
	}

	struct timeval tv;
	if (gettimeofday(&tv, NULL) != 0) {
		cerr << "Can't get current system time, so I cannot initialize the RNG.";
	}
	srand ( tv.tv_usec + 1000000 * tv.tv_sec );

	int inx = 0;

	// Load corpus from state file
	Corpus corpus;

	ifstream corpus_state_file(argv[++inx]);
	boost::archive::text_iarchive corpus_archive(corpus_state_file);
	corpus_archive >> corpus;
	corpus_state_file.close();

	// Load model from state file

	int batch;
	mmsb model(corpus);

	ifstream model_state_file(argv[++inx]);
	boost::archive::text_iarchive model_archive(model_state_file);
	model_archive >> batch;
	model_archive >> model;
	corpus_state_file.close();

	int iterations = atoi(argv[++inx]);
	int batch_size = atoi(argv[++inx]);
	int save_state_interval = atoi(argv[++inx]);
	char* save_state_path = argv[++inx];

	if (save_state_interval > 0 && save_state_interval % batch_size != 0) {
		cerr << "Argument Error: save_state_interval must be an either <= 0 or an integer multiple of batch_size\n\n";
		return 0;
	}

	assert (inx == 6);

	for ( ; batch < iterations; batch += batch_size) {
		model.sample(MIN(batch_size, iterations - batch));
		#ifdef DEBUG
		model.checkConsistency();
		#endif
		/*
		printMembershipVectors(model.getMembershipVectors());
		printBlockmodel(model.getBlockmodel());
		*/

		cout << "F-Score: " << fscore(model.getPredictions(), corpus) << "\n";

		if (batch % save_state_interval == 0) {
			int sample_number = batch + MIN(batch_size, iterations - batch);

			stringstream ss;
			ss << save_state_path << '/' << sample_number << ".model_state";
			ofstream state_file(ss.str().c_str());
			boost::archive::text_oarchive archive(state_file);
			archive << sample_number;
			archive << model;
			state_file.close();
		}

//		cout << "Marginal F-Score: " << fscore(model.getMarginalPredictions(corpus.getEdges()), corpus) << "\n";
	}
}

double fscore(vector<vector<vector<bool> > > sample, Corpus &corpus) {
	int tp = 0,
		fp = 0,
		tn = 0,
		fn = 0;

	for (int a = 0; a < sample.size(); a++) {
		for (int d = 0; d < sample[a].size(); d++) {
			if (corpus.isObscured(a, d)) {
				for (int r = 0; r < sample[a][d].size(); r++) {
					if (a != r) {
						int trueEdge = corpus.getEdge(a, d, r);
						if (sample[a][d][r]) {
							if (trueEdge) {
								tp++;
							} else {
								fp++;
							}
						} else {
							if (trueEdge) {
								fn++;
							} else {
								tn++;
							}
						}
					}
				}
			}
		}
	}

	double precision = (1.0 * tp) / (tp + fp);
	double recall = (1.0 * tp) / (tp + fn);

	cout << "tp: " << tp << ", fp: " << fp << ", tn: " << tn << ", fn: " << fn << "\n";
	cout << "Precision: " << precision << "; Recall: " << recall << "\n";

	return 2 * precision * recall / (precision + recall);
}

void printMembershipVectors(vector<vector<double> > memberships) {
	cout << "Mixed-Membership Vectors\n";

	for (int a = 0; a < memberships.size(); a++) {
		cout << a << ": ";
		for (int p = 0; p < memberships[a].size(); p++) {
			cout << memberships[a][p] << "\t";
		}
		cout << "\n";
	}
}

void printBlockmodel(vector<vector<double> > blockmodel) {
	cout << "Blockmodel\n";
	for (int p = 0; p < blockmodel.size(); p++) {
		for (int q = 0; q < blockmodel.size(); q++) {
			cout << blockmodel[p][q] << "\t";
		}
		cout << "\n";
	}
}

