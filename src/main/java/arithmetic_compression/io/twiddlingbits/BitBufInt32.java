package arithmetic_compression.io.twiddlingbits;

/**
 * Contains static methods for using an int (32 bit integer) as a fifo buffer
 * for bits. Standard methods are failfast, i.e. throw exception on buffer
 * overflow or underrun and they check that the buffers and values are empty in
 * non-data regions. For performance, after thoroughly testing your program, you
 * can switch to the _perf-methods.
 */
public class BitBufInt32 {

	/**
	 * Adds val_sz many of the LSB of val to the buffer buf, which currently
	 * holds buf_sz many bits in buf's least significant positions. The val_sz
	 * many bits are added written to the bit positions more significant than
	 * the previous content buf. The previous content of buf stays in buf where
	 * it was. The resulting buf is returned.
	 */
	public static int add(int buf, int buf_sz, int val, int val_sz) {
		sanity_check_size_in_range_0_to_32(buf_sz, "buf_sz");
		sanity_check_size_in_range_0_to_32(val_sz, "val_sz");
		sanity_check_used_region(buf, buf_sz, "buf", "buf_sz");
		sanity_check_used_region(val, val_sz, "val", "val_sz");

		if (buf_sz + val_sz > 32) {
			throw new IllegalArgumentException("buffer overflow: " + //
					" invalid (buf_sz + val_sz)=" + (buf_sz + val_sz) + " must be in range [0, 32]" + //
					" (buf_sz,val_sz)=(" + buf_sz + "," + val_sz + ")");
		}

		return add_perf(buf, buf_sz, val, val_sz);
	}

	/**
	 * Same as add(...), but for performance without sanity checks.
	 */
	protected static int add_perf(int buf, int buf_sz, int val, int val_sz) {
		if (buf_sz == 0) {
			return val;
		} else {
			// when buf_sz == 32, val is exspected to be 0
			return (val << buf_sz) | buf;
		}
	}

	/**
	 * Removes val_sz many of the LSB (least significant bits) from buffer buf
	 * and returns the resulting buf.
	 */
	public static int remove(int buf, int buf_sz, int val_sz) {
		sanity_check_size_in_range_0_to_32(buf_sz, "buf_sz");
		sanity_check_size_in_range_0_to_32(val_sz, "val_sz");
		sanity_check_used_region(buf, buf_sz, "buf", "buf_sz");

		sanity_check_buf_sz_at_least_val_sz(buf_sz, val_sz);

		return remove_perf(buf, buf_sz, val_sz);
	}

	/**
	 * Same as remove(...), but for performance without sanity checks.
	 */
	public static int remove_perf(int buf, int buf_sz, int val_sz) {
		return buf >>> val_sz;
	}

	/**
	 * Gets (peeks) val_sz many of the LSB (least significant bits) from buffer
	 * buf and returns the resulting value, leaving the buffer unchanged.
	 */
	public static int get(int buf, int buf_sz, int val_sz) {
		sanity_check_size_in_range_0_to_32(buf_sz, "buf_sz");
		sanity_check_size_in_range_0_to_32(val_sz, "val_sz");
		sanity_check_used_region(buf, buf_sz, "buf", "buf_sz");

		sanity_check_buf_sz_at_least_val_sz(buf_sz, val_sz);

		return get_perf(buf, buf_sz, val_sz);
	}

	/**
	 * Same as get(...), but for performance without sanity checks.
	 */
	public static int get_perf(int buf, int buf_sz, int val_sz) {
		return buf & Bitmask.n_lsb_set(val_sz);
	}

	public static void sanity_check_buf_sz_at_least_val_sz(int buf_sz, int val_sz) {
		if (buf_sz < val_sz) {
			throw new IllegalArgumentException("buffer underrun: " + //
					"buf_sz < val_sz is not allowed." + //
					" (buf_sz,val_sz)=(" + buf_sz + "," + val_sz + ")");
		}
	}

	public static void sanity_check_used_region(int val, int sz, String var_name_val, String var_name_sz) {
		if ((val & Bitmask.all_but_n_lsb_set(sz)) != 0) {
			throw new IllegalArgumentException(String.format( //
					"bits set in unused region. %s=%d %s=%s (little endian)", //
					var_name_sz, sz, var_name_val, BinaryFormatter.toBinStrLE(val)) //
			);
		}
	}

	public static void sanity_check_size_in_range_0_to_32(int sz, String var_name_sz) {
		if (sz < 0 || sz > 32) {
			throw new IllegalArgumentException("invalid " + var_name_sz + "=" + sz + ". must be in range [0, 32]");
		}
	}

}
