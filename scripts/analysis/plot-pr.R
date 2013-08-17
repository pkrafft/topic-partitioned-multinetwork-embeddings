dataset <- 'enron'
plot.ci <- F

d <- read.table(paste('../output-nips/output-',dataset,'/fscore.csv',sep=''),sep=',',stringsAsFactors=F)
md <- read.table(paste('../output-nips/output-',dataset,'/fscore-mmsb.csv',sep=''),sep=',',stringsAsFactors=F)
ed <- read.table(paste('../output-nips/output-',dataset,'/fscore-efe.csv',sep=''),sep=',',stringsAsFactors=F)
ed <- cbind(ed[,1:2],rep(0,nrow(ed)),ed[,3])
colnames(ed) <- colnames(d)

d <- rbind(d,md,ed)

sizes = c(1,2,5,30,50,75,100,150,200)
names = unique(d[,1])
means = matrix(NA,length(sizes),length(names))
colnames(means) = names
colors = grey(seq(0,0.5,length=length(names)))
#colors = grey(c(0,0.6,0.1,0.2,0.4,0.3))
#colors = rep(1,length(names))
sds = matrix(NA,length(sizes),length(names))
reps = matrix(NA,length(sizes),length(names))

i = 1
for(exp in names) {
  temp = d[d[,1] == exp,]
  temp1 = aggregate(temp[,4],list(temp[,3]),mean)
  print(exp)
  print(temp1)
  temp2 = aggregate(temp[,4],list(temp[,3]),sd)
  temp3 = aggregate(temp[,4],list(temp[,3]),length)
  if(exp == "EdgeFrequencyExperiment") {
    means[1:length(sizes),i] = rep(temp1[[2]],length(sizes))
    sds[1:length(sizes),i] = rep(temp2[[2]],length(sizes))
    reps[1:length(sizes),i] = rep(temp3[[2]],length(sizes))
  } else if (exp == "MMSBExperiment" | exp == "MMLSMExperiment") {
    j <- which.max(temp1[[2]])
    means[1:length(sizes),i] = rep(temp1[[2]][j],length(sizes))
    sds[1:length(sizes),i] = rep(temp2[[2]][j],length(sizes))
    reps[1:length(sizes),i] = rep(temp3[[2]][j],length(sizes))
  } else {
    means[1:length(temp1[[2]]),i] = temp1[[2]]
    sds[1:length(temp2[[2]]),i] = temp2[[2]]
    reps[1:length(temp2[[2]]),i] = temp3[[2]]
  }
  i = i + 1
}

names = c('our model','Erosheva','baseline 2','LSM','MMSB','baseline 1')
colnames(means) <- names
colnames(sds) <- names
colnames(reps) <- names

permutation <- c('our model','Erosheva','baseline 2','MMSB','baseline 1','LSM')
means <- means[,permutation]
sds <- sds[,permutation]
reps <- reps[,permutation]
names <- permutation

#pdf(paste('../output-nips/output-',dataset,'/fscore-',dataset,'.pdf', sep = ''), height = 7, width = 7)
pdf(paste('./writing/nips-edited/fscore-',dataset,'.pdf', sep = ''), height = 7, width = 7)

p <- 0.05
upper <- means + qnorm(1 - p/2)*sds/sqrt(reps)
lower <- means + qnorm(p/2)*sds/sqrt(reps)
ylim = c(0,0.42)

line.lwd = 4
point.lwd = 4
text.cex = 1
par(lwd = line.lwd, cex = 2, cex.lab = text.cex,
    cex.lab = text.cex,
    mai = c(1,1,0.1,0.2),
    mgp = c(0,0.5,0),
    family = 'serif')
  
plot.new()
plot.window(
            xlim = c(1,200),
            ylim = ylim,
            xaxs='r',
            yaxs='r')
axis(1)
axis(2)
title(xlab = 'Number of Topics',
      ylab = expression(paste('Average ',F[1],' Score')),
      mgp=c(1.4,0,0))


for(i in 1:ncol(means)) {
  if(sum(names[i] == c('MMSB','baseline 1','LSM'))) {
 #   points(sizes[1], means[1,i], pch = i, col = colors[i],lwd=point.lwd)
 #   points(sizes[length(sizes)], means[1,i], pch = i, col = colors[i],lwd=point.lwd)
  } else {
    points(sizes, means[,i], pch = 19, col = colors[i], cex=0.57, lwd=point.lwd)
  }
}

for(i in 1:ncol(means)) {
  lines(sizes, means[,i], col = colors[i], lty=i)
  if(plot.ci) {
    arrows(sizes, upper[,i], sizes, lower[,i],
           code = 3, angle = 90, length = .1*range(sizes),
           col = colors[i])
  }
}



y <- 0.1
x <- 10
legend(x,y,
       bty = 'n',
       names[1:3],
#       pch = 1:3,
       col = colors[ 1:3 ],
       lty = 1:3,
       lwd = line.lwd,
       pt.lwd = point.lwd,
#       cex = text.cex
       )

x <- x + 97
legend(x,y,
       bty = 'n',
       names[4:6],
#       pch = 4:6,
       col = colors[4:6],
       lty = 4:6,
       lwd = line.lwd,
       pt.lwd = point.lwd,
#       cex = text.cex
       )

  

dev.off()

