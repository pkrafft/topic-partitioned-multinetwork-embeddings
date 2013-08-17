#!/usr/bin/python

import sys
import os
import pickle

from email.utils import parsedate
import time

vocab = {}

def read_address_list(authors_file):
	return map(lambda x: x.split()[1].strip().lower(), open(authors_file).readlines())

def filter_email(email, address_list, cutoff):
	email['from'] = filter_addresses(email['from'], address_list)
	email['to'] = filter_addresses(email['to'], address_list)
	email['cc'] = filter_addresses(email['cc'], address_list)

	# IF email['from'][0] in email['to'] or email['cc'] remove it!

	if email['from']:
		addr = email['from'][0]
		email['to'] = filter(lambda x: x != addr, email['to'])
		email['cc'] = filter(lambda x: x != addr, email['cc'])

	if email['from'] and (email['to'] or email['cc']):
		message = email['subject'] + '\n\n' + email['message']
		tokens = message.split()
		date = time.mktime(parsedate(email['date']))
		email['past_cutoff'] = (date >= cutoff)
		print date
		print email['past_cutoff']
		email['tokens'] = filter(lambda x: x != None, map(lambda x: map_token(x, not email['past_cutoff']), tokens))

		return email

def filter_addresses(addresses, address_list):
	return filter(lambda x: x in address_list, map(lambda x: x.lower(), addresses))

def map_token(token, add):
	if token not in vocab:
		if add:
			vocab[token] = len(vocab)
		else:
			return None
	
	return vocab[token]

if __name__ == '__main__':
	if len(sys.argv) < 4:
		print >> sys.stderr, "Usage: " + sys.argv[0] + " pickle_file authors_file cutoff OUT_DIR"
		sys.exit()

	pickle_file = sys.argv[1]
	authors_file = sys.argv[2]
	cutoff = int(sys.argv[3])
	OUT_DIR = sys.argv[4]

	if not os.path.isdir(OUT_DIR):
		print >> sys.stderr, OUT_DIR + " is not a directory"
		sys.exit()

	address_list = read_address_list(authors_file)
	print address_list
	
	emails = pickle.load(open(pickle_file))
	
	print "Processing emails..."
	messages = []
	total = 0
	past_cutoff = 0
	for email in emails:
		email = filter_email(email, address_list, cutoff)
		if email != None:
			total += 1
			if email['past_cutoff']:
				past_cutoff += 1
			messages.append(email)
	
	print str(total) + ' emails total'
	print str(past_cutoff) + ' emails past cutoff'
	print "Writing output files..."
	
	vocab_file = open(os.path.join(OUT_DIR, 'vocab.txt'), 'w')
	vocab = sorted(vocab.keys(), key=vocab.get)
	vocab_file.write('\n'.join(vocab))
	vocab_file.close()
	
	word_file = open(os.path.join(OUT_DIR, 'word-matrix.csv'), 'w')
	connections_file = open(os.path.join(OUT_DIR, 'edge-matrix.csv'), 'w')

	summary_file = open(os.path.join(OUT_DIR, 'summary.tab'), 'w')
	summary_file.write("File\tFrom\tToCC\tDate\tSubject\tTokens\tPast Cutoff\n")

	for email in messages:
		summary_file.write('{0}\t{1}\t{2}\t{3}\t{4}\t{5}\t{6}\n'.format(email['file'], email['from'][0], len(email['to']) + len(email['cc']), email['date'], email['subject'], len(email['tokens']), email['past_cutoff']))

		tokens = email['tokens']
		word_file.write(email['file'])
		for word in range(len(vocab)):
			count = len(filter(lambda x: x == word, tokens))
			word_file.write(',' + str(count))
		word_file.write('\n')
	
		connections_file.write(email['file'])
		connections_file.write(',' + str(address_list.index(email['from'][0])))
		for address in address_list:
			if address in email['to'] or address in email['cc']:
				connections_file.write(',1')
			else:
				connections_file.write(',0')
		connections_file.write('\n')
	
	print "Done!"
	
	summary_file.close()
	word_file.close()
	connections_file.close()

