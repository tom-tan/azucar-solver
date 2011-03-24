package jp.ac.kobe_u.cs.sugar.csp;
 
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import jp.ac.kobe_u.cs.sugar.SugarException;

/**
 * This class implements an integer domain class.
 * @see CSP
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class IntegerDomain {
	public static int MAX_SET_SIZE = 128;
	// public static int MAX_SET_SIZE = 256;
	private int lb;
	private int ub;
	private SortedSet<Integer> domain;

	private static IntegerDomain create(SortedSet<Integer> domain) throws SugarException {
		int lb = domain.first();
		int ub = domain.last();
		if (domain.size() <= MAX_SET_SIZE) {
			boolean sparse = false;
			for (int value = lb; value <= ub; value++) {
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
	
	public IntegerDomain(int lb, int ub) throws SugarException {
		if (lb > ub) {
			throw new SugarException("Illegal domain instantiation " + lb + " " + ub);
		}
		this.lb = lb;
		this.ub = ub;
		domain = null;
	}

	public IntegerDomain(SortedSet<Integer> domain) {
		lb = domain.first();
		ub = domain.last();
		this.domain = domain;
	}

	public IntegerDomain(IntegerDomain d) {
		lb = d.lb;
		ub = d.ub;
		domain = null;
		if (d.domain != null) {
			domain = new TreeSet<Integer>(d.domain);
		}
	}

	public int size() {
		if (domain == null) {
			return lb <= ub ? ub - lb + 1 : 0; 
		} else {
			return domain.size();
		}
	}

	public boolean isContiguous() {
		return domain == null;
	}
	
	public boolean isEmpty() {
		return size() == 0;
	}

	public int getLowerBound() {
		if (domain == null) {
			return lb;
		} else {
			return domain.first();
		}
	}

	public int getUpperBound() {
		if (domain == null) {
			return ub;
		} else {
			return domain.last();
		}
	}

	public boolean contains(int value) {
		if (domain == null) {
			return lb <= value && value <= ub;
		} else {
			return domain.contains(value);
		}
	}

	public int sizeLE(int value) {
		if (value < lb)
			return 0;
		if (value >= ub)
			return size();
		if (domain == null) {
			return value - lb + 1;
		} else {
			return domain.headSet(value + 1).size();
		}
	}

	public IntegerDomain bound(int lb, int ub) throws SugarException {
		if (lb <= this.lb && this.ub <= ub)
			return this;
		if (domain == null) {
			lb = Math.max(this.lb, lb);
			ub = Math.min(this.ub, ub);
			return new IntegerDomain(lb, ub);
		} else {
			return new IntegerDomain(domain.subSet(lb, ub + 1));
		}
	}

	private class Iter implements Iterator<Integer> {
		int value;
		int ub;
		
		public Iter(int lb, int ub) {
			value = lb;
			this.ub = ub;
		}
		
		public boolean hasNext() {
			return value <= ub;
		}

		public Integer next() {
			return value++;
		}

		public void remove() {
		}

	}
	
	public Iterator<Integer> values(int lb, int ub) {
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
	
	public Iterator<Integer> values() {
		return values(lb, ub);
	}
	
	public IntegerDomain cup(IntegerDomain d1) throws SugarException {
		if (domain == null || d1.domain == null) {
			int lb = Math.min(this.lb, d1.lb);
			int ub = Math.max(this.ub, d1.ub);
			return new IntegerDomain(lb, ub);
		} else {
			SortedSet<Integer> d = new TreeSet<Integer>(domain);
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
			SortedSet<Integer> d = new TreeSet<Integer>();
			for (int value : domain) {
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
			SortedSet<Integer> d = new TreeSet<Integer>();
			for (int value : domain) {
				d.add(-value);
			}
			// return new IntegerDomain(d);
			return create(d);
		}
	}

	public IntegerDomain abs() throws SugarException {
		if (domain == null) {
			int lb0 = Math.min(Math.abs(lb), Math.abs(ub));
			int ub0 = Math.max(Math.abs(lb), Math.abs(ub));
			if (lb <= 0 && 0 <= ub) {
				return new IntegerDomain(0, ub0);
			} else {
				return new IntegerDomain(lb0, ub0);
			}
		} else {
			SortedSet<Integer> d = new TreeSet<Integer>();
			for (int value : domain) {
				d.add(Math.abs(value));
			}
			// return new IntegerDomain(d);
			return create(d);
		}
	}
	
	public IntegerDomain add(int a) throws SugarException {
		if (domain == null) {
			return new IntegerDomain(lb+a, ub+a);
		} else {
			SortedSet<Integer> d = new TreeSet<Integer>();
			for (int value : domain) {
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
			int lb0 = lb + d.lb;
			int ub0 = ub + d.ub;
			return new IntegerDomain(lb0, ub0);
		} else {
			SortedSet<Integer> d0 = new TreeSet<Integer>();
			for (int value1 : domain) {
				for (int value2 : d.domain) {
					d0.add(value1 + value2);
				}
			}
			// return new IntegerDomain(d0);
			return create(d0);
		}
	}

	public IntegerDomain sub(int a) throws SugarException {
		return add(-a);
	}

	public IntegerDomain sub(IntegerDomain d) throws SugarException {
		return add(d.neg());
	}

	public IntegerDomain mul(int a) throws SugarException {
		if (domain == null) {
			// XXX
			if (false && size() <= MAX_SET_SIZE) {
				SortedSet<Integer> d = new TreeSet<Integer>();
				for (int value = lb; value <= ub; value++) {
					d.add(value * a);
				}
				return create(d);
			} else if (a < 0) {
				return new IntegerDomain(ub*a, lb*a);
			} else {
				return new IntegerDomain(lb*a, ub*a);
			}
		} else {
			SortedSet<Integer> d = new TreeSet<Integer>();
			for (int value : domain) {
				d.add(value * a);
			}
			return create(d);
		}
	}

	public IntegerDomain mul(IntegerDomain d) throws SugarException {
		if (d.size() == 1) {
			return mul(d.lb);
		} else 	if (size() == 1) {
			return d.mul(lb);
		}
		if (domain == null || d.domain == null
				|| size() * d.size() > MAX_SET_SIZE) {
			int b00 = lb * d.lb;
			int b01 = lb * d.ub;
			int b10 = ub * d.lb;
			int b11 = ub * d.ub;
			int lb0 = Math.min(Math.min(b00, b01), Math.min(b10, b11));
			int ub0 = Math.max(Math.max(b00, b01), Math.max(b10, b11));
			return new IntegerDomain(lb0, ub0);
		} else {
			SortedSet<Integer> d0 = new TreeSet<Integer>();
			for (int value1 : domain) {
				for (int value2 : d.domain) {
					d0.add(value1 * value2);
				}
			}
			return create(d0);
		}
	}

	private int div(int x, int y) {
		if (x < 0 && x % y != 0) {
			return x / y - 1;
		}
		return x / y;
	}
	
	public IntegerDomain div(int a) throws SugarException {
		if (domain == null) {
			if (a < 0) {
				return new IntegerDomain(div(ub,a), div(lb,a));
			} else {
				return new IntegerDomain(div(lb,a), div(ub,a));
			}
		} else {
			SortedSet<Integer> d = new TreeSet<Integer>();
			for (int value : domain) {
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
			int b00 = div(lb, d.lb);
			int b01 = div(lb, d.ub);
			int b10 = div(ub, d.lb);
			int b11 = div(ub, d.ub);
			int lb0 = Math.min(Math.min(b00, b01), Math.min(b10, b11));
			int ub0 = Math.max(Math.max(b00, b01), Math.max(b10, b11));
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
			SortedSet<Integer> d0 = new TreeSet<Integer>();
			for (int value1 : domain) {
				for (int value2 : d.domain) {
					d0.add(div(value1, value2));
				}
			}
			return create(d0);
		}
	}

	public IntegerDomain mod(int a) throws SugarException {
		a = Math.abs(a);
		if (domain == null) {
			return new IntegerDomain(0, a - 1);
		} else {
			SortedSet<Integer> d = new TreeSet<Integer>();
			for (int value : domain) {
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
			int lb0 = 0;
			int ub0 = Math.max(Math.abs(d.lb), Math.abs(d.ub)) - 1;
			return new IntegerDomain(lb0, ub0);
		} else {
			SortedSet<Integer> d0 = new TreeSet<Integer>();
			for (int value1 : domain) {
				for (int value2 : d.domain) {
					d0.add(value1 % value2);
				}
			}
			return create(d0);
		}
	}

	public IntegerDomain pow(int a) throws SugarException {
		if (domain == null) {
			int a1 = (int)Math.round(Math.pow(lb, a));
			int a2 = (int)Math.round(Math.pow(ub, a));
			int lb0 = Math.min(a1, a2);
			int ub0 = Math.max(a1, a2);
			if (a % 2 == 0 && lb <= 0 && 0 <= ub) {
				return new IntegerDomain(0, ub0);
			} else {
				return new IntegerDomain(lb0, ub0);
			}
		} else {
			SortedSet<Integer> d = new TreeSet<Integer>();
			for (int value : domain) {
				d.add((int)Math.round(Math.pow(value, a)));
			}
			return create(d);
		}
	}
	
	public IntegerDomain min(IntegerDomain d) throws SugarException {
		int lb0 = Math.min(lb, d.lb);
		int ub0 = Math.min(ub, d.ub);
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
				SortedSet<Integer> d1 = new TreeSet<Integer>(domain);
				d1.addAll(d.domain);
				d1 = d1.subSet(lb0, ub0 + 1);
				// return new IntegerDomain(d1);
				return create(d1);
			}
		}
	}

	public IntegerDomain max(IntegerDomain d) throws SugarException {
		int lb0 = Math.max(lb, d.lb);
		int ub0 = Math.max(ub, d.ub);
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
				SortedSet<Integer> d1 = new TreeSet<Integer>(domain);
				d1.addAll(d.domain);
				d1 = d1.subSet(lb0, ub0 + 1);
				// return new IntegerDomain(d1);
				return create(d1);
			}
		}
	}

	public void appendValues(StringBuilder sb) {
		if (domain == null) {
			sb.append(lb + ".." + ub);
		} else if (false) {
			String delim = "";
			for (int value : domain) {
				sb.append(delim);
				sb.append(value);
				delim = " ";
			}
		} else {
			String delim = "";
			int value0 = Integer.MIN_VALUE;
			int value1 = Integer.MIN_VALUE;
			for (int value : domain) {
				if (value0 == Integer.MIN_VALUE) {
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
			if (value0 != Integer.MIN_VALUE) {
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
		sb.append("(");
		appendValues(sb);
		sb.append(")");
		return sb.toString();
	}

}
