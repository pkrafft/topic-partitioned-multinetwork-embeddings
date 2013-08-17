import os
import shutil
import sys
import optparse

parser = optparse.OptionParser()
parser.set_defaults(debug=False)
parser.add_option("-d", "--debug", action="store_true", dest="debug")
(options, args) = parser.parse_args()

manager_subset = 'between-managers'

edge_file = '../data/nhc_email_data/' + manager_subset + '-connections.txt'
n_authors = '30'

top_output_dir = '../output-baseline'

debug = options.debug

if debug:
	repetitions = 2
	n_iterations = '3'
	long = False
else:
	repetitions = 20
	n_iterations = '10'
	long = False

def base_command(output_dir, long):
	command = 'qsub -cwd -o ' + output_dir + '/stdout.txt'
	command += ' -e ' + output_dir + '/stderr.txt'
	command += ' -l mem_free=2G'
	if(long and not debug):
		command += ' -l long=TRUE'
	command += ' ./scripts/run-job.sh '
	return command

def mkdir_f(output_dir):
	if os.path.exists(output_dir):
		shutil.rmtree(output_dir)
	os.mkdir(output_dir)

def run_experiment(experiment, id, args, long):
	output_dir = top_output_dir + '/' + experiment + '-' + id
	experiment = 'experiments.' + experiment
	mkdir_f(output_dir)
	command = ' '.join([base_command(output_dir, long)] + [experiment] + args)
	os.system(command)	

if not os.path.exists(top_output_dir):
	os.mkdir(top_output_dir)

os.system('ant')

for i in range(repetitions):
	id = str(i)
	args = [edge_file, n_authors]
	run_experiment('EdgeFrequencyExperiment', id, args, long)
