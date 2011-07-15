package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.ArrayList;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.Operator;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.LinearLiteral;
import jp.ac.kobe_u.cs.sugar.csp.LinearSum;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;
import jp.ac.kobe_u.cs.sugar.encoder.Adjuster;
import jp.ac.kobe_u.cs.sugar.encoder.oe.OEEncoder;

public class COEEncoder extends Encoder {
	private Encoder encoder;

	public COEEncoder(CSP csp) {
		super(csp);
		encoder = new OEEncoder(csp);
	}

	@Override
	public int getCode(LinearLiteral lit) throws SugarException {
		return encoder.getCode(lit);
	}

	@Override
	public void encode(IntegerVariable v) throws SugarException, IOException {
		if (v.getDigits() == null) {
			encoder.encode(v);
		}
	}

	@Override
	public void encode(LinearLiteral lit, int[] clause) throws SugarException, IOException {
		encoder.encode(lit, clause);
	}

	@Override
	public int getSatVariablesSize(IntegerVariable v) {
		return encoder.getSatVariablesSize(v);
	}

	@Override
	public void reduce() throws SugarException {
		csp = Adjuster.adjust(csp);
		toTernary();
		toRCSP();
		toCCSP();
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
				assert(c.getArithmeticLiterals().size() == 1);
				LinearLiteral ll = c.getArithmeticLiterals().get(0);
				if (ll.getLinearExpression().size() <= 3) {
					newClauses.add(c);
					continue;
				}
				List<BooleanLiteral> bls = c.getBooleanLiterals();
				Clause cls = new Clause(bls);
				LinearSum ls = simplifyLinearExpression(ll.getLinearExpression(), newClauses);
				cls.add(new LinearLiteral(ls, ll.getOperator()));
				newClauses.add(cls);
			}
		}
		csp.setClauses(newClauses);
	}

	private void toRCSP() {
		final String AUX_PREFIX = "RR";
		BooleanVariable.setPrefix(AUX_PREFIX);
		BooleanVariable.setIndex(0);
		IntegerVariable.setPrefix(AUX_PREFIX);
		IntegerVariable.setIndex(0);

		List<Clause> newClauses = new ArrayList<Clause>();
		for (Clause c: csp.getClauses()) {
			if (c.getArithmeticLiterals().size() == 0) {
				newClauses.add(c);
			} else {
				assert(c.getArithmeticLiterals().size() == 1);
				LinearLiteral ll = c.getArithmeticLiterals().get(0);
				LinearSum ls = ll.getLinearExpression();
				for (Entry<IntegerVariable, Integer> es :
				       ls.getCoef().entrySet()) {
					if (Math.abs(es.getValue()) == 1) {
						continue;
					}
					int a = Math.abs(es.getValue());
					IntegerVariable v = es.getKey();
					IntegerDomain dom = v.getDomain().mul(a);
					IntegerVariable av = new IntegerVariable(dom);
					Literal lit = new MulLit(av, a, v);
					csp.add(new Clause(lit));
				} // ls を更新する必要あり
				// lhs と rhs に分ける．
				if (lhs > 2) {
					// introduce
				}
				if (rhs > 2) {
					// introduce
				}
				if (lhs == 2 && rhs == 2) {
					// reduce lhs
				}
				// lhs op rhs
				if (lhs == 1 && rhs == 1) {
					switch(op) {
					case EQ:
						lhs == rhs;
					case LE:
						lhs <= rhs;
					case NE:
						lhs != rhs;
					}
				} else if (lhs == 1 && rhs == 2) {
					//lhs op rhs
				} else if (lhs == 2 && rhs == 1) {
					// rhs op lhs
				}
				// y=x+a, y<=x+a, y>=x+a
				// z=x+y, z<=x+y, z>=x+y
			}
		}
		csp.setClauses(newClauses);
	}

	private void toCCSP() {
		final String AUX_PREFIX = "RC";
		BooleanVariable.setPrefix(AUX_PREFIX);
		BooleanVariable.setIndex(0);
		IntegerVariable.setPrefix(AUX_PREFIX);
		IntegerVariable.setIndex(0);
	}

	private LinearSum simplifyLinearExpression(LinearSum e, List<Clause> clss) throws SugarException {
		if (e.size() <= 3) {
			return e;
		}
		int b = e.getB();
		LinearSum[] es = e.split(3);
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
}