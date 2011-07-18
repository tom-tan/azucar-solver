package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.Operator;

/**
 * z op x+y (op in {=, <=, >=, !=})
 */
public class OpAdd extends RCSPLiteral {
	private IntegerHolder z, x, y;
	private Operator op;

	public OpAdd(Operator op, IntegerHolder z, IntegerHolder x, IntegerHolder y) {
		this.z = z;
		this.x = x;
		this.y = y;
		this.op = op;
	}

	public OpAdd(Operator op, IntegerVariable z, int x, IntegerVariable y) {
		this(op, new IntegerHolder(z), new IntegerHolder(x),
				 new IntegerHolder(y));
	}

	public OpAdd(Operator op, IntegerVariable z,
							 IntegerHolder x, IntegerHolder y) {
		this(op, new IntegerHolder(z), x, y);
	}

	@Override
	public List<Clause> toCCSP(CSP csp) throws SugarException {
		int b = csp.getBases().get(0);
		List<Clause> ret = new ArrayList<Clause>();
		int m = Math.max(Math.max(x.nDigits(b), y.nDigits(b)),
										 z.nDigits(b));

		IntegerVariable[] c = new IntegerVariable[m];
		for (int i=1; i<m; i++) {
			c[i] = new IntegerVariable(new IntegerDomain(0, 1));
			csp.add(c[i]);
		}

		switch(op) {
		case LE:{
			BooleanVariable[] s = new BooleanVariable[m-1];
			for (int i=1; i<m; i++) {
				s[i] = new BooleanVariable();
				csp.add(s[i]);
			}

			// -s(i+1) or (z(i)+B*c(i+1) <= x(i)+y(i)+c(i)) (when 0 <= i < m-1)
			for (int i=0; i<m-1; i++) {
				Clause cls = new Clause(new BooleanLiteral(s[i+1], true));
				LLExpression lhs = z.nth(i).add(lle(c[i+1]).mul(b));
				LLExpression rhs = (i == 0) ?
													 x.nth(i).add(y.nth(i)) :
													 x.nth(i).add(y.nth(i)).add(lle(c[i]));
				cls.add(lhs.le(rhs));
				ret.add(cls);
			}
			// z(i) <= x(i)+y(i)+c(i) (when i == m-1)
			{
				int i = m-1;
				LLExpression lhs = z.nth(i);
				LLExpression rhs = (i == 0) ?
													 x.nth(i).add(y.nth(i)) :
													 x.nth(i).add(y.nth(i)).add(lle(c[i]));
				Clause cls = new Clause(lhs.le(rhs));
				ret.add(cls);
			}

			// -s(i+1) or (z(i)+B*c(i+1) <= x(i)+y(i)+c(i)-1) or s(i)
			// (when 1 <= i < m-1)
			for (int i=1; i<m-1; i++) {
				Clause cls = new Clause(new BooleanLiteral(s[i+1], true));
				LLExpression lhs = z.nth(i).add(lle(c[i+1]).mul(b));
				LLExpression rhs = x.nth(i).add(y.nth(i)).add(lle(c[i])).sub(1);
				cls.add(lhs.le(rhs));
				cls.add(new BooleanLiteral(s[i], false));
				ret.add(cls);
			}
			// (z(i) <= x(i)+y(i)+c(i)-1) or s(i) (when i == m-1)
			{
				int i = m-1;
				LLExpression lhs = z.nth(i);
				LLExpression rhs = (i == 0) ?
													 x.nth(i).add(y.nth(i)).sub(1) :
													 x.nth(i).add(y.nth(i)).add(lle(c[i])).sub(1);
				Clause cls = new Clause(lhs.le(rhs));
				cls.add(new BooleanLiteral(s[i], false));
				ret.add(cls);
			}

			for (int i=0; i<m-1; i++) {
				LLExpression lhs = (i == 0) ?
													 x.nth(i).add(y.nth(i)) :
													 x.nth(i).add(y.nth(i)).add(lle(c[i]));
				// c(i+1) <= 0 or x(i)+y(i)+c(i) >= B
				Clause ltor = new Clause(lle(c[i+1]).le(0));
				ltor.add(lhs.ge(b));
				ret.add(ltor);

				// c(i+1) >= 1 or x(i)+y(i)+c(i) <= B-1
				Clause rtol = new Clause(lle(c[i+1]).ge(1));
				rtol.add(lhs.le(b-1));
				ret.add(rtol);
			}
			return ret;
		}

		case GE:{
			BooleanVariable[] s = new BooleanVariable[m-1];
			for (int i=1; i<m; i++) {
				s[i] = new BooleanVariable();
				csp.add(s[i]);
			}

			// -s(i+1) or (z(i)+B*c(i+1) >= x(i)+y(i)+c(i)) (when 0 <= i < m-1)
			for (int i=0; i<m-1; i++) {
				Clause cls = new Clause(new BooleanLiteral(s[i+1], true));
				LLExpression lhs = z.nth(i).add(lle(c[i+1]).mul(b));
				LLExpression rhs = (i == 0) ?
													 x.nth(i).add(y.nth(i)) :
													 x.nth(i).add(y.nth(i)).add(lle(c[i]));
				cls.add(lhs.ge(rhs));
				ret.add(cls);
			}
			// z(i) >= x(i)+y(i)+c(i) (when i == m-1)
			{
				int i = m-1;
				LLExpression lhs = z.nth(i);
				LLExpression rhs = (i == 0) ?
													 x.nth(i).add(y.nth(i)) :
													 x.nth(i).add(y.nth(i)).add(lle(c[i]));
				Clause cls = new Clause(lhs.ge(rhs));
				ret.add(cls);
			}

			// -s(i+1) or (z(i)+B*c(i+1)-1 >= x(i)+y(i)+c(i)) or s(i)
			// (when 1 <= i < m-1)
			for (int i=1; i<m-1; i++) {
				Clause cls = new Clause(new BooleanLiteral(s[i+1], true));
				LLExpression lhs = z.nth(i).add(lle(c[i+1]).mul(b)).sub(1);
				LLExpression rhs = x.nth(i).add(y.nth(i)).add(lle(c[i]));
				cls.add(lhs.ge(rhs));
				cls.add(new BooleanLiteral(s[i], false));
				ret.add(cls);
			}
			// (z(i)-1 >= x(i)+y(i)+c(i)) or s(i) (when i == m-1)
			{
				int i = m-1;
				LLExpression lhs = z.nth(i).sub(1);
				LLExpression rhs = (i == 0) ?
													 x.nth(i).add(y.nth(i)) :
													 x.nth(i).add(y.nth(i)).add(lle(c[i]));
				Clause cls = new Clause(lhs.ge(rhs));
				cls.add(new BooleanLiteral(s[i], false));
				ret.add(cls);
			}

			for (int i=0; i<m-1; i++) {
				LLExpression lhs = (i == 0) ?
													 x.nth(i).add(y.nth(i)) :
													 x.nth(i).add(y.nth(i)).add(lle(c[i]));
				// c(i+1) <= 0 or x(i)+y(i)+c(i) >= B
				Clause ltor = new Clause(lle(c[i+1]).le(0));
				ltor.add(lhs.ge(b));
				ret.add(ltor);

				// c(i+1) >= 1 or x(i)+y(i)+c(i) <= B-1
				Clause rtol = new Clause(lle(c[i+1]).ge(1));
				rtol.add(lhs.le(b-1));
				ret.add(rtol);
			}
			return ret;
		}

		case EQ:{
			for (int i=0; i<m; i++) {
				LLExpression lhs = (i == m-1) ?
													 z.nth(i) :
													 z.nth(i).add(lle(c[i+1]).mul(b));

				LLExpression rhs =  (i == 0) ?
														x.nth(i).add(y.nth(i)) :
														x.nth(i).add(y.nth(i)).add(lle(c[i]));

				ret.add(new Clause(lhs.le(rhs)));
				ret.add(new Clause(lhs.ge(rhs)));
			}
			return ret;
		}

		case NE:{
			BooleanVariable[] p = new BooleanVariable[m];
			BooleanVariable[] q = new BooleanVariable[m];
			Clause at_least_one = new Clause();
			for (int i=0; i<m; i++) {
				p[i] = new BooleanVariable();
				q[i] = new BooleanVariable();
				at_least_one.add(new BooleanLiteral(p[i], false));
				at_least_one.add(new BooleanLiteral(q[i], false));
			}
			ret.add(at_least_one);

			for (int i=0; i<m; i++) {
				LLExpression lhs = (i == m-1) ?
													 z.nth(i) :
													 z.nth(i).add(lle(c[i+1]).mul(b));

				LLExpression rhs =  (i == 0) ?
														x.nth(i).add(y.nth(i)) :
														x.nth(i).add(y.nth(i)).add(lle(c[i]));

				Clause cls0 = new Clause(new BooleanLiteral(p[i], true));
				cls0.add(lhs.le(rhs.sub(1)));
				ret.add(cls0);

				Clause cls1 = new Clause(new BooleanLiteral(q[i], true));
				cls1.add(lhs.sub(1).ge(rhs));
				ret.add(cls1);
			}

			for (int i=0; i<m-1; i++) {
				LLExpression lhs = (i == 0) ?
													 x.nth(i).add(y.nth(i)) :
													 x.nth(i).add(y.nth(i)).add(lle(c[i]));
				// c(i+1) <= 0 or x(i)+y(i)+c(i) >= B
				Clause ltor = new Clause(lle(c[i+1]).le(0));
				ltor.add(lhs.ge(b));
				ret.add(ltor);

				// c(i+1) >= 1 or x(i)+y(i)+c(i) <= B-1
				Clause rtol = new Clause(lle(c[i+1]).ge(1));
				rtol.add(lhs.le(b-1));
				ret.add(rtol);
			}
			return ret;
		}

		default:
			throw new SugarException("Internal Error");
		}
	}

	private LLExpression lle(IntegerVariable v) {
		return new LLExpression(v);
	}

	@Override
	public String toString() {
		return "("+op+ "add " + z + " " + x + " " + y + ")";
	}
}