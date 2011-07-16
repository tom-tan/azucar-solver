package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.util.Set;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.Operator;

/**
 * x op y (op in {=, <=, !=})
 */
public class OpXY extends RCSPLiteral {
	private IntegerHolder x, y;
	private Operator op;

	public OpXY(Operator op, IntegerHolder x, IntegerHolder y) {
		if (op == Operator.GE) {
			this.x = y;
			this.y = x;
			this.op = Operator.LE;
		} else {
			this.x = x;
			this.y = y;
			this.op = op;
		}
	}

	public OpXY(Operator op, IntegerVariable x, IntegerVariable y) {
		this(op, new IntegerHolder(x), new IntegerHolder(y));
	}

	public OpXY(Operator op, IntegerVariable x, int y) {
		this(op, new IntegerHolder(x), new IntegerHolder(y));
	}

	@Override
	public List<Clause> toCCSP() {
		assert op != Operator.GE;
		return null;
	}

	@Override
	public String toString() {
		return "(" + op + " " + x + " "+ y + ")";
	}
}