package util;

/**
 * A class that implements slice sampling. The code is complex enough that it is
 * nicer to have an interface and a static utility method for doing the sampling
 * than to have to re-implement it for every model component.
 */
public class SliceSampler {

	SliceSamplable model;
	LogRandoms rng;

	// the bounds that describe the current horizontal slice
	// these are fields so that testing is easier
	double[] leftBound;
	double[] rightBound;

	/**
	 * 
	 * @param model
	 *            the distribution whose parameters are to be slice sampled
	 */
	public SliceSampler(SliceSamplable model, LogRandoms rng) {
		this.model = model;
		this.rng = rng;
	}

	/**
	 * Sample a set of model parameters using slice sampling
	 * 
	 * @param rng
	 *            a random number generator
	 * @param numIterations
	 *            the number of iterations to sample
	 * @param stepSize
	 *            the slice sampling step size
	 */
	public void sampleParameters(int numIterations, double stepSize) {

		double[] parameters = model.getSliceSamplableParameters();
		leftBound = new double[parameters.length];
		rightBound = new double[parameters.length];
		
		int I = parameters.length;

		double[] rawParam = new double[I];
		double jacobianTerm = 0.0;

		for (int i = 0; i < I; i++) {
			rawParam[i] = Math.log(parameters[i]);
			jacobianTerm += rawParam[i];
		}

		for (int s = 0; s < numIterations; s++) {

			double lp = logProb(parameters, rawParam) + jacobianTerm;
			double lpNew = Math.log(rng.nextDouble()) + lp;

			for (int i = 0; i < I; i++) {
				leftBound[i] = rawParam[i] - rng.nextDouble() * stepSize;
				rightBound[i] = leftBound[i] + stepSize;
			}

			double[] rawParamNew = new double[I];
			double newJacobianTerm = 0.0;

			while (true) {

				newJacobianTerm = 0.0;

				for (int i = 0; i < I; i++) {
					rawParamNew[i] = leftBound[i] + rng.nextDouble()
							* (rightBound[i] - leftBound[i]);
					newJacobianTerm += rawParamNew[i];
				}

				if (logProb(parameters, rawParamNew) + newJacobianTerm >= lpNew)
					break;
				else
					for (int i = 0; i < I; i++)
						if (rawParamNew[i] < rawParam[i]) {
							leftBound[i] = rawParamNew[i];
						} else {
							rightBound[i] = rawParamNew[i];
						}
			}

			rawParam = rawParamNew;
			jacobianTerm = newJacobianTerm;
		}

		double[] newValues = new double[parameters.length];
		for (int i = 0; i < I; i++) {
			newValues[i] = Math.exp(rawParam[i]);
		}
		model.setSliceSamplableParameters(newValues);
	}

	/**
	 * Calculate the joint probability of a model and a data set using a
	 * particular value of the parameters being sampled
	 * 
	 * @param parameters
	 *            the current parameter values
	 * @param newLogValues
	 *            the log of the new parameter values
	 * @return the log probability
	 */
	private double logProb(double[] parameters, double[] newLogValues) {

		double[] oldValues = new double[parameters.length];
		System.arraycopy(parameters, 0, oldValues, 0, parameters.length);

		double[] newValues = new double[newLogValues.length];
		for (int i = 0; i < parameters.length; i++) {
			newValues[i] = Math.exp(newLogValues[i]);
		}
		model.setSliceSamplableParameters(newValues);

		double logProb = model.logProb();

		model.setSliceSamplableParameters(oldValues);

		return logProb;
	}

	public double[] getLeftBound() {
		return leftBound;
	}

	public double[] getRightBound() {
		return rightBound;
	}
}
