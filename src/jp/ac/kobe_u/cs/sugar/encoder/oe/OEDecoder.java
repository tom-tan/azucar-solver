package jp.ac.kobe_u.cs.sugar.encoder.oe;

import java.util.BitSet;

import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.encoder.Decoder;

public class OEDecoder extends Decoder {
	public OEDecoder(CSP csp) {
		super(csp);
	}

	@Override
	public void decode(IntegerVariable v, BitSet satValues) {
		assert(v.getDigits() == null);
		assert(!v.isDigit());
		IntegerDomain domain = v.getDomain();
		int lb = domain.getLowerBound();
		int ub = domain.getUpperBound();
		int code = v.getCode();
		v.setValue(ub);
		for (int c = lb; c < ub; c++) {
			if (domain.contains(c)) {
				if (satValues.get(code)) {
					v.setValue(c);
					break;
				}
				code++;
			}
		}
		v.setValue(v.getValue()+v.getOffset());
	}
}
