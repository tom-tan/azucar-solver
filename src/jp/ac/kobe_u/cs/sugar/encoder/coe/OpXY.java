package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.Operator;

/**
 * x op y (op in {=, <=, !=})
 */
public class OpXY extends RCSPLiteral {
	private final IntegerHolder x, y;
	private final Operator op;
	public static int nLe;
	public static int nEq;
	public static int nNe;

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
		switch(op) {
		case EQ: nEq++; break;
		case NE: nNe++; break;
		case LE: nLe++; break;
		}
	}

	public OpXY(Operator op, IntegerVariable x, IntegerVariable y) {
		this(op, new IntegerHolder(x), new IntegerHolder(y));
	}

	public OpXY(Operator op, IntegerVariable x, int y) {
		this(op, new IntegerHolder(x), new IntegerHolder(y));
	}

	@Override
	public Set<IntegerVariable> getVariables() {
		final Set<IntegerVariable> set = new TreeSet<IntegerVariable>();
		if (x.isVariable())
			set.add(x.getVariable());
		if (y.isVariable())
			set.add(y.getVariable());
		return set;
	}

	public Operator getOperator() {
		return op;
	}

	@Override
	public List<Clause> toCCSP(CSP csp, COEEncoder encoder) throws SugarException {
		assert op != Operator.GE;
		final List<Clause> ret = new ArrayList<Clause>();
		final int b = csp.getBases()[0];
		final int m = Math.max(x.nDigits(b), y.nDigits(b));

		switch(op) {
		case LE:
			if (x.isConstant() || y.isConstant()) {
				for (int i=0; i<m; i++) {
					final Clause cls = new Clause(x.nth(i).le(y.nth(i)));
					for (int j=i+1; j<m; j++) {
						cls.add(x.nth(j).le(y.nth(j).sub(1)));
					}
					ret.add(cls);
				}
			} else {
				final BooleanVariable[] s = new BooleanVariable[m];
				for (int i=1; i<m; i++) {
					s[i] = new BooleanVariable();
					csp.add(s[i]);
				}
				// -s(i+1) or x(i) <= y(i) (when 0 <= i < m-1)
				for (int i=0; i<m-1; i++) {
					final Clause cls = new Clause(new BooleanLiteral(s[i+1], true));
					cls.add(x.nth(i).le(y.nth(i)));
					ret.add(cls);
				}
				// x(i) <= y(i) (when i == m-1)
				ret.add(new Clause(x.nth(m-1).le(y.nth(m-1))));

				// -s(i+1) or (x(i) <= y(i)-1) or s(i) (when 1 <= i < m-1)
				for (int i=1; i<m-1; i++) {
					final Clause cls = new Clause(new BooleanLiteral(s[i+1], true));
					cls.add(x.nth(i).le(y.nth(i).sub(1)));
					cls.add(new BooleanLiteral(s[i], false));
					ret.add(cls);
				}
				if (m > 1) {
					// (x(i) <= y(i)-1) or s(i) (when i == m-1)
					final Clause cls0 = new Clause(x.nth(m-1).le(y.nth(m-1).sub(1)));
					cls0.add(new BooleanLiteral(s[m-1], false));
					ret.add(cls0);
				}
			}
			break;

		case EQ:
			for (int i=0; i<m; i++) {
				ret.add(new Clause(x.nth(i).le(y.nth(i))));
				ret.add(new Clause(x.nth(i).ge(y.nth(i))));
			}
			break;

		case NE:{
			final Clause cls = new Clause();
			for (int i=0; i<m; i++) {
				cls.add(x.nth(i).le(y.nth(i).sub(1)));
				cls.add(x.nth(i).sub(1).ge(y.nth(i)));
			}
			ret.addAll(encoder.simplify(cls));
			break;
		}
		default:
			throw new SugarException("Internal Error");
		}

		if (!ret.isEmpty()) {
			ret.get(0).setComment(toString());
		}
		return ret;
	}

	@Override
	public boolean isValid() throws SugarException {
		final IntegerDomain xd = x.getDomain();
		final IntegerDomain yd = y.getDomain();
		switch(op) {
		case EQ:
			return xd.size() == 1 && yd.size() == 1 && xd.getUpperBound() == yd.getUpperBound();
		case LE:
			return xd.getUpperBound() <= yd.getLowerBound();
		case NE:
			return xd.cap(yd).isEmpty();
		}
		assert false;
		throw new SugarException("This should not be called.");
	}

	@Override
	public String toString() {
		return "(" + op + " " + x + " "+ y + ")";
	}
}
