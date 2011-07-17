package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.util.Set;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;

/**
 * z = x*y
 */
public class EqMul extends RCSPLiteral {
	private IntegerHolder z, x, y;

	public EqMul(IntegerHolder z, IntegerHolder x, IntegerHolder y) {
		this.z = z;
		this.x = x;
		this.y = y;
	}

	public EqMul(IntegerVariable z, IntegerVariable x, IntegerVariable y) {
		this.z = new IntegerHolder(z);
		this.x = new IntegerHolder(x);
		this.y = new IntegerHolder(y);
	}

	public EqMul(IntegerVariable z, int x, IntegerVariable y) {
		this.z = new IntegerHolder(z);
		this.x = new IntegerHolder(x);
		this.y = new IntegerHolder(y);
	}

	@Override
	public List<Clause> toCCSP(CSP csp) {
		return null;
	}

	@Override
	public String toString() {
		return "(eqmul " + z + " " + x + " "+ y + ")";
	}
}