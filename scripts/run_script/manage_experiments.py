#!/usr/bin/python

import os
import distutils.dir_util
import sys
import re

def run_qstat():
	global JOB_STATUS
	JOB_STATUS = {}
    
	qstat = os.popen('qstat')
	for line in qstat.readlines()[2:]:
		line = line.split()
		jid = line[0]
		status = line[4]
		JOB_STATUS[int(jid)] = status
	
def get_jid(output_dir):
	try:
		return int(open(os.path.join(output_dir, 'jid')).readline())
	except:
		return None

def is_complete(output_dir):
	try:
		return bool(open(os.path.join(output_dir, 'completed')).readline())
	except:
		return False

def experiment_status(output_dir):
	if 'JOB_STATUS' not in globals():
		run_qstat()

	if not os.path.exists(output_dir):
		return 'DNE'
	elif not os.path.isdir(output_dir):
		return 'Bad Path'
	elif is_complete(output_dir):
		return 'Complete'
	else:
		jid = get_jid(output_dir)

		if jid == None:
			return 'Died Immediately'
		elif jid in JOB_STATUS:
			return JOB_STATUS[jid]
		else:
			return 'Died'

def run_experiment(experiment):
	num_started = 0
	errors = 0
	for i in range(experiment.repetitions):
		output_dir = os.path.join(experiment.output_dir(), str(i))
		print output_dir
		status = experiment_status(output_dir)

		if status != 'DNE':
			print 'Experiment ' + str(experiment) + ' repetition ' + str(i) + ' already exists (status ' + status + '). Not starting.'
			return 0

		distutils.dir_util.mkpath(output_dir)

		# TODO Give the job a useful name

		# TODO run_job arguments
		line = os.popen('qsub -cwd -o ' + os.path.join(output_dir, 'stdout') + ' -e ' + os.path.join(output_dir, 'stderr ') + \
			experiment.qsub_args() + ' ' + experiment.run_command() + ' ' + experiment.input_dir() + ' ' + output_dir + \
			experiment.run_args()).readline()
#		line = os.popen('qsub -cwd run_job.sh -o ' + os.path.join(output_dir, 'stdout') + ' -e ' + os.path.join(output_dir, 'stderr')).readline()
		match = re.match('Your job (\d+)', line)
		if match:
			jid = match.group(1)
			open(os.path.join(output_dir, 'jid'), 'w').write(jid + '\n')
			num_started += 1
		else:
			print >> sys.stderr, 'Error submitting job'
			errors += 1
	
	return num_started

def main():
	if len(sys.argv) < 3:
		print >> sys.stderr, 'Usage: ' + sys.argv[0] + ' ACTION EXPERIMENT_FILE [EXPERIMENT_SET]'
		sys.exit()

	action = sys.argv[1]

	experiment_filename = sys.argv[2]
	# Remove .py extension, if it is present.
	experiment_filename = re.sub('.py\s*$', '', experiment_filename, 1)

	if len(sys.argv) >= 4:
		experiment_set_name = sys.argv[3]
	else:
		experiment_set_name = 'ALL'

	try:
		exec('from ' + experiment_filename + ' import get_experiment_set')
		experiment_set = get_experiment_set(experiment_set_name)
	except ImportError:
#		print >> sys.stderr, 'ERROR: Cannot load experiment set {0} from file {1}.py. Please check that the variable {0} exists in {1}.py.'.format(experiment_set_name, experiment_filename)
		print >> sys.stderr, 'ERROR: Cannot load experiment set ' + str(experiment_set_name, experiment_filename) + \
			'from file ' + str(experiment_filename) + '.py.'
		sys.exit()

	total_experiments = 0
	if action == 'start':
		jobs_started = 0
		for experiment in experiment_set.enumerate():
			total_experiments += 1
			jobs_started += run_experiment(experiment)
		
		print str(jobs_started) + ' Jobs Started'
		print str(total_experiments) + ' Experiments'
	if action == 'status':
		total_jobs = 0
		status = {}
		for experiment in experiment_set.enumerate():
			total_experiments += 1
			for i in range(experiment.repetitions):
				output_dir = os.path.join(experiment.output_dir(), str(i))
				s = experiment_status(output_dir)
				if s not in status:
					status[s] = []

				status[s].append(experiment)
				total_jobs += 1

		for s in sorted(status):
#			print '\t{0}: {1}'.format(s, len(status[s]))
			print '\t' + s + ' ' + str(len(status[s]))

		print "Total Jobs: " + str(total_jobs)
		print str(total_experiments) + ' Experiments'

if __name__ == '__main__':
	main()

