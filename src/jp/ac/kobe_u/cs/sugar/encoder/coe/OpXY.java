package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
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
	public List<Clause> toCCSP(CSP csp) throws SugarException {
		assert op != Operator.GE;
		int b = csp.getBases().get(0);
		List<Clause> ret = new ArrayList<Clause>();
		int m = Math.max(x.nDigits(b), y.nDigits(b));
		switch(op) {
		case LE:
			if (x.isConstant() || y.isConstant()) {
			} else {
				// 中間変数導入する必要あり
			}
			return ret;
		case EQ:
			for (int i=0; i<m; i++) {
				ret.add(new Clause(x.nth(i).le(y.nth(i))));
				ret.add(new Clause(x.nth(i).ge(y.nth(i))));
			}
			return ret;
		case NE:
			return ret;
		default:
			throw new SugarException("Internal Error");
		}
	}

	@Override
	public String toString() {
		return "(" + op + " " + x + " "+ y + ")";
	}
}