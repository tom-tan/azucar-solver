package jp.ac.kobe_u.cs.sugar.encoder.coe;

import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;

public class IntegerHolder {
	int val;
	IntegerVariable var;

	public IntegerHolder(int v) {
		val = v;
	}

	public IntegerHolder(IntegerVariable v) {
		var = v;
	}
}
