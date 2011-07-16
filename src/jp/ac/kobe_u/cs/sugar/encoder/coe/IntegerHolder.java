package jp.ac.kobe_u.cs.sugar.encoder.coe;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;

/**
 * Wrapper for IntegerVariable and integer constant.
 */
public class IntegerHolder implements Comparable<IntegerHolder>{
	private boolean isConstant_;
	private int constant;
	private IntegerVariable variable;

	public IntegerHolder(int v) {
		constant = v;
		isConstant_ = true;
	}

	public IntegerHolder(IntegerVariable v) {
		variable = v;
		isConstant_ = false;
	}

	public IntegerDomain getDomain() throws SugarException{
		if (isConstant_) {
			return new IntegerDomain(constant, constant);
		} else {
			return variable.getDomain();
		}
	}

	@Override
	public int compareTo(IntegerHolder v) {
		if (this == v)
			return 0;
		if (v == null)
			return 1;
		int ub1 = isConstant_ ? constant : variable.getDomain().getUpperBound();
		int ub2 = v.isConstant_ ? v.constant : variable.getDomain().getUpperBound();
		if (ub1 != ub2)
			return ub1 < ub2 ? -1 : 1;
		return this.toString().compareTo(v.toString());
	}

		@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final IntegerHolder other = (IntegerHolder) obj;
		if (other.isConstant_) {
			return isConstant_ && other.constant == constant;
		} else {
			return !isConstant_ && other.variable == variable;
		}
	}

	public boolean isConstant() {
		return isConstant_;
	}

	public boolean isVariable() {
		return !isConstant_;
	}

	public IntegerVariable getVariable() {
		assert !isConstant_;
		return variable;
	}

	public int getValue() {
		assert isConstant_;
		return constant;
	}

	public String toString() {
		return isConstant_ ? Integer.toString(constant) : variable.getName();
	}
}
