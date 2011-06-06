package jp.ac.kobe_u.cs.sugar.rcsp;

import jp.ac.kobe_u.cs.sugar.csp.*;

class Atom{
  int value;
  IntegerVariable variable;
  boolean isConstant_;

  public boolean isVariable() {
    return true;
  }

  public boolean isConstant() {
    return true;
  }

  public int nthValue(int n, int B) {
    if(!isConstant_) throw new SugarException("Constant required.");
    return (value/Math.pow(B, n))%B;
  }

  public IntegerVariable nthVariable(int n) {
    if(isConstant_) throw new SugarException("Variable required.");
    return null;
  }

  public String toString() {
    return isConstant_ ? Integer.toString(value) :
      variable.toString();
  }
}