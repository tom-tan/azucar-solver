package jp.ac.kobe_u.cs.sugar.converter;

import java.util.ArrayList;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.Logger;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.expression.Expression;
import jp.ac.kobe_u.cs.sugar.expression.Sequence;

/**
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 *
 */
public class GlobalConstraints {
	
	protected static Expression convertAllDifferent(Converter converter, Sequence seq)
	throws SugarException {
		int di = 1;
		int n = seq.length() - 1;
		if (n == 1 && seq.get(n).isSequence()) {
			seq = (Sequence)seq.get(n);
			n = seq.length();
			di = 0;
		}
		List<Expression> xs = new ArrayList<Expression>();
		xs.add(Expression.AND);
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				xs.add(seq.get(i+di).ne(seq.get(j+di)));
			}
		}
		Expression x = Expression.create(xs);
		if (Converter.OPT_PIGEON) {
			int lb = Integer.MAX_VALUE;
			int ub = Integer.MIN_VALUE;
			for (int i = 0; i < n; i++) {
				IntegerDomain d = converter.convertFormula(seq.get(i+di)).getDomain();
				lb = Math.min(lb, d.getLowerBound());
				ub = Math.max(ub, d.getUpperBound());
			}
			List<Expression> xs1 = new ArrayList<Expression>();
			xs1.add(Expression.AND);
			List<Expression> xs2 = new ArrayList<Expression>();
			xs2.add(Expression.AND);
			for (int i = 0; i < n; i++) {
				xs1.add(seq.get(i+di).lt(Expression.create(lb + n - 1)));
				xs2.add(seq.get(i+di).gt(Expression.create(ub - n + 1)));
			}
			x = x.and(Expression.create(xs1).not())
			.and(Expression.create(xs2).not());
		}
		return x;
	}
	
	protected static Expression convertWeightedSum(Converter converter, Sequence seq)
	throws SugarException {
		converter.checkArity(seq, 3);
		if (! seq.get(1).isSequence()) {
			converter.syntaxError(seq);
		}
		Sequence seq1 = (Sequence)seq.get(1);
		Expression x2 = seq.get(2);
		Expression x3 = seq.get(3);
		List<Expression> xs1 = new ArrayList<Expression>();
		xs1.add(Expression.ADD);
		for (int i = 0; i < seq1.length(); i++) {
			if (! seq1.get(i).isSequence()) {
				converter.syntaxError(seq);
			}
			Sequence seqi = (Sequence)seq1.get(i);
			converter.checkArity(seqi, 1);
			xs1.add(seqi.get(0).mul(seqi.get(1)));
		}
		List<Expression> xs = new ArrayList<Expression>();
		xs.add(x2);
		xs.add(Expression.create(xs1));
		xs.add(x3);
		Expression x = Expression.create(xs);
		return x;
	}
	
	protected static Expression convertCumulative(Converter converter, Sequence seq)
	throws SugarException {
		converter.checkArity(seq, 2);
		if (! seq.get(1).isSequence()) {
			converter.syntaxError(seq);
		}
		Sequence seq1 = (Sequence)seq.get(1);
		Expression x2 = seq.get(2);
		int n = seq1.length();
		Expression[] t0 = new Expression[n];
		Expression[] t1 = new Expression[n];
		List<Expression> xs = new ArrayList<Expression>();
		xs.add(Expression.AND);
		int lb = Integer.MAX_VALUE;
		int ub = Integer.MIN_VALUE;
		for (int i = 0; i < n; i++) {
			if (! seq1.get(i).isSequence(3)) {
				converter.syntaxError(seq);
			}
			Sequence task = (Sequence)seq1.get(i);
			Expression origin = task.get(0);
			Expression duration = task.get(1);
			Expression end = task.get(2);
			if (origin.equals(Expression.NIL)) {
				t0[i] = end.sub(duration);
				t1[i] = end;
			} else if (duration.equals(Expression.NIL)) {
				t0[i] = origin;
				t1[i] = end;
			} else if (end.equals(Expression.NIL)) {
				t0[i] = origin;
				t1[i] = origin.add(duration);
			} else {
				xs.add((origin.add(duration)).eq(end));
				t0[i] = origin;
				t1[i] = end;
			}
			IntegerDomain d1 = converter.convertFormula(t0[i]).getDomain();
			IntegerDomain d2 = converter.convertFormula(t1[i]).getDomain();
			lb = Math.min(lb, d1.getLowerBound());
			ub = Math.max(ub, d2.getUpperBound() - 1);
		}
		for (int value = lb; value <= ub; value++) {
			Expression t = Expression.create(value);
			List<Expression> sum = new ArrayList<Expression>();
			sum.add(Expression.ADD);
			for (int i = 0; i < n; i++) {
				Sequence task = (Sequence)seq1.get(i);
				Expression height = task.get(3);
				Expression s = Expression.create(Expression.IF,
						(t0[i].le(t)).and(t1[i].gt(t)),
						Expression.ONE,
						Expression.ZERO);
				sum.add(height.mul(s));
			}
			xs.add(Expression.create(sum).le(x2));
		}
		Expression x = Expression.create(xs);
		return x;
	}
	
	protected static Expression convertElement(Converter converter, Sequence seq)
	throws SugarException {
		converter.checkArity(seq, 3);
		if (! seq.get(2).isSequence()) {
			converter.syntaxError(seq);
		}
		Expression x1 = seq.get(1);
		Sequence seq2 = (Sequence)seq.get(2);
		Expression x3 = seq.get(3);
		int n = seq2.length();
		List<Expression> xs = new ArrayList<Expression>();
		xs.add(Expression.AND);
		xs.add(x1.gt(Expression.ZERO));
		xs.add(x1.le(Expression.create(n)));
		for (int i = 0; i < n; i++) {
			xs.add((x1.eq(Expression.create(i+1))).imp(x3.eq(seq2.get(i))));
		}
		Expression x = Expression.create(xs);
		return x;
	}
	
	protected static Expression convertDisjunctive(Converter converter, Sequence seq)
	throws SugarException {
		// (disjuctive ((o1 d1) (o2 d2) ...))
		// --> di=0 || dj=0 || (oi+di<=oj) || (oj+dj<=oi)  (for all i<j)
		converter.checkArity(seq, 1);
		if (! seq.get(1).isSequence()) {
			converter.syntaxError(seq);
		}
		Sequence seq1 = (Sequence)seq.get(1);
		int n = seq1.length();
		List<Expression> xs = new ArrayList<Expression>();
		xs.add(Expression.AND);
		for (int i = 0; i < n; i++) {
			if (! seq1.get(i).isSequence(1)) {
				converter.syntaxError(seq);
			}
			Sequence task1 = (Sequence)seq1.get(i);
			Expression origin1 = task1.get(0);
			Expression duration1 = task1.get(1);
			for (int j = i + 1; j < n; j++) {
				Sequence task2 = (Sequence)seq1.get(j);
				Expression origin2 = task2.get(0);
				Expression duration2 = task2.get(1);
				Expression x1 = origin1.add(duration1).le(origin2);
				Expression x2 = origin2.add(duration2).le(origin1);
				xs.add(duration1.eq(0).or(duration2.eq(0)).or(x1).or(x2));
			}
		}
		Expression x = Expression.create(xs);
		return x;
	}

	protected static Expression convertLex_less(Converter converter, Sequence seq)
	throws SugarException {
		// (lex_less (x1 x2 x3) (y1 y2 y3))
		// --> (x1<=y1 && (x1==y1 -> (x2<=y2 && (x2==y2 -> x3<y3))))
		converter.checkArity(seq, 2);
		if (! seq.get(1).isSequence() || ! seq.get(2).isSequence()) {
			converter.syntaxError(seq);
		}
		Sequence seq1 = (Sequence)seq.get(1);
		Sequence seq2 = (Sequence)seq.get(2);
		int n = seq1.length(); 
		if (n == 0 || n != seq2.length()) {
			converter.syntaxError(seq);
		}
		Expression x = seq1.get(n - 1).lt(seq2.get(n - 1)); 
		for (int i = n - 2; i >= 0; i--) {
			Expression x1 = seq1.get(i);
			Expression x2 = seq2.get(i);
			x = (x1.le(x2)).and((x1.eq(x2)).imp(x));
		}
		return x;
	}

	protected static Expression convertLex_lesseq(Converter converter, Sequence seq)
	throws SugarException {
		// (lex_lesseq (x1 x2 x3) (y1 y2 y3))
		// --> (x1<=y1 && (x1==y1 -> (x2<=y2 && (x2==y2 -> x3<=y3))))
		converter.checkArity(seq, 2);
		if (! seq.get(1).isSequence() || ! seq.get(2).isSequence()) {
			converter.syntaxError(seq);
		}
		Sequence seq1 = (Sequence)seq.get(1);
		Sequence seq2 = (Sequence)seq.get(2);
		int n = seq1.length(); 
		if (n == 0 || n != seq2.length()) {
			converter.syntaxError(seq);
		}
		Expression x = seq1.get(n - 1).le(seq2.get(n - 1)); 
		for (int i = n - 2; i >= 0; i--) {
			Expression x1 = seq1.get(i);
			Expression x2 = seq2.get(i);
			x = (x1.le(x2)).and((x1.eq(x2)).imp(x));
		}
		return x;
	}

	protected static Expression convertNvalue(Converter converter, Sequence seq)
	throws SugarException {
		// (nvalue s (x1 x2 x3))
		// --> s>=1 && s<=3 && s==if(x1==x2||x1==x3,0,1)+if(x2==x3,0,1)+1
		converter.checkArity(seq, 2);
		if (! seq.get(2).isSequence()) {
			converter.syntaxError(seq);
		}
		Expression x1 = seq.get(1);
		Sequence seq2 = (Sequence)seq.get(2);
		int n = seq2.length();
		Expression x;
		if (n == 0) {
			x = x1.eq(0);
		} else {
			List<Expression> xs = new ArrayList<Expression>();
			xs.add(Expression.ADD);
			for (int i = 0; i < n; i++) {
				List<Expression> ys = new ArrayList<Expression>();
				ys.add(Expression.OR);
				for (int j = i + 1; j < n; j++) {
					ys.add(seq2.get(i).eq(seq2.get(j)));
				}
				if (i < n - 1) {
					xs.add(Expression.create(ys).ifThenElse(Expression.ZERO, Expression.ONE));
				} else {
					xs.add(Expression.ONE);
				}
			}
			Expression sum = Expression.create(xs);
			x = (x1.ge(1)).and(x1.le(n)).and(x1.eq(sum));
		}
		return x;
	}

	protected static Expression convertCount(Converter converter, Sequence seq)
	throws SugarException {
		// (count val (x1 x2 ... xn) op c)
		// --> c>=0 && c<=n && c==if(x1 op val,1,0)+...+if(xn op val,1,0)
		// --> if(x1==val,1,0)+...+if(xn==val,1,0) op c
		converter.checkArity(seq, 4);
		Expression val = seq.get(1);
		Sequence seq2 = (Sequence)seq.get(2);
		Expression op = seq.get(3);
		Expression c = seq.get(4);
		List<Expression> sum = new ArrayList<Expression>();
		sum.add(Expression.ADD);
		int n = seq2.length();
		for (int i = 0; i < n; i++) {
			sum.add(seq2.get(i).eq(val).ifThenElse(Expression.ONE, Expression.ZERO));
		}
		Expression x = Expression.create(op, Expression.create(sum), c);
		return x;
	}

	protected static Expression convertGlobal_cardinality(Converter converter, Sequence seq)
	throws SugarException {
		// (global_cardinality (x1 x2 x3) ((1 c1) (2 c2)))
		// --> (count 1 (x1 x2 x3) eq c1) && (count 2 (x1 x2 x3) eq c2) && c1+c2<=3
		converter.checkArity(seq, 2);
		if (! seq.get(1).isSequence() || ! seq.get(2).isSequence()) {
			converter.syntaxError(seq);
		}
		Sequence vars = (Sequence)seq.get(1);
		Sequence counts = (Sequence)seq.get(2);
		List<Expression> xs = new ArrayList<Expression>();
		xs.add(Expression.AND);
		List<Expression> sum = new ArrayList<Expression>();
		sum.add(Expression.ADD);
		for (int i = 0; i < counts.length(); i++) {
			if (! counts.get(i).isSequence(1)) {
				converter.syntaxError(seq);
			}
			Sequence s = (Sequence)counts.get(i);
			Expression val = s.get(0);
			Expression count = s.get(1);
			xs.add(Expression.count(val, vars, Expression.EQ, count));
			sum.add(count);
		}
		xs.add(Expression.create(sum).le(vars.length()));
		Expression x = Expression.create(xs);
		return x;
	}

	protected static Expression convertGlobal_cardinality_with_costs(Converter converter, Sequence seq)
	throws SugarException {
		// (global_cardinality_with_costs (x1 x2) ((v1 c1) (v2 c2)) ((a1 b1 w1) ...) cost)
		// --> (global_cardinality (x1 x2 x3) ((v1 c1) (v2 c2)))
		//  && cost==if(x[a1]==v[b1],w1,0)+...
		converter.checkArity(seq, 4);
		if (! seq.get(1).isSequence() || ! seq.get(2).isSequence() || ! seq.get(3).isSequence()) {
			converter.syntaxError(seq);
		}
		Sequence vars = (Sequence)seq.get(1);
		Sequence counts = (Sequence)seq.get(2);
		Sequence weights = (Sequence)seq.get(3);
		Expression cost = seq.get(4);
		Expression x1 = Expression.global_cardinality(vars, counts);
		List<Expression> sum = new ArrayList<Expression>();
		sum.add(Expression.ADD);
		for (int i = 0; i < weights.length(); i++) {
			if (! weights.get(i).isSequence(2)) {
				converter.syntaxError(seq);
			}
			Sequence ws = (Sequence)weights.get(i);
			if (! ws.get(0).isInteger() || ! ws.get(1).isInteger()) {
				converter.syntaxError(seq);
			}
			int a = ws.get(0).integerValue();
			int b = ws.get(1).integerValue();
			Expression w = ws.get(2);
			if (a < 1 || a > vars.length() || b < 1 || b > counts.length()) {
				converter.syntaxError(seq);
			}
			Expression var = vars.get(a - 1);
			Expression val = ((Sequence)counts.get(b - 1)).get(0);
			sum.add((var.eq(val)).ifThenElse(w, Expression.ZERO));
		}
		Expression x = x1.and(cost.eq(Expression.create(sum)));
		return x;
	}

}
