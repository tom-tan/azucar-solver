package jp.ac.kobe_u.cs.sugar.encoder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;

public abstract class Decoder {
	protected final CSP csp;

	public Decoder(CSP csp) {
		this.csp = csp;
	}

	public abstract void decode(IntegerVariable v, HashMap<BigInteger, Boolean> satValues);
	public abstract void decodeBigInteger(IntegerVariable v) throws SugarException;

	protected void decode(BooleanVariable v, HashMap<BigInteger, Boolean> satValues) {
		v.setValue(satValues.get(v.getCode()));
	}

	public boolean decode(String outFileName) throws SugarException, IOException {
		String result = null;
		boolean sat = false;
		final BufferedReader rd = new BufferedReader(new FileReader(outFileName));
		final StreamTokenizer st = new StreamTokenizer(rd);
		st.resetSyntax();
		st.whitespaceChars(0x0000, 0x0020);
		st.wordChars('A', 'Z');
		st.wordChars('a', 'z');
		st.wordChars('_', '_');
		st.wordChars('0', '9');
		char[] chars = {
			'+', '-', '*', '/', '%',
				'=', '<', '>', '!', '&', '|' };
		for (char c : chars) {
			st.wordChars(c, c);
		}
		st.wordChars('$', '$');
		st.wordChars(0x000080, 0x10FFFF);
		st.eolIsSignificant(false);

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
			final HashMap<BigInteger, Boolean> satValues = new HashMap<BigInteger, Boolean>();
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
					} else if (st.sval.matches("-??[0-9]+")) {
						final BigInteger value = new BigInteger(st.sval);
						final BigInteger i = value.abs();
						if (i.compareTo(BigInteger.ZERO) > 0) {
							satValues.put(i, value.compareTo(BigInteger.ZERO) > 0);
						}
						break;
					}
					else {
						throw new SugarException("Unknown output " + st.sval);
					}
					break;
				default:
					throw new SugarException("Unknown output " + st.sval);
				}
			}
			final List<IntegerVariable> bigints = new ArrayList<IntegerVariable>();
			for (IntegerVariable v : csp.getIntegerVariables()) {
				if (v.getDigits().length > 1) {
					bigints.add(v);
				} else {
					decode(v, satValues);
				}
			}
			for (IntegerVariable v : bigints) {
				decodeBigInteger(v);
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
