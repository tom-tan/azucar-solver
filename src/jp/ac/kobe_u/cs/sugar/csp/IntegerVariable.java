package jp.ac.kobe_u.cs.sugar.csp;

import java.io.IOException;
import java.util.BitSet;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.SugarException;


/**
 * This class implements an integer variable of CSP.
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class IntegerVariable implements Comparable<IntegerVariable> {
  private static final String AUX_PRE = "_$I";
	private static String AUX_NAME_PREFIX = AUX_PRE;
	private static int auxIntegerVariablesSize = 0;
	private String name;
	private IntegerDomain domain;
	private boolean aux;
	private String comment = null;
	private boolean modified = true;
	private int code;
	private int value;
	private int offset;
  private boolean isDigit_;
	private List<IntegerVariable> vs = null;

  public static void setPrefix(String pre) {
    AUX_NAME_PREFIX = AUX_PRE + pre;
  }

  public static void setIndex(int index) {
    auxIntegerVariablesSize = 0;
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
		this(AUX_NAME_PREFIX + (++auxIntegerVariablesSize), domain);
		aux = true;
	}

  public IntegerVariable(String name, List<IntegerVariable> digits) {
    this.name = name;
    vs = digits;
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

	public int getOffset() {
		return offset;
	}

	public void setOffset(int off) {
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

	public int bound(int lb, int ub) throws SugarException {
		IntegerDomain oldDomain = domain;
		domain = domain.bound(lb, ub);
		if (! domain.equals(oldDomain)) {
			modified = true;
		}
		return oldDomain.size() - domain.size();
	}

	/**
	 * Returns the code value in the encoded representation. 
	 * @return the code value in the encoded representation
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Sets the code value in the encoded representation. 
	 * @param code the code value
	 */
	public void setCode(int code) {
		this.code = code;
	}

	/**
	 * Returns the value of the integer variable.
	 * @return the value
	 */
	public int getValue() {
    // 値はただ1つに決まっている
    assert(domain.size() == 1);
		return value;
	}

	/**
	 * Sets the value of the integer variable.
	 * @param value the value to set
	 */
	public void setValue(int value) {
		this.value = value;
	}

	public boolean isUnsatisfiable() {
		return domain.isEmpty();
	}

  public List<IntegerVariable> getDigits() {
    return vs;
  }

  public void setDigits(List<IntegerVariable> ds) {
    vs = ds;
  }

  public boolean isDigit() {
    return isDigit_;
  }

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
