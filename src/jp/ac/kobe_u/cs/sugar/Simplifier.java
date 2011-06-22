package jp.ac.kobe_u.cs.sugar;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.converter.Converter;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;
import jp.ac.kobe_u.cs.sugar.expression.Expression;
import jp.ac.kobe_u.cs.sugar.expression.Parser;

class Simplifier{
	
	private static CSP readCSP(String cspFileName)
		throws SugarException, IOException {
		Logger.fine("Parsing " + cspFileName);
		InputStream in = new FileInputStream(cspFileName);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		Parser parser = new Parser(reader, true);
		List<Expression> expressions = parser.parse();
		Logger.info("parsed " + expressions.size() + " expressions");
		Logger.status();
		Logger.fine("Converting to clausal form CSP");
		CSP csp = new CSP();
		Converter converter = new Converter(csp);
		converter.convert(expressions);
		Logger.fine("CSP : " + csp.summary());
		return csp;
	}
	
	public static CSP simplify(CSP csp)throws SugarException{
    final String pre = "simp";
    IntegerVariable.setPrefix(pre);
    BooleanVariable.setPrefix(pre);
    Logger.fine("Simplifing CSP by introducing new Boolean variables");
    csp.simplify();
    Logger.info("CSP : " + csp.summary());
		return csp;
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

		CSP csp = readCSP(cspFileName);
		csp = simplify(csp);
		File file = new File(outFileName);
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
		pw.println(csp);
    pw.close();
	}
}
