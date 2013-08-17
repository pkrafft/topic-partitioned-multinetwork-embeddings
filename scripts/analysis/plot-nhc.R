
data <- read.csv('../data/nhc/edge-matrix.csv',F)
data <- as.matrix(data[,2:ncol(data)])
graph <- matrix(0,30,30)
for(i in 1:nrow(data)) {
  a <- data[i,1] + 1
  graph[a,] <- graph[a,] + data[i,2:ncol(data)]
}

departments <- c("PS","CE","AM","FN","CM","EL","AM","BG","PI","FS","HR","HL","SF","VS","LB","PM","RD","EV","EG","PG","YS","SS","IT","MS","TX","CC","RM","DS","CA","EM")

	library(sna)
	
	edge_color <- matrix(0,nrow(graph),nrow(graph))
	
	### adapted from Bruce Desmarais's code
	for(i in 1:nrow(graph)) {
		for(j in 1:nrow(graph)) {
			p <- mean(c(graph) <= graph[i,j])
			num <- round(50 + (1 - p)*150)
			edge_color[i,j] <- paste("grey", num, sep="")
			if(graph[i,j] == 0) {
				edge_color[i,j] <- "white"
			}
		}
	}
	
	# this is a hack, how else to initialize the random seed?
	junk <- runif(1)
	old_seed <- .Random.seed
	set.seed(25)
	pdf("../output/nhnet.pdf", height=5,width=5,
		pointsize=10,family="Times")
	#par(mar=c(.01,.01,.01,.01))
	xy <- gplot(graph, label=departments, label.cex=.55, edge.col=edge_color, vertex.col="orange",
		vertex.border="orange")
	dev.off()
	.Random.seed <- old_seed
	### end adapted
