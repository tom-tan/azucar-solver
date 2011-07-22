package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.io.IOException;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map.Entry;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.Logger;
import jp.ac.kobe_u.cs.sugar.csp.ArithmeticLiteral;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.Operator;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.LinearLiteral;
import jp.ac.kobe_u.cs.sugar.csp.LinearSum;
import jp.ac.kobe_u.cs.sugar.csp.Literal;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;
import jp.ac.kobe_u.cs.sugar.encoder.oe.OEEncoder;

public class COEEncoder extends OEEncoder {
	private int[] bases;
	private int ndigits;

	public COEEncoder(CSP csp) {
		super(csp);
		ndigits = 2;
	}

	public COEEncoder(CSP csp, int[] bases) {
		this(csp);
		this.bases = bases;
		ndigits = 0;
	}

	public COEEncoder(CSP csp, int ndigits) {
		this(csp);
		this.ndigits = ndigits;
	}

	@Override
	protected boolean isSimple(Literal lit) {
		if (lit instanceof OpXY) {
			OpXY l = (OpXY)lit;
			assert !l.getVariables().isEmpty();
			return l.getVariables().size() == 1
				&& l.getVariables().iterator().next().getDigits().length <= 1;
		} else if (lit instanceof EqMul) {
			EqMul l = (EqMul)lit;
			assert !l.getVariables().isEmpty();
			return l.getVariables().size() == 1
				&& l.getVariables().iterator().next().getDigits().length <= 1;
		} else if (lit instanceof OpAdd) {
			return false;
		}
		return super.isSimple(lit);
	}

	@Override
	protected void encode(IntegerVariable v) throws SugarException, IOException {
		if (v.getDigits().length <= 1) {
			super.encode(v);
		}
	}

	@Override
	public void reduce() throws SugarException {
		Logger.fine("Compact Order Encoding: Recuding CSP");
		// System.out.println("======== CSP =========\n"+csp);
		adjust();
		// System.out.println("======== CSP =========\n"+csp);
		toTernary();
		// System.out.println("======== CSP =========\n"+csp);
		toRCSP();
		// System.out.println("======== CSP =========\n"+csp);
		if (bases == null) {
			int size = 0;
			for (IntegerVariable v : csp.getIntegerVariables()) {
				size = Math.max(size, v.getDomain().size());
			}
			bases = new int[1];
			bases[0] = (int)Math.ceil(Math.pow(size, 1.0/ndigits));
			Logger.fine("Compact Order Encoding: Largest Domain size = "+ size);
		}
		Logger.fine("Compact Order Encoding: Base = "+ bases[0]);
		csp.setBases(bases);
		simplify();
		// System.out.println("======== CSP =========\n"+csp);
		toCCSP();
		Logger.fine("Compact Order Encoding: Reduction finished");
		// System.out.println("======== CSP =========\n"+csp);
	}

	private void toTernary() throws SugarException {
		final String AUX_PREFIX = "RT";
		BooleanVariable.setPrefix(AUX_PREFIX);
		BooleanVariable.setIndex(0);
		IntegerVariable.setPrefix(AUX_PREFIX);
		IntegerVariable.setIndex(0);

		List<Clause> newClauses = new ArrayList<Clause>();
		for (Clause c: csp.getClauses()) {
			if (c.getArithmeticLiterals().size() == 0) {
				newClauses.add(c);
			} else {
				Clause cls = new Clause(c.getBooleanLiterals());
				cls.setComment(c.getComment());
				for (ArithmeticLiteral lit: c.getArithmeticLiterals()) {
					LinearLiteral ll = (LinearLiteral)lit;
					if (ll.getLinearExpression().size() <= 3) {
						cls.add(ll);
						continue;
					}
					LinearSum ls = simplifyLinearExpression(ll.getLinearExpression(), newClauses);
					cls.add(new LinearLiteral(ls, ll.getOperator()));
				}
				newClauses.add(cls);
			}
		}
		csp.setClauses(newClauses);
	}

	private void toRCSP() throws SugarException{
		final String AUX_PREFIX = "RR";
		BooleanVariable.setPrefix(AUX_PREFIX);
		BooleanVariable.setIndex(0);
		IntegerVariable.setPrefix(AUX_PREFIX);
		IntegerVariable.setIndex(0);

		List<Clause> newClauses = new ArrayList<Clause>();
		for (Clause c: csp.getClauses()) {
			if (c.getArithmeticLiterals().size() == 0) {
				c.setComment(c.toString());
				newClauses.add(c);
			} else {
				Clause cls = new Clause(c.getBooleanLiterals());
				if (c.getComment() == null) {
					cls.setComment(c.toString());
				} else {
					cls.setComment(c.getComment());
				}

				for (ArithmeticLiteral al: c.getArithmeticLiterals()) {
					LinearLiteral ll = (LinearLiteral)al;
					LinearSum ls = ll.getLinearExpression();
					LinearSum lhs, rhs;
					if (ls.getB() > 0) {
						lhs = new LinearSum(ls.getB());
						rhs = new LinearSum(0);
					} else {
						lhs = new LinearSum(0);
						rhs = new LinearSum(-ls.getB());
					}
					for (Entry<IntegerVariable, Integer> es :
								 ls.getCoef().entrySet()) {
						int a = es.getValue();
						IntegerVariable v = es.getKey();
						if (a == 1) {
							lhs.setA(1, v);
							continue;
						} else if (a == -1) {
							rhs.setA(1, v);
							continue;
						}
						a = Math.abs(a);
						assert v.getDomain().getLowerBound() == 0;
						IntegerDomain dom = v.getDomain().mul(a);
						IntegerVariable av = new IntegerVariable(dom);
						csp.add(av);
						Literal lit = new EqMul(av, a, v);
						newClauses.add(new Clause(lit));
						if (es.getValue() > 0) {
							lhs.add(new LinearSum(av));
						} else {
							rhs.add(new LinearSum(av));
						}
					}

					lhs = simplifyForRCSP(lhs, newClauses,
																(lhs.getB() == 0) ? 2 : 1);
					int lsize = lhs.size() + (lhs.getB() == 0 ? 0 : 1);
					int rsize = rhs.size() + (rhs.getB() == 0 ? 0 : 1);
					if (rsize >= 3) {
						rhs = simplifyForRCSP(rhs, newClauses,
																	(rhs.getB() == 0) ? 2 : 1);
					} else if (rsize == 2 && lsize == 2) {
						if (rhs.getB() == 0) {
							rhs = simplifyForRCSP(rhs, newClauses, 1);
						} else {
							IntegerDomain dom = new IntegerDomain(0, rhs.getDomain().getUpperBound());
							List<IntegerHolder> rh = getHolders(rhs);
							IntegerVariable ax = new IntegerVariable(dom);
							csp.add(ax);
							Literal geB = new OpXY(Operator.GE, ax, rhs.getB());
							newClauses.add(new Clause(geB));
							Literal eqadd = new OpAdd(Operator.EQ, ax, rh.get(0),
																				rh.get(1));
							newClauses.add(new Clause(eqadd));
							rhs = new LinearSum(ax);
						}
					}

					Operator op = ll.getOperator();
					List<IntegerHolder> lh = getHolders(lhs);
					List<IntegerHolder> rh = getHolders(rhs);
					assert lh.size()+rh.size() <= 3;

					Literal lit = null;
					if (lh.size() == 1 && rh.size() == 1) {
						lit = new OpXY(op, lh.get(0), rh.get(0));

					} else if (lh.size() == 1 && rh.size() == 2) {
						lit = new OpAdd(op, lh.get(0), rh.get(0), rh.get(1));

					} else if (lh.size() == 2 && rh.size() == 1) {
						switch(op) {
						case LE:
							lit = new OpAdd(Operator.GE, rh.get(0),
															lh.get(0), lh.get(1));
						case GE:
							lit = new OpAdd(Operator.LE, rh.get(0),
															lh.get(0), lh.get(1));
						default:
							lit = new OpAdd(op, rh.get(0),
															lh.get(0), lh.get(1));
						}
					}
					cls.add(lit);
				}
				newClauses.add(cls);
			}
		}
		csp.setClauses(newClauses);
	}

	private void toCCSP() throws SugarException {
		final String AUX_PREFIX = "RC";
		BooleanVariable.setPrefix(AUX_PREFIX);
		BooleanVariable.setIndex(0);
		IntegerVariable.setPrefix(AUX_PREFIX);
		IntegerVariable.setIndex(0);

		List<Clause> newClauses = new ArrayList<Clause>();
		List<IntegerVariable> newVars = new ArrayList<IntegerVariable>();
		for (IntegerVariable v : csp.getIntegerVariables()) {
			newVars.addAll(v.splitToDigits(csp));
			int ub = v.getDomain().getUpperBound();
			int m = v.getDigits().length;
			if (m > 1 ||
					ub <= Math.pow(bases[0], m)-1) {
				RCSPLiteral le = new OpXY(Operator.LE, v, ub);
				List<Clause> lst = le.toCCSP(csp, this);
				newClauses.addAll(lst);
			}
		}
		for (IntegerVariable v: newVars) {
			if (v.isDigit())
				csp.add(v);
		}

		for (Clause cls : csp.getClauses()) {
			if (cls.getArithmeticLiterals().size() == 0) {
				newClauses.add(cls);
			} else {
				assert cls.getArithmeticLiterals().size() == 1;
				assert cls.getArithmeticLiterals().get(0) instanceof RCSPLiteral;

				RCSPLiteral ll = (RCSPLiteral)cls.getArithmeticLiterals().get(0);
				List<BooleanLiteral> bls = cls.getBooleanLiterals();
				List<Clause> ccspClss = ll.toCCSP(csp, this);
				for (Clause c : ccspClss) {
					c.addAll(bls);
				}
				newClauses.addAll(ccspClss);
			}
		}
		csp.setClauses(newClauses);
	}

	private LinearSum simplifyLinearExpression(LinearSum e, List<Clause> clss) throws SugarException {
		if (e.size() <= 3) {
			return e;
		}
		int b = e.getB();
		LinearSum[] es = e.split(2);
		e = new LinearSum(b);
		for (int i = 0; i < es.length; i++) {
			LinearSum ei = es[i];
			int factor = ei.factor();
			if (factor > 1) {
				ei.divide(factor);
			}
			ei = simplifyLinearExpression(ei, clss);
			// System.out.println(es[i] + " ==> " + ei);
			if (ei.size() > 1) {
				IntegerVariable v = new IntegerVariable(ei.getDomain());
				v.setComment(v.getName() + " : " + ei);
				csp.add(v);
				clss.addAll(adjust(v, false));
				// v == ei
				LinearSum ls = new LinearSum(v);
				ls.subtract(ei);
				LinearLiteral ll = new LinearLiteral(ls, Operator.EQ);
				Clause cls = new Clause(ll);
				cls.setComment(v.getName() + " == " + ei);
				clss.add(cls);
				ei = new LinearSum(v);
			}
			if (factor > 1) {
				ei.multiply(factor);
			}
			e.add(ei);
		}
		return e;
	}

	private LinearSum simplifyForRCSP(LinearSum e, List<Clause> clss,
																		int maxlen) throws SugarException {

		int esize = e.size() + (e.getB() == 0 ? 0 : 1);

		if (esize <= maxlen) {
			return e;
		}
		assert (esize == 4 && maxlen == 2)
			|| (esize == 3 && maxlen == 2)
			|| (esize == 2 && maxlen == 1);

		List<IntegerHolder> holders = getHolders(e);
		assert holders.size() <= 4;
		Collections.sort(holders);

		IntegerHolder v0 = holders.get(0);
		IntegerHolder v1 = holders.get(1);
		IntegerVariable w0 = add(v0, v1);
		Literal lit0 = new OpAdd(Operator.EQ, new IntegerHolder(w0),
														 v0, v1);
		Clause cls0 = new Clause(lit0);
		cls0.setComment(w0.getName() + " == " + v0 + " + " + v1);
		clss.add(cls0);

		if (holders.size() == 2) {
			return new LinearSum(w0);
		} else if (holders.size() == 3) {
			LinearSum ret = new LinearSum(w0);
			IntegerHolder v2 = holders.get(2);
			if (v2.isConstant()) {
				ret.setB(v2.getValue());
			} else {
				ret.setA(1, v2.getVariable());
			}
			return ret;
		} else {
			assert holders.size() == 4;

			IntegerHolder v2 = holders.get(2);
			IntegerHolder v3 = holders.get(3);
			IntegerVariable w1 = add(v2, v3);
			Literal lit1 = new OpAdd(Operator.EQ, new IntegerHolder(w1),
															 v2, v3);
			Clause cls1 = new Clause(lit1);
			cls1.setComment(w1.getName() + " == " + v2 + " + " + v3);
			clss.add(cls1);

			LinearSum ret = new LinearSum(w0);
			ret.setA(1, w1);
			return ret;
		}
	}

	private IntegerVariable add(IntegerHolder x, IntegerHolder y)
	throws SugarException{
		IntegerDomain dom = x.getDomain().add(y.getDomain());
		// 中間変数なので，下限は無視しても ok
		IntegerVariable v = new IntegerVariable(dom);
		csp.add(v);
		return v;
	}

	private IntegerHolder getHolder(LinearSum e) {
		assert (e.size() == 1 && e.getB() == 0)
			|| (e.size() == 0 && e.getB() >= 0);
		if (e.size() == 1) {
			return new IntegerHolder(e.getCoef().firstKey());
		} else {
			return new IntegerHolder(e.getB());
		}
	}

	private List<IntegerHolder> getHolders(LinearSum e) {
		List<IntegerHolder> ret = new ArrayList<IntegerHolder>();
		for (IntegerVariable v : e.getVariables()) {
			ret.add(new IntegerHolder(v));
		}
		if (e.size() == 0 || e.getB() > 0) {
			ret.add(new IntegerHolder(e.getB()));
		}
		return ret;
	}

	@Override
	public int getSatVariablesSize(IntegerVariable v) {
		if (v.getDigits().length >= 2)
			return 0;
		return super.getSatVariablesSize(v);
	}
}
