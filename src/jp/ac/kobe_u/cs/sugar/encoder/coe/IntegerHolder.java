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
	private int[] digits;
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

	private void intToDigits(int b) {
		assert digits == null;
		assert isConstant_;
		int m = Math.ceil(Math.log(b)/Math.log(constant+1));
		int ub = constant;
		digits = new int[m];
		for (int i=0; i<m; i++, ub /= b) {
			digits[i] = ub%b;
		}
		assert digits[m-1] > 0;
	}

	public int nDigits(int b) {
		if (isConstant_) {
			if (digits == null)
				intToDigits(b);
			return digits.length;
		} else {
			return variable.getDigits().length;
		}
	}

	public LLExpression nth(int n) {
		assert digits != null;
		if (isConstant_) {
			return new LLExpression(digits.length < n ? digits[n] : 0);
		} else {
			if (variable.getDigits().length < n) {
				return new LLExpression(variable.getDigits()[n]);
			} else {
				return new LLExpression(0);
			}
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
