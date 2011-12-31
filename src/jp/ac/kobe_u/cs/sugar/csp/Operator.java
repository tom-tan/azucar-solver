package jp.ac.kobe_u.cs.sugar.csp;

public enum Operator{
	LE, GE, EQ, NE;
	@Override
	public String toString() {
		switch(this) {
		case LE: return "le";
		case GE: return "ge";
		case EQ: return "eq";
		case NE: return "ne";
		}
		assert false;
		return "";
	}
}
