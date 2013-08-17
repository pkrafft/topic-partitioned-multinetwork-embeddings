package mixedmembership;

/**
 * An interface for edge scores that use marginalized components
 */
public interface Resettable extends EdgeScore {

	public void resetCounts();

}
