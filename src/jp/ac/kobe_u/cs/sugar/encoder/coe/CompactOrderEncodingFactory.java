package jp.ac.kobe_u.cs.sugar.encoder.coe;

import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.encoder.EncodingFactory;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;
import jp.ac.kobe_u.cs.sugar.encoder.Decoder;
import jp.ac.kobe_u.cs.sugar.encoder.Simplifier;

public class CompactOrderEncodingFactory implements EncodingFactory {
	static EncodingFactory ef;
	private Encoder encoder;
	private Decoder decoder;
	private Simplifier simplifier;

	public static EncodingFactory getInstance() {
		if (ef == null) {
			ef = new CompactOrderEncodingFactory();
		}
		return ef;
	}

	@Override
	public Encoder createEncoder(CSP csp) {
		if (encoder == null) {
			encoder = new COEEncoder(csp);
		}
		return encoder;
	}

	@Override
	public Decoder createDecoder(CSP csp) {
		if (decoder == null) {
			decoder = new COEDecoder(csp);
		}
		return decoder;
	}

	@Override
	public Simplifier createSimplifier(CSP csp) {
		if (simplifier == null) {
			simplifier = new Simplifier(csp);
		}
		return simplifier;
	}
}
