package jp.ac.kobe_u.cs.sugar.converter;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.expression.Expression;
import jp.ac.kobe_u.cs.sugar.expression.LinearExpression;
import jp.ac.kobe_u.cs.sugar.expression.Sequence;

/**
 * Relation class.
 * @see Converter 
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class Relation {
	public String name;
	public int arity;
	public boolean conflicts;
	private HashSet<Tuple> tupleSet;
	private static final int UNDEF = Integer.MIN_VALUE;
	private LinearExpression[] les;
	private Map<String,IntegerDomain> domMap;

	public Relation(String name, int arity, Sequence body) throws SugarException {
		this.name = name;
		this.arity = arity;
		if (body.isSequence(Expression.SUPPORTS)) {
			conflicts = false;
		} else if (body.isSequence(Expression.CONFLICTS)) {
			conflicts = true;
		} else {
			throw new SugarException("Syntax error " + body);
		}
		int n = body.length() - 1;
		int[][] tuples = new int[n][];
		for (int i = 1; i <= n; i++) {
			if (! body.get(i).isSequence()) {
				throw new SugarException("Syntax error " + body);
			}
			Sequence seq = (Sequence)body.get(i);
			int[] tuple = new int[arity];
			for (int j = 0; j < arity; j++) {
				tuple[j] = seq.get(j).integerValue();
			}
			tuples[i-1] = tuple;
		}
		tupleSet = new HashSet<Tuple>();
		for (int[] tuple : tuples) {
			tupleSet.add(new Tuple(tuple.clone()));
			int[] tuple0 = tuple.clone();
			for (int i = tuple.length - 1; i >= 1; i--) {
				tuple0[i] = UNDEF;
				tupleSet.add(new Tuple(tuple0.clone()));
			}
		}
	}

	private class Tuple {
		public int[] values;

		public Tuple(int[] values) {
			this.values = values;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + Arrays.hashCode(values);
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
			final Tuple other = (Tuple) obj;
			if (!Arrays.equals(values, other.values))
				return false;
			return true;
		}
	}

	private class Brick {
		public int[] lb;
		public int[] ub;

		public Brick(int[] lb, int[] ub) {
			this.lb = lb;
			this.ub = ub;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			Expression.appendString(sb, lb);
			sb.append(")-(");
			Expression.appendString(sb, ub);
			sb.append(")");
			return sb.toString();
		}
	}

	private boolean contactInside(Brick brick1, Brick brick2, int i) {
		int n = brick1.lb.length;
		for (int j = 0; j < n; j++) {
			if (j != i) {
				if (! (brick2.lb[j] <= brick1.lb[j] && brick1.ub[j] <= brick2.ub[j])) {
					return false;
				}
			}
		}
		return true;
	}

	private List<Brick> combineBricks2(List<Brick> bricks1, List<Brick> bricks2,
			int i, int value1, int value2) {
		List<Brick> bricks = new ArrayList<Brick>();
		int j = 0;
		while (j < bricks1.size()) {
			Brick brick = bricks1.get(j);
			if (brick.ub[i] == value1) {
				j++;
			} else {
				bricks.add(brick);
				bricks1.remove(j);
			}
		}
		j = 0;
		while (j < bricks2.size()) {
			Brick brick = bricks2.get(j);
			if (brick.lb[i] == value2) {
				j++;
			} else {
				bricks.add(brick);
				bricks2.remove(j);
			}
		}
		for (Brick brick1 : bricks1) {
			int j2 = 0;
			while (j2 < bricks2.size()) {
				Brick brick2 = bricks2.get(j2);
				if (contactInside(brick1, brick2, i)) {
					int[] ub = brick1.ub.clone();
					ub[i] = brick2.ub[i];
					brick1.ub = ub;
					if (! contactInside(brick2, brick1, i)) {
						bricks.add(brick2);
					}
					bricks2.remove(j2);
					break;
				} else if (contactInside(brick2, brick1, i)) {
					int[] lb = brick2.lb.clone();
					lb[i] = brick1.lb[i];
					brick2.lb = lb;
					bricks.add(brick2);
					bricks2.remove(j2);
					break;
				} else {
					j2++;
				}
			}
		}
		bricks.addAll(bricks1);
		bricks.addAll(bricks2);
		return bricks;
	}

	private List<Brick> combineBricks(int i, List<Integer> values, Tuple tuple) throws SugarException {
		List<Brick> bricks = null;
		if (i == les.length - 1) {
			bricks = new ArrayList<Brick>();
			Iterator<Integer> iter = les[i].getDomain(domMap).values();
			int lb[] = null;
			int ub[] = null;
			while (iter.hasNext()) {
				int value = iter.next();
				tuple.values[i] = value;
				if (conflicts(tuple)) {
					int[] point = tuple.values.clone();
					if (lb == null) {
						lb = ub = point;
					} else {
						ub = point;
					}
				} else {
					if (lb != null) {
						bricks.add(new Brick(lb, ub));
					}
					lb = ub = null;
				}
			}
			if (lb != null) {
				bricks.add(new Brick(lb, ub));
			}
		} else {
			if (values == null) {
				values = new ArrayList<Integer>();
				Iterator<Integer> iter = les[i].getDomain(domMap).values();
				while (iter.hasNext()) {
					int value = iter.next();
					values.add(value);
				}
			}
			int size = values.size();
			if (size == 1) {
				tuple.values[i] = values.get(0);
				for (int j = i + 1; j < tuple.values.length; j++) {
					tuple.values[j] = UNDEF;
				}
				if (conflicts) {
					if (tupleSet.contains(tuple)) {
						bricks = combineBricks(i + 1, null, tuple);
					} else {
						bricks = new ArrayList<Brick>();
					}
				} else {
					if (tupleSet.contains(tuple)) {
						bricks = combineBricks(i + 1, null, tuple);
					} else {
						bricks = new ArrayList<Brick>();
						int[] lb = tuple.values.clone();
						int[] ub = tuple.values.clone();
						for (int j = i + 1; j < les.length; j++) {
							lb[j] = les[j].getDomain(domMap).getLowerBound();
							ub[j] = les[j].getDomain(domMap).getUpperBound();
						}
						bricks.add(new Brick(lb, ub));
					}
				}
			} else {
				int m = size / 2;
				List<Brick> bricks1 = combineBricks(i, values.subList(0, m), tuple);
				List<Brick> bricks2 = combineBricks(i, values.subList(m, size), tuple);
				int value1 = values.get(m - 1);
				int value2 = values.get(m);
				bricks = combineBricks2(bricks1, bricks2, i, value1, value2);
			}
		}
		return bricks;
	}

	private boolean conflicts(Tuple tuple) {
		return (conflicts && tupleSet.contains(tuple))
		|| (! conflicts && ! tupleSet.contains(tuple));
	}

	private List<Brick> getConflictBricks() throws SugarException {
		Tuple tuple = new Tuple(new int[les.length]);
		List<Brick> bricks = combineBricks(0, null, tuple);
		return bricks;
	}

	public Expression apply(LinearExpression[] args, Map<String,IntegerDomain> expDomainMap) throws SugarException {
		les = args;
		domMap = expDomainMap;
		List<Expression> ret = new ArrayList<Expression>();
		List<Brick> bricks = getConflictBricks();
		for (Brick brick : bricks) {
			List<Expression> pt = new ArrayList<Expression>();
			for (int i = 0; i < arity; i++) {
				LinearExpression le = les[i];
				if (brick.lb[i] == brick.ub[i]) {
					pt.add(le.toSeqExpression().ne(brick.lb[i]));
				} else {
					pt.add(le.toSeqExpression().lt(brick.lb[i]));
					LinearExpression le1 = new LinearExpression(le);
					pt.add(le1.toSeqExpression().gt(brick.ub[i]));
				}
			}
			ret.add(Expression.create(Expression.OR, pt));
		}
		les = null;
		domMap = null;
		return Expression.create(Expression.AND, ret);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(" + Expression.RELATION_DEFINITION + " ");
		sb.append(name + " " + arity + " (");
		sb.append(conflicts ? Expression.CONFLICTS : Expression.SUPPORTS);
		for (Tuple tuple : tupleSet) {
			sb.append(" (");
			String delim = "";
			for (int j = 0; j < arity; j++) {
				sb.append(delim + tuple.values[j]);
				delim = " ";
			}
			sb.append(")");
		}
		sb.append("))");
		return sb.toString();
	}
}
