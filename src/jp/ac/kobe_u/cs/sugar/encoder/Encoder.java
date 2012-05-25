package jp.ac.kobe_u.cs.sugar.encoder;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import jp.ac.kobe_u.cs.sugar.Logger;
import jp.ac.kobe_u.cs.sugar.SugarConstants;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.SugarMain;
import jp.ac.kobe_u.cs.sugar.csp.ArithmeticLiteral;
import jp.ac.kobe_u.cs.sugar.csp.LinearSum;
import jp.ac.kobe_u.cs.sugar.csp.Literal;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.LinearLiteral;
import jp.ac.kobe_u.cs.sugar.csp.Operator;
import jp.ac.kobe_u.cs.sugar.csp.ProductLiteral;

/**
 * Encoder encodes CSP into SAT.
 * @see CSP 
 * @author Tomoya Tanjo (tanjo@nii.ac.jp)
 */
public abstract class Encoder {
	public static boolean simplifyAll = true;
	public static boolean ESTIMATE_SATSIZE = false; // bad
	public static long MAX_LINEARSUM_SIZE = 1024L;
	public static int SPLITS = 2;

	public static final long FALSE_CODE = 0;

	public static final long TRUE_CODE = Long.MIN_VALUE;

	protected final CSP csp;

	protected CNFWriter writer;


	public abstract long getCode(LinearLiteral lit) throws SugarException;
	protected abstract void encode(IntegerVariable v) throws SugarException, IOException;
	protected abstract void encode(LinearLiteral lit, long[] clause) throws SugarException, IOException;
	public abstract long getSatVariablesSize(IntegerVariable ivar);
	public abstract void reduce() throws SugarException;
	protected abstract boolean isSimple(Literal lit);

	protected boolean isSimple(Clause c) {
		return (c.size()-simpleSize(c)) <= 1;
	}

	protected int simpleSize(Clause c) {
		int simpleLiterals = c.getBooleanLiterals().size();
		for (Literal lit : c.getArithmeticLiterals()) {
			if (isSimple(lit)) {
				simpleLiterals++;
			}
		}
		return simpleLiterals;
	}

	public static long negateCode(long code) {
		if (code == FALSE_CODE) {
			code = TRUE_CODE;
		} else if (code == TRUE_CODE) {
			code = FALSE_CODE;
		} else {
			code = - code;
		}
		return code;
	}

	public Encoder(CSP csp) {
		this.csp = csp;
	}

	protected long getCode(BooleanLiteral lit) throws SugarException {
		final long code = lit.getBooleanVariable().getCode();
		return lit.getNegative() ? -code : code;
	}

	protected void encode(Clause cl) throws SugarException, IOException {
		if (! isSimple(cl)) {
			throw new SugarException("Cannot encode non-simple clause " + cl.toString());
		}
		writer.writeComment(cl.toString());
		if (cl.isValid()) {
			return;
		}
		final long[] clause = new long[simpleSize(cl)];
		LinearLiteral lit = null;
		int i = 0;
		for (BooleanLiteral literal : cl.getBooleanLiterals()) {
			clause[i] = getCode(literal);
			i++;
		}
		for (ArithmeticLiteral literal : cl.getArithmeticLiterals()) {
			if (isSimple(literal)) {
				clause[i] = getCode((LinearLiteral)literal);
				i++;
			} else {
				lit = (LinearLiteral)literal;
			}
		}
		if (lit == null) {
			writer.writeClause(clause);
		} else {
			encode(lit, clause);
		}
	}

	protected void encode(BooleanVariable v) {
	}

	protected int getSatVariablesSize(BooleanVariable bvar) {
		return 1;
	}

	protected List<Clause> adjust(IntegerVariable v, boolean useOffset) throws SugarException {
		final List<Clause> ret = new ArrayList<Clause>();
		final IntegerDomain d = v.getDomain();
		if (! d.isContiguous()) {
			long lst = d.getLowerBound()-1;
			final Iterator<Long> iter = d.values();
			while(iter.hasNext()) {
				final long i = iter.next();
				if (lst+2 == i) {
					final Clause c = new Clause(new LinearLiteral(new LinearSum(1, v, -(lst+1)),
																												Operator.NE));
					c.setComment(v.getName() + " != " + (lst+1));
					ret.add(c);
				} else if (lst+1 != i) {
					final BooleanVariable b = new BooleanVariable();
					csp.add(b);
					final Clause c1 = new Clause(new LinearLiteral(new LinearSum(1, v, -lst),
																												 Operator.LE));
					c1.add(new BooleanLiteral(b, true));
					c1.setComment(v.getName() + " <= " + lst + " || "
												+ v.getName() + " >= " + i);
					ret.add(c1);
					final Clause c2 = new Clause(new LinearLiteral(new LinearSum(-1, v, i),
																												 Operator.LE));
					c2.add(new BooleanLiteral(b, false));
					ret.add(c2);
				}
				lst = i;
			}
		}
		final long offset = d.getLowerBound();
		if (useOffset) {
			v.setOffset(offset);
			v.setDomain(new IntegerDomain(0, d.getUpperBound()-offset));
		} else {
			final Clause c = new Clause(new LinearLiteral(new LinearSum(-1, v, offset),
																										Operator.LE));
			ret.add(c);
			v.setDomain(new IntegerDomain(0, d.getUpperBound()));
		}
		return ret;
	}

	protected void adjust() throws SugarException {
		BooleanVariable.setPrefix("A");
		BooleanVariable.setIndex(0);
		IntegerVariable.setPrefix("A");
		IntegerVariable.setIndex(0);
		Logger.fine("Adjust the lower bound of integer variables to 0");
		for (IntegerVariable v: csp.getIntegerVariables()) {
			csp.getClauses().addAll(adjust(v, true));
		}

		final List<Clause> newClauses = new ArrayList<Clause>();
		final int size = csp.getClauses().size();
		for (int i=0; i<size; i++) {
			final Clause c = csp.getClauses().get(i);
			csp.getClauses().set(i, null);
			Clause newCls = null;
			if(c.getArithmeticLiterals().isEmpty()) {
				newCls = c;
			} else {
				newCls = new Clause(c.getBooleanLiterals());
				newCls.setComment(c.getComment());

				for (ArithmeticLiteral lit: c.getArithmeticLiterals()) {
					if (lit instanceof LinearLiteral) {
						final LinearLiteral ll = (LinearLiteral)lit;
						final LinearSum ls = ll.getLinearExpression();
						for (Entry<IntegerVariable, Long> es :
									 ls.getCoef().entrySet()) {
							ls.setB(ls.getB()+es.getKey().getOffset()*es.getValue());
						}
						newCls.add(new LinearLiteral(ls, ll.getOperator()));
					} else {
						assert lit instanceof ProductLiteral;
						final IntegerVariable z = ((ProductLiteral)lit).getV();
						final IntegerVariable x = ((ProductLiteral)lit).getV1();
						final IntegerVariable y = ((ProductLiteral)lit).getV2();
						final long zoffset = z.getOffset();
						final long xoffset = x.getOffset();
						final long yoffset = y.getOffset();
						if (zoffset == 0 && xoffset == 0 && yoffset == 0) {
							newCls.add(lit);
						} else {
							/*
								(z+zoffset) = (x+xoffset)(y+yoffset)
								z = p+yoffset*x+xoffset*y+xoffset*yoffset-zoffset
								--> -z + p + yoffset*x + xoffset*y + xoffset*yoffset-zoffset = 0
								p = xy --> p=xy
							*/
							final IntegerDomain xdom = x.getDomain();
							final IntegerDomain ydom = y.getDomain();
							final LinearSum ls = new LinearSum(xoffset*yoffset-zoffset);
							ls.setA(-1, z);
							final IntegerVariable p = new IntegerVariable(new IntegerDomain(0, xdom.mul(ydom).getUpperBound()));
							csp.add(p);
							ls.setA(1, p);
							ls.setA(yoffset, x);
							ls.setA(xoffset, y);
							newCls.add(new LinearLiteral(ls, Operator.EQ));
							newClauses.add(new Clause(new ProductLiteral(p, x, y)));
						}
					}
				}
			}
			assert newCls != null;
			newClauses.add(newCls);
		}

		csp.setClauses(newClauses);
		Logger.info("CSP : " + csp.summary());
	}

	public List<Clause> simplify(Clause clause) throws SugarException {
		final List<Clause> newClauses = new ArrayList<Clause>();
		final Clause c = new Clause(clause.getBooleanLiterals());
		c.setComment(clause.getComment());

		int complex = 0;
		for (Literal literal : clause.getArithmeticLiterals()) {
			if (isSimple(literal)) {
				c.add(literal);
			} else {
				complex++;
				if (! simplifyAll && complex == 1) {
					c.add(literal);
				} else {
					final BooleanVariable p = new BooleanVariable();
					csp.add(p);
					final Literal posLiteral = new BooleanLiteral(p, false);
					final Literal negLiteral = new BooleanLiteral(p, true);
					final Clause newClause = new Clause();
					newClause.add(negLiteral);
					newClause.add(literal);
					newClauses.add(newClause);
					c.add(posLiteral);
				}
			}
		}
		newClauses.add(c);
		return newClauses;
	}

	protected void simplify() throws SugarException {
		BooleanVariable.setPrefix("S");
		BooleanVariable.setIndex(0);
		Logger.fine("Simplifing CSP by introducing new Boolean variables");
		final List<Clause> newClauses = new ArrayList<Clause>();
		final int size = csp.getClauses().size();
		for (int i=0; i<size; i++) {
			final Clause clause = csp.getClauses().get(i);
			csp.getClauses().set(i, null);
			if (clause.isValid()) {
				// nop
			} else if (isSimple(clause)) {
				newClauses.add(clause);
			} else {
				newClauses.addAll(simplify(clause));
			}
		}
		csp.setClauses(newClauses);
	}

	public void encode(String satFileName, boolean incremental) throws SugarException, IOException {
		writer = new CNFWriter(satFileName, incremental);

		for (IntegerVariable v : csp.getIntegerVariables()) {
			v.setCode(writer.getSatVariablesCount() + 1);
			final long size = getSatVariablesSize(v);
			writer.addSatVariables(size);
		}
		for (BooleanVariable v : csp.getBooleanVariables()) {
			v.setCode(writer.getSatVariablesCount() + 1);
			final int size = getSatVariablesSize(v);
			writer.addSatVariables(size);
		}

		int count = 0;
		int n = csp.getIntegerVariables().size();
		int percent = 10;
		for (IntegerVariable v : csp.getIntegerVariables()) {
			encode(v);
			count++;
			if ((100*count)/n >= percent) {
				Logger.fine(count + " (" + percent + "%) "
						+ "CSP integer variables are encoded"
						+ " (" + writer.getSatClausesCount() + " clauses, " + writer.getSatFileSize() + " bytes)");
				percent += 10;
			}
		}
		count = 0;
		n = csp.getClauses().size();
		percent = 10;
		for (Clause c : csp.getClauses()) {
			final int satClausesCount0 = writer.getSatClausesCount();
			if (! c.isValid()) {
				encode(c);
			}
			count++;
			if (SugarMain.debug >= 1) {
				final int k = writer.getSatClausesCount() - satClausesCount0;
				Logger.fine(k + " SAT clauses for " + c);
			}
			if ((100*count)/n >= percent) {
				Logger.fine(count + " (" + percent + "%) "
						+ "CSP clauses are encoded"
						+ " (" + writer.getSatClausesCount() + " clauses, " + writer.getSatFileSize() + " bytes)");
				percent += 10;
			}
		}
		Logger.fine(count + " CSP clauses encoded");
		writer.close();
	}

	public void outputMap(String mapFileName) throws SugarException, IOException {
		final BufferedWriter mapWriter =
			new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mapFileName), "UTF-8"));
		if (csp.getBases() != null) {
			final StringBuilder sb = new StringBuilder();
			sb.append("bases");
			for (int base : csp.getBases()) {
				sb.append(" "+base);
			}
			mapWriter.write(sb.toString());
			mapWriter.write('\n');
		}
		if (csp.getObjectiveVariable() != null) {
			String s = "objective ";
			if (csp.getObjective().equals(CSP.Objective.MINIMIZE)) {
				s += SugarConstants.MINIMIZE;
			} else if (csp.getObjective().equals(CSP.Objective.MAXIMIZE)) {
				s += SugarConstants.MAXIMIZE;
			}
			s += " " + csp.getObjectiveVariable().getName();
			mapWriter.write(s);
			mapWriter.write('\n');
		}

		final List<IntegerVariable> bigints = new ArrayList<IntegerVariable>();
		for (IntegerVariable v : csp.getIntegerVariables()) {
			if (v.getDigits().length >= 2 && !v.isAux()) {
				final StringBuilder sb = new StringBuilder();
				sb.append("bigint " + v.getName() + " " + v.getOffset());
				for (IntegerVariable digit : v.getDigits()) {
					sb.append(" "+digit.getName());
				}
				mapWriter.write(sb.toString());
				mapWriter.write('\n');
			} else if (v.isDigit() || !v.isAux() || SugarMain.debug > 0) {
				final long code = v.getCode();
				StringBuilder sb = new StringBuilder();
				sb.append("int " + v.getName() + " " + v.getOffset()+ " " + code + " ");
				v.getDomain().appendValues(sb);
				mapWriter.write(sb.toString());
				mapWriter.write('\n');
			}
		}

		for (BooleanVariable v : csp.getBooleanVariables()) {
			if (! v.isAux() || SugarMain.debug > 0) {
				final long code = v.getCode();
				final String s = "bool " + v.getName() + " " + code;
				mapWriter.write(s);
				mapWriter.write('\n');
			}
		}
		mapWriter.close();
	}

	public String summary() {
		return
			writer.getSatVariablesCount() + " SAT variables, " +
			writer.getSatClausesCount() + " SAT clauses, " +
			writer.getSatFileSize() + " bytes";
	}

	protected long[] expand(long[] clause0, int n) {
		final long[] clause = new long[clause0.length + n];
		for (int i = 0; i < clause0.length; i++) {
			clause[i + n] = clause0[i];
		}
		return clause;
	}
}
