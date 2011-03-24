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
import jp.ac.kobe_u.cs.sugar.csp.BooleanVariable;
import jp.ac.kobe_u.cs.sugar.csp.CSP;
import jp.ac.kobe_u.cs.sugar.csp.Clause;
import jp.ac.kobe_u.cs.sugar.csp.IntegerVariable;

/**
 * Encoder encodes CSP into SAT.
 * @see CSP 
 * @author Naoyuki Tamura (tamura@kobe-u.ac.jp)
 */
public class Encoder {
	public static String satSolverName = "minisat2"; 

	public static final int FALSE_CODE = 0;

	public static final int TRUE_CODE = Integer.MIN_VALUE;

	public static boolean USE_NEWIO = true;
	
	public static int SAT_BUFFER_SIZE = 4*1024;
	
	public static long MAX_SAT_SIZE = 3*1024*1024*1024L;
	
	public static boolean OPT_COMPACT = false;
	
	public static int BASE = 10;
	
	private CSP csp;
	
	// private String satFileName;

	private BufferedOutputStream satFile;

	private StringBuilder satStringBuffer;

	private byte[] satByteArray;
	
	private FileChannel satFileChannel; 

	private ByteBuffer satByteBuffer;
	
	// private String outFileName;

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

	public Encoder(CSP csp) {
		this.csp = csp;
		// this.satFileName = satFileName;
		// this.outFileName = outFileName;
	}

	public int getSatClausesCount() {
		return satClausesCount;
	}

	public long getSatFileSize() {
		return satFileSize;
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
			int size = v.getSatVariablesSize();
			satVariablesCount += size;
		}
		for (BooleanVariable v : csp.getBooleanVariables()) {
			v.setCode(satVariablesCount + 1);
			int size = v.getSatVariablesSize();
			satVariablesCount += size;
		}
		int count = 0;
		int n = csp.getIntegerVariables().size();
		int percent = 10;
		for (IntegerVariable v : csp.getIntegerVariables()) {
			v.encode(this);
			count++;
			if ((100*count)/n >= percent) {
				Logger.fine(count + " (" + percent + "%) "
						+ "CSP integer variables are encoded"
						+ " (" + satClausesCount + " clauses, " + satFileSize + " bytes)");
				percent += 10;
			}
			if (satFileSize >= MAX_SAT_SIZE) {
				throw new SugarException("Too large " + satFileName);
			}
		}
		count = 0;
		n = csp.getClauses().size();
		percent = 10;
		for (Clause c : csp.getClauses()) {
			int satClausesCount0 = satClausesCount;
			if (! c.isValid()) {
				c.encode(this);
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
			if (satFileSize >= MAX_SAT_SIZE) {
				throw new SugarException("Too large " + satFileName);
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
//		satFileSize = (new File(satFileName)).length();
	}

	public void modifySat(String satFileName, int[][] clauses) throws IOException {
		RandomAccessFile satFile1 = new RandomAccessFile(satFileName, "rw");
		satFile1.seek(satFileSize);
		for (int[] clause : clauses) {
			for (int code : clause) {
				satFile1.write(Integer.toString(code).getBytes());
				satFile1.write(' ');
			}
			satFile1.write('0');
			satFile1.write('\n');
		}
		satFile1.seek(0);
		int n = clauses.length;
		satFile1.write(getHeader(satVariablesCount, satClausesCount+n).getBytes());
		satFile1.close();
	}
	
	public void modifySat(String satFileName, int[] clause) throws IOException {
		int[][] clauses = { clause };
		modifySat(satFileName, clauses);
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

	/*
	public void solveSAT() throws IOException, InterruptedException {
		File outFile = new File(outFileName);
		if (outFile.exists()) {
			outFile.delete();
		}
		String[] command = { satSolverName, satFileName, outFileName };
		SugarMain.log(satSolverName + " " + satFileName + " " + outFileName);
		Process process = Runtime.getRuntime().exec(command);
		BufferedReader stdout = new BufferedReader(
				new InputStreamReader(process.getInputStream()));
		BufferedReader stderr = new BufferedReader(
				new InputStreamReader(process.getErrorStream()));
		while (true) {
			String line = stderr.readLine();
			if (line == null)
				break;
			SugarMain.log(line);
		}
		stderr.close();
		while (true) {
			String line = stdout.readLine();
			if (line == null)
				break;
			SugarMain.log(line);
		}
		stdout.close();
		process.waitFor();
	}
	*/

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

	public String summary() {
		return
		satVariablesCount + " SAT variables, " +
		satClausesCount + " SAT clauses, " +
		satFileSize + " bytes";
	}
	
}
