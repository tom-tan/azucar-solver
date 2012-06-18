package jp.ac.kobe_u.cs.sugar.csp;

import java.util.Set;
import java.util.TreeSet;

import jp.ac.kobe_u.cs.sugar.SugarException;

/**
 * NOT IMPLEMENTED YET.
 * This class implements a literal for arithmetic product.
 * v = v1*v2
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class ProductLiteral extends ArithmeticLiteral {
	private IntegerVariable v;
	private IntegerVariable v1;
	private IntegerVariable v2;

	public ProductLiteral(IntegerVariable v, IntegerVariable v1, IntegerVariable v2) {
		this.v = v;
		this.v1 = v1;
		this.v2 = v2;
	}

	public IntegerVariable getV() {
		return v;
	}

	public IntegerVariable getV1() {
		return v1;
	}

	public IntegerVariable getV2() {
		return v2;
	}

	@Override
	public Set<IntegerVariable> getVariables() {
		Set<IntegerVariable> set = new TreeSet<IntegerVariable>();
		set.add(v);
		set.add(v1);
		set.add(v2);
		return set;
	}

	@Override
	public long[] getBound(IntegerVariable v) throws SugarException {
		if (this.v == v) {
			final IntegerDomain muld = v1.getDomain().mul(v2.getDomain());
			final long lb = muld.getLowerBound();
			final long ub = muld.getUpperBound();
			return new long[] { lb, ub };
		}
		final IntegerDomain lhsd = this.v.getDomain();
		IntegerDomain rhsd = null;
		if (this.v1 == v) {
			rhsd = v2.getDomain();
		} else {
			assert this.v2 == v;
			rhsd = v1.getDomain();
		}
		if (rhsd.contains(0) || rhsd.isEmpty())
			return null;

		final IntegerDomain dom = lhsd.div(rhsd);
		return new long[] { dom.getLowerBound(), dom.getUpperBound() };
	}

	@Override
	public boolean isValid() throws SugarException {
		final IntegerDomain d = v.getDomain();
		final IntegerDomain muld = v1.getDomain().mul(v2.getDomain());
		return d.size() == 1 && muld.size() == 1 &&
			d.getLowerBound() == muld.getLowerBound();
	}

	@Override
	public boolean isUnsatisfiable() throws SugarException {
		final IntegerDomain d = v.getDomain();
		final IntegerDomain muld = v1.getDomain().mul(v2.getDomain());
		return d.cap(muld).isEmpty();
	}

	@Override
	public String toString() {
		String s = "(product " + v.getName() + " " + v1.getName() + " " + v2.getName() + ")";
		return s;
	}
}
