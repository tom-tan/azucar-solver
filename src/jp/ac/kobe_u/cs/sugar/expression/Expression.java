package jp.ac.kobe_u.cs.sugar.expression;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.SugarConstants;

/**
 * This is an abstract class for expressions.
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public abstract class Expression implements Comparable<Expression> {
	public static boolean intern = false;
	// public static final int MAX_MAP_SIZE = 100000;
	public static final int MAX_MAP_SIZE = 10000;
	private static HashMap<Expression,Expression> map =
		new HashMap<Expression,Expression>();
	public static final Expression DOMAIN_DEFINITION =
		create(SugarConstants.DOMAIN_DEFINITION);
	public static final Expression INT_DEFINITION =
		create(SugarConstants.INT_DEFINITION);
	public static final Expression BOOL_DEFINITION =
		create(SugarConstants.BOOL_DEFINITION);
	public static final Expression PREDICATE_DEFINITION =
		create(SugarConstants.PREDICATE_DEFINITION);
	public static final Expression RELATION_DEFINITION =
		create(SugarConstants.RELATION_DEFINITION);
	public static final Expression OBJECTIVE_DEFINITION =
		create(SugarConstants.OBJECTIVE_DEFINITION);
	public static final Expression MINIMIZE =
		create(SugarConstants.MINIMIZE);
	public static final Expression MAXIMIZE =
		create(SugarConstants.MAXIMIZE);
	public static final Expression SUPPORTS =
		create(SugarConstants.SUPPORTS);
	public static final Expression CONFLICTS =
		create(SugarConstants.CONFLICTS);
	public static final Expression FALSE =
		create(SugarConstants.FALSE);
	public static final Expression TRUE =
		create(SugarConstants.TRUE);
	public static final Expression NOT =
		create(SugarConstants.NOT);
	public static final Expression AND =
		create(SugarConstants.AND);
	public static final Expression OR =
		create(SugarConstants.OR);
	public static final Expression IMP =
		create(SugarConstants.IMP);
	public static final Expression XOR =
		create(SugarConstants.XOR);
	public static final Expression IFF =
		create(SugarConstants.IFF);
	public static final Expression EQ =
		create(SugarConstants.EQ);
	public static final Expression NE =
		create(SugarConstants.NE);
	public static final Expression LE =
		create(SugarConstants.LE);
	public static final Expression LT =
		create(SugarConstants.LT);
	public static final Expression GE =
		create(SugarConstants.GE);
	public static final Expression GT =
		create(SugarConstants.GT);
	public static final Expression NEG =
		create(SugarConstants.NEG);
	public static final Expression ABS =
		create(SugarConstants.ABS);
	public static final Expression ADD =
		create(SugarConstants.ADD);
	public static final Expression SUB =
		create(SugarConstants.SUB);
	public static final Expression MUL =
		create(SugarConstants.MUL);
	public static final Expression DIV =
		create(SugarConstants.DIV);
	public static final Expression MOD =
		create(SugarConstants.MOD);
	public static final Expression POW =
		create(SugarConstants.POW);
	public static final Expression MIN =
		create(SugarConstants.MIN);
	public static final Expression MAX =
		create(SugarConstants.MAX);
	public static final Expression IF =
		create(SugarConstants.IF);
	public static final Expression ALLDIFFERENT =
		create(SugarConstants.ALLDIFFERENT);
	public static final Expression WEIGHTEDSUM =
		create(SugarConstants.WEIGHTEDSUM);
	public static final Expression CUMULATIVE =
		create(SugarConstants.CUMULATIVE);
	public static final Expression ELEMENT =
		create(SugarConstants.ELEMENT);
	public static final Expression DISJUNCTIVE =
		create(SugarConstants.DISJUNCTIVE);
	public static final Expression LEX_LESS =
		create(SugarConstants.LEX_LESS);
	public static final Expression LEX_LESSEQ =
		create(SugarConstants.LEX_LESSEQ);
	public static final Expression NVALUE =
		create(SugarConstants.NVALUE);
	public static final Expression COUNT =
		create(SugarConstants.COUNT);
	public static final Expression GLOBAL_CARDINALITY =
		create(SugarConstants.GLOBAL_CARDINALITY);
	public static final Expression GLOBAL_CARDINALITY_WITH_COSTS =
		create(SugarConstants.GLOBAL_CARDINALITY_WITH_COSTS);
	
	public static final Expression NIL =
		create(SugarConstants.NIL);
	public static final Expression ZERO =
		intern(new Atom(0L));
	public static final Expression ONE =
		intern(new Atom(1L));
	private String comment = null;

	private static Expression intern(Expression x) {
		if (intern) {
			if (! map.containsKey(x)) {
				if (map.size() < MAX_MAP_SIZE) {
					map.put(x, x);
				}
			} else {
				// System.out.println("Found " + x);
			}
		}
		return x;
	}
	
	public static Expression create(long i) {
		if (i == 0) {
			return ZERO;
		} else if (i == 1) {
			return ONE;
		} else {
			return intern(new Atom(i));
		}
	}

	public static Expression create(String token) {
		return intern(new Atom(token));
	}

	public static Expression create(Expression[] expressions) {
		return intern(new Sequence(expressions));
	}

	public static Expression create(List<Expression> expressions) {
		return intern(new Sequence(expressions));
	}

	public static Expression create(Expression x0) {
		return create(new Expression[] { x0 });
	}

	public static Expression create(Expression x0, Expression x1) {
		return create(new Expression[] { x0, x1 });
	}

	public static Expression create(Expression x0, Expression x1, Expression x2) {
		return create(new Expression[] { x0, x1, x2 });
	}

	public static Expression create(Expression x0, Expression x1, Expression x2, Expression x3) {
		return create(new Expression[] { x0, x1, x2, x3 });
	}

	public static Expression create(Expression x0, Expression x1, Expression x2, Expression x3, Expression x4) {
		return create(new Expression[] { x0, x1, x2, x3, x4 });
	}

	public static Expression create(Expression x, Expression[] xs) {
		Expression[] xs0 = new Expression[xs.length + 1];
		int i = 0;
		xs0[i++] = x;
		for (Expression x0 : xs) {
			xs0[i++] = x0;
		}
		return create(xs0);
	}

	public static Expression create(Expression x, List<Expression> xs) {
		Expression[] xs0 = new Expression[xs.size() + 1];
		int i = 0;
		xs0[i++] = x;
		for (Expression x0 : xs) {
			xs0[i++] = x0;
		}
		return create(xs0);
	}

	public static void clear() {
		map.clear();
	}

	public boolean isAtom() {
		return false;
	}

	public boolean isString() {
		return false;
	}

	public boolean isString(String s) {
		return false;
	}

	public boolean isInteger() {
		return false;
	}

	public boolean isSequence() {
		return false;
	}

	public boolean isSequence(Expression x) {
		return false;
	}

	public boolean isSequence(int arity) {
		return false;
	}

	public String stringValue() {
		return null;
	}

	public Long integerValue() {
		return null;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Expression not() {
		return create(NOT, this);
	}

	public Expression and(Expression x) {
		return create(AND, this, x);
	}

	public Expression or(Expression x) {
		return create(OR, this, x);
	}

	public Expression imp(Expression x) {
		return create(IMP, this, x);
	}

	public Expression xor(Expression x) {
		return create(XOR, this, x);
	}

	public Expression iff(Expression x) {
		return create(IFF, this, x);
	}

	public Expression eq(Expression x) {
		return create(EQ, this, x);
	}

	public Expression eq(long x) {
		return eq(create(x));
	}

	public Expression ne(Expression x) {
		return create(NE, this, x);
	}

	public Expression ne(long x) {
		return ne(create(x));
	}

	public Expression le(Expression x) {
		return create(LE, this, x);
	}

	public Expression le(long x) {
		return le(create(x));
	}

	public Expression lt(Expression x) {
		return create(LT, this, x);
	}

	public Expression lt(long x) {
		return lt(create(x));
	}

	public Expression ge(Expression x) {
		return create(GE, this, x);
	}

	public Expression ge(long x) {
		return ge(create(x));
	}

	public Expression gt(Expression x) {
		return create(GT, this, x);
	}

	public Expression gt(long x) {
		return gt(create(x));
	}

	public Expression neg() {
		return create(NEG, this);
	}

	public Expression abs() {
		return create(ABS, this);
	}

	public Expression add(Expression x) {
		return create(ADD, this, x);
	}

	public Expression add(long x) {
		return add(create(x));
	}

	public static Expression add(Expression[] xs) {
		return create(ADD, xs);
	}

	public static Expression add(List<Expression> xs) {
		return create(ADD, xs);
	}

	public Expression sub(Expression x) {
		return create(SUB, this, x);
	}

	public Expression sub(long x) {
		return sub(create(x));
	}

	public Expression mul(Expression x) {
		return create(MUL, this, x);
	}

	public Expression mul(long x) {
		return mul(create(x));
	}

	public Expression div(Expression x) {
		return create(DIV, this, x);
	}

	public Expression div(int x) {
		return div(create(x));
	}

	public Expression mod(Expression x) {
		return create(MOD, this, x);
	}

	public Expression mod(long x) {
		return mod(create(x));
	}

	public Expression min(Expression x) {
		return create(MIN, this, x);
	}

	public Expression min(long x) {
		return min(create(x));
	}

	public Expression max(Expression x) {
		return create(MAX, this, x);
	}

	public Expression max(long x) {
		return max(create(x));
	}

	public Expression ifThenElse(Expression x, Expression y) {
		return create(IF, this, x, y);
	}

	public Expression ifThenElse(long x, Expression y) {
		return create(IF, this, create(x), y);
	}

	public Expression ifThenElse(Expression x, long y) {
		return create(IF, this, x, create(y));
	}

	public Expression ifThenElse(long x, long y) {
		return create(IF, this, create(x), create(y));
	}

	public static Expression alldifferent(Expression x) {
		return create(ALLDIFFERENT, x);
	}

	/*
	public static Expression alldifferent(Expression[] xs) {
		return create(ALLDIFFERENT, create(xs));
	}

	public static Expression alldifferent(List<Expression> xs) {
		return create(ALLDIFFERENT, create(xs));
	}
	*/

	public static Expression weightedsum(Expression x, Expression y, Expression z) {
		return create(WEIGHTEDSUM, x, y, z);
	}

	public static Expression weightedsum(Expression x, Expression y, long z) {
		return create(WEIGHTEDSUM, x, y, create(z));
	}

	/*
	public static Expression weightedsum(Expression[] xs, Expression y, Expression z) {
		return create(WEIGHTEDSUM, create(xs), y, z);
	}

	public static Expression weightedsum(Expression[] xs, Expression y, int z) {
		return create(WEIGHTEDSUM, create(xs), y, create(z));
	}

	public static Expression weightedsum(List<Expression> xs, Expression y, Expression z) {
		return create(WEIGHTEDSUM, create(xs), y, z);
	}

	public static Expression weightedsum(List<Expression> xs, Expression y, int z) {
		return create(WEIGHTEDSUM, create(xs), y, create(z));
	}
	*/

	public static Expression cumulative(Expression x, Expression y) {
		return create(CUMULATIVE, x, y);
	}

	public static Expression cumulative(Expression x, long y) {
		return create(CUMULATIVE, x, create(y));
	}

	/*
	public static Expression cumulative(Expression[] xs, Expression y) {
		return create(CUMULATIVE, create(xs), y);
	}

	public static Expression cumulative(Expression[] xs, int y) {
		return create(CUMULATIVE, create(xs), create(y));
	}
	
	public static Expression cumulative(List<Expression> xs, Expression y) {
		return create(CUMULATIVE, create(xs), y);
	}

	public static Expression cumulative(List<Expression> xs, int y) {
		return create(CUMULATIVE, create(xs), create(y));
	}
	*/

	public static Expression element(Expression x, Expression y, Expression z) {
		return create(ELEMENT, x, y, z);
	}

	/*
	public static Expression element(Expression x, Expression[] ys, Expression z) {
		return create(ELEMENT, x, create(ys), z);
	}

	public static Expression element(Expression x, List<Expression> ys, Expression z) {
		return create(ELEMENT, x, create(ys), z);
	}
	*/

	public static Expression disjunctive(Expression x) {
		return create(DISJUNCTIVE, x);
	}

	public static Expression lex_less(Expression x, Expression y) {
		return create(LEX_LESS, x, y);
	}

	public static Expression lex_lesseq(Expression x, Expression y) {
		return create(LEX_LESSEQ, x, y);
	}

	public static Expression nvalue(Expression x, Expression y) {
		return create(NVALUE, x, y);
	}

	public static Expression nvalue(long x, Expression y) {
		return create(NVALUE, create(x), y);
	}

	public static Expression count(Expression x, Expression y, Expression z, Expression w) {
		return create(COUNT, x, y, z, w);
	}

	public static Expression global_cardinality(Expression x, Expression y) {
		return create(GLOBAL_CARDINALITY, x, y);
	}

	public static Expression global_cardinality_with_costs(Expression x, Expression y,
			Expression z, Expression w) {
		return create(GLOBAL_CARDINALITY_WITH_COSTS, x, y, z, w);
	}

	public static Expression global_cardinality_with_costs(Expression x, Expression y,
			Expression z, long w) {
		return create(GLOBAL_CARDINALITY_WITH_COSTS, x, y, z, create(w));
	}

	public static void appendString(StringBuilder sb, long[] xs) {
		String delim = "";
		for (long x : xs) {
			sb.append(delim + x);
			delim = " ";
		}
	}

	public static void appendString(StringBuilder sb, Object[] xs) {
		String delim = "";
		for (Object x : xs) {
			sb.append(delim);
			sb.append(x.toString());
			delim = " ";
		}
	}

}
