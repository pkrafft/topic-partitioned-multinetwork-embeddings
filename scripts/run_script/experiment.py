#
# Notes on directory naming
#
# Directories are OUTPUT_DIR/experiment_name/parameter_str/rep/
# Where parameter_str is a combination of the topic/dim number
# and the hyperparameter names.
#
# iterations should be implicit in all experiments, right?
# test_range should also be implicit in the experiment-specific
# output directory
#
# INPUT_DIR needs to contain TODO
#

##
## REQUIRED ARGUMENTS
## 
## Experiment
##  * repetitions
##  * memory (optional) Required memory allocation
##  * long_q (optional) True if experiment runs for > 2 hours
##
## Global
##  * input_dir
##  * output_dir
##  * print_interval
##  * save_state_interval
##  * verbose
##
## ConditionalMMLSMMissingEdgeExperiment
##     <instance_list> <num_actors> <num_topics> <latent_dim>
##     <num_itns> <print_interval> <save_state_interval> <uniform_proportions> <verbose> " +
##     <alpha> <output_dir>
##  * latent_dim
##  * n_iterations
##  * uniform_proportions
##  * hypers
##    * alpha
##
## class DataSummaryExperiment
##     <word_matrix> <vocab_list> <edge_matrix>
##     <num_authors> <output_dir>
##  * None
##
## DisjointModelMissingEdgeExperiment
## INHERITED from JointModelExperiment
##     <word_matrix> <vocab_list> <edge_matrix>
##     <num_authors> <num_topics> <latent_dim> <num_itns>
##     <print_interval> <save_state_interval> <sampleHypers> <integrated_assignments>
##     <verbose> <alpha> <obscure_random> <test_set_start> <test_set_end>
##     <output_dir>
##  * latent_dim
##  * n_iterations
##  * integrated_assignments
##  * obscure_random
##  * test_set START:END
##  * sample_hypers
##  * hypers
##    * alpha
##
## EdgeFrequencyExperiment
##     <edge_matrix> <num_actors> <obscure_random>
##     <test_set_start> <test_set_end>
##  * obscure_random
##  * test_set START:END
##
## JointModelExperiment
##     <word_matrix> <vocab_list> <edge_matrix>
##     <num_authors> <num_topics> <latent_dim> <num_itns>
##     <print_interval> <save_state_interval> <sampleHypers> <integrated_assignments>
##     <verbose> <alpha> <obscure_random> <test_set_start> <test_set_end>
##     <output_dir>
##  * latent_dim
##  * n_iterations
##  * integrated_assignments
##  * obscure_random
##  * test_set START:END
##  * sample_hypers
##  * hypers
##    * alpha
##
## JointModelMissingEdgeExperiment
## INHERITED from JointModelExperiment
##     <word_matrix> <vocab_list> <edge_matrix>
##     <num_authors> <num_topics> <latent_dim> <num_itns>
##     <print_interval> <save_state_interval> <sampleHypers> <integrated_assignments>
##     <verbose> <alpha> <obscure_random> <test_set_start> <test_set_end>
##     <output_dir>
##
## DAExperiment
##     <instance_list> <vocab_list> <num_topics>
##     <num_itns> <print_interval> <save_state_interval> <sampleHypers>
##     <verbose> <alpha> <output_dir>
##
## MMLSEMExperiment
##     <instance_list> <num_actors> <num_topics> <latent_dim>
##     <num_itns> <print_interval> <save_state_interval> <sample_alphas>
##     <using_proportions> <verbose> <alpha> <obscure_random>
##     <test_set_start> <test_set_end> <output_dir>
##
## MMLSEMMissingEdgeExperiment
## INHERITS from MMLSEMExperiment
##     <instance_list> <num_actors> <num_topics> <latent_dim>
##     <num_itns> <print_interval> <save_state_interval> <sample_alphas>
##     <using_proportions> <verbose> <alpha> <obscure_random>
##     <test_set_start> <test_set_end> <output_dir>
##
## SimulatedDataExperiment
##     <word_matrix> <vocab_list> <edge_matrix>
##     <num_authors> <num_topics> <latent_dim>
##     <using_word_model> <using_edge_model> <using_single_intercept>
##    <using_marginalized_assignments>
##     <num_itns> <print_interval> <save_state_interval>
##     <print_space_networks> <infer_model_from_simulated_data>
##     <sample_hypers> <alpha> <output_dir>
##
## SyntheticDataExperiment
##     <num_docs> <num_actors> <num_words>
##     <num_topics> <latent_dim> <mean_words_per_doc>
##     <num_itns> <print_interval> <save_state_interval>
##     <sampleAlphas> <usingWordModel> <using_proportions>
##     <alpha> <output_dir>

# TODO w/ Java code
#     Make it write to logical files and ALWAYS write verbose output
#     Look at input format and see if we can use some kind of sparse format instead
#
#     Come on, N AUTHORS should be inferred NOT SPECIFIED HERE
#     Verbose should ALWAYS be on. We're only talking about a few MB per day of compute time here

### CONFIGURATION SECTION ###

INPUT_DIR = './input'
OUTPUT_DIR = './output'

## USES TOPICS, USES ALPHA, BETA
+		# LDAExperiment

## USES BOTH, USES ALPHA, BETA
DEL		# ConditionalMMLSMMissingEdgeExperiment
+		# DisjointModelMissingEdgeExperiment
+		# JointModelExperiment
+		# JointModelMissingEdgeExperiment
		# MMLSEMExperiment
-+		# MMLSEMMissingEdgeExperiment
DEL		# SimulatedDataExperiment
DEL		# SyntheticDataExperiment

## USES NEITHER, NO HYPERS
		# DataSummaryExperiment
		# EdgeFrequencyExperiment
	
GLOBAL = {
	'n_iterations': 1000,
	'repetitions': 20,

	'memory': '2G',
	'long_q': False
}

EXPERIMENTS = [
	{
		'types': [LDAExperiment, DisjointModelMissingEdgeExperiment, JointModelExperiment, JointModelMissingEdgeExperiment, MMLSEMMissingEdgeExperiment],

		'latent_dim': 2,
		'n_topics':  [1, 2, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 125, 150],

		'hyperparameters': {
			'alpha': [4,5,6],
			'beta': [4,5,6]
		}
	},

	{
		'types': [MMLSEMMissingEdgeExperiment],

		'topics': 1,
		'n_dimensions':  [1, 2, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 125, 150],

		'hyperparameters': {
			'alpha': [1,2,3],
			'beta': [1,2,3]
		}
	}
]

TEST_EXPERIMENTS = [
	{
		'types': [TestExperiment],
		'repetitions': 1,

		'n_dimensions':  [1, 2, 5],

		'hyperparameters': {
			'alpha': [1,2],
			'beta': [1,2],
			'sample_hypers': True
		}
	}
]

# Make data consistent
for e in EXPERIMENTS:
	if 'input_dir' not in e:
		e['input_dir'] = INPUT_DIR
	if 'output_dir' not in e:
		e['input_dir'] = OUTPUT_DIR
	for key in GLOBAL:
		if key not in e:
			e[key] = GLOBAL[key]

### END CONFIGURATION SECTION ###

import os.path

class experiment_set:
	def __init__(self, experiment_list):
		self.experiment_list = experiment_list
	
	def enumerate(self):
		for e in self.experiment_list:
			for type in e['types']:
				for params in self.enumerate_params(e):
					attrs = e.copy()
					attrs.update(params)

					yield type(attrs)
	
	def enumerate_params(self, e):
		for hypers in self.enumerate_hypers(e):
			returned = 0
			if 'n_topics' in e:
				for topics in e['n_topics']:
					returned += 1
					yield {
						'topics': topics,
						'hyperparameters': hypers
					}
			if 'n_dimensions' in e:
				for dimensions in e['n_dimensions']:
					returned += 1
					yield {
						'latent_dim': dimensions,
						'hyperparameters': hypers
					}
			if returned == 0:
				yield {
					'hyperparameters': hypers
				}

	def enumerate_hypers(self, e):
		returned = 0
		if 'hyperparameters' in e:
			for alpha in e['hyperparameters']['alpha']:
				for beta in e['hyperparameters']['beta']:
					returned += 1
					yield {
						'alpha': alpha,
						'beta': beta
					}

		if returned == 0 or ('sample_hypers' in e and e['sample_hypers']):
			yield {
				'sample_hypers': True
			}

class Experiment:
	def __init__(self, attrs):
		self.name = self.__class__.__name__
		self.repetitions = attrs['repetitions']
		self.attrs = attrs
	
	def input_dir(self):
		return self.input_dir
	
	def output_dir(self):
		return os.path.join(self.output_dir, self.name, self.params_str())
	
	def params_str(self):
		"""
		Return a directory name for the parameter configuration of the given experiment
		"""

		result = ''

		if 'topics' in self.attrs:
			result += 'T' + str(self.attrs['topics']) + '-'
		if 'latent_dim' in self.attrs:
			result += 'D' + str(self.attrs['latent-dim']) + '-'

		if 'alpha' in self.attrs['hyperparameters'] and 'beta' in self.attrs['hyperparameters']:
#			result += 'alpha_{0}-beta_{1}'.format(self.attrs['hyperparameters']['alpha'], self.attrs['hyperparameters']['beta'])
			result += 'alpha_' + str(self.attrs['hyperparameters']['alpha']) + \
				'-beta_' + str(self.attrs['hyperparameters']['beta'])
		else:
			result += 'sample_hypers'

		return result

	def qsub_args(self):
	command = ''

	if 'memory' in self.args:
		command += ' -l mem_free=' + self.args['memory']
	if 'long_q' in self.args and self.args['long_q']:
		command += ' -l long=TRUE'

	returm command
	
	def run_command(self):
		# TODO
		return 'run_job.sh'
	
	def __str__(self):
		return self.name + '::' + self.params_str()

class ConditionalMMLSMMissingEdgeExperiment(Experiment):
	# Usage: <instance_list> <num_actors> <num_topics> <latent_dim>
	#     <num_itns> <print_interval> <save_state_interval> <uniform_proportions> <verbose> " +
	#     <alpha> <output_dir>
	def run_args(self):
		# TODO
		return ' '.join(map(str, [
		])

class DataSummaryExperiment(Experiment):
	# Usage: <word_matrix> <vocab_list> <edge_matrix>
	#     <num_authors> <output_dir>
	def run_args(self):
		# TODO
		return ' '.join(map(str, [
		])

class DisjointModelMissingEdgeExperiment(Experiment):
	# Usage inherited from JointModelExperiment
	def run_args(self):
		# TODO
		return ' '.join(map(str, [
		])

class EdgeFrequencyExperiment(Experiment):
	# Usage: <edge_matrix> <num_actors> <obscure_random>
	#     <test_set_start> <test_set_end>
	def run_args(self):
		# TODO
		return ' '.join(map(str, [
		])

class JointModelExperiment(Experiment):
	# Usage: <word_matrix> <vocab_list> <edge_matrix>
	#     <num_authors> <num_topics> <latent_dim> <num_itns>
	#     <print_interval> <save_state_interval> <sampleHypers> <integrated_assignments>
	#     <verbose> <alpha> <obscure_random> <test_set_start> <test_set_end>
	#     <output_dir>
	def run_args(self):
		# TODO
		return ' '.join(map(str, [
		])

class JointModelMissingEdgeExperiment(Experiment):
	# Usage inherited from JointModelExperiment
	def run_args(self):
		# TODO
		return ' '.join(map(str, [
		])

class LDAExperiment(Experiment):
	# Usage: <instance_list> <vocab_list> <num_topics>
	#     <num_itns> <print_interval> <save_state_interval> <sampleHypers>
	#     <verbose> <alpha> <output_dir>
	def run_args(self):
		# TODO
		return ' '.join(map(str, [
		])

class MMLSEMExperiment(Experiment):
	# Usage: <instance_list> <num_actors> <num_topics> <latent_dim>
	#     <num_itns> <print_interval> <save_state_interval> <sample_alphas>
	#     <using_proportions> <verbose> <alpha> <obscure_random>
	#     <test_set_start> <test_set_end> <output_dir>
	def run_args(self):
		# TODO
		return ' '.join(map(str, [
		])

class MMLSEMMissingEdgeExperiment(Experiment):
	# Usage inherited from MMLSEMExperiment
	def run_args(self):
		# TODO
		return ' '.join(map(str, [
		])

class SimulatedDataExperiment(Experiment):
	# Usage: <word_matrix> <vocab_list> <edge_matrix>
	#     <num_authors> <num_topics> <latent_dim>
	#     <using_word_model> <using_edge_model> <using_single_intercept>
	#    <using_marginalized_assignments>
	#     <num_itns> <print_interval> <save_state_interval>
	#     <print_space_networks> <infer_model_from_simulated_data>
    #     <sample_hypers> <alpha> <output_dir>
	def run_args(self):
		# TODO
		return ' '.join(map(str, [
		])

class SyntheticDataExperiment(Experiment):
	# Usage: <num_docs> <num_actors> <num_words>
	#     <num_topics> <latent_dim> <mean_words_per_doc>
	#     <num_itns> <print_interval> <save_state_interval>
	#     <sampleAlphas> <usingWordModel> <using_proportions>
	#     <alpha> <output_dir>
	def run_args(self):
		# TODO
		return ' '.join(map(str, [
		])

def get_experiment_set(name):
	if name == 'ALL':
		return experiment_set(EXPERIMENTS)
	if name == 'TEST':
		return experiment_set(TEST_EXPERIMENTS)
	
	raise Exception('Unknown experiment set')

