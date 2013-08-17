dataset <- 'nhc'
plot.ci <- F

d <- read.table(paste('../output-nips/output-',dataset,'/coherence.csv',sep=''),sep=',',stringsAsFactors=F)

sizes = c(1,2,5,30,50,75,100,150,200)
names = unique(d[,1])
means = matrix(NA,length(sizes),length(names))
colnames(means) = names
colors = grey(seq(0,0.5,length=length(names)))

i = 1
for(exp in names) {
  temp = d[d[,1] == exp,]
  temp = aggregate(temp[,4],list(temp[,3]),mean)
  means[1:length(temp[[2]]),i] = temp[[2]]
  i = i + 1
}

names = c('LDA','our model','Erosheva','baseline 2')
colnames(means) <- names

permutation <- c('our model','Erosheva','baseline 2','LDA')
means <- means[,permutation]
names <- permutation

pdf(paste('./writing/nips-edited/coherence-',dataset,'.pdf', sep = ''), height = 7, width = 7)


p <- 0.05
upper <- means + qnorm(1 - p/2)*sds/sqrt(reps)
lower <- means + qnorm(p/2)*sds/sqrt(reps)
ylim = c(-115,-50)

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
      ylab = expression(paste('Average Topic Coherence')),
      mgp=c(1.4,0,0))


for(i in 1:ncol(means)) {
    points(sizes, means[,i], pch = 19, col = colors[i], cex=0.57, lwd=point.lwd)
}

for(i in 1:ncol(means)) {
  lines(sizes, means[,i], col = colors[i], lty=i)
  if(plot.ci) {
    arrows(sizes, upper[,i], sizes, lower[,i],
           code = 3, angle = 90, length = .1*range(sizes),
           col = colors[i])
  }
}



y <- -45
x <- 113

legend('topright',
       bty = 'n',
       names[1:4],
#       pch = 1:4,
       col = colors[ 1:4 ],
       lty = 1:4,
       lwd = line.lwd,
       pt.lwd = point.lwd,
#       cex = text.cex
       )

## x <- x + 97
## legend(x,y,
##        bty = 'n',
##        names[4:6],
## #       pch = 4:6,
##        col = colors[4:6],
##        lty = 4:6,
##        lwd = line.lwd,
##        pt.lwd = point.lwd,
## #       cex = text.cex
##        )

  

dev.off()


