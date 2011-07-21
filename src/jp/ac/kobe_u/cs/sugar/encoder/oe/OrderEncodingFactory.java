package jp.ac.kobe_u.cs.sugar.encoder.oe;

import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.encoder.EncodingFactory;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;
import jp.ac.kobe_u.cs.sugar.encoder.Decoder;

public class OrderEncodingFactory extends EncodingFactory {
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
}
