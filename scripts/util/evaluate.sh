for f in `ls`; do echo ----------$f--------; ls $f; done
for f in `ls`; do echo ----------$f--------; cat $f/stderr.txt; done
for f in `ls`; do echo ----------$f--------; cat $f/stdout.txt; done
for f in `ls`; do echo ----------$f--------; cat $f/log_prob.txt; done