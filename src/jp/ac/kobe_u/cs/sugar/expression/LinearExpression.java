package jp.ac.kobe_u.cs.sugar.expression;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.SugarConstants;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.expression.Expression;

public class LinearExpression extends Expression {
	private int b;
	private SortedMap<Atom,Integer> coef;
	private IntegerDomain domain = null;

	public LinearExpression(int b) {
		coef = new TreeMap<Atom,Integer>();
		this.b = b;
	}

	public LinearExpression(int a0, Atom v0, int b) {
		this(b);
		coef.put(v0, a0);
	}

	public LinearExpression(Atom v0) {
		this(1, v0, 0);
	}

	public LinearExpression(LinearExpression e) {
		b = e.b;
		coef = new TreeMap<Atom,Integer>(e.coef);
		domain = null;
	}

	/**
	 * Returns the size of the linear expression. 
	 * @return the size
	 */
	public int size() {
		return coef.size();
	}
	
	public int getB() {
		return b;
	}

	public void setB(int b) {
		this.b = b;
	}
	
	public SortedMap<Atom,Integer> getCoef() {
		return coef;
	}

	public Set<Atom> getVariables() {
		return coef.keySet();
	}

	public boolean isIntegerVariable() {
		return b == 0 && size() == 1 && getA(coef.firstKey()) == 1;
	}
	
	public Integer getA(Atom v) {
		Integer a = coef.get(v);
		if (a == null) {
			a = 0;
		}
		return a;
	}
	
	public void setA(int a, Atom v) {
		if (a == 0) {
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
		b += linearExpression.b;
		for (Atom v : linearExpression.coef.keySet()) {
			int a = getA(v) + linearExpression.getA(v);
			setA(a, v);
		}
		domain = null;
	}

	/**
	 * Subtracts the given linear expression.
	 * @param linearSum the linear expression to be subtracted.
	 */
	public void subtract(LinearExpression linearExpression) {
		b -= linearExpression.b;
		for (Atom v : linearExpression.coef.keySet()) {
			int a = getA(v) - linearExpression.getA(v);
			setA(a, v);
		}
		domain = null;
	}
  
	/**
	 * Multiplies the given constant.
	 * @param c the constant to be multiplied by
	 */
	public void multiply(int c) {
		b *= c;
		for (Atom v : coef.keySet()) {
			int a = c * getA(v);
			setA(a, v);
		}
		domain = null;
	}

	public void divide(int c) {
		b /= c;
		for (Atom v : coef.keySet()) {
			int a = getA(v) / c;
			setA(a, v);
		}
		domain = null;
	}

	public IntegerDomain getDomain(Map<String, IntegerDomain> m) throws SugarException {
		if (domain == null) {
			domain = new IntegerDomain(b, b);
			for (Atom v : coef.keySet()) {
				int a = getA(v);
				domain = domain.add(m.get(v.toString()).mul(a));
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

	@Override
	public int compareTo(Expression x) {
		if (x == null)
			return 1;
		if (x instanceof Atom)
			return 1;
		if (x instanceof Sequence)
			return -1;
		if (this.equals(x))
			return 0;
		LinearExpression another = (LinearExpression)x;
		if (coef.size() < another.coef.size())
			return -1;
		if (coef.size() > another.coef.size())
			return 1;
		Iterator<Atom> it1 = coef.keySet().iterator();
		Iterator<Atom> it2 = coef.keySet().iterator();
		while(it1.hasNext()) {
			assert(it2.hasNext());
			Atom v1 = it1.next();
			Atom v2 = it2.next();
			int cv = v1.compareTo(v2);
			if (cv != 0)
				return cv;
			int ca = getA(v1).compareTo(another.getA(v2));
			if (ca != 0)
				return ca;
		}
		return ((Integer)b).compareTo(another.b);
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
		result = PRIME * result + b;
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
    sb.append("(add ");
		for (Atom v : coef.keySet()) {
			int c = getA(v);
			if (c == 0) {
			}else if(c == 1) {
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
