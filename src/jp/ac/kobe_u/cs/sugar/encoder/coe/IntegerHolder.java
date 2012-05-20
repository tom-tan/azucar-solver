package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.math.BigInteger;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;

/**
 * Wrapper for IntegerVariable and integer constant.
 */
public class IntegerHolder implements Comparable<IntegerHolder>{
	private boolean isConstant_;
	private BigInteger constant;
	private int[] digits;
	private IntegerVariable variable;

	public IntegerHolder(BigInteger v) {
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

	public BigInteger getUpperBound() {
		if (isConstant_)
			return constant;
		else
			return variable.getDomain().getUpperBound();
	}

	private void intToDigits(int b) {
		assert digits == null;
		assert isConstant_;
		final int m = constant.toString(b).length();
		BigInteger ub = constant;
		digits = new int[m];
		for (int i=0; i<m; i++, ub = ub.divide(new BigInteger(Integer.toString(b)))) {
			digits[i] = ub.remainder(new BigInteger(Integer.toString(b))).intValue();
		}
		assert constant.compareTo(BigInteger.ZERO) == 0 || digits[m-1] > 0;
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
		if (isConstant_) {
			assert digits != null;
			return new LLExpression(new BigInteger(Integer.toString(digits.length > n ? digits[n] : 0)));
		} else {
			if (variable.getDigits().length > n) {
				return new LLExpression(variable.getDigits()[n]);
			} else {
				return new LLExpression(BigInteger.ZERO);
			}
		}
	}

	public int nthValue(int n) {
		assert isConstant_;
		return digits.length > n ? digits[n] : 0;
	}

	@Override
	public int compareTo(IntegerHolder v) {
		if (this == v)
			return 0;
		if (v == null)
			return 1;
		final BigInteger ub1 = isConstant_ ? constant : variable.getDomain().getUpperBound();
		final BigInteger ub2 = v.isConstant_ ? v.constant : v.variable.getDomain().getUpperBound();
		if (ub1.compareTo(ub2) != 0)
			return ub1.compareTo(ub2) < 0 ? -1 : 1;
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
			return isConstant_ && other.constant.compareTo(constant) == 0;
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

	public BigInteger getValue() {
		assert isConstant_;
		return constant;
	}

	public String toString() {
		return isConstant_ ? constant.toString() : variable.getName();
	}
}
