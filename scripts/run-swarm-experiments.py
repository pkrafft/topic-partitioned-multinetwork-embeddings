############################ WARNING: ###############################
# run this script only once. if you run it again while jobs are still
# running, you will ruin everything!
#####################################################################

# this class runs all the experiments (with the exception of mmsb and
# baseline) needed to generate the quantitative figures in the paper

# it will only work with the right type of cluster

# choose the dataset to use with -n for nhc or -e for enron

# choose the methods to use with their flags or use -a to run all and
# use the flags to leave out methods

# the script checks if the results already exist and only starts them
# if it needs to, and it will also restart from whatever the last
# iteration was if it is not the requested total number

# you can also use -l to evaluate the f-score and held-out edge log
# prob at a particular iteration without running any new iterations as
# long as the results already exist. this is useful for getting
# results from all methods at a particular iteration.

# you can also use -p to parse the output from all the folders
# associated with the indicated dataset and methods. This creates
# three files: one for coherence, one for f-score, and one for
# held-out edge log likelihood. parse should be used after evaluate
# has been used.

# this script relies on a particular directory structure. It assumes
# there is one main directory, d. The output will be written to the
# directory with the name supplied on the command line,
# d/out_folder_name. The data should be in d/data/nhc/ and
# d/data/enron (each with word-matrix.csv, edge-matrix.csv, and
# vocab.txt). The source folder is hard coded below. The files
# run-job.sh and run-big-job.sh should be in d/source_folder/scripts.

### parameters you probably should change ###

# the directory that will contain the data, source, and output
# directories. This must be an absolute path so that the code doesn't
# get confused when it is running on a compute node.
main_folder = '/lustre/work1/wallach/pkrafft'

# the directory containing build/jar/NetworkModels.jar. Likewise, this
# should be an absolute path.
source_folder = main_folder + '/Java-Network-Models'

import os
import shutil
import sys
import optparse
import random

parser = optparse.OptionParser()

parser.set_defaults(parse_output=False, check_status=False)

parser.add_option("-d", "--debug", action="store_true", dest="debug")
parser.add_option("-n", "--nhc", action="store_true", dest="nhc")
parser.add_option("-e", "--enron", action="store_true", dest="enron")
parser.add_option("-l", "--evaluate", dest="eval_iteration")
parser.add_option("-p", "--parse_output", action="store_true", dest="parse_output")
parser.add_option("-c", "--check_status", action="store_true", dest="check_status")
parser.add_option("-i", "--iterations", dest="num_iter")
parser.add_option("-r", "--repetitions", dest="repetitions")
parser.add_option("-f", "--out_folder_name", dest="out_folder")
parser.add_option("-t", "--manual_topic_size_setting", dest="topic_size")
parser.add_option("--lda", action="store_true", dest="lda")
parser.add_option("--tpme", action="store_true", dest="tpme")
parser.add_option("--tpme_full", action="store_true", dest="tpme_full")
parser.add_option("--exchangeable", action="store_true", dest="exchangeable")
parser.add_option("--erosheva", action="store_true", dest="erosheva")
parser.add_option("--bernoulli", action="store_true", dest="bernoulli")
parser.add_option("--bernoulli_full", action="store_true", dest="bernoulli_full")
parser.add_option("--asymmetric", action="store_true", dest="asymmetric")
parser.add_option("--joint_mixture", action="store_true", dest="joint_mixture")
parser.add_option("--word_mixture", action="store_true", dest="word_mixture")
parser.add_option("--lsm", action="store_true", dest="lsm")
parser.add_option("--mlsm", action="store_true", dest="mlsm")
parser.add_option("--mmlsm", action="store_true", dest="mmlsm")
parser.add_option("-a", "--all", action="store_true", dest="all")
parser.add_option("-o", "--hyper_optim", action="store_true", dest="hyper_optim")
(options, args) = parser.parse_args()

nhc = options.nhc
enron = options.enron
if nhc and enron:
    sys.exit("You must choose one dataset at a time!")

debug = options.debug

eval_iter = options.eval_iteration
if eval_iter != None:
    evaluate = True
else:
    evaluate = False

parse = options.parse_output
check = options.check_status
if parse + check + evaluate > 1:
    sys.exit("You must choose between checking, evaluating, and parsing!")

n_iterations = options.num_iter
repetitions = int(options.repetitions)

out_dir_name = options.out_folder
if out_dir_name == None:
    sys.exit("You must specify the name of an output directory!")

# if this is not specified, the default topic sizes are used
topic_size = options.topic_size

if options.all:
    lda = not options.lda
    tpme = not options.tpme
    tpme_full = not options.tpme_full
    exchangeable = not options.exchangeable
    erosheva = not options.erosheva
    bernoulli = not options.bernoulli
    bernoulli_full = not options.bernoulli_full
    asymmetric = not options.asymmetric
    joint_mixture = not options.joint_mixture
    word_mixture = not options.word_mixture
    lsm = not options.lsm
    mlsm = not options.mlsm
    mmlsm = not options.mmlsm
else:
    lda = options.lda
    tpme = options.tpme
    tpme_full = options.tpme_full
    exchangeable = options.exchangeable
    erosheva = options.erosheva
    bernoulli = options.bernoulli
    bernoulli_full = options.bernoulli_full
    asymmetric = options.asymmetric
    joint_mixture = options.joint_mixture
    word_mixture = options.word_mixture
    lsm = options.lsm
    mlsm = options.mlsm
    mmlsm = options.mmlsm

sample_hypers = options.hyper_optim

top_output_dir = main_folder + '/' + out_dir_name

if nhc:
    word_file = main_folder + '/data/nhc/word-matrix.csv'
    vocab_file = main_folder + '/data/nhc/vocab.txt'
    edge_file = main_folder + '/data/nhc/edge-matrix.csv'
    n_authors = '30'
elif enron:
    word_file = main_folder + '/data/enron/word-matrix.csv'
    vocab_file = main_folder + '/data/enron/vocab.txt'
    edge_file = main_folder + '/data/enron/edge-matrix.csv'
    n_authors = '50'
else:
    sys.exit("You must choose a dataset!")

if nhc:
    alpha_admixture_base = 1
    alpha_mixture_base = 26
if enron:
    alpha_admixture_base = 2
    alpha_mixture_base = 7
alpha_power = -0

beta_admixture_base = 1
beta_mixture_base = 5
beta_power = -2
beta_admixture = beta_admixture_base*(10**beta_power)
beta_admixture = str(beta_admixture)
beta_mixture = beta_mixture_base*(10**beta_power)
beta_mixture = str(beta_mixture)

gamma_base = 1
gamma_power = -2
gamma = gamma_base*(10**gamma_power)
gamma = str(gamma)

if topic_size == None:
    if debug:
        n_topics = [1, 10]
    else:
        n_topics = [1, 2, 5, 30, 50, 75, 100, 150, 200]
else:
    n_topics = [int(topic_size)]
            
latent_dim = '2'

if evaluate:
    n_iterations = '0'

print_interval = '1'
save_state_interval = str(min(1000, int(n_iterations)))

if enron:
    big = True
else:
    big = False

long = False

def run_experiment(experiment, id, args, long):

	output_dir = top_output_dir + '/' + experiment + '-' + id

        if evaluate:
            load_iter = eval_iter
            long = False
        else:
            load_iter = '0'
            if os.path.exists(output_dir):
                load_iter = search_iters(output_dir)
            else:
                os.mkdir(output_dir)

        stdout_file = output_dir + '/stdout.' + load_iter + '.txt'
        stderr_file = output_dir + '/stderr.' + load_iter + '.txt'
        if evaluate:
            stdout_file += '.eval'
            stderr_file += '.eval'
        
        command = 'qsub -cwd -o ' + stdout_file
        command += ' -e ' + stderr_file
        if long:
            command += ' -l long=TRUE'
        if big:
            command += ' -l mem_free=2G -l mem_token=2G'
            command += ' ' + source_folder + '/scripts/run-big-job.sh '
        else:
            command += ' -l mem_free=1G -l mem_token=1G'
            command += ' ' + source_folder + '/scripts/run-job.sh '

        command += 'experiments.' + experiment + args

        command += ' -c -of=' + output_dir + ' -sd=' + source_folder

        command += ' -wf=' + word_file
        command += ' -vf=' + vocab_file
        command += ' -ef=' + edge_file
        command += ' -a=' + n_authors
        command += ' -p=' + print_interval
        command += ' -s=' + save_state_interval
        if not evaluate:
            command += ' -v'

        if load_iter != '0' and not evaluate:
            command += ' --r -rf=' + output_dir
            command += ' -i=' + load_iter
            command += ' -n=' + str(int(n_iterations) - int(load_iter))
        else:
            if evaluate:
                command += ' --r -rf=' + output_dir
                command += ' -i=' + load_iter
            command += ' -n=' + n_iterations
            
        if debug:
            command += ' -m=10000'
        elif long:
            command += ' -m=' + str(12*60*60*1000)
        else:
            command += ' -m=' + str(4*60*60*1000)
        if big:
            command += ' -b'
        if long:
            command += ' -l'

        if (load_iter != n_iterations) or evaluate:
            if check:
                print experiment + '-' + id + ' has only completed ' + load_iter + ' iterations!'
            else:
                print command
                os.system(command)	

# find the last completed iteration in which a state was saved
def search_iters(dir):
    log_prob_file = dir + "/log_prob.txt"
    last_iter = '0'
    if os.path.exists(log_prob_file):
        l = open(log_prob_file).readlines()
        m = map(lambda x: x.strip().split(','), l)
        if len(m) > 0:
            last_iter = m[-1][0]
    if last_iter != '0' and int(last_iter) < 50000:
        last_iter = 0
        for f in os.listdir(dir):
            s = f.split('.')
            if((s[0] == 'edge_state' or s[0] == 'word_state') and len(s) == 4):
                n = int(s[3])
                if(n > last_iter):
                    last_iter = n
    return str(last_iter)

def parse_score(experiment, id, line_from_end = 1):
    dir = top_output_dir + '/' + experiment + '-' + id
    i = n_iterations
    out_file = dir + '/stdout.' + i + '.txt.eval'
    print out_file
    out_file = open(out_file)
    lines = out_file.readlines()
    score = lines[len(lines) - line_from_end]
    score = score.split(':')[1].strip()
    return score

def write_line(filename, experiment, i, t, score):
    filename.write(experiment + ',' + str(i) + ',' + str(t) + ',' + score + '\n')

if parse:
    coherence_file = open(top_output_dir + '/coherence.csv', 'w')
    f_file = open(top_output_dir + '/fscore.csv', 'w')
    ll_file = open(top_output_dir + '/heldoutll.csv', 'w')

offset = 0
for i in range(repetitions):

	i += offset

	for t in n_topics:

            # admixture models
            
            args = ''
            
            alpha = alpha_admixture_base*(10**alpha_power)/float(t)
            alpha = str(alpha)

            id = str(i) + '-' + str(t)

            args += ' -k=' + latent_dim
            args += ' -t=' + str(t)
            if sample_hypers:
                args += ' -alpha=' + str(random.gammavariate(1,1))
                args += ' -beta=' + str(random.gammavariate(1,1))
                args += ' -gamma=' + str(random.gammavariate(1,1))
                args += ' -h'
            else:
                args += ' -alpha=' + alpha
                args += ' -beta=' + beta_admixture
                args += ' -gamma=' + gamma


            if lda:
                experiment = 'LDAExperiment'
                if parse:
                    score = parse_score(experiment, id, 2)
                    write_line(coherence_file, experiment, i, t, score)
                else:
                    run_experiment(experiment, id, args, long)

            if tpme_full:
                experiment = 'ConditionalStructureExperiment'
                if parse:
                    sys.exit('Unimplemented!')
                else:
                    run_experiment(experiment, id, args, long)

            if bernoulli_full:
                experiment = 'ConditionalStructureBernoulliExperiment'
                if parse:
                    sys.exit('Unimplemented!')
                else:
                    run_experiment(experiment, id, args, long)

            id += '-missing'
            args += ' -e'

            if tpme:
                experiment = 'ConditionalStructureExperiment'
                if parse:
                    score = parse_score(experiment, id, 8)
                    write_line(coherence_file, experiment, i, t, score)
                    score = parse_score(experiment, id, 2)
                    write_line(f_file, experiment, i, t, score)
                    score = parse_score(experiment, id, 1)
                    write_line(ll_file, experiment, i, t, score)
                else:
                    run_experiment(experiment, id, args, long)
            
            if exchangeable:
                experiment = 'ExchangeableStructureExperiment'
                if parse:
                    sys.exit('Unimplemented!')
                else:
                    run_experiment(experiment, id, args, long)
            
            if mmlsm:
                experiment = 'MMLSMExperiment'
                if parse:
                    sys.exit('Unimplemented!')
                else:
                    run_experiment(experiment, id, args, long)
            
            if erosheva:
                experiment = 'BernoulliEroshevaExperiment'
                if parse:
                    score = parse_score(experiment, id, 8)
                    write_line(coherence_file, experiment, i, t, score)
                    score = parse_score(experiment, id, 2)
                    write_line(f_file, experiment, i, t, score)
                    score = parse_score(experiment, id, 1)
                    write_line(ll_file, experiment, i, t, score)
                else:
                    run_experiment(experiment, id, args, long)
            
            if bernoulli:
                experiment = 'ConditionalStructureBernoulliExperiment'
                if parse:
                    score = parse_score(experiment, id, 8)
                    write_line(coherence_file, experiment, i, t, score)
                    score = parse_score(experiment, id, 2)
                    write_line(f_file, experiment, i, t, score)
                    score = parse_score(experiment, id, 1)
                    write_line(ll_file, experiment, i, t, score)
                else:
                    run_experiment(experiment, id, args, long)
            
            if asymmetric:
                experiment = 'ConditionalStructureAsymmetricBernoulliExperiment'
                if parse:
                    sys.exit('Unimplemented!')
                else:
                    run_experiment(experiment, id, args, long)

            
            # mixture models
            
            args = ''
            
            alpha = alpha_mixture_base*(10**alpha_power)/float(t)
            alpha = str(alpha)

            id = str(i) + '-' + str(t)

            args += ' -k=' + latent_dim
            args += ' -t=' + str(t)
            if sample_hypers:
                args += ' -alpha=' + str(random.gammavariate(1,1))
                args += ' -beta=' + str(random.gammavariate(1,1))
                args += ' -h'
            else:
                args += ' -alpha=' + alpha
                args += ' -beta=' + beta_mixture

            if word_mixture:
                experiment = 'TextMixtureExperiment'
                if parse:
                    sys.exit('Unimplemented!')
                else:
                    run_experiment(experiment, id, args, long)

            id += '-missing'
            args += ' -e'

            if joint_mixture:
                experiment = 'MixtureStructureExperiment'
                if parse:
                    sys.exit('Unimplemented!')
                else:
                    run_experiment(experiment, id, args, long)

            if mlsm:
                experiment = 'MLSMExperiment'
                if parse:
                    sys.exit('Unimplemented!')
                else:
                    run_experiment(experiment, id, args, long)

        for k in n_topics:

            # latent space models

            args = ''
            
            alpha = '1'

            id = str(i) + '-1-' + str(k)
            
            args += ' -t=1'
            args += ' -k=' + str(k)
            args += ' -alpha=' + alpha
            
            id += '-missing'
            args += ' -e'

            if lsm:
                experiment = 'MMLSMExperiment'
                if parse:
                    score = parse_score(experiment, id, 2)
                    write_line(f_file, experiment, i, k, score)
                    score = parse_score(experiment, id, 1)
                    write_line(ll_file, experiment, i, k, score)
                else:
                    run_experiment(experiment, id, args, long)
