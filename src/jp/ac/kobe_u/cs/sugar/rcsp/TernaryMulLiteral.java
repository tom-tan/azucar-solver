package jp.ac.kobe_u.cs.sugar.rcsp;

import jp.ac.kobe_u.cs.sugar.csp.*;

class TernaryMulLiteral extends Literal{
  Atom x;
  Atom y;
  Atom z;
  String op;

  public TernaryMulLiteral(Atom x, Atom y, Atom z, String op) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.op = op;
  }

  public List<Clause> toCCSP(int m, int base,
			     CSP csp, List<Literal> literals) {
    throw new SugarException("Not implemented.");
    return null;
  }

  public String toString() {
    return "(" + x.toString()+"*"+ y.toString() + op
      z.toString() + ")";
  }
}