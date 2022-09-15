package arithmetic_compression.coder.arithmetic;

import java.util.Arrays;

public class SymbolProbabilityDistribution {

	int[] cum_count;

	int total_count;

	public SymbolProbabilityDistribution(SymbolProbabilityDistribution src) {
		cum_count = Arrays.copyOf(src.cum_count, src.cum_count.length);
		init_total_count();
	}

	public SymbolProbabilityDistribution(int[] count) {
		cum_count = PrefixSumCalculator.prefix_sum_int(count);
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

		for (int i = 0; i < cum_count.length - 1; i++) {
			int curr = cum_count[i];
			int next = cum_count[i + 1];
			int count = next - curr;

			String prob_str = Double.toString(((double) count) / total_count);

			while (prob_str.length() < 12) {
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