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

	@Override
	public int getCode() throws SugarException {
		if (! isSimple()) {
			throw new SugarException("Internal error " + toString()); 
		}
		if (op == Operator.EQ || op == Operator.NE) {
			throw new SugarException("Internal error " + toString()); 
		}
		int b = linearSum.getB(); 
		int code;
		if (linearSum.size() == 0) {
			code = b <= 0 ? AbstractEncoder.TRUE_CODE : AbstractEncoder.FALSE_CODE; 
		} else {
			IntegerVariable v = linearSum.getCoef().firstKey();
			int a = linearSum.getA(v);
			code = v.getCodeLE(a, -b);
		}
		return code;
	}

	/*
	 * a1*v1+a2*v2+a3*v3+b <= 0
	 * <--> v1>=c1 -> a2*v2+a3*v3+b+a1*c1 <= 0 (when a1>0)
	 *      v1<=c1 -> a2*v2+a3*v3+b+a1*c1 <= 0 (when a1<0)
	 * <--> v1>=c1 -> v2>=c2 -> a3*v3+b+a1*c1+a2*c2<= 0 (when a1>0, a2>0)
	 * 
	 */
	private void encode(AbstractEncoder encoder, IntegerVariable[] vs, int i, int s, int[] clause)
		throws IOException, SugarException {
		if (op == Operator.EQ || op == Operator.NE) {
			throw new SugarException("Internal error " + toString()); 
		}
		if (i >= vs.length - 1) {
			int a = linearSum.getA(vs[i]);
			// encoder.writeComment(a + "*" + vs[i].getName() + " <= " + (-s));
			clause[i] = vs[i].getCodeLE(a, -s);
			encoder.writeClause(clause);
		} else {
			int lb0 = s;
			int ub0 = s;
			for (int j = i + 1; j < vs.length; j++) {
				int a = linearSum.getA(vs[j]); 
				if (a > 0) {
					lb0 += a * vs[j].getDomain().getLowerBound();
					ub0 += a * vs[j].getDomain().getUpperBound();
				} else {
					lb0 += a * vs[j].getDomain().getUpperBound();
					ub0 += a * vs[j].getDomain().getLowerBound();
				}
			}
			int a = linearSum.getA(vs[i]);
			IntegerDomain domain = vs[i].getDomain();
			int lb = domain.getLowerBound();
			int ub = domain.getUpperBound();
			if (a >= 0) {
				// ub = Math.min(ub, (int)Math.floor(-(double)lb0 / a));
				if (-lb0 >= 0) {
					ub = Math.min(ub, -lb0/a);
				} else {
					ub = Math.min(ub, (-lb0-a+1)/a);
				}
				// XXX
				Iterator<Integer> iter = domain.values(lb, ub); 
				while (iter.hasNext()) {
					int c = iter.next();
					// vs[i]>=c -> ...
					// encoder.writeComment(vs[i].getName() + " <= " + (c-1));
					clause[i] = vs[i].getCodeLE(c - 1);
					encode(encoder, vs, i+1, s+a*c, clause);
				}
				clause[i] = vs[i].getCodeLE(ub);
				encode(encoder, vs, i+1, s+a*(ub+1), clause);
			} else {
				// lb = Math.max(lb, (int)Math.ceil(-(double)lb0/a));
				if (-lb0 >= 0) {
					lb = Math.max(lb, -lb0/a);
				} else {
					lb = Math.max(lb, (-lb0+a+1)/a);
				}
				// XXX
				clause[i] = AbstractEncoder.negateCode(vs[i].getCodeLE(lb - 1));
				encode(encoder, vs, i+1, s+a*(lb-1), clause);
				Iterator<Integer> iter = domain.values(lb, ub); 
				while (iter.hasNext()) {
					int c = iter.next();
					// vs[i]<=c -> ...
					clause[i] = AbstractEncoder.negateCode(vs[i].getCodeLE(c));
					encode(encoder, vs, i+1, s+a*c, clause);
				}
			}
		}
	}
	
	@Override
	public void encode(AbstractEncoder encoder, int[] clause) throws SugarException, IOException {
		if (op == Operator.EQ || op == Operator.NE) {
			throw new SugarException("Internal error " + toString()); 
		}
		if (isValid()) {
		} if (isSimple()) {
			clause = expand(clause, 1);
			clause[0] = getCode();
			encoder.writeClause(clause);
		} else {
			IntegerVariable[] vs = linearSum.getVariablesSorted();
			int n = linearSum.size();
			clause = expand(clause, n);
			encode(encoder, vs, 0, linearSum.getB(), clause);
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
