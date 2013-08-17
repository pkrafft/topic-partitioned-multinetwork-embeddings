
folder <- '../output-summaries/'

models <- c('joint-missing', 'disjoint-model-missing', 'mmbm-missing',
'mmlsm-missing', 'baseline', 'lsm-missing', 'joint', 'lda', 'mmbm')

indices <- matrix(c(4,5,9, 4,5,7, 4,5,7, 4,5,9, 4,5,6, 4,6,8, 4,5,9,
4,5,7, 4,5,7),3,length(models))

subsets <- list(c(1:4,7:9))

numInstances <- vector("list", length = length(subsets))

for(j in 1:length(subsets)) {

  sizes <- NULL

  results <- NULL
  deviations <- NULL
  upper <- NULL
  lower <- NULL
  
  for(i in 1:length(subsets[[j]])) {
    
    time <- read.csv(paste(folder, models[subsets[[j]][i]], '-time.csv', sep = ''), sep = '\t', header = F)
    time <- time[,indices[,subsets[[j]][i]]]
    
    MinTime <- aggregate(time[,3], list(time[,2]), min)
    MeanTime <- aggregate(time[,3], list(time[,2]), mean)
    MaxTime <- aggregate(time[,3], list(time[,2]), max)
    
    if(is.null(sizes)) {
      sizes = MinTime[,1]
    }

    num <- nrow(MinTime)

    if(nrow(MinTime) == 1) {
      num <- length(sizes)
    }

    add <- rep(NA, length(sizes))
    add[1:num] <- MeanTime[,2]
    results <- cbind(results, add)
    
    add <- rep(NA, length(sizes))
    add[1:num] <- MaxTime[,2]
    upper <- cbind(upper, add)

    add <- rep(NA, length(sizes))
    add[1:num] <- MinTime[,2]
    lower <- cbind(lower, add)

    add <- rep(NA, length(sizes))
    add[1:length(MeanTime[,2])] <- table(time[,2])
    numInstances[[j]] <- cbind(numInstances[[j]], add)
  }
  
}

print(numInstances)
