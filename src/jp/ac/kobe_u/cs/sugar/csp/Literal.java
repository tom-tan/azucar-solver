package jp.ac.kobe_u.cs.sugar.csp;

import java.util.Set;

import jp.ac.kobe_u.cs.sugar.SugarException;

/**
 * This is an abstract class for literals of CSP.
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public abstract class Literal {
	/**
	 * Returns true when the literal is simple.
	 * A literal is simple when it is a boolean literal or
	 * a comparison literal with at most one integer variable.
	 * @return true when the literal is simple
	 */
	public abstract boolean isSimple();

	public abstract boolean isValid() throws SugarException;

	public abstract boolean isUnsatisfiable() throws SugarException;
}
