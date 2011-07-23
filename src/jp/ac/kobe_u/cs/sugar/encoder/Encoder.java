package jp.ac.kobe_u.cs.sugar.encoder;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map.Entry;

import jp.ac.kobe_u.cs.sugar.Logger;
import jp.ac.kobe_u.cs.sugar.SugarConstants;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.SugarMain;
import jp.ac.kobe_u.cs.sugar.csp.Literal;
import jp.ac.kobe_u.cs.sugar.csp.LinearSum;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.Operator;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.LinearLiteral;
import jp.ac.kobe_u.cs.sugar.csp.ArithmeticLiteral;

/**
 * Encoder encodes CSP into SAT.
 * @see CSP 
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public abstract class Encoder {
	public static boolean simplifyAll = false;

	public static final int FALSE_CODE = 0;

	public static final int TRUE_CODE = Integer.MIN_VALUE;

	protected CSP csp;

	protected CNFWriter writer;

	public abstract int getCode(LinearLiteral lit) throws SugarException;
	protected abstract void encode(IntegerVariable v) throws SugarException, IOException;
	protected abstract void encode(LinearLiteral lit, int[] clause) throws SugarException, IOException;
	public abstract int getSatVariablesSize(IntegerVariable ivar);
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

	public static int negateCode(int code) {
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

	protected int getCode(BooleanLiteral lit) throws SugarException {
		int code = lit.getBooleanVariable().getCode();
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
		int[] clause = new int[simpleSize(cl)];
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
		List<Clause> ret = new ArrayList<Clause>();
		IntegerDomain d = v.getDomain();
		if (! d.isContiguous()) {
			int lst = d.getLowerBound()-1;
			Iterator<Integer> iter = d.values();
			while(iter.hasNext()) {
				int i = iter.next();
				if (lst+2 == i) {
					Clause c = new Clause(new LinearLiteral(new LinearSum(1, v, -lst),
																									Operator.NE));
					ret.add(c);
				} else
					if (lst+1 != i) {
					BooleanVariable b = new BooleanVariable();
					Clause c1 = new Clause(new LinearLiteral(new LinearSum(1, v, -lst),
																									 Operator.LE));
					c1.add(new BooleanLiteral(b, true));
					c1.setComment("; "+ v.getName() + " <= " + lst + " || "
												+ v.getName() + " >= " + i);
					ret.add(c1);
					Clause c2 = new Clause(new LinearLiteral(new LinearSum(-1, v, i),
																									 Operator.LE));
					c2.add(new BooleanLiteral(b, false));
					ret.add(c2);
				}
				lst = i;
			}
		}
		int offset = d.getLowerBound();
		if (useOffset) {
			v.setOffset(offset);
			v.setDomain(new IntegerDomain(0, d.getUpperBound()-offset));
		} else {
			Clause c = new Clause(new LinearLiteral(new LinearSum(-1, v, offset),
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

		List<Clause> newClauses = new ArrayList<Clause>();
		for (Clause c: csp.getClauses()) {
			Clause newCls = null;
			if(c.getArithmeticLiterals().isEmpty()) {
				newCls = c;
			} else {
				newCls = new Clause(c.getBooleanLiterals());
				newCls.setComment(c.getComment());

				for (ArithmeticLiteral lit: c.getArithmeticLiterals()) {
					LinearLiteral ll = (LinearLiteral)lit;
					LinearSum ls = ll.getLinearExpression();
					for (Entry<IntegerVariable, Integer> es :
								 ls.getCoef().entrySet()) {
						ls.setB(ls.getB()+es.getKey().getOffset()*es.getValue());
					}
					newCls.add(new LinearLiteral(ls, ll.getOperator()));
				}
			}
			assert newCls != null;
			newClauses.add(newCls);
		}

		csp.setClauses(newClauses);
		Logger.info("CSP : " + csp.summary());
	}

	public List<Clause> simplify(Clause clause) throws SugarException {
		List<Clause> newClauses = new ArrayList<Clause>();
		Clause c = new Clause(clause.getBooleanLiterals());
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
					BooleanVariable p = new BooleanVariable();
					csp.add(p);
					Literal posLiteral = new BooleanLiteral(p, false);
					Literal negLiteral = new BooleanLiteral(p, true);
					Clause newClause = new Clause();
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
		List<Clause> newClauses = new ArrayList<Clause>();
		for (Clause clause : csp.getClauses()) {
			if (isSimple(clause)) {
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
			int size = getSatVariablesSize(v);
			writer.addSatVariables(size);
		}
		for (BooleanVariable v : csp.getBooleanVariables()) {
			v.setCode(writer.getSatVariablesCount() + 1);
			int size = getSatVariablesSize(v);
			writer.addSatVariables(size);
		}

		int count = 0;
		int n = csp.getIntegerVariables().size();
		int percent = 10;
		for (IntegerVariable v : csp.getIntegerVariables()) {
			encode(v);
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
			int satClausesCount0 = writer.getSatClausesCount();
			if (! c.isValid()) {
				encode(c);
			}
			count++;
			if (SugarMain.debug >= 1) {
				int k = writer.getSatClausesCount() - satClausesCount0;
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
		BufferedWriter mapWriter = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(mapFileName), "UTF-8"));
		if (csp.getBases() != null) {
			StringBuilder sb = new StringBuilder();
			sb.append("bases");
			for (int base : csp.getBases()) {
				sb.append(" "+base);
			}
			mapWriter.write(sb.toString());
			mapWriter.write('\n');
		}
		if (csp.getObjectiveVariable() != null) {
			// muli-objective にするには変更が必要
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

		List<IntegerVariable> bigints = new ArrayList<IntegerVariable>();
		for (IntegerVariable v : csp.getIntegerVariables()) {
			if (v.getDigits().length >= 2 && !v.isAux()) {
				bigints.add(v);
			} else if (v.isDigit() || !v.isAux() || SugarMain.debug > 0) {
				int code = v.getCode();
				StringBuilder sb = new StringBuilder();
				sb.append("int " + v.getName() + " " + v.getOffset()+ " " + code + " ");
				v.getDomain().appendValues(sb);
				mapWriter.write(sb.toString());
				mapWriter.write('\n');
			}
		}

		for (IntegerVariable v : bigints) {
			StringBuilder sb = new StringBuilder();
			sb.append("bigint " + v.getName() + " " + v.getOffset());
			for (IntegerVariable digit : v.getDigits()) {
				sb.append(" "+digit.getName());
			}
			mapWriter.write(sb.toString());
			mapWriter.write('\n');
		}

		for (BooleanVariable v : csp.getBooleanVariables()) {
			if (! v.isAux() || SugarMain.debug > 0) {
				int code = v.getCode();
				String s = "bool " + v.getName() + " " + code;
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

	protected int[] expand(int[] clause0, int n) {
		int[] clause = new int[clause0.length + n];
		for (int i = 0; i < clause0.length; i++) {
			clause[i + n] = clause0[i];
		}
		return clause;
	}
}
