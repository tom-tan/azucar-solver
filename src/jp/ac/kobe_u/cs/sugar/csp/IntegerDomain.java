package jp.ac.kobe_u.cs.sugar.csp;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.expression.Expression;

/**
 * This class implements an integer domain class.
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class IntegerDomain {
	public static long MAX_SET_SIZE = 128;
	private long lb;
	private long ub;
	private SortedSet<Long> domain;
	private long size_ = -1;

	private static IntegerDomain create(SortedSet<Long> domain) throws SugarException {
		long lb = domain.first();
		long ub = domain.last();
		if (domain.size() <= MAX_SET_SIZE) {
			boolean sparse = false;
			for (long value = lb; value <= ub; value++) {
				if (! domain.contains(value)) {
					sparse = true;
					break;
				}
			}
			if (! sparse) {
				domain = null;
			}
		} else {
			domain = null;
		}
		if (domain == null) {
			return new IntegerDomain(lb, ub);
		}
		return new IntegerDomain(domain);
	}

	public IntegerDomain(long lb, long ub) throws SugarException {
		if (lb > ub) {
			throw new SugarException("Illegal domain instantiation " + lb + " " + ub);
		}
		this.lb = lb;
		this.ub = ub;
		domain = null;
	}

	public IntegerDomain(SortedSet<Long> domain) {
		lb = domain.first();
		ub = domain.last();
		this.domain = domain;
	}

	public IntegerDomain(IntegerDomain d) {
		lb = d.lb;
		ub = d.ub;
		domain = null;
		if (d.domain != null) {
			domain = new TreeSet<Long>(d.domain);
		}
	}

	private IntegerDomain() {
		domain = new TreeSet<Long>();
	}

	public long size() {
		if (size_ != -1) {
			return size_;
		}
		if (domain == null) {
			size_ = lb <= ub ? ub - lb + 1 : 0;
		} else if (ub-lb > Integer.MAX_VALUE) {
			size_ = 0;
			for(Long v: domain) {
				size_++;
			}
		} else {
			size_ = domain.size();
		}
		return size_;
	}

	public boolean isContiguous() {
		return domain == null;
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public long getLowerBound() {
		if (domain == null) {
			return lb;
		} else {
			return domain.first();
		}
	}

	public long getUpperBound() {
		if (domain == null) {
			return ub;
		} else {
			return domain.last();
		}
	}

	public boolean contains(long value) {
		if (domain == null) {
			return lb <= value && value <= ub;
		} else {
			return domain.contains(value);
		}
	}

	public IntegerDomain bound(long lb, long ub) throws SugarException {
		if (lb <= this.lb && this.ub <= ub)
			return this;
		if (domain == null) {
			lb = Math.max(this.lb, lb);
			ub = Math.min(this.ub, ub);
			return lb > ub ? new IntegerDomain() : new IntegerDomain(lb, ub);
		} else {
			return new IntegerDomain(domain.subSet(lb, ub + 1));
		}
	}

	private class Iter implements Iterator<Long> {
		long value;
		long ub;

		public Iter(long lb, long ub) {
			value = lb;
			this.ub = ub;
		}

		public boolean hasNext() {
			return value <= ub;
		}

		public Long next() {
			return value++;
		}

		public void remove() {
		}
	}

	public Iterator<Long> values(long lb, long ub) {
		if (lb > ub) {
			return new Iter(lb, ub);
		} else if (domain == null) {
				lb = Math.max(lb, this.lb);
				ub = Math.min(ub, this.ub);
				return new Iter(lb, ub);
		} else {
			return domain.subSet(lb, ub + 1).iterator();
		}
	}

	public Iterator<Long> values() {
		return values(lb, ub);
	}

	public IntegerDomain cup(IntegerDomain d1) throws SugarException {
		if (domain == null || d1.domain == null) {
			long lb = Math.min(this.lb, d1.lb);
			long ub = Math.max(this.ub, d1.ub);
			return new IntegerDomain(lb, ub);
		} else {
			SortedSet<Long> d = new TreeSet<Long>(domain);
			d.addAll(d1.domain);
			// return new IntegerDomain(d);
			return create(d);
		}
	}

	public IntegerDomain cap(IntegerDomain d1) throws SugarException {
		if (d1.domain == null) {
			return bound(d1.lb, d1.ub);
		} else if (domain == null) {
			return d1.bound(lb, ub);
		} else {
			SortedSet<Long> d = new TreeSet<Long>();
			for (long value : domain) {
				if (d1.contains(value)) {
					d.add(value);
				}
			}
			return new IntegerDomain(d);
		}
	}

	public IntegerDomain neg() throws SugarException {
		if (domain == null) {
			return new IntegerDomain(-ub, -lb);
		} else {
			SortedSet<Long> d = new TreeSet<Long>();
			for (long value : domain) {
				d.add(-value);
			}
			// return new IntegerDomain(d);
			return create(d);
		}
	}

	public IntegerDomain abs() throws SugarException {
		if (domain == null) {
			long lb0 = Math.min(Math.abs(lb), Math.abs(ub));
			long ub0 = Math.max(Math.abs(lb), Math.abs(ub));
			if (lb <= 0 && 0 <= ub) {
				return new IntegerDomain(0, ub0);
			} else {
				return new IntegerDomain(lb0, ub0);
			}
		} else {
			SortedSet<Long> d = new TreeSet<Long>();
			for (long value : domain) {
				d.add(Math.abs(value));
			}
			// return new IntegerDomain(d);
			return create(d);
		}
	}

	public IntegerDomain add(long a) throws SugarException {
		if (domain == null) {
			return new IntegerDomain(lb+a, ub+a);
		} else {
			SortedSet<Long> d = new TreeSet<Long>();
			for (long value : domain) {
				d.add(value + a);
			}
			// return new IntegerDomain(d);
			return create(d);
		}
	}

	public IntegerDomain add(IntegerDomain d) throws SugarException {
		if (d.size() == 1) {
			return add(d.lb);
		} else 	if (size() == 1) {
			return d.add(lb);
		}
		if (domain == null || d.domain == null) {
			long lb0 = lb + d.lb;
			long ub0 = ub + d.ub;
			return new IntegerDomain(lb0, ub0);
		} else {
			SortedSet<Long> d0 = new TreeSet<Long>();
			for (long value1 : domain) {
				for (long value2 : d.domain) {
					d0.add(value1 + value2);
				}
			}
			// return new IntegerDomain(d0);
			return create(d0);
		}
	}

	public IntegerDomain sub(long a) throws SugarException {
		return add(-a);
	}

	public IntegerDomain sub(IntegerDomain d) throws SugarException {
		return add(d.neg());
	}

	public IntegerDomain mul(long a) throws SugarException {
		if (domain == null) {
			if (size() <= MAX_SET_SIZE) {
				SortedSet<Long> d = new TreeSet<Long>();
				for (long value = lb; value <= ub; value++) {
					d.add(value * a);
				}
				return create(d);
			} else if (a < 0) {
				return new IntegerDomain(ub*a, lb*a);
			} else {
				return new IntegerDomain(lb*a, ub*a);
			}
		} else {
			SortedSet<Long> d = new TreeSet<Long>();
			for (long value : domain) {
				d.add(value * a);
			}
			return create(d);
		}
	}

	public IntegerDomain mul(IntegerDomain d) throws SugarException {
		if (d.size() == 1) {
			return mul(d.lb);
		} else if (size() == 1) {
			return d.mul(lb);
		}
		if (domain == null || d.domain == null
				|| size() * d.size() > MAX_SET_SIZE) {
			long b00 = lb * d.lb;
			long b01 = lb * d.ub;
			long b10 = ub * d.lb;
			long b11 = ub * d.ub;
			long lb0 = Math.min(Math.min(b00, b01), Math.min(b10, b11));
			long ub0 = Math.max(Math.max(b00, b01), Math.max(b10, b11));
			return new IntegerDomain(lb0, ub0);
		} else {
			SortedSet<Long> d0 = new TreeSet<Long>();
			for (long value1 : domain) {
				for (long value2 : d.domain) {
					d0.add(value1 * value2);
				}
			}
			return create(d0);
		}
	}

	private long div(long x, long y) {
		if (x < 0 && x % y != 0) {
			return x / y - 1;
		}
		return x / y;
	}

	public IntegerDomain div(long a) throws SugarException {
		if (domain == null) {
			if (a < 0) {
				return new IntegerDomain(div(ub,a), div(lb,a));
			} else {
				return new IntegerDomain(div(lb,a), div(ub,a));
			}
		} else {
			SortedSet<Long> d = new TreeSet<Long>();
			for (long value : domain) {
				d.add(div(value, a));
			}
			return create(d);
		}
	}

	public IntegerDomain div(IntegerDomain d) throws SugarException {
		if (d.size() == 1) {
			return div(d.lb);
		}
		if (domain == null || d.domain == null
				|| size() * d.size() > MAX_SET_SIZE) {
			long b00 = div(lb, d.lb);
			long b01 = div(lb, d.ub);
			long b10 = div(ub, d.lb);
			long b11 = div(ub, d.ub);
			long lb0 = Math.min(Math.min(b00, b01), Math.min(b10, b11));
			long ub0 = Math.max(Math.max(b00, b01), Math.max(b10, b11));
			if (d.lb <= 1 && 1 <= d.ub) {
				lb0 = Math.min(lb0, Math.min(lb, ub));
				ub0 = Math.max(ub0, Math.max(lb, ub));
			}
			if (d.lb <= -1 && -1 <= d.ub) {
				lb0 = Math.min(lb0, Math.min(-lb, -ub));
				ub0 = Math.max(ub0, Math.max(-lb, -ub));
			}
			return new IntegerDomain(lb0, ub0);
		} else {
			SortedSet<Long> d0 = new TreeSet<Long>();
			for (long value1 : domain) {
				for (long value2 : d.domain) {
					d0.add(div(value1, value2));
				}
			}
			return create(d0);
		}
	}

	public IntegerDomain mod(long a) throws SugarException {
		a = Math.abs(a);
		if (domain == null) {
			return new IntegerDomain(0, a - 1);
		} else {
			SortedSet<Long> d = new TreeSet<Long>();
			for (long value : domain) {
				d.add(value % a);
			}
			return create(d);
		}
	}

	public IntegerDomain mod(IntegerDomain d) throws SugarException {
		if (d.size() == 1) {
			return mod(d.lb);
		}
		if (domain == null || d.domain == null) {
			long lb0 = 0;
			long ub0 = Math.max(Math.abs(d.lb), Math.abs(d.ub)) - 1;
			return new IntegerDomain(lb0, ub0);
		} else {
			SortedSet<Long> d0 = new TreeSet<Long>();
			for (long value1 : domain) {
				for (long value2 : d.domain) {
					d0.add(value1 % value2);
				}
			}
			return create(d0);
		}
	}

	public IntegerDomain pow(long a) throws SugarException {
		if (domain == null) {
			long a1 = (long)Math.round(Math.pow(lb, a));
			long a2 = (long)Math.round(Math.pow(ub, a));
			long lb0 = Math.min(a1, a2);
			long ub0 = Math.max(a1, a2);
			if (a % 2 == 0 && lb <= 0 && 0 <= ub) {
				return new IntegerDomain(0, ub0);
			} else {
				return new IntegerDomain(lb0, ub0);
			}
		} else {
			SortedSet<Long> d = new TreeSet<Long>();
			for (long value : domain) {
				d.add((long)Math.round(Math.pow(value, a)));
			}
			return create(d);
		}
	}

	public IntegerDomain min(IntegerDomain d) throws SugarException {
		long lb0 = Math.min(lb, d.lb);
		long ub0 = Math.min(ub, d.ub);
		if (ub <= d.lb) {
			return this;
		} else if (d.ub <= lb) {
			return d;
		}
		if (domain == null) {
			if (d.domain == null) {
				return new IntegerDomain(lb0, ub0);
			} else {
				return d.min(this);
			}
		} else {
			if (d.domain == null) {
				return create(domain.subSet(lb0, ub0 + 1));
			} else {
				SortedSet<Long> d1 = new TreeSet<Long>(domain);
				d1.addAll(d.domain);
				d1 = d1.subSet(lb0, ub0 + 1);
				return create(d1);
			}
		}
	}

	public IntegerDomain max(IntegerDomain d) throws SugarException {
		long lb0 = Math.max(lb, d.lb);
		long ub0 = Math.max(ub, d.ub);
		if (lb >= d.ub) {
			return this;
		} else if (d.lb >= ub) {
			return d;
		}
		if (domain == null) {
			if (d.domain == null) {
				return new IntegerDomain(lb0, ub0);
			} else {
				return d.max(this);
			}
		} else {
			if (d.domain == null) {
				return create(domain.subSet(lb0, ub0 + 1));
			} else {
				SortedSet<Long> d1 = new TreeSet<Long>(domain);
				d1.addAll(d.domain);
				d1 = d1.subSet(lb0, ub0 + 1);
				return create(d1);
			}
		}
	}

	public SortedSet<Long> headSet(long value) {
		return domain.headSet(value);
	}

	public Expression toExpression() {
		if (domain == null) {
			return Expression.create(Expression.create(Expression.create(lb),
			                         Expression.create(ub)));
		}
		List<Expression> doms = new ArrayList<Expression>();
		long value0 = Long.MIN_VALUE;
		long value1 = Long.MIN_VALUE;
		for (long value : domain) {
			if (value0 == Long.MIN_VALUE) {
				value0 = value1 = value;
			} else if (value1 + 1 == value) {
				value1 = value;
			} else {
				if (value0 == value1) {
					doms.add(Expression.create(value0));
				} else {
					doms.add(Expression.create(Expression.create(value0),
					                           Expression.create(value1)));
				}
				value0 = value1 = value;
			}
		}
		if (value0 == value1) {
			doms.add(Expression.create(value0));
		} else {
			doms.add(Expression.create(Expression.create(value0),
																 Expression.create(value1)));
		}
		return Expression.create(doms);
	}

	public void appendValues(StringBuilder sb) {
		if (domain == null) {
			sb.append(lb + ".." + ub);
		} else {
			String delim = "";
			long value0 = Long.MIN_VALUE;
			long value1 = Long.MIN_VALUE;
			for (long value : domain) {
				if (value0 == Long.MIN_VALUE) {
					value0 = value1 = value;
				} else if (value1 + 1 == value) {
					value1 = value;
				} else {
					sb.append(delim);
					if (value0 == value1) {
						sb.append(value0);
					} else {
						sb.append(value0 + ".." + value1);
					}
					delim = " ";
					value0 = value1 = value;
				}
			}
			if (value0 != Long.MIN_VALUE) {
				sb.append(delim);
				if (value0 == value1) {
					sb.append(value0);
				} else {
					sb.append(value0 + ".." + value1);
				}
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (domain == null) {
			sb.append(lb + " " + ub);
		} else {
			sb.append("(");
			String delim = "";
			long value0 = Long.MIN_VALUE;
			long value1 = Long.MIN_VALUE;
			for (long value : domain) {
				if (value0 == Long.MIN_VALUE) {
					value0 = value1 = value;
				} else if (value1 + 1 == value) {
					value1 = value;
				} else {
					sb.append(delim);
					if (value0 == value1) {
						sb.append(value0);
					} else {
						sb.append("(" + value0 + " " + value1 + ")");
					}
					delim = " ";
					value0 = value1 = value;
				}
			}
			if (value0 != Long.MIN_VALUE) {
				sb.append(delim);
				if (value0 == value1) {
					sb.append(value0);
				} else {
					sb.append("(" + value0 + " " + value1 + ")");
				}
			}
			sb.append(")");
		}
		return sb.toString();
	}
}
