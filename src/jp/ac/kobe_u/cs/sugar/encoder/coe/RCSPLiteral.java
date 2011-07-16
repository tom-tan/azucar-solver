package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.util.List;
import java.util.Set;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.ArithmeticLiteral;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;

public abstract class RCSPLiteral extends ArithmeticLiteral {
	public abstract List<Clause> toCCSP();

	@Override
	public Set<IntegerVariable> getVariables() {
		assert false: "Not supported.";
		return null;
	}

	@Override
	public int[] getBound(IntegerVariable v) throws SugarException {
		throw new SugarException("Not supported.");
	}

	@Override
	public boolean isSimple() {
		assert false: "Not supported.";
		return false;
	}

	@Override
	public boolean isValid() throws SugarException {
		throw new SugarException("Not supported.");
	}

	@Override
	public boolean isUnsatisfiable() throws SugarException {
		throw new SugarException("Not supported.");
	}
}