dataset <- 'enron'

d <- read.table(paste('../',dataset,'-coherence.csv',sep=''),sep=',',stringsAsFactors=F)

sizes = c(1,2,5,30,50,75,100,150,200)
names = unique(d[,1])
real.names = c("RichMissingExperiment","RichMissingExperimentbernoulli",
  "BernoulliEroshevaMissingEdgeExperiment","LDAExperiment")
results = matrix(NA,9,length(names))
colors = 1:ncol(results)

i = 1
for(exp in names) {
  temp = d[d[,1] == exp,]
  temp = aggregate(temp[,4],list(temp[,3]),mean)
  if(exp == "EdgeFrequencyExperiment") {
     results[1:length(sizes),i] = rep(temp[[2]],length(sizes))
  } else {
    results[1:length(temp[[2]]),i] = temp[[2]]
  }
  i = i + 1
}

permute = 1:length(real.names)
for(i in 1:length(real.names)) {
  permute[i] = which(real.names[i] == names)
}

results = results[,permute]
names = c('tmpe','bernoulli','erosheva','lda')

pdf(paste('./nips/coherence-',dataset,'.pdf', sep = ''), height = 7, width = 7)

##   upper <- results + 2*deviations/sqrt(numInstances[[j]])
##   lower <- results - 2*deviations/sqrt(numInstances[[j]])
##   ylim = c(min(lower, na.rm = TRUE), max(upper, na.rm = TRUE))
if(dataset == 'nhc') {
ylim = c(min(results,na.rm=T)-.02,max(results,na.rm=T))
} else {
  ylim = c(min(results,na.rm=T),max(results,na.rm=T))
}
  par(cex = 1.3)
  
  lwd <- 3
  cex <- 1
  
  plot(sizes, results[,1],
       type = 'l', col = colors[1],
       xlim = c(0,200),
       ylim = ylim,
#       main = 'Missing Edge Performance',
       xlab = 'topic size',
       ylab = 'average coherence',
       lwd = lwd)
##   arrows(sizes, upper[,1], sizes, lower[,1],
##          code = 3, angle = 90, length = .1*range(sizes), col = colors[ subsets[[j]][1] ])
  for(i in 2:ncol(results)) {
    lines(sizes, results[,i], col = colors[i], lwd = lwd)
##     arrows(sizes, upper[,i], sizes, lower[,i],
##            code = 3, angle = 90, length = .1*range(sizes),
##            col = colors[ subsets[[j]][i] ])
  }
  points(sizes, results[,1], pch = 1, lwd = lwd, cex = cex)
  for(i in 2:ncol(results)) {
    points(sizes, results[,i], pch = i, lwd = lwd, cex = cex)
  }

if(dataset == 'nhc') {
   legend('bottomright',
          bty = 'n',
          names,
          pch = 1:length(names),
          col = colors[ 1:length(names) ],
          pt.lwd = lwd,
          pt.cex = cex)
 }
  dev.off()
