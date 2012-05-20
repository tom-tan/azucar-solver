package jp.ac.kobe_u.cs.sugar.csp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jp.ac.kobe_u.cs.sugar.SugarException;

/**
 * This class implements a clause in CSP.
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class Clause {
	private List<BooleanLiteral> boolLiterals;
	private List<ArithmeticLiteral> arithLiterals;
	private Set<IntegerVariable> commonVariables = null;
	private String comment = null;

	/**
	 * Constructs a new clause with give literals.
	 * @param literals the literals of the clause
	 */
	public Clause(List<BooleanLiteral> literals) {
		this();
		boolLiterals.addAll(literals);
	}

	/**
	 * Constructs a new clause.
	 */
	public Clause() {
		boolLiterals = new ArrayList<BooleanLiteral>();
		arithLiterals = new ArrayList<ArithmeticLiteral>();
	}

	public Clause(Literal literal) {
		this();
		add(literal);
	}

	public List<BooleanLiteral> getBooleanLiterals() {
		return boolLiterals;
	}

	public List<ArithmeticLiteral> getArithmeticLiterals() {
		return arithLiterals;
	}

	/**
	 * Adds all given literals to the clause.
	 * @param literals the literals to be added
	 */
	public void addAll(List<Literal> literals) {
		for (Literal l : literals) {
			add(l);
		}
	}

	public void add(Literal literal) {
		if (literal instanceof BooleanLiteral)
			boolLiterals.add((BooleanLiteral)literal);
		else
			arithLiterals.add((ArithmeticLiteral)literal);
	}

	public int size() {
		return boolLiterals.size()+arithLiterals.size();
	}

	/**
	 * Returns the comment set to the clause.
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Sets the comment to the clause.
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean isModified() {
		for (ArithmeticLiteral lit : arithLiterals) {
			Set<IntegerVariable> vs = lit.getVariables();
			if (vs != null) {
				for (IntegerVariable v : vs) {
					if (v.isModified()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public Set<IntegerVariable> getCommonVariables() {
		if (commonVariables == null && size() > 0) {
			if (!boolLiterals.isEmpty()) {
				commonVariables = null;
			} else {
				for (ArithmeticLiteral lit : arithLiterals) {
					Set<IntegerVariable> vs = lit.getVariables();
					if (vs == null) {
						commonVariables = null;
						break;
					} else if (commonVariables == null) {
						commonVariables = vs;
					} else {
						Set<IntegerVariable> vars = new TreeSet<IntegerVariable>();
						for (IntegerVariable v : commonVariables) {
							if (vs.contains(v)) {
								vars.add(v);
							}
						}
						commonVariables = vars;
					}
				}
			}
			if (commonVariables == null) {
				commonVariables = new TreeSet<IntegerVariable>();
			}
		}
		return commonVariables;
	}

	public boolean isValid() throws SugarException {
		for (Literal lit : boolLiterals) {
			if (lit.isValid()) {
				return true;
			}
		}
		for (Literal lit : arithLiterals) {
			if (lit.isValid()) {
				return true;
			}
		}
		return false;
	}

	public boolean isUnsatisfiable() throws SugarException {
		for (Literal lit : boolLiterals) {
			if (! lit.isUnsatisfiable()) {
				return false;
			}
		}
		for (Literal lit : arithLiterals) {
			if (! lit.isUnsatisfiable()) {
				return false;
			}
		}
		return true;
	}

	public long propagate() throws SugarException {
		if (size() == 0)
			return 0;
		long count = 0;
		for (IntegerVariable v : getCommonVariables()) {
			assert boolLiterals.isEmpty();
			BigInteger[] bound = null;
			for (ArithmeticLiteral lit : arithLiterals) {
				BigInteger[] b = lit.getBound(v);
				if (b == null) {
					bound = null;
					break;
				} else {
					// System.out.println("Bound " + v + " " + b[0] + " " + b[1] + " by " + lit);
					if (bound == null) {
						bound = b;
					} else {
						bound[0] = bound[0].min(b[0]);
						bound[1] = bound[1].max(b[1]);
					}
				}
			}
			if (bound != null && bound[0].compareTo(bound[1]) <= 0) {
				// System.out.println("Bound " + v.getName() + " " + bound[0] + " " + bound[1]);
				count += v.bound(bound[0], bound[1]);
			}
		}
		return count;
	}

	public int removeFalsefood() throws SugarException {
		int count = 0;
		int i = 0;
		while (i < boolLiterals.size()) {
			if (boolLiterals.get(i).isUnsatisfiable()) {
				boolLiterals.remove(i);
				count++;
			} else {
				i++;
			}
		}
		while (i < arithLiterals.size()) {
			if (arithLiterals.get(i).isUnsatisfiable()) {
				arithLiterals.remove(i);
				count++;
			} else {
				i++;
			}
		}
		return count;
	}

	/**
	 * Returns the string representation of the clause.
	 * @return the string representation
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(or");
    for (Literal literal : boolLiterals) {
			sb.append(" ");
			sb.append(literal.toString());
    }
		for (Literal literal : arithLiterals) {
			sb.append(" ");
			sb.append(literal.toString());
		}
		sb.append(")");
		return sb.toString();
	}
}
