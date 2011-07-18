package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
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
		List<Clause> ret = new ArrayList<Clause>();
		int b = csp.getBases()[0];
		int m = Math.max(x.nDigits(b), y.nDigits(b));

		switch(op) {
		case LE:
			if (x.isConstant() || y.isConstant()) {
				for (int i=0; i<m; i++) {
					if (x.nth(i).getDomain().getUpperBound()
							== y.nth(i).getDomain().getUpperBound()) {
						continue;
					}
					Clause cls = new Clause(x.nth(i).le(y.nth(i)));
					for (int j=i+1; j<m; j++) {
						cls.add(x.nth(j).le(y.nth(j).sub(1)));
					}
					ret.add(cls);
				}
			} else {
				BooleanVariable[] s = new BooleanVariable[m-1];
				for (int i=1; i<m; i++) {
					s[i] = new BooleanVariable();
					csp.add(s[i]);
				}
				// -s(i+1) or x(i) <= y(i) (when 0 <= i < m-1)
				for (int i=0; i<m-1; i++) {
					Clause cls = new Clause(new BooleanLiteral(s[i+1], true));
					cls.add(x.nth(i).le(y.nth(i)));
					ret.add(cls);
				}
				// x(i) <= y(i) (when i == m-1)
				ret.add(new Clause(x.nth(m-1).le(y.nth(m-1))));

				// -s(i+1) or (x(i) <= y(i)-1) or s(i) (when 1 <= i < m-1)
				for (int i=1; i<m-1; i++) {
					Clause cls = new Clause(new BooleanLiteral(s[i+1], true));
					cls.add(x.nth(i).le(y.nth(i).sub(1)));
					cls.add(new BooleanLiteral(s[i], false));
					ret.add(cls);
				}
				if (m > 1) {
					// (x(i) <= y(i)-1) or s(i) (when i == m-1)
					Clause cls0 = new Clause(x.nth(m-1).le(y.nth(m-1).sub(1)));
					cls0.add(new BooleanLiteral(s[m-1], false));
					ret.add(cls0);
				}
			}
			return ret;

		case EQ:
			for (int i=0; i<m; i++) {
				ret.add(new Clause(x.nth(i).le(y.nth(i))));
				ret.add(new Clause(x.nth(i).ge(y.nth(i))));
			}
			return ret;

		case NE:
			if (x.isConstant() || y.isConstant()) {
				Clause cls = new Clause();
				for (int i=0; i<m; i++) {
					cls.add(x.nth(i).le(y.nth(i).sub(1)));
					cls.add(x.nth(i).sub(1).le(y.nth(i)));
				}
				ret.add(cls);

			} else {
				BooleanVariable[] p = new BooleanVariable[m];
				BooleanVariable[] q = new BooleanVariable[m];
				Clause at_least_one = new Clause();
				for (int i=0; i<m; i++) {
					p[i] = new BooleanVariable();
					q[i] = new BooleanVariable();
					at_least_one.add(new BooleanLiteral(p[i], false));
					at_least_one.add(new BooleanLiteral(q[i], false));
				}
				ret.add(at_least_one);

				for (int i=0; i<m; i++) {
					Clause cls0 = new Clause(new BooleanLiteral(p[i], true));
					cls0.add(x.nth(i).le(y.nth(i).sub(1)));
					ret.add(cls0);

					Clause cls1 = new Clause(new BooleanLiteral(q[i+m], true));
					cls1.add(x.nth(i).sub(1).ge(y.nth(i)));
					ret.add(cls1);
				}
			}
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