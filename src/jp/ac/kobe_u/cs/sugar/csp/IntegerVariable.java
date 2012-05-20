package jp.ac.kobe_u.cs.sugar.csp;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;

import jp.ac.kobe_u.cs.sugar.SugarException;

/**
 * This class implements an integer variable of CSP.
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class IntegerVariable implements Comparable<IntegerVariable> {
	private static final String AUX_PRE = "_$I";
	private static String AUX_NAME_PREFIX = AUX_PRE;
	private static BigInteger auxIntegerVariablesSize = BigInteger.ZERO;
	private String name;
	private IntegerDomain domain;
	private boolean aux;
	private String comment = null;
	private boolean modified = true;
	private BigInteger code;
	private BigInteger value;
	private BigInteger offset = BigInteger.ZERO;
	private boolean isDigit_;
	private IntegerVariable[] vs = null;

	public static void setPrefix(String pre) {
		AUX_NAME_PREFIX = AUX_PRE + pre;
	}

	public static void setIndex(BigInteger index) {
		auxIntegerVariablesSize = index;
	}

	public IntegerVariable(String name, IntegerDomain domain) throws SugarException {
		this.name = name;
		this.domain = domain;
		if (domain.isEmpty()) {
			throw new SugarException("Integer variable domain error " + name);
		}
		value = domain.getLowerBound();
		aux = false;
	}

	public IntegerVariable(IntegerDomain domain) throws SugarException {
		this(AUX_NAME_PREFIX + (auxIntegerVariablesSize.add(BigInteger.ONE)), domain);
		auxIntegerVariablesSize = auxIntegerVariablesSize.add(BigInteger.ONE);
		aux = true;
	}

	public IntegerVariable(String name) {
		this.name = name;
	}

	/**
	 * Returns the name of the integer variable. 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	public IntegerDomain getDomain() {
		return domain;
	}

	public void setDomain(IntegerDomain d) {
		domain = d;
	}

	/**
	 * Returns true when the integer variable is aux.
	 * @return true when the integer variable is aux
	 */
	public boolean isAux() {
		return aux;
	}

	public void isAux(boolean aux) {
		this.aux = aux;
	}

	public BigInteger getOffset() {
		return offset;
	}

	public void setOffset(BigInteger off) {
		offset = off;
	}

	/**
	 * Returns the comment set to the integer variable.
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Sets the comment to the integer variable.
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return the modified
	 */
	public boolean isModified() {
		return modified;
	}

	/**
	 * @param modified the modified to set
	 */
	public void setModified(boolean modified) {
		this.modified = modified;
	}

	public long bound(BigInteger lb, BigInteger ub) throws SugarException {
		IntegerDomain oldDomain = domain;
		domain = domain.bound(lb, ub);
		if (! domain.equals(oldDomain)) {
			modified = true;
		}
		return oldDomain.size().subtract(domain.size()).intValue();
	}

	/**
	 * Returns the code value in the encoded representation. 
	 * @return the code value in the encoded representation
	 */
	public BigInteger getCode() {
		return code;
	}

	/**
	 * Sets the code value in the encoded representation. 
	 * @param code the code value
	 */
	public void setCode(BigInteger code) {
		this.code = code;
	}

	/**
	 * Returns the value of the integer variable.
	 * @return the value
	 */
	public BigInteger getValue() {
		return value;
	}

	/**
	 * Sets the value of the integer variable.
	 * @param value the value to set
	 */
	public void setValue(BigInteger value) {
		this.value = value;
	}

	public boolean isUnsatisfiable() {
		return domain.isEmpty();
	}

	public IntegerVariable[] getDigits() {
		if (vs == null) {
			vs = new IntegerVariable[1];
			vs[0] = this;
		}
		return vs;
	}

	public void setDigits(IntegerVariable[] ds) {
		vs = ds;
	}

	public boolean isDigit() {
		return isDigit_;
	}

	public void isDigit(boolean b) {
		isDigit_ = b;
	}

	public List<IntegerVariable> splitToDigits(CSP csp) throws SugarException {
		BigInteger ub = domain.getUpperBound();
		int b = csp.getBases()[0];
		int m = ub.toString(b).length();

		vs = new IntegerVariable[m];
		if (m == 1) {
			vs[0] = this;
		} else {
			for (int i=0; i<m; i++, ub = ub.divide(new BigInteger(Integer.toString(b)))) {
				assert ub.compareTo(BigInteger.ZERO) > 0;
				BigInteger ubi = (i == m-1) ? ub : new BigInteger(Integer.toString(b-1));
				IntegerDomain dom = new IntegerDomain(BigInteger.ZERO, ubi);
				vs[i] = new IntegerVariable(dom);
				vs[i].isDigit(true);
				vs[i].setComment(getName() + "["+i+"]");
				if (this.isAux())
					vs[i].isAux(true);
			}
		}
		List<IntegerVariable> ret = new ArrayList<IntegerVariable>();
		for (IntegerVariable v: vs) {
			ret.add(v);
		}
		return ret;
	}

	@Override
	public int compareTo(IntegerVariable v) {
		if (this == v)
			return 0;
		if (v == null)
			return 1;
		return name.compareTo(v.name);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final IntegerVariable other = (IntegerVariable) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/**
	 * Returns the string representation of the integer variable.
	 * @return the string representation
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(int ");
		sb.append(name);
		sb.append(" ");
		sb.append(domain.toString());
		sb.append(")");
		return sb.toString();
	}
}
