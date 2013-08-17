package util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SliceSamplerTest {

	@Test
	public void test() {

		MockDistribution model = new MockDistribution();
		MockLogRandoms rng = new MockLogRandoms();
		SliceSampler sampler = new SliceSampler(model, rng);

		sampler.sampleParameters(2, 2.0);

		assertEquals(0.6, sampler.getLeftBound()[0], 1e-12);
		assertEquals(2.5, sampler.getRightBound()[0], 1e-12);
		assertEquals(Math.exp(1.5), model.getSliceSamplableParameters()[0],
				1e-12);
	}

	private class MockDistribution implements SliceSamplable {

		double[] x = new double[] { Math.exp(1) };

		@Override
		public double[] getSliceSamplableParameters() {
			return x;
		}

		@Override
		public void setSliceSamplableParameters(double[] newValues) {
			System.arraycopy(newValues, 0, x, 0, x.length);
		}

		@Override
		public double logProb() {
			if (x[0] <= 0) {
				return Math.log(0);
			} else if (x[0] <= Math.exp(1)) {
				return Math.log(0.8 / Math.exp(1));
			} else if (x[0] <= Math.exp(2)) {
				return Math.log(0.2 / (Math.exp(2) - Math.exp(1)));
			} else {
				return Math.log(0);
			}
		}
	}

	@SuppressWarnings("serial")
	private class MockLogRandoms extends LogRandoms {

		// following Algorithm 3.2 from Wallach's thesis
		// (with slight modifications since these samples must always be between
		// 0 and 1)
		double[] doubleSamples = new double[] {
				// s = 1
				0.4 * Math.exp(1) / 0.8 / Math.exp(1), // u'
				0.2 / 2.0, // r
				(1.5 - 0.8) / (2.8 - 0.8), // x'
				(0.9 - 0.8) / (1.5 - 0.8), // x'
				// s = 2
				0.1 * Math.exp(1) / 0.8 / Math.exp(0.9), // u'
				0.3 / 2.0, // r
				(2.5 - 0.6) / (2.6 - 0.6), // x'
				(1.5 - 0.6) / (2.5 - 0.6) // x'
		};
		int doubleIndex = 0;

		public double nextDouble() {
			return doubleSamples[doubleIndex++];
		}
	}
}
