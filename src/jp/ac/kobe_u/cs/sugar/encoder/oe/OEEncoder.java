package jp.ac.kobe_u.cs.sugar.encoder.oe;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.Logger;
import jp.ac.kobe_u.cs.sugar.SugarException;
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
 * Encoder encodes CSP into SAT.
 * @see CSP 
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
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
	public BigInteger getCode(LinearLiteral lit) throws SugarException {
		if (! isSimple(lit)) {
			throw new SugarException("Internal error " + lit.toString());
		}
		if (lit.getOperator() == Operator.EQ ||
		    lit.getOperator() == Operator.NE) {
			throw new SugarException("Internal error " + lit.toString());
		}
		final LinearSum ls = lit.getLinearExpression();
		final BigInteger b = ls.getB();
		BigInteger code;
		if (ls.size() == 0) {
			code = (b.compareTo(BigInteger.ZERO) <= 0) ? TRUE_CODE : FALSE_CODE;
		} else {
			final IntegerVariable v = ls.getCoef().firstKey();
			final BigInteger a = ls.getA(v);
			code = getCodeLE(v, a, b.negate());
		}
		return code;
	}

	private BigInteger getCodeLE(IntegerVariable var, BigInteger value) {
		final IntegerDomain domain = var.getDomain();
		if (value.compareTo(domain.getLowerBound()) < 0) {
			return FALSE_CODE;
		} else if (value.compareTo(domain.getUpperBound()) >= 0) {
			return TRUE_CODE;
		}
		return var.getCode().add(sizeLE(domain, value)).subtract(BigInteger.ONE);
	}

	private BigInteger sizeLE(IntegerDomain d, BigInteger value) {
		if (value.compareTo(d.getLowerBound()) < 0)
			return BigInteger.ZERO;
		if (value.compareTo(d.getUpperBound()) >= 0)
			return d.size();
		if (d.isContiguous()) {
			return value.subtract(d.getLowerBound()).add(BigInteger.ONE);
		} else {
			// ここがまずい．オーバーフローの可能性あり
			return new BigInteger(Integer.toString(d.headSet(value.add(BigInteger.ONE)).size()));
		}
	}

	@Override
	protected void encode(IntegerVariable v) throws SugarException, IOException {
		writer.writeComment(v.toString());
		final IntegerDomain domain = v.getDomain();
		final BigInteger[] clause = new BigInteger[2];
		BigInteger a0 = domain.getLowerBound();
		for (BigInteger a = a0.add(BigInteger.ONE); a.compareTo(domain.getUpperBound()) <= 0; a = a.add(BigInteger.ONE)) {
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
	private void encode(LinearSum ls, IntegerVariable[] vs, int i, BigInteger s, BigInteger[] clause)
		throws IOException, SugarException {
		if (i >= vs.length - 1) {
			final BigInteger a = ls.getA(vs[i]);
			// encoder.writeComment(a + "*" + vs[i].getName() + " <= " + (-s));
			clause[i] = getCodeLE(vs[i], a, s.negate());
			writer.writeClause(clause);
		} else {
			BigInteger lb0 = s;
			BigInteger ub0 = s;
			for (int j = i + 1; j < vs.length; j++) {
				BigInteger a = ls.getA(vs[j]);
				if (a.compareTo(BigInteger.ZERO) > 0) {
					lb0 = lb0.add(a.multiply(vs[j].getDomain().getLowerBound()));
					ub0 = ub0.add(a.multiply(vs[j].getDomain().getUpperBound()));
				} else {
					lb0 = lb0.add(a.multiply(vs[j].getDomain().getUpperBound()));
					ub0 = ub0.add(a.multiply(vs[j].getDomain().getLowerBound()));
				}
			}
			final BigInteger a = ls.getA(vs[i]);
			final IntegerDomain domain = vs[i].getDomain();
			BigInteger lb = domain.getLowerBound();
			BigInteger ub = domain.getUpperBound();
			if (a.compareTo(BigInteger.ZERO) >= 0) {
				// ub = Math.min(ub, (int)Math.floor(-(double)lb0 / a));
				if (lb0.compareTo(BigInteger.ZERO) <= 0) {
					ub = ub.min(lb0.negate().divide(a));
				} else {
					ub = ub.min(lb0.negate().subtract(a).add(BigInteger.ONE).divide(a));
				}
				// XXX
				final Iterator<BigInteger> iter = domain.values(lb, ub);
				while (iter.hasNext()) {
					final BigInteger c = iter.next();
					// vs[i]>=c -> ...
					// encoder.writeComment(vs[i].getName() + " <= " + (c-1));
					clause[i] = getCodeLE(vs[i], c.subtract(BigInteger.ONE));
					encode(ls, vs, i+1, s.add(a.multiply(c)), clause);
				}
				clause[i] = getCodeLE(vs[i], ub);
				encode(ls, vs, i+1, s.add(a.multiply(ub.add(BigInteger.ONE))), clause);
			} else {
				// lb = Math.max(lb, (int)Math.ceil(-(double)lb0/a));
				if (lb0.compareTo(BigInteger.ZERO) <= 0) {
					lb = lb.max(lb0.negate().divide(a));
				} else {
					lb = lb.max(lb0.negate().add(a).add(BigInteger.ONE).divide(a));
				}
				// XXX
				clause[i] = negateCode(getCodeLE(vs[i], lb.subtract(BigInteger.ONE)));
				encode(ls, vs, i+1, s.add(a.multiply(lb.subtract(BigInteger.ONE))), clause);
				Iterator<BigInteger> iter = domain.values(lb, ub);
				while (iter.hasNext()) {
					final BigInteger c = iter.next();
					// vs[i]<=c -> ...
					clause[i] = negateCode(getCodeLE(vs[i], c));
					encode(ls, vs, i+1, s.add(a.multiply(c)), clause);
				}
			}
		}
	}

	@Override
	protected void encode(LinearLiteral lit, BigInteger[] clause) throws SugarException, IOException {
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


	private BigInteger getCodeLE(IntegerVariable v, BigInteger a, BigInteger b) {
		BigInteger code;
		if (a.compareTo(BigInteger.ZERO) >= 0) {
//			int c = (int) Math.floor((double) b / a);
			BigInteger c;
			if (b.compareTo(BigInteger.ZERO) >= 0) {
				c = b.divide(a);
			} else {
				c = b.subtract(a).add(BigInteger.ONE).divide(a);
			}
			code = getCodeLE(v, c);
		} else {
//			int c = (int) Math.ceil((double) b / a) - 1;
			BigInteger c;
			if (b.compareTo(BigInteger.ZERO) >= 0) {
				c = b.divide(a).subtract(BigInteger.ONE);
			} else {
				c = b.add(a).add(BigInteger.ONE).divide(a.subtract(BigInteger.ONE));
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
	private void split() {
		final String AUX_PREFIX = "RS";
		BooleanVariable.setPrefix(AUX_PREFIX);
		BooleanVariable.setIndex(BigInteger.ZERO);
		IntegerVariable.setPrefix(AUX_PREFIX);
		IntegerVariable.setIndex(BigInteger.ZERO);
		// TODO
	}

	private void toLinearLe() throws SugarException {
		final String AUX_PREFIX = "RL";
		BooleanVariable.setPrefix(AUX_PREFIX);
		BooleanVariable.setIndex(BigInteger.ZERO);
		IntegerVariable.setPrefix(AUX_PREFIX);
		IntegerVariable.setIndex(BigInteger.ZERO);

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
					ls.multiply(BigInteger.ONE.negate());
					c2.add(new LinearLiteral(ls, Operator.LE));
					newClauses.add(c2);
					break;
				}
				case NE:{
					final Clause c1 = new Clause(bls);

					final LinearSum ls1 = new LinearSum(ll.getLinearExpression());
					ls1.setB(ls1.getB().add(BigInteger.ONE));
					c1.add(new LinearLiteral(ls1, Operator.LE));

					final LinearSum ls2 = new LinearSum(ll.getLinearExpression());
					ls2.multiply(BigInteger.ONE.negate());
					ls2.setB(ls2.getB().add(BigInteger.ONE));
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
	public BigInteger getSatVariablesSize(IntegerVariable v) {
		return v.getDomain().size().subtract(BigInteger.ONE);
	}
}
