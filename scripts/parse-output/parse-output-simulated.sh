
outputFolder=../output-simulated-networks

mkdir $outputFolder-summaries

indeg=$outputFolder-summaries/in-degree.txt
outdeg=$outputFolder-summaries/out-degree.txt
inward=$outputFolder-summaries/inward-connectedness.txt
outward=$outputFolder-summaries/outward-connectedness.txt
trans=$outputFolder-summaries/transitivity.txt

touch $indeg
touch $outdeg
touch $inward
touch $outward
touch $trans

for f in `ls $outputFolder`; do
    iter=`echo $f | sed $'s/-/\t/g' | awk '{print $2}'`
    net=$outputFolder-summaries/network-$iter.txt
    old_loc=$outputFolder/$f/simulated-network/network.txt
    if [ -f $old_loc ]; then
	cp $outputFolder/$f/simulated-network/network.txt $net
	net=$outputFolder-summaries/geodesic-distances-$iter.txt
	cp $outputFolder/$f/simulated-network/geodesic-distances.txt $net
	echo $iter,`cat $outputFolder/$f/simulated-network/in-degree.txt` >> $indeg
	echo $iter,`cat $outputFolder/$f/simulated-network/out-degree.txt` >> $outdeg
	echo $iter,`cat $outputFolder/$f/simulated-network/inward-connectedness.txt` >> $inward
	echo $iter,`cat $outputFolder/$f/simulated-network/outward-connectedness.txt` >> $outward
	echo $iter,`cat $outputFolder/$f/simulated-network/transitivity.txt` >> $trans
    fi
done
