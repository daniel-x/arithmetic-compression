package arithmetic_compression.io.twiddlingbits;

public class Bitmask {

	/**
	 * Creates a bitmask. Starting to count from MSB (most significant
	 * bit).<br/>
	 * all_but_n_msb_set(5) = 00000111111111111111111111111111₂.
	 */
	public static int all_but_n_msb_set(int n) {
		return (n >= 32) ? 0 : (-1 >>> n);
	}

	/**
	 * Creates a bitmask. Starting to count from MSB (most significant
	 * bit).<br/>
	 * n_msb_set(5) = 11111000000000000000000000000000₂.
	 */
	public static int n_msb_set(int n) {
		return (n >= 32) ? -1 : (~(-1 >>> n));
	}

	/**
	 * Creates a bitmask. Starting to count from LSB (least significant
	 * bit).<br/>
	 * all_but_n_lsb_set(5) = 11111111111111111111111111100000₂.
	 */
	public static int all_but_n_lsb_set(int n) {
		return (n >= 32) ? 0 : (-1 << n);
	}

	/**
	 * Creates a bitmask. Starting to count from LSB (least significant
	 * bit).<br/>
	 * n_lsb_set(5) = 00000000000000000000000000011111₂.
	 */
	public static int n_lsb_set(int n) {
		return (n >= 32) ? -1 : (~(-1 << n));
	}

}
