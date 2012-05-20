package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.math.BigInteger;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.LinearSum;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.LinearLiteral;
import jp.ac.kobe_u.cs.sugar.csp.Operator;

public class LLExpression {
	private LinearSum linearSum;

	public LLExpression(IntegerVariable v) {
		linearSum = new LinearSum(v);
	}

	public LLExpression(BigInteger v) {
		linearSum = new LinearSum(v);
	}

	public LLExpression(LinearSum e) {
		linearSum = e;
	}

	public IntegerDomain getDomain() throws SugarException {
		return linearSum.getDomain();
	}

	public LinearLiteral le(LLExpression rhs) {
		LinearSum l = new LinearSum(linearSum);
		l.subtract(rhs.linearSum);
		return new LinearLiteral(l, Operator.LE);
	}

	public LinearLiteral le(BigInteger e) {
		LinearSum l = new LinearSum(linearSum);
		l.setB(l.getB().subtract(e));
		return new LinearLiteral(l, Operator.LE);
	}

	public LinearLiteral ge(LLExpression rhs) {
		return rhs.le(this);
	}

	public LinearLiteral ge(BigInteger e) {
		LinearSum l = new LinearSum(linearSum);
		l.setB(l.getB().subtract(e));
		l.multiply(BigInteger.ONE.negate());
		return new LinearLiteral(l, Operator.LE);
	}

	public LLExpression add(BigInteger e) {
		LinearSum l = new LinearSum(linearSum);
		l.setB(l.getB().add(e));
		return new LLExpression(l);
	}

	public LLExpression add(LLExpression e) {
		LinearSum l = new LinearSum(linearSum);
		l.add(e.linearSum);
		return new LLExpression(l);
	}

	public LLExpression sub(BigInteger e) {
		return add(e.negate());
	}

	public LLExpression mul(BigInteger c) {
		LinearSum l = new LinearSum(linearSum);
		l.multiply(c);
		return new LLExpression(l);
	}

	@Override
	public String toString() {
		return linearSum.toString();
	}
}
