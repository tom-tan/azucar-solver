package jp.ac.kobe_u.cs.sugar.csp;

import jp.ac.kobe_u.cs.sugar.SugarException;

/**
 * This class implements a boolean variable of CSP.
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class BooleanVariable implements Comparable<BooleanVariable> {
	private static final String AUX_PRE = "_$B";
	private static String AUX_NAME_PREFIX = AUX_PRE;
	private static long auxBooleanVariablesSize = 0;
	private String name;
	private boolean aux;
	private String comment = null;
	private long code;
	private boolean value;

	public static void setPrefix(String pre) {
		AUX_NAME_PREFIX = AUX_PRE + pre;
	}

	public static void setIndex(long index) {
		auxBooleanVariablesSize = 0;
	}

	/**
	 * Adds a new boolean variable with give name.
	 * @param name the name of the boolean variable
	 * @throws SugarException when the name is duplicated
	 */
	public BooleanVariable(String name) throws SugarException {
		this.name = name;
		aux = false;
	}

	public BooleanVariable() throws SugarException {
		this(AUX_NAME_PREFIX + (++auxBooleanVariablesSize));
		aux = true;
	}

	/**
	 * Returns the name of the boolean variable.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns true when the boolean variable is aux.
	 * @return true when the boolean variable is aux
	 */
	public boolean isAux() {
		return aux;
	}

	public void isAux(boolean aux) {
		this.aux = aux;
	}

	/**
	 * Returns the comment set to the boolean variable.
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Sets the comment to the boolean variable.
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * Returns the code value in the encoded representation. 
	 * @return the code value in the encoded representation
	 */
	public long getCode() {
		return code;
	}

	/**
	 * Sets the code value in the encoded representation. 
	 * @param code the code value
	 */
	public void setCode(long code) {
		this.code = code;
	}

	/**
	 * Returns the value of the boolean variable.
	 * @return the value
	 */
	public boolean getValue() {
		return value;
	}

	/**
	 * Sets the value of the boolean variable.
	 * @param value the value to set
	 */
	public void setValue(boolean value) {
		this.value = value;
	}

	public int compareTo(BooleanVariable v) {
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
		final BooleanVariable other = (BooleanVariable) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/**
	 * Returns the string representation of the boolean variable.
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return "(bool " + name + ")";
	}
}
