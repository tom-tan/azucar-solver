package jp.ac.kobe_u.cs.sugar.encoder.oe;

import java.util.HashMap;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.encoder.Decoder;

public class OEDecoder extends Decoder {
	public OEDecoder(CSP csp) {
		super(csp);
	}

	@Override
	public void decode(IntegerVariable v, HashMap<Long, Boolean> satValues) {
		assert v.getDigits().length <= 1;
		final IntegerDomain domain = v.getDomain();
		final long lb = domain.getLowerBound();
		final long ub = domain.getUpperBound();
		long code = v.getCode();
		v.setValue(ub);
		for (long c = lb; c < ub; c++) {
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

	@Override
	public void decodeBigInteger(IntegerVariable v) throws SugarException {
		throw new SugarException("Internal Error");
	}
}
