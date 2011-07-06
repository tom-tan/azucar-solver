package jp.ac.kobe_u.cs.sugar;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.ArrayList;

import jp.ac.kobe_u.cs.sugar.converter.Converter;
import jp.ac.kobe_u.cs.sugar.converter.Decomposer;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.Literal;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;
import jp.ac.kobe_u.cs.sugar.expression.Expression;
import jp.ac.kobe_u.cs.sugar.expression.Parser;

class Simplifier{
	public static boolean simplifyAll = true;
	private CSP csp;

	public Simplifier(CSP csp) {
		this.csp = csp;
	}
	
	private static CSP readCSP(String cspFileName)
		throws SugarException, IOException {
		Logger.fine("Parsing " + cspFileName);
		InputStream in = new FileInputStream(cspFileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		Parser parser = new Parser(reader, true);
		List<Expression> expressions = parser.parse();
		Logger.info("parsed " + expressions.size() + " expressions");
		Logger.status();
		long start = System.currentTimeMillis();
		Decomposer dec = new Decomposer();
		expressions = dec.decompose(expressions);
		long end = System.currentTimeMillis();
		System.err.println("Decomposed in "+ (end - start) + " msec");
		Logger.fine("Converting to clausal form CSP");
		CSP csp = new CSP();
		Converter converter = new Converter(csp);
		converter.convert(expressions);
		Logger.fine("CSP : " + csp.summary());
		return csp;
	}

  private List<Clause> simplify(Clause clause) throws SugarException {
		List<Literal> literals = clause.getLiterals();
		List<Clause> newClauses = new ArrayList<Clause>();
		clause = new Clause();
		int complex = 0;
		for (Literal literal : literals) {
			if (literal.isSimple()) {
				clause.add(literal);
			} else {
				complex++;
				if (! simplifyAll && complex == 1) {
					clause.add(literal);
				} else {
					BooleanVariable p = new BooleanVariable();
					csp.add(p);
					Literal posLiteral = new BooleanLiteral(p, false);
					Literal negLiteral = new BooleanLiteral(p, true);
					Clause newClause = new Clause();
					newClause.add(negLiteral);
					newClause.add(literal);
					newClauses.add(newClause);
					clause.add(posLiteral);
				}
			}
		}
		newClauses.add(clause);
		return newClauses;
	}
	
	public void simplify() throws SugarException {
    BooleanVariable.setPrefix("S");
    BooleanVariable.setIndex(0);
		List<Clause> newClauses = new ArrayList<Clause>();
		for (Clause clause : csp.getClauses()) {
			if (clause.isSimple()) {
				newClauses.add(clause);
			} else {
				newClauses.addAll(simplify(clause));
			}
		}
		csp.setClauses(newClauses);
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
			System.out.println("Usage : java Simplifier [-v] inputCSPFile outCSPFile");
			System.exit(1);
		}
		String cspFileName = args[i];
		String outFileName = args[i+1];
		long start = System.currentTimeMillis();
		CSP csp = readCSP(cspFileName);
		long parsed = System.currentTimeMillis();
		System.err.println("Parsed in "+ (parsed - start) + " msec");
		Simplifier simp = new Simplifier(csp);
		System.err.println("Simplifing CSP by introducing new Boolean variables");
		simp.simplify();
		long simpTime = System.currentTimeMillis()-parsed;
		System.err.println("Simplified in "+ simpTime + " msec");
		Logger.info("CSP : " + csp.summary());
		File file = new File(outFileName);
		PrintStream ps = new PrintStream(file);
		if (csp.isUnsatisfiable()) {
			ps.println("(or)");
		}else{
			csp.output(ps, "");
		}
		ps.close();
		long writed = System.currentTimeMillis();
	}
}
