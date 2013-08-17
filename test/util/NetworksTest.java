package util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NetworksTest {

	@Test
	public void testGetTransitivity() {
		int[][] network;
		Network net;
		
		network = new int[][]{
				{0, 1, 2},
				{3, 0, 1},
				{1, 0, 0}};
		
		net = new Network(network);
		assertEquals(9/11.0, net.getTransitivity(), 1e-12);
		
		network = new int[][]{
				{0, 1, 1, 0, 0, 0},
				{1, 0, 1, 1, 1, 0},
				{1, 1, 0, 0, 0, 0},
				{0, 1, 0, 0, 0, 0},
				{0, 1, 0, 0, 0, 1},
				{0, 0, 0, 0, 1, 0}};
		
		net = new Network(network);
		assertEquals(0.33, net.getTransitivity(), 1e-2);
		
		network = new int[][]{
				{0, 4, 2, 0, 0, 0},
				{4, 0, 4, 1, 2, 0},
				{2, 4, 0, 0, 0, 0},
				{0, 1, 0, 0, 0, 0},
				{0, 2, 0, 0, 0, 1},
				{0, 0, 0, 0, 1, 0}};
		
		net = new Network(network);
		assertEquals(40/96.0, net.getTransitivity(), 1e-2);
	}
	
	@Test
	public void testGetInwardConnectedness() {
		int[][] network = new int[][]{
				{0, 8, 1, 0}, 
				{4, 0, 2, 8}, 
				{0, 0, 0, 0}, 
				{1, 0, 8, 0}};
		
		Network net = new Network(network);
		
		// the type of these should be double[], but my current version of JUnit doesn't allow that... 
		double[][] trueValue = new double[][]{{0, 0, 4, 0}};
		assertArrayEquals(trueValue, new double[][]{net.getVertexInwardConnectedness()});
	}

	@Test
	public void testGetOutwardConnectedness() {
		int[][] network = new int[][]{
				{0, 8, 1, 0}, 
				{4, 0, 2, 8}, 
				{0, 0, 0, 0}, 
				{1, 0, 8, 0}};
		
		Network net = new Network(network);
		
		// the type of these should be double[], but my current version of JUnit doesn't allow that... 
		double[][] trueValue = new double[][]{{3/0.75, 3/0.625, 0, 3/2.25}};
		assertArrayEquals(trueValue, new double[][]{net.getVertexOutwardConnectedness()});
	}
	
	@Test
	public void testGetInDegree() {
		int[][] network = new int[][]{{1, 0, 1}, {0, 0, 2}, {1, 0, 1}};
		Network net = new Network(network);
		assertArrayEquals(new int[]{1, 0, 3}, net.getVertexInDegrees());
	}

	@Test
	public void testGetOutDegree() {
		int[][] network = new int[][]{{1, 0, 1}, {0, 0, 2}, {1, 0, 0}};
		Network net = new Network(network);
		assertArrayEquals(new int[]{1, 2, 1}, net.getVertexOutDegrees());
	}

}
