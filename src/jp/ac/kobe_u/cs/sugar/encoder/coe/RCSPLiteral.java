package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.util.List;
import java.util.Set;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.ArithmeticLiteral;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;

public abstract class RCSPLiteral extends ArithmeticLiteral {
	public abstract List<Clause> toCCSP(CSP csp, COEEncoder encoder) throws SugarException;

	@Override
	public int[] getBound(IntegerVariable v) throws SugarException {
		throw new SugarException("Not supported.");
	}

	public abstract int getUpperBound();
	public abstract boolean isValid() throws SugarException;

	@Override
	public boolean isUnsatisfiable() throws SugarException {
		throw new SugarException("Not supported.");
	}
}