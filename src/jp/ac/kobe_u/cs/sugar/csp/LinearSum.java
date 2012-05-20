package jp.ac.kobe_u.cs.sugar.csp;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import jp.ac.kobe_u.cs.sugar.SugarConstants;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.expression.Expression;

/**
 * A class for linear expressions.
 * A linear expression represents the following formula:<br>
 * a0*x0+a1*x1+...+an*xn+b<br>
 * where ai's and b are integer constants and xi's are integer variables
 * of CSP.
 * @see CSP
 * @see LinearLiteral
 * @author Naoyuki Tamura
 */
public class LinearSum {
	private BigInteger b;
	private SortedMap<IntegerVariable, BigInteger> coef;
	private IntegerDomain domain = null;

	public LinearSum(BigInteger b) {
		coef = new TreeMap<IntegerVariable, BigInteger>();
		this.b = b;
	}

	public LinearSum(BigInteger a0, IntegerVariable v0, BigInteger b) {
		this(b);
		coef.put(v0, a0);
	}

	public LinearSum(IntegerVariable v0) {
		this(BigInteger.ONE, v0, BigInteger.ZERO);
	}

	public LinearSum(LinearSum e) {
		b = e.b;
		coef = new TreeMap<IntegerVariable, BigInteger>(e.coef);
		domain = null;
	}

	/**
	 * Returns the size of the linear expression. 
	 * @return the size
	 */
	public int size() {
		return coef.size();
	}

	public BigInteger getB() {
		return b;
	}

	public void setB(BigInteger b) {
		this.b = b;
	}

	public SortedMap<IntegerVariable,BigInteger> getCoef() {
		return coef;
	}

	public Set<IntegerVariable> getVariables() {
		return coef.keySet();
	}

	public boolean isIntegerVariable() {
		return b.compareTo(BigInteger .ZERO) == 0 && size() == 1 && getA(coef.firstKey()).compareTo(BigInteger.ONE) == 0;
	}

	public BigInteger getA(IntegerVariable v) {
		BigInteger a = coef.get(v);
		if (a == null) {
			a = BigInteger.ZERO;
		}
		return a;
	}

	public void setA(BigInteger a, IntegerVariable v) {
		if (a.compareTo(BigInteger.ZERO) == 0) {
			coef.remove(v);
		} else {
			coef.put(v, a);
		}
		domain = null;
	}

	public boolean isDomainLargerThan(BigInteger limit) {
		BigInteger size = BigInteger.ONE;
		for (IntegerVariable v : coef.keySet()) {
			size = size.multiply(v.getDomain().size());
			if (size.compareTo(limit) > 0)
				return true;
		}
		return false;
	}

	public boolean isDomainLargerThanExcept(BigInteger limit, IntegerVariable v) {
		BigInteger size = BigInteger.ONE;
		for (IntegerVariable v0 : coef.keySet()) {
			if (v0.equals(v))
				continue;
			size = size.multiply(v0.getDomain().size());
			if (size.compareTo(limit) > 0)
				return true;
		}
		return false;
	}

	public boolean isDomainLargerThanExcept(BigInteger limit) {
		IntegerVariable v = getLargestDomainVariable();
		return isDomainLargerThanExcept(limit, v);
	}

	/**
	 * Adds the given linear expression.
	 * @param linearSum the linear expression to be added.
	 */
	public void add(LinearSum linearSum) {
		b = b.add(linearSum.b);
		for (IntegerVariable v : linearSum.coef.keySet()) {
			BigInteger a = getA(v).add(linearSum.getA(v));
			setA(a, v);
		}
		domain = null;
	}

	/**
	 * Subtracts the given linear expression.
	 * @param linearSum the linear expression to be subtracted.
	 */
	public void subtract(LinearSum linearSum) {
		b = b.subtract(linearSum.b);
		for (IntegerVariable v : linearSum.coef.keySet()) {
			BigInteger a = getA(v).subtract(linearSum.getA(v));
			setA(a, v);
		}
		domain = null;
	}

	/**
	 * Multiplies the given constant.
	 * @param c the constant to be multiplied by
	 */
	public void multiply(BigInteger c) {
		b = b.multiply(c);
		for (IntegerVariable v : coef.keySet()) {
			BigInteger a = c.multiply(getA(v));
			setA(a, v);
		}
		domain = null;
	}

	public void divide(BigInteger c) {
		b = b.divide(c);
		for (IntegerVariable v : coef.keySet()) {
			BigInteger a = getA(v).divide(c);
			setA(a, v);
		}
		domain = null;
	}

	// private long gcd(long p, long q) {
	// 	while (true) {
	// 		long r = p % q;
	// 		if (r == 0)
	// 			break;
	// 		p = q;
	// 		q = r;
	// 	}
	// 	return q;
	// }

	public BigInteger factor() {
		if (size() == 0) {
			return b.compareTo(BigInteger.ZERO) == 0 ? BigInteger.ONE : b.abs();
		}
		BigInteger gcd = getA(coef.firstKey()).abs();
		for (IntegerVariable v : coef.keySet()) {
			gcd = gcd.gcd(getA(v).abs());
			if (gcd.compareTo(BigInteger.ONE) == 0)
				break;
		}
		if (b.compareTo(BigInteger.ZERO) != 0) {
			gcd = gcd.gcd(b.abs());
		}
		return gcd;
	}

	public void factorize() {
		BigInteger factor = factor();
		if (factor.compareTo(BigInteger.ONE) > 0) {
			divide(factor);
		}
	}

	public IntegerDomain getDomain() throws SugarException {
		if (domain == null) {
			domain = new IntegerDomain(b, b);
			for (IntegerVariable v : coef.keySet()) {
				BigInteger a = getA(v);
				domain = domain.add(v.getDomain().mul(a));
			}
		}
		return domain;
	}

	public IntegerDomain getDomainExcept(IntegerVariable v) throws SugarException {
		// Re-calculation is needed since variable domains might be modified. 
		IntegerDomain d = new IntegerDomain(b, b);
		for (IntegerVariable v1 : coef.keySet()) {
			if (! v1.equals(v)) {
				BigInteger a = getA(v1);
				d = d.add(v1.getDomain().mul(a));
			}
		}
		return d;
	}

	public LinearSum[] split(int m) {
		LinearSum[] es = new LinearSum[m];
		for (int i = 0; i < m; i++) {
			es[i] = new LinearSum(BigInteger.ZERO);
		}
		IntegerVariable[] vs = getVariablesSorted();
		for (int i = 0; i < vs.length; i++) {
			IntegerVariable v = vs[i];
			es[i % m].setA(getA(v), v);
		}
		return es;
	}

	public IntegerVariable getLargestDomainVariable() {
		IntegerVariable var = null;
		for (IntegerVariable v : coef.keySet()) {
			if (var == null || var.getDomain().size().compareTo(v.getDomain().size()) < 0) {
				var = v;
			}
		}
		return var;
	}

	public IntegerVariable[] getVariablesSorted() {
		int n = coef.size();
		IntegerVariable[] vs = new IntegerVariable[n];
		vs = coef.keySet().toArray(vs);
		Arrays.sort(vs, new Comparator<IntegerVariable>() {
			public int compare(IntegerVariable v1, IntegerVariable v2) {
				BigInteger s1 = v1.getDomain().size();
				BigInteger s2 = v2.getDomain().size();
				if (s1.compareTo(s2) != 0)
					return s1.compareTo(s2) < 0 ? -1 : 1;
				BigInteger a1 = getA(v1).abs();
				BigInteger a2 = getA(v2).abs();
				if (a1.compareTo(a2) != 0)
					return a1.compareTo(a2) > 0 ? -1 : 1;
				return v1.compareTo(v2);
			}
		});
		return vs;
	}

	public Expression toExpression() {
		Expression x = Expression.create(b);
		for (IntegerVariable v : coef.keySet()) {
			Expression ax = Expression.create(getA(v));
			Expression vx = Expression.create(v.getName());
			x = x.add(ax.mul(vx));
		}
		return x;
	}

	/**
	 * Returns true when the linear expression is equal to the given linear expression.
	 * @param linearSum the linear expression to be compared
	 * @return true when the linear expression is equal to the given linear expression
	 */
	public boolean equals(LinearSum linearSum) {
		if (linearSum == null)
			return false;
		if (this == linearSum)
			return true;
		return b.compareTo(linearSum.b) == 0 && coef.equals(linearSum.coef);
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
		return equals((LinearSum)obj);
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
		result = PRIME * result + b.intValue();
		return result;
	}

	/**
	 * Returns the string representation of the linear expression.
	 * @return the string representation
	 */
	public String toString0() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(SugarConstants.ADD);
		for (IntegerVariable v : coef.keySet()) {
			sb.append(" (");
			sb.append(SugarConstants.MUL);
			sb.append(" ");
			sb.append(coef.get(v));
			sb.append(" ");
			sb.append(v.getName());
			sb.append(")");
		}
		sb.append(" ");
		sb.append(b);
		sb.append(")");
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(add ");
		for (IntegerVariable v : coef.keySet()) {
			BigInteger c = getA(v);
			if (c.compareTo(BigInteger.ZERO) == 0) {
			}else if(c.compareTo(BigInteger.ONE) == 0) {
				sb.append(v.getName());
			}else{
				sb.append("(mul ");
				sb.append(c);
				sb.append(" ");
				sb.append(v.getName());
				sb.append(")");
			}
			sb.append(" ");
		}
		sb.append(b);
		sb.append(")");
		return sb.toString();
	}
}
