package jp.ac.kobe_u.cs.sugar.csp;

import java.io.IOException;
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
	private long b;
	private SortedMap<IntegerVariable, Long> coef;
	private IntegerDomain domain = null;

	public LinearSum(long b) {
		coef = new TreeMap<IntegerVariable, Long>();
		this.b = b;
	}

	public LinearSum(long a0, IntegerVariable v0, long b) {
		this(b);
		coef.put(v0, a0);
	}

	public LinearSum(IntegerVariable v0) {
		this(1, v0, 0);
	}

	public LinearSum(LinearSum e) {
		b = e.b;
		coef = new TreeMap<IntegerVariable, Long>(e.coef);
		domain = null;
	}

	/**
	 * Returns the size of the linear expression. 
	 * @return the size
	 */
	public int size() {
		return coef.size();
	}

	public long getB() {
		return b;
	}

	public void setB(long b) {
		this.b = b;
	}

	public SortedMap<IntegerVariable,Long> getCoef() {
		return coef;
	}

	public Set<IntegerVariable> getVariables() {
		return coef.keySet();
	}

	public boolean isIntegerVariable() {
		return b == 0 && size() == 1 && getA(coef.firstKey()) == 1;
	}

	public Long getA(IntegerVariable v) {
		Long a = coef.get(v);
		if (a == null) {
			a = 0L;
		}
		return a;
	}

	public void setA(long a, IntegerVariable v) {
		if (a == 0) {
			coef.remove(v);
		} else {
			coef.put(v, a);
		}
		domain = null;
	}

	public boolean isDomainLargerThan(long limit) {
		long size = 1;
		for (IntegerVariable v : coef.keySet()) {
			size *= v.getDomain().size();
			if (size > limit)
				return true;
		}
		return false;
	}

	public boolean isDomainLargerThanExcept(long limit, IntegerVariable v) {
		long size = 1;
		for (IntegerVariable v0 : coef.keySet()) {
			if (v0.equals(v))
				continue;
			size *= v0.getDomain().size();
			if (size > limit)
				return true;
		}
		return false;
	}

	public boolean isDomainLargerThanExcept(long limit) {
		IntegerVariable v = getLargestDomainVariable();
		return isDomainLargerThanExcept(limit, v);
	}

	/**
	 * Adds the given linear expression.
	 * @param linearSum the linear expression to be added.
	 */
	public void add(LinearSum linearSum) {
		b += linearSum.b;
		for (IntegerVariable v : linearSum.coef.keySet()) {
			long a = getA(v) + linearSum.getA(v);
			setA(a, v);
		}
		domain = null;
	}

	/**
	 * Subtracts the given linear expression.
	 * @param linearSum the linear expression to be subtracted.
	 */
	public void subtract(LinearSum linearSum) {
		b -= linearSum.b;
		for (IntegerVariable v : linearSum.coef.keySet()) {
			long a = getA(v) - linearSum.getA(v);
			setA(a, v);
		}
		domain = null;
	}

	/**
	 * Multiplies the given constant.
	 * @param c the constant to be multiplied by
	 */
	public void multiply(long c) {
		b *= c;
		for (IntegerVariable v : coef.keySet()) {
			long a = c * getA(v);
			setA(a, v);
		}
		domain = null;
	}

	public void divide(long c) {
		b /= c;
		for (IntegerVariable v : coef.keySet()) {
			long a = getA(v) / c;
			setA(a, v);
		}
		domain = null;
	}

	private long gcd(long p, long q) {
		while (true) {
			long r = p % q;
			if (r == 0)
				break;
			p = q;
			q = r;
		}
		return q;
	}

	public long factor() {
		if (size() == 0) {
			return b == 0 ? 1 : Math.abs(b);
		}
		long gcd = Math.abs(getA(coef.firstKey()));
		for (IntegerVariable v : coef.keySet()) {
			gcd = gcd(gcd, Math.abs(getA(v)));
			if (gcd == 1)
				break;
		}
		if (b != 0) {
			gcd = gcd(gcd, Math.abs(b));
		}
		return gcd;
	}

	public void factorize() {
		long factor = factor();
		if (factor > 1) {
			divide(factor);
		}
	}

	public IntegerDomain getDomain() throws SugarException {
		if (domain == null) {
			domain = new IntegerDomain(b, b);
			for (IntegerVariable v : coef.keySet()) {
				long a = getA(v);
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
				long a = getA(v1);
				d = d.add(v1.getDomain().mul(a));
			}
		}
		return d;
	}

	public LinearSum[] split(int m) {
		LinearSum[] es = new LinearSum[m];
		for (int i = 0; i < m; i++) {
			es[i] = new LinearSum(0);
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
			if (var == null || var.getDomain().size() < v.getDomain().size()) {
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
				long s1 = v1.getDomain().size();
				long s2 = v2.getDomain().size();
				if (s1 != s2)
					return s1 < s2 ? -1 : 1;
				long a1 = Math.abs(getA(v1));
				long a2 = Math.abs(getA(v2));
				if (a1 != a2)
					return a1 > a2 ? -1 : 1;
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
		return b == linearSum.b && coef.equals(linearSum.coef);
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
		result = PRIME * result + (int)b;
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
			long c = getA(v);
			if (c == 0) {
			}else if(c == 1) {
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
