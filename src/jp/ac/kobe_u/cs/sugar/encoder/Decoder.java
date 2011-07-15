package jp.ac.kobe_u.cs.sugar.encoder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StreamTokenizer;
import java.io.IOException;
import java.util.BitSet;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;

public abstract class Decoder {
	protected CSP csp;

	public Decoder(CSP csp) {
		this.csp = csp;
	}

	public abstract void decode(IntegerVariable v, BitSet satValues);

	protected void decode(BooleanVariable v, BitSet satValues) {
		v.setValue(satValues.get(v.getCode()));
	}

	public boolean decode(String outFileName) throws SugarException, IOException {
		String result = null;
		boolean sat = false;
		BufferedReader rd = new BufferedReader(new FileReader(outFileName));
		StreamTokenizer st = new StreamTokenizer(rd);
		st.eolIsSignificant(true);
		while (result == null) {
			st.nextToken();
			if (st.ttype == StreamTokenizer.TT_WORD) {
				if (st.sval.equals("c")) {
					do {
						st.nextToken();
					} while (st.ttype != StreamTokenizer.TT_EOL);
				} else if (st.sval.equals("s")) {
					st.nextToken();
					result = st.sval;
				} else {
					result = st.sval;
				}
			} else {
				throw new SugarException("Unknown output " + st.sval);
			}
		}
		if (result.startsWith("SAT")) {
			sat = true;
			BitSet satValues = new BitSet();
			while (true) {
				st.nextToken();
				if (st.ttype == StreamTokenizer.TT_EOF)
					break;
				switch (st.ttype) {
				case StreamTokenizer.TT_EOL:
					break;
				case StreamTokenizer.TT_WORD:
					if (st.sval.equals("v")) {
					} else if (st.sval.equals("c")) {
						do {
							st.nextToken();
						} while (st.ttype != StreamTokenizer.TT_EOL);
					} else {
						throw new SugarException("Unknown output " + st.sval);
					}
					break;
				case StreamTokenizer.TT_NUMBER:
					int value = (int)st.nval;
					int i = Math.abs(value);
					if (i > 0) {
						satValues.set(i, value > 0);
					}
					break;
				default:
					throw new SugarException("Unknown output " + st.sval);
				}
			}
			for (IntegerVariable v : csp.getIntegerVariables()) {
				decode(v, satValues);
			}
			for (BooleanVariable v : csp.getBooleanVariables()) {
				decode(v, satValues);
			}
		} else if (result.startsWith("UNSAT")) {
			sat = false;
		} else {
			throw new SugarException("Unknown output result " + result);
		}
		rd.close();
		return sat;
	}


}