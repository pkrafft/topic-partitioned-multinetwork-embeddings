package util;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Methods for dealing with arrays
 */
public class Arrays {


	/**
	 * Print array to CSV.
	 * 
	 * @param array array to print
	 * @param fileName location for file
	 * @throws FileNotFoundException
	 */
	public static void printToFile(int[] array, String fileName) throws FileNotFoundException {
		
		String string = Integer.toString(array[0]);
		for (int i = 1; i < array.length; i++) {
			string += "," + array[i];
		}
		
		PrintWriter pw = new PrintWriter(fileName);
		pw.print(string);
		pw.close();
	}
	
	/**
	 * Print array to CSV
	 * 
	 * @param array array to print
	 * @param fileName location for file
	 * @throws FileNotFoundException
	 */
	public static void printToFile(double[] array, String fileName) throws FileNotFoundException {
		
		String string = Double.toString(array[0]);
		for (int i = 1; i < array.length; i++) {
			string += "," + array[i];
		}
		
		PrintWriter pw = new PrintWriter(fileName);
		pw.print(string);
		pw.close();
	}
	
	/**
	 * Print array to CSV
	 * 
	 * @param array array to print
	 * @param fileName location for file
	 * @throws FileNotFoundException
	 */
	public static void printToFile(double[][] array, String fileName) throws FileNotFoundException {
		
		PrintWriter pw = new PrintWriter(fileName);
		pw.print(toString(array));
		pw.close();
	}
	
	public static String toString(double[][] array) {
		String s = "";
		int i;
		for (i = 0; i < array.length - 1; i++) {
			int j;
			for (j = 0; j < array[i].length - 1; j++) {
				s += array[i][j] + ",";
			}
			s += array[i][j] + "\n";
		}
		int j;
		for (j = 0; j < array[i].length - 1; j++) {
			s += array[i][j] + ",";
		}
		s += array[i][j];
		return s;
	}
	
	public static String toString(int[][] array) {
		String s = "";
		int i;
		for (i = 0; i < array.length - 1; i++) {
			int j;
			for (j = 0; j < array[i].length - 1; j++) {
				s += array[i][j] + ",";
			}
			s += array[i][j] + "\n";
		}
		int j;
		for (j = 0; j < array[i].length - 1; j++) {
			s += array[i][j] + ",";
		}
		s += array[i][j];
		return s;
	}
	
}
