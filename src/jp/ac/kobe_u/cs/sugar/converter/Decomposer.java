package jp.ac.kobe_u.cs.sugar.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
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

public class Decomposer {
	public static boolean OPT_PIGEON = true;
	public static int MAX_EQUIVMAP_SIZE = 1000;
	public static boolean expandABS = true;
	private final String IAUX_PREFIX = "_$ID";
	private int iidx = 0;
	private final String BAUX_PREFIX = "_$BD";
	private int bidx = 0;

	private class EquivMap extends LinkedHashMap<Expression,Atom> {
		private static final long serialVersionUID = -4882267868872050198L;

		EquivMap() {
			super(100, 0.75f, true);
		}

		/* (non-Javadoc)
		 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
		 */
		@Override
		protected boolean removeEldestEntry(Entry<Expression, Atom> eldest) {
			return size() > MAX_EQUIVMAP_SIZE;
		}
	}

	private List<Expression> decomposed;
	private Map<String,Expression> domainMap;
	private Map<String,Atom> intMap;
	private Map<String,Atom> boolMap;
	private Map<String,IntegerDomain> expDomainMap;
	private Map<String,Predicate> predicateMap;
	private Map<String,Relation> relationMap;
	private Map<Expression,Atom> equivMap;

	public Decomposer() {
		decomposed = new ArrayList<Expression>();
		domainMap = new HashMap<String,Expression>();
		intMap = new HashMap<String,Atom>();
		boolMap = new HashMap<String,Atom>();
		expDomainMap = new HashMap<String,IntegerDomain>();
		predicateMap = new HashMap<String,Predicate>();
		relationMap = new HashMap<String,Relation>();
		equivMap = new EquivMap();
	}

	private void decomposeDomainDefinition(Sequence seq) throws SugarException {
		String name = null;
		Expression domain = null;
		if (seq.matches("WWII")) {
			name = seq.get(1).stringValue();
			int lb = seq.get(2).integerValue();
			int ub = seq.get(3).integerValue();
			Expression[] exps = {Expression.create(lb),
													 Expression.create(ub)};
			domain = Expression.create(Expression.create(exps));
		} else if (seq.matches("WWI")) {
			name = seq.get(1).stringValue();
			int lb = seq.get(2).integerValue();
			Expression[] exps = {Expression.create(lb),
													 Expression.create(lb)};
			domain = Expression.create(Expression.create(exps));
		} else if (seq.matches("WWS")) {
			name = seq.get(1).stringValue();
			domain = (Sequence)seq.get(2);
		} else {
			throw new SugarException("Bad definition " + seq);
		}
		if (domainMap.containsKey(name)) {
			throw new SugarException("Duplicated definition " + seq);
		}
		domainMap.put(name, domain);
	}

	private void decomposeIntDefinition(Sequence seq) throws SugarException {
		List<Expression> ret = new ArrayList<Expression>();
		String name = null;
		Expression domainExp = null;
		IntegerDomain domain = null;
		if (seq.matches("WWW")) {
			name = seq.get(1).stringValue();
			String domainName = seq.get(2).stringValue();
			domainExp = domainMap.get(domainName);
			Expression[] exp = {seq.get(0), seq.get(1), domainExp};
			decomposeIntDefinition(new Sequence(exp));
			return;
		} else if (seq.matches("WWS")) {
			name = seq.get(1).stringValue();
			domainExp = (Sequence)seq.get(2);
			SortedSet<Integer> d = new TreeSet<Integer>();
			Sequence x = (Sequence)seq.get(2);
			if (x.length() == 1) {
				Sequence seq1 = (Sequence)x.get(0);
				if (seq1.matches("II")) {
					int lb = seq1.get(0).integerValue();
					int ub = seq1.get(1).integerValue();
					Expression[] exps = {Expression.create(lb),
															 Expression.create(ub)};
					domainExp = Expression.create(Expression.create(exps));
					domain = new IntegerDomain(lb, ub);
				} else {
					throw new SugarException("Bad definition " + seq);
				}
			} else {
				for (int i = 0; i < x.length(); i++) {
					if (x.get(i).isInteger()) {
						d.add(x.get(i).integerValue());
					} else if (x.get(i).isSequence()) {
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
			}
		} else if (seq.matches("WWII")) {
			name = seq.get(1).stringValue();
			int lb = seq.get(2).integerValue();
			int ub = seq.get(3).integerValue();
			Expression[] exps = {Expression.create(lb),
													 Expression.create(ub)};
			domainExp = Expression.create(Expression.create(exps));
			domain = new IntegerDomain(lb, ub);
		} else if (seq.matches("WWI")) {
			name = seq.get(1).stringValue();
			int lb = seq.get(2).integerValue();
			Expression[] exps = {Expression.create(lb),
													 Expression.create(lb)};
			domainExp = Expression.create(Expression.create(exps));
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
		Expression[] retexps = {seq.get(0), seq.get(1), domainExp};
		Sequence v = new Sequence(retexps);
		intMap.put(name, (Atom)seq.get(1));
		expDomainMap.put(name, domain);
		decomposed.add(v);
	}

	private void decomposeBoolDefinition(Sequence seq) throws SugarException {
		List<Expression> ret = new ArrayList<Expression>();
		String name = null;
		if (seq.matches("WW")) {
			name = seq.get(1).stringValue();
		} else {
			throw new SugarException("Bad definition " + seq);
		}
		if (boolMap.containsKey(name)) {
			throw new SugarException("Duplicated definition " + seq);
		}
		decomposed.add(seq);
		boolMap.put(name, (Atom)seq.get(1));
	}

	private void decomposeObjectiveDefinition(Sequence seq) throws SugarException {
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
		Atom v = intMap.get(name);
		if (v == null) {
			throw new SugarException("Unknown objective variable " + seq);
		}
		decomposed.add(seq);
	}

	private void decomposePredicateDefinition(Sequence seq) throws SugarException {
		if (! seq.matches("WSS")) {
			syntaxError(seq);
		}
		Sequence def = (Sequence)seq.get(1);
		String name = def.get(0).stringValue();
		Sequence body = (Sequence)seq.get(2);
		Predicate pred = new Predicate(def, body);
		predicateMap.put(name, pred);
	}

	private void decomposeRelationDefinition(Sequence seq) throws SugarException {
		if (! seq.matches("WWIS")) {
			syntaxError(seq);
		}
		String name = seq.get(1).stringValue();
		int arity = seq.get(2).integerValue();
		Sequence body = (Sequence)seq.get(3);
		Relation rel = new Relation(name, arity, body);
		relationMap.put(name, rel);
	}

	private void addEquivalence(Atom v, Expression x) {
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

	private Atom newIntegerVariable(IntegerDomain d, Expression x)
	throws SugarException {
		final String name = IAUX_PREFIX + Integer.toString(iidx++);
		final Atom v = (Atom)Expression.create(name);
		final Expression exp = Expression.create(Expression.INT_DEFINITION,
																						 v, d.toExpression());
		exp.setComment(name + " : " + x.toString());
		decomposed.add(exp);
		expDomainMap.put(name, d);
		intMap.put(name, v);
		return v;
	}

	private LinearExpression decomposeInteger(Atom x) throws SugarException {
		return new LinearExpression(x.integerValue());
	}

	private LinearExpression decomposeString(Atom x) throws SugarException {
		if (! intMap.containsKey(x.stringValue())) {
			syntaxError(x);
		}
		return new LinearExpression(x);
	}

	private Atom newBooleanVariable()
		throws SugarException {
		String name = BAUX_PREFIX + Integer.toString(bidx++);
		Atom v = (Atom)Expression.create(name);
		Expression exp = Expression.create(Expression.BOOL_DEFINITION, v);
		decomposed.add(exp);
		boolMap.put(name, v);
		return v;
	}

	private LinearExpression decomposeADD(Sequence seq) throws SugarException {
		LinearExpression e = new LinearExpression(0);
		for (int i = 1; i < seq.length(); i++) {
			LinearExpression ei = decomposeFormula(seq.get(i));
			e.add(ei);
		}
		return e;
	}

	private LinearExpression decomposeSUB(Sequence seq) throws SugarException {
		LinearExpression e = null;
		if (seq.length() == 1) {
			syntaxError(seq);
		} else if (seq.length() == 2) {
			e = decomposeFormula(seq.get(1));
			e.multiply(-1);
		} else {
			e = decomposeFormula(seq.get(1));
			for (int i = 2; i < seq.length(); i++) {
				LinearExpression ei = decomposeFormula(seq.get(i));
				e.subtract(ei);
			}
		}
		return e;
	}

	private LinearExpression decomposeABS(Sequence seq) throws SugarException {
		checkArity(seq, 1);
		Expression x1 = seq.get(1);
		LinearExpression e1 = decomposeFormula(x1);
		IntegerDomain d1 = e1.getDomain(expDomainMap);
		if (d1.getLowerBound() >= 0) {
			return e1;
		} else if (d1.getUpperBound() <= 0) {
			e1.multiply(-1);
			return e1;
		}
		IntegerDomain d = d1.abs();
		Atom x = newIntegerVariable(d, seq);
		Expression eq =
			(x.ge(x1))
			.and(x.ge(x1.neg()))
			.and((x.le(x1)).or(x.le(x1.neg())));
		eq.setComment(x.toString() + " == " + seq);
		decomposeConstraint(eq);
		addEquivalence(x, seq);
		return new LinearExpression((Atom)x);
	}

	private LinearExpression decomposeMUL(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		final Expression x1 = seq.get(1);
		final Expression x2 = seq.get(2);
		final LinearExpression e1 = decomposeFormula(x1);
		final LinearExpression e2 = decomposeFormula(x2);
		final IntegerDomain d1 = e1.getDomain(expDomainMap);
		final IntegerDomain d2 = e2.getDomain(expDomainMap);
		if (d1.size() == 1) {
			e2.multiply(d1.getLowerBound());
			return e2;
		} else if (d2.size() == 1) {
			e1.multiply(d2.getLowerBound());
			return e1;
		} else if (d1.size() <= d2.size()) {
			// 変数同士の乗算に対応するにはこの辺りをいじる．
			// e1 と e2 を変数に置き換える
			// final IntegerDomain newDom = d1.mul(d2);
			// final Expression mulExp = e1.mul
			// Atom newInt = newIntegerVariable(newDom, x1.mul(x2));
			// newInt.eq(x1.mul(x2));
			// return newInt;
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
			return decomposeIF((Sequence)x);
		} else {
			return decomposeMUL((Sequence)x2.mul(x1));
		}
	}

	private LinearExpression decomposeDIV(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		LinearExpression e1 = decomposeFormula(x1);
		LinearExpression e2 = decomposeFormula(x2);
		IntegerDomain d1 = e1.getDomain(expDomainMap);
		IntegerDomain d2 = e2.getDomain(expDomainMap);
		IntegerDomain qd = d1.div(d2);
		IntegerDomain rd = d1.mod(d2);
		Atom q = newIntegerVariable(qd, seq);
		Atom r = newIntegerVariable(rd, x1.mod(x2));
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
			eq.setComment(q.toString() + " == " + seq);
			decomposeConstraint(eq);
			addEquivalence(q, seq);
			return new LinearExpression(q);
		}
		// TODO
		throw new SugarException("Unsupported " + seq);
	}

	private LinearExpression decomposeMOD(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		LinearExpression e1 = decomposeFormula(x1);
		LinearExpression e2 = decomposeFormula(x2);
		IntegerDomain d1 = e1.getDomain(expDomainMap);
		IntegerDomain d2 = e2.getDomain(expDomainMap);
		IntegerDomain qd = d1.div(d2);
		IntegerDomain rd = d1.mod(d2);
		Atom q = newIntegerVariable(qd, seq);
		Atom r = newIntegerVariable(rd, x1.mod(x2));
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
			eq.setComment(q.toString() + " == " + seq);
			decomposeConstraint(eq);
			addEquivalence(r, seq);
			return new LinearExpression(r);
		}
		// TODO
		throw new SugarException("Unsupported " + seq);
	}

	private LinearExpression decomposePOW(Sequence seq) throws SugarException {
		// TODO pow
		throw new SugarException("Unsupported " + seq);
	}

	private LinearExpression decomposeMIN(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		LinearExpression e1 = decomposeFormula(x1);
		LinearExpression e2 = decomposeFormula(x2);
		IntegerDomain d1 = e1.getDomain(expDomainMap);
		IntegerDomain d2 = e2.getDomain(expDomainMap);
		if (d1.getUpperBound() <= d2.getLowerBound()) {
			return e1;
		} else if (d2.getUpperBound() <= d1.getLowerBound()) {
			return e2;
		}
		IntegerDomain d = d1.min(d2);
		Atom x = newIntegerVariable(d, seq);
		Expression eq =
			(x.le(x1))
			.and(x.le(x2))
			.and((x.ge(x1)).or(x.ge(x2)));
		eq.setComment(x.toString() + " == " + seq);
		decomposeConstraint(eq);
		addEquivalence(x, seq);
		return new LinearExpression(x);
	}

	private LinearExpression decomposeMAX(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		LinearExpression e1 = decomposeFormula(x1);
		LinearExpression e2 = decomposeFormula(x2);
		IntegerDomain d1 = e1.getDomain(expDomainMap);
		IntegerDomain d2 = e2.getDomain(expDomainMap);
		if (d1.getUpperBound() <= d2.getLowerBound()) {
			return e2;
		} else if (d2.getUpperBound() <= d1.getLowerBound()) {
			return e1;
		}
		IntegerDomain d = d1.max(d2);
		Atom x = newIntegerVariable(d, seq);
		Expression eq =
			(x.ge(x1))
			.and(x.ge(x2))
			.and((x.le(x1)).or(x.le(x2)));
		eq.setComment(x.toString() + " == " + seq);
		decomposeConstraint(eq);
		addEquivalence(x, seq);
		return new LinearExpression(x);
	}

	private LinearExpression decomposeIF(Sequence seq) throws SugarException {
		checkArity(seq, 3);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		Expression x3 = seq.get(3);
		LinearExpression e2 = decomposeFormula(x2);
		LinearExpression e3 = decomposeFormula(x3);
		IntegerDomain d2 = e2.getDomain(expDomainMap);
		IntegerDomain d3 = e3.getDomain(expDomainMap);
		IntegerDomain d = d2.cup(d3);
		Atom x = newIntegerVariable(d, seq);
		Expression eq =
			((x1.not()).or(x.eq(x2)))
			.and(x1.or(x.eq(x3)));
		eq.setComment(x.toString() + " == " + seq);
		decomposeConstraint(eq);
		addEquivalence(x, seq);
		return new LinearExpression(x);
	}

	protected LinearExpression decomposeFormula(Expression x) throws SugarException {
		LinearExpression e = null;
		Atom v = equivMap.get(x);
		if (v != null) {
			e = new LinearExpression(v);
		} else if (x.isAtom()) {
			if (x.isInteger()) {
				e = decomposeInteger((Atom)x);
			} else {
				e = decomposeString((Atom)x);
			}
		} else {
			if (x.isSequence(Expression.ADD)) {
				e = decomposeADD((Sequence)x);
			} else if (x.isSequence(Expression.NEG) || x.isSequence(Expression.SUB)) {
				e = decomposeSUB((Sequence)x);
			} else if (x.isSequence(Expression.ABS)) {
				e = decomposeABS((Sequence)x);
			} else if (x.isSequence(Expression.MUL)) {
				e = decomposeMUL((Sequence)x);
			} else if (x.isSequence(Expression.DIV)) {
				e = decomposeDIV((Sequence)x);
			} else if (x.isSequence(Expression.MOD)) {
				e = decomposeMOD((Sequence)x);
			} else if (x.isSequence(Expression.POW)) {
				e = decomposePOW((Sequence)x);
			} else if (x.isSequence(Expression.MIN)) {
				e = decomposeMIN((Sequence)x);
			} else if (x.isSequence(Expression.MAX)) {
				e = decomposeMAX((Sequence)x);
			} else if (x.isSequence(Expression.IF)) {
				e = decomposeIF((Sequence)x);
			} else {
				syntaxError(x);
			}
		}
		return e;
	}

	private List<Expression> decomposeComparison(Expression x, Expression op) throws SugarException {
		assert op.equals(Expression.LE)
			|| op.equals(Expression.EQ)
			|| op.equals(Expression.NE);
		LinearExpression e = decomposeFormula(x);
		List<Expression> exps = new ArrayList<Expression>();
		IntegerDomain d = e.getDomain(expDomainMap);
		if (op.equals(Expression.LE)) {
			if (d.getUpperBound() <= 0) {
				return exps;
			}
			if (d.getLowerBound() > 0) {
				exps.add(Expression.create(Expression.OR));
				return exps;
			}
			exps.add(Expression.create(Expression.OR, e.le(0)));
			return exps;
		}else if (op.equals(Expression.EQ)) {
			if (!d.contains(0)) {
				exps.add(Expression.create(Expression.OR));
				return exps;
			}
			exps.add(Expression.create(Expression.OR, e.eq(0)));
			return exps;
		}else if (op.equals(Expression.NE)) {
			if (!d.contains(0)) {
				return exps;
			}
			exps.add(Expression.create(Expression.OR, e.ne(0)));
			return exps;
		}
		throw new SugarException("!!!");
	}

	private List<Expression> decomposeDisj(Sequence seq, boolean negative) throws SugarException {
		List<Expression> exps = null;
		if (seq.length() == 1) {
			exps = new ArrayList<Expression>();
			exps.add(Expression.create(Expression.OR));
		} else if (seq.length() == 2) {
			exps = decomposeConstraint(seq.get(1), negative);
		} else {
			exps = new ArrayList<Expression>();
			List<Expression> lits = new ArrayList<Expression>();
			// clause.setComment(seq.toString());
			for (int i = 1; i < seq.length(); i++) {
				List<Expression> exps0 = decomposeConstraint(seq.get(i), negative);
				if (exps0.size() == 0) {
					return exps0;
				} else if (exps0.size() == 1) {
					Sequence seq0 = (Sequence)exps0.get(0);
					for(int j = 1; j < seq0.length(); j++) {
						lits.add(seq0.get(j));
					}
				} else {
					Atom v = newBooleanVariable();
					// v.setComment(seq.toString());
					Expression v0 = v;
					Expression v1 = v.not();
					lits.add(v0);
					for (Expression exp0 : exps0) {
						exps.add(exp0.or(v1));
					}
				}
			}
			exps.add(Expression.create(Expression.OR, lits));
		}
		return exps;
	}

	private Expression decomposePredicate(Sequence seq) throws SugarException {
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

	private Expression decomposeRelation(Sequence seq) throws SugarException {
		String name = seq.get(0).stringValue();
		Relation rel = relationMap.get(name);
		if (rel == null) {
			throw new SugarException("Undefined relation " + name + " in " + seq);
		}
    // ややこしそうなので後回し．
    throw new SugarException("Not implemented");
		// IntegerVariable[] vs = new IntegerVariable[seq.length() - 1];
		// for (int i = 1; i < seq.length(); i++) {
		// 	IntegerVariable v = intMap.get(seq.get(i).stringValue());
		// 	if (v == null) {
		// 		syntaxError(seq);
		// 	}
		// 	vs[i-1] = v;
		// }
		// return new RelationLiteral(rel.arity, rel.conflicts, rel.tuples, vs);
	}

	private List<Expression> decomposeConstraint(Expression x, boolean negative) throws SugarException {
		List<Expression> exps = null;
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
					exps = new ArrayList<Expression>();
					exps.add(Expression.create(Expression.OR));
					break;
				} else if ((x.equals(Expression.FALSE) && negative)
						|| (x.equals(Expression.TRUE) && ! negative)) {
					exps = new ArrayList<Expression>();
					break;
				} else if (boolMap.containsKey(x.stringValue())) {
					Atom v = boolMap.get(x.stringValue());
					Expression exp = Expression.create(Expression.OR, negative ? v.not() : v);
					exps = new ArrayList<Expression>();
					exps.add(exp);
					break;
				} else {
					syntaxError(x);
				}
			} else {
				Sequence seq = (Sequence)x;
				if (seq.length() == 0) {
					syntaxError(seq);
				} else if (predicateMap.containsKey(seq.get(0).stringValue())) {
					x = decomposePredicate(seq);
				} else if (relationMap.containsKey(seq.get(0).stringValue())) {
					Expression e = decomposeRelation(seq);
					exps = new ArrayList<Expression>();
					exps.add(e);
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
					exps = new ArrayList<Expression>();
					for (int i = 1; i < seq.length(); i++) {
						List<Expression> exps0 = decomposeConstraint(seq.get(i), negative);
						exps.addAll(exps0);
					}
					break;
				} else if ((seq.isSequence(Expression.OR) && ! negative)
						|| (seq.isSequence(Expression.AND) && negative)) {
					exps = decomposeDisj(seq, negative);
					break;
				} else if ((seq.isSequence(Expression.EQ) && ! negative)
									 || (seq.isSequence(Expression.NE) && negative)) {
					checkArity(seq, 2);
					if (seq.get(1).isSequence(Expression.MUL) && seq.get(2).equals(Expression.ZERO)) {
						Expression a1 = ((Sequence)seq.get(1)).get(1);
						Expression a2 = ((Sequence)seq.get(1)).get(2);
						// a1*a2 == 0
						x = (a1.eq(Expression.ZERO)).or(a2.eq(Expression.ZERO));
						continue;
					}
					if (seq.get(2).isSequence(Expression.MUL) && seq.get(1).equals(Expression.ZERO)) {
						Expression a1 = ((Sequence)seq.get(2)).get(1);
						Expression a2 = ((Sequence)seq.get(2)).get(2);
						x = (a1.eq(Expression.ZERO)).or(a2.eq(Expression.ZERO));
						continue;
					}
					exps = decomposeComparison(seq.get(1).sub(seq.get(2)),
																		 Expression.EQ);
					break;
				} else if ((seq.isSequence(Expression.NE) && !negative)
									 || (seq.isSequence(Expression.EQ) && negative)) {
					checkArity(seq, 2);
					if (seq.get(1).isSequence(Expression.MUL) && seq.get(2).equals(Expression.ZERO)) {
						Expression a1 = ((Sequence)seq.get(1)).get(1);
						Expression a2 = ((Sequence)seq.get(1)).get(2);
						// a1*a2 != 0
						x = (a1.ne(Expression.ZERO)).and(a2.ne(Expression.ZERO));
						continue;
					}
					if (seq.get(2).isSequence(Expression.MUL) && seq.get(1).equals(Expression.ZERO)) {
						Expression a1 = ((Sequence)seq.get(2)).get(1);
						Expression a2 = ((Sequence)seq.get(2)).get(2);
						x = (a1.ne(Expression.ZERO)).and(a2.ne(Expression.ZERO));
						continue;
					}
					exps = decomposeComparison(seq.get(1).sub(seq.get(2)),
																		 Expression.NE);
					break;
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
					exps = decomposeComparison(seq.get(1).sub(seq.get(2)),
																		 Expression.LE);
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
					exps = decomposeComparison(seq.get(1).sub(seq.get(2))
																		 .add(Expression.create(1)),
																		 Expression.LE);
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
					exps = decomposeComparison(seq.get(2).sub(seq.get(1)),
																		 Expression.LE);
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
					exps = decomposeComparison(seq.get(2).sub(seq.get(1))
																		 .add(Expression.create(1)),
																		 Expression.LE);
					break;
				} else if (seq.isSequence(Expression.ALLDIFFERENT) && ! negative) {
					x = GlobalConstraints.decomposeAllDifferent(this, seq);
				} else if (seq.isSequence(Expression.WEIGHTEDSUM) && ! negative) {
					x = GlobalConstraints.decomposeWeightedSum(this, seq);
				} else if (seq.isSequence(Expression.CUMULATIVE) && ! negative) {
					x = GlobalConstraints.decomposeCumulative(this, seq);
				} else if (seq.isSequence(Expression.ELEMENT) && ! negative) {
					x = GlobalConstraints.decomposeElement(this, seq);
				} else if (seq.isSequence(Expression.DISJUNCTIVE) && ! negative) {
					x = GlobalConstraints.decomposeDisjunctive(this, seq);
				} else if (seq.isSequence(Expression.LEX_LESS) && ! negative) {
					x = GlobalConstraints.decomposeLex_less(this, seq);
				} else if (seq.isSequence(Expression.LEX_LESSEQ) && ! negative) {
					x = GlobalConstraints.decomposeLex_lesseq(this, seq);
				} else if (seq.isSequence(Expression.NVALUE) && ! negative) {
					x = GlobalConstraints.decomposeNvalue(this, seq);
				} else if (seq.isSequence(Expression.COUNT) && ! negative) {
					x = GlobalConstraints.decomposeCount(this, seq);
				} else if (seq.isSequence(Expression.GLOBAL_CARDINALITY) && ! negative) {
					x = GlobalConstraints.decomposeGlobal_cardinality(this, seq);
				} else if (seq.isSequence(Expression.GLOBAL_CARDINALITY_WITH_COSTS) && ! negative) {
					x = GlobalConstraints.decomposeGlobal_cardinality_with_costs(this, seq);
				} else {
					syntaxError(x);
				}
			}
		}
		return exps;
	}

	private void decomposeConstraint(Expression x) throws SugarException {
		List<Expression> exps = decomposeConstraint(x, false);
		if (exps.size() > 0) {
			if (x.getComment() == null) {
				exps.get(0).setComment(x.toString());
			} else {
				exps.get(0).setComment(x.getComment());
			}
		}
		for (Expression e : exps) {
			decomposed.add(e);
		}
	}

	private void decomposeExpression(Expression x) throws SugarException {
		if (x.isSequence(Expression.DOMAIN_DEFINITION)) {
			decomposeDomainDefinition((Sequence)x);
		} else if (x.isSequence(Expression.INT_DEFINITION)) {
			decomposeIntDefinition((Sequence)x);
		} else if (x.isSequence(Expression.BOOL_DEFINITION)) {
			decomposeBoolDefinition((Sequence)x);
		} else if (x.isSequence(Expression.OBJECTIVE_DEFINITION)) {
			decomposeObjectiveDefinition((Sequence)x);
		} else if (x.isSequence(Expression.PREDICATE_DEFINITION)) {
			decomposePredicateDefinition((Sequence)x);
		} else if (x.isSequence(Expression.RELATION_DEFINITION)) {
			decomposeRelationDefinition((Sequence)x);
		} else {
			decomposeConstraint(x);
		}
	}

	public List<Expression> decompose(List<Expression> expressions) throws SugarException {
		int n = expressions.size();
		int percent = 10;
		int count = 0;
		for (Expression x : expressions) {
			decomposeExpression(x);
			count++;
			if ((100*count)/n >= percent) {
				Logger.fine("converted " + count + " (" + percent + "%) expressions");
				percent += 10;
			}
		}
		return decomposed;
	}

	public Map<String,IntegerDomain> getDomMap() {
		return expDomainMap;
	}

	private static List<Expression> readExpression(String cspFileName)
		throws SugarException, IOException {
		Logger.fine("Parsing " + cspFileName);
		InputStream in = new FileInputStream(cspFileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		Parser parser = new Parser(reader, false);
		List<Expression> expressions = parser.parse();
		Logger.info("parsed " + expressions.size() + " expressions");
		Logger.status();
		return expressions;
	}

	public static void main(String[] args)
		throws SugarException, IOException {
		int i = 0;
		while (i < args.length) {
			if (args[i].equals("-v") || args[i].equals("-verbose")) {
				Logger.verboseLevel++;
			}else{
				break;
			}
			i++;
		}
		int n = args.length - i;
		if (n != 2) {
			System.out.println("Usage : java Decomposer [-v] inputCSPFile outCSPFile");
			System.exit(1);
		}
		String cspFileName = args[i];
		String outFileName = args[i+1];

		Decomposer dec = new Decomposer();
		List<Expression> exps = readExpression(cspFileName);
		exps = dec.decompose(exps);
		File file = new File(outFileName);
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
		for(Expression e : exps) {
			pw.println(e);
		}
		pw.close();
	}
}
