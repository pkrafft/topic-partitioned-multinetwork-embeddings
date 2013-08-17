############################ WARNING: ###############################
# run this script only once. if you run it again while jobs are still
# running, you will ruin everything!
#####################################################################

# this class runs all the mmsb experiments. it is a separate script
# because the mmsb code is written in C++ whereas the rest of the code
# is writte in Java, and each code base works a little differently

import os
import optparse
import time

parser = optparse.OptionParser()
parser.set_defaults(parse_output=False, check_status=False)
parser.add_option("-d", "--debug", action="store_true", dest="debug")
parser.add_option("-p", "--parse_output", action="store_true", dest="parse_output")
(options, args) = parser.parse_args()

debug = options.debug
parse = options.parse_output

DATASETS = {
	'nhc': '/lustre/work1/wallach/pkrafft/data/nhc/corpus',
	'enron': '/lustre/work1/wallach/pkrafft/data/enron/corpus'
}
if debug:
	K = [1, 2]
	repetitions = 2
	iterations = 30
	batch_size = 10
	save_state_interval = 10
	output_base = '/lustre/work1/wallach/pkrafft/debug/output-mmsb'
else:
	K = [1, 2, 5, 10, 15, 30]
	repetitions = 5
	iterations = 5000
	batch_size = 100
	save_state_interval = 100
	output_base = '/lustre/work1/wallach/pkrafft/output-mmsb'


if not  os.path.exists(output_base + '-nhc'):
	os.mkdir(output_base + '-nhc')
if not  os.path.exists(output_base + '-enron'):
	os.mkdir(output_base + '-enron')

alpha = 1e-1
beta_d = 1e-1
beta_od = 1e-2

# find the last completed iteration in which a state was saved
def search_iters(dir):
	last_iter = 0
        for f in os.listdir(dir):
		s = f.split('.')
		if len(s) == 2 and (s[1] == 'model_state' and s[0] != 'corpus'):
			n = int(s[0])
			if(n > last_iter):
				last_iter = n
	return str(last_iter)

def parse_score(name, line_from_end = 1):
    dir = name
    out_file = dir + '/stdout.txt'
    print out_file
    out_file = open(out_file)
    lines = out_file.readlines()
    score = lines[len(lines) - line_from_end]
    score = score.split(':')[1].strip()
    return score

def write_line(filename, experiment, i, t, score):
    filename.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')

offset = 0
for k in K:
	for i in range(repetitions):
		for dataset in DATASETS:

			name = "/MMSBExperiment"
			name += "-" + str(i + offset)
			name += "-" + str(k)
			
			out_dir = output_base + "-" + dataset + name
			if os.path.exists(out_dir):
				iter = search_iters(out_dir)
			else:
				iter = '0'
				os.mkdir(out_dir)

			top_output_dir = output_base + "-" + dataset
			if parse:
				f_file = top_output_dir + '/fscore.csv'
				if os.path.exists(f_file):
					f_file = open(top_output_dir + '/fscore.csv', 'a')
				else:
					f_file = open(top_output_dir + '/fscore.csv', 'w')
				score = parse_score(out_dir)
				write_line(f_file,'MMSBExperiment',i,k,score)
				f_file.close()
			else:
				out_file = out_dir + "/stdout.txt"
				err_file = out_dir + "/stderr.txt"
				
				command = "qsub -cwd"
				command += " -o " + out_file
				command += " -e " + err_file
				command += " -l mem_token=100M"
				command += " -l mem_free=100M"
				command += " -l long=TRUE"

				time.sleep(0.1)
				if iter != '0':
					if int(iter) < iterations:
						command += " ./scripts/restart-mmsb-job.sh"
						command += " " + out_dir + '/corpus.model_state'
						command += " " + out_dir + '/' + iter + '.model_state'
						command += " " + str(iterations)
						command += " " + str(batch_size)
						command += " " + str(save_state_interval)
						command += " " + out_dir
						print command
						os.system(command)
				else:
					command += " ./scripts/run-mmsb-job.sh"
					command += " " + DATASETS[dataset]
					command += " " + str(k)
					command += " " + str(iterations)
					command += " " + str(batch_size)
					command += " " + str(save_state_interval)
					command += " " + out_dir
					command += " " + str(alpha)
					command += " " + str(beta_d)
					command += " " + str(beta_d)
					command += " " + str(beta_od)
					command += " " + str(beta_od)					
					print command
					os.system(command)

