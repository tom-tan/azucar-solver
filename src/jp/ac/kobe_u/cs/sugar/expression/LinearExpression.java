package jp.ac.kobe_u.cs.sugar.expression;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.SugarConstants;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;
import jp.ac.kobe_u.cs.sugar.expression.Expression;

public class LinearExpression {
	private Atom b;
	private SortedMap<Atom,Atom> coef;
	private IntegerDomain domain = null;

	public LinearExpression(Atom b) {
		coef = new TreeMap<Atom,Atom>();
		this.b = b;
	}

	public LinearExpression(Atom a0, Atom v0, Atom b) {
		this(b);
		coef.put(v0, a0);
	}

	public LinearExpression(Atom v0) {
		this((Atom)Expression.ONE, v0, (Atom)Expression.ZERO);
	}

	public LinearExpression(LinearExpression e) {
		b = e.b;
		coef = new TreeMap<Atom,Atom>(e.coef);
		domain = null;
	}

	/**
	 * Returns the size of the linear expression. 
	 * @return the size
	 */
	public int size() {
		return coef.size();
	}
	
	public Atom getB() {
		return b;
	}

	public void setB(Atom b) {
		this.b = b;
	}
	public void setB(int b) {
		this.b = (Atom)Expression.create(b);
	}
	
	public SortedMap<Atom,Atom> getCoef() {
		return coef;
	}

	public Set<Atom> getVariables() {
		return coef.keySet();
	}

	public boolean isIntegerVariable() {
		return b.integerValue() == 0 && size() == 1 && getA(coef.firstKey()).integerValue() == 1;  
	}
	
	public Atom getA(Atom v) {
		Atom a = coef.get(v);
		if (a == null) {
			a = (Atom)Expression.ZERO;
		}
		return (Atom)Expression.create(a);
	}
	
	public void setA(Atom a, Atom v) {
		if (a.integerValue() == 0) {
			coef.remove(v);
		} else {
			coef.put(v, a);
		}
		domain = null;
	}

	/**
	 * Adds the given linear expression.
	 * @param linearSum the linear expression to be added.
	 */
	public void add(LinearExpression linearExpression) {
		setB(b.integerValue() + linearExpression.b.integerValue());
		for (Atom v : linearExpression.coef.keySet()) {
			int a = getA(v).integerValue() + linearExpression.getA(v).integerValue();
			setA((Atom)Expression.create(a), v);
		}
		domain = null;
	}

	/**
	 * Subtracts the given linear expression.
	 * @param linearSum the linear expression to be subtracted.
	 */
	public void subtract(LinearExpression linearExpression) {
		setB(b.integerValue() - linearExpression.b.integerValue());
		for (Atom v : linearExpression.coef.keySet()) {
			int a = getA(v).integerValue() - linearExpression.getA(v).integerValue();
			setA((Atom)Expression.create(a), v);
		}
		domain = null;
	}
  
	/**
	 * Multiplies the given constant.
	 * @param c the constant to be multiplied by
	 */
	public void multiply(int c) {
		setB(b.integerValue() * c);
		for (Atom v : coef.keySet()) {
			int a = c * getA(v).integerValue();
			setA((Atom)Expression.create(a), v);
		}
		domain = null;
	}

	public void divide(int c) {
		setB(b.integerValue() / c);
		for (Atom v : coef.keySet()) {
			int a = getA(v).integerValue() / c;
			setA((Atom)Expression.create(a), v);
		}
		domain = null;
	}

	public IntegerDomain getDomain() throws SugarException {
		if (domain == null) {
			domain = new IntegerDomain(b.integerValue(), b.integerValue());
			for (Atom v : coef.keySet()) {
				int a = getA(v).integerValue();
				domain = domain.add(v.getDomain().mul(a));
			}
		}
		return domain;
	}
	
	/**
	 * Returns true when the linear expression is equal to the given linear expression.
	 * @param linearSum the linear expression to be compared
	 * @return true when the linear expression is equal to the given linear expression
	 */
	public boolean equals(LinearExpression linearExpression) {
		if (linearExpression == null)
			return false;
		if (this == linearExpression)
			return true;
		return b == linearExpression.b && coef.equals(linearExpression.coef);
	}
	
	/**
	 * Returns true when the linear expression is equal to the given object.
	 * @param obj the object to be compared
	 * @return true when the linear expression is equal to the given object.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return equals((LinearExpression)obj);
	}

	/**
	 * Returns the hash code of the linear expression.
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((coef == null) ? 0 : coef.hashCode());
		result = PRIME * result + b.integerValue();
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
    sb.append("(add ");
		for (Atom v : coef.keySet()) {
			Atom c = getA(v);
			if (c.integerValue() == 0) {
			}else if(c.integerValue() == 1) {
        sb.append(v.toString());
      }else{
        sb.append("(mul ");
        sb.append(c);
        sb.append(" ");
        sb.append(v.toString());
        sb.append(")");
      }
      sb.append(" ");
		}
    sb.append(b);
    sb.append(")");
		return sb.toString();
	}

}
