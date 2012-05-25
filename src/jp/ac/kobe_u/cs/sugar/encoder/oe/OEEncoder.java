package jp.ac.kobe_u.cs.sugar.encoder.oe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.Logger;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.ArithmeticLiteral;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.LinearLiteral;
import jp.ac.kobe_u.cs.sugar.csp.LinearSum;
import jp.ac.kobe_u.cs.sugar.csp.Literal;
import jp.ac.kobe_u.cs.sugar.csp.Operator;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;

/**
 * Encoder encodes CSP into SAT by using the order encoding.
 * @see CSP
 * @author Tomoya Tanjo (tanjo@nii.ac.jp)
 */
public class OEEncoder extends Encoder {
	public OEEncoder(CSP csp) {
		super(csp);
	}

	@Override
	protected boolean isSimple(Literal lit) {
		if (lit instanceof BooleanLiteral)
			return true;
		else if (lit instanceof LinearLiteral) {
			final LinearLiteral l = (LinearLiteral)lit;
			return l.getLinearExpression().getCoef().size() <= 1
				&& (l.getOperator() == Operator.LE
						|| l.getOperator() == Operator.GE);
		}
		return false;
	}

	@Override
	public long getCode(LinearLiteral lit) throws SugarException {
		if (! isSimple(lit)) {
			throw new SugarException("Internal error " + lit.toString());
		}
		if (lit.getOperator() == Operator.EQ ||
		    lit.getOperator() == Operator.NE) {
			throw new SugarException("Internal error " + lit.toString());
		}
		final LinearSum ls = lit.getLinearExpression();
		final long b = ls.getB();
		long code;
		if (ls.size() == 0) {
			code = (b <= 0) ? TRUE_CODE : FALSE_CODE;
		} else {
			final IntegerVariable v = ls.getCoef().firstKey();
			final long a = ls.getA(v);
			code = getCodeLE(v, a, -b);
		}
		return code;
	}

	private long getCodeLE(IntegerVariable var, long value) {
		final IntegerDomain domain = var.getDomain();
		if (value < domain.getLowerBound()) {
			return FALSE_CODE;
		} else if (value >= domain.getUpperBound()) {
			return TRUE_CODE;
		}
		return var.getCode() + sizeLE(domain, value) - 1;
	}

	private long sizeLE(IntegerDomain d, long value) {
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
	protected void encode(IntegerVariable v) throws SugarException, IOException {
		writer.writeComment(v.toString());
		final IntegerDomain domain = v.getDomain();
		final long[] clause = new long[2];
		long a0 = domain.getLowerBound();
		for (long a = a0 + 1; a <= domain.getUpperBound(); a++) {
			if (domain.contains(a)) {
				clause[0] = negateCode(getCodeLE(v, a0));
				clause[1] = getCodeLE(v, a);
				writer.writeClause(clause);
				a0 = a;
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
	private void encode(LinearSum ls, IntegerVariable[] vs, int i, long s, long[] clause)
		throws IOException, SugarException {
		if (i >= vs.length - 1) {
			final long a = ls.getA(vs[i]);
			// encoder.writeComment(a + "*" + vs[i].getName() + " <= " + (-s));
			clause[i] = getCodeLE(vs[i], a, -s);
			writer.writeClause(clause);
		} else {
			long lb0 = s;
			long ub0 = s;
			for (int j = i + 1; j < vs.length; j++) {
				long a = ls.getA(vs[j]); 
				if (a > 0) {
					lb0 += a * vs[j].getDomain().getLowerBound();
					ub0 += a * vs[j].getDomain().getUpperBound();
				} else {
					lb0 += a * vs[j].getDomain().getUpperBound();
					ub0 += a * vs[j].getDomain().getLowerBound();
				}
			}
			final long a = ls.getA(vs[i]);
			final IntegerDomain domain = vs[i].getDomain();
			long lb = domain.getLowerBound();
			long ub = domain.getUpperBound();
			if (a >= 0) {
				// ub = Math.min(ub, (int)Math.floor(-(double)lb0 / a));
				if (-lb0 >= 0) {
					ub = Math.min(ub, -lb0/a);
				} else {
					ub = Math.min(ub, (-lb0-a+1)/a);
				}
				// XXX
				final Iterator<Long> iter = domain.values(lb, ub);
				while (iter.hasNext()) {
					final long c = iter.next();
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
				Iterator<Long> iter = domain.values(lb, ub);
				while (iter.hasNext()) {
					final long c = iter.next();
					// vs[i]<=c -> ...
					clause[i] = negateCode(getCodeLE(vs[i], c));
					encode(ls, vs, i+1, s+a*c, clause);
				}
			}
		}
	}

	@Override
	protected void encode(LinearLiteral lit, long[] clause) throws SugarException, IOException {
		if (lit.getOperator() == Operator.EQ
		    || lit.getOperator() == Operator.NE) {
			throw new SugarException("Internal error " + lit.toString());
		}
		if (lit.isValid()) {
		} if (isSimple(lit)) {
			clause = expand(clause, 1);
			clause[0] = getCode(lit);
			writer.writeClause(clause);
		} else {
			final LinearSum ls = lit.getLinearExpression();
			final IntegerVariable[] vs = lit.getLinearExpression().getVariablesSorted();
			final int n = ls.size();
			clause = expand(clause, n);
			encode(ls, vs, 0, lit.getLinearExpression().getB(), clause);
		}
	}


	private long getCodeLE(IntegerVariable v, long a, long b) {
		long code;
		if (a >= 0) {
//			int c = (int) Math.floor((double) b / a);
			long c;
			if (b >= 0) {
				c = b/a;
			} else {
				c = (b-a+1)/a;
			}
			code = getCodeLE(v, c);
		} else {
//			int c = (int) Math.ceil((double) b / a) - 1;
			long c;
			if (b >= 0) {
				c = b/a - 1;
			} else {
				c = (b+a+1)/a - 1;
			}
			code = negateCode(getCodeLE(v, c));
		}
		return code;
	}

	private boolean satSizeLE(LinearSum sum, long limit) throws SugarException {
		if (sum.getCoef().size() <= 1) {
			return 1 <= limit;
		} else {
			IntegerVariable[] vs = sum.getVariablesSorted();
			long size = calcSatSize(sum, limit, vs, 0, sum.getB());
			return size <= limit;
		}
	}

	private long calcSatSize(LinearSum sum, long limit, IntegerVariable[] vs, int i, long s) throws SugarException {
		long size = 0;
		if (i >= vs.length - 1) {
			size = 1;
		} else {
			long lb0 = s;
			long ub0 = s;
			for (int j = i + 1; j < vs.length; j++) {
				long a = sum.getA(vs[j]);
				if (a > 0) {
					lb0 += a * vs[j].getDomain().getLowerBound();
					ub0 += a * vs[j].getDomain().getUpperBound();
				} else {
					lb0 += a * vs[j].getDomain().getUpperBound();
					ub0 += a * vs[j].getDomain().getLowerBound();
				}
			}
			long a = sum.getA(vs[i]);
			IntegerDomain domain = vs[i].getDomain();
			long lb = domain.getLowerBound();
			long ub = domain.getUpperBound();
			if (a >= 0) {
				// ub = Math.min(ub, (int)Math.floor(-(double)lb0 / a));
				if (-lb0 >= 0) {
					ub = Math.min(ub, -lb0/a);
				} else {
					ub = Math.min(ub, (-lb0-a+1)/a);
				}
				Iterator<Long> iter = domain.values(lb, ub);
				while (iter.hasNext()) {
					long c = iter.next();
					// vs[i]>=c -> ...
					// encoder.writeComment(vs[i].getName() + " <= " + (c-1));
					size += calcSatSize(sum, limit, vs, i+1, s+a*c);
					if (size > limit) {
						return size;
					}
				}
				size += calcSatSize(sum, limit, vs, i+1, s+a*(ub+1));
				if (size > limit) {
					return size;
				}
			} else {
				// lb = Math.max(lb, (int)Math.ceil(-(double)lb0/a));
				if (-lb0 >= 0) {
					lb = Math.max(lb, -lb0/a);
				} else {
					lb = Math.max(lb, (-lb0+a+1)/a);
				}
				size += calcSatSize(sum, limit, vs, i+1, s+a*(lb-1));
				if (size > limit) {
					return size;
				}
				Iterator<Long> iter = domain.values(lb, ub);
				while (iter.hasNext()) {
					long c = iter.next();
					// vs[i]<=c -> ...
					size += calcSatSize(sum, limit, vs, i+1, s+a*c);
					if (size > limit) {
						return size;
					}
				}
			}
		}
		return size;
	}

	/**
	 * 符号化しやすい節に還元する．
	 * 1. 全ての LinearLiteral を
	 *  a1*x1+a2*x2+...+b <= 0
	 *  の形にする．
	 **/
	@Override
	public void reduce() throws SugarException {
		split();
		simplify();
		toLinearLe();
		Logger.info("CSP : " + csp.summary());
	}

	/**
	 * LinearLiteral 中の整数変数の数を制限する．
	 */
	private void split() throws SugarException {
		final String AUX_PREFIX = "RS";
		BooleanVariable.setPrefix(AUX_PREFIX);
		BooleanVariable.setIndex(0);
		IntegerVariable.setPrefix(AUX_PREFIX);
		IntegerVariable.setIndex(0);
		// TODO
		final List<Clause> newClauses = new ArrayList<Clause>();
		final int size = csp.getClauses().size();
		for(int i=0; i<size; i++) {
			final Clause c = csp.getClauses().get(i);
			csp.getClauses().set(i, null);
			final Clause cls = new Clause(c.getBooleanLiterals());
			cls.setComment(c.getComment());
			for(ArithmeticLiteral al: c.getArithmeticLiterals()) {
				if (al instanceof LinearLiteral) {
					final LinearLiteral ll = (LinearLiteral)al;
					final LinearSum ls = simplifyLinearExpression(ll.getLinearExpression(), true, newClauses);
					cls.add(new LinearLiteral(ls, ll.getOperator()));
				} else {
					cls.add(al);
				}
			}
			newClauses.add(cls);
		}
		csp.setClauses(newClauses);
	}

	private LinearSum simplifyLinearExpression(LinearSum e, boolean first, List<Clause> clss) throws SugarException {
		if (ESTIMATE_SATSIZE) {
			if (satSizeLE(e, MAX_LINEARSUM_SIZE)) {
				return e;
			}
		} else {
			if (e.size() <= 1 || ! e.isDomainLargerThan(MAX_LINEARSUM_SIZE)) {
			// if (e.size() <= 1 || ! e.isDomainLargerThanExcept(MAX_LINEARSUM_SIZE)) {
				return e;
			}
		}
		long b = e.getB();
		LinearSum[] es = e.split(first ? 3 : SPLITS);
		e = new LinearSum(b);
		for (int i = 0; i < es.length; i++) {
			LinearSum ei = es[i];
			long factor = ei.factor();
			if (factor > 1) {
				ei.divide(factor);
			}
			ei = simplifyLinearExpression(ei, false, clss);
			// System.out.println(es[i] + " ==> " + ei);
			if (ei.size() > 1) {
				IntegerVariable v = new IntegerVariable(ei.getDomain());
				v.setComment(v.getName() + " : " + ei);
				csp.add(v);
				/// v == ei
				final LinearSum auxSum = ei;
				auxSum.subtract(new LinearSum(v));
				final Clause aux = new Clause(new LinearLiteral(auxSum, Operator.EQ));
				aux.setComment(v.getName() + " == " + auxSum);
				clss.add(aux);
				ei = new LinearSum(v);
			}
			if (factor > 1) {
				ei.multiply(factor);
			}
			e.add(ei);
		}
		return e;
	}

	private void toLinearLe() throws SugarException {
		final String AUX_PREFIX = "RL";
		BooleanVariable.setPrefix(AUX_PREFIX);
		BooleanVariable.setIndex(0);
		IntegerVariable.setPrefix(AUX_PREFIX);
		IntegerVariable.setIndex(0);

		final List<Clause> newClauses = new ArrayList<Clause>();
		final int size = csp.getClauses().size();
		for (int i=0; i<size; i++) {
			final Clause c = csp.getClauses().get(i);
			csp.getClauses().set(i, null);
			if (c.size() == simpleSize(c)) {
				newClauses.add(c);
			} else {
				assert c.size() == simpleSize(c)+1;
				final LinearLiteral ll = (LinearLiteral)c.getArithmeticLiterals().get(0);
				final List<BooleanLiteral> bls = c.getBooleanLiterals();
				switch(ll.getOperator()) {
				case LE:{
					newClauses.add(c);
					break;
				}
				case EQ:{
					final Clause c1 = new Clause(bls);
					c1.add(new LinearLiteral(ll.getLinearExpression(),
					                         Operator.LE));
					newClauses.add(c1);
					final Clause c2 = new Clause(bls);
					final LinearSum ls = new LinearSum(ll.getLinearExpression());
					ls.multiply(-1);
					c2.add(new LinearLiteral(ls, Operator.LE));
					newClauses.add(c2);
					break;
				}
				case NE:{
					final Clause c1 = new Clause(bls);

					final LinearSum ls1 = new LinearSum(ll.getLinearExpression());
					ls1.setB(ls1.getB()+1);
					c1.add(new LinearLiteral(ls1, Operator.LE));

					final LinearSum ls2 = new LinearSum(ll.getLinearExpression());
					ls2.multiply(-1);
					ls2.setB(ls2.getB()+1);
					c1.add(new LinearLiteral(ls2, Operator.LE));

					newClauses.addAll(simplify(c1));
					break;
				}
				default: new SugarException("Internal Error");
				}
			}
		}
		csp.setClauses(newClauses);
	}

	@Override
	public long getSatVariablesSize(IntegerVariable v) {
		return v.getDomain().size()-1;
	}
}
