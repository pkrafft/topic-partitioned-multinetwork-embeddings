package util;

public class Graphs {

	/**
	 * Calculate the shortest path between all nodes in a graph.
	 * 
	 * @param distanceGraph
	 *            The matrix containing the distance between all pairs of nodes.
	 * @return A matrix giving the geodesic graph distance between each pair of
	 *         nodes.
	 */
	public static double[][] getShortestPathsFloydWarshall(
			double[][] distanceGraph) {

		for (int k = 0; k < distanceGraph.length; k++) {
			for (int i = 0; i < distanceGraph.length; i++) {
				for (int j = 0; j < distanceGraph.length; j++) {
					distanceGraph[i][j] = Math.min(distanceGraph[i][j],
							distanceGraph[i][k] + distanceGraph[k][j]);
				}
			}
		}

		return distanceGraph;
	}

}
