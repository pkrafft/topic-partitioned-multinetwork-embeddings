paste = function(...) .Internal(paste(list(...), "", NULL))
source('./scripts/plot.topic.proportions.R')

n.authors <- 30

in.folder <- paste('../output-nips')

n.topics <- 100
lda.100.folder <- paste(in.folder,'/LDAExperiment-1-',n.topics)
tpme.100.folder <- paste(in.folder,'/ConditionalStructureExperiment-1-',n.topics,'-missing')

n.topics <- 50
lda.50.folder <- paste(in.folder,'/LDAExperiment-1-',n.topics)
tpme.50.folder <- paste(in.folder,'/ConditionalStructureExperiment-1-',n.topics,'-missing')

n.topics <- 30
lda.30.folder <- paste(in.folder,'/LDAExperiment-1-',n.topics)
tpme.30.folder <- paste(in.folder,'/ConditionalStructureExperiment-1-',n.topics,'-missing')

n.topics <- 150
lda.150.folder <- paste(in.folder,'/LDAExperiment-1-',n.topics)
tpme.150.folder <- paste(in.folder,'/ConditionalStructureExperiment-1-',n.topics,'-missing')

percent.assignments <- function(topic.assignments) {

  n.docs <- length(topic.assignments)
  doc.lengths <- sapply(topic.assignments, length)
  
  counts <- lapply(topic.assignments,
                        function(x) sort(table(x), decreasing=T))
  
  m <- matrix(0, n.docs, max(sapply(counts, length)))
  for(i in 1:n.docs) {
    m[i,] <- c(counts[[i]], rep(0,ncol(m) - length(counts[[i]])))
  }

  return(apply(m,2,sum)/sum(m))
}


get.topic.assignments <- function(in.folder) {
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
  
  return(w.t)
}

lda.100.ass <- get.topic.assignments(lda.100.folder)
tpme.100.ass <- get.topic.assignments(tpme.100.folder)
lda.100.percents <- percent.assignments(lda.100.ass)
tpme.100.percents <- percent.assignments(tpme.100.ass)

lda.50.ass <- get.topic.assignments(lda.50.folder)
tpme.50.ass <- get.topic.assignments(tpme.50.folder)
lda.50.percents <- percent.assignments(lda.50.ass)
tpme.50.percents <- percent.assignments(tpme.50.ass)

lda.30.ass <- get.topic.assignments(lda.30.folder)
tpme.30.ass <- get.topic.assignments(tpme.30.folder)
lda.30.percents <- percent.assignments(lda.30.ass)
tpme.30.percents <- percent.assignments(tpme.30.ass)

lda.150.ass <- get.topic.assignments(lda.150.folder)
tpme.150.ass <- get.topic.assignments(tpme.150.folder)
lda.150.percents <- percent.assignments(lda.150.ass)
tpme.150.percents <- percent.assignments(tpme.150.ass)


percentages <- list(lda.30.percents, 
                    lda.50.percents, 
                    lda.100.percents,
                    lda.150.percents, 
                    tpme.30.percents,
                    tpme.50.percents,
                    tpme.100.percents,
                    tpme.150.percents)
labels <- c('lda30',
            'lda50',
            'lda100',
            'lda150',
            'tpme30',
            'tpme50',
            'tpme100',
            'tpme150')

pdf(paste(in.folder,'/topic-assignments-pretty.pdf'))
plot.model.percents(percentages,labels)
dev.off()


