package jp.ac.kobe_u.cs.sugar.encoder.coe;

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
	public void decode(IntegerVariable v, BitSet satValues) {
		if (v.getDigits().length <= 1) {
			super.decode(v, satValues);
		} else {
			final int b = bases[0];
			int dbase = 1;
			int value = 0;
			for (IntegerVariable digit: v.getDigits()) {
				value += dbase*digit.getValue();
				dbase *= b;
			}
			v.setValue(value+v.getOffset());
		}
	}
}
