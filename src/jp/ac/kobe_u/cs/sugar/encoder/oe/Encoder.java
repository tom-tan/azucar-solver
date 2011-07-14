package jp.ac.kobe_u.cs.sugar.encoder.oe;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.StreamTokenizer;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.encoder.AbstractEncoder;
import jp.ac.kobe_u.cs.sugar.Logger;
import jp.ac.kobe_u.cs.sugar.SugarConstants;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.SugarMain;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.Operator;
import jp.ac.kobe_u.cs.sugar.csp.LinearLiteral;
import jp.ac.kobe_u.cs.sugar.csp.LinearSum;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;

/**
 * Encoder encodes CSP into SAT.
 * @see CSP 
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class Encoder extends AbstractEncoder{
	public Encoder(CSP csp) {
    super(csp);
	}

  public void encode(IntegerVariable ivar)throws SugarException, IOException{
    ivar.encode(this);
  }

  public void encode(Clause cl)throws SugarException, IOException{
    cl.encode(this);
  }

  /**
   * 符号化しやすい節に還元する．
   * 1. 全ての LinearLiteral を
   *  a1*x1+a2*x2+...+b <= 0
   *  の形にする．
   * 2. TODO LinearLiteral 中の整数変数の数を制限する．
   *  (Not implemented)
   **/
  public void reduce()throws SugarException{
    final String AUX_PREFIX = "R";
    BooleanVariable.setPrefix(AUX_PREFIX);
    BooleanVariable.setIndex(0);
    IntegerVariable.setPrefix(AUX_PREFIX);
    IntegerVariable.setIndex(0);

    List<Clause> newClauses = new ArrayList<Clause>();
    for (Clause c: csp.getClauses()) {
      if(c.getArithmeticLiterals().size() == 0) {
        newClauses.add(c);
      }else{
        assert(c.getArithmeticLiterals().size() == 1);
        LinearLiteral ll = c.getArithmeticLiterals().get(0);
        List<BooleanLiteral> bls = c.getBooleanLiterals();
        switch(ll.getOperator()) {
        case LE:{
          newClauses.add(c);
          break;
        }
        case EQ:{
          Clause c1 = new Clause(bls);
          c1.add(new LinearLiteral(ll.getLinearExpression(),
                                   Operator.LE));
          newClauses.add(c1);
          Clause c2 = new Clause(bls);
          LinearSum ls = new LinearSum(ll.getLinearExpression());
          ls.multiply(-1);
          c2.add(new LinearLiteral(ls, Operator.LE));
          newClauses.add(c2);
          break;
        }
        case NE:{
          Clause c1 = new Clause(bls);
          LinearSum ls1 = new LinearSum(ll.getLinearExpression());
          ls1.setB(ls1.getB()+1);
          c1.add(new LinearLiteral(ls1, Operator.LE));
          newClauses.add(c1);

          LinearSum ls2 = new LinearSum(ll.getLinearExpression());
          ls2.multiply(-1);
          ls2.setB(ls2.getB()+1);
          BooleanVariable p = new BooleanVariable();
          csp.add(p);
          BooleanLiteral posLiteral = new BooleanLiteral(p, false);
          BooleanLiteral negLiteral = new BooleanLiteral(p, true);
          Clause c2 = new Clause();
          c2.add(negLiteral);
          c2.add(new LinearLiteral(ls2, Operator.LE));
          c1.add(posLiteral);
          newClauses.add(c2);
          break;
        }
        default: new SugarException("Internal Error");
        }
      }
    }
    csp.setClauses(newClauses);
  }

	public void outputMap(String mapFileName) throws SugarException, IOException {
		BufferedWriter mapWriter = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(mapFileName), "UTF-8"));
//		BufferedOutputStream mapFile =
//			new BufferedOutputStream(new FileOutputStream(mapFileName));
		if (csp.getObjectiveVariable() != null) {
			String s = "objective ";
			if (csp.getObjective().equals(CSP.Objective.MINIMIZE)) {
				s += SugarConstants.MINIMIZE;
			} else if (csp.getObjective().equals(CSP.Objective.MAXIMIZE)) {
				s += SugarConstants.MAXIMIZE;
			}
			s += " " + csp.getObjectiveVariable().getName();
//			mapFile.write(s.getBytes());
//			mapFile.write('\n');
			mapWriter.write(s);
			mapWriter.write('\n');
		}
		for (IntegerVariable v : csp.getIntegerVariables()) {
			if (! v.isAux() || SugarMain.debug > 0) {
				int code = v.getCode();
				StringBuilder sb = new StringBuilder();
				sb.append("int " + v.getName() + " " + code + " ");
				v.getDomain().appendValues(sb);
//				mapFile.write(sb.toString().getBytes());
//				mapFile.write('\n');
				mapWriter.write(sb.toString());
				mapWriter.write('\n');
			}
		}
		for (BooleanVariable v : csp.getBooleanVariables()) {
			if (! v.isAux() || SugarMain.debug > 0) {
				int code = v.getCode();
				String s = "bool " + v.getName() + " " + code;
//				mapFile.write(s.getBytes());
//				mapFile.write('\n');
				mapWriter.write(s);
				mapWriter.write('\n');
			}
		}
//		mapFile.close();
		mapWriter.close();
	}

	public boolean decode(String outFileName) throws SugarException, IOException {
		String result = null;
		boolean sat = false;
		BufferedReader rd = new BufferedReader(new FileReader(outFileName));
		StreamTokenizer st = new StreamTokenizer(rd);
		st.eolIsSignificant(true);
		while (result == null) {
			st.nextToken();
			if (st.ttype == StreamTokenizer.TT_WORD) {
				if (st.sval.equals("c")) {
					do {
						st.nextToken();
					} while (st.ttype != StreamTokenizer.TT_EOL);
				} else if (st.sval.equals("s")) {
					st.nextToken();
					result = st.sval;
				} else {
					result = st.sval;
				}
			} else {
				throw new SugarException("Unknown output " + st.sval);
			}
		} 
		if (result.startsWith("SAT")) {
			sat = true;
			BitSet satValues = new BitSet();
			while (true) {
				st.nextToken();
				if (st.ttype == StreamTokenizer.TT_EOF)
					break;
				switch (st.ttype) {
				case StreamTokenizer.TT_EOL:
					break;
				case StreamTokenizer.TT_WORD:
					if (st.sval.equals("v")) {
					} else if (st.sval.equals("c")) {
						do {
							st.nextToken();
						} while (st.ttype != StreamTokenizer.TT_EOL);
					} else {
						throw new SugarException("Unknown output " + st.sval);
					}
					break;
				case StreamTokenizer.TT_NUMBER:
					int value = (int)st.nval;
					int i = Math.abs(value);
					if (i > 0) {
						satValues.set(i, value > 0);
					}
					break;
				default:
					throw new SugarException("Unknown output " + st.sval);
				}
			}
			for (IntegerVariable v : csp.getIntegerVariables()) {
				v.decode(satValues);
			}
			for (BooleanVariable v : csp.getBooleanVariables()) {
				v.decode(satValues);
			}
		} else if (result.startsWith("UNSAT")) {
			sat = false;
		} else {
			throw new SugarException("Unknown output result " + result);
		}
		rd.close();
		return sat;
	}
}
