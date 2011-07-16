package jp.ac.kobe_u.cs.sugar.encoder.oe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.encoder.Encoder;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.Operator;
import jp.ac.kobe_u.cs.sugar.csp.LinearLiteral;
import jp.ac.kobe_u.cs.sugar.csp.LinearSum;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;

/**
 * Encoder encodes CSP into SAT.
 * @see CSP 
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class OEEncoder extends Encoder {
	public OEEncoder(CSP csp) {
		super(csp);
	}

	@Override
	public int getCode(LinearLiteral lit) throws SugarException {
		if (! lit.isSimple()) {
			throw new SugarException("Internal error " + lit.toString());
		}
		if (lit.getOperator() == Operator.EQ ||
		    lit.getOperator() == Operator.NE) {
			throw new SugarException("Internal error " + lit.toString());
		}
		LinearSum ls = lit.getLinearExpression();
		int b = ls.getB();
		int code;
		if (ls.size() == 0) {
			code = (b <= 0) ? TRUE_CODE : FALSE_CODE;
		} else {
			IntegerVariable v = ls.getCoef().firstKey();
			int a = ls.getA(v);
			code = getCodeLE(v, a, -b);
		}
		return code;
	}

	private int getCodeLE(IntegerVariable var, int value) {
		IntegerDomain domain = var.getDomain();
		if (value < domain.getLowerBound()) {
			return FALSE_CODE;
		} else if (value >= domain.getUpperBound()) {
			return TRUE_CODE;
		}
		return var.getCode() + sizeLE(domain, value) - 1;
	}

	private int sizeLE(IntegerDomain d, int value) {
		if (value < d.getLowerBound())
			return 0;
		if (value >= d.getUpperBound())
			return d.size();
		if (d.isContiguous()) {
			return value - d.getLowerBound() + 1;
		} else {
			return d.headSet(value + 1).size();
		}
	}

	@Override
	public void encode(IntegerVariable ivar) throws SugarException, IOException {
		writeComment(ivar.toString());
		if (ivar.getDigits() == null) {
			IntegerDomain domain = ivar.getDomain();
			int[] clause = new int[2];
			int a0 = domain.getLowerBound();
			for (int a = a0 + 1; a <= domain.getUpperBound(); a++) {
				if (domain.contains(a)) {
					clause[0] = negateCode(getCodeLE(ivar, a0));
					clause[1] = getCodeLE(ivar, a);
					writeClause(clause);
					a0 = a;
				}
			}
		}
	}

	/*
	 * a1*v1+a2*v2+a3*v3+b <= 0
	 * <--> v1>=c1 -> a2*v2+a3*v3+b+a1*c1 <= 0 (when a1>0)
	 *      v1<=c1 -> a2*v2+a3*v3+b+a1*c1 <= 0 (when a1<0)
	 * <--> v1>=c1 -> v2>=c2 -> a3*v3+b+a1*c1+a2*c2<= 0 (when a1>0, a2>0)
	 * 
	 */
	private void encode(LinearSum ls, IntegerVariable[] vs, int i, int s, int[] clause)
		throws IOException, SugarException {
		if (i >= vs.length - 1) {
			int a = ls.getA(vs[i]);
			// encoder.writeComment(a + "*" + vs[i].getName() + " <= " + (-s));
			clause[i] = getCodeLE(vs[i], a, -s);
			writeClause(clause);
		} else {
			int lb0 = s;
			int ub0 = s;
			for (int j = i + 1; j < vs.length; j++) {
				int a = ls.getA(vs[j]); 
				if (a > 0) {
					lb0 += a * vs[j].getDomain().getLowerBound();
					ub0 += a * vs[j].getDomain().getUpperBound();
				} else {
					lb0 += a * vs[j].getDomain().getUpperBound();
					ub0 += a * vs[j].getDomain().getLowerBound();
				}
			}
			int a = ls.getA(vs[i]);
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
					clause[i] = getCodeLE(vs[i], c - 1);
					encode(ls, vs, i+1, s+a*c, clause);
				}
				clause[i] = getCodeLE(vs[i], ub);
				encode(ls, vs, i+1, s+a*(ub+1), clause);
			} else {
				// lb = Math.max(lb, (int)Math.ceil(-(double)lb0/a));
				if (-lb0 >= 0) {
					lb = Math.max(lb, -lb0/a);
				} else {
					lb = Math.max(lb, (-lb0+a+1)/a);
				}
				// XXX
				clause[i] = negateCode(getCodeLE(vs[i], lb - 1));
				encode(ls, vs, i+1, s+a*(lb-1), clause);
				Iterator<Integer> iter = domain.values(lb, ub);
				while (iter.hasNext()) {
					int c = iter.next();
					// vs[i]<=c -> ...
					clause[i] = negateCode(getCodeLE(vs[i], c));
					encode(ls, vs, i+1, s+a*c, clause);
				}
			}
		}
	}

	@Override
	public void encode(LinearLiteral lit, int[] clause) throws SugarException, IOException {
		if (lit.getOperator() == Operator.EQ
		    || lit.getOperator() == Operator.NE) {
			throw new SugarException("Internal error " + lit.toString());
		}
		if (lit.isValid()) {
		} if (lit.isSimple()) {
			clause = expand(clause, 1);
			clause[0] = getCode(lit);
			writeClause(clause);
		} else {
			LinearSum ls = lit.getLinearExpression();
			IntegerVariable[] vs = lit.getLinearExpression().getVariablesSorted();
			int n = ls.size();
			clause = expand(clause, n);
			encode(ls, vs, 0, lit.getLinearExpression().getB(), clause);
		}
	}


	private int getCodeLE(IntegerVariable v, int a, int b) {
		int code;
		if (a >= 0) {
//			int c = (int) Math.floor((double) b / a);
			int c;
			if (b >= 0) {
				c = b/a;
			} else {
				c = (b-a+1)/a;
			}
			code = getCodeLE(v, c);
		} else {
//			int c = (int) Math.ceil((double) b / a) - 1;
			int c;
			if (b >= 0) {
				c = b/a - 1;
			} else {
				c = (b+a+1)/a - 1;
			}
			code = negateCode(getCodeLE(v, c));
		}
		return code;
	}

	/**
	 * 符号化しやすい節に還元する．
	 * 1. 全ての LinearLiteral を
	 *  a1*x1+a2*x2+...+b <= 0
	 *  の形にする．
	 * 2. TODO LinearLiteral 中の整数変数の数を制限する．
	 *  (Not implemented)
	 **/
	@Override
	public void reduce() throws SugarException {
		final String AUX_PREFIX = "R";
		BooleanVariable.setPrefix(AUX_PREFIX);
		BooleanVariable.setIndex(0);
		IntegerVariable.setPrefix(AUX_PREFIX);
		IntegerVariable.setIndex(0);

		List<Clause> newClauses = new ArrayList<Clause>();
		for (Clause c: csp.getClauses()) {
			if (c.getArithmeticLiterals().size() == 0) {
				newClauses.add(c);
			} else {
				assert c.getArithmeticLiterals().size() == 1;
				LinearLiteral ll = (LinearLiteral)c.getArithmeticLiterals().get(0);
				List<BooleanLiteral> bls = c.getBooleanLiterals();
				switch(ll.getOperator()) {
				case LE:{
					newClauses.add(c);
					break;
				}
				case EQ:{
					Clause c1 = new Clause(bls);
					c1.add(new LinearLiteral(ll.getLinearExpression(),
					                         Operator.LE));
					newClauses.add(c1);
					Clause c2 = new Clause(bls);
					LinearSum ls = new LinearSum(ll.getLinearExpression());
					ls.multiply(-1);
					c2.add(new LinearLiteral(ls, Operator.LE));
					newClauses.add(c2);
					break;
				}
				case NE:{
					Clause c1 = new Clause(bls);
					LinearSum ls1 = new LinearSum(ll.getLinearExpression());
					ls1.setB(ls1.getB()+1);
					c1.add(new LinearLiteral(ls1, Operator.LE));
					newClauses.add(c1);

					LinearSum ls2 = new LinearSum(ll.getLinearExpression());
					ls2.multiply(-1);
					ls2.setB(ls2.getB()+1);
					BooleanVariable p = new BooleanVariable();
					csp.add(p);
					BooleanLiteral posLiteral = new BooleanLiteral(p, false);
					BooleanLiteral negLiteral = new BooleanLiteral(p, true);
					Clause c2 = new Clause();
					c2.add(negLiteral);
					c2.add(new LinearLiteral(ls2, Operator.LE));
					c1.add(posLiteral);
					newClauses.add(c2);
					break;
				}
				default: new SugarException("Internal Error");
				}
			}
		}
		csp.setClauses(newClauses);
	}

	@Override
	public int getSatVariablesSize(IntegerVariable v) {
		if (v.getDigits() != null)
			return 0;
		return v.getDomain().size()-1;
	}
}
