#include "corpus.hpp"
#include <stdlib.h>

Corpus::Corpus() {}

Corpus::Corpus(int A, int V) {
	this->A = A;
	this->V = V;
}

Corpus::Corpus(istream &file) {
	if (file.fail()) {
		throw 1;
	}

	string line;
	getline(file, line);
	stringstream lineStream(line);

	lineStream >> A >> V;

	edges.resize(A);
	messageObscured.resize(A);

	int i = 0;
	while (getline(file, line)) {
		i++;

		stringstream lineStream(line);
		string tok;

		getline(lineStream, tok, ',');
		int a;
		stringstream(tok) >> a;
		
		int d = edges[a].size();
		edges[a].push_back(vector<bool>(A));
		messageObscured[a].push_back(false);
	
		int r;
		while (getline(lineStream, tok, ',')) {
			stringstream(tok) >> r;
			if (r >= A) {
				throw 0;
			}
			edges[a][d][r] = true;
		}
		
		getline(file, line); // Throw away token info
		i++;
	}
}

// Returns the number of obscured messages
int Corpus::obscureRandomMessages(double p) {
	int count = 0;
	for (int a = 0; a < A; a++) {
		int D = edges[a].size();
		for (int d = 0; d < D; d++) {
			double u = (double)rand() / RAND_MAX;
			if (u < p) {
				messageObscured[a][d] = true;
				count++;
			}
		}
	}

	return count;
}

/*
// Returns the number of obscured edges
int Corpus::obscureRandomEdges(double p) {
	int count = 0;
	for (int a = 0; a < A; a++) {
		int D = edges[a].size();
		for (int d = 0; d < D; d++) {
			for (int r = 0; r < A; r++) {
				double u = (double)rand() / RAND_MAX;
				if (u < p) {
					edgeObscured[a][d][r] = true;
					count++;
				}
			}
		}
	}

	return count;
}
*/

vector<vector<vector<bool> > > Corpus::getEdges() {
	return edges;
}

bool Corpus::isObscured(int a, int d) {
	return messageObscured[a][d];
}

/*
bool Corpus::isObscured(int a, int d, int r) {
	return edgeObscured[a][d][r];
}
*/

bool Corpus::getEdge(int a, int d, int r) {
	return edges[a][d][r];
}

string Corpus::printFlatNetwork() {
	std::stringstream ss;
	for (int a = 0; a < A; a++) {
		for (int r = 0; r < A; r++) {
			int sum = 0;
			for (int d = 0; d < edges[a].size(); d++) {
				sum += edges[a][d][r] ? 1 : 0;
			}

			ss << sum << " ";
		}
		ss << "\n";
	}

	return ss.str();
}

int Corpus::numObscuredMessages() {
	int numObscured = 0;

	for (int a = 0; a < A; a++) {
		int D = edges[a].size();
		for (int d = 0; d < D; d++) {
			if (isObscured(a, d)) {
				numObscured++;
			}
		}
	}

	return numObscured;
}

//void Corpus::serialize(boost::archive::text_oarchive & ar, const unsigned int version) {
/*
template<class Archive>
void Corpus::serialize(Archive & ar, const unsigned int version) {
	ar & A;
	ar & V;
	ar & edges;
	ar & messageObscured;
}
*/

