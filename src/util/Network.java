package util;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * A class for representing network data and calculating various network
 * statistics.
 * 
 * Each network is represented as two matrices, one of "present connections" and
 * on of "absent connections". The present connections give the number of times
 * each actor chooses to communicate with each other actor. The absent
 * connections give the number of times each actor chooses not to communicate
 * with each other actor.
 */
public class Network {

	int[][] presentEdges;
	int[][] absentEdges;

	/**
	 * 
	 * @param network
	 *            the matrix representation of the present connections in a
	 *            network
	 */
	public Network(int[][] network) {
		this.presentEdges = network;
	}

	/**
	 * 
	 * @param presentEdges
	 *            the matrix representation of the present connections in a
	 *            network
	 * @param absentEdges
	 *            the matrix representation of the absent connections in a
	 *            network
	 */
	public Network(int[][] presentEdges, int[][] absentEdges) {
		this.presentEdges = presentEdges;
		this.absentEdges = absentEdges;
	}

	/**
	 * Calculate the scalar value global generalized transitivity of the network
	 * using the present connections.
	 * 
	 * A triple is a tuple of three nodes (A, B, C). A triple is nonvacuous if
	 * and only if A -> B and B -> C. A nonvacuous triple is transitive if and
	 * only if A -> C.
	 * 
	 * The global transitivity of a binary network is the number of nonvacuous
	 * transitive triples divided by the number of nonvacuous triples. The
	 * generalized transitivity for weighted networks is the sum of the values
	 * of all nonvacuous transitive triples divided by the sum of the values of
	 * all nonvacuous triples, where the value of a nonvacuous triple is the sum
	 * of the values on the edges A -> B and B -> C. This definition was
	 * introduced by Opsahl and Panzarasa (2009).
	 * 
	 * @return the sum of the values of all nonvacuous transitive triples
	 *         divided by the sum of the values of all nonvacuous triples.
	 */
	public double getTransitivity() {

		int nonvacuousTriples = 0;
		int transitiveTriples = 0;

		for (int i = 0; i < presentEdges.length; i++) {

			for (int j = 0; j < presentEdges.length; j++) {

				if (i != j && presentEdges[i][j] > 0) {

					for (int k = 0; k < presentEdges.length; k++) {

						if (j != k && i != k && presentEdges[j][k] > 0) {

							nonvacuousTriples += presentEdges[i][j];
							nonvacuousTriples += presentEdges[j][k];

							if (presentEdges[i][k] > 0) {
								transitiveTriples += presentEdges[i][j];
								transitiveTriples += presentEdges[j][k];
							}
						}
					}
				}
			}
		}

		return transitiveTriples / (double) nonvacuousTriples;
	}

	/**
	 * Calculate the geodesic distance between all pair of actors.
	 * 
	 * The distance between a pair of actors is defined as the reciprocal of the
	 * number of present connections between the two actors. The geodesic
	 * distance between those actors is defined as distance of the shortest path
	 * between the two actors in the graph formed by the distances between all
	 * actor.
	 * 
	 * @return a matrix of the geodesic distances between each pair of actors
	 */
	public double[][] getGeodesicDistances() {

		double[][] distanceGraph = new double[presentEdges.length][presentEdges.length];
		for (int i = 0; i < distanceGraph.length; i++) {
			for (int j = 0; j < distanceGraph.length; j++) {
				if (i != j) {
					distanceGraph[i][j] = 1.0 / presentEdges[i][j];
				} else {
					distanceGraph[i][j] = 0;
				}
			}
		}

		distanceGraph = Graphs.getShortestPathsFloydWarshall(distanceGraph);

		return distanceGraph;
	}

	/**
	 * Calculate Fowler's connectedness for each actor.
	 * 
	 * The inward connectedness of an actor is defined as the reciprocal of the
	 * average geodesic distance from each other actor to that actor. The
	 * outward connectedness is the reciprocal of the average geodesic distance
	 * from that actor to each other actor.
	 * 
	 * @param outward
	 *            whether to calculate inward or outward connectedness
	 * @return the connectedness for each actor
	 */
	public double[] getVertexConnectedness(boolean outward) {

		double[][] distanceGraph = getGeodesicDistances();

		double[] connectedness = new double[presentEdges.length];
		for (int i = 0; i < connectedness.length; i++) {
			for (int j = 0; j < connectedness.length; j++) {
				if (outward) {
					connectedness[i] += distanceGraph[i][j];
				} else {
					connectedness[i] += distanceGraph[j][i];
				}
			}
			connectedness[i] = (connectedness.length - 1) / connectedness[i];
		}

		return connectedness;
	}

	public double[] getVertexInwardConnectedness() {
		return getVertexConnectedness(false);
	}

	public double[] getVertexOutwardConnectedness() {
		return getVertexConnectedness(true);
	}

	/**
	 * Calculate the in or out degree of each actor in the network.
	 * 
	 * @param outDegree
	 *            whether to compute in degree or out degree.
	 * @return the degree of each actor
	 */
	public int[] getVertexDegrees(boolean outDegree) {
		int[] degrees = new int[presentEdges.length];
		for (int i = 0; i < presentEdges.length; i++) {
			for (int j = 0; j < presentEdges.length; j++) {
				if (i != j) {
					if (outDegree) {
						degrees[i] += presentEdges[i][j];
					} else {
						degrees[i] += presentEdges[j][i];
					}
				}
			}
		}
		return degrees;
	}

	public int[] getVertexInDegrees() {
		return getVertexDegrees(false);
	}

	public int[] getVertexOutDegrees() {
		return getVertexDegrees(true);
	}

	public int[][] getPresentEdges() {
		return presentEdges;
	}

	public int[][] getAbsentEdges() {
		return absentEdges;
	}

	public String toString() {
		return Arrays.toString(presentEdges);
	}

	/**
	 * Print network and network statistics to file.
	 * 
	 * @param outputDir
	 *            location to write files to
	 * @param summary
	 *            whether to print network statistics as well as the network
	 *            itself
	 */
	public void printNetwork(String outputDir, boolean summary) {
		try {

			PrintWriter pw = new PrintWriter(outputDir + "/network.txt");
			pw.print(this);
			pw.close();

			if (summary) {

				pw = new PrintWriter(outputDir + "/transitivity.txt");
				pw.print(getTransitivity());
				pw.close();

				Arrays.printToFile(getGeodesicDistances(), outputDir
						+ "/geodesic-distances.txt");
				Arrays.printToFile(getVertexInwardConnectedness(), outputDir
						+ "/inward-connectedness.txt");
				Arrays.printToFile(getVertexOutwardConnectedness(), outputDir
						+ "/outward-connectedness.txt");

				Arrays.printToFile(getVertexInDegrees(), outputDir
						+ "/in-degree.txt");
				Arrays.printToFile(getVertexOutDegrees(), outputDir
						+ "/out-degree.txt");
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
