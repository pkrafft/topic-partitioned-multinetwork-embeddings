import pickle
import re

emails = pickle.load(open('pickle'))

addr = {}
for e in emails:
	if (re.search('sent', e['file'])):
		if len(e['from']) != 1:
			print "Unexpected `from` length:", len(e['from'])

		a = e['from'][0]

		if re.search('@enron.com', a):
			to = filter(lambda r: re.search('@enron.com', r), e['to'] + e['cc'])
			if to:
				if a not in addr:
					addr[a] = {}
				for r in to:
					if r not in addr[a]:
						addr[a][r] = 0
					addr[a][r] += 1

print len(addr)

changed = 1
while changed:
	changed = 0
	print "Changed"
	for a in addr.keys():
		for r in addr[a].keys():
			if r not in addr.keys():
				del addr[a][r]
				changed = 1
	for a in addr.keys():
		if len(addr[a]) == 0:
			del addr[a]

degree = {}
for a in addr.keys():
	for r in addr[a].keys():
		if a not in degree:
			degree[a] = 0
		degree[a] += addr[a][r]
		if r not in degree:
			degree[r] = 0
		degree[r] += addr[a][r]

i = 0
s = 0
for a in sorted(degree.keys(), key=degree.get, reverse=1):
	i += 1
	s += degree[a]
	print i, a, degree[a], s/2

exit()

print '\n'.join(addr) + '\n'

print "Total messages:", count, "\n";

for a in sorted(addr, cmp=lambda x, y: cmp(len(addr[x]), len(addr[y]))):
	print len(addr[a])

exit()

i = 0
s = 0
for a in sorted(set(author) & set(recipient), key=addr.get, reverse=1):
	i += 1
	if i > 50:
		break
	print i, "\t", a, "\t", addr[a], "\t", author[a], "\t", recipient[a], "\t", max(author[a], recipient[a])
	s += addr[a]

print "Total: ", s

