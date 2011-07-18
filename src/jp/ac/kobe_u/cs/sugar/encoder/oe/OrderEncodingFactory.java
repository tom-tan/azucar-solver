package jp.ac.kobe_u.cs.sugar.encoder.oe;

import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.encoder.EncodingFactory;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;
import jp.ac.kobe_u.cs.sugar.encoder.Decoder;
import jp.ac.kobe_u.cs.sugar.encoder.Simplifier;

public class OrderEncodingFactory extends EncodingFactory {
	static EncodingFactory ef;
	private Encoder encoder;
	private Decoder decoder;
	private Simplifier simplifier;

	public static EncodingFactory getInstance() {
		if (ef == null) {
			ef = new OrderEncodingFactory();
		}
		return ef;
	}

	@Override
	public Encoder createEncoder(CSP csp) {
		if (encoder == null) {
			encoder = new OEEncoder(csp);
		}
		return encoder;
	}

	@Override
	public Decoder createDecoder(CSP csp) {
		if (decoder == null) {
			decoder = new OEDecoder(csp);
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
