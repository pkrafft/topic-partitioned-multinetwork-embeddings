# this class runs all the edge frequency baseline experiments. it is a
# separate script because the code is structured slightly differently
# from the rest of the Java package

import os
import optparse

JAVA_COMMAND = 'java'

parser = optparse.OptionParser()
parser.set_defaults(parse_output=False, check_status=False)
parser.add_option("-d", "--debug", action="store_true", dest="debug")
parser.add_option("-p", "--parse_output", action="store_true", dest="parse_output")
(options, args) = parser.parse_args()

debug = options.debug
parse = options.parse_output

DATASETS = {
	'nhc': '/lustre/work1/wallach/pkrafft/data/nhc/edge-matrix.csv',
	'enron': '/lustre/work1/wallach/pkrafft/data/enron/edge-matrix.csv'
}

NUM_ACTORS = ['30', '50']

repetitions = 5
if debug:
    output_base = '/lustre/work1/wallach/pkrafft/debug/output'
else:
    output_base = '/lustre/work1/wallach/pkrafft/output'

def parse_score(name, line_from_end = 2):
    dir = name
    out_file = dir + '/stdout.txt'
    print out_file
    out_file = open(out_file)
    lines = out_file.readlines()
    score = lines[len(lines) - line_from_end]
    score = score.split(':')[1].strip()
    return score

def write_line(filename, experiment, i, score):
    filename.write(experiment + ',' + str(i) + ',' + score + '\n')


offset = 0

for d in range(len(DATASETS)):

    dataset = DATASETS.keys()[d]
    num_actors = NUM_ACTORS[d]

    top_output_dir = output_base + "-" + dataset
    if parse:
        f_file = open(top_output_dir + '/fscore-efe.csv', 'w')
    
    for i in range(repetitions):
        
        name = "/EdgeFrequencyExperiment"
        name += "-" + str(i + offset)
        
        out_dir = output_base + "-" + dataset + name
        
        if not os.path.exists(out_dir):
            os.mkdir(out_dir)
            
        if parse:
            score = parse_score(out_dir)
            write_line(f_file,'EdgeFrequencyExperiment',i,score)
        else:
            out_file = out_dir + "/stdout.txt"
            command = JAVA_COMMAND
            command += " -cp ./build/jar/NetworkModels.jar"
            command += " experiments.EdgeFrequencyExperiment"
            command += " " + DATASETS[dataset]
            command += " " + num_actors
            command += " true"
            command += " -1 -1"
            command += " > " + out_file
            print command
            os.system(command)
        
    if parse:
        f_file.close()


