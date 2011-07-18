package jp.ac.kobe_u.cs.sugar.encoder.coe;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.Operator;

/**
 * z op x+y (op in {=, <=, >=, !=})
 */
public class OpAdd extends RCSPLiteral {
	private IntegerHolder z, x, y;
	private Operator op;

	public OpAdd(Operator op, IntegerHolder z, IntegerHolder x, IntegerHolder y) {
		this.z = z;
		this.x = x;
		this.y = y;
		this.op = op;
	}

	public OpAdd(Operator op, IntegerVariable z, int x, IntegerVariable y) {
		this(op, new IntegerHolder(z), new IntegerHolder(x),
				 new IntegerHolder(y));
	}

	public OpAdd(Operator op, IntegerVariable z,
							 IntegerHolder x, IntegerHolder y) {
		this(op, new IntegerHolder(z), x, y);
	}

	@Override
	public List<Clause> toCCSP(CSP csp) throws SugarException {
		int b = csp.getBases().get(0);
		List<Clause> ret = new ArrayList<Clause>();
		int m = Math.max(Math.max(x.nDigits(b), y.nDigits(b)),
										 z.nDigits(b));
		switch(op) {
		case LE:
			return ret;
		case EQ:
			for (int i=0; i<m; i++) {
				//hogehoge
			}
			return ret;
		case NE:
			return ret;
		default:
			throw new SugarException("Internal Error");
		}
	}

	@Override
	public String toString() {
		return "("+op+ "add " + z + " " + x + " " + y + ")";
	}
}