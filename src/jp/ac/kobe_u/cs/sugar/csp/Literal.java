package jp.ac.kobe_u.cs.sugar.csp;

import java.util.Set;

import jp.ac.kobe_u.cs.sugar.SugarException;

/**
 * This is an abstract class for literals of CSP.
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public abstract class Literal {
	public abstract boolean isValid() throws SugarException;

	public abstract boolean isUnsatisfiable() throws SugarException;
}
