package jp.ac.kobe_u.cs.sugar.encoder;

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
import java.util.BitSet;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.Logger;
import jp.ac.kobe_u.cs.sugar.SugarConstants;
import jp.ac.kobe_u.cs.sugar.SugarException;
import jp.ac.kobe_u.cs.sugar.SugarMain;
import jp.ac.kobe_u.cs.sugar.csp.BooleanLiteral;
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;
import jp.ac.kobe_u.cs.sugar.csp.LinearLiteral;
import jp.ac.kobe_u.cs.sugar.csp.Literal;

/**
 * Encoder encodes CSP into SAT.
 * @see CSP 
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public abstract class AbstractEncoder {
	public static final int FALSE_CODE = 0;

	public static final int TRUE_CODE = Integer.MIN_VALUE;

	protected CSP csp;

	public static boolean USE_NEWIO = true;
	
	public static int SAT_BUFFER_SIZE = 4*1024;

	private BufferedOutputStream satFile;

	private StringBuilder satStringBuffer;

	private byte[] satByteArray;
	
	private FileChannel satFileChannel; 

	private ByteBuffer satByteBuffer;
	
	private int satVariablesCount = 0;

	private int satClausesCount = 0;
	
	private long satFileSize = 0;

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

	public AbstractEncoder(CSP csp) {
		this.csp = csp;
	}

	public int getSatClausesCount() {
		return satClausesCount;
	}

	public long getSatFileSize() {
		return satFileSize;
	}

  protected int getCode(BooleanLiteral lit)throws SugarException{
		int code = lit.getBooleanVariable().getCode();
		return lit.getNegative() ? -code : code;
  }
  protected abstract int getCode(LinearLiteral lit)throws SugarException;

	public String getHeader(int numOfVariables, int numOfClauses) throws UnsupportedEncodingException {
		int n = 64;
		StringBuilder s = new StringBuilder();
		s.append("p cnf ");
		s.append(Integer.toString(numOfVariables));
		s.append(" ");
		s.append(Integer.toString(numOfClauses));
		while (s.length() < n - 1) {
			s.append(" ");
		}
		s.append("\n");
		return s.toString();
	}

  protected abstract void encode(IntegerVariable v) throws SugarException, IOException;

  protected void encode(Clause cl) throws SugarException, IOException{
		if (! cl.isSimple()) {
			throw new SugarException("Cannot encode non-simple clause " + cl.toString());
		}
		writeComment(cl.toString());
		if (cl.isValid()) {
			return;
		}
		int[] clause = new int[cl.simpleSize()];
		LinearLiteral lit = null;
		int i = 0;
    for (BooleanLiteral literal : cl.getBooleanLiterals()) {
      assert(literal.isSimple());
      clause[i] = getCode(literal);
      i++;
    }
		for (LinearLiteral literal : cl.getArithmeticLiterals()) {
			if (literal.isSimple()) {
				clause[i] = getCode(literal);
				i++;
			} else {
				lit = literal;
			}
		}
		if (lit == null) {
			writeClause(clause);
		} else {
			encode(lit, clause);
		}
  }

  protected abstract void encode(LinearLiteral lit, int[] clause) throws SugarException, IOException;

	protected void encode(BooleanVariable v) {}

	protected void decode(BooleanVariable v, BitSet satValues) {
		v.setValue(satValues.get(v.getCode()));
	}

  protected abstract int getSatVariablesSize(IntegerVariable ivar);

  protected int getSatVariablesSize(BooleanVariable bvar) {
    return 1;
  }

  public abstract void reduce()throws SugarException;

	public void encode(String satFileName, boolean incremental) throws SugarException, IOException {
		satFileSize = 0;
		if (USE_NEWIO) {
			satFileChannel = (new FileOutputStream(satFileName)).getChannel();
			satByteBuffer = ByteBuffer.allocateDirect(SAT_BUFFER_SIZE);
		} else {
			satFile = new BufferedOutputStream(new FileOutputStream(satFileName));
			satStringBuffer = new StringBuilder(SAT_BUFFER_SIZE);
			satByteArray = new byte[SAT_BUFFER_SIZE];
		}
		write(getHeader(0, 0));
		satVariablesCount = 0;
		satClausesCount = 0;
		for (IntegerVariable v : csp.getIntegerVariables()) {
			v.setCode(satVariablesCount + 1);
			int size = getSatVariablesSize(v);
			satVariablesCount += size;
		}
		for (BooleanVariable v : csp.getBooleanVariables()) {
			v.setCode(satVariablesCount + 1);
			int size = getSatVariablesSize(v);
			satVariablesCount += size;
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
						+ " (" + satClausesCount + " clauses, " + satFileSize + " bytes)");
				percent += 10;
			}
		}
		count = 0;
		n = csp.getClauses().size();
		percent = 10;
		for (Clause c : csp.getClauses()) {
			int satClausesCount0 = satClausesCount;
			if (! c.isValid()) {
				encode(c);
			}
			count++;
			if (SugarMain.debug >= 1) {
				int k = satClausesCount - satClausesCount0;
				Logger.fine(k + " SAT clauses for " + c);
			}
			if ((100*count)/n >= percent) {
				Logger.fine(count + " (" + percent + "%) "
						+ "CSP clauses are encoded"
						+ " (" + satClausesCount + " clauses, " + satFileSize + " bytes)");
				percent += 10;
			}
		}
		flush();
		if (USE_NEWIO) {
			satFileChannel.close();
			satFileChannel = null;
			satByteBuffer = null;
		} else {
			satFile.close();
			satStringBuffer = null;
			satByteArray = null;
		}
		Logger.fine(count + " CSP clauses encoded");
		RandomAccessFile satFile1 = new RandomAccessFile(satFileName, "rw");
		satFile1.seek(0);
		if (csp.getObjectiveVariable() == null || incremental) {
			satFile1.write(getHeader(satVariablesCount, satClausesCount).getBytes());
		} else {
			satFile1.write(getHeader(satVariablesCount, satClausesCount + 1).getBytes());
		}
		satFile1.close();
	}

  public void outputMap(String mapFileName) throws SugarException, IOException {
		BufferedWriter mapWriter = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(mapFileName), "UTF-8"));
    if (!csp.getBases().isEmpty()) {
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
		for (IntegerVariable v : csp.getIntegerVariables()) {
      if (v.getDigits() != null) {
        StringBuilder sb = new StringBuilder();
        sb.append("bigint " + v.getName() + " " + v.getOffset());
        for (IntegerVariable digit : v.getDigits()) {
          sb.append(" "+digit.getName());
        }
        mapWriter.write(sb.toString());
        mapWriter.write('\n');
      }else if (v.isDigit() || !v.isAux() || SugarMain.debug > 0) {
        int code = v.getCode();
        StringBuilder sb = new StringBuilder();
        sb.append("int " + v.getName() + " " + v.getOffset()+ " " + code + " ");
        v.getDomain().appendValues(sb);
        mapWriter.write(sb.toString());
        mapWriter.write('\n');
      }
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

	protected abstract void decode(IntegerVariable v, BitSet satValues);
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
				decode(v, satValues);
			}
			for (BooleanVariable v : csp.getBooleanVariables()) {
				decode(v, satValues);
			}
		} else if (result.startsWith("UNSAT")) {
			sat = false;
		} else {
			throw new SugarException("Unknown output result " + result);
		}
		rd.close();
		return sat;
	}

	public String summary() {
		return
		satVariablesCount + " SAT variables, " +
		satClausesCount + " SAT clauses, " +
		satFileSize + " bytes";
	}

	public void flush() throws IOException {
		if (USE_NEWIO) {
			satByteBuffer.flip();
			satFileChannel.write(satByteBuffer);
			satByteBuffer.clear();
		} else {
			int n = satStringBuffer.length();
			for (int i = 0; i < n; i++) {
				satByteArray[i] = (byte)satStringBuffer.charAt(i);
			}
			satFile.write(satByteArray, 0, n);
			satStringBuffer.setLength(0);
		}
	}

	public void write(String s) throws IOException {
		if (USE_NEWIO) {
			if (satByteBuffer.position() + s.length() > SAT_BUFFER_SIZE) {
				flush();
			}
			for (int i = 0; i < s.length(); i++) {
				satByteBuffer.put((byte)s.charAt(i));
			}
		} else {
			if (satStringBuffer.length() + s.length() > SAT_BUFFER_SIZE) {
				flush();
			}
			satStringBuffer.append(s);
		}
		satFileSize += s.length();
	}
	
	public void writeComment(String comment) throws IOException {
		if (SugarMain.debug >= 1) {
			write("c " + comment + "\n");
		}
	}
	
	public void writeClause(int[] clause) throws IOException {
		for (int code : clause) {
			if (code == TRUE_CODE) {
				return;
			}
		}
		for (int code : clause) {
			if (code != FALSE_CODE) {
				write(Integer.toString(code) + " ");
			}
		}
		write("0\n");
		satClausesCount++;
	}

	public void writeClause(List<Integer> clause0) throws IOException {
		int[] clause = new int[clause0.size()];
		for (int i = 0; i < clause.length; i++) {
			clause[i] = clause0.get(i);
		}
		writeClause(clause);
	}

	protected int[] expand(int[] clause0, int n) {
		int[] clause = new int[clause0.length + n];
		for (int i = 0; i < clause0.length; i++) {
			clause[i + n] = clause0[i];
		}
		return clause;
	}
}
