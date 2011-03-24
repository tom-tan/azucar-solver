package jp.ac.kobe_u.cs.sugar.csp;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.Logger;
import jp.ac.kobe_u.cs.sugar.SugarException;

/**
 * A class for CSP (Constraint Satisfaction Problems).
 * A CSP consists of IntegerVariables, BooleanVariables, Clauses, and
 * an optional objective IntegerVariable with specific objective (MINIMIZE or MAXIMIZE).
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class CSP {
	public static boolean simplifyAll = true;
	
	private List<IntegerVariable> integerVariables;

	private List<BooleanVariable> booleanVariables;

	private List<Clause> clauses;

	private HashMap<String,IntegerVariable> integerVariableMap;

	private HashMap<String,BooleanVariable> booleanVariableMap;

	private IntegerVariable objectiveVariable = null;

	private Objective objective = Objective.NONE;

	/**
	 * Objective types.
	 */
	public enum Objective {
		/**
		 * No objectives 
		 */
		NONE,
		/**
		 * Maximize
		 */
		MAXIMIZE,
		/**
		 * Minimize 
		 */
		MINIMIZE
	}

	/**
	 * Constructs a new CSP.
	 */
	public CSP() {
		integerVariables = new ArrayList<IntegerVariable>();
		booleanVariables = new ArrayList<BooleanVariable>();
		clauses = new ArrayList<Clause>();
		objectiveVariable = null;
		objective = Objective.NONE;
		integerVariableMap = new HashMap<String,IntegerVariable>();
		booleanVariableMap = new HashMap<String,BooleanVariable>();
	}

	/**
	 * Returns the objective variable or null.
	 * @return the objective variable or null
	 */
	public IntegerVariable getObjectiveVariable() {
		return objectiveVariable;
	}

	public void setObjectiveVariable(IntegerVariable objectiveVariable) {
		this.objectiveVariable = objectiveVariable;
	}
	
	/**
	 * Returns the objective.
	 * @return the objective
	 */
	public Objective getObjective() {
		return objective;
	}

	public void setObjective(Objective objective) {
		this.objective = objective;
	}
	
	/**
	 * Returns the integer variables.
	 * @return the integer variables
	 */
	public List<IntegerVariable> getIntegerVariables() {
		return integerVariables;
	}

	/**
	 * Returns the integer variable of the given name.
	 * @param name the name of the integer variable
	 * @return the integer variable or null
	 */
	public IntegerVariable getIntegerVariable(String name) {
		return integerVariableMap.get(name);
	}

	public void add(IntegerVariable v) throws SugarException {
		String name = v.getName();
		if (integerVariableMap.containsKey(name)) {
			throw new SugarException("Duplicated integer variable " + name); 
		}
		integerVariableMap.put(v.getName(), v);
		integerVariables.add(v);
	}

	/**
	 * Returns the boolean variables.
	 * @return the boolean variables
	 */
	public List<BooleanVariable> getBooleanVariables() {
		return booleanVariables;
	}

	public void add(BooleanVariable v) throws SugarException {
		String name = v.getName();
		if (booleanVariableMap.containsKey(name)) {
			throw new SugarException("Duplicated boolean variable " + name); 
		}
		booleanVariableMap.put(v.getName(), v);
		booleanVariables.add(v);
	}

	/**
	 * Returns the boolean variable of the given name.
	 * @param name the name of the boolean variable
	 * @return the boolean variable or null
	 */
	public BooleanVariable getBooleanVariable(String name) {
		return booleanVariableMap.get(name);
	}

	/**
	 * Returns the clauses.
	 * @return the clauses
	 */
	public List<Clause> getClauses() {
		return clauses;
	}

	/**
	 * Adds a clause.
	 * @param clause the clause to be added
	 */
	public void add(Clause clause) {
		clauses.add(clause);
	}

	public boolean isUnsatisfiable() throws SugarException {
		for (IntegerVariable v : integerVariables) {
			if (v.isUnsatisfiable()) {
				Logger.fine("Unsatisfiable integer variable " + v);
				return true;
			}
		}
		for (Clause c : clauses) {
			if (c.isUnsatisfiable()) {
				Logger.fine("Unsatisfiable constraint " + c.toString());
				return true;
			}
		}
		return false;
	}

	public int propagate() throws SugarException {
		int removedValues = 0;
		int removedLiterals = 0;
		while (true) {
			List<Clause> modifiedClauses = new ArrayList<Clause>();
			for (Clause clause : clauses) {
				if (clause.isModified()) {
					modifiedClauses.add(clause);
				}
			}		
			for (IntegerVariable v : integerVariables) {
				v.setModified(false);
			}
			int values = 0;
			int literals = 0;
			for (Clause clause : modifiedClauses) {
				values += clause.propagate();
				literals += clause.removeFalsefood();
			}
			if (values == 0 && literals == 0)
				break;
			removedValues += values;
			removedLiterals += literals;
		}
		int removedClauses = 0;
		int i = 0;
		while (i < clauses.size()) {
			if (clauses.get(i).isValid()) {
				clauses.remove(i);
				removedClauses++;
			} else {
				i++;
			}
		}
		Logger.fine(removedValues + " values, " 
				+ removedLiterals + " unsatisfiable literals, and "
				+ removedClauses + " valid clauses are removed");
		return removedValues + removedLiterals + removedClauses;
	}
	
	private List<Clause> simplify(Clause clause) throws SugarException {
		List<Literal> literals = clause.getLiterals();
		List<Clause> newClauses = new ArrayList<Clause>();
		clause = new Clause();
		int complex = 0;
		for (Literal literal : literals) {
			if (literal.isSimple()) {
				clause.add(literal);
			} else {
				complex++;
				if (! simplifyAll && complex == 1) {
					clause.add(literal);
				} else {
					BooleanVariable p = new BooleanVariable();
					add(p);
					Literal posLiteral = new BooleanLiteral(p, false);
					Literal negLiteral = new BooleanLiteral(p, true);
					Clause newClause = new Clause();
					newClause.add(negLiteral);
					newClause.add(literal);
					newClauses.add(newClause);
					clause.add(posLiteral);
				}
			}
		}
		newClauses.add(clause);
		return newClauses;
	}
	
	public void simplify() throws SugarException {
		List<Clause> newClauses = new ArrayList<Clause>();
		for (Clause clause : clauses) {
			if (clause.isSimple()) {
				newClauses.add(clause);
			} else {
				newClauses.addAll(simplify(clause));
			}
		}
		clauses = newClauses;
	}

	public void compact() throws SugarException {
		throw new SugarException("Unimplemented method compact()");
		/*
		int n = integerVariables.size();
		for (int i = 0; i < n; i++) {
			IntegerVariable v = integerVariables.get(i);
			v.compact(this);
		}
		List<Clause> newClauses = new ArrayList<Clause>();
		for (Clause clause : clauses) {
			if (clause.isCompact()) {
				newClauses.add(clause);
			} else {
				newClauses.addAll(compact(clause));
			}
		}
		clauses = newClauses;
		*/
	}
	
	/**
	 * Returns true when the CSP is satisfied.
	 * @return true when the CSP is satisfied
	 */
	public boolean isSatisfied() {
		for (IntegerVariable v : integerVariables) {
			if (! v.isSatisfied()) {
				// Logger.fine(v.toString() + " is not satisfied");
				return false;
			}
		}
		for (Clause clause : clauses) {
			if (! clause.isSatisfied()) {
				// Logger.fine(clause.toString() + " is not satisfied");
				return false;
			}
		}
		return true;
	}

	public void outputValues(PrintStream out) {
		for (IntegerVariable v : integerVariables) {
			if (v.isAux()) {
				out.println(v.getName() + " = " + v.getValue());
			} else {
				out.println(v.getName() + " = " + v.getValue());
			}
		}
		for (BooleanVariable v : booleanVariables) {
			if (! v.isAux()) {
				out.println(v.getName() + " = " + v.getValue());
			} else {
				out.println(v.getName() + " = " + v.getValue());
			}
		}
	}

	public void output(PrintStream out, String pre) {
		for (IntegerVariable v : integerVariables) {
			if (v.getComment() != null) {
				out.print(pre + "; ");
				out.println(v.getComment());
			}
			out.println(pre + v.toString());
		}
		for (BooleanVariable v : booleanVariables) {
			if (v.getComment() != null) {
				out.print(pre + "; ");
				out.println(v.getComment());
			}
			out.println(pre + v.toString());
		}
		for (Clause clause : clauses) {
			if (clause.getComment() != null) {
				out.print(pre + "; ");
				out.println(clause.getComment());
			}
			out.println(pre + clause.toString());
		}
		IntegerVariable v = getObjectiveVariable();
		if (v != null) {
			String obj = "none";
			if (objective == Objective.MINIMIZE) {
				obj = "minimize";
			} else if (objective == Objective.MAXIMIZE) {
				obj = "maximize";
			}
			out.println(pre + "(objective " + obj + " " + v.getName() + ")");
		}
	}

	public String summary() {
		int size = 0;
		for (IntegerVariable v : integerVariables) {
			size = Math.max(size, v.getDomain().size());
		}
		return 
		getIntegerVariables().size() + " integers, " +
		getBooleanVariables().size() + " booleans, " +
		getClauses().size() + " clauses, " +
		"largest domain size " + size;
	}
	
	/**
	 * Returns the string representation of the CSP.
	 * @return the string representation
	 */
	@Override
	public String toString() {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(bs);
		output(out, "");
		return bs.toString();
	}

}
