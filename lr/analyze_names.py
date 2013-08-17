#!/usr/bin/python

import os
import numpy

managers = (
	('ahovis@nhcgov.com', 'Alice Hovis'),
	('alhight@nhcgov.com', 'Al Hight'),
	('amallette@nhcgov.com', 'Andre Mallette'),
	('apinder@nhcgov.com', 'Avril Pinder'),
	('bshell@nhcgov.com', 'Bruce Shell'),
	('bwilliams@nhcgov.com', 'Bonnie Williams'),
	('ccoudriet@nhcgov.com', 'Chris Coudriet'),
	('cgriffin@nhcgov.com', 'Cam Griffin'),
	('cokeefe@nhcgov.com', 'Chris Keefe O\'Keefe'),
	('dhall@nhcgov.com', 'Donnie Hall'),
	('dihnat@nhcgov.com', 'Dennis Ihnat'),
	('drice@nhcgov.com', 'David Rice'),
	('emcmahon@nhcgov.com', 'Edward McMahon Mc Mahon'),
	('epinder@nhcgov.com', 'Ellis Pinder'),
	('htuchmayer@nhcgov.com', 'Harry Tuchmayer'),
	('jfennell@nhcgov.com', 'Jerome Fennell'),
	('jhardison@nhcgov.com', 'Jennifer Hardison MacNeish Mac Neish'),
	('jhubbard@nhcgov.com', 'John Hubbard'),
	('jiannucci@nhcgov.com', 'Jim Iannucci'),
	('jmcdaniel@nhcgov.com', 'Jim McDaniel Mc Daniel'),
	('kstoute@nhcgov.com', 'Kathy Stoute'),
	('lnesmith@nhcgov.com', 'LaVaughn La Vaughn Nesmith'),
	('lstanfield@nhcgov.com', 'Leslie Stanfield'),
	('rhaas@nhcgov.com', 'Ruth Haas'),
	('rkelley@nhcgov.com', 'Roger Kelley'),
	('sschult@nhcgov.com', 'Sheila Schult'),
	('tallen@nhcgov.com', 'Tiffany Allen'),
	('troberts@nhcgov.com', 'Tony Roberts'),
	('wcopley@nhcgov.com', 'Wanda Copley'),
	('wlee@nhcgov.com', 'Warren Lee'),
)

vocab = map(str.strip, open('../data/nhc/input/vocab.txt').readlines())

for f in sorted(os.listdir('results')):
	if f[:12] == 'model-words-':
		lines = open('results/' + f).readlines()
		intercept = float(lines[1])
		word_coeffs = numpy.array([x.strip() for x in lines[2:]], dtype=float)

		manager = int(f.split('-')[2])

		if manager < len(managers):
			names = managers[manager][1].lower().split()
			print f, 'Intercept:', intercept, 'Mean:', numpy.abs(word_coeffs).mean(),
			for name in sorted(names):
				print name + ':',
				if name in vocab:
					print word_coeffs[vocab.index(name)],
				else:
					print '---',
			print

