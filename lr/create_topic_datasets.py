#!/usr/bin/python

import sys
import numpy

if len(sys.argv) < 3:
	print >> sys.stderr, 'Usage: word_file topic_file edge_file'
	sys.exit()

word_file = sys.argv[1]
topic_file = sys.argv[2]
edge_file = sys.argv[3]

#words = numpy.array(map(lambda x: map(int, x.strip().split(',')[1:]), open(word_file).readlines()))
edges = numpy.array(map(lambda x: map(int, x.strip().split(',')[1:]), open(edge_file).readlines()))

D = edges.shape[0]

topics = numpy.zeros([D, 150])
i = 0
for line in open(topic_file).readlines()[1:]:
	topic_data = numpy.array(map(float, line.strip().split('\t')[2:]))
	assignments = numpy.array(topic_data[::2], dtype=int)
	counts = topic_data[1::2]
	
	topics[i,assignments] = counts
	i += 1

print topics

# Remove author column
authors = edges[:,0]
edges = edges[:,1:]

#idf = numpy.log(D / (words > 0).sum(0))

#words = words * idf

A = edges.shape[1]

for r in range(A):
#	# ADDING AUTHOR AS A FEATURE
#	data = numpy.concatenate((words, edges[:,r].reshape(-1, 1)), axis=1)
#	print topics.shape
	topic_data = numpy.concatenate((topics, edges[:,r].reshape(-1, 1)), axis=1)
#	data = numpy.concatenate((authors.reshape(-1, 1), words, edges[:,r].reshape(-1, 1)), axis=1)

	mask = numpy.transpose(authors != r)
	mask = mask.nonzero()[0]
#	data = data[mask,:]
	topic_data = topic_data[mask,:]
#	print r, ' : ', mask.shape[0], ' : ', words.shape[0] - mask.shape[0], ' : ', data[:,-1].sum()

	# Output in CSV format
#	open('out/' + str(r) + '.csv', 'w').write('\n'.join([','.join(map(str, x)) for x in data]))
	open('out_topics/' + str(r) + '-topics.csv', 'w').write('\n'.join([','.join(map(str, x)) for x in topic_data]))


