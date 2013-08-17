#ifndef __JMOORE_CORPUS_H__
#define __JMOORE_CORPUS_H__

#include <vector>
#include <string>
#include <fstream>
#include <sstream>

#include <boost/serialization/vector.hpp>

using namespace std;

class mmsbTest;

class Corpus {
private:
	friend class boost::serialization::access;

	template<class Archive>
	void serialize(Archive & ar, const unsigned int version) //;
{
	ar & A;
	ar & V;
	ar & edges;
	ar & messageObscured;
}


protected:
	int A, V;
	vector<vector<vector<bool> > > edges;
//	vector<vector<vector<bool> > > edgeObscured;
	vector<vector<bool> > messageObscured;
public:
	Corpus();
	Corpus(int A, int V);
	Corpus(istream &file);
//	int obscureRandomEdges(double p);
	int obscureRandomMessages(double p);
	vector<vector<vector<bool> > > getEdges();
	bool getEdge(int a, int d, int r);
//	bool isObscured(int a, int d, int r);
	bool isObscured(int a, int d);
	std::string printFlatNetwork();
	int numObscuredMessages();

	friend class mmsbTest;
};

// TODO read word data

#endif
