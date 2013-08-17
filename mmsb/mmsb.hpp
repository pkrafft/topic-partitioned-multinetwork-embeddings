#ifndef __JMOORE_MMSB_H__
#define __JMOORE_MMSB_H__

#include <vector>
#include <boost/math/special_functions/gamma.hpp>

#include <boost/serialization/vector.hpp>

#include "corpus.hpp"

using namespace std;
using namespace boost::math;

class mmsbTest;

class mmsb {
private:
	friend class boost::serialization::access;

	template<class Archive>
	void serialize(Archive & ar, const unsigned int version)
{
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

	void makeConsistent();

protected:
	Corpus corpus;
	vector<vector<vector<bool> > > y;
	vector<double> alpha, betaDiagonal, betaOffDiagonal; //, gamma;
	int A, K;

	/* Latent Variables */
//	vector<vector<vector<bool> > > Y;
	vector<vector<vector<int> > > Z_to, Z_from;

	/* Counts */
	vector<vector<int> > N_author_group;
	vector<vector<vector<int> > > N_group_edge;
	vector<vector<int> > N_group;
//	int N_1edge_unobserved;
//	int N_1edge;

	int sample_number;

	vector<double> y_cdf(int z_to, int z_from);
	vector<double> y_marginal_cdf(int a, int r);
	vector<double> Z_prior_cdf(int a);
	vector<double> Z_joint_cdf(int a, int r, int y);

//	vector<vector<vector<int> > > y_marginal;

public:
	mmsb(Corpus &corpus);
	mmsb(Corpus &corpus, const vector<double> &alpha, const vector<double> &betaDiagonal, const vector<double> &betaOffDiagonal);
	void sample(int S);
	double log_like();
	double log_prob();
	bool checkConsistency();
//	vector<vector<vector<bool> > > getSample_y();
//	vector<vector<vector<bool> > > getMarginalPredictions(vector<vector<vector<bool> > > true_values);
	vector<vector<double> > getMembershipVectors();
	vector<vector<double> > getBlockmodel();
	vector<vector<vector<bool> > > getPredictions();
	vector<vector<vector<bool> > > getMarginalPredictions();

	friend class mmsbTest;
};

#endif
