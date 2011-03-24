package jp.ac.kobe_u.cs.sugar.csp;

import java.io.IOException;
import java.util.BitSet;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;

/**
 * This class implements an integer variable of CSP.
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class IntegerVariable implements Comparable<IntegerVariable> {
	private static String AUX_NAME_PREFIX = "$I";
	private static int auxIntegerVariablesSize = 0;
	private String name;
	private IntegerDomain domain;
	private boolean aux;
	private String comment = null;
	private boolean modified = true;
	private int code;
	private int value;
	private int offset;
	private IntegerVariable[] vs = null;
	
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

	/**
	 * Returns true when the integer variable is aux.
	 * @return true when the integer variable is aux
	 */
	public boolean isAux() {
		return aux;
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

	/**
	 * Returns true when the value is within the bounds.
	 * @return true when the value is within the bounds
	 */
	public boolean isSatisfied() {
		return domain.contains(value);
	}

	public void compact(CSP csp) throws SugarException {
		if (! domain.isContiguous() || domain.size() <= Encoder.BASE) {
			return;
		}
		vs = new IntegerVariable[2];
		offset = domain.getLowerBound();
		int max = domain.getUpperBound() - offset;
		int ub0 = Encoder.BASE - 1;
		int ub1 = max / Encoder.BASE;
		vs[0] = new IntegerVariable(new IntegerDomain(0, ub0));
		csp.add(vs[0]);
		vs[1] = new IntegerVariable(new IntegerDomain(0, ub1));
		csp.add(vs[1]);
		int c = max % Encoder.BASE;
		if (c != Encoder.BASE - 1) {
			Clause clause = new Clause();
			clause.add(new LinearLiteral(
					new LinearSum(1, vs[1], -ub1+1)));
			clause.add(new LinearLiteral(
					new LinearSum(1, vs[0], -c)));
			csp.add(clause);
		}
	}
	
	public int getSatVariablesSize() {
		if (vs != null)
			return 0;
		return domain.size() - 1;
	}

	public int getCodeLE(int value) {
		if (value < domain.getLowerBound()) {
			return Encoder.FALSE_CODE;
		} else if (value >= domain.getUpperBound()) {
			return Encoder.TRUE_CODE;
		}
		return code + domain.sizeLE(value) - 1;
	}

	public int getCodeLE(int a, int b) {
		int code;
		if (a >= 0) {
//			int c = (int) Math.floor((double) b / a);
			int c;
			if (b >= 0) {
				c = b/a;
			} else {
				c = (b-a+1)/a;
			}
			code = getCodeLE(c);
		} else {
//			int c = (int) Math.ceil((double) b / a) - 1;
			int c;
			if (b >= 0) {
				c = b/a - 1;
			} else {
				c = (b+a+1)/a - 1;
			}
			code = Encoder.negateCode(getCodeLE(c));
		}
		return code;
	}

	public void encode(Encoder encoder) throws IOException {
		encoder.writeComment(toString());
		if (vs == null) {
			int[] clause = new int[2];
			int a0 = domain.getLowerBound();
			for (int a = a0 + 1; a <= domain.getUpperBound(); a++) {
				if (domain.contains(a)) {
					clause[0] = Encoder.negateCode(getCodeLE(a0));
					clause[1] = getCodeLE(a);
					encoder.writeClause(clause);
					a0 = a;
				}
			}
		} else {
			
		}
	}

	public void decode(BitSet satValues) {
		int lb = domain.getLowerBound();
		int ub = domain.getUpperBound();
		int code = getCode();
		value = ub;
		for (int c = lb; c < ub; c++) {
			if (domain.contains(c)) {
				if (satValues.get(code)) {
					value = c;
					break;
				}
				code++;
			}
		}
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
