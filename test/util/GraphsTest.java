package util;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class GraphsTest {

	@Test
	public void testShortestPath() {
		
		double i = Double.POSITIVE_INFINITY;
		double[][] graph = new double[][]{
				{0, 0.125, 1, i}, 
				{0.25, 0, 0.5, 0.125}, 
				{i, i, 0, i}, 
				{1, i, 0.125, 0}};
		double[][] trueValue = new double[][]{
				{0, 0.125, 0.375, 0.25},
				{0.25, 0, 0.25, 0.125},
				{i, i, 0, i},
				{1, 1.125, 0.125, 0}};
				
		assertArrayEquals(trueValue, Graphs.getShortestPathsFloydWarshall(graph));
	}

}
