package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.util.BitSet;

import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.encoder.Decoder;
import jp.ac.kobe_u.cs.sugar.encoder.oe.OEDecoder;

public class COEDecoder extends Decoder {
	private Decoder decoder;

	public COEDecoder(CSP csp) {
		super(csp);
		decoder = new OEDecoder(csp);
	}

	@Override
	public void decode(IntegerVariable v, BitSet satValues) {
		if (v.getDigits() == null) {
			decoder.decode(v, satValues);
		} else {
			// ここがメイン
		}
	}
}
