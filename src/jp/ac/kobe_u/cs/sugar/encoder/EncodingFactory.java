package jp.ac.kobe_u.cs.sugar.encoder;

import java.util.List;

import jp.ac.kobe_u.cs.sugar.csp.CSP;

public abstract class EncodingFactory {
	static protected EncodingFactory ef;
	protected Encoder encoder;
	protected Decoder decoder;
	protected int ndigits;
	protected int[] bases;

	public abstract Encoder createEncoder(CSP csp);

	public abstract Decoder createDecoder(CSP csp);

	public void setNDigits(int ndigits) {
		this.ndigits = ndigits;
	}

	public void setBases(int[] bases) {
		this.bases = bases;
	}
}
