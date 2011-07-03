package jp.ac.kobe_u.cs.sugar.csp;

public enum Operator{
	LE, EQ, NE;
	@Override
	public String toString() {
		switch(this) {
		case LE: return "le";
		case EQ: return "eq";
		case NE: return "ne";
		}
		assert(false);
		return "";
	}
}
