package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.Operator;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;

/**
 * z = x*y
 */
public class EqMul extends RCSPLiteral {
	private IntegerHolder z, x, y;

	public EqMul(IntegerHolder z, IntegerHolder x, IntegerHolder y) {
		this.z = z;
		if (y.isConstant()) {
			this.x = y;
			this.y = x;
		} else {
			this.x = x;
			this.y = y;
		}
		assert this.y.isVariable();
	}

	public EqMul(IntegerVariable z, IntegerVariable x, IntegerVariable y) {
		this(new IntegerHolder(z),
				 new IntegerHolder(x), new IntegerHolder(y));
	}

	public EqMul(IntegerVariable z, int x, IntegerVariable y) {
		this(new IntegerHolder(z),
				 new IntegerHolder(x), new IntegerHolder(y));
	}

	public EqMul(IntegerVariable z, int x, IntegerHolder y) {
		this(new IntegerHolder(z), new IntegerHolder(x), y);
	}

	@Override
	public List<Clause> toCCSP(CSP csp) throws SugarException {
		int b = csp.getBases().get(0);
		List<Clause> ret = new ArrayList<Clause>();
		int m = Math.max(Math.max(x.nDigits(b), y.nDigits(b)),
										 z.nDigits(b));

		if (x.isConstant() && x.getValue() < b) {
			IntegerHolder[] v = new IntegerHolder[m];
			int a = x.getValue();
			for (int i=0; i<m; i++) {
				IntegerDomain d = new IntegerDomain(0, a*y.nth(i).getDomain().getUpperBound());
				IntegerVariable vi = new IntegerVariable(d);
				csp.add(vi);
				v[i] = new IntegerHolder(vi);
			}

			for (int i=0; i<m; i++) {
				ret.add(new Clause(v[i].nth(1).mul(b).add(v[i].nth(0)).le(y.nth(i).mul(a))));
				ret.add(new Clause(v[i].nth(1).mul(b).add(v[i].nth(0)).ge(y.nth(i).mul(a))));
			}

			IntegerVariable[] c = new IntegerVariable[m];
			IntegerDomain d = new IntegerDomain(0, 1);
			for (int i=2; i<m; i++) {
				c[i] = new IntegerVariable(d);
			}

			for (int i=0; i<m; i++) {
				LLExpression lhs;
				if (i == 0 || i == m-1) {
					lhs = z.nth(i);
				} else {
					lhs = z.nth(i).add(lle(c[i+1]).mul(b));
				}

				LLExpression rhs;
				if (i == 0) {
					rhs = v[i].nth(0);
				} else if (i == 1) {
					rhs = v[i].nth(0).add(v[i-1].nth(1));
				} else if (i == m-1) {
					rhs = v[i].nth(1).add(lle(c[i]));
				} else {
					rhs = v[i].nth(0).add(v[i-1].nth(1)).add(lle(c[i]));
				}

				Clause cls0 = new Clause(lhs.le(rhs));
				ret.add(cls0);

				Clause cls1 = new Clause(lhs.ge(rhs));
				ret.add(cls1);
			}
		} else {
			IntegerVariable[] w = new IntegerVariable[m];
			int uby = y.getDomain().getUpperBound();
			for (int i=0, ubz = z.getDomain().getUpperBound();
					 i<m; i++, ubz /= b) {
				IntegerDomain d;
				if (x.isConstant()) {
					d = new IntegerDomain(0, Math.min(x.nthValue(i)*uby, ubz));
				} else {
					d = new IntegerDomain(0, Math.min((b-1)*uby, ubz));
				}
				w[i] = new IntegerVariable(d);
				csp.add(w[i]);
			}

			if (x.isConstant()) {
				// x(i) と x(j) が同じ時には共有したほうがいい
				for (int i=0; i<m; i++) {
					ret.addAll((new EqMul(w[i], x.nthValue(i), y)).toCCSP(csp));
				}
			} else {
				IntegerVariable[] ya = new IntegerVariable[b];
				for (int a=0; a<b; a++) {
					ya[a] = new IntegerVariable(new IntegerDomain(0, a*uby));
					csp.add(ya[a]);
				}

				for (int i=0; i<m; i++) {
					for (int a=0; a<b; a++) {
						for (Clause c: (new OpXY(Operator.EQ, w[i], ya[a])).toCCSP(csp)) {
							c.add(x.nth(i).le(a-1));
							c.add(x.nth(i).ge(a+1));
							ret.add(c);
						}
					}
				}

				for (int a=0; a<b; a++) {
					ret.addAll((new EqMul(ya[a], a, y)).toCCSP(csp));
				}
			}
			// [z = Sum_(i=0)^(m-1)B^iw_i]
		}
		return ret;
	}

	private LLExpression lle(IntegerVariable v) {
		return new LLExpression(v);
	}

	private LLExpression lle(IntegerHolder v) {
		if (v.isConstant())
			return new LLExpression(v.getValue());
		return new LLExpression(v.getVariable());
	}

	@Override
	public String toString() {
		return "(eqmul " + z + " " + x + " "+ y + ")";
	}
}