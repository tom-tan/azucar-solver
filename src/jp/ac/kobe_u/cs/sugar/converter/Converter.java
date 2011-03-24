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
	// public static long MAX_LINEARSUM_SIZE = 2048L;
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
	// private List<Expression> expressions;
	private Map<String,IntegerDomain> domainMap;
	private Map<String,IntegerVariable> intMap;
	private Map<String,BooleanVariable> boolMap;
	private Map<String,Predicate> predicateMap;
	private Map<String,Relation> relationMap;
	private Map<Expression,IntegerVariable> equivMap;
	private List<Expression> extra;
	
	public Converter(CSP csp) {
		this.csp = csp;
		domainMap = new HashMap<String,IntegerDomain>();
		intMap = new HashMap<String,IntegerVariable>();
		boolMap = new HashMap<String,BooleanVariable>();
		predicateMap = new HashMap<String,Predicate>();
		relationMap = new HashMap<String,Relation>();
		equivMap = new EquivMap();
		extra = new ArrayList<Expression>();
	}
	
	private void convertDomainDefinition(Sequence seq) throws SugarException {
		String name = null;
		IntegerDomain domain = null;
		if (seq.matches("WWII")) {
			name = seq.get(1).stringValue();
			int lb = seq.get(2).integerValue();
			int ub = seq.get(3).integerValue();
			domain = new IntegerDomain(lb, ub);
		} else if (seq.matches("WWI")) {
			name = seq.get(1).stringValue();
			int lb = seq.get(2).integerValue();
			domain = new IntegerDomain(lb, lb);
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
		} else {
			throw new SugarException("Bad definition " + seq);
		}
		if (domainMap.containsKey(name)) {
			throw new SugarException("Duplicated definition " + seq);
		}
		domainMap.put(name, domain);
	}

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

	private void convertPredicateDefinition(Sequence seq) throws SugarException {
		if (! seq.matches("WSS")) {
			syntaxError(seq);
		}
		Sequence def = (Sequence)seq.get(1);
		String name = def.get(0).stringValue();
		Sequence body = (Sequence)seq.get(2);
		Predicate pred = new Predicate(def, body);
		predicateMap.put(name, pred);
	}

	private void convertRelationDefinition(Sequence seq) throws SugarException {
		if (! seq.matches("WWIS")) {
			syntaxError(seq);
		}
		String name = seq.get(1).stringValue();
		int arity = seq.get(2).integerValue();
		Sequence body = (Sequence)seq.get(3);
		Relation rel = new Relation(name, arity, body);
		relationMap.put(name, rel);
	}

	private void addEquivalence(IntegerVariable v, Expression x) {
		equivMap.put(x, v);
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
	
	private IntegerVariable toIntegerVariable(LinearSum e, Expression x)
	throws SugarException {
		IntegerVariable v;
		if (e.isIntegerVariable()) {
			v = e.getCoef().firstKey();
		} else {
			v = newIntegerVariable(e.getDomain(), x);
			Expression eq = Expression.create(v.getName()).eq(x);
			eq.setComment(v.getName() + " == " + x);
			convertConstraint(eq);
			addEquivalence(v, x);
		}
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
	
	private LinearSum convertADD(Sequence seq) throws SugarException {
		LinearSum e = new LinearSum(0);
		for (int i = 1; i < seq.length(); i++) {
			LinearSum ei = convertFormula(seq.get(i));
			e.add(ei);
		}
		return e;
	}
	
	private LinearSum convertSUB(Sequence seq) throws SugarException {
		LinearSum e = null;
		if (seq.length() == 1) {
			syntaxError(seq);
		} else if (seq.length() == 2) {
			e = convertFormula(seq.get(1));
			e.multiply(-1);
		} else {
			e = convertFormula(seq.get(1));
			for (int i = 2; i < seq.length(); i++) {
				LinearSum ei = convertFormula(seq.get(i));
				e.subtract(ei);
			}
		}
		return e;
	}
	
	private LinearSum convertABS(Sequence seq) throws SugarException {
		checkArity(seq, 1);
		Expression x1 = seq.get(1);
		LinearSum e1 = convertFormula(x1);
		IntegerDomain d1 = e1.getDomain();
		if (d1.getLowerBound() >= 0) {
			return e1;
		} else if (d1.getUpperBound() <= 0) {
			e1.multiply(-1);
			return e1;
		}
		IntegerDomain d = d1.abs();
		IntegerVariable v = newIntegerVariable(d, seq);
		Expression x = Expression.create(v.getName());
		Expression eq =
			(x.ge(x1))
			.and(x.ge(x1.neg()))
			.and((x.le(x1)).or(x.le(x1.neg())));
		eq.setComment(v.getName() + " == " + seq);
		convertConstraint(eq);
		addEquivalence(v, seq);
		return new LinearSum(v);
	}
	
	private LinearSum convertMUL(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		LinearSum e1 = convertFormula(x1);
		LinearSum e2 = convertFormula(x2);
		IntegerDomain d1 = e1.getDomain();
		IntegerDomain d2 = e2.getDomain();
		if (d1.size() == 1) {
			e2.multiply(d1.getLowerBound());
			return e2;
		} else if (d2.size() == 1) {
			e1.multiply(d2.getLowerBound());
			return e1;
		} else if (d1.size() <= d2.size()) {
			Expression x = null;
			Iterator<Integer> iter = d1.values();
			while (iter.hasNext()) {
				int a1 = iter.next();
				if (x == null) {
					x = x2.mul(a1);
				} else {
					x = (x1.ge(a1)).ifThenElse(x2.mul(a1), x);
				}
			}
			return convertIF((Sequence)x);
		} else {
			return convertMUL((Sequence)x2.mul(x1));
		}
		/*
		if (true) {
			throw new SugarException("Unsupported " + seq);
		}
		IntegerVariable v;
		IntegerVariable v1 = toIntegerVariable(e1, x1);
		IntegerVariable v2 = toIntegerVariable(e2, x2);
		if (v1.equals(v2)) {
			IntegerDomain d = d1.pow(2);
			v = newIntegerVariable(d, seq);
			// TODO Clause clause = new Clause(new PowerLiteral(v, v1, 2));
			Clause clause = new Clause(new ProductLiteral(v, v1, v2));
			clause.setComment(v.getName() + " == " + seq);
			csp.add(clause);
		} else {
			IntegerDomain d = d1.mul(d2);
			v = newIntegerVariable(d, seq);
			Clause clause = new Clause(new ProductLiteral(v, v1, v2));
			clause.setComment(v.getName() + " == " + seq);
			csp.add(clause);
		}
		addEquivalence(v, seq);
		return new LinearSum(v);
		*/
	}
	
	private LinearSum convertDIV(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		LinearSum e1 = convertFormula(x1);
		LinearSum e2 = convertFormula(x2);
		IntegerDomain d1 = e1.getDomain();
		IntegerDomain d2 = e2.getDomain();
		IntegerDomain qd = d1.div(d2);
		IntegerDomain rd = d1.mod(d2);
		IntegerVariable qv = newIntegerVariable(qd, seq);
		IntegerVariable rv = newIntegerVariable(rd, x1.mod(x2));
		Expression q = Expression.create(qv.getName());
		Expression r = Expression.create(rv.getName());
		Expression px = x2.mul(q);
		if (d2.size() == 1) {
			int value2 = d2.getLowerBound();
			if (value2 == 1) {
				return e1;
			} else if (value2 == -1) {
				e1.multiply(-1);
				return e1;
			}
			// TODO
			if (value2 <= 0) {
				throw new SugarException("Unsupported " + seq);
			}
			Expression eq =
				(x1.eq(px.add(r)))
				.and((r.ge(Expression.ZERO)).and((x2.abs()).gt(r)));
			eq.setComment(qv.getName() + " == " + seq);
			convertConstraint(eq);
			addEquivalence(qv, seq);
			return new LinearSum(qv);
		}
		// TODO
		if (true) {
			throw new SugarException("Unsupported " + seq);
		}
		IntegerVariable v2 = toIntegerVariable(e2, x2);
		IntegerDomain pd = d1.sub(rd);
		IntegerVariable pv = newIntegerVariable(pd, px);
		Clause clause = new Clause(new ProductLiteral(pv, qv, v2));
		clause.setComment(pv.getName() + " == " + px);
		csp.add(clause);
		Expression p = Expression.create(pv.getName());
		Expression eq =
			(x1.eq(p.add(r)))
			.and((r.ge(Expression.ZERO)).and((x2.abs()).gt(r)));
		eq.setComment(qv.getName() + " == " + seq);
		convertConstraint(eq);
		addEquivalence(qv, seq);
		return new LinearSum(qv);
	}
	
	private LinearSum convertMOD(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		LinearSum e1 = convertFormula(x1);
		LinearSum e2 = convertFormula(x2);
		IntegerDomain d1 = e1.getDomain();
		IntegerDomain d2 = e2.getDomain();
		IntegerDomain qd = d1.div(d2);
		IntegerDomain rd = d1.mod(d2);
		IntegerVariable qv = newIntegerVariable(qd, seq);
		IntegerVariable rv = newIntegerVariable(rd, x1.mod(x2));
		Expression q = Expression.create(qv.getName());
		Expression r = Expression.create(rv.getName());
		Expression px = x2.mul(q);
		if (d2.size() == 1) {
			int value2 = d2.getLowerBound();
			// TODO
			if (value2 <= 0) {
				throw new SugarException("Unsupported " + seq);
			}
			Expression eq =
				(x1.eq(px.add(r)))
				.and((r.ge(Expression.ZERO)).and((x2.abs()).gt(r)));
			eq.setComment(qv.getName() + " == " + seq);
			convertConstraint(eq);
			addEquivalence(rv, seq);
			return new LinearSum(rv);
		}
		// TODO
		if (true) {
			throw new SugarException("Unsupported " + seq);
		}
		IntegerVariable v2 = toIntegerVariable(e2, x2);
		IntegerDomain pd = d1.sub(rd);
		IntegerVariable pv = newIntegerVariable(pd, px);
		Clause clause = new Clause(new ProductLiteral(pv, qv, v2));
		clause.setComment(pv.getName() + " == " + px);
		csp.add(clause);
		Expression p = Expression.create(pv.getName());
		Expression eq =
			(x1.eq(p.add(r)))
			.and((r.ge(Expression.ZERO)).and((x2.abs()).gt(r)));
		eq.setComment(rv.getName() + " == " + seq);
		convertConstraint(eq);
		addEquivalence(rv, seq);
		return new LinearSum(rv);
	}
	
	private LinearSum convertPOW(Sequence seq) throws SugarException {
		// TODO pow
		throw new SugarException("Unsupported " + seq);
		// return null;
	}
	
	private LinearSum convertMIN(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		LinearSum e1 = convertFormula(x1);
		LinearSum e2 = convertFormula(x2);
		IntegerDomain d1 = e1.getDomain();
		IntegerDomain d2 = e2.getDomain();
		if (d1.getUpperBound() <= d2.getLowerBound()) {
			return e1;
		} else if (d2.getUpperBound() <= d1.getLowerBound()) {
			return e2;
		}
		IntegerDomain d = d1.min(d2);
		IntegerVariable v = newIntegerVariable(d, seq);
		Expression x = Expression.create(v.getName());
		Expression eq =
			(x.le(x1))
			.and(x.le(x2))
			.and((x.ge(x1)).or(x.ge(x2)));
		eq.setComment(v.getName() + " == " + seq);
		convertConstraint(eq);
		addEquivalence(v, seq);
		return new LinearSum(v);
	}
	
	private LinearSum convertMAX(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		LinearSum e1 = convertFormula(x1);
		LinearSum e2 = convertFormula(x2);
		IntegerDomain d1 = e1.getDomain();
		IntegerDomain d2 = e2.getDomain();
		if (d1.getUpperBound() <= d2.getLowerBound()) {
			return e2;
		} else if (d2.getUpperBound() <= d1.getLowerBound()) {
			return e1;
		}
		IntegerDomain d = d1.max(d2);
		IntegerVariable v = newIntegerVariable(d, seq);
		Expression x = Expression.create(v.getName());
		Expression eq =
			(x.ge(x1))
			.and(x.ge(x2))
			.and((x.le(x1)).or(x.le(x2)));
		eq.setComment(v.getName() + " == " + seq);
		convertConstraint(eq);
		addEquivalence(v, seq);
		return new LinearSum(v);
	}
	
	private LinearSum convertIF(Sequence seq) throws SugarException {
		checkArity(seq, 3);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		Expression x3 = seq.get(3);
		LinearSum e2 = convertFormula(x2);
		LinearSum e3 = convertFormula(x3);
		IntegerDomain d2 = e2.getDomain();
		IntegerDomain d3 = e3.getDomain();
		IntegerDomain d = d2.cup(d3);
		IntegerVariable v = newIntegerVariable(d, seq);
		Expression x = Expression.create(v.getName());
		Expression eq =
			((x1.not()).or(x.eq(x2)))
			.and(x1.or(x.eq(x3)));
		eq.setComment(v.getName() + " == " + seq);
		convertConstraint(eq);
		addEquivalence(v, seq);
		return new LinearSum(v);
	}
	
	protected LinearSum convertFormula(Expression x) throws SugarException {
		LinearSum e = null;
		IntegerVariable v = equivMap.get(x);
		if (v != null) {
			e = new LinearSum(v);
		} else if (x.isAtom()) {
			if (x.isInteger()) {
				e = convertInteger((Atom)x);
			} else {
				e = convertString((Atom)x);
			}
		} else {
			if (x.isSequence(Expression.ADD)) {
				e = convertADD((Sequence)x);
			} else if (x.isSequence(Expression.NEG) || x.isSequence(Expression.SUB)) {
				e = convertSUB((Sequence)x);
			} else if (x.isSequence(Expression.ABS)) {
				e = convertABS((Sequence)x);
			} else if (x.isSequence(Expression.MUL)) {
				e = convertMUL((Sequence)x);
			} else if (x.isSequence(Expression.DIV)) {
				e = convertDIV((Sequence)x);
			} else if (x.isSequence(Expression.MOD)) {
				e = convertMOD((Sequence)x);
			} else if (x.isSequence(Expression.POW)) {
				e = convertPOW((Sequence)x);
			} else if (x.isSequence(Expression.MIN)) {
				e = convertMIN((Sequence)x);
			} else if (x.isSequence(Expression.MAX)) {
				e = convertMAX((Sequence)x);
			} else if (x.isSequence(Expression.IF)) {
				e = convertIF((Sequence)x);
			} else {
				syntaxError(x);
			}
		}
		return e;
	}
	
	private LinearSum simplifyLinearSum(LinearSum e) throws SugarException {
		if (e.size() <= 3) {
			return e;
		}
		/*
		if (! e.isDomainLargerThan(MAX_LINEARSUM_SIZE)) {
			return e;
		}
		*/
		IntegerVariable var = e.getLargestDomainVariable();
		if (! e.isDomainLargerThanExcept(MAX_LINEARSUM_SIZE, var)) {
			return e;
		}
		IntegerVariable[] vs = e.getVariablesSorted();
		/*
		LinearSum e0 = new LinearSum(e.getB());
		int domainSize = 1;
		int i = 0;
		for (i = 0; i < vs.length - 2 && domainSize <= MAX_LINEARSUM_SIZE; i++) {
			e0.setA(e.getA(vs[i]), vs[i]);
			domainSize *= vs[i].getDomain().size();
		}
		LinearSum e1 = new LinearSum(0);
		for (int j = i; j < vs.length; j++) {
			e1.setA(e.getA(vs[j]), vs[j]);
		}
		int factor = e1.factor();
		if (factor > 1) {
			e1.divide(factor);
		}
		IntegerVariable v = new IntegerVariable(e1.getDomain());
		v.setComment(v.getName() + " : " + e1);
		csp.add(v);
		Expression x = Expression.create(v.getName());
		Expression ex = e1.toExpression();
		Expression eq = x.eq(ex);
		extra.add(eq);
		e0.setA(factor, v);
		return e0;
		*/
		LinearSum e1 = new LinearSum(0);
		for (int i = 2; i < vs.length; i++) {
			e1.setA(e.getA(vs[i]), vs[i]);
		}
		int factor = e1.factor();
		if (factor > 1) {
			e1.divide(factor);
		}
		// v == (a[2]*vs[2] + a[3]*vs[3] + ... + a[n]*vs[n]) / factor
		IntegerVariable v = new IntegerVariable(e1.getDomain());
		v.setComment(v.getName() + " : " + e1);
		csp.add(v);
		Expression x = Expression.create(v.getName());
		Expression ex = e1.toExpression();
		Expression eq = x.eq(ex);
		// eq.setComment(v.getName() + " == " + e1);
		// XXX 
		// convertConstraint(eq);
		extra.add(eq);
		// e0 = b + a[0]*vs[0] + a[1]*vs[1] + factor*v
		LinearSum e0 = new LinearSum(e.getB());
		e0.setA(e.getA(vs[0]), vs[0]);
		e0.setA(e.getA(vs[1]), vs[1]);
		e0.setA(factor, v);
//		System.out.println(e + " ==> " + e0 + " with " + eq);
		return e0;
	}
	
	private LinearSum simplifyLinearExpression(LinearSum e, boolean first) throws SugarException {
		if (ESTIMATE_SATSIZE) {
			if (e.satSizeLE(MAX_LINEARSUM_SIZE)) {
				return e;
			}
		} else {
			if (e.size() <= 1 || ! e.isDomainLargerThan(MAX_LINEARSUM_SIZE)) {
			// if (e.size() <= 1 || ! e.isDomainLargerThanExcept(MAX_LINEARSUM_SIZE)) {
				return e;
			}
		}
		int b = e.getB();
		LinearSum[] es = e.split(first ? 3 : SPLITS);
		e = new LinearSum(b);
		for (int i = 0; i < es.length; i++) {
			LinearSum ei = es[i];
			int factor = ei.factor();
			if (factor > 1) {
				ei.divide(factor);
			}
			ei = simplifyLinearExpression(ei, false);
			// System.out.println(es[i] + " ==> " + ei);
			if (ei.size() > 1) {
				IntegerVariable v = new IntegerVariable(ei.getDomain());
				v.setComment(v.getName() + " : " + ei);
				csp.add(v);
				Expression x = Expression.create(v.getName());
				Expression ex = ei.toExpression();
				Expression eq = x.eq(ex);
				eq.setComment(v.getName() + " == " + ei);
				convertConstraint(eq);
				ei = new LinearSum(v);
			}
			if (factor > 1) {
				ei.multiply(factor);
			}
			e.add(ei);
		}
		return e;
	}
	
	private List<Clause> convertComparison(Expression x) throws SugarException {
		LinearSum e = convertFormula(x);
		e.factorize();
		List<Clause> clauses = new ArrayList<Clause>();
		IntegerDomain d = e.getDomain();
		if (d.getUpperBound() <= 0) {
			return clauses;
		}
		if (d.getLowerBound() > 0) {
			clauses.add(new Clause());
			return clauses;
		}
		if (NEW_VARIABLE && e.size() > 3) {
			if (ESTIMATE_SATSIZE) {
				if (! e.satSizeLE(MAX_LINEARSUM_SIZE)) {
					e = simplifyLinearExpression(e, true);
				}
			} else {
				// if (e.isDomainLargerThan(MAX_LINEARSUM_SIZE)) {
				if (e.isDomainLargerThanExcept(MAX_LINEARSUM_SIZE)) {
					// XXX simplifyLinearExpression is better
					if (false) {
						e = simplifyLinearSum(e);
					} else {
						e = simplifyLinearExpression(e, true);
					}
				}
			}
		}
		clauses.add(new Clause(new LinearLiteral(e)));
		return clauses;
	}
	
	private List<Clause> convertDisj(Sequence seq, boolean negative) throws SugarException {
		List<Clause> clauses = null;
		if (seq.length() == 1) {
			clauses = new ArrayList<Clause>();
			clauses.add(new Clause());
		} else if (seq.length() == 2) {
			clauses = convertConstraint(seq.get(1), negative);
		} else {
			clauses = new ArrayList<Clause>();
			Clause clause = new Clause();
			// clause.setComment(seq.toString());
			clauses.add(clause);
			for (int i = 1; i < seq.length(); i++) {
				List<Clause> clauses0 = convertConstraint(seq.get(i), negative);
				if (clauses0.size() == 0) {
					return clauses0;
				} else if (clauses0.size() == 1) {
					clause.addAll(clauses0.get(0).getLiterals());
				} else {
					BooleanVariable v = new BooleanVariable();
					csp.add(v);
					// v.setComment(seq.toString());
					BooleanLiteral v0 = new BooleanLiteral(v, false);
					BooleanLiteral v1 = new BooleanLiteral(v, true);
					clause.add(v0);
					for (Clause clause0 : clauses0) {
						clause0.add(v1);
					}
					clauses.addAll(clauses0);
				}
			}
		}
		return clauses;
	}

	private Expression convertPredicate(Sequence seq) throws SugarException {
		String name = seq.get(0).stringValue();
		Predicate pred = predicateMap.get(name);
		if (pred == null) {
			throw new SugarException("Undefined predicate " + name + " in " + seq);
		}
		Expression[] args = new Expression[seq.length() - 1];
		for (int i = 1; i < seq.length(); i++) {
			args[i-1] = seq.get(i);
		}
		Expression x = pred.apply(args);
		return x;
	}
	
	private Literal convertRelation(Sequence seq) throws SugarException {
		String name = seq.get(0).stringValue();
		Relation rel = relationMap.get(name);
		if (rel == null) {
			throw new SugarException("Undefined relation " + name + " in " + seq);
		}
		IntegerVariable[] vs = new IntegerVariable[seq.length() - 1];
		for (int i = 1; i < seq.length(); i++) {
			IntegerVariable v = intMap.get(seq.get(i).stringValue());
			if (v == null) {
				syntaxError(seq);
			}
			vs[i-1] = v;
		}
		return new RelationLiteral(rel.arity, rel.conflicts, rel.tuples, vs);
	}

	private List<Clause> convertConstraint(Expression x, boolean negative) throws SugarException {
		List<Clause> clauses = null;
		while (true) {
			if (x.isAtom()) {
				if (x.isInteger()) {
					if (x.integerValue() > 0) {
						x = Expression.TRUE;
					} else {
						x = Expression.FALSE;
					}
				} else if ((x.equals(Expression.FALSE) && ! negative)
						|| (x.equals(Expression.TRUE) && negative)) {
					clauses = new ArrayList<Clause>();
					clauses.add(new Clause());
					break;
				} else if ((x.equals(Expression.FALSE) && negative)
						|| (x.equals(Expression.TRUE) && ! negative)) {
					clauses = new ArrayList<Clause>();
					break;
				} else if (boolMap.containsKey(x.stringValue())) {
					BooleanVariable v = boolMap.get(x.stringValue());
					Clause clause = new Clause(new BooleanLiteral(v, negative));
					clauses = new ArrayList<Clause>();
					clauses.add(clause);
					break;
				} else {
					syntaxError(x);
				}
			} else {
				Sequence seq = (Sequence)x;
				if (seq.length() == 0) {
					syntaxError(seq);
				} else if (predicateMap.containsKey(seq.get(0).stringValue())) {
					x = convertPredicate(seq);
				} else if (relationMap.containsKey(seq.get(0).stringValue())) {
					Literal lit = convertRelation(seq);
					clauses = new ArrayList<Clause>();
					clauses.add(new Clause(lit));
					break;
				} else if (seq.isSequence(Expression.NOT)) {
					checkArity(seq, 1);
					x = seq.get(1);
					negative = ! negative;
				} else if (seq.isSequence(Expression.IMP)) {
					checkArity(seq, 2);
					x = seq.get(1).not().or(seq.get(2));
				} else if (seq.isSequence(Expression.XOR)) {
					checkArity(seq, 2);
					x = (seq.get(1).or(seq.get(2))).and(seq.get(1).not().or(seq.get(2).not()));
				} else if (seq.isSequence(Expression.IFF)) {
					checkArity(seq, 2);
					x = (seq.get(1).not().or(seq.get(2))).and(seq.get(1).or(seq.get(2).not()));
				} else if ((seq.isSequence(Expression.AND) && ! negative)
						|| (seq.isSequence(Expression.OR) && negative)) {
					clauses = new ArrayList<Clause>();
					for (int i = 1; i < seq.length(); i++) {
						List<Clause> clauses0 = convertConstraint(seq.get(i), negative);
						clauses.addAll(clauses0);
					}
					break;
				} else if ((seq.isSequence(Expression.OR) && ! negative)
						|| (seq.isSequence(Expression.AND) && negative)) {
					clauses = convertDisj(seq, negative);
					break;
				} else if (seq.isSequence(Expression.EQ)) {
					checkArity(seq, 2);
					x = (seq.get(1).le(seq.get(2))).and(seq.get(1).ge(seq.get(2)));
				} else if (seq.isSequence(Expression.NE)) {
					checkArity(seq, 2);
					x = (seq.get(1).lt(seq.get(2))).or(seq.get(1).gt(seq.get(2)));
				} else if ((seq.isSequence(Expression.LE) && ! negative)
						|| (seq.isSequence(Expression.GT) && negative)) {
					checkArity(seq, 2);
					if (seq.get(1).isSequence(Expression.MUL) && seq.get(2).equals(Expression.ZERO)) {
						Expression a1 = ((Sequence)seq.get(1)).get(1);
						Expression a2 = ((Sequence)seq.get(1)).get(2);
						// a1*a2 <= 0
						x = ((a1.lt(Expression.ZERO)).and(a2.gt(Expression.ZERO)))
						.or((a1.gt(Expression.ZERO)).and(a2.lt(Expression.ZERO)))
						.or(a1.eq(Expression.ZERO))
						.or(a2.eq(Expression.ZERO));
						continue;
					}
					if (seq.get(2).isSequence(Expression.MUL) && seq.get(1).equals(Expression.ZERO)) {
						Expression a1 = ((Sequence)seq.get(2)).get(1);
						Expression a2 = ((Sequence)seq.get(2)).get(2);
						// a1*a2 >= 0
						x = ((a1.lt(Expression.ZERO)).and(a2.lt(Expression.ZERO)))
						.or((a1.gt(Expression.ZERO)).and(a2.gt(Expression.ZERO)))
						.or(a1.eq(Expression.ZERO))
						.or(a2.eq(Expression.ZERO));
						continue;
					}
					if (expandABS) {
//						if (seq.get(1).isSequence(Expression.ABS) &&
//								seq.get(2).isSequence(Expression.ABS)) {
//							Expression a1 = ((Sequence)seq.get(1)).get(1);
//							Expression a2 = ((Sequence)seq.get(2)).get(1);
//							// abs(a1) <= abs(a2)
//							Expression x1 = (a1.le(Expression.ZERO)).or(a1.le(a2)).or(a1.le(a2.neg()));
//							Expression x2 = (a1.ge(Expression.ZERO)).or(a1.ge(a2)).or(a1.ge(a2.neg()));
//							x = x1.and(x2);
//							continue;
//						}
						if (seq.get(1).isSequence(Expression.ABS)) {
							Expression a1 = ((Sequence)seq.get(1)).get(1);
							Expression x2 = seq.get(2);
							// abs(a1) <= x2
							x = (a1.le(x2)).and(a1.ge(x2.neg()));
							continue;
						}
						if (seq.get(2).isSequence(Expression.ABS)) {
							Expression a1 = ((Sequence)seq.get(2)).get(1);
							Expression x1 = seq.get(1);
							// abs(a1) >= x1
							x = (a1.ge(x1)).or(a1.le(x1.neg()));
							continue;
						}
					}
					clauses = convertComparison(seq.get(1).sub(seq.get(2)));
					break;
				} else if ((seq.isSequence(Expression.LT) && ! negative)
						|| (seq.isSequence(Expression.GE) && negative)) {
					checkArity(seq, 2);
					if (seq.get(1).isSequence(Expression.MUL) && seq.get(2).equals(Expression.ZERO)) {
						Expression a1 = ((Sequence)seq.get(1)).get(1);
						Expression a2 = ((Sequence)seq.get(1)).get(2);
						// a1*a2 < 0
						x = ((a1.lt(Expression.ZERO)).and(a2.gt(Expression.ZERO)))
						.or((a1.gt(Expression.ZERO)).and(a2.lt(Expression.ZERO)));
						continue;
					}
					if (seq.get(2).isSequence(Expression.MUL) && seq.get(1).equals(Expression.ZERO)) {
						Expression a1 = ((Sequence)seq.get(1)).get(1);
						Expression a2 = ((Sequence)seq.get(1)).get(2);
						// a1*a2 > 0
						x = ((a1.lt(Expression.ZERO)).and(a2.lt(Expression.ZERO)))
						.or((a1.gt(Expression.ZERO)).and(a2.gt(Expression.ZERO)));
						continue;
					}
					if (expandABS) {
//						if (seq.get(1).isSequence(Expression.ABS) &&
//								seq.get(2).isSequence(Expression.ABS)) {
//							Expression a1 = ((Sequence)seq.get(1)).get(1);
//							Expression a2 = ((Sequence)seq.get(2)).get(1);
//							// abs(a1) < abs(a2)
//							Expression x1 = (a1.lt(Expression.ZERO)).or(a1.lt(a2)).or(a1.lt(a2.neg()));
//							Expression x2 = (a1.gt(Expression.ZERO)).or(a1.gt(a2)).or(a1.gt(a2.neg()));
//							x = x1.and(x2);
//							continue;
//						}
						if (seq.get(1).isSequence(Expression.ABS)) {
							Expression a1 = ((Sequence)seq.get(1)).get(1);
							Expression x2 = seq.get(2);
							// abs(a1) < x2
							x = (a1.lt(x2)).and(a1.gt(x2.neg()));
							continue;
						}
						if (seq.get(2).isSequence(Expression.ABS)) {
							Expression a1 = ((Sequence)seq.get(2)).get(1);
							Expression x1 = seq.get(1);
							// abs(a1) > x1
							x = (a1.gt(x1)).or(a1.lt(x1.neg()));
							continue;
						}
					}
					clauses = convertComparison(seq.get(1).sub(seq.get(2))
							.add(Expression.create(1)));
					break;
				} else if ((seq.isSequence(Expression.GE) && ! negative)
						|| (seq.isSequence(Expression.LT) && negative)) {
					checkArity(seq, 2);
					if (seq.get(1).isSequence(Expression.MUL) && seq.get(2).equals(Expression.ZERO)) {
						Expression a1 = ((Sequence)seq.get(1)).get(1);
						Expression a2 = ((Sequence)seq.get(1)).get(2);
						// a1*a2 >= 0
						x = ((a1.lt(Expression.ZERO)).and(a2.lt(Expression.ZERO)))
						.or((a1.gt(Expression.ZERO)).and(a2.gt(Expression.ZERO)))
						.or(a1.eq(Expression.ZERO))
						.or(a2.eq(Expression.ZERO));
						continue;
					}
					if (seq.get(2).isSequence(Expression.MUL) && seq.get(1).equals(Expression.ZERO)) {
						Expression a1 = ((Sequence)seq.get(2)).get(1);
						Expression a2 = ((Sequence)seq.get(2)).get(2);
						// a1*a2 <= 0
						x = ((a1.lt(Expression.ZERO)).and(a2.gt(Expression.ZERO)))
						.or((a1.gt(Expression.ZERO)).and(a2.lt(Expression.ZERO)))
						.or(a1.eq(Expression.ZERO))
						.or(a2.eq(Expression.ZERO));
						continue;
					}
					if (expandABS) {
//						if (seq.get(1).isSequence(Expression.ABS) &&
//								seq.get(2).isSequence(Expression.ABS)) {
//							Expression a1 = ((Sequence)seq.get(1)).get(1);
//							Expression a2 = ((Sequence)seq.get(2)).get(1);
//							// abs(a1) >= abs(a2)
//							Expression x1 = (a2.le(Expression.ZERO)).or(a2.le(a1)).or(a2.le(a1.neg()));
//							Expression x2 = (a2.ge(Expression.ZERO)).or(a2.ge(a1)).or(a2.ge(a1.neg()));
//							x = x1.and(x2);
//							continue;
//						}
						if (seq.get(1).isSequence(Expression.ABS)) {
							Expression a1 = ((Sequence)seq.get(1)).get(1);
							Expression x2 = seq.get(2);
							// abs(a1) >= x2
							x = (a1.ge(x2)).or(a1.le(x2.neg()));
							continue;
						}
						if (seq.get(2).isSequence(Expression.ABS)) {
							Expression a1 = ((Sequence)seq.get(2)).get(1);
							Expression x1 = seq.get(1);
							// abs(a1) <= x1
							x = (a1.le(x1)).and(a1.ge(x1.neg()));
							continue;
						}
					}
					clauses = convertComparison(seq.get(2).sub(seq.get(1)));
					break;
				} else if ((seq.isSequence(Expression.GT) && ! negative)
						|| (seq.isSequence(Expression.LE) && negative)) {
					checkArity(seq, 2);
					if (seq.get(1).isSequence(Expression.MUL) && seq.get(2).equals(Expression.ZERO)) {
						Expression a1 = ((Sequence)seq.get(1)).get(1);
						Expression a2 = ((Sequence)seq.get(1)).get(2);
						// a1*a2 > 0
						x = ((a1.lt(Expression.ZERO)).and(a2.lt(Expression.ZERO)))
						.or((a1.gt(Expression.ZERO)).and(a2.gt(Expression.ZERO)));
						continue;
					}
					if (seq.get(1).equals(Expression.ZERO) && seq.get(2).isSequence(Expression.MUL)) {
						Expression a1 = ((Sequence)seq.get(2)).get(1);
						Expression a2 = ((Sequence)seq.get(2)).get(2);
						// a1*a2 < 0
						x = ((a1.lt(Expression.ZERO)).and(a2.gt(Expression.ZERO)))
						.or((a1.gt(Expression.ZERO)).and(a2.lt(Expression.ZERO)));
						continue;
					}
					if (expandABS) {
//						if (seq.get(1).isSequence(Expression.ABS) &&
//						seq.get(2).isSequence(Expression.ABS)) {
//						Expression a1 = ((Sequence)seq.get(1)).get(1);
//						Expression a2 = ((Sequence)seq.get(2)).get(1);
//						// abs(a1) > abs(a2)
//						Expression x1 = (a2.lt(Expression.ZERO)).or(a2.lt(a1)).or(a2.lt(a1.neg()));
//						Expression x2 = (a2.gt(Expression.ZERO)).or(a2.gt(a1)).or(a2.gt(a1.neg()));
//						x = x1.and(x2);
//						continue;
//						}
						if (seq.get(1).isSequence(Expression.ABS)) {
							Expression a1 = ((Sequence)seq.get(1)).get(1);
							Expression x2 = seq.get(2);
							// abs(a1) > x2
							x = (a1.gt(x2)).or(a1.lt(x2.neg()));
							continue;
						}
						if (seq.get(2).isSequence(Expression.ABS)) {
							Expression a1 = ((Sequence)seq.get(2)).get(1);
							Expression x1 = seq.get(1);
							// abs(a1) < x1
							x = (a1.lt(x1)).and(a1.gt(x1.neg()));
							continue;
						}
					}
					clauses = convertComparison(seq.get(2).sub(seq.get(1))
							.add(Expression.create(1)));
					break;
				} else if (seq.isSequence(Expression.ALLDIFFERENT) && ! negative) {
					x = GlobalConstraints.convertAllDifferent(this, seq);
				} else if (seq.isSequence(Expression.WEIGHTEDSUM) && ! negative) {
					x = GlobalConstraints.convertWeightedSum(this, seq);
				} else if (seq.isSequence(Expression.CUMULATIVE) && ! negative) {
					x = GlobalConstraints.convertCumulative(this, seq);
				} else if (seq.isSequence(Expression.ELEMENT) && ! negative) {
					x = GlobalConstraints.convertElement(this, seq);
				} else if (seq.isSequence(Expression.DISJUNCTIVE) && ! negative) {
					x = GlobalConstraints.convertDisjunctive(this, seq);
				} else if (seq.isSequence(Expression.LEX_LESS) && ! negative) {
					x = GlobalConstraints.convertLex_less(this, seq);
				} else if (seq.isSequence(Expression.LEX_LESSEQ) && ! negative) {
					x = GlobalConstraints.convertLex_lesseq(this, seq);
				} else if (seq.isSequence(Expression.NVALUE) && ! negative) {
					x = GlobalConstraints.convertNvalue(this, seq);
				} else if (seq.isSequence(Expression.COUNT) && ! negative) {
					x = GlobalConstraints.convertCount(this, seq);
				} else if (seq.isSequence(Expression.GLOBAL_CARDINALITY) && ! negative) {
					x = GlobalConstraints.convertGlobal_cardinality(this, seq);
				} else if (seq.isSequence(Expression.GLOBAL_CARDINALITY_WITH_COSTS) && ! negative) {
					x = GlobalConstraints.convertGlobal_cardinality_with_costs(this, seq);
				} else {
					syntaxError(x);
				}
			}
		}
		return clauses;
	}

	/* TODO
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
				if (complex == 1) {
					clause.add(literal);
				} else {
					BooleanVariable p = new BooleanVariable();
					csp.add(p);
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
	
	private List<Clause> simplify(List<Clause> clauses) throws SugarException {
		List<Clause> newClauses = new ArrayList<Clause>();
		for (Clause clause : clauses) {
			if (clause.isSimple()) {
				newClauses.add(clause);
			} else {
				newClauses.addAll(simplify(clause));
			}
		}
		return newClauses;
	}
	*/

	private void convertConstraint(Expression x) throws SugarException {
		List<Clause> clauses = convertConstraint(x, false);
		// clauses = simplify(clauses);
		if (clauses.size() > 0) {
			if (x.getComment() == null) {
				clauses.get(0).setComment(x.toString());
			} else {
				clauses.get(0).setComment(x.getComment());
			}
		}
		for (Clause clause : clauses) {
			csp.add(clause);
			if (INCREMENTAL_PROPAGATE) {
				clause.propagate();
			}
		}
	}

	private void convertExpression(Expression x) throws SugarException {
		if (x.isSequence(Expression.DOMAIN_DEFINITION)) {
			convertDomainDefinition((Sequence)x);
		} else if (x.isSequence(Expression.INT_DEFINITION)) {
			convertIntDefinition((Sequence)x);
		} else if (x.isSequence(Expression.BOOL_DEFINITION)) {
			convertBoolDefinition((Sequence)x);
		} else if (x.isSequence(Expression.OBJECTIVE_DEFINITION)) {
			convertObjectiveDefinition((Sequence)x);
		} else if (x.isSequence(Expression.PREDICATE_DEFINITION)) {
			convertPredicateDefinition((Sequence)x);
		} else if (x.isSequence(Expression.RELATION_DEFINITION)) {
			convertRelationDefinition((Sequence)x);
		} else {
			convertConstraint(x);
		}
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
		while (extra.size() > 0) {
			Expression x = extra.remove(0);
			convertExpression(x);
			count++;
			if (count % 1000 == 0) {
				Logger.fine("converted " + count + " extra expressions, remaining " + extra.size());
			}
		}
	}
	
}
