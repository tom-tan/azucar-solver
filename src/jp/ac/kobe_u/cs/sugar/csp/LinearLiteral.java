package jp.ac.kobe_u.cs.sugar.csp;

import java.util.Set;
import java.math.BigInteger;
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
		BigInteger factor = linearSum.factor();
		if (factor.compareTo(BigInteger.ONE) > 0) {
			linearSum.divide(factor);
		}
		this.linearSum = linearSum;
		if (op.equals(Expression.LE))      this.op = Operator.LE;
		else if (op.equals(Expression.EQ)) this.op = Operator.EQ;
		else                               this.op = Operator.NE;
	}

	public LinearLiteral(LinearSum linearSum, Operator op) {
		BigInteger factor = linearSum.factor();
		if (factor.compareTo(BigInteger.ONE) > 0) {
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
	 * Returns (long)Math.ceil((double)b/a)
	 */
	static private BigInteger divceil(BigInteger b, BigInteger a) {
		if ((a.compareTo(BigInteger.ZERO) >= 0 && b.compareTo(BigInteger.ZERO) >= 0) ||
				(a.compareTo(BigInteger.ZERO) < 0  && b.compareTo(BigInteger.ZERO) <  0)) {
			return b.divide(a);
		} else if (a.compareTo(BigInteger.ZERO) < 0) {
			return b.negate().add(a).add(BigInteger.ONE).divide(a.negate());
		} else {
			return b.subtract(a).add(BigInteger.ONE).divide(a);
		}
	}

	/*
	 * Returns (long)Math.floor((double)b/a)
	 */
	static private BigInteger divfloor(BigInteger b, BigInteger a) {
		if (a.compareTo(BigInteger.ZERO) >= 0 && b.compareTo(BigInteger.ZERO) >= 0) {
			return b.divide(a);
		} else if (a.compareTo(BigInteger.ZERO) < 0 && b.compareTo(BigInteger.ZERO) < 0) {
			return b.negate().divide(a.negate());
		} else if (a.compareTo(BigInteger.ZERO) >= 0 && b.compareTo(BigInteger.ZERO) < 0) {
			return b.subtract(a).add(BigInteger.ONE).divide(a);
		} else {
			return b.negate().add(a).add(BigInteger.ONE).divide(a.negate());
		}
	}

	@Override
	public BigInteger[] getBound(IntegerVariable v) throws SugarException {
		final BigInteger a = linearSum.getA(v);
		final IntegerDomain d = linearSum.getDomainExcept(v);
		final BigInteger olb = d.getLowerBound();
		final BigInteger oub = d.getUpperBound();
		BigInteger lb = v.getDomain().getLowerBound();
		BigInteger ub = v.getDomain().getUpperBound();
		switch(op) {
		case LE:
			if (a.compareTo(BigInteger.ZERO) > 0) {
				ub = divfloor(olb.negate(), a);
			} else if (a.compareTo(BigInteger.ZERO) < 0) {
				lb = divceil(olb.negate(), a);
			}
			break;
		case GE:
			throw new SugarException("This code is never called.");
		case EQ:
			if (a.compareTo(BigInteger.ZERO) > 0) {
				lb = divceil(oub.negate(), a);
				ub = divfloor(olb.negate(), a);
			} else if (a.compareTo(BigInteger.ZERO) < 0) {
				lb = divceil(olb.negate(), a);
				ub = divfloor(oub.negate(), a);
			}
			break;
		case NE: return null;
		}
		if (lb.compareTo(ub) > 0)
			return null;
		return new BigInteger[] { lb, ub };
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
		final IntegerDomain d = linearSum.getDomain();
		switch(op) {
		case LE:
			return d.getUpperBound().compareTo(BigInteger.ZERO) <= 0;
		case EQ:
			return d.contains(BigInteger.ZERO) && d.size().compareTo(BigInteger.ONE) == 0;
		case NE:
			return !d.contains(BigInteger.ZERO);
		default:
			assert false;
			throw new SugarException("This code is never called.");
		}
	}

	@Override
	public boolean isUnsatisfiable() throws SugarException {
		switch(op) {
		case LE:
			return linearSum.getDomain().getLowerBound().compareTo(BigInteger.ZERO) > 0;
		case EQ:
			return !linearSum.getDomain().contains(BigInteger.ZERO);
		case NE:
			return linearSum.getDomain().contains(BigInteger.ZERO)
			  && linearSum.getDomain().size().compareTo(BigInteger.ONE) == 0;
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
