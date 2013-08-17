#!/usr/bin/python

#
# Code for running logistic text regression classifier
#

import sys
import os
import shlex, subprocess
import numpy

def run(filename, N, name):
	subprocesses = []
	test_true = []
	for i in range(N):
		print '\tIteration ' + str(i)
		rows = numpy.array(map(str.strip, open(filename).readlines()))
		mask = numpy.random.rand(len(rows)) > 0.9
		mask_c = (1 - mask).nonzero()[0]
		mask = mask.nonzero()[0]

		train_rows = rows[mask_c]
		test_rows = rows[mask]

		model = name + '-' + str(i)

		print '\tTraining: ' + str(len(train_rows)) + ', Testing: ' + str(len(test_rows)) + ', Total: ' + str(len(rows))

		open('/tmp/train-' + model + '.csv', 'w').write('\n'.join(train_rows))
		open('/tmp/test-' + model + '.csv', 'w').write('\n'.join(test_rows))

		args = shlex.split('/home/jmoore/Downloads/lr_trirls_20060531/programs/train in /tmp/train-' + model + '.csv save /tmp/model-' + model)
		subprocesses.append(subprocess.Popen(args))

		test_true.append(numpy.array(map(lambda x: bool(float(x.split(',')[-1])), test_rows)))

	results = numpy.zeros([N, 4])
	for i in range(N):
		subprocesses[i].wait()

		model = name + '-' + str(i)

		os.system('/home/jmoore/Downloads/lr_trirls_20060531/programs/predict in /tmp/test-' + model + '.csv load /tmp/model-' + model + ' pout /tmp/predictions-' + model)

		predictions = numpy.array(map(float, open('/tmp/predictions-' + model).readlines()))
		test_pred = numpy.random.rand(*predictions.shape) < predictions

		tp = 1.0 * ((test_pred == True) * (test_true[i] == True)).sum()
		fp = 1.0 * ((test_pred == True) * (test_true[i] == False)).sum()
		tn = 1.0 * ((test_pred == False) * (test_true[i] == False)).sum()
		fn = 1.0 * ((test_pred == False) * (test_true[i] == True)).sum()

		print '\t', tp, fp, tn, fn

		results[i] = numpy.array((tp, fp, tn, fn))

	return results

if __name__ == '__main__':
	if len(sys.argv) < 2:
		print >> sys.stderr, "Usage: " + sys.argv[0] + ' splits'
		sys.exit()

	N = int(sys.argv[1])

	results = numpy.zeros([N, 4])
	for r in range(30):
		print 'Recipient ' + str(r)
		filename = 'out/' + str(r) + '-topics.csv'
		results += run(filename, N, 'topics-' + str(r))

	print results

	tp = results[:,0]
	fp = results[:,1]
	tn = results[:,2]
	fn = results[:,3]

	precision = tp / (tp + fp + 1e-200)
	recall = tp / (tp + fn + 1e-200)
	Fscore = 2 * precision * recall / (precision + recall + 1e-200)

	print 'Fscore: ' + str(Fscore)

