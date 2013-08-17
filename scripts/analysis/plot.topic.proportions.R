
plot.topic.proportions <- function(topic.assignments,
                                   bar.style = 'equal',
                                   cols = rainbow(5)) {
		
	n.docs <- length(topic.assignments)
	doc.lengths <- sapply(topic.assignments, length)
	
	percentages <- lapply(topic.assignments,
                              function(x) sort(table(x)/length(x), decreasing=T))

        m <- matrix(0, n.docs, max(sapply(percentages, length)))
        for(i in 1:n.docs) {
          m[i,] <- c(percentages[[i]], rep(0,ncol(m) - length(percentages[[i]])))
        }
        m <- cbind(doc.lengths,m)
        permutation <- do.call(order, as.list(data.frame(m)))
        
	plot.new()
	
	x.min <- 0
        if(bar.style == 'equal') {
          x.max <- 1
        } else if(bar.style == 'log') {
          x.max <- log(max(doc.lengths) + 1)
        }
	y.min <- 0.5
	y.max <- n.docs + 0.5
	
	plot.window(c(x.min, x.max), c(y.min, y.max))
        pos <- seq(log(2), x.max, length = 5)
        if(bar.style == 'equal') {
          title(ylab = 'document', main = 'document topic proportions')
        } else if (bar.style == 'log') {
          axis(1,
             at = pos,
             labels = paste('log(',round(exp(pos) - 1,0),'+1)'))
          title(ylab = 'document', xlab = 'log(document length + 1)',
                main = 'document topic proportions')
        }
	
	for(i in 1:n.docs) {
		
		d <- permutation[i]
		
		props <- percentages[[d]]
                if(bar.style == 'equal') {
                  bar.length <- 1
                } else if(bar.style == 'log') {
                  bar.length <- log(doc.lengths[d] + 1)
                }
		
		right.side <- x.min
		c <- 1
		for(t in 1:length(props)) {
			left.side <- right.side
			right.side <- left.side + props[t]*bar.length
			rect(left.side, i - 0.5, right.side, i + 0.5, col = cols[c],
                             border=NA)
			c <- c + 1
			if(c > length(cols)) {
				c <- 1
			}
		}
	}
}

plot.model.percents <- function(percentages, labels, cols = rainbow(5)) {

  n.models <- length(percentages)
  
  m <- matrix(0, n.models, max(sapply(percentages, length)))
  for(i in 1:n.models) {
    m[i,] <- c(percentages[[i]], rep(0,ncol(m) - length(percentages[[i]])))
  }
  
  plot.new()
  
  x.min <- 0
  x.max <- 1
  y.min <- 0.5
  y.max <- n.models + 0.5
  
  plot.window(c(x.min, x.max), c(y.min, y.max))
  title(ylab = 'model', main = 'percent tokens assigned to topics')
  axis(2, at = 1:n.models, labels = labels)
	
  for(i in 1:n.models) {
    
    props <- percentages[[i]]
    bar.length <- 1
    
    right.side <- x.min
    c <- 1
    for(t in 1:length(props)) {
      left.side <- right.side
      right.side <- left.side + props[t]*bar.length
      rect(left.side, i - 0.5, right.side, i + 0.5, col = cols[c],
           border=NA)
      c <- c + 1
      if(c > length(cols)) {
        c <- 1
      }
    }
  }
}
