package jp.ac.kobe_u.cs.sugar.csp;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import jp.ac.kobe_u.cs.sugar.SugarConstants;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.encoder.AbstractEncoder;
import jp.ac.kobe_u.cs.sugar.expression.*;

/**
 * This class implements a comparison literal of CSP.
 * The comparison represents the condition "linearSum &lt;= 0".
 * @see CSP
 * @see LinearSum
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class LinearLiteral extends Literal {
	private LinearSum linearSum;
	private Operator op;

	/**
	 * Constructs a new comparison literal of given linear expression.
	 * @param linearSum the linear expression
	 */
	public LinearLiteral(LinearSum linearSum, Atom op) {
		assert(op.equals(Expression.LE)
					 || op.equals(Expression.EQ)
					 || op.equals(Expression.NE));
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

	@Override
	public int[] getBound(IntegerVariable v) throws SugarException {
    switch(op) {
		case LE:{
      int a = linearSum.getA(v);
      int lb = v.getDomain().getLowerBound();
      int ub = v.getDomain().getUpperBound();
      if (a != 0) {
        IntegerDomain d = linearSum.getDomainExcept(v);
        d = d.neg();
        int b = d.getUpperBound();
        if (a >= 0) {
          // ub = (int)Math.floor((double)b / a);
          if (b >= 0) {
            ub = b/a;
          } else {
            ub = (b-a+1)/a;
          }
        } else {
          // lb = (int)Math.ceil((double)b / a);
          if (b >= 0) {
            lb = b/a;
          } else {
            lb = (b+a+1)/a;
          }
        }
      }
      if (lb > ub)
        return null;
      return new int[] { lb, ub };
    }
		case EQ: return null;
		case NE: return null;
		default: throw new SugarException("!!!");
    }
	}

	/**
	 * Returns true when the linear expression is simple.
	 * @return true when the linear expression is simple
	 * @see LinearSum#isSimple()
	 */
	public boolean isSimple() {
		return linearSum.isSimple();
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
      throw new SugarException("!!!");
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
