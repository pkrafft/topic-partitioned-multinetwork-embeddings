import experiment

INPUT_DIR = './input'
OUTPUT_DIR = './output'

GLOBAL = {
	'n_iterations': 1000,
	'repetitions': 20,

	'print_interval': 1,
	'save_state_interval': 0,

	'verbose': 1,

	'memory': '2G',
	'long_q': False
}

ALL_EXPERIMENTS = [
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
		'n_dimensions': [1, 2, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 125, 150],

		'hyperparameters': {
			'alpha': [1,2,3],
			'beta': [1,2,3]
		}
	}
]

#TEST_EXPERIMENTS = [
#	{
#		'types': [TestExperiment],
#		'repetitions': 1,
#
#		'n_dimensions':  [1, 2, 5],
#
#		'hyperparameters': {
#			'alpha': [1,2],
#			'beta': [1,2],
#			'sample_hypers': True
#		}
#	}
#]

EXPERIMENT_SETS = {
	'ALL': ALL_EXPERIMENTS
}

# Make data consistent
for set in EXPERIMENT_SETS:
	for e in set:
		if 'input_dir' not in e:
			e['input_dir'] = INPUT_DIR
		if 'output_dir' not in e:
			e['input_dir'] = OUTPUT_DIR
		for key in GLOBAL:
			if key not in e:
				e[key] = GLOBAL[key]

def get_experiment_set(name):
	return experiment_set(EXPERIMENT_SETS['name'])
	raise Exception('Unknown experiment set')

