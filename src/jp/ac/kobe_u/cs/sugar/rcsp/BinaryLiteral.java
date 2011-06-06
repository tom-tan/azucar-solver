package jp.ac.kobe_u.cs.sugar.rcsp;

import jp.ac.kobe_u.cs.sugar.SugarConstants;
import jp.ac.kobe_u.cs.sugar.csp.*;

class BinaryLiteral extends Literal{
  Atom x;
  Atom y;
  String op;

  public BinaryLiteral(Atom x, Atom y, String op) {
    if(op == SugarConstants.GE) {
      this.x = y;
      this.y = x;
      this.op = SugarConstants.LE;
    }else{
      assert(op == SugarConstants.LE ||
             op == SugarConstants.EQ ||
             op == SugarConstants.NE);
      this.x = x;
      this.y = y;
      this.op = op;
    }
  }

  public List<Clause> toCCSP(int m, int base,
                             CSP csp, List<Literal> literals) {
		List<Clause> newClauses = new ArrayList<Clause>();

    // x <= a
    // x <= y
    // a <= x
    for(int i=0; i<m; i++) {
      
    }

    throw new SugarException("Not implemented.");
    return newClauses;
  }

  public String toString() {
    return "(" + x.toString() + " " + op + " "+ y.toString() + ")";
  }
}