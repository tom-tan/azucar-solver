package jp.ac.kobe_u.cs.sugar.csp;

import java.util.Set;

import jp.ac.kobe_u.cs.sugar.SugarException;

public abstract class ArithmeticLiteral extends Literal {
	public abstract Set<IntegerVariable> getVariables();
	public abstract int[] getBound(IntegerVariable v) throws SugarException;
}