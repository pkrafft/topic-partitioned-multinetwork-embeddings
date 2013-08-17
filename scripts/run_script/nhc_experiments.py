from experiment import *
import copy

INPUT_DIR = '/lustre/work1/wallach/jmoore/email_networks/input/nhc/'
OUTPUT_DIR = '/lustre/work1/wallach/jmoore/email_networks/output/nhc/'
N_TOPICS_DIMENSIONS = [1, 2, 5, 10, 25, 50, 75, 100, 125, 150, 200]
ALPHAS =  [0.0001, 0.001, 0.01, 0.1]
BETAS = [0.01]

NUM_AUTHORS = 30

GLOBAL = {
	'n_iterations': 1000,
	'repetitions': 10,

	'print_interval': 1,
	'save_state_interval': 0,

	'verbose': 'true',

	'memory': '2G',
	'long_q': False,
	
	'obscure_random': True
#	'test_set_start': 
#	'test_set_end': 
}

ALL_EXPERIMENTS = [
#	{
#		'types': [DisjointModelMissingEdgeExperiment],
#
#		'latent_dim': 2,
#		'n_topics': N_TOPICS_DIMENSIONS,
#
#		'integrated_assignments': 'false',
#
#		'hyperparameters': {
#			'alpha': ALPHAS,
#			'beta': BETAS
#		}
#	},

	{
		'types': [JointModelMissingEdgeExperiment, MMLSEMMissingEdgeExperiment],

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

TEMPORAL_EXPERIMENTS_3DAY = copy.deepcopy(ALL_EXPERIMENTS)
TEMPORAL_EXPERIMENTS_5DAY = copy.deepcopy(ALL_EXPERIMENTS)

for e in TEMPORAL_EXPERIMENTS_3DAY:
	e['input_dir'] = '/lustre/work1/wallach/jmoore/email_networks/input/nhc_temporal_3day/'
	e['output_dir'] = '/lustre/work1/wallach/jmoore/email_networks/output/nhc_temporal_3day/'
	e['obscure_random'] = False
	e['test_set_start'] = 1478
	e['test_set_end'] = 1739

for e in TEMPORAL_EXPERIMENTS_5DAY:
	e['input_dir'] = '/lustre/work1/wallach/jmoore/email_networks/input/nhc_temporal_5day/'
	e['output_dir'] = '/lustre/work1/wallach/jmoore/email_networks/output/nhc_temporal_5day/'
	e['obscure_random'] = False
	e['test_set_start'] = 1303
	e['test_set_end'] = 1739

DEBUG_EXPERIMENTS = [
	{
		'output_dir': '/lustre/work1/wallach/jmoore/email_networks/debug/nhc/',
		'repetitions': 2,

		'types': [DisjointModelMissingEdgeExperiment],

		'latent_dim': 2,
		'n_topics':  [1, 2, 5],

		'integrated_assignments': 'false',

		'hyperparameters': {
			'alpha': [0.001],
			'beta': [0.01]
		}
	},

	{
		'output_dir': '/lustre/work1/wallach/jmoore/email_networks/debug/nhc/',
		'repetitions': 2,

		'types': [JointModelMissingEdgeExperiment, MMLSEMMissingEdgeExperiment],

		'latent_dim': 2,
		'n_topics':  [1, 2, 5],

		'integrated_assignments': 'true',

		'hyperparameters': {
			'alpha': [0.001],
			'beta': [0.01]
		}
	},

	{
		'output_dir': '/lustre/work1/wallach/jmoore/email_networks/debug/nhc/',
		'repetitions': 2,

		'types': [MMLSEMMissingEdgeExperiment],

		'topics': 1,
		'n_dimensions':  [1, 2, 5],

		'integrated_assignments': 'true',

		'hyperparameters': {
			'alpha': [0.001],
			'beta': [0.01]
		}
	}
]

EXPERIMENT_SETS = {
	'ALL': ALL_EXPERIMENTS,
	'TEMPORAL_3DAY': TEMPORAL_EXPERIMENTS_3DAY,
	'TEMPORAL_5DAY': TEMPORAL_EXPERIMENTS_5DAY,
	'TEMPORAL': TEMPORAL_EXPERIMENTS_3DAY + TEMPORAL_EXPERIMENTS_5DAY,
	'DEBUG': DEBUG_EXPERIMENTS
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

