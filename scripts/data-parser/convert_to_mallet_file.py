#!/usr/bin/python

import sys
import os
import re
import pickle

vocab = {}

def read_address_list(authors_file):
	return map(lambda x: x.split()[1].strip().lower(), open(authors_file).readlines())

def filter_email(email, address_list):
	email['from'] = filter_addresses(email['from'], address_list)
	email['to'] = filter_addresses(email['to'], address_list)
	email['cc'] = filter_addresses(email['cc'], address_list)

	# IF email['from'][0] in email['to'] or email['cc'] remove it!

	if email['from']:
		addr = email['from'][0]

		# Remove "From" address from "To" and "CC"
		email['to'] = filter(lambda x: x != addr, email['to'])
		email['cc'] = filter(lambda x: x != addr, email['cc'])

	if email['from'] and (email['to'] or email['cc']):
		message = email['subject'] + '\n\n' + email['message']
		tokens = message.split()
		email['tokens'] = map(map_token, tokens)

		if re.search('sent', email['file']):
			return email
		
		return

def filter_addresses(addresses, address_list):
	return filter(lambda x: x in address_list, map(lambda x: x.lower(), addresses))

def map_token(token):
	if token not in vocab:
		vocab[token] = len(vocab)
	
	return vocab[token]

if __name__ == '__main__':
	if len(sys.argv) < 4:
		print >> sys.stderr, "Usage: " + sys.argv[0] + " pickle_file authors_file OUT_DIR"
		sys.exit()

	pickle_file = sys.argv[1]
	authors_file = sys.argv[2]
	OUT_DIR = sys.argv[3]

	if not os.path.isdir(OUT_DIR):
		print >> sys.stderr, OUT_DIR + " is not a directory"
		sys.exit()

	address_list = read_address_list(authors_file)
	print address_list
	
	emails = pickle.load(open(pickle_file))
	
	print "Processing emails..."
	messages = []
	for email in emails:
		email = filter_email(email, address_list)
		if email != None:
			messages.append(email)
	
	print len(messages), "messages"
	print "Writing output files..."
	
	mallet_file = open(os.path.join(OUT_DIR, 'mallet_corpus'), 'w')
	mallet_file.write(str(len(address_list)) + ',' + str(len(vocab)))

	first = True
	for email in messages:
		if first:
			first = False
		else:
			mallet_file.write('\n');
		tokens = email['tokens']
		mallet_file.write(','.join(map(vocab.get, email['tokens'])));
	
	print "Done!"
	
	corpus_file.close()
	summary_file.close()
	vocab_file.close()

