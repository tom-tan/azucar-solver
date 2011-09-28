package jp.ac.kobe_u.cs.sugar.csp;

import java.util.Set;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.expression.Expression;
import jp.ac.kobe_u.cs.sugar.expression.Atom;

/**
 * This class implements a comparison literal of CSP.
 * The comparison represents the condition "linearSum &lt;= 0".
 * @see CSP
 * @see LinearSum
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class LinearLiteral extends ArithmeticLiteral {
	private LinearSum linearSum;
	private Operator op;

	/**
	 * Constructs a new comparison literal of given linear expression.
	 * @param linearSum the linear expression
	 */
	public LinearLiteral(LinearSum linearSum, Atom op) {
		assert op.equals(Expression.LE)
			|| op.equals(Expression.EQ)
			|| op.equals(Expression.NE);
		int factor = linearSum.factor();
		if (factor > 1) {
			linearSum.divide(factor);
		}
		this.linearSum = linearSum;
		if (op.equals(Expression.LE))      this.op = Operator.LE;
		else if (op.equals(Expression.EQ)) this.op = Operator.EQ;
		else                               this.op = Operator.NE;
	}

	public LinearLiteral(LinearSum linearSum, Operator op) {
		int factor = linearSum.factor();
		if (factor > 1) {
			linearSum.divide(factor);
		}
		this.linearSum = linearSum;
		this.op = op;
	}

	@Override
	public Set<IntegerVariable> getVariables() {
		return linearSum.getVariables();
	}

	/*
	 * Returns (int)Math.ceil((double)b/a)
	 */
	static private int divceil(int b, int a) {
		if ((a >= 0 && b >= 0) ||
				(a < 0  && b <  0)) {
			return b/a;
		} else if (a < 0) {
			return (-b+a+1)/-a;
		} else {
			return (b-a+1)/a;
		}
	}

	/*
	 * Returns (int)Math.floor((double)b/a)
	 */
	static private int divfloor(int b, int a) {
		if (a >= 0 && b >= 0) {
			return (-b+a+1)/-a;
		} else if (a < 0 && b < 0) {
			return (b-a+1)/a;
		} else {
			return b/a;
		}
	}

	@Override
	public int[] getBound(IntegerVariable v) throws SugarException {
		final int a = linearSum.getA(v);
		final IntegerDomain d = linearSum.getDomainExcept(v);
		final int olb = d.getLowerBound();
		final int oub = d.getUpperBound();
		int lb = 0;
		int ub = 0;
		if (a == 0) {
			// nop
		} else if (a > 0) {
			lb = divceil(-oub, a);
			ub = divfloor(-olb, a);
		} else {
			lb = divceil(-olb, a);
			ub = divfloor(-oub, a);
		}
		switch(op) {
		case LE:
			if (a == 0) {
				lb = v.getDomain().getLowerBound();
				ub = v.getDomain().getUpperBound();
			} else if (a > 0) {
				lb = v.getDomain().getLowerBound();
			} else if (a < 0) {
				ub = v.getDomain().getUpperBound();
			}
			break;
		case EQ: break;
		case NE: return null;
		}
		if (lb > ub)
			return null;
		return new int[] { lb, ub };
	}

	/**
	 * Returns the linear expression of the comparison literal.
	 * @return the linear expression
	 */
	public LinearSum getLinearExpression() {
		return linearSum;
	}

	public Operator getOperator() {
		return op;
	}

	@Override
	public boolean isValid() throws SugarException {
		switch(op) {
		case LE:
			return linearSum.getDomain().getUpperBound() <= 0;
		case EQ:
			return linearSum.getDomain().contains(0)
			  && linearSum.getDomain().size() == 1;
		case NE:
			return !linearSum.getDomain().contains(0);
		default:
			assert false;
			throw new SugarException("This code is never called.");
		}
	}

	@Override
	public boolean isUnsatisfiable() throws SugarException {
		switch(op) {
		case LE:
			return linearSum.getDomain().getLowerBound() > 0;
		case EQ:
			return !linearSum.getDomain().contains(0);
		case NE:
			return linearSum.getDomain().contains(0)
			  && linearSum.getDomain().size() == 1;
		default: throw new SugarException("!!!");
		}
	}

	/**
	 * Returns the string representation of the comparison literal.
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return "(" + op +" " + linearSum.toString() + " 0)";
	}
}
