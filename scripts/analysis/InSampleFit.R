inputDir = "/Users/peter/Documents/Wallach/output-nips/RichSimulatedExperiment-0-100-nhc/"
outDir = "/Users/peter/Documents/Wallach/Network-Models/writing/nips-edited/"
n.actor = 30

# Read in network
net <- as.matrix(read.table(paste(inputDir,"true-network/network.txt",sep=""),sep=","))

# Read in simulated networks
simNets <- array(0,dim=c(n.actor,n.actor,1000))
for(i in 1:1000){
	filei <- paste(inputDir,"summaries/network-",i-1,".txt",sep="")
	simNets[,,i] <- as.matrix(read.table(filei,sep=","))
	}

include <- upper.tri(matrix(0,n.actor,n.actor))
include <- include | lower.tri(matrix(0,n.actor,n.actor))
each <- sum(include)
simGeo <- array(0,dim=c(1,1000*each))
j <- 1
for(i in 1:1000){
	filei <- paste(inputDir,"summaries/geodesic-distances-",i-1,".txt",sep="")
        geo <- as.matrix(read.table(filei,sep=","))
        geo[geo == "Infinity"] <- "Inf"
        geo <- geo[include]
        geo <- as.numeric(geo)
        simGeo[j:(j + each - 1)] <- geo
        j <- j + each
	}

# Read in simulated data
simIdegree <- read.table(paste(inputDir,"summaries/in-degree.txt",sep=""), sep=",")
simOdegree <- read.table(paste(inputDir,"summaries/out-degree.txt",sep=""), sep=",")
simTrans <- read.table(paste(inputDir,"summaries/transitivity.txt",sep=""),sep=",")

# Read in network data
obsTrans <- read.table(paste(inputDir,"true-network/transitivity.txt",sep=""))
obsIdegree <- t(read.table(paste(inputDir,"true-network/in-degree.txt",sep=""),sep=","))
obsOdegree <- t(read.table(paste(inputDir,"true-network/out-degree.txt",sep=""),sep=","))
obsGeo <- as.matrix(read.table(paste(inputDir,"true-network/geodesic-distances.txt",sep=""),sep=","))
obsGeo[obsGeo == "Infinity"] <- "Inf"
obsGeo <- obsGeo[include]
obsGeo <- as.numeric(obsGeo)

# Degree Fit Plots

srt_ideg <- obsIdegree[order(obsIdegree,decreasing=T)]
srtSimideg <- as.matrix(simIdegree[,2:31])[,order(obsIdegree,decreasing=T)]

srt_odeg <- obsOdegree[order(obsOdegree,decreasing=T)]
srtSimodeg <- as.matrix(simOdegree[,2:31])[,order(obsOdegree,decreasing=T)]

obsDegree <- obsOdegree + obsIdegree
srt_deg <- obsDegree[order(obsDegree,decreasing=T)]
srtSimdeg <- as.matrix(simOdegree[,2:31] + simIdegree[,2:31])[,order(obsDegree,decreasing=T)]

# Add Dyad intensity
dyad.intensity <- function(g){
	di <- sqrt(g*t(g))
	c(di[upper.tri(di)],di[lower.tri(di)])
	}

# Compute Dyad intensity
obsDyint <- dyad.intensity(net)
simDyint <- apply(simNets,3,dyad.intensity)

pdf(paste(outDir,"deg_box.pdf",sep=""),height=7,width=8)
par(las=1, cex = 2.5, cex.lab=1, mai=c(1.6,1.5,0.2,0.2), mgp = c(2.1,0.6,0), family = 'serif')
boxplot(srtSimdeg,pch=1,cex=.75,xaxt="n",border="gray60")
title(xlab="Actor (Sorted by Observed Degree)",ylab="Degree")
lines(srt_deg,lwd=2.5) 
dev.off()

pdf(paste(outDir,"trans_hist.pdf",sep=""),height=7,width=8)
par(las=1, cex = 2.5, cex.lab=1, mai=c(1.6,1.5,0.2,0.2), mgp = c(2.1,0.6,0), family = 'serif')
hist(simTrans[,2],xlab="",col="gray75",border="gray55",main="", mgp = c(2,0.6,0))
title(xlab="Transitivity")
abline(v=obsTrans,lwd=3.5)
lines(srt_odeg,lwd=2.5) 
dev.off()

pdf(paste(outDir,"dyintQQ.pdf",sep=""),height=7,width=8)
par(las=1, cex = 2.5, cex.lab=1, mai=c(1.6,1.5,0.2,0.2), mgp = c(2.1,0.6,0), family = 'serif')
qqplot(c(obsDyint),c(simDyint),xlab="",col="gray60",pch=4,main="",ylab="Simulated Quantile")
abline(0,1,lwd=3)
title(xlab="Observed Quantile")
dev.off()

pdf(paste(outDir,"geodQQ.pdf",sep=""),height=7,width=8)
par(las=1, cex = 2.5, cex.lab=1, mai=c(1.6,1.5,0.2,0.2), mgp = c(2.1,0.6,0), family = 'serif')
qqplot(c(obsGeo),c(simGeo),xlab="",col="gray60",pch=4,main="",ylab="Simulated Quantile")
abline(0,1,lwd=3)
title(xlab="Observed Quantile",line=2.2)
dev.off()


