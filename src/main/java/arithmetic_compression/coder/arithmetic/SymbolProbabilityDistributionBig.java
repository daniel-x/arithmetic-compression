package arithmetic_compression.coder.arithmetic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Arrays;

public class SymbolProbabilityDistributionBig {

	BigInteger[] cum_count;

	BigInteger total_count;

	public SymbolProbabilityDistributionBig(SymbolProbabilityDistribution src) {
		cum_count = toBigInteger(src.cum_count);
		init_total_count();
	}

	public static BigInteger[] toBigInteger(int[] a) {
		BigInteger[] result = new BigInteger[a.length];

		for (int i = 0; i < result.length; i++) {
			result[i] = BigInteger.valueOf(a[i]);
		}

		return result;
	}

	public SymbolProbabilityDistributionBig(SymbolProbabilityDistributionBig src) {
		cum_count = Arrays.copyOf(src.cum_count, src.cum_count.length);
		init_total_count();
	}

	public SymbolProbabilityDistributionBig(BigInteger[] count) {
		cum_count = PrefixSumCalculator.prefix_sum_big(count);
		init_total_count();
	}

	public SymbolProbabilityDistributionBig(int[] count) {
		cum_count = PrefixSumCalculator.prefix_sum_big(count);
		init_total_count();
	}

	private void init_total_count() {
		total_count = cum_count[cum_count.length - 1];
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		buf.append(getClass().getSimpleName()).append(" (total=" + total_count + ")\n");
		buf.append("sym_idx\tcount\tcum_count\tprobability\n");

		BigDecimal total_count_d = new BigDecimal(total_count);

		for (int i = 0; i < cum_count.length - 1; i++) {
			BigInteger curr = cum_count[i];
			BigInteger next = cum_count[i + 1];

			BigInteger count = next.subtract(curr);

			BigDecimal count_d = new BigDecimal(count);

			BigDecimal prob = count_d.divide(total_count_d, MathContext.DECIMAL128);
			String prob_str = prob.toEngineeringString();

			while (prob_str.length() < 128) {
				prob_str = prob_str + " ";
			}

			buf.append(i).append("\t");
			buf.append(count).append("\t");
			buf.append(cum_count[i]).append("\t");
			buf.append(prob_str).append("\n");
		}
		buf.append("\t\t").append(total_count).append("\n");

		return buf.toString();
	}
}