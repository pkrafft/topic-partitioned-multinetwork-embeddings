#!/usr/bin/python

"""
Given an input directory, a stopword file, and an output location,
this script parses the email messages in the directory and writes
a pickle file that can subsequently be consumed by one of the
matrix writer scripts.
"""

import sys
import os
import re
import pickle
import email.parser
import email.utils
import time

import text_utilities

__DEBUG__ = False

parser = email.parser.Parser()

file_re = re.compile('^\d+\.?$')

def parse_path(path, stop_words, filter_addr_fn = None):
	emails = []
	if os.path.isdir(path):
		for filename in os.listdir(path):
			emails += parse_path(os.path.join(path, filename), stop_words, filter_addr_fn = filter_addr_fn)
	else:
		filename = os.path.basename(path)
		if not file_re.match(filename):
			if __DEBUG__:
				print >> sys.stderr, "File " + filename + " is not in the expected \d+. filename format. Skipping..."
		else:
			e = parser.parse(open(path))
			e = process_email(e, stop_words, filter_addr_fn = filter_addr_fn)

			if e != None:
				e['file'] = path
				emails = [e]

	return emails

unique_emails = set()

def process_email(email, stop_words, filter_addr_fn = None):
	e = {}
	e['from'] = parse_addresses(email.get_all('From'))
	if e['from'] == []:
		e['from'] = parse_addresses(email.get_all('X-From'))
#	I'm not sure about this tag and we don't use it anyway
#	e['senders'] = parse_addresses(email.get_all('Sender'))
	e['to'] = parse_addresses(email.get_all('To'))
	if e['to'] == []:
		e['to'] = parse_addresses(email.get_all('X-To'))
	e['cc'] = parse_addresses(email.get_all('Cc'))
	if e['cc'] == []:
		e['cc'] = parse_addresses(email.get_all('X-cc'))

	if filter_addr_fn:
		e['from'] = filter_addr_fn(e['from'])
		e['to'] = filter_addr_fn(e['to'])
		e['cc'] = filter_addr_fn(e['cc'])

#	if not e['from'] or (not e['to'] and not e['cc']):
#		print "Skipping..."
#		return None

	e['date'] = email.get('date')
	e['subject'] = parse_text(email.get('subject'), stop_words)

	# Check that this is a unique message
	meta_data = str([e['from'], e['to'], e['cc'], e['subject'], e['date']])
	if meta_data in unique_emails:
		return None
	else:
		unique_emails.add(meta_data)

#	I don't know about the Thread ID and we aren't using it for
#	this project anyway
#	e['thread'] = (email.get('Thread-Index' or email.get('thread-index')))

	# Extract the plaintext message from the tree of different body MIME types
	message = email
	while True:
		try:
			message = message.get_payload(0)
		except:
			break
	message = message.get_payload()
	e['message'] = parse_message(message, stop_words)

	return e

address_re1 = re.compile('\s*,\s*')
address_re2 = re.compile('<\.?(.*)>')
address_re3 = re.compile('^[a-zA-Z0-9&\'*+_\-.@/=?^{}~]+@[a-zA-Z0-9_\-.~]+\.[a-zA-Z]+$')

def parse_addresses(string):
	addresses = []

	if string != None:
		strings = address_re1.split(string[0])
		for s in strings:
			# Extract email address from strings of the form "Jim Smith" <jsmith@example.com>
			match = address_re2.search(s)
			if match:
				s = match.group(1)

			# Search for illegal characters (especially spaces)
			if address_re3.match(s):
				addresses.append(s.lower())
			elif __DEBUG__:
				print >> sys.stderr, "Bad email string: " + s

	return addresses

filter_re1 = re.compile('@enron.com$')

def filter_addresses(addr):
	return filter(lambda x: filter_re1.search(x), addr)

message_re1 = re.compile('\S[IMAGE]\S')
message_re2 = re.compile('[\n\r]+')
# I'm not going to remove forward headers, at Peter's request
#message_re3 = re.compile('\s*(?:[<|]?[-=_*.]{8}|-+-alt---boun|-+---boundary|-+----\s?Inline|\??-+----\s?Origina|-+----\s?Forwarded |-+----\s?Begin forward|Enron on .*-+)', flags=re.IGNORECASE)
message_re3 = re.compile('\s*(?:[<|]?[-=_*.]{8}|-+-alt---boun|-+---boundary|-+----\s?Inline|\??-+----\s?Origina|Enron on .*-+)', flags=re.IGNORECASE)
message_re4 = re.compile('\s*(?:From:|To:)')
message_re5 = re.compile('\s*>')

def parse_message(message, stop_words):
	# Remove all image tags
	message = message_re1.sub('', message)

	# Split the message on newline boundaries
	lines = message_re2.split(message)

	result = []
	for i in range(len(lines)):
		# Look for forward/reply-to delimiters and cut off the message
		if message_re3.match(lines[i]):
			break

		# Sometimes there are no delimiters except a 
		# On Tuesday ... blah blah
		# From: 
		elif len(lines) > i+1 and message_re4.match(lines[i+1]):
			break
		# Look for lines beginning with >, as they are probably quoted text
		elif not message_re5.match(lines[i]):
			result.append(lines[i])

	# Tokenize
	message = parse_text('\n'.join(result), stop_words)

	return message

def parse_text(line, stop_words):
	# Exact copy of Peter's original method
	line = line.strip()
	words = line.split(' ')
	url_tags = ['<htt', 'http', '<www', 'www.', '.com','.net','.org','.gov',
		'.edu','.com>', '.net>','.org>','.gov>','.edu>']
	remove = map(lambda x: x[:4] in url_tags or x[-4:] in url_tags or
		x[-5:] in url_tags, words)
	words = [words[i] for i in range(len(words)) if not remove[i]]
	junk_tags = ['!SIG']
	remove = map(lambda x: x[:4] in junk_tags, words)
	words = [words[i] for i in range(len(words)) if not remove[i]]
	junk_tags = ['DTSTAMP:','CREATED:','LAST-MOD','DTSTART;','DTEND;VA']
	remove = map(lambda x: x[:8] in junk_tags, words)
	words = [words[i] for i in range(len(words)) if not remove[i]]
	line = ' '.join(words)
	line = text_utilities.nice(line, stop_words)
	return(line)

def compare_email_dates(a, b):
	"""
	Compares email-format dates for sorting
	"""
	a_date = time.mktime(email.utils.parsedate(a['date']))
	b_date = time.mktime(email.utils.parsedate(b['date']))

	return cmp(a_date, b_date)

if __name__ == '__main__':
	if len(sys.argv) < 4:
		print >> sys.stderr, "Usage: " + sys.argv[0] + " IN_DIR STOP_WORDS_FILE OUT_PICKLE_FILE"
		sys.exit()

	IN_DIR = sys.argv[1]
	STOP_WORDS_FILE = sys.argv[2]
	OUT_PICKLE_FILE = sys.argv[3]

	if not os.path.isdir(IN_DIR):
		print >> sys.stderr, IN_DIR + " is not a valid directory"
		sys.exit()

	stop_words = []
	stop_word_file = open(STOP_WORDS_FILE)
	for l in stop_word_file:
		stop_words += [l.strip()]
	stop_word_file.close()

	emails = []

	for folder in os.listdir(IN_DIR):
		print '-' * (len(folder) + 12)
		print 'Parsing ' + folder + ' ...'
		print '-' * (len(folder) + 12)

		emails += parse_path(os.path.join(IN_DIR, folder), stop_words)

	print "Emails: " + str(len(emails))

	print "Sorting (this may take a while...)"

	emails = sorted(emails, cmp=compare_email_dates)

	print "Saving pickle..."

	pickle_file = open(OUT_PICKLE_FILE, 'w')
	pickle.dump(emails, pickle_file)
	pickle_file.close()

