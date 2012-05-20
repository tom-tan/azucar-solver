package jp.ac.kobe_u.cs.sugar.encoder.oe;

import java.math.BigInteger;
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
	public void decode(IntegerVariable v, HashMap<BigInteger, Boolean> satValues) {
		assert v.getDigits().length <= 1;
		final IntegerDomain domain = v.getDomain();
		final BigInteger lb = domain.getLowerBound();
		final BigInteger ub = domain.getUpperBound();
		BigInteger code = v.getCode();
		v.setValue(ub);
		for (BigInteger c = lb; c.compareTo(ub) < 0; c = c.add(BigInteger.ONE)) {
			if (domain.contains(c)) {
				if (satValues.get(code)) {
					v.setValue(c);
					break;
				}
				code = code.add(BigInteger.ONE);
			}
		}
		v.setValue(v.getValue().add(v.getOffset()));
	}

	@Override
	public void decodeBigInteger(IntegerVariable v) throws SugarException {
		throw new SugarException("Internal Error");
	}
}
