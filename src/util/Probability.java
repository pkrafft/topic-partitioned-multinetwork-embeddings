package util;

public class Probability implements Comparable<Probability> {

  public int index;
  public double prob;

  public Probability(int index, double prob) {

    this.index = index;
    this.prob = prob;
  }

  public final int compareTo(Probability o) {

    if (prob > o.prob)
      return -1;
    else if (prob == o.prob)
      return 0;
    else
      return 1;
  }
}
