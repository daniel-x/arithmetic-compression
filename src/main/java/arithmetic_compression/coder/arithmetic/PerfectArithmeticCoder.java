package arithmetic_compression.coder.arithmetic;

import java.math.BigInteger;

import arithmetic_compression.io.BitInputStream;
import arithmetic_compression.io.BitOutputStream;
import arithmetic_compression.io.twiddlingbits.BinaryFormatter;

public class PerfectArithmeticCoder {

	SymbolProbabilityDistributionBig dist;

	int wordSize;

	/**
	 * Lower bound for the bits to write.
	 */
	BigInteger l;

	/**
	 * Upper bound for the bits to write.
	 */
	BigInteger u;

	/**
	 * l and u are used as bitsets and luMsbIdx is the index of the most
	 * significant bit, i.e. the length-1 of each of these bitsets. This is
	 * necessary because we need to remember that sometimes there are leading 0
	 * bits in both of them and there is no other way of tracking this.
	 */
	int luMsbIdx;

	public PerfectArithmeticCoder(SymbolProbabilityDistribution dist) {
		this.dist = new SymbolProbabilityDistributionBig(dist);
		init();
	}

	public PerfectArithmeticCoder(SymbolProbabilityDistributionBig dist) {
		this.dist = new SymbolProbabilityDistributionBig(dist);
		init();
	}

	private void init() {
		ensureSaneTotalCount(dist.total_count);

		wordSize = dist.total_count.bitLength() + 2;

		l = BigInteger.ZERO;
		u = BigInteger.ONE;

		luMsbIdx = 0;

		System.out.println(this);
	}

	/**
	 * Encodes the specified symbol and writes it to the sink, returns the
	 * number of bits written. Note that it is normal for arithmetic encoding to
	 * sometimes not write any bits for a symbol and instead only modify the
	 * internal status of the encoder.
	 */
	public int encode(int symbol, BitOutputStream sink) {
		log(0, "encode(" + symbol + ")");

		BigInteger cum_count_prev = dist.cum_count[symbol];
		BigInteger cum_count_symb = dist.cum_count[symbol + 1];
		BigInteger total_count = dist.total_count;
		int write_count = 0;

		// long d = u - l + 1;
		BigInteger d = u.subtract(l).add(BigInteger.ONE);

		log(1, "d=u-l+1=" + BinaryFormatter.toBinStrBE_minLen(d, luMsbIdx + 1) + " (" + d + ")");

		while (d.bitLength() < wordSize) { // maybe while (d < total_count * 4)
											// is enough
			log(1, "d.bitLength() < wordSize (" + d.bitLength() + " < " + wordSize + ")");

			l = l.shiftLeft(1);
			u = u.shiftLeft(1).or(BigInteger.ONE);
			d = u.subtract(l).add(BigInteger.ONE);

			luMsbIdx++;

			log(2, "l=" + BinaryFormatter.toBinStrBE_minLen(l, luMsbIdx + 1) + " (" + l + ")");
			log(2, "u=" + BinaryFormatter.toBinStrBE_minLen(u, luMsbIdx + 1) + " (" + u + ")");
			log(2, "d=" + BinaryFormatter.toBinStrBE_minLen(d, luMsbIdx + 1) + " (" + d + ")");
		}

		// u = l + (int) (diam * cum_count_symb / total_count) - 1;
		// l = l + (int) (diam * cum_count_prev / total_count);
		BigInteger l_prev = l;
		l = l_prev.add(d.multiply(cum_count_prev).divide(total_count));
		u = l_prev.add(d.multiply(cum_count_symb).divide(total_count).subtract(BigInteger.ONE));

		log(1, "l = " + l_prev + " + " + d + " * " + cum_count_prev + " / " + total_count + "     = "
				+ BinaryFormatter.toBinStrBE_minLen(l, luMsbIdx + 1) + " (" + l + ")");
		log(1, "u = " + l_prev + " + " + d + " * " + cum_count_symb + " / " + total_count + " - 1 = "
				+ BinaryFormatter.toBinStrBE_minLen(u, luMsbIdx + 1) + " (" + u + ")");

		while (is_msb_equal()) {
			int b = l.testBit(luMsbIdx) ? 1 : 0;
			log(1, "msb_eq, write " + b);
			sink.write(b);
			write_count++;
			// l = ((l << 1) & maskWord);
			// u = ((u << 1) & maskWord) | 1;
			if (b == 1) {
				l = l.clearBit(luMsbIdx);
				u = u.clearBit(luMsbIdx);
			}
			luMsbIdx--;

			log(2, "l=" + BinaryFormatter.toBinStrBE_minLen(l, luMsbIdx + 1) + " (" + l + ")");
			log(2, "u=" + BinaryFormatter.toBinStrBE_minLen(u, luMsbIdx + 1) + " (" + u + ")");
		}

		log();

		return write_count;
	}

	/**
	 * After the last symbol was written to the sink, there might be additional
	 * data required by the decoder. This method writes any such data to the
	 * sink. Returned is the number of bits written by this method.
	 */
	public int finish_encoding(BitOutputStream sink) {
		log(0, "finish_encoding()");

		// Introduction to Data Compression, Khalid Sayood, chapter 4, p. 106,
		// section "Example 4.4.4":
		// "If we wished to terminate the encoding at this point, we have to
		// send the current status of the tag. This can be done by sending the
		// value of the lower limit l." (it then goes on about how to handle
		// scale3>0, for which additional writes need to be performed)
		//
		// for future improvements or understanding:
		// can we send less bits?
		// I think if we encounter a bit which is 0 in l and 1 in u, we can
		// simply send the middle between 0 and 1, i.e. 0.5 = '01' in binary)
		// and stop.
		// Since by the design of the algorithm, the MSBs in l and u after the
		// last write operation are always 0 and 1 respectively, shouldn't
		// appending '01' in general be enough? if yes, that means it's always
		// '01' anyways, so then, can we simply leave this finalization out
		// alltogether?

		int write_count = 0;

		for (int i = luMsbIdx; i >= 0; i--) {
			// b = (l >> i) & 1;
			int b = l.testBit(i) ? 1 : 0;
			log(1, "write l[" + i + "]=" + BinaryFormatter.toBinStrBE_minLen(b, 1));
			sink.write(b);
			write_count++;
		}

		l = BigInteger.ZERO;
		u = BigInteger.ONE;
		luMsbIdx = 0;

		// log();

		return write_count;
	}

	/**
	 */
	public void start_decoding(BitInputStream source) {
	}

	// /**
	// */
	// public int decode(BitInputStream source) {
	// // log("decode()");
	//
	// long total_count = dist.total_count;
	//
	// long num = (t - l + 1) * total_count - 1;
	// int target_cum_count = (int) (num / (u - l + 1));
	//
	// // log(" target_cum_count=" + target_cum_count);
	//
	// int symbol = Arrays.binarySearch(dist.cum_count, target_cum_count);
	// if (symbol < 0) {
	// symbol = -(symbol + 2);
	// }
	//
	// // log(" symbol=" + symbol + " (dist.cum_count.length=" +
	// // dist.cum_count.length + ")");
	//
	// long cum_count_prev = dist.cum_count[symbol];
	// long cum_count_symb = dist.cum_count[symbol + 1];
	//
	// long diam = u - l + 1;
	// u = l + (int) (diam * cum_count_symb / total_count) - 1;
	// l = l + (int) (diam * cum_count_prev / total_count);
	// // log(" l=" + BinaryFormatter.toBinStrBE_minLen(l, wordSize) + " (" + l
	// // + ")");
	// // log(" u=" + BinaryFormatter.toBinStrBE_minLen(u, wordSize) + " (" + u
	// // + ")");
	//
	// for (;;) {
	// if (is_msb_equal()) {
	// // log(" msb_equal");
	//
	// l = ((l << 1) & maskWord);
	// u = ((u << 1) & maskWord) | 1;
	// t = ((t << 1) & maskWord);
	//
	// int b = source.read(1);
	// b = (1 - ((b + 1) & 1)); // #nobranch
	// t |= b;
	//
	// // log(" l=" + BinaryFormatter.toBinStrBE_minLen(l, wordSize) +
	// // " (" + l + ")");
	// // log(" u=" + BinaryFormatter.toBinStrBE_minLen(u, wordSize) +
	// // " (" + u + ")");
	// // log(" t=" + BinaryFormatter.toBinStrBE_minLen(t, wordSize) +
	// // " (" + t + ")");
	//
	// } else if (is_second_msb_close()) {
	// // log(" second_msb_close");
	//
	// l = ((l << 1) & maskWord);
	// u = ((u << 1) & maskWord) | 1;
	// t = ((t << 1) & maskWord);
	//
	// int b = source.read(1);
	// b = (1 - ((b + 1) & 1)); // #nobranch
	// t |= b;
	//
	// l ^= maskMsb;
	// u ^= maskMsb;
	// t ^= maskMsb;
	//
	// // log(" l=" + BinaryFormatter.toBinStrBE_minLen(l, wordSize) +
	// // " (" + l + ")");
	// // log(" u=" + BinaryFormatter.toBinStrBE_minLen(u, wordSize) +
	// // " (" + u + ")");
	// // log(" t=" + BinaryFormatter.toBinStrBE_minLen(t, wordSize) +
	// // " (" + t + ")");
	//
	// } else {
	// break;
	// }
	// }
	//
	// // log();
	//
	// return symbol;
	// }

	/**
	 * Returns true if, and only if, u and l are both in the first or both in
	 * the second half of the scaled interval.
	 */
	private boolean is_msb_equal() {
		return l.testBit(luMsbIdx) == u.testBit(luMsbIdx);
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();

		buf.append(getClass().getSimpleName()).append("\n");
		buf.append("    wordSize = ").append(wordSize).append("\n");
		buf.append("    luMsbIdx = ").append(luMsbIdx).append("\n");
		buf.append("    l        = ").append(BinaryFormatter.toBinStrBE_minLen(l, luMsbIdx + 1)) //
				.append(" (").append(l).append(")\n");
		buf.append("    u        = ").append(BinaryFormatter.toBinStrBE_minLen(u, luMsbIdx + 1)) //
				.append(" (").append(u).append(")\n");
		buf.append("    dist = ").append(dist);

		return buf.toString();
	}

	private void ensureSaneTotalCount(BigInteger total_count) {
		if (total_count.signum() < 0) {
			throw new ArithmeticException("total number of occurences in "
					+ SymbolProbabilityDistributionBig.class.getSimpleName()
					+ " is negative (probably exceeds Integer.MAX_VALUE): " + total_count
					+ ", thus this implementation can't do the arithmetic encoding; workaround: use lower numbers of occurences in the probability distribution.");
		}

		if (total_count.signum() == 0) {
			throw new ArithmeticException("total number of occurences in "
					+ SymbolProbabilityDistributionBig.class.getSimpleName()
					+ " is 0; this is not a valid probability distribution" + ", thus cannot be used for encoding");
		}

		if (total_count.equals(BigInteger.ONE)) {
			throw new ArithmeticException("total number of occurences in "
					+ SymbolProbabilityDistributionBig.class.getSimpleName() + " is 1"
					+ ", thus entropy is 0 and arithmetic encoding is not applicable / is trivial and always of length 0.");
		}
	}

	private static final String INDENTATION_SPACE = "    ";

	private static void log() {
		System.out.println();
		// throw new RuntimeException();
	}

	private static void log(int indentLevel, Object o) {
		for (int i = 0; i < indentLevel; i++) {
			System.out.print(INDENTATION_SPACE);
		}
		System.out.println(o);
		// throw new RuntimeException();
	}
}
