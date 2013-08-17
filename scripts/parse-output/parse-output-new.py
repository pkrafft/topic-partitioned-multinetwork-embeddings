import os
import shutil
import sys
import optparse

parser = optparse.OptionParser()
parser.set_defaults(verbose=True, debug=False, enron=False)
parser.add_option("-n", "--nhc", action="store_true", dest="nhc")
parser.add_option("-e", "--enron", action="store_true", dest="enron")
(options, args) = parser.parse_args()

nhc = options.nhc
enron = options.enron

main_folder = '/lustre/work1/wallach/pkrafft'

n_topics = [1, 2, 5, 30, 50, 75, 100, 150, 200]
repetitions = 5
if nhc:
    top_output_dir = main_folder + '/output-nhc'
if enron:
    top_output_dir = main_folder + '/output-enron'

iter_limit = '50000'

# def search_iters(dir):
#     last_iter = 0
#     for f in os.listdir(dir):
#         s = f.split('.')
#         if s[0] == 'stdout' and len(s) == 3:
#             n = int(s[1])
#             if(n > last_iter and n < iter_limit):
#                 last_iter = n
#    return str(last_iter)

def parse_score(experiment, id, line_from_end = 1):
    dir = top_output_dir + '/' + experiment + '-' + id
    #i = search_iters(dir)
    i = iter_limit
    out_file = dir + '/stdout.' + i + '.txt.eval'
    print out_file
    #if i == '0' and not os.path.exists(out_file):
    #    out_file = dir + '/stdout.txt'
    out_file = open(out_file)
    lines = out_file.readlines()
    score = lines[len(lines) - line_from_end]
    score = score.split(':')[1].strip()
    return score

coherence_file = open(top_output_dir + '/coherence.csv', 'w')
f_file = open(top_output_dir + '/fscore.csv', 'w')
ll_file = open(top_output_dir + '/heldoutll.csv', 'w')

offset = 0
for i in range(repetitions):

	i += offset

	for t in n_topics:

            id = str(i) + '-' + str(t)
            
            experiment = 'LDAExperiment'
            score = parse_score(experiment, id, 2)
            coherence_file.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')

            id = str(i) + '-' + str(t) + '-missing'
            
            experiment = 'ConditionalStructureExperiment'
            score = parse_score(experiment, id, 8)
            coherence_file.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')
            score = parse_score(experiment, id, 2)
            f_file.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')
            score = parse_score(experiment, id, 1)
            ll_file.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')

            experiment = 'BernoulliEroshevaExperiment'
            score = parse_score(experiment, id, 8)
            coherence_file.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')
            score = parse_score(experiment, id, 2)
            f_file.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')
            score = parse_score(experiment, id, 1)
            ll_file.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')

            experiment = 'ConditionalStructureBernoulliExperiment'
            score = parse_score(experiment, id, 8)
            coherence_file.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')
            score = parse_score(experiment, id, 2)
            f_file.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')
            score = parse_score(experiment, id, 1)
            ll_file.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')

            id = str(i) + '-1-' + str(t) + '-missing'
            
            experiment = 'MMLSEMExperiment'
            score = parse_score(experiment, id, 2)
            f_file.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')
            score = parse_score(experiment, id, 1)
            ll_file.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')

