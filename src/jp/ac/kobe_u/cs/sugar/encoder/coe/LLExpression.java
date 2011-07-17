package jp.ac.kobe_u.cs.sugar.encoder.coe;

import jp.ac.kobe_u.cs.sugar.csp.LinearSum;
import jp.ac.kobe_u.cs.sugar.csp.LinearLiteral;
import jp.ac.kobe_u.cs.sugar.csp.Operator;

public class LLExpression {
	private LinearSum linearSum;

	public LLExpression(IntegerVariable v) {
		linearSum = new linearSum(v);
	}

	public LLExpression(int v) {
		linearSum = new linearSum(v);
	}

	public LinearLiteral le(LLExpression rhs) {
		linearSum.subtract(rhs.linearSum);
		return new LinearLiteral(linearSum, Operator.LE);
	}

	public LinearLiteral ge(LLExpression rhs) {
		rhs.linearSum.subtract(linearSum);
		return new LinearLiteral(rhs.linearSum, Operator.LE);
	}

	public LLExpression add(int e) {
		linearSum.setB(linearSum.getB()+e);
		return this;
	}

	public LLExpression add(LLExpression e) {
		linearSum.add(e.linearSum);
		return this;
	}

	public LLExpression mul(int c) {
		linearSum.multiply(c);
		return this;
	}
}
