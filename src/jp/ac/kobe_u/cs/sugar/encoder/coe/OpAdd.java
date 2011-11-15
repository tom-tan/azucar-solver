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
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.Operator;

/**
 * z op x+y (op in {=, <=, >=, !=})
 */
public class OpAdd extends RCSPLiteral {
	private final  IntegerHolder z, x, y;
	private final Operator op;
	public static int nLe;
	public static int nGe;
	public static int nEq;
	public static int nNe;

	public OpAdd(Operator op, IntegerHolder z, IntegerHolder x, IntegerHolder y) {
		this.z = z;
		this.x = x;
		this.y = y;
		this.op = op;

		switch(op) {
		case EQ: nEq++; break;
		case NE: nNe++; break;
		case LE: nLe++; break;
		case GE: nGe++; break;
		}
	}

	public OpAdd(Operator op, IntegerVariable z, int x, IntegerVariable y) {
		this(op, new IntegerHolder(z), new IntegerHolder(x),
				 new IntegerHolder(y));
	}

	public OpAdd(Operator op, IntegerVariable z,
							 IntegerHolder x, IntegerHolder y) {
		this(op, new IntegerHolder(z), x, y);
	}

	@Override
	public Set<IntegerVariable> getVariables() {
		final Set<IntegerVariable> set = new TreeSet<IntegerVariable>();
		if (x.isVariable())
			set.add(x.getVariable());
		if (y.isVariable())
			set.add(y.getVariable());
		if (z.isVariable())
			set.add(z.getVariable());
		return set;
	}

	@Override
	public List<Clause> toCCSP(CSP csp, COEEncoder encoder) throws SugarException {
		final int b = csp.getBases()[0];
		final List<Clause> ret = new ArrayList<Clause>();
		final int m = Math.max(Math.max(x.nDigits(b), y.nDigits(b)),
													 z.nDigits(b));

		final IntegerVariable[] c = new IntegerVariable[m];
		for (int i=1; i<m; i++) {
			c[i] = new IntegerVariable(new IntegerDomain(0, 1));
			c[i].setComment(i + "-th carry for " + toString());
			csp.add(c[i]);
		}

		final LLExpression[] lhs = new LLExpression[m];
		for (int i=0; i<m-1; i++) {
			lhs[i] = z.nth(i).add(lle(c[i+1]).mul(b));
		}
		lhs[m-1] = z.nth(m-1);

		final LLExpression[] rhs = new LLExpression[m];
		rhs[0] = x.nth(0).add(y.nth(0));
		for (int i=1; i<m; i++) {
			rhs[i] = x.nth(i).add(y.nth(i)).add(lle(c[i]));
		}

		switch(op) {
		case LE:{
			final BooleanVariable[] s = new BooleanVariable[m];
			for (int i=1; i<m; i++) {
				s[i] = new BooleanVariable();
				csp.add(s[i]);
			}

			// -s(i+1) or (z(i)+B*c(i+1) <= x(i)+y(i)+c(i)) (when 0 <= i < m-1)
			for (int i=0; i<m-1; i++) {
				final Clause cls = new Clause(new BooleanLiteral(s[i+1], true));
				cls.add(lhs[i].le(rhs[i]));
				ret.add(cls);
			}
			// z(i) <= x(i)+y(i)+c(i) (when i == m-1)
			ret.add(new Clause(lhs[m-1].le(rhs[m-1])));

			// -s(i+1) or (z(i)+B*c(i+1) <= x(i)+y(i)+c(i)-1) or s(i)
			// (when 1 <= i < m-1)
			for (int i=1; i<m-1; i++) {
				final Clause cls = new Clause(new BooleanLiteral(s[i+1], true));
				cls.add(lhs[i].le(rhs[i].sub(1)));
				cls.add(new BooleanLiteral(s[i], false));
				ret.add(cls);
			}
			// (z(i) <= x(i)+y(i)+c(i)-1) or s(i) (when i == m-1)
			if (m > 1) {
				final Clause cls = new Clause(lhs[m-1].le(rhs[m-1].sub(1)));
				cls.add(new BooleanLiteral(s[m-1], false));
				ret.add(cls);
			}

			for (int i=0; i<m-1; i++) {
				// c(i+1) <= 0 or x(i)+y(i)+c(i) >= B
				final Clause ltor = new Clause(lle(c[i+1]).le(0));
				ltor.add(rhs[i].ge(b));
				ret.add(ltor);

				// c(i+1) >= 1 or x(i)+y(i)+c(i) <= B-1
				final Clause rtol = new Clause(lle(c[i+1]).ge(1));
				rtol.add(rhs[i].le(b-1));
				ret.add(rtol);
			}
			break;
		}

		case GE:{
			final BooleanVariable[] s = new BooleanVariable[m];
			for (int i=1; i<m; i++) {
				s[i] = new BooleanVariable();
				csp.add(s[i]);
			}

			// -s(i+1) or (z(i)+B*c(i+1) >= x(i)+y(i)+c(i)) (when 0 <= i < m-1)
			for (int i=0; i<m-1; i++) {
				final Clause cls = new Clause(new BooleanLiteral(s[i+1], true));
				cls.add(lhs[i].ge(rhs[i]));
				ret.add(cls);
			}
			// z(i) >= x(i)+y(i)+c(i) (when i == m-1)
			ret.add(new Clause(lhs[m-1].ge(rhs[m-1])));

			// -s(i+1) or (z(i)+B*c(i+1)-1 >= x(i)+y(i)+c(i)) or s(i)
			// (when 1 <= i < m-1)
			for (int i=1; i<m-1; i++) {
				final Clause cls = new Clause(new BooleanLiteral(s[i+1], true));
				cls.add(lhs[i].sub(1).ge(rhs[i]));
				cls.add(new BooleanLiteral(s[i], false));
				ret.add(cls);
			}
			// (z(i)-1 >= x(i)+y(i)+c(i)) or s(i) (when i == m-1)
			if (m > 1) {
				final Clause cls = new Clause(lhs[m-1].sub(1).ge(rhs[m-1]));
				cls.add(new BooleanLiteral(s[m-1], false));
				ret.add(cls);
			}

			for (int i=0; i<m-1; i++) {
				// c(i+1) <= 0 or x(i)+y(i)+c(i) >= B
				final Clause ltor = new Clause(lle(c[i+1]).le(0));
				ltor.add(rhs[i].ge(b));
				ret.add(ltor);

				// c(i+1) >= 1 or x(i)+y(i)+c(i) <= B-1
				final Clause rtol = new Clause(lle(c[i+1]).ge(1));
				rtol.add(rhs[i].le(b-1));
				ret.add(rtol);
			}
			break;
		}

		case EQ:
			for (int i=0; i<m; i++) {
				ret.add(new Clause(lhs[i].le(rhs[i])));
				ret.add(new Clause(lhs[i].ge(rhs[i])));
			}
			break;

		case NE:{
			final Clause cls = new Clause();
			for (int i=0; i<m; i++) {
				cls.add(lhs[i].le(rhs[i].sub(1)));
				cls.add(lhs[i].sub(1).ge(rhs[i]));
			}
			ret.addAll(encoder.simplify(cls));

			for (int i=0; i<m-1; i++) {
				// c(i+1) <= 0 or x(i)+y(i)+c(i) >= B
				final Clause ltor = new Clause(lle(c[i+1]).le(0));
				ltor.add(rhs[i].ge(b));
				ret.add(ltor);

				// c(i+1) >= 1 or x(i)+y(i)+c(i) <= B-1
				final Clause rtol = new Clause(lle(c[i+1]).ge(1));
				rtol.add(rhs[i].le(b-1));
				ret.add(rtol);
			}
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

	public boolean isValid() throws SugarException {
		final IntegerDomain zd = z.getDomain();
		final IntegerDomain xd = x.getDomain();
		final IntegerDomain yd = y.getDomain();
		switch(op) {
		case EQ:
			return zd.size() == 1 && xd.size() == 1 && yd.size() == 1 &&
				zd.getUpperBound() == xd.getUpperBound() + yd.getUpperBound();
		case LE:
			return zd.getUpperBound() <= xd.getLowerBound() + yd.getLowerBound();
		case GE:
			return zd.getLowerBound() >= xd.getUpperBound() + yd.getUpperBound();
		case NE:
			return zd.cap(xd.add(yd)).isEmpty();
		}
		assert false;
		throw new SugarException("This should not be called.");
	}

	private LLExpression lle(IntegerVariable v) {
		return new LLExpression(v);
	}

	@Override
	public String toString() {
		return "("+op+ "add " + z + " " + x + " " + y + ")";
	}
}