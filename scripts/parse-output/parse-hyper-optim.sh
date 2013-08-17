indirbase=../output-hyper-optim
outdir=.
iter=10000

for d in nhc enron;
  do 
  dir=$indirbase-$d
  for f in `ls $dir`;
    do
    parsename=`echo $f | sed 's/-/ /g' | awk '{print $1, $2, $3}' | sed 's/ /,/g'`
    echo $d,$parsename,`cat $dir/$f/alpha.txt.$iter` >> $outdir/alpha-optim.txt
    echo $d,$parsename,`cat $dir/$f/beta.txt.$iter` >> $outdir/beta-optim.txt
  done
done