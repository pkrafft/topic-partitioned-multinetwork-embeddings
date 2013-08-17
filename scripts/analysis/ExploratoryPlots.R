#setwd("/Users/brucedesmarais/Desktop/Research/Java-Network-Models/output-for-bruce/latent-spaces/")
inDir = '../output-nips/RichExperiment-0-100-long-nhc'

topics <- read.table(paste(inDir,"/topic_summary.txt",sep=''),sep=",")
managers <- read.delim(paste(inDir,"/managers_departments.txt",sep=''))
authorProps <- read.table(paste(inDir,"/author-topic-counts.csv",sep=''),sep=",")

dataFile = paste(inDir,'/latent_spaces.txt',sep='')
interceptFile = paste(inDir,'/intercepts.txt',sep='')
latentDim = 2
numActors = 30
numFeatures = 100
data = t(as.matrix(read.table(dataFile, sep = ',')))
data = array(data, c(latentDim, numActors, numFeatures))
intercepts = read.table(intercepts)

latent_spaces = data

ls_list <- list()
for(i in 1:dim(latent_spaces)[3]){
	ls_list[[i]] <- t(latent_spaces[,,i])
	}

labels <- c("PS","CE","AM","FN","CM","EL","AM","BG","PI","FS","HR","HL","SF","VS","LB","PM","RD","EV","EG","PG","YS","SS","IT","MS","TX","CC","RM","DS","CA","EM")
#labels <- 0:29
deps <- c("red","blue","black","darkgreen","black","black","black","darkgreen","red","red","blue","red","black","red","blue","blue","black","red","red","blue","red","red","blue","blue","darkgreen","black","black","red","black","blue")

# Color starts: red=0, yellow=1/6, green=2/6, cyan=3/6, blue=4/6 and magenta=5/6

color.convert <- function(color,trans){
	# Color is a string color indicator of blue, red, black, green
	# trans is in 0,1 with 0 being completely transparent, and 1 being not transparent
	color_key <- cbind(c(0,2/6,4/6),c("red","darkgreen","blue"))
	not_black <- which(color!="black")
	col_nb <- color[not_black]
	new_col <- numeric(length(color))
	for(i in 1:length(not_black)){
	new_col[not_black[i]] <- rainbow(1,start=color_key[match(col_nb[i],color_key[,2]),1],alpha=trans[not_black[i]])
	}
	new_col[-not_black] <- gray(1-trans[-not_black])
	new_col
	}
	
color.convert <- function(color,trans){
	# Color is a string color indicator of blue, red, black, green
	# trans is in 0,1 with 0 being completely transparent, and 1 being not transparent
	rgb3 <- t(col2rgb(color))
	cols <- numeric(nrow(rgb3))
	for(i in 1:length(cols)){
	cols[i] <- rgb(red=rgb3[i,1],green=rgb3[i,2],blue=rgb3[i,3],alpha=trans[i]*255,maxColorValue=255)
	}
	cols
	}

# Explore topics
#i <- 1
zoom <- F
for(i in 1:numFeatures) {
  if(sum(authorProps[,i]) > 0) {
     pdf(paste(inDir,'/latent-space-',i-1,'.pdf',sep=''))
     net <- read.csv(paste(inDir,'/topic-net-',i,'.csv',sep=''),F)
     if(zoom) {
       accept <- authorProps[,i] > 0
     } else {
       accept <- authorProps[,i] >= 0
     }
     ce <- 0.45+1.25*sqrt(authorProps[,i])/sqrt(max(authorProps[,i]))
     trans <- pexp(ce)
     plot(ls_list[[i]][accept,],main=i-1,col="white",ylab="",xlab=""); 
     for(a in 1:(numActors-1)) {
	for(r in (a + 1):numActors)
	if (net[a,r] + net[r,a] > 0) {
          p <- ls_list[[i]][a,]
          q <- ls_list[[i]][r,]
          y <- net[a,r] + net[r,a]
          v <- 0.4*(1 - exp(-sqrt(y)/4))
          lines(c(p[1],q[1]), c(p[2],q[2]),col=rgb(0,0,0,v),lwd=sqrt(y)/2)
	}
      }
     cols <- color.convert(deps,trans)
     text(ls_list[[i]][accept,],lab=labels[accept],cex=ce[accept],col=cols[accept])
     dev.off()
   }
}
#i <- i+1
# Clustered, visually - 44 (i=45), 47 (i=48)
# Not Clustered, visually - 59 (i=60), Actors clustered in the center 44 (i=45)

topics <- c(45,47,59,44)
# Make latent position plots
for(j in topics){
	filej <- paste("/Users/brucedesmarais/Desktop/Research/Java-Network-Models/icml2012/ls_topic",j,".pdf",sep="")
	i <- j+1
	pdf(filej,height=4,width=4.5,pointsize=12)
	par(las=1,mar=c(3.25,3.25,.5,.5))
	ce <- 0.45+1.25*sqrt(authorProps[,i])/sqrt(max(authorProps[,i]))
	trans <- pexp(ce)
	trans <- (trans/max(trans))^(6/5)
	plot(ls_list[[i]],main="",col="white",ylab="",xlab=""); 
	text(ls_list[[i]],lab=labels,cex=ce,col=color.convert(deps,trans))
	dev.off()
	}

### Department Key ###
dept_acro <- cbind(as.character(managers[,3]),labels)
depcol <- deps[order(dept_acro[,1])]
dept_acro <- dept_acro[order(dept_acro[,1]),]

dept_acro <- cbind(dept_acro,depcol)

dept_acro <- unique(dept_acro)

pdf("/Users/brucedesmarais/Desktop/Research/Java-Network-Models/icml2012/dep_key.pdf",height=4,width=1.75,pointsize=7)
par(mar=c(.1,.1,.1,.1))
plot(2,2,col="white",xlim=c(0,4.5),ylim=c(1,nrow(dept_acro)),xaxt="n",yaxt="n",ylab="",xlab="",main="",bty="n")
text(rep(0,nrow(dept_acro)),nrow(dept_acro):1,lab=dept_acro[,1],col=dept_acro[,3],pos=4)
text(rep(4,nrow(dept_acro)),nrow(dept_acro):1,lab=dept_acro[,2],col=dept_acro[,3],pos=4)
dev.off()

