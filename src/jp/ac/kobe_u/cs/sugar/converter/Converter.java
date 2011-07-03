package jp.ac.kobe_u.cs.sugar.converter;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import jp.ac.kobe_u.cs.sugar.Logger;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.*;
import jp.ac.kobe_u.cs.sugar.csp.CSP.Objective;
import jp.ac.kobe_u.cs.sugar.expression.*;

/**
 * Converter class is used to convert input expressions to a CSP.
 * @see Expression
 * @see CSP 
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class Converter {
	public static int MAX_EQUIVMAP_SIZE = 1000;
	public static long MAX_LINEARSUM_SIZE = 1024L;
	public static boolean expandABS = true;
	public static boolean OPT_PIGEON = true;
	public static boolean INCREMENTAL_PROPAGATE = true;
	public static boolean ESTIMATE_SATSIZE = false; // bad
    public static boolean NEW_VARIABLE = true;
    public static int SPLITS = 2;
	
	private class EquivMap extends LinkedHashMap<Expression,IntegerVariable> {

		private static final long serialVersionUID = -4882267868872050198L;

		EquivMap() {
			super(100, 0.75f, true);
		}

		/* (non-Javadoc)
		 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
		 */
		@Override
		protected boolean removeEldestEntry(Entry<Expression, IntegerVariable> eldest) {
			return size() > MAX_EQUIVMAP_SIZE;
		}
		
	}
	
	private CSP csp;
	private Map<String,IntegerDomain> domainMap;
	private Map<String,IntegerVariable> intMap;
	private Map<String,BooleanVariable> boolMap;
	// private Map<String,Predicate> predicateMap;
	// private Map<String,Relation> relationMap;
	// private Map<Expression,IntegerVariable> equivMap;
	// private List<Expression> extra;
	
	public Converter(CSP csp) {
		this.csp = csp;
		domainMap = new HashMap<String,IntegerDomain>();
		intMap = new HashMap<String,IntegerVariable>();
		boolMap = new HashMap<String,BooleanVariable>();
		// predicateMap = new HashMap<String,Predicate>();
		// relationMap = new HashMap<String,Relation>();
		// equivMap = new EquivMap();
		// extra = new ArrayList<Expression>();
	}
	
	// private void convertDomainDefinition(Sequence seq) throws SugarException {
	// 	String name = null;
	// 	IntegerDomain domain = null;
	// 	if (seq.matches("WWII")) {
	// 		name = seq.get(1).stringValue();
	// 		int lb = seq.get(2).integerValue();
	// 		int ub = seq.get(3).integerValue();
	// 		domain = new IntegerDomain(lb, ub);
	// 	} else if (seq.matches("WWI")) {
	// 		name = seq.get(1).stringValue();
	// 		int lb = seq.get(2).integerValue();
	// 		domain = new IntegerDomain(lb, lb);
	// 	} else if (seq.matches("WWS")) {
	// 		name = seq.get(1).stringValue();
	// 		SortedSet<Integer> d = new TreeSet<Integer>(); 
	// 		Sequence x = (Sequence)seq.get(2);
	// 		for (int i = 0; i < x.length(); i++) {
	// 			if (x.get(i).isInteger()) {
	// 				d.add(x.get(i).integerValue());
	// 			} else 	if (x.get(i).isSequence()) {
	// 				Sequence seq1 = (Sequence)x.get(i);
	// 				if (seq1.matches("II")) {
	// 					int value0 = ((Sequence)x.get(i)).get(0).integerValue();
	// 					int value1 = ((Sequence)x.get(i)).get(1).integerValue();
	// 					for (int value = value0; value <= value1; value++) {
	// 						d.add(value);
	// 					}
	// 				} else {
	// 					throw new SugarException("Bad definition " + seq);
	// 				}
	// 			} else {
	// 				throw new SugarException("Bad definition " + seq);
	// 			}
	// 		}
	// 		domain = new IntegerDomain(d);
	// 	} else {
	// 		throw new SugarException("Bad definition " + seq);
	// 	}
	// 	if (domainMap.containsKey(name)) {
	// 		throw new SugarException("Duplicated definition " + seq);
	// 	}
	// 	domainMap.put(name, domain);
	// }

	private void convertIntDefinition(Sequence seq) throws SugarException {
		String name = null;
		IntegerDomain domain = null;
		if (seq.matches("WWW")) {
			name = seq.get(1).stringValue();
			String domainName = seq.get(2).stringValue();
			domain = domainMap.get(domainName);
		} else if (seq.matches("WWS")) {
			name = seq.get(1).stringValue();
			SortedSet<Integer> d = new TreeSet<Integer>(); 
			Sequence x = (Sequence)seq.get(2);
			for (int i = 0; i < x.length(); i++) {
				if (x.get(i).isInteger()) {
					d.add(x.get(i).integerValue());
				} else 	if (x.get(i).isSequence()) {
					Sequence seq1 = (Sequence)x.get(i);
					if (seq1.matches("II")) {
						int value0 = ((Sequence)x.get(i)).get(0).integerValue();
						int value1 = ((Sequence)x.get(i)).get(1).integerValue();
						for (int value = value0; value <= value1; value++) {
							d.add(value);
						}
					} else {
						throw new SugarException("Bad definition " + seq);
					}
				} else {
					throw new SugarException("Bad definition " + seq);
				}
			}
			domain = new IntegerDomain(d);
		} else if (seq.matches("WWII")) {
			name = seq.get(1).stringValue();
			int lb = seq.get(2).integerValue();
			int ub = seq.get(3).integerValue();
			domain = new IntegerDomain(lb, ub);
		} else if (seq.matches("WWI")) {
			name = seq.get(1).stringValue();
			int lb = seq.get(2).integerValue();
			domain = new IntegerDomain(lb, lb);
		} else {
			throw new SugarException("Bad definition " + seq);
		}
		if (domain == null) {
			throw new SugarException("Unknown domain " + seq);
		}
		if (intMap.containsKey(name)) {
			throw new SugarException("Duplicated definition " + seq);
		}
		IntegerVariable v = new IntegerVariable(name, domain);
		csp.add(v);
		intMap.put(name, v);
	}

	private void convertBoolDefinition(Sequence seq) throws SugarException {
		String name = null;
		if (seq.matches("WW")) {
			name = seq.get(1).stringValue();
		} else {
			throw new SugarException("Bad definition " + seq);
		}
		if (boolMap.containsKey(name)) {
			throw new SugarException("Duplicated definition " + seq);
		}
		BooleanVariable v = new BooleanVariable(name);
		csp.add(v);
		boolMap.put(name, v);
	}

	private void convertObjectiveDefinition(Sequence seq) throws SugarException {
		Objective objective = null;
		String name = null;
		if (seq.matches("WWW")) {
			if (seq.get(1).equals(Expression.MINIMIZE)) {
				objective = Objective.MINIMIZE;
			} else if (seq.get(1).equals(Expression.MAXIMIZE)) {
				objective = Objective.MAXIMIZE;
			}
			name = seq.get(2).stringValue();
		}
		if (objective == null || name == null) {
			throw new SugarException("Bad definition " + seq);
		}
		IntegerVariable v = intMap.get(name);
		if (v == null) {
			throw new SugarException("Unknown objective variable " + seq);
		}
		csp.setObjectiveVariable(v);
		csp.setObjective(objective);
	}

	protected void syntaxError(String s) throws SugarException {
		throw new SugarException("Syntax error " + s);
	}
	
	protected void syntaxError(Expression x) throws SugarException {
		syntaxError(x.toString());
	}
	
	protected void checkArity(Expression x, int arity) throws SugarException {
		if (! x.isSequence(arity)) {
			syntaxError(x);
		}
	}

	private IntegerVariable newIntegerVariable(IntegerDomain d, Expression x)
	throws SugarException {
		IntegerVariable v = new IntegerVariable(d);
		csp.add(v);
		v.setComment(v.getName() + " : " + x.toString());
		return v;
	}
	
	private LinearSum convertInteger(Atom x) throws SugarException {
		return new LinearSum(x.integerValue());
	}
	
	private LinearSum convertString(Atom x) throws SugarException {
		String s = x.stringValue();
		if (csp.getIntegerVariable(s) == null) {
			syntaxError(x);
		}
		IntegerVariable v = csp.getIntegerVariable(s);
		return new LinearSum(v);
	}

	private LinearSum convertPOW(Sequence seq) throws SugarException {
		// TODO pow
		throw new SugarException("Unsupported " + seq);
	}

	private LinearSum convertLinearSum(LinearExpression x)
		throws SugarException {
		LinearSum ls = new LinearSum(x.getB());
		for(Atom atom: x.getVariables()) {
			IntegerVariable v = intMap.get(atom.stringValue());
			if (v == null) throw new SugarException("!!!");
			int a = x.getA(atom);
			if (a != 0) {
				ls.setA(a, v);
			}
		}
		return ls;
	}

	private Literal convertLiteral(Expression x) throws SugarException {
		if (x.isAtom()) {
			if (boolMap.containsKey(x.stringValue())) {
				BooleanVariable v = boolMap.get(x.stringValue());
				return new BooleanLiteral(v, false);
			}else{
				syntaxError(x);
			}
		}else if (x.isSequence(Expression.NOT)) {
			checkArity(x, 1);
			Sequence seq = (Sequence)x;
			if (boolMap.containsKey(seq.get(1).stringValue())) {
				BooleanVariable v = boolMap.get(seq.get(1).stringValue());
				return new BooleanLiteral(v, true);
			}else{
				syntaxError(x);
			}
		}else if (x.isSequence(Expression.LE)
							|| x.isSequence(Expression.EQ)
							|| x.isSequence(Expression.NE)) {
			checkArity(x, 2);
			Sequence seq = (Sequence)x;
			assert(seq.get(2).equals(Expression.ZERO));
			LinearSum ls = convertLinearSum((LinearExpression)seq.get(1));
			return new LinearLiteral(ls, (Atom)seq.get(0));
		}
		syntaxError(x);
		return null;
	}

	private void convertClause(Sequence x) throws SugarException {
		assert(x.isSequence(Expression.OR));
		Clause c = new Clause();
		for(int i=1 ; i < x.length(); i++) {
			c.add(convertLiteral(x.get(i)));
		}
		c.setComment(x.getComment());
		csp.add(c);
	}

	private void convertExpression(Expression x) throws SugarException {
		if (x.isSequence(Expression.INT_DEFINITION)) {
			convertIntDefinition((Sequence)x);
		} else if (x.isSequence(Expression.BOOL_DEFINITION)) {
			convertBoolDefinition((Sequence)x);
		} else if (x.isSequence(Expression.OBJECTIVE_DEFINITION)) {
			convertObjectiveDefinition((Sequence)x);
		} else if (x.isSequence(Expression.OR)) {
			convertClause((Sequence)x);
		}
		assert(false);
	}
	
	public void convert(List<Expression> expressions) throws SugarException {
		int n = expressions.size();
		int percent = 10;
		int count = 0;
		for (Expression x : expressions) {
			convertExpression(x);
			count++;
			if ((100*count)/n >= percent) {
				Logger.fine("converted " + count + " (" + percent + "%) expressions");
				percent += 10;
			}
		}
	}
}
