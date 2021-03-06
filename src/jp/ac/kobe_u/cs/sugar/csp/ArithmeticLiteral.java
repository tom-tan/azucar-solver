package jp.ac.kobe_u.cs.sugar.csp;

import java.util.Set;

import jp.ac.kobe_u.cs.sugar.SugarException;

public abstract class ArithmeticLiteral implements Literal {
	public abstract Set<IntegerVariable> getVariables();
	public abstract long[] getBound(IntegerVariable v) throws SugarException;
}