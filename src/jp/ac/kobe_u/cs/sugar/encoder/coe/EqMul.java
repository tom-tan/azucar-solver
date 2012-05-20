package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.Operator;

/**
 * z = x*y
 * @author Tomoya Tanjo (tanjo@nii.ac.jp)
 */
public class EqMul extends RCSPLiteral {
	private final IntegerHolder z, x, y;
	public static int nOccur;


	public EqMul(IntegerHolder z, IntegerHolder x, IntegerHolder y) {
		this.z = z;
		if (y.isConstant()) {
			this.x = y;
			this.y = x;
		} else {
			this.x = x;
			this.y = y;
		}
		nOccur++;
		assert this.y.isVariable();
	}

	public EqMul(IntegerVariable z, IntegerVariable x, IntegerVariable y) {
		this(new IntegerHolder(z),
				 new IntegerHolder(x), new IntegerHolder(y));
	}

	public EqMul(BigInteger z, BigInteger x, IntegerVariable y) {
		this(new IntegerHolder(z),
				 new IntegerHolder(x), new IntegerHolder(y));
	}

	public EqMul(IntegerVariable z, BigInteger x, IntegerVariable y) {
		this(new IntegerHolder(z),
				 new IntegerHolder(x), new IntegerHolder(y));
	}

	public EqMul(IntegerVariable z, BigInteger x, IntegerHolder y) {
		this(new IntegerHolder(z), new IntegerHolder(x), y);
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
		final int m = Math.max(Math.max(x.nDigits(b), y.nDigits(b)),
													 z.nDigits(b));
		final List<Clause> ret = new ArrayList<Clause>();

		if (x.isConstant() && x.getValue().compareTo(new BigInteger(Integer.toString(b))) < 0) {
			if (x.getValue().compareTo(BigInteger.ZERO) == 0) {
				assert z.isVariable();
				return (new OpXY(Operator.LE, z.getVariable(), BigInteger.ZERO)).toCCSP(csp, encoder);
			} else if (x.getValue().compareTo(BigInteger.ONE) == 0) {
				return (new OpXY(Operator.EQ, z, y)).toCCSP(csp, encoder);
			}
			final IntegerHolder[] v = new IntegerHolder[m];
			final BigInteger a = x.getValue();
			for (int i=0; i<m; i++) {
				final IntegerDomain d = new IntegerDomain(BigInteger.ZERO, a.multiply(y.nth(i).getDomain().getUpperBound()));
				final IntegerVariable vi = new IntegerVariable(d);
				csp.add(vi);
				final List<IntegerVariable> vdigits = vi.splitToDigits(csp);
				if (vdigits.size() > 1) {
					for (IntegerVariable digit: vdigits) {
						csp.add(digit);
					}
				}
				v[i] = new IntegerHolder(vi);
			}

			for (int i=0; i<m; i++) {
				ret.add(new Clause(v[i].nth(1).mul(new BigInteger(Integer.toString(b))).add(v[i].nth(0)).le(y.nth(i).mul(a))));
				ret.add(new Clause(v[i].nth(1).mul(new BigInteger(Integer.toString(b))).add(v[i].nth(0)).ge(y.nth(i).mul(a))));
			}

			final IntegerVariable[] c = new IntegerVariable[m];
			final IntegerDomain d = new IntegerDomain(BigInteger.ZERO, BigInteger.ONE);
			for (int i=2; i<m; i++) {
				c[i] = new IntegerVariable(d);
				csp.add(c[i]);
			}

			for (int i=0; i<m; i++) {
				LLExpression lhs;
				if (i == 0 || i == m-1) {
					lhs = z.nth(i);
				} else {
					lhs = z.nth(i).add(lle(c[i+1]).mul(new BigInteger(Integer.toString(b))));
				}

				LLExpression rhs;
				if (i == 0) {
					rhs = v[i].nth(0);
				} else if (i == 1) {
					rhs = v[i].nth(0).add(v[i-1].nth(1));
				} else {
					rhs = v[i].nth(0).add(v[i-1].nth(1)).add(lle(c[i]));
				}

				final Clause cls0 = new Clause(lhs.le(rhs));
				ret.add(cls0);

				final Clause cls1 = new Clause(lhs.ge(rhs));
				ret.add(cls1);
			}
		} else {
			// z = xy
			final IntegerVariable[] w = new IntegerVariable[m];
			final BigInteger uby = y.getDomain().getUpperBound();
      BigInteger ubz = z.getDomain().getUpperBound();
			for (int i=0; i<m; i++, ubz = ubz.divide(new BigInteger(Integer.toString(b)))) {
				IntegerDomain d;
				if (x.isConstant()) {
					d = new IntegerDomain(BigInteger.ZERO, (new BigInteger(Integer.toString(x.nthValue(i)))).multiply(uby).min(ubz));
				} else {
					d = new IntegerDomain(BigInteger.ZERO, (new BigInteger(Integer.toString(b-1))).multiply(uby).min(ubz));
				}
				w[i] = new IntegerVariable(d);
				csp.add(w[i]);
				final List<IntegerVariable> vdigits = w[i].splitToDigits(csp);
				if (vdigits.size() > 1) {
					for (IntegerVariable digit: vdigits) {
						csp.add(digit);
					}
				}
			}

			if (x.isConstant()) {
				for (int i=0; i<m; i++) {
					ret.addAll((new EqMul(w[i], new BigInteger(Integer.toString(x.nthValue(i))), y)).toCCSP(csp, encoder));
				}
			} else {
				final IntegerVariable[] ya = new IntegerVariable[b];
				for (int a=0; a<b; a++) {
					ya[a] = new IntegerVariable(new IntegerDomain(BigInteger.ZERO, (new BigInteger(Integer.toString(a))).multiply(uby)));
					final List<IntegerVariable> vdigits = ya[a].splitToDigits(csp);
					csp.add(ya[a]);
					if (vdigits.size() > 1) {
						for (IntegerVariable digit: vdigits) {
							csp.add(digit);
						}
					}
				}

				for (int i=0; i<m; i++) {
					for (int a=0; a<b; a++) {
						for (Clause c: (new OpXY(Operator.EQ, w[i], ya[a])).toCCSP(csp, encoder)) {
							c.add(x.nth(i).le(new BigInteger(Integer.toString(a-1))));
							c.add(x.nth(i).ge(new BigInteger(Integer.toString(a+1))));
							ret.add(c);
						}
					}
				}

				for (int a=0; a<b; a++) {
					ret.addAll((new EqMul(ya[a], new BigInteger(Integer.toString(a)), y)).toCCSP(csp, encoder));
				}
			}

			// [z = Sum_(i=0)^(m-1)B^iw_i]
			final IntegerHolder[] zi = new IntegerHolder[m];
			zi[m-1] = new IntegerHolder(w[m-1]);
			for (int i=m-2; i>0; i--) {
				final IntegerDomain d = new IntegerDomain(BigInteger.ZERO, (new BigInteger(Integer.toString(b))).multiply(zi[i+1].getDomain().getUpperBound()).add(w[i].getDomain().getUpperBound()));
				final IntegerVariable zii = new IntegerVariable(d);
				final List<IntegerVariable> vdigits = zii.splitToDigits(csp);
				csp.add(zii);
				if (vdigits.size() > 1) {
					for (IntegerVariable digit: vdigits) {
						csp.add(digit);
					}
				}
				zi[i] = new IntegerHolder(zii);
			}
			zi[0] = z;

			if (m == 1) {
				ret.add(new Clause(z.nth(0).le((new IntegerHolder(w[0])).nth(0))));
				ret.add(new Clause(z.nth(0).ge((new IntegerHolder(w[0])).nth(0))));
			} else {
				for (int i=0; i<m-1; i++) {
					ret.addAll(shiftAddtoCCSP(zi[i], zi[i+1], new IntegerHolder(w[i]), csp));
				}
			}
		}
		if (!ret.isEmpty()) {
			ret.get(0).setComment(toString());
		}
		return ret;
	}

	/**
	 * u = b*s+t
	 */
	private List<Clause> shiftAddtoCCSP(IntegerHolder u,
																			IntegerHolder s,
																			IntegerHolder t, CSP csp)
	throws SugarException {
		final int b = csp.getBases()[0];
		final int m = Math.max(s.nDigits(b), t.nDigits(b))+1;
		final List<Clause> ret = new ArrayList<Clause>();

		final IntegerVariable[] c = new IntegerVariable[m];
		final IntegerDomain d = new IntegerDomain(BigInteger.ZERO, BigInteger.ONE);
		for (int i=2; i<m; i++) {
			c[i] = new IntegerVariable(d);
			csp.add(c[i]);
		}

		for (int i=0; i<m; i++) {
			LLExpression lhs;
			if (i == 0 || i == m-1) {
				lhs = u.nth(i);
			} else {
				lhs = u.nth(i).add(lle(c[i+1]).mul(new BigInteger(Integer.toString(b))));
			}

			LLExpression rhs;
			if (i == 0) {
				rhs = t.nth(i);
			} else if (i == 1) {
				rhs = t.nth(i).add(s.nth(i-1));
			} else if (i == m-1) {
				rhs = s.nth(i-1).add(lle(c[i]));
			} else {
				rhs = t.nth(i).add(s.nth(i-1)).add(lle(c[i]));
			}

			ret.add(new Clause(lhs.le(rhs)));
			ret.add(new Clause(lhs.ge(rhs)));
		}
		return ret;
	}

	@Override
	public BigInteger getUpperBound() {
		return z.getUpperBound().max(x.getUpperBound()).max(y.getUpperBound());
	}

	@Override
	public boolean isValid() throws SugarException {
		final IntegerDomain zd = z.getDomain();
		final IntegerDomain xd = x.getDomain();
		final IntegerDomain yd = y.getDomain();
		if (zd.size().compareTo(BigInteger.ONE) != 0)
			return false;
		if (xd.size().compareTo(BigInteger.ONE) != 0 && yd.size().compareTo(BigInteger.ONE) != 0)
			return false;
		return zd.getUpperBound().compareTo(xd.getUpperBound().multiply(yd.getUpperBound())) == 0;
	}

	private LLExpression lle(IntegerVariable v) {
		return new LLExpression(v);
	}

	@Override
	public String toString() {
		return "(eqmul " + z + " " + x + " "+ y + ")";
	}
}
