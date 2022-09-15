package arithmetic_compression.coder.arithmetic;

import java.math.BigInteger;

public class PrefixSumCalculator {

	public static int[] prefix_sum_int(int[] a) {
		int[] result = new int[a.length + 1];

		int sum = 0;

		for (int i = 0; i < a.length;) {
			if (a[i] > Integer.MAX_VALUE - sum) {
				throwArithmeticOnOverflow(sum, a[i], i, Integer.MAX_VALUE);
			}

			sum += a[i];
			i++;
			result[i] = sum;
		}

		return result;
	}

	public static long[] prefix_sum_long(long[] a) {
		long[] result = new long[a.length + 1];

		long sum = 0;

		for (int i = 0; i < a.length;) {
			if (a[i] > Long.MAX_VALUE - sum) {
				throwArithmeticOnOverflow(sum, a[i], i, Integer.MAX_VALUE);
			}

			sum += a[i];
			i++;
			result[i] = sum;
		}

		return result;
	}

	public static BigInteger[] prefix_sum_big(BigInteger[] a) {
		BigInteger[] result = new BigInteger[a.length + 1];

		BigInteger sum = BigInteger.ZERO;

		for (int i = 0; i < a.length;) {
			sum = sum.add(a[i]);
			i++;
			result[i] = sum;
		}

		return result;
	}

	public static BigInteger[] prefix_sum_big(int[] a) {
		BigInteger[] result = new BigInteger[a.length + 1];

		BigInteger sum = BigInteger.ZERO;

		for (int i = 0; i < a.length;) {
			BigInteger ai = BigInteger.valueOf(a[i]);
			sum = sum.add(ai);
			i++;
			result[i] = sum;
		}

		return result;
	}

	public static void throwArithmeticOnOverflow(long sum, long ai, long i, long limit) {
		BigInteger sum_big = BigInteger.valueOf(sum);
		BigInteger ai_big = BigInteger.valueOf(ai);
		sum_big = sum_big.add(ai_big);
		throw new ArithmeticException("integer overflow while calculating prefix sum at index " + i + ": sum_prev="
				+ sum + "; a[i]=" + ai + "; sum_prev+a[i]=" + sum_big + ">" + limit);
	}
}
