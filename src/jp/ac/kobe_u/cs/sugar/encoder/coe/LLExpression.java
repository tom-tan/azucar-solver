package jp.ac.kobe_u.cs.sugar.encoder.coe;

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

	public LLExpression(int v) {
		linearSum = new LinearSum(v);
	}

	public IntegerDomain getDomain() throws SugarException {
		return linearSum.getDomain();
	}

	public LinearLiteral le(LLExpression rhs) {
		linearSum.subtract(rhs.linearSum);
		return new LinearLiteral(linearSum, Operator.LE);
	}

	public LinearLiteral le(int e) {
		linearSum.setB(linearSum.getB()-e);
		return new LinearLiteral(linearSum, Operator.LE);
	}

	public LinearLiteral ge(LLExpression rhs) {
		return rhs.le(this);
	}

	public LinearLiteral ge(int e) {
		linearSum.setB(linearSum.getB()-e);
		linearSum.multiply(-1);
		return new LinearLiteral(linearSum, Operator.LE);
	}

	public LLExpression add(int e) {
		linearSum.setB(linearSum.getB()+e);
		return this;
	}

	public LLExpression add(LLExpression e) {
		linearSum.add(e.linearSum);
		return this;
	}

	public LLExpression sub(int e) {
		return add(-e);
	}

	public LLExpression mul(int c) {
		linearSum.multiply(c);
		return this;
	}
}
