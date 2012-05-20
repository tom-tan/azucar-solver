package jp.ac.kobe_u.cs.sugar.csp;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;

import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.expression.Expression;

/**
 * This class implements an integer domain class.
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class IntegerDomain {
	public static BigInteger MAX_SET_SIZE = new BigInteger("128");
	private BigInteger lb;
	private BigInteger ub;
	private SortedSet<BigInteger> domain;

	private static IntegerDomain create(SortedSet<BigInteger> domain) throws SugarException {
		BigInteger lb = domain.first();
		BigInteger ub = domain.last();
		if ((new BigInteger(Integer.toString(domain.size()))).compareTo(MAX_SET_SIZE) <= 0) {
			boolean sparse = false;
			for (BigInteger value = lb; value.compareTo(ub) <= 0; value = value.add(BigInteger.ONE)) {
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

	public IntegerDomain(BigInteger lb, BigInteger ub) throws SugarException {
		if (lb.compareTo(ub) > 0) {
			throw new SugarException("Illegal domain instantiation " + lb + " " + ub);
		}
		this.lb = lb;
		this.ub = ub;
		domain = null;
	}

	public IntegerDomain(SortedSet<BigInteger> domain) {
		lb = domain.first();
		ub = domain.last();
		this.domain = domain;
	}

	public IntegerDomain(IntegerDomain d) {
		lb = d.lb;
		ub = d.ub;
		domain = null;
		if (d.domain != null) {
			domain = new TreeSet<BigInteger>(d.domain);
		}
	}

	private IntegerDomain() {
		domain = new TreeSet<BigInteger>();
	}

	public BigInteger size() {
		if (domain == null) {
			return lb.compareTo(ub) <= 0 ? ub.subtract(lb).add(BigInteger.ONE) : BigInteger.ZERO;
		} else {
			return new BigInteger(Integer.toString(domain.size()));
		}
	}

	public boolean isContiguous() {
		return domain == null;
	}

	public boolean isEmpty() {
		return size().compareTo(BigInteger.ZERO) == 0;
	}

	public BigInteger getLowerBound() {
		if (domain == null) {
			return lb;
		} else {
			return domain.first();
		}
	}

	public BigInteger getUpperBound() {
		if (domain == null) {
			return ub;
		} else {
			return domain.last();
		}
	}

	public boolean contains(BigInteger value) {
		if (domain == null) {
			return lb.compareTo(value) <= 0 && value.compareTo(ub) <= 0;
		} else {
			return domain.contains(value);
		}
	}

	public IntegerDomain bound(BigInteger lb, BigInteger ub) throws SugarException {
		if (lb.compareTo(this.lb) <= 0 && this.ub.compareTo(ub) <= 0)
			return this;
		if (domain == null) {
			lb = this.lb.max(lb);
			ub = this.ub.min(ub);
			return lb.compareTo(ub) > 0 ? new IntegerDomain() : new IntegerDomain(lb, ub);
		} else {
			return new IntegerDomain(domain.subSet(lb, ub.add(BigInteger.ONE)));
		}
	}

	private class Iter implements Iterator<BigInteger> {
		BigInteger value;
		BigInteger ub;

		public Iter(BigInteger lb, BigInteger ub) {
			value = lb;
			this.ub = ub;
		}

		public boolean hasNext() {
			return value.compareTo(ub) <= 0;
		}

		public BigInteger next() {
			BigInteger ret = value;
			value = value.add(BigInteger.ONE);
			return value.subtract(BigInteger.ONE);
		}

		public void remove() {
		}
	}

	public Iterator<BigInteger> values(BigInteger lb, BigInteger ub) {
		if (lb.compareTo(ub) > 0) {
			return new Iter(lb, ub);
		} else if (domain == null) {
				lb = lb.max(this.lb);
				ub = ub.min(this.ub);
				return new Iter(lb, ub);
		} else {
			return domain.subSet(lb, ub.add(BigInteger.ONE)).iterator();
		}
	}

	public Iterator<BigInteger> values() {
		return values(lb, ub);
	}

	public IntegerDomain cup(IntegerDomain d1) throws SugarException {
		if (domain == null || d1.domain == null) {
			BigInteger lb = this.lb.min(d1.lb);
			BigInteger ub = this.ub.max(d1.ub);
			return new IntegerDomain(lb, ub);
		} else {
			SortedSet<BigInteger> d = new TreeSet<BigInteger>(domain);
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
			SortedSet<BigInteger> d = new TreeSet<BigInteger>();
			for (BigInteger value : domain) {
				if (d1.contains(value)) {
					d.add(value);
				}
			}
			return new IntegerDomain(d);
		}
	}

	public IntegerDomain neg() throws SugarException {
		if (domain == null) {
			return new IntegerDomain(ub.negate(), lb.negate());
		} else {
			SortedSet<BigInteger> d = new TreeSet<BigInteger>();
			for (BigInteger value : domain) {
				d.add(value.negate());
			}
			// return new IntegerDomain(d);
			return create(d);
		}
	}

	public IntegerDomain abs() throws SugarException {
		if (domain == null) {
			BigInteger lb0 = lb.abs().min(ub.abs());
			BigInteger ub0 = lb.abs().max(ub.abs());
			if (lb.compareTo(BigInteger.ZERO) <= 0 && ub.compareTo(BigInteger.ZERO) >= 0) {
				return new IntegerDomain(BigInteger.ZERO, ub0);
			} else {
				return new IntegerDomain(lb0, ub0);
			}
		} else {
			SortedSet<BigInteger> d = new TreeSet<BigInteger>();
			for (BigInteger value : domain) {
				d.add(value.abs());
			}
			// return new IntegerDomain(d);
			return create(d);
		}
	}

	public IntegerDomain add(BigInteger a) throws SugarException {
		if (domain == null) {
			return new IntegerDomain(lb.add(a), ub.add(a));
		} else {
			SortedSet<BigInteger> d = new TreeSet<BigInteger>();
			for (BigInteger value : domain) {
				d.add(value.add(a));
			}
			// return new IntegerDomain(d);
			return create(d);
		}
	}

	public IntegerDomain add(IntegerDomain d) throws SugarException {
		if (d.size().compareTo(BigInteger.ONE) == 0) {
			return add(d.lb);
		} else 	if (size().compareTo(BigInteger.ONE) == 0) {
			return d.add(lb);
		}
		if (domain == null || d.domain == null) {
			BigInteger lb0 = lb.add(d.lb);
			BigInteger ub0 = ub.add(d.ub);
			return new IntegerDomain(lb0, ub0);
		} else {
			SortedSet<BigInteger> d0 = new TreeSet<BigInteger>();
			for (BigInteger value1 : domain) {
				for (BigInteger value2 : d.domain) {
					d0.add(value1.add(value2));
				}
			}
			// return new IntegerDomain(d0);
			return create(d0);
		}
	}

	public IntegerDomain sub(BigInteger a) throws SugarException {
		return add(a.negate());
	}

	public IntegerDomain sub(IntegerDomain d) throws SugarException {
		return add(d.neg());
	}

	public IntegerDomain mul(BigInteger a) throws SugarException {
		if (domain == null) {
			// XXX
			if (false && size().compareTo(MAX_SET_SIZE) <= 0) {
				SortedSet<BigInteger> d = new TreeSet<BigInteger>();
				for (BigInteger value = lb; value.compareTo(ub) <= 0; value = value.add(BigInteger.ONE)) {
					d.add(value.multiply(a));
				}
				return create(d);
			} else if (a.compareTo(BigInteger.ZERO) < 0) {
				return new IntegerDomain(ub.multiply(a), lb.multiply(a));
			} else {
				return new IntegerDomain(lb.multiply(a), ub.multiply(a));
			}
		} else {
			SortedSet<BigInteger> d = new TreeSet<BigInteger>();
			for (BigInteger value : domain) {
				d.add(value.multiply(a));
			}
			return create(d);
		}
	}

	public IntegerDomain mul(IntegerDomain d) throws SugarException {
		if (d.size().compareTo(BigInteger.ONE) == 0) {
			return mul(d.lb);
		} else if (size().compareTo(BigInteger.ONE) == 0) {
			return d.mul(lb);
		}
		if (domain == null || d.domain == null
				|| size().multiply(d.size()).compareTo(MAX_SET_SIZE) > 0) {
			BigInteger b00 = lb.multiply(d.lb);
			BigInteger b01 = lb.multiply(d.ub);
			BigInteger b10 = ub.multiply(d.lb);
			BigInteger b11 = ub.multiply(d.ub);
			BigInteger lb0 = b00.min(b01).min(b10.min(b11));
			BigInteger ub0 = b00.max(b01).max(b10.max(b11));
			return new IntegerDomain(lb0, ub0);
		} else {
			SortedSet<BigInteger> d0 = new TreeSet<BigInteger>();
			for (BigInteger value1 : domain) {
				for (BigInteger value2 : d.domain) {
					d0.add(value1.multiply(value2));
				}
			}
			return create(d0);
		}
	}

	private BigInteger div(BigInteger x, BigInteger y) {
		if (x.compareTo(BigInteger.ZERO) < 0 && x.remainder(y).compareTo(BigInteger.ZERO) != 0) {
			return x.divide(y.subtract(BigInteger.ONE));
		}
		return x.divide(y);
	}

	public IntegerDomain div(BigInteger a) throws SugarException {
		if (domain == null) {
			if (a.compareTo(BigInteger.ZERO) < 0) {
				return new IntegerDomain(div(ub,a), div(lb,a));
			} else {
				return new IntegerDomain(div(lb,a), div(ub,a));
			}
		} else {
			SortedSet<BigInteger> d = new TreeSet<BigInteger>();
			for (BigInteger value : domain) {
				d.add(div(value, a));
			}
			return create(d);
		}
	}

	public IntegerDomain div(IntegerDomain d) throws SugarException {
		if (d.size().compareTo(BigInteger.ONE) == 0) {
			return div(d.lb);
		}
		if (domain == null || d.domain == null
				|| size().multiply(d.size()).compareTo(MAX_SET_SIZE) > 0) {
			BigInteger b00 = div(lb, d.lb);
			BigInteger b01 = div(lb, d.ub);
			BigInteger b10 = div(ub, d.lb);
			BigInteger b11 = div(ub, d.ub);
			BigInteger lb0 = b00.min(b01).min(b10.min(b11));
			BigInteger ub0 = b00.max(b01).max(b10.max(b11));
			if (d.lb.compareTo(BigInteger.ONE) <= 0 && d.ub.compareTo(BigInteger.ONE) >= 0) {
				lb0 = lb0.min(lb.min(ub));
				ub0 = ub0.max(lb.max(ub));
			}
			if (d.lb.compareTo(BigInteger.ONE.negate()) <= 0 && d.ub.compareTo(BigInteger.ONE.negate()) >= 0) {
				lb0 = lb0.min(lb.negate().min(ub.negate()));
				ub0 = ub0.max(lb.negate().max(ub.negate()));
			}
			return new IntegerDomain(lb0, ub0);
		} else {
			SortedSet<BigInteger> d0 = new TreeSet<BigInteger>();
			for (BigInteger value1 : domain) {
				for (BigInteger value2 : d.domain) {
					d0.add(div(value1, value2));
				}
			}
			return create(d0);
		}
	}

	public IntegerDomain mod(BigInteger a) throws SugarException {
		a = a.abs();
		if (domain == null) {
			return new IntegerDomain(BigInteger.ZERO, a.subtract(BigInteger.ONE));
		} else {
			SortedSet<BigInteger> d = new TreeSet<BigInteger>();
			for (BigInteger value : domain) {
				d.add(value.remainder(a));
			}
			return create(d);
		}
	}

	public IntegerDomain mod(IntegerDomain d) throws SugarException {
		if (d.size().compareTo(BigInteger.ONE) == 0) {
			return mod(d.lb);
		}
		if (domain == null || d.domain == null) {
			BigInteger lb0 = BigInteger.ZERO;
			BigInteger ub0 = d.lb.abs().max(d.ub.abs()).subtract(BigInteger.ONE);
			return new IntegerDomain(lb0, ub0);
		} else {
			SortedSet<BigInteger> d0 = new TreeSet<BigInteger>();
			for (BigInteger value1 : domain) {
				for (BigInteger value2 : d.domain) {
					d0.add(value1.remainder(value2));
				}
			}
			return create(d0);
		}
	}

	public IntegerDomain pow(BigInteger a) throws SugarException {
		if (domain == null) {
			BigInteger a1 = lb.pow(a.intValue());
			BigInteger a2 = ub.pow(a.intValue());
			BigInteger lb0 = a1.min(a2);
			BigInteger ub0 = a1.max(a2);
			if (a.remainder(new BigInteger("2")).compareTo(BigInteger.ZERO) == 0 && lb.compareTo(BigInteger.ZERO) <= 0 && ub.compareTo(BigInteger.ZERO) >= 0) {
				return new IntegerDomain(BigInteger.ZERO, ub0);
			} else {
				return new IntegerDomain(lb0, ub0);
			}
		} else {
			SortedSet<BigInteger> d = new TreeSet<BigInteger>();
			for (BigInteger value : domain) {
				d.add(value.pow(a.intValue()));
			}
			return create(d);
		}
	}

	public IntegerDomain min(IntegerDomain d) throws SugarException {
		BigInteger lb0 = lb.min(d.lb);
		BigInteger ub0 = ub.min(d.ub);
		if (ub.compareTo(d.lb) <= 0) {
			return this;
		} else if (d.ub.compareTo(lb) <= 0) {
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
				return create(domain.subSet(lb0, ub0.add(BigInteger.ONE)));
			} else {
				SortedSet<BigInteger> d1 = new TreeSet<BigInteger>(domain);
				d1.addAll(d.domain);
				d1 = d1.subSet(lb0, ub0.add(BigInteger.ONE));
				return create(d1);
			}
		}
	}

	public IntegerDomain max(IntegerDomain d) throws SugarException {
		BigInteger lb0 = lb.max(d.lb);
		BigInteger ub0 = ub.max(d.ub);
		if (lb.compareTo(d.ub) >= 0) {
			return this;
		} else if (d.lb.compareTo(ub) >= 0) {
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
				return create(domain.subSet(lb0, ub0.add(BigInteger.ONE)));
			} else {
				SortedSet<BigInteger> d1 = new TreeSet<BigInteger>(domain);
				d1.addAll(d.domain);
				d1 = d1.subSet(lb0, ub0.add(BigInteger.ONE));
				return create(d1);
			}
		}
	}

	public SortedSet<BigInteger> headSet(BigInteger value) {
		return domain.headSet(value);
	}

	public Expression toExpression() {
		if (domain == null) {
			return Expression.create(Expression.create(Expression.create(lb),
			                         Expression.create(ub)));
		}
		List<Expression> doms = new ArrayList<Expression>();
		BigInteger value0 = null;
		BigInteger value1 = null;
		for (BigInteger value : domain) {
			if (value0 == null) {
				value0 = value1 = value;
			} else if (value1.add(BigInteger.ONE).compareTo(value) == 0) {
				value1 = value;
			} else {
				if (value0.compareTo(value1) == 0) {
					doms.add(Expression.create(value0));
				} else {
					doms.add(Expression.create(Expression.create(value0),
					                           Expression.create(value1)));
				}
				value0 = value1 = value;
			}
		}
		if (value0.compareTo(value1) == 0) {
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
			BigInteger value0 = null;
			BigInteger value1 = null;
			for (BigInteger value : domain) {
				if (value0 == null) {
					value0 = value1 = value;
				} else if (value1.add(BigInteger.ONE).compareTo(value) == 0) {
					value1 = value;
				} else {
					sb.append(delim);
					if (value0.compareTo(value1) == 0) {
						sb.append(value0);
					} else {
						sb.append(value0 + ".." + value1);
					}
					delim = " ";
					value0 = value1 = value;
				}
			}
			if (value0 != null) {
				sb.append(delim);
				if (value0.compareTo(value1) == 0) {
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
			BigInteger value0 = null;
			BigInteger value1 = null;
			for (BigInteger value : domain) {
				if (value0 == null) {
					value0 = value1 = value;
				} else if (value1.add(BigInteger.ONE).compareTo(value) == 0) {
					value1 = value;
				} else {
					sb.append(delim);
					if (value0.compareTo(value1) == 0) {
						sb.append(value0);
					} else {
						sb.append("(" + value0 + " " + value1 + ")");
					}
					delim = " ";
					value0 = value1 = value;
				}
			}
			if (value0 != null) {
				sb.append(delim);
				if (value0.compareTo(value1) == 0) {
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
