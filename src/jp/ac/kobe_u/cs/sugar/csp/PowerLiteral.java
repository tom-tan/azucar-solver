package jp.ac.kobe_u.cs.sugar.csp;

import java.io.IOException;
import java.util.Set;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;

/**
 * NOT IMPLEMENTED YET.
 * This class implements a literal for arithmetic power.
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class PowerLiteral extends Literal {

	@Override
	public Set<IntegerVariable> getVariables() {
		// TODO
		return null;
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public boolean isValid() throws SugarException {
		return false;
	}

	@Override
	public boolean isUnsatisfiable() throws SugarException {
		return false;
	}
	
	@Override
	public int propagate() {
		// TODO propagate
		return 0;
	}

	@Override
	public boolean isSatisfied() {
		// TODO isSatisfied
		return false;
	}

	@Override
	public void encode(Encoder encoder, int[] clause) throws SugarException, IOException {
		// TODO encode
	}

	@Override
	public String toString() {
		// TODO toString
		String s = "(power)";
		return s;
	}

}
