package jp.ac.kobe_u.cs.sugar.csp;

import jp.ac.kobe_u.cs.sugar.SugarException;

/**
 * This is an abstract class for literals of CSP.
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public interface Literal {
	public boolean isValid() throws SugarException;

	public boolean isUnsatisfiable() throws SugarException;
}
