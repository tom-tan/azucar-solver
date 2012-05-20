package jp.ac.kobe_u.cs.sugar.encoder;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import jp.ac.kobe_u.cs.sugar.SugarMain;

public class CNFWriter {
	private boolean incremental;
	public static boolean USE_NEWIO = true;
	public static int SAT_BUFFER_SIZE = 4*1024;

	private BufferedOutputStream satFile;

	private StringBuilder satStringBuffer;

	private byte[] satByteArray;
	private FileChannel satFileChannel;

	protected ByteBuffer satByteBuffer;

	private BigInteger satVariablesCount = BigInteger.ZERO;

	private BigInteger satClausesCount = BigInteger.ZERO;

	private BigInteger satFileSize = BigInteger.ZERO;

	private final String satFileName;

	public CNFWriter(String satFileName, boolean incremental) throws IOException {
		this.satFileName = satFileName;
		satFileSize = BigInteger.ZERO;
		satVariablesCount = BigInteger.ZERO;
		satClausesCount = BigInteger.ZERO;
		this.incremental = incremental;
		if (USE_NEWIO) {
			satFileChannel = (new FileOutputStream(satFileName)).getChannel();
			satByteBuffer = ByteBuffer.allocateDirect(SAT_BUFFER_SIZE);
		} else {
			satFile = new BufferedOutputStream(new FileOutputStream(satFileName));
			satStringBuffer = new StringBuilder(SAT_BUFFER_SIZE);
			satByteArray = new byte[SAT_BUFFER_SIZE];
		}
		write(getHeader(BigInteger.ZERO, BigInteger.ZERO));
	}

	public String getHeader(BigInteger numOfVariables, BigInteger numOfClauses) throws UnsupportedEncodingException {
		int n = 64;
		StringBuilder s = new StringBuilder();
		s.append("p cnf ");
		s.append(numOfVariables.toString());
		s.append(" ");
		s.append(numOfClauses.toString());
		while (s.length() < n - 1) {
			s.append(" ");
		}
		s.append("\n");
		return s.toString();
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
			assert satByteBuffer != null: "Assertion failure";
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
		satFileSize = satFileSize.add(new BigInteger(Integer.toString(s.length())));
	}

	public void writeComment(String comment) throws IOException {
		if (SugarMain.debug >= 1) {
			write("c " + comment + "\n");
		}
	}

	public void writeClause(BigInteger[] clause) throws IOException {
		for (BigInteger code : clause) {
			if (code == Encoder.TRUE_CODE) {
				return;
			}
		}
		for (BigInteger code : clause) {
			if (code != Encoder.FALSE_CODE) {
				write(code + " ");
			}
		}
		write("0\n");
		satClausesCount = satClausesCount.add(BigInteger.ONE);
	}

	public void writeClause(List<BigInteger> clause0) throws IOException {
		BigInteger[] clause = new BigInteger[clause0.size()];
		for (int i = 0; i < clause.length; i++) {
			clause[i] = clause0.get(i);
		}
		writeClause(clause);
	}

	public void close() throws IOException {
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
		RandomAccessFile satFile1 = new RandomAccessFile(satFileName, "rw");
		satFile1.seek(0);
		if (true) { //csp.getObjectiveVariable() == null || incremental
			satFile1.write(getHeader(satVariablesCount, satClausesCount).getBytes());
		} else {
			satFile1.write(getHeader(satVariablesCount, satClausesCount.add(BigInteger.ONE)).getBytes());
		}
		satFile1.close();
	}

	public void addSatVariables(BigInteger nvars) {
		satVariablesCount = satVariablesCount.add(nvars);
	}

	public BigInteger getSatVariablesCount() {
		return satVariablesCount;
	}

	public BigInteger getSatClausesCount() {
		return satClausesCount;
	}

	public BigInteger getSatFileSize() {
		return satFileSize;
	}
}
