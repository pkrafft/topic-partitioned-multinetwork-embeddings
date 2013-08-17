w.sum <- function(m,v) sum((m/v)*1/sum(1/v))
compute.hyper <- function(data, round = TRUE) {
  m <- aggregate(data[,5],list(data[,4]),mean)[range,2]
  v <- aggregate(data[,5],list(data[,4]),var)[range,2]
  value <- w.sum(m,v)
  if(round) {
    value <- round(value)
  }
  print(value)
}

d <- read.csv('alpha-optim.txt',F)

n.l <- d[d[,1] == 'nhc' & d[,2] == 'LDAExperiment',]
n.t <- d[d[,1] == 'nhc' & d[,2] == 'TextMixtureExperiment',]
e.l <- d[d[,1] == 'enron' & d[,2] == 'LDAExperiment',]
e.t <- d[d[,1] == 'enron' & d[,2] == 'TextMixtureExperiment',]

range <- 4:9

print('NHC LDA alpha:')
compute.hyper(n.l)

print('NHC mixture alpha:')
compute.hyper(n.t)

print('Enron LDA alpha:')
compute.hyper(e.l)

print('Enron mixture alpha:')
compute.hyper(e.t)


d <- read.csv('beta-optim.txt',F)

n.words.nhc <- 6275
n.words.enron <- 23274

n.l <- d[d[,1] == 'nhc' & d[,2] == 'LDAExperiment',]
n.t <- d[d[,1] == 'nhc' & d[,2] == 'TextMixtureExperiment',]
e.l <- d[d[,1] == 'enron' & d[,2] == 'LDAExperiment',]
e.t <- d[d[,1] == 'enron' & d[,2] == 'TextMixtureExperiment',]

n.l[,5] <- n.l[,5]/n.words.nhc
n.t[,5] <- n.t[,5]/n.words.nhc
e.l[,5] <- e.l[,5]/n.words.enron
e.t[,5] <- e.t[,5]/n.words.enron

print('NHC mixture alpha:')
compute.hyper(n.t, F)

print('Enron mixture alpha:')
compute.hyper(e.t, F)
