package jp.ac.kobe_u.cs.sugar;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import jp.ac.kobe_u.cs.sugar.converter.Converter;
import jp.ac.kobe_u.cs.sugar.converter.Decomposer;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.encoder.EncodingFactory;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;
import jp.ac.kobe_u.cs.sugar.encoder.Decoder;
import jp.ac.kobe_u.cs.sugar.encoder.oe.OrderEncodingFactory;
import jp.ac.kobe_u.cs.sugar.encoder.coe.CompactOrderEncodingFactory;
import jp.ac.kobe_u.cs.sugar.expression.Expression;
import jp.ac.kobe_u.cs.sugar.expression.Parser;

/**
 * SugarMain main class.
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class SugarMain {
	boolean maxCSP = false;
	boolean competition = false;
	boolean incremental = false;
	boolean propagate = true;
	public static int debug = 0;
	public EncodingFactory ef;

	private List<Expression> toMaxCSP(List<Expression> expressions0) throws SugarException {
		List<Expression> expressions = new ArrayList<Expression>();
		List<Expression> sum = new ArrayList<Expression>();
		sum.add(Expression.ADD);
		int n = 0;
		for (Expression x: expressions0) {
			if (x.isSequence(Expression.DOMAIN_DEFINITION)
				|| x.isSequence(Expression.INT_DEFINITION)
				|| x.isSequence(Expression.BOOL_DEFINITION)
				|| x.isSequence(Expression.PREDICATE_DEFINITION)
				|| x.isSequence(Expression.RELATION_DEFINITION)) {
				expressions.add(x);
			} else if (x.isSequence(Expression.OBJECTIVE_DEFINITION)){
				throw new SugarException("Illegal " + x);
			} else {
				// (int _Cn 0 1)
				// (or (ge _Cn 1) constraint)
				Expression c = Expression.create("_C" + n);
				expressions.add(Expression.create(
						Expression.INT_DEFINITION,
						c,
						Expression.ZERO,
						Expression.ONE));
				x = (c.ge(Expression.ONE)).or(x);
				expressions.add(x);
				sum.add(c);
				n++;
			}
		}
		// (int _COST 0 n)
		// (ge _COST (add _C1 ... _Cn))
		// (objective minimize _COST)
		Expression cost = Expression.create("_COST");
		expressions.add(Expression.create(
				Expression.INT_DEFINITION,
				cost,
				Expression.ZERO,
				Expression.create(n)));
		expressions.add(cost.ge(Expression.create(sum)));
		expressions.add(Expression.create(
				Expression.OBJECTIVE_DEFINITION,
				Expression.MINIMIZE,
				cost));
		Logger.info("MAX CSP: " + n + " constraints");
		return expressions;
	}

	public void encode(String cspFileName, String satFileName, String mapFileName)
	throws SugarException, IOException {
		Logger.fine("Parsing " + cspFileName);
		InputStream in;
		if (cspFileName.endsWith(".gz")) {
			in = new GZIPInputStream(new FileInputStream(cspFileName));
		} else {
			in = new FileInputStream(cspFileName);
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		Parser parser = new Parser(reader, false);
		List<Expression> expressions = parser.parse();
		Logger.info("parsed " + expressions.size() + " expressions");
		Logger.status();
		if (maxCSP) {
			expressions = toMaxCSP(expressions);
		}
		Logger.fine("Decomposing CSP");
		Decomposer decomposer = new Decomposer();
		expressions = decomposer.decompose(expressions);

		Logger.fine("Converting to clausal form CSP");
		CSP csp = new CSP();
		Converter converter = new Converter(csp);
		converter.INCREMENTAL_PROPAGATE = propagate;
		converter.convert(expressions);
		Logger.fine("CSP : " + csp.summary());
		assert csp.getIntegerVariables().get(0).getDomain().isContiguous():
		"Assertion failure!!";
		// csp.output(System.out, "c ");
		// if (propagate) {
		// 	Logger.status();
		// 	Logger.fine("Propagation in CSP");
		// 	csp.propagate();
		// 	Logger.fine("CSP : " + csp.summary());
		// }
		// csp.output(System.out, "c ");
		Logger.status();
		if (csp.isUnsatisfiable()) {
			Logger.info("CSP is unsatisfiable after propagation");
			Logger.println("s UNSATISFIABLE");
		} else {
			// Logger.info("CSP : " + csp.summary());
			// if (debug > 0) {
			// 	csp.output(System.out, "c ");
			// }
			// Logger.status();

			parser = null;
			converter = null;
			expressions = null;
			decomposer = null;
			Expression.clear();
			Runtime.getRuntime().gc();

			if (csp.isUnsatisfiable()) {
				Logger.info("CSP is unsatisfiable after propagation");
				Logger.println("s UNSATISFIABLE");
			} else {
				Logger.fine("Encoding CSP to SAT : " + satFileName);
				Encoder encoder = ef.createEncoder(csp);
				encoder.reduce();
				if (csp.isUnsatisfiable()) {
					Logger.info("CSP is unsatisfiable after propagation");
					Logger.println("s UNSATISFIABLE");
				} else {
					encoder.encode(satFileName, incremental);
					Logger.fine("Writing map file : " + mapFileName);
					encoder.outputMap(mapFileName);
					Logger.status();
					Logger.info("SAT : " + encoder.summary());}
			}
		}
	}

	public void decode(String outFileName, String mapFileName)
	throws SugarException, IOException {
		Logger.fine("Decoding " + outFileName);
		CSP csp = new CSP();
		String objectiveVariableName = null;
		BufferedReader rd = new BufferedReader(
				new InputStreamReader(new FileInputStream(mapFileName), "UTF-8"));
		while (true) {
			String line = rd.readLine();
			if (line == null)
				break;
			String[] s = line.split("\\s+");
			if (s[0].equals("objective")) {
				// multi-objective にするなら要変更
				if (s[1].equals(SugarConstants.MINIMIZE)) {
					csp.setObjective(CSP.Objective.MINIMIZE);
				} else if (s[1].equals(SugarConstants.MAXIMIZE)) {
					csp.setObjective(CSP.Objective.MAXIMIZE);
				}
				objectiveVariableName = s[2];
			} else if (s[0].equals("bases")) {
				int[] bases = new int[s.length-1];
				for (int i=1; i<s.length ; i++) {
					bases[i-1] = Integer.parseInt(s[i]);
				}
				ef.setBases(bases);
			} else if (s[0].equals("bigint")) {
				String name = s[1];
				int offset = Integer.parseInt(s[2]);
				IntegerVariable[] digits = new IntegerVariable[s.length-3];
				for (int i=3, j=0; i<s.length ; i++, j++) {
					IntegerVariable di = csp.getIntegerVariable(s[i]);
					assert di != null;
					di.isDigit(true);
					digits[j] = di;
				}
				IntegerVariable v = new IntegerVariable(name, digits);
				v.setOffset(offset);
				csp.add(v);
			} else if (s[0].equals("int")) {
				String name = s[1];
				int offset = Integer.parseInt(s[2]);
				int code = Integer.parseInt(s[3]);
				IntegerDomain domain = null;
				if (s.length == 5) {
					int lb;
					int ub;
					int pos = s[4].indexOf("..");
					if (pos < 0) {
						lb = ub = Integer.parseInt(s[4]);
					} else {
						lb = Integer.parseInt(s[4].substring(0, pos));
						ub = Integer.parseInt(s[4].substring(pos+2));
					}
					domain = new IntegerDomain(lb, ub);
				} else {
					SortedSet<Integer> d = new TreeSet<Integer>();
					for (int i = 4; i < s.length; i++) {
						int lb;
						int ub;
						int pos = s[i].indexOf("..");
						if (pos < 0) {
							lb = ub = Integer.parseInt(s[i]);
						} else {
							lb = Integer.parseInt(s[i].substring(0, pos));
							ub = Integer.parseInt(s[i].substring(pos+2));
						}
						for (int value = lb; value <= ub; value++) {
							d.add(value);
						}
					}
					domain = new IntegerDomain(d);
				}
				IntegerVariable v = new IntegerVariable(name, domain);
				v.setOffset(offset);
				v.setCode(code);
				csp.add(v);
				if (name.equals(objectiveVariableName)) {
					csp.setObjectiveVariable(v);
				}
			} else if (s[0].equals("bool")) {
				// TODO
				String name = s[1];
				int code = Integer.parseInt(s[2]);
				BooleanVariable v = new BooleanVariable(name);
				v.setCode(code);
				csp.add(v);
			}
		}
		rd.close();
		Decoder encoder = ef.createDecoder(csp);
		if (encoder.decode(outFileName)) {
			if (csp.getObjectiveVariable() == null) {
				Logger.println("s SATISFIABLE");
			} else {
				String name = csp.getObjectiveVariable().getName();
				int value = csp.getObjectiveVariable().getValue();
				Logger.println("c OBJECTIVE " + name + " " + value);
				Logger.println("o " + value);
			}
			if (competition) {
				Logger.print("v");
				for (IntegerVariable v : csp.getIntegerVariables()) {
					if (! v.isAux() && ! v.getName().startsWith("_")) {
						Logger.print(" " + v.getValue());
					}
				}
				Logger.println("");
			} else {
				for (IntegerVariable v : csp.getIntegerVariables()) {
					if (! v.isAux() && ! v.getName().startsWith("_")) {
						Logger.println("a " + v.getName() + "\t" + v.getValue());
					}
				}
				for (BooleanVariable v : csp.getBooleanVariables()) {
					if (! v.isAux() && ! v.getName().startsWith("_")) {
						Logger.println("a " + v.getName() + "\t" + v.getValue());
					}
				}
				Logger.println("a");
			}
		} else {
			Logger.println("s UNSATISFIABLE");
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			SugarMain sugarMain = new SugarMain();
			String option = null;
			int base = 0;
			int ndigits = 0;
			int i = 0;
			while (i < args.length) {
				if (args[i].equals("-max")) {
					sugarMain.maxCSP = true;
				} else if (args[i].equals("-competition")) {
					sugarMain.competition = true;
				} else if (args[i].equals("-incremental")) {
					sugarMain.incremental = true;
				} else if (args[i].equals("-option") && i + 1 < args.length) {
					String[] opts = args[i+1].split(",");
					for (String opt : opts) {
						if (opt.matches("(no_)?pigeon")) {
							Converter.OPT_PIGEON = ! opt.startsWith("no_");
						} else if (opt.matches("(no_)?compact")) {
							//Encoder.OPT_COMPACT = ! opt.startsWith("no_");
						} else if (opt.matches("(no_)?estimate_satsize")) {
							Converter.ESTIMATE_SATSIZE = ! opt.startsWith("no_");
							//Encoder.OPT_COMPACT = ! opt.startsWith("no_");
						} else if (opt.matches("(no_)?new_variable")) {
							Converter.NEW_VARIABLE = ! opt.startsWith("no_");
						} else if (opt.matches("equiv=(\\d+)")) {
							int n = "equiv=".length();
							Converter.MAX_EQUIVMAP_SIZE = Integer.parseInt(opt.substring(n));
						} else if (opt.matches("linearsum=(\\d+)")) {
							int n = "linearsum=".length();
							Converter.MAX_LINEARSUM_SIZE = Long.parseLong(opt.substring(n));
						} else if (opt.matches("split=(\\d+)")) {
							int n = "split=".length();
							Converter.SPLITS = Integer.parseInt(opt.substring(n));
						} else if (opt.matches("domain=(\\d+)")) {
							int n = "domain=".length();
							IntegerDomain.MAX_SET_SIZE = Integer.parseInt(opt.substring(n));
						} else {
							throw new SugarException("Unknown option " + opt);
						}
					}
					i++;
				} else if (args[i].equals("-debug") && i + 1 < args.length) {
					debug = Integer.parseInt(args[i+1]);
					i++;
				} else if (args[i].equals("-m") && i+1 < args.length) {
					ndigits = Integer.parseInt(args[i+1]);
					if (base != 0) {
						throw new SugarException("Base and NDigits are exclusive");
					}
				} else if (args[i].equals("-b") && i+1 < args.length) {
					base = Integer.parseInt(args[i+1]);
					if (ndigits != 0) {
						throw new SugarException("Base and NDigits are exclusive");
					}
				} else if (args[i].equals("-encoding") && i+1 < args.length) {
					String enc = args[i+1];
					if (enc.equals("oe")) {
						sugarMain.ef = OrderEncodingFactory.getInstance();
					} else if (enc.equals("coe")) {
						sugarMain.ef = CompactOrderEncodingFactory.getInstance();
					} else {
						throw new SugarException("Not supported encoding: "+ enc);
					}
				} else if (args[i].equals("-v") || args[i].equals("-verbose")) {
					Logger.verboseLevel++;
				} else if (args[i].startsWith("-")) {
					option = args[i];
					break;
				}
				i++;
			}

			if (sugarMain.ef == null) {
				sugarMain.ef = CompactOrderEncodingFactory.getInstance();
			}
			if (base != 0) {
				int[] bases = new int[1];
				bases[0] = base;
				sugarMain.ef.setBases(bases);
			}else if (ndigits != 0) {
				sugarMain.ef.setNDigits(ndigits);
			}
			int n = args.length - i;
			if (option.equals("-encode") && n == 4) {
				String cspFileName = args[i+1];
				String satFileName = args[i+2];
				String mapFileName = args[i+3];
				sugarMain.encode(cspFileName, satFileName, mapFileName);
			} else if (option.equals("-decode") && n == 3) {
				String outFileName = args[i+1];
				String mapFileName = args[i+2];
				sugarMain.decode(outFileName, mapFileName);
			} else {
				String s = "";
				for (String a : args) {
					s += " " + a;
				}
				throw new SugarException("Invalid arguments " + s);
			}
			Logger.status();
		} catch (Exception e) {
			Logger.println("c ERROR Exception " + e.getMessage());
			for (StackTraceElement t : e.getStackTrace()) {
				Logger.info(t.toString());
			}
			Logger.println("s UNKNOWN");
			System.exit(1);
		} catch (Error e) {
			Logger.println("c ERROR Exception " + e.getMessage());
			for (StackTraceElement t : e.getStackTrace()) {
				Logger.info(t.toString());
			}
			Logger.println("s UNKNOWN");
			System.exit(1);
		}
	}
}
