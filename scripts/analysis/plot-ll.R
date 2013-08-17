dataset <- 'enron'

d <- read.table(paste('../output-',dataset,'/heldoutll.csv',sep=''),sep=',',stringsAsFactors=F)

sizes = c(1,2,5,30,50,75,100,150,200)
names = unique(d[,1])
results = matrix(NA,length(sizes),length(names))
colnames(results) = names
colors = 1:(ncol(results) + 1)

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

maxes = apply(results,2,which.max)
#results[,2:length(names)] = t(matrix(rep(apply(results,2,max,na.rm=T)[2:length(names)],nrow(results)),length(names)-1,nrow(results)))
names = c('tmpe','erosheva','bernoulli','lsm')#,'baseline','mmsb')
#i = which(names == 'mmsb')
#results = cbind(results[,1:(i-1)],rep(MMSB_SCORE,nrow(results)))
maxes = c(maxes[1:(i-1)],1)

pdf(paste('../output-',dataset,'/heldoutll.pdf', sep = ''), height = 7, width = 7)

##   upper <- results + 2*deviations/sqrt(numInstances[[j]])
##   lower <- results - 2*deviations/sqrt(numInstances[[j]])
##   ylim = c(min(lower, na.rm = TRUE), max(upper, na.rm = TRUE))
if(dataset == 'nhc') {
  ylim = c(min(results,na.rm=T)-.04,max(results,na.rm=T))
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
       ylab = 'average F-score',
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
    points(sizes[maxes[i]], results[maxes[i],i], pch = i, lwd = lwd, cex = cex)
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
