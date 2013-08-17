from experiment import *
import copy

INPUT_DIR = '/lustre/work1/wallach/pkrafft/data/nhc/'
OUTPUT_DIR = '/lustre/work1/wallach/pkrafft/output/'
#N_TOPICS_DIMENSIONS = [1, 2, 5, 10, 25, 50, 75, 100, 125, 150, 200, 250]
N_TOPICS_DIMENSIONS = [1,2,3]
REPETITIONS = 2
ITERATIONS = 3
ALPHAS =  [-1] # initial alpha
BETAS = [-1] # initial beta

NUM_AUTHORS = 30

GLOBAL = {
	'n_iterations': ITERATIONS,
	'repetitions': REPETITIONS,

	'print_interval': 1,
	'save_state_interval': 0,

	'sample_hypers': True,

	'verbose': 'true',

	'memory': '2G',
	'long_q': False,
	
	'obscure_random': True,
	'test_set_start': -1,
	'test_set_end': -1
}

ALL_EXPERIMENTS = [
	{
		'types': [DisjointModelMissingEdgeExperiment],

		'latent_dim': 2,
		'n_topics': N_TOPICS_DIMENSIONS,

		'integrated_assignments': 'false',

		'hyperparameters': {
			'alpha': ALPHAS,
			'beta': BETAS
		}
	},

	{
		'types': [LDAExperiment, JointModelExperiment, JointModelMissingEdgeExperiment, MMLSEMMissingEdgeExperiment],

		'latent_dim': 2,
		'n_topics': N_TOPICS_DIMENSIONS,

		'integrated_assignments': 'true',

		'hyperparameters': {
			'alpha': ALPHAS,
			'beta': BETAS
		}
	},

	{
		'types': [MMLSEMMissingEdgeExperiment],

		'topics': 1,
		'n_dimensions': N_TOPICS_DIMENSIONS,

		'integrated_assignments': 'true',

		'hyperparameters': {
			'alpha': ALPHAS,
			'beta': BETAS
		}
	}
]

EXPERIMENT_SETS = {
	'ALL': ALL_EXPERIMENTS,
}

# Make parameters consistent
for set in EXPERIMENT_SETS.values():
	for e in set:
		if 'input_dir' not in e:
			e['input_dir'] = INPUT_DIR
		if 'output_dir' not in e:
			e['output_dir'] = OUTPUT_DIR
		if 'num_authors' not in e:
			e['num_authors'] = NUM_AUTHORS
		for key in GLOBAL:
			if key not in e:
				e[key] = GLOBAL[key]

def get_experiment_set(name):
	return experiment_set(EXPERIMENT_SETS[name])
	raise Exception('Unknown experiment set')

