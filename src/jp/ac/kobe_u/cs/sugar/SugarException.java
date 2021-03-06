package jp.ac.kobe_u.cs.sugar;

/**
 * SugarException class.
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class SugarException extends Exception {

	private static final long serialVersionUID = 7879608175187039868L;

	/**
	 * Constructs a SugarException object.
	 * @param message
	 */
	public SugarException(String message) {
		super(message);
	}
}
