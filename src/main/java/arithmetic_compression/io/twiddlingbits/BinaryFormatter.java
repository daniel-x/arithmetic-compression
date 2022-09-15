package arithmetic_compression.io.twiddlingbits;

import java.math.BigInteger;
import java.util.Arrays;

public class BinaryFormatter {

	private static final int CHAR_ZERO = '0';

	/**
	 * Converts the len many least significant bits of x to their binary
	 * representation as a String in little endian bit order (least significant
	 * bit (LSB) first).
	 */
	public static String toBinStrLE(int x, int len) {
		char[] str = new char[len];

		for (int i = 0; i < len; i++) {
			str[i] = (char) (CHAR_ZERO + ((x >> i) & 1));
		}

		return new String(str);
	}

	public static String toBinStrLE(int x) {
		return toBinStrLE(x, 32);
	}

	public static String toBinStrLE(byte[] a) {
		if (a.length > 0) {

			char[] result = new char[a.length * 9];
			int len = 0;
			for (int i = 0; i < a.length; i++) {
				int ai = a[i];
				for (int shift = 0; shift < 8; shift++) {
					result[len++] = (char) (CHAR_ZERO + ((ai >> shift) & 1));
				}

				result[len++] = ' ';
			}

			return new String(result, 0, result.length - 1);

		} else {
			return "";
		}
	}

	/**
	 * Converts at least the minLen many least significant bits of x to a String
	 * in big endian bit order. That's the standard/most widespread way values
	 * are shown in binary, though mathematically not the most logical for
	 * cultures where reading/writing direction is from left to right.
	 */
	public static String toBinStrBE_minLen(int x, int minLen) {
		int base_len = 32 - Integer.numberOfLeadingZeros(x);
		int len = Math.max(minLen, base_len);

		char[] result = new char[len];
		int pos = len - base_len;
		Arrays.fill(result, 0, pos, '0');
		for (int i = base_len - 1; i >= 0; i--) {
			result[pos++] = (char) (CHAR_ZERO + ((x >> i) & 1));
		}

		return new String(result);
	}

	/**
	 * Converts at least the minLen many least significant bits of x to a String
	 * in big endian bit order. That's the standard/most widespread way values
	 * are shown in binary, though mathematically not the most logical for
	 * cultures where reading/writing direction is from left to right.
	 */
	public static String toBinStrBE_minLen(BigInteger x, int minLen) {
		int base_len = x.bitLength();
		int len = Math.max(minLen, base_len);

		char[] result = new char[len];
		int pos = len - base_len;
		Arrays.fill(result, 0, pos, '0');
		for (int i = base_len - 1; i >= 0; i--) {
			result[pos++] = (char) (CHAR_ZERO + (x.testBit(i) ? 1 : 0));
		}

		return new String(result);
	}
}
