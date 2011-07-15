package jp.ac.kobe_u.cs.sugar.coe;

import jp.ac.kobe_u.cs.sugar.encoder.AbstractEncoder;
import jp.ac.kobe_u.cs.sugar.encoder.Adjuster;
import jp.ac.kobe_u.cs.sugar.encoder.oe.Encoder;

public class Encoder extends AbstractEncoder {
	private oe.Encoder encoder;

	public Encoder(CSP csp) {
		super(csp);
		encoder = new oe.Encoder(csp);
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
	public void decode(IntegerVariable v, BitSet satValues) {
		encoder.decode(v, satValues);
	}

	@Override
	public void reduce() throws SugarException {
		csp = Adjuster.adjust(csp);
		toTernary();
		toRCSP();
		toCCSP();
	}

	private void toTernary() {
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
				// split する．
			}
		}
		csp.setClauses(newClauses);
	}

	private void toRCSP() {
	}

	private void toCCSP() {
	}
}