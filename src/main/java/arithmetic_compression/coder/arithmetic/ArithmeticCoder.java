package arithmetic_compression.coder.arithmetic;

import java.util.Arrays;

import arithmetic_compression.io.BitInputStream;
import arithmetic_compression.io.BitOutputStream;
import arithmetic_compression.io.twiddlingbits.BinaryFormatter;

public class ArithmeticCoder {

	SymbolProbabilityDistribution dist;

	int wordSize;

	/**
	 * By how many bits to shift from lsb to msb or vice versa; this is always
	 * (wordSize - 1).
	 */
	int shiftMsbToLsb;

	int maskWord;

	int maskMsb;

	int mask2ndMsb;

	/**
	 * Lower bound for the bits to write.
	 */
	int l;

	/**
	 * Upper bound for the bits to write.
	 */
	int u;

	int t;

	int scale3;

	public ArithmeticCoder(SymbolProbabilityDistribution dist) {
		this.dist = new SymbolProbabilityDistribution(dist);

		int total_count = this.dist.total_count;

		ensureSaneTotalCount(total_count);

		// Introduction to Data Compression, Khalid Sayood, chapter 4, p. 104,
		// section "Example 4.4.4":
		// "In order to make sure that the endpoints of the intervals always
		// remain distinct, we need to make sure that all values in the range
		// from 0 to total_count, which is the same as cum_count[3], are
		// uniquely represented in the smallest range an interval under
		// consideration can be without triggering a rescaling. The interval is
		// smallest without triggering a rescaling when l_n is just below the
		// midpoint of the interval and u_n is at three-quarters of the
		// interval, or when u_n is right at the midpoint of the interval and
		// l_n is just below a quarter of the interval. That is, the smallest
		// the interval [l_n, u_n] can be is one-quarter of the total available
		// range of 2^m values."
		// (I find the sentence directly after this quote misleading: "Thus, m
		// should be large enough to accommodate uniquely the set of values
		// between 0 and total_count.")
		wordSize = (32 - Integer.numberOfLeadingZeros(total_count)) + 2;

		shiftMsbToLsb = wordSize - 1;

		maskWord = ~(-1 << wordSize);

		maskMsb = 1 << shiftMsbToLsb;
		mask2ndMsb = maskMsb >> 1;

		l = 0;
		u = maskWord;
		t = 0;
		scale3 = 0;

		// log(0, toString());
	}

	/**
	 * Encodes the specified symbol and writes it to the sink, returns the
	 * number of bits written. Note that it is normal for arithmetic encoding to
	 * sometimes not write any bits for a symbol and instead only modify the
	 * internal status of the encoder.
	 */
	public int encode(int symbol, BitOutputStream sink) {
//		log(0, "encode(" + symbol + ")");

		long cum_count_prev = dist.cum_count[symbol];
		long cum_count_symb = dist.cum_count[symbol + 1];
		long total_count = dist.total_count;
		int write_count = 0;

		long diam = u - l + 1;
		u = l + (int) (diam * cum_count_symb / total_count) - 1;
		l = l + (int) (diam * cum_count_prev / total_count);
//		log(1, "l=" + BinaryFormatter.toBinStrBE_minLen(l, wordSize) + " (" + l + ")");
//		log(1, "u=" + BinaryFormatter.toBinStrBE_minLen(u, wordSize) + " (" + u + ")");

		for (;;) {
			if (is_msb_equal()) {
				int b = (l >> shiftMsbToLsb) & 1;
//				log(1, "msb_eq, write " + b);
				sink.write(b);
				write_count++;
				l = ((l << 1) & maskWord);
				u = ((u << 1) & maskWord) | 1;
//				log(2, "l=" + BinaryFormatter.toBinStrBE_minLen(l, wordSize) + " (" + l + ")");
//				log(2, "u=" + BinaryFormatter.toBinStrBE_minLen(u, wordSize) + " (" + u + ")");

				write_count += write_scale3_if_gt_0(sink, b);

			} else if (is_second_msb_close()) {
//				log(1, "second_msb_close, scale3++");

				try {
					scale3 = Math.incrementExact(scale3);
				} catch (ArithmeticException e) {
					throw new RuntimeException("too many e3 scalings. can't encode data with this implementation.", e);
				}

				l = ((l << 1) & maskWord);
				u = ((u << 1) & maskWord) | 1;
				l ^= maskMsb;
				u ^= maskMsb;

//				log(2, "l=" + BinaryFormatter.toBinStrBE_minLen(l, wordSize) + " (" + l + ")");
//				log(2, "u=" + BinaryFormatter.toBinStrBE_minLen(u, wordSize) + " (" + u + ")");

			} else {
				break;
			}
		}

//		log();

		return write_count;
	}

	/**
	 * returns the number of bits written
	 */
	private int write_scale3_if_gt_0(BitOutputStream sink, int prev_bit) {
		int write_count = 0;

		if (scale3 > 0) {
			int b = 1 - prev_bit;
			do {
//				log(1, "scale3=" + scale3 + ", write " + b);
				sink.write(b);
				write_count++;
				scale3--;
			} while (scale3 > 0);
		}

		return write_count;
	}

	/**
	 * After the last symbol was written to the sink, there might be additional
	 * data required by the decoder. This method writes any such data to the
	 * sink. Returned is the number of bits written by this method.
	 */
	public int finish_encoding(BitOutputStream sink) {
//		log(0, "finish_encoding()");

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

		// the BitOutputStream takes chunks of bits in little endian. However,
		// we want to write out in big endian, so we have to write one by one
		// bit in reverse order
		int i = wordSize - 1;

		int b = (l >> i) & 1;
		log(1, "write l[" + i + "]=" + BinaryFormatter.toBinStrBE_minLen(b, 1));
		sink.write(b);
		write_count++;
		i--;

		write_count += write_scale3_if_gt_0(sink, b);

		for (; i >= 0; i--) {
			b = (l >> i) & 1;
//			log(1, "write l[" + i + "]=" + BinaryFormatter.toBinStrBE_minLen(b, 1));
			sink.write(b);
			write_count++;
		}

		// log();

		return write_count;
	}

	/**
	 */
	public void start_decoding(BitInputStream source) {
		for (int i = 0; i < wordSize; i++) {
			t = (t << 1) | source.read();
		}
	}

	/**
	 */
	public int decode(BitInputStream source) {
		// log("decode()");

		long total_count = dist.total_count;

		long num = (t - l + 1) * total_count - 1;
		int target_cum_count = (int) (num / (u - l + 1));

		// log(" target_cum_count=" + target_cum_count);

		int symbol = Arrays.binarySearch(dist.cum_count, target_cum_count);
		if (symbol < 0) {
			symbol = -(symbol + 2);
		}

		// log(" symbol=" + symbol + " (dist.cum_count.length=" +
		// dist.cum_count.length + ")");

		long cum_count_prev = dist.cum_count[symbol];
		long cum_count_symb = dist.cum_count[symbol + 1];

		long diam = u - l + 1;
		u = l + (int) (diam * cum_count_symb / total_count) - 1;
		l = l + (int) (diam * cum_count_prev / total_count);
		// log(" l=" + BinaryFormatter.toBinStrBE_minLen(l, wordSize) + " (" + l
		// + ")");
		// log(" u=" + BinaryFormatter.toBinStrBE_minLen(u, wordSize) + " (" + u
		// + ")");

		for (;;) {
			if (is_msb_equal()) {
				// log(" msb_equal");

				l = ((l << 1) & maskWord);
				u = ((u << 1) & maskWord) | 1;
				t = ((t << 1) & maskWord);

				int b = source.read();
				b = (1 - ((b + 1) & 1)); // #nobranch
				t |= b;

				// log(" l=" + BinaryFormatter.toBinStrBE_minLen(l, wordSize) +
				// " (" + l + ")");
				// log(" u=" + BinaryFormatter.toBinStrBE_minLen(u, wordSize) +
				// " (" + u + ")");
				// log(" t=" + BinaryFormatter.toBinStrBE_minLen(t, wordSize) +
				// " (" + t + ")");

			} else if (is_second_msb_close()) {
				// log(" second_msb_close");

				l = ((l << 1) & maskWord);
				u = ((u << 1) & maskWord) | 1;
				t = ((t << 1) & maskWord);

				int b = source.read();
				b = (1 - ((b + 1) & 1)); // #nobranch
				t |= b;

				l ^= maskMsb;
				u ^= maskMsb;
				t ^= maskMsb;

				// log(" l=" + BinaryFormatter.toBinStrBE_minLen(l, wordSize) +
				// " (" + l + ")");
				// log(" u=" + BinaryFormatter.toBinStrBE_minLen(u, wordSize) +
				// " (" + u + ")");
				// log(" t=" + BinaryFormatter.toBinStrBE_minLen(t, wordSize) +
				// " (" + t + ")");

			} else {
				break;
			}
		}

		// log();

		return symbol;
	}

	/**
	 * Checks if the second msb of l is high and that of u is low.
	 */
	private boolean is_second_msb_close() {
		return (l & mask2ndMsb) != 0 && (u & mask2ndMsb) == 0;
	}

	/**
	 * Returns true if, and only if, u and l are both in the first or both in
	 * the second half of the scaled interval.
	 */
	private boolean is_msb_equal() {
		return (l & maskMsb) == (u & maskMsb);
	}

	private void ensureSaneTotalCount(int total_count) {
		if (total_count < 0) {
			throw new ArithmeticException(
					"total number of occurences in " + SymbolProbabilityDistribution.class.getSimpleName()
							+ " is negative (probably exceeds Integer.MAX_VALUE): " + total_count
							+ ", thus this implementation can't do the arithmetic encoding; workaround: "
							+ "use lower numbers of occurences in the probability distribution.");
		}

		if (total_count == 0) {
			throw new ArithmeticException(
					"total number of occurences in " + SymbolProbabilityDistribution.class.getSimpleName()
							+ " is 0; this is not a valid probability distribution, thus cannot be used for encoding");
		}

		if (total_count == 1) {
			throw new ArithmeticException(
					"total number of occurences in " + SymbolProbabilityDistribution.class.getSimpleName() + " is 1"
							+ ", thus entropy is 0 and arithmetic encoding is not applicable / is trivial "
							+ "and always of length 0.");
		}

		if (Integer.numberOfLeadingZeros(total_count) < 3) {
			throw new ArithmeticException("less than 2 bits of margin space above total number of occurences in "
					+ SymbolProbabilityDistribution.class.getSimpleName()
					+ ", thus this implementation can't do the arithmetic encoding; workaround: "
					+ "use lower numbers of occurences in the probability distribution.");
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

	public String toString() {
		StringBuilder buf = new StringBuilder();

		buf.append(getClass().getSimpleName()).append("\n");
		buf.append("    wordSize   = ").append(wordSize).append("\n");
		buf.append("    wordMask   = ").append(BinaryFormatter.toBinStrBE_minLen(maskWord, wordSize));
		buf.append("    maskMsb    = ").append(BinaryFormatter.toBinStrBE_minLen(maskMsb, wordSize));
		buf.append("    mask2ndMsb = ").append(BinaryFormatter.toBinStrBE_minLen(mask2ndMsb, wordSize));
		buf.append("    l          = ").append(BinaryFormatter.toBinStrBE_minLen(l, wordSize)) //
				.append(" (").append(l).append(")\n");
		buf.append("    u          = ").append(BinaryFormatter.toBinStrBE_minLen(u, wordSize)) //
				.append(" (").append(u).append(")\n");
		buf.append("    dist = ").append(dist);

		return buf.toString();
	}
}
