package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.math.BigInteger;
import java.util.BitSet;

import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.encoder.Decoder;
import jp.ac.kobe_u.cs.sugar.encoder.oe.OEDecoder;

public class COEDecoder extends OEDecoder {
	private int[] bases;

	public COEDecoder(CSP csp, int[] bases) {
		super(csp);
		this.bases = bases;
	}

	@Override
	public void decodeBigInteger(IntegerVariable v) {
		assert v.getDigits().length > 1;
		final int b = bases[0];
		BigInteger dbase = BigInteger.ONE;
		BigInteger value = BigInteger.ZERO;
		for (IntegerVariable digit: v.getDigits()) {
			value = value.add(dbase.multiply(digit.getValue()));
			dbase = dbase.multiply(new BigInteger(Integer.toString(b)));
		}
		v.setValue(value.add(v.getOffset()));
	}
}
