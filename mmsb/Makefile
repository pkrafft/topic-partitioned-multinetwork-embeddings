all: mmsb reload_mmsb

mmsb: categorical_sample.cpp corpus.cpp main.cpp mmsb.cpp
	g++ -g -o mmsb categorical_sample.cpp main.cpp mmsb.cpp -lboost_serialization corpus.cpp
reload_mmsb: categorical_sample.cpp corpus.cpp reload.cpp mmsb.cpp
	g++ -g -o reload_mmsb categorical_sample.cpp reload.cpp mmsb.cpp -lboost_serialization corpus.cpp
test: categorical_sample.cpp  corpus.cpp  main.cpp  mmsb.cpp  test.cpp
	g++ -g -o test categorical_sample.cpp  corpus.cpp mmsb.cpp  test.cpp -lboost_serialization


