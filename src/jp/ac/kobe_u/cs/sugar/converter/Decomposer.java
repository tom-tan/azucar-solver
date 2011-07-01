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

public class Decomposer {
	public static int MAX_EQUIVMAP_SIZE = 1000;
	public static long MAX_LINEARSUM_SIZE = 1024L;
	public static boolean expandABS = true;
	public static boolean OPT_PIGEON = true;
	public static boolean INCREMENTAL_PROPAGATE = true;
	public static boolean ESTIMATE_SATSIZE = false; // bad
    public static boolean NEW_VARIABLE = true;
    public static int SPLITS = 2;
	
	private class EquivMap extends LinkedHashMap<Expression,Expression> {

		private static final long serialVersionUID = -4882267868872050198L;

		EquivMap() {
			super(100, 0.75f, true);
		}

		/* (non-Javadoc)
		 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
		 */
		@Override
		protected boolean removeEldestEntry(Entry<Expression, Expression> eldest) {
			return size() > MAX_EQUIVMAP_SIZE;
		}
		
	}
	
	// private CSP csp;
	private Map<String,Expression> domainMap;
	private Map<String,Expression> intMap;
	private Map<String,Expression> boolMap;
	private Map<String,Predicate> predicateMap;
	private Map<String,Relation> relationMap;
	private Map<Expression,Expression> equivMap;
	private List<Expression> extra;
	
	public Decomposer(CSP csp) {
	// 	this.csp = csp;
		domainMap = new HashMap<String,Expression>();
		intMap = new HashMap<String,Expression>();
		boolMap = new HashMap<String,Expression>();
		predicateMap = new HashMap<String,Predicate>();
		relationMap = new HashMap<String,Relation>();
		equivMap = new EquivMap();
	// 	extra = new ArrayList<Expression>();
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
      domain = Expression.create(exps);
		} else if (seq.matches("WWI")) {
			name = seq.get(1).stringValue();
			int lb = seq.get(2).integerValue();
      Expression[] exps = {Expression.create(lb),
                           Expression.create(lb)};
      domain = Expression.create(exps);
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

	private List<Expression> decomposeIntDefinition(Sequence seq) throws SugarException {
    List<Expression> ret = new ArrayList<Expression>();
		String name = null;
    Expression domain = null;
		if (seq.matches("WWW")) {
			name = seq.get(1).stringValue();
			String domainName = seq.get(2).stringValue();
			domain = domainMap.get(domainName);
		} else if (seq.matches("WWS")) {
			name = seq.get(1).stringValue();
      domain = (Sequence)seq.get(2);
		} else if (seq.matches("WWII")) {
			name = seq.get(1).stringValue();
			int lb = seq.get(2).integerValue();
			int ub = seq.get(3).integerValue();
      Expression[] exps = {Expression.create(lb),
                           Expression.create(ub)};
      domain = new Sequence(exps);
		} else if (seq.matches("WWI")) {
			name = seq.get(1).stringValue();
			int lb = seq.get(2).integerValue();
      Expression[] exps = {Expression.create(lb),
                           Expression.create(lb)};
      domain = new Sequence(exps);
		} else {
			throw new SugarException("Bad definition " + seq);
		}
		if (domain == null) {
			throw new SugarException("Unknown domain " + seq);
		}
		if (intMap.containsKey(name)) {
			throw new SugarException("Duplicated definition " + seq);
		}
    Expression[] retexps = {seq.get(1),
                            Expression.create(name),
                            domain};
    Sequence v = new Sequence(retexps);
		intMap.put(name, v);
    ret.add(v);
    return ret;
	}

	private List<Expression> decomposeBoolDefinition(Sequence seq) throws SugarException {
    List<Expression> ret = new ArrayList<Expression>();
		String name = null;
		if (seq.matches("WW")) {
      // do nothing
		} else {
			throw new SugarException("Bad definition " + seq);
		}
		if (boolMap.containsKey(name)) {
			throw new SugarException("Duplicated definition " + seq);
		}
		boolMap.put(name, seq);
    ret.add(seq);
    return ret;
	}

	private List<Expression> decomposeObjectiveDefinition(Sequence seq) throws SugarException {
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
		Expression v = intMap.get(name);
		if (v == null) {
			throw new SugarException("Unknown objective variable " + seq);
		}
    List<Expression> ret = new ArrayList<Expression>();
    ret.add(seq);
    return ret;
		// csp.setObjectiveVariable(v);
		// csp.setObjective(objective);
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

	private void addEquivalence(Expression v, Expression x) {
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

	private Expression newIntegerVariable(Expression d, Expression x)
	throws SugarException {
    final String AUX_PREFIX = "$Idec";
    /*static*/ int idx = 0;
    String name = AUX_PREFIX + Integer.toString(idx++);
    Expression v = Expression.create(name);
    Expression exp = Expression.create(Expression.INT_DEFINITION,
                                       v, d);
    //IntegerVariable v = new IntegerVariable(d);
		//csp.add(v);
		exp.setComment(name + " : " + x.toString());
    // v を intMap に add
		return v;
	}
	
	private Expression toIntegerVariable(//LinearSum e,
                                       Expression e,
                                       Expression x)
	throws SugarException {
    Expression v;
		//IntegerVariable v;
		// if (e.isIntegerVariable()) {
    if (((Sequence)e).matches("W")) {
			v = e;//e.getCoef().firstKey();
		} else {
			v = newIntegerVariable(e.getDomain(), x);
			Expression eq = v.eq(x);
			eq.setComment(v.toString() + " == " + x);
			decomposeConstraint(eq);
			addEquivalence(v, x);
		}
		return v;
	}

	private Expression newBooleanVariable()
    throws SugarException {
    final String AUX_PREFIX = "$Bdec";
    /*static*/ int idx = 0;
    String name = AUX_PREFIX + Integer.toString(idx++);
    Expression v = Expression.create(name);
    Expression exp = Expression.create(Expression.BOOL_DEFINITION, v);
    //IntegerVariable v = new IntegerVariable(d);
		//csp.add(v);
		//exp.setComment(name + " : " + x.toString());
    // v を intMap に add
		return v;
	}

	// private LinearSum decomposeInteger(Atom x) throws SugarException {
	// 	return new LinearSum(x.integerValue());
	// }
	
	// private LinearSum decomposeString(Atom x) throws SugarException {
	// 	String s = x.stringValue();
	// 	if (csp.getIntegerVariable(s) == null) {
	// 		syntaxError(x);
	// 	}
	// 	IntegerVariable v = csp.getIntegerVariable(s);
	// 	return new LinearSum(v);
	// }
	
	private Expression decomposeADD(Sequence seq) throws SugarException {
		//LinearSum e = new LinearSum(0);
    List<Expression> forms = new ArrayList<Expression>();
		for (int i = 1; i < seq.length(); i++) {
			forms.add(decomposeFormula(seq.get(i)));
			//e.add(ei);
		}
		return Expression.add(forms);
	}
	
	private Expression decomposeSUB(Sequence seq) throws SugarException {
		//LinearSum e = null;
    Expression e = null;
		if (seq.length() == 1) {
			syntaxError(seq);
		} else if (seq.length() == 2) {
			e = decomposeFormula(seq.get(1));
			e = e.mul(-1);
		} else {
			e = decomposeFormula(seq.get(1));
			for (int i = 2; i < seq.length(); i++) {
				Expression ei = decomposeFormula(seq.get(i));
				e = e.sub(ei);
			}
		}
		return e;
	}
	
	private Expression decomposeABS(Sequence seq) throws SugarException {
		checkArity(seq, 1);
		Expression x1 = seq.get(1);
		Expression e1 = decomposeFormula(x1);
		//IntegerDomain d1 = e1.getDomain();
    IntegerDomain d1 = e1.getDomain();
		if (d1.getLowerBound() >= 0) {
			return e1;
		} else if (d1.getUpperBound() <= 0) {
			e1 = e1.mul(-1);
			return e1;
		}
		IntegerDomain d = d1.abs();
		Expression x = newIntegerVariable(d, seq);
		//Expression x = Expression.create(v.getName());
		Expression eq =
			(x.ge(e1))
			.and(x.ge(e1.neg()))
			.and((x.le(e1)).or(x.le(e1.neg())));
		eq.setComment(x.toString() + " == " + seq);
		decomposeConstraint(eq);
		addEquivalence(x, seq);
		return x;
	}
	
	private Expression decomposeMUL(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		// LinearSum e1 = decomposeFormula(x1);
		// LinearSum e2 = decomposeFormula(x2);
		Expression e1 = decomposeFormula(x1);
		Expression e2 = decomposeFormula(x2);
		IntegerDomain d1 = e1.getDomain();
		IntegerDomain d2 = e2.getDomain();
		if (d1.size() == 1) {
			e2 = e2.mul(d1.getLowerBound());
			return e2;
		} else if (d2.size() == 1) {
			e1 = e1.mul(d2.getLowerBound());
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
			return decomposeIF((Sequence)x);
		} else {
			return decomposeMUL((Sequence)x2.mul(x1));
		}
	}
	
	private Expression decomposeDIV(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		// LinearSum e1 = decomposeFormula(x1);
		// LinearSum e2 = decomposeFormula(x2);
		Expression e1 = decomposeFormula(x1);
		Expression e2 = decomposeFormula(x2);
		IntegerDomain d1 = e1.getDomain();
		IntegerDomain d2 = e2.getDomain();
		IntegerDomain qd = d1.div(d2);
		IntegerDomain rd = d1.mod(d2);
		// IntegerVariable qv = newIntegerVariable(qd, seq);
		// IntegerVariable rv = newIntegerVariable(rd, x1.mod(x2));
		Expression q = newIntegerVariable(qd, seq);
		Expression r = newIntegerVariable(rd, x1.mod(x2));
		// Expression q = Expression.create(qv.getName());
		// Expression r = Expression.create(rv.getName());
		Expression px = x2.mul(q);
		if (d2.size() == 1) {
			int value2 = d2.getLowerBound();
			if (value2 == 1) {
				return e1;
			} else if (value2 == -1) {
				e1 = e1.mul(-1);
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
			return q;//new LinearSum(qv);
		}
		// TODO
		if (true) {
			throw new SugarException("Unsupported " + seq);
		}
		// IntegerVariable v2 = toIntegerVariable(e2, x2);
		// IntegerDomain pd = d1.sub(rd);
		// IntegerVariable pv = newIntegerVariable(pd, px);
		// Clause clause = new Clause(new ProductLiteral(pv, qv, v2));
		// clause.setComment(pv.getName() + " == " + px);
		// csp.add(clause);
		// Expression p = Expression.create(pv.getName());
		// Expression eq =
		// 	(x1.eq(p.add(r)))
		// 	.and((r.ge(Expression.ZERO)).and((x2.abs()).gt(r)));
		// eq.setComment(qv.getName() + " == " + seq);
		// decomposeConstraint(eq);
		// addEquivalence(qv, seq);
		// return new LinearSum(qv);
	}
	
	private Expression decomposeMOD(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		// LinearSum e1 = decomposeFormula(x1);
		// LinearSum e2 = decomposeFormula(x2);
		Expression e1 = decomposeFormula(x1);
		Expression e2 = decomposeFormula(x2);
		IntegerDomain d1 = e1.getDomain();
		IntegerDomain d2 = e2.getDomain();
		IntegerDomain qd = d1.div(d2);
		IntegerDomain rd = d1.mod(d2);
		// IntegerVariable qv = newIntegerVariable(qd, seq);
		// IntegerVariable rv = newIntegerVariable(rd, x1.mod(x2));
		Expression q = newIntegerVariable(qd, seq);
		Expression r = newIntegerVariable(rd, x1.mod(x2));
		// Expression q = Expression.create(qv.getName());
		// Expression r = Expression.create(rv.getName());
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
			return r;//new LinearSum(rv);
		}
		// TODO
		if (true) {
			throw new SugarException("Unsupported " + seq);
		}
		// IntegerVariable v2 = toIntegerVariable(e2, x2);
		// IntegerDomain pd = d1.sub(rd);
		// IntegerVariable pv = newIntegerVariable(pd, px);
		// Clause clause = new Clause(new ProductLiteral(pv, qv, v2));
		// clause.setComment(pv.getName() + " == " + px);
		// csp.add(clause);
		// Expression p = Expression.create(pv.getName());
		// Expression eq =
		// 	(x1.eq(p.add(r)))
		// 	.and((r.ge(Expression.ZERO)).and((x2.abs()).gt(r)));
		// eq.setComment(rv.getName() + " == " + seq);
		// decomposeConstraint(eq);
		// addEquivalence(rv, seq);
		// return new LinearSum(rv);
	}
	
	private Expression decomposePOW(Sequence seq) throws SugarException {
		// TODO pow
		throw new SugarException("Unsupported " + seq);
	}
	
	private Expression decomposeMIN(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		// LinearSum e1 = decomposeFormula(x1);
		// LinearSum e2 = decomposeFormula(x2);
		Expression e1 = decomposeFormula(x1);
		Expression e2 = decomposeFormula(x2);
		IntegerDomain d1 = e1.getDomain();
		IntegerDomain d2 = e2.getDomain();
		if (d1.getUpperBound() <= d2.getLowerBound()) {
			return e1;
		} else if (d2.getUpperBound() <= d1.getLowerBound()) {
			return e2;
		}
		IntegerDomain d = d1.min(d2);
		// IntegerVariable v = newIntegerVariable(d, seq);
		Expression x = newIntegerVariable(d, seq);
		//Expression x = Expression.create(v.getName());
		Expression eq =
			(x.le(x1))
			.and(x.le(x2))
			.and((x.ge(x1)).or(x.ge(x2)));
		eq.setComment(x.toString() + " == " + seq);
		decomposeConstraint(eq);
		addEquivalence(x, seq);
		return x;//new LinearSum(v);
	}
	
	private Expression decomposeMAX(Sequence seq) throws SugarException {
		checkArity(seq, 2);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		// LinearSum e1 = decomposeFormula(x1);
		// LinearSum e2 = decomposeFormula(x2);
		Expression e1 = decomposeFormula(x1);
		Expression e2 = decomposeFormula(x2);
		IntegerDomain d1 = e1.getDomain();
		IntegerDomain d2 = e2.getDomain();
		if (d1.getUpperBound() <= d2.getLowerBound()) {
			return e2;
		} else if (d2.getUpperBound() <= d1.getLowerBound()) {
			return e1;
		}
		IntegerDomain d = d1.max(d2);
		// IntegerVariable v = newIntegerVariable(d, seq);
		Expression x = newIntegerVariable(d, seq);
		// Expression x = Expression.create(v.getName());
		Expression eq =
			(x.ge(x1))
			.and(x.ge(x2))
			.and((x.le(x1)).or(x.le(x2)));
		eq.setComment(x.toString() + " == " + seq);
		decomposeConstraint(eq);
		addEquivalence(x, seq);
		return x;//new LinearSum(v);
	}
	
	private Expression decomposeIF(Sequence seq) throws SugarException {
		checkArity(seq, 3);
		Expression x1 = seq.get(1);
		Expression x2 = seq.get(2);
		Expression x3 = seq.get(3);
		// LinearSum e2 = decomposeFormula(x2);
		// LinearSum e3 = decomposeFormula(x3);
		Expression e2 = decomposeFormula(x2);
		Expression e3 = decomposeFormula(x3);
		IntegerDomain d2 = e2.getDomain();
		IntegerDomain d3 = e3.getDomain();
		IntegerDomain d = d2.cup(d3);
		//IntegerVariable v = newIntegerVariable(d, seq);
		Expression x = newIntegerVariable(d, seq);
		//Expression x = Expression.create(v.getName());
		Expression eq =
			((x1.not()).or(x.eq(x2)))
			.and(x1.or(x.eq(x3)));
		eq.setComment(x.toString() + " == " + seq);
		decomposeConstraint(eq);
		addEquivalence(x, seq);
		return x;//new LinearSum(v);
	}
	
	protected Expression decomposeFormula(Expression x) throws SugarException {
		//LinearSum e = null;
		Expression e = null;
		Expression v = equivMap.get(x);
		if (v != null) {
			e = v;//new LinearSum(v);
		} else if (x.isAtom()) {
			if (x.isInteger()) {
				e = v;//decomposeInteger((Atom)x);
			} else {
				e = v;//decomposeString((Atom)x);
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
	
// 	private LinearSum simplifyLinearSum(LinearSum e) throws SugarException {
// 		if (e.size() <= 3) {
// 			return e;
// 		}
// 		IntegerVariable[] vs = e.getVariablesSorted();
// 		LinearSum e1 = new LinearSum(0);
// 		for (int i = 2; i < vs.length; i++) {
// 			e1.setA(e.getA(vs[i]), vs[i]);
// 		}
// 		int factor = e1.factor();
// 		if (factor > 1) {
// 			e1.divide(factor);
// 		}
// 		// v == (a[2]*vs[2] + a[3]*vs[3] + ... + a[n]*vs[n]) / factor
// 		IntegerVariable v = new IntegerVariable(e1.getDomain());
// 		v.setComment(v.getName() + " : " + e1);
// 		csp.add(v);
// 		Expression x = Expression.create(v.getName());
// 		Expression ex = e1.toExpression();
// 		Expression eq = x.eq(ex);
// 		// eq.setComment(v.getName() + " == " + e1);
// 		// XXX 
// 		// convertConstraint(eq);
// 		extra.add(eq);
// 		// e0 = b + a[0]*vs[0] + a[1]*vs[1] + factor*v
// 		LinearSum e0 = new LinearSum(e.getB());
// 		e0.setA(e.getA(vs[0]), vs[0]);
// 		e0.setA(e.getA(vs[1]), vs[1]);
// 		e0.setA(factor, v);
// //		System.out.println(e + " ==> " + e0 + " with " + eq);
// 		return e0;
// 	}
	
	// private LinearSum simplifyLinearExpression(LinearSum e, boolean first) throws SugarException {
	// 	if (ESTIMATE_SATSIZE) {
	// 		if (e.satSizeLE(MAX_LINEARSUM_SIZE)) {
	// 			return e;
	// 		}
	// 	} else {
	// 		if (e.size() <= 1) {
	// 			return e;
	// 		}
	// 	}
	// 	int b = e.getB();
	// 	LinearSum[] es = e.split(first ? 3 : SPLITS);
	// 	e = new LinearSum(b);
	// 	for (int i = 0; i < es.length; i++) {
	// 		LinearSum ei = es[i];
	// 		int factor = ei.factor();
	// 		if (factor > 1) {
	// 			ei.divide(factor);
	// 		}
	// 		ei = simplifyLinearExpression(ei, false);
	// 		// System.out.println(es[i] + " ==> " + ei);
	// 		if (ei.size() > 1) {
	// 			IntegerVariable v = new IntegerVariable(ei.getDomain());
	// 			v.setComment(v.getName() + " : " + ei);
	// 			csp.add(v);
	// 			Expression x = Expression.create(v.getName());
	// 			Expression ex = ei.toExpression();
	// 			Expression eq = x.eq(ex);
	// 			eq.setComment(v.getName() + " == " + ei);
	// 			convertConstraint(eq);
	// 			ei = new LinearSum(v);
	// 		}
	// 		if (factor > 1) {
	// 			ei.multiply(factor);
	// 		}
	// 		e.add(ei);
	// 	}
	// 	return e;
	// }
	
	private List<Expression> decomposeComparison(Expression x) throws SugarException {
		Expression e = decomposeFormula(x);
		//e.factorize();
		List<Expression> exps = new ArrayList<Expression>();
		IntegerDomain d = e.getDomain();
		if (d.getUpperBound() <= 0) {
			return exps;
		}
		if (d.getLowerBound() > 0) {
			//clauses.add(new Clause());
      exps.add(Expression.create(Expression.OR));
			return exps;
		}
		// if (NEW_VARIABLE && e.size() > 3) {
		// 	if (ESTIMATE_SATSIZE) {
		// 		e = simplifyLinearExpression(e, true);
		// 	} else {
		// 		e = simplifyLinearExpression(e, true);
		// 	}
		// }
		//clauses.add(new Clause(new LinearLiteral(e)));
    exps.add(Expression.create(Expression.OR,
                               e.le(0)));
		return exps;
	}
	// ここまで
	private List<Expression> decomposeDisj(Sequence seq, boolean negative) throws SugarException {
		List<Expression> exps = null;
		if (seq.length() == 1) {
			//clauses = new ArrayList<Clause>();
			//clauses.add(new Clause());
      exps = new ArrayList<Expression>();
      exps.add(Expression.create(Expression.OR));
		} else if (seq.length() == 2) {
			exps = decomposeConstraint(seq.get(1), negative);
		} else {
			exps = new ArrayList<Expression>();
			Expression exp = Expression.create(Expression.OR);
			// clause.setComment(seq.toString());
			exps.add(exp);
			for (int i = 1; i < seq.length(); i++) {
				List<Expression> exps0 = decomposeConstraint(seq.get(i), negative);
				if (exps0.size() == 0) {
					return exps0;
				} else if (exps0.size() == 1) {
					exp = exp.or(exps0.get(0)); // これではまずい
				} else {
					Expression v = newBooleanVariable();
					//csp.add(v);
					// v.setComment(seq.toString());
					Expression v0 = v;//new BooleanLiteral(v, false);
					Expression v1 = v.not();//new BooleanLiteral(v, true);
					//clause.add(v0);
          exp = exp.or(v0);
					for (Expression exp0 : exps0) {
						exp0.or(v1); // exps0 に反映されていない!
					}
					exps.addAll(exps0);
				}
			}
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
					Expression v = boolMap.get(x.stringValue());
					Expression exp = Expression.create(Expression.OR,
                                             negative ? v.not() : v);//new Clause(new BooleanLiteral(v, negative));
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
					exps = decomposeComparison(seq.get(1).sub(seq.get(2)));
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
					exps = decomposeComparison(seq.get(2).sub(seq.get(1)));
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
                                     .add(Expression.create(1)));
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

	private List<Expression> decomposeConstraint(Expression x) throws SugarException {
		List<Expression> exps = decomposeConstraint(x, false);
		if (exps.size() > 0) {
			if (x.getComment() == null) {
				exps.get(0).setComment(x.toString());
			} else {
				exps.get(0).setComment(x.getComment());
			}
		}
		// for (Expression clause : clauses) {
		// 	csp.add(clause);
		// 	if (INCREMENTAL_PROPAGATE) {
		// 		clause.propagate();
		// 	}
		// }
    return exps;
	}

	private List<Expression> decomposeExpression(Expression x) throws SugarException {
		if (x.isSequence(Expression.DOMAIN_DEFINITION)) {
			decomposeDomainDefinition((Sequence)x);
		} else if (x.isSequence(Expression.INT_DEFINITION)) {
			return decomposeIntDefinition((Sequence)x);
		} else if (x.isSequence(Expression.BOOL_DEFINITION)) {
			return decomposeBoolDefinition((Sequence)x);
		} else if (x.isSequence(Expression.OBJECTIVE_DEFINITION)) {
			return decomposeObjectiveDefinition((Sequence)x);
		} else if (x.isSequence(Expression.PREDICATE_DEFINITION)) {
			decomposePredicateDefinition((Sequence)x);
		} else if (x.isSequence(Expression.RELATION_DEFINITION)) {
			decomposeRelationDefinition((Sequence)x);
		} else {
			return decomposeConstraint(x);
		}
    assert(false);
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
		while (extra.size() > 0) {
			Expression x = extra.remove(0);
			decomposeExpression(x);
			count++;
			if (count % 1000 == 0) {
				Logger.fine("converted " + count + " extra expressions, remaining " + extra.size());
			}
		}
	}
	
}
