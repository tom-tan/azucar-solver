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
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map.Entry;

import jp.ac.kobe_u.cs.sugar.converter.Converter;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.Literal;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.LinearLiteral;
import jp.ac.kobe_u.cs.sugar.csp.LinearSum;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.IntegerDomain;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.encoder.Encoder;
import jp.ac.kobe_u.cs.sugar.expression.Expression;
import jp.ac.kobe_u.cs.sugar.expression.Parser;

class Adjuster{
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
	
	public static CSP adjust(CSP csp)throws SugarException{
    Logger.fine("Adjust the lower bound of integer variables to 0");
    for(IntegerVariable v: csp.getIntegerVariables()) {
      IntegerDomain d = v.getDomain();
      int offset = d.getLowerBound();
      v.setOffset(offset);
      if(!d.isContiguous()) {
        int lst = d.getLowerBound()-1;
        Iterator<Integer> iter = d.values();
        while(iter.hasNext()) {
          int i = iter.next();
          if(lst+1 != i) {
            BooleanVariable b = new BooleanVariable();
            Clause c1 = new Clause(new LinearLiteral(new LinearSum(1, v, lst)));
            c1.add(new BooleanLiteral(b, true));
            c1.setComment("; "+ v.getName() + " <= " + Integer.toString(lst)
                          + " || "
                          + v.getName() + " >= " + Integer.toString(i));
            csp.add(c1);
            Clause c2 = new Clause(new LinearLiteral(new LinearSum(-1, v, -i)));
            c2.add(new BooleanLiteral(b, false));
            csp.add(c2);
          }
          lst = i;
        }
      }
      v.setDomain(new IntegerDomain(0, d.getUpperBound()-offset));
    }

    List<Clause> newClauses = new ArrayList<Clause>();
    for(Clause c: csp.getClauses()) {
      Clause newCls = null;
      if((c.size() - c.simpleSize()) == 0) {
        newCls = c;
      }else{
        assert(c.simpleSize() == 1);
        Pair<LinearLiteral, List<Literal>>
          pair = splitLiterals(c);
        assert(pair.first() != null);
        LinearSum ls = pair.first().getLinearExpression();
        for(Entry<IntegerVariable,
              Integer> es : ls.getCoef().entrySet()) {
          ls.setB(ls.getB()+es.getKey().getOffset()*es.getValue());
        }
        newCls = new Clause(new LinearLiteral(ls));
        newCls.addAll(pair.second());
      }
      assert(newCls != null);
      newClauses.add(newCls);
    }

    csp.setClauses(newClauses);
    Logger.info("CSP : " + csp.summary());
		return csp;
	}

  static class Pair<P1, P2> {
    private P1 fst;
    private P2 snd;
    public Pair(P1 f, P2 s) {
      fst = f;
      snd = s;
    }

    public P1 first() {
      return fst;
    }
    public P2 second() {
      return snd;
    }
  }

  static Pair<LinearLiteral, List<Literal>>
    splitLiterals(Clause c)throws SugarException{
    assert(!c.isSimple());
    LinearLiteral ll = null;
    List<Literal> bls = new ArrayList<Literal>();
    for(Literal l: c.getLiterals()) {
      if(l instanceof LinearLiteral) {
        if(ll != null) throw new SugarException("Clauses must be simple");
        ll = (LinearLiteral)l;
      }else{
        assert(l instanceof BooleanLiteral);
        bls.add(l);
      }
    }
    assert((ll == null ?  bls.size() : bls.size()+1) == c.size());
    return new Pair<LinearLiteral, List<Literal>>(ll, bls);
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
		if (n != 3) {
			System.out.println("Usage : java Adjuster [-v] inputCSPFile outCSPFile mapFile");
			System.exit(1);
		}
		String cspFileName = args[i];
		String outFileName = args[i+1];
		String mapFile     = args[i+2];

		CSP csp = readCSP(cspFileName);
		csp = adjust(csp);
    File map = new File(mapFile);
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(map)));
    for(IntegerVariable v: csp.getIntegerVariables()) {
      pw.println(v.getName() + " " + v.getOffset());
    }
    pw.close();

		File file = new File(outFileName);
		PrintWriter pw1 = new PrintWriter(new BufferedWriter(new FileWriter(file)));
		pw1.println(csp);
    pw1.close();
	}
}