paste = function(...) .Internal(paste(list(...), "", NULL))
source('./scripts/analysis/plot.topic.proportions.R')

n.topics <- 100
n.authors <- 30

in.folder <- paste('../output-nips/RichExperiment-0-',n.topics,'-long-nhc')
#in.folder <- paste('../output-nips/ConditionalStructureExperiment-0-',n.topics,'-missing')
#in.folder <- paste('../output-nips/ExchangeableStructureExperiment-0-100-missing')
#in.folder <- './'
log.prob <- read.table(paste(in.folder,'/log_prob.txt'),sep = ',')
e <- read.table(paste(in.folder,'/edge_state.txt'),sep=',')
w <- read.table(paste(in.folder,'/word_state.txt'),sep=',',stringsAsFactors=F)

w.t <- list(c())

doc.names <- list(c())

last.doc <- 0
ind <- 1
for(i in 1:nrow(w)) {
  doc <- w[i,1] + 1
  topic <- w[i,6]
  if(doc > last.doc) {
    last.doc <- doc
    ind = 1
  }
  if(is.null(w.t[doc][[1]])) {
    w.t[[doc]] = topic
    doc.names[[doc]] <- w[i,2]
  } else {
    w.t[[doc]][ind] = topic
  }
  ind = ind + 1
}

pdf(paste(in.folder,'/topic-assignments-pretty-1.pdf'))
plot.topic.proportions(w.t)
dev.off()
pdf(paste(in.folder,'/topic-assignments-pretty-2.pdf'))
plot.topic.proportions(w.t, bar.style='log')
dev.off()

n.docs <- length(w.t)

a.t <- matrix(0,n.authors,n.topics)

edge.topics <- rep(NA,nrow(e))
topic.nets <- array(0,c(n.authors,n.authors,n.topics))

for(i in 1:nrow(e)) {

  doc = e[i,1] + 1
  token = e[i,7] + 1
  topic = w.t[[doc]][token]
  
  edge.topics[i] = topic
  
  if(e[i,6] == 1) {
    author = e[i,4] + 1
    recipient = e[i,5] + 1
    topic = topic + 1
    y <- topic.nets[author,recipient,topic]
    topic.nets[author,recipient,topic] <- y + 1
    a.t[author,topic] = a.t[author,topic] + 1
    a.t[recipient,topic] = a.t[recipient,topic] + 1
  }
}

e.2 <- e
e.2[,ncol(e.2)] <- edge.topics


for(t in 1:n.topics) {
  write.table(topic.nets[,,t],
              file=paste(in.folder,'/topic-net-',t,'.csv'),
              sep=',',row.names=F,col.names=F)
}

topic.edge.counts <- apply(a.t,2,sum)/2

write.table(a.t,
            file=paste(in.folder,'/author-topic-counts.csv'),
            sep=',',row.names=F,col.names=F)
write.table(topic.edge.counts,
            file=paste(in.folder,'/topic-prevalence-number-edges-explained.csv'),
            sep=',',row.names=F,col.names=F)

topic.counts <- matrix(0,n.topics,1)

for(d in 1:length(w.t)) {
  for(t in 1:n.topics) {
    if((t - 1) %in% w.t[[d]]) {
      topic.counts[t] <- topic.counts[t] + 1
    }
  }
}

write.table(topic.counts,
            file=paste(in.folder,'/topic-prevalence-number-docs-containing.csv'),
            sep=',',row.names=F,col.names=F)

mode.percent <- function(y) sort(table(y),TRUE)[1]/length(y)
percent.largest <- lapply(w.t,mode.percent)
pdf(paste(in.folder,'/topic-assignment-percents.pdf'))
hist(unlist(percent.largest),xlab='percent tokens assigned to largest topic',main='')
dev.off()

num.unique <- function(y) length(unique(y))
unique.topics <- unlist(lapply(w.t,num.unique))
pdf(paste(in.folder,'/topic-assignments-unique.pdf'))
hist(unique.topics,seq(0.5,max(unique.topics)+0.5,by=1),xlab='number of topics per document',main='')
dev.off()

num.tokens <- unlist(lapply(w.t,length))
pdf(paste(in.folder,'/topic-assignments.pdf'))
plot(log(num.tokens),jitter(unique.topics), xlab='log(number of tokens per document)', ylab='number of topics per document')
dev.off()

number.explained <- matrix(NA,nrow(e.2),4)
colnames(number.explained) <- c('doc','length','topic','numTokens')
for( i in 1:nrow(e.2) ) {
  doc <- e.2[i,1] + 1
  t <- as.character(e.2[i,7])
  number.explained[i,] <- c(doc - 1, length(w.t[[doc]]), e.2[i,7], table(w.t[[doc]])[t])
}
unique.number.explained <- unique(number.explained)
pdf(paste(in.folder,'/topic-assignment-counts.pdf'))
plot(jitter(unique.number.explained[,'length']),
     jitter(unique.number.explained[,'numTokens']),
     xlim=c(0,50),ylim=c(0,50),
     xlab = 'document length (jittered)',
     ylab = 'number of tokens assigned to a topic per document (jittered)',
     main = 'topic concentration by document length')
dev.off()

table(number.explained[,'numTokens'])

bad.token.edges <- number.explained[number.explained[,'numTokens'] == 1,]
a <- bad.token.edges[,'doc']
b <- bad.token.edges[,'topic']
edges.to.token <- aggregate(bad.token.edges[,'numTokens'],list(a,b),length)
pdf(paste(in.folder,'/topic-assignments-determined-by-edges.pdf'))
hist(edges.to.token[,3],xlab='#edges per document explained by a single token with a topic unique to that document',
     main = 'edges defining topics')
dev.off()

num.single <- sum(unlist(lapply(w.t,function(y) sum(table(y) == 1))))
print('number of tokens assigned to unique topics in their documents:')
print(num.single)

num.single.explaining <- nrow(unique(bad.token.edges))
print('number of tokens assigned to unique topics in their documents that are used to explain edges:')
print(num.single.explaining)
print('percentage of single topic tokens used to explain edges:')
print(num.single.explaining/num.single)

num.tokens.explaining.edges <- nrow(unique(e[,c(1,7)]))
num.tokens <- nrow(w)
print('percentage of all tokens used to explain edges:')
print(num.tokens.explaining.edges/num.tokens)

print('percentage of tokens assigned to single topics:')
print(num.single/num.tokens)

all.assigned <- function(t) e.2[e.2[,7] == t & e.2[,6] == 1,c(1,2,4,5)]
between <- function(i,j,t) {
  m <- all.assigned(t)
  m[(m[,3] == i & m[,4] == j) | (m[,3] == j & m[,4] == i),]
}

doc.topics <- matrix(NA,n.docs,n.topics)
data <- strsplit(readLines(paste(in.folder,'/doc_topics.txt')), ' ')
for (i in 2:length(data)) {

  d <- i - 1
  
  for ( j in seq(length(data[[i]]) - n.topics*2 + 1, length(data[[i]]) - 1, by = 2) ) {
    
    t <- as.numeric(data[[i]][j]) + 1
    proportion <- as.numeric(data[[i]][j + 1])
    doc.topics[d, t] <- proportion
  }
}

top.docs <- function(t, num = 10) {
  ind <- order(doc.topics[,t + 1], decreasing = TRUE)[1:num]
  docs <- doc.names[ind]
  names(docs) <- ind
  docs
}

represent.docs <- function(t,num=6) {
  top <- names(top.docs(t,num))
  assigned <- all.assigned(t)[,1] + 1
  assigned <- table(assigned)
  assigned <- names(assigned)[order(assigned, decreasing=TRUE)]
  inter <- intersect(top,assigned)
  if(length(inter) > 0) {
    ind <- inter[1:min(length(inter),ceiling(num/3))]
  }
  r <- num - length(ind)
  top <- setdiff(top, ind)
  assigned <- setdiff(assigned, inter)
  assigned <- assigned[order(doc.topics[as.numeric(assigned),t+1], decreasing=TRUE)]
  top.i <- 1
  ass.i <- 1
  for(i in 1:r) {
    if(i %% 2 & ass.i <= length(assigned)) {
      ind <- c(ind, assigned[ass.i])
      ass.i <- ass.i + 1
    } else {
      ind <- c(ind, top[top.i])
      top.i <- top.i + 1
    }
  }
  ind
}

doc.names[as.numeric(represent.docs(11))]
doc.names[as.numeric(represent.docs(15))]
doc.names[as.numeric(represent.docs(16))]
doc.names[as.numeric(represent.docs(17))]
doc.names[as.numeric(represent.docs(25))]
doc.names[as.numeric(represent.docs(31))]
doc.names[as.numeric(represent.docs(63))]
doc.names[as.numeric(represent.docs(86))]
