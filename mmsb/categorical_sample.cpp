#include "categorical_sample.hpp"
#include <iostream>

#define CDF_CHECK
#undef DEBUG

//
// TODO Should do this in log-space
//

using namespace std;

/*
 * Implements a binary search algorithm for sampling a discrete CDF
 */

inline int search_unnormalized_cdf(const vector<double>& cdf, double target);
int search_unnormalized_cdf(const vector<double>& cdf, int start, int stop, double target);

int sample_unnormalized_cdf(const vector<double> &cdf) {
	#ifdef CDF_CHECK
	for (int i = 0; i < cdf.size(); i++) {
		if (cdf[i] < 0 || (i > 0 && cdf[i] < cdf[i-1])) {
			// Invalid cdf!
			throw 22;
		}
	}
	#endif

	double target = ((double)rand() / RAND_MAX) * cdf[cdf.size()-1];

	int result = search_unnormalized_cdf(cdf, target);
	#ifdef DEBUG
	double min = 0;
	if (result > 0) {
		min = cdf[result-1];
	}
	if (result < 0 || result >= cdf.size() || target > cdf[result] || target < min) {
		cout << "Target = " << target << "\n";
		cout << "min = " << min << "\n";
		cout << "cdf[result] = " << cdf[result] << "\n";

		throw 22;
	}
	#endif

	return result;
}

inline int search_unnormalized_cdf(const vector<double>& cdf, double target) {
	return search_unnormalized_cdf(cdf, 0, cdf.size(), target);
}

//
// Binary search algorithm for log-time CDF search
//
int search_unnormalized_cdf(const vector<double>& cdf, int start, int stop, double target) {
	int pivot = start + (stop - start) / 2;

	if (pivot > 0 && target < cdf[pivot-1]) {
		return search_unnormalized_cdf(cdf, start, pivot, target);
	} else if (target > cdf[pivot]) {
		return search_unnormalized_cdf(cdf, pivot+1, stop, target);
	} else {
		return pivot;
	}
}

