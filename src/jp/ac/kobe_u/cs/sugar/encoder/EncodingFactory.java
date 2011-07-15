package jp.ac.kobe_u.cs.sugar.encoder;

import jp.ac.kobe_u.cs.sugar.csp.CSP;

public interface EncodingFactory {
	public Encoder createEncoder(CSP csp);

	public Decoder createDecoder(CSP csp);

	public Simplifier createSimplifier(CSP csp);
}
