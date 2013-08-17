
import sys
filename = sys.argv[1]

f = open(filename)

exp = []
rep = []
par = []
score = []

last_name = ""
for line in reversed(f.readlines()):
    l = line.split(':')
    name = l[0].split('/')[2]
    if name != last_name:
        split_name = name.split('-')
        if len(split_name) > 3:
            exp += [split_name[0] + split_name[3]]
        else:
            exp += [split_name[0]]
        rep += [split_name[1]]
        if len(split_name) > 2:
            par += [split_name[2]]
        else:
            par += ['-1']
        score += [l[2].strip()]
    last_name = name

f.close()

g = open(filename + '.csv', 'w')

for i in range(len(exp)):
    g.write(','.join([exp[i],rep[i],par[i],score[i]]) + '\n') 

g.close()
