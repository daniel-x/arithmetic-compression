package arithmetic_compression.coder;

public class DeltaCoder {

	public static final DeltaCoder INSTANCE = new DeltaCoder();

	/**
	 * data is expected to be in (r,g,b) format, 3 bytes per pixel, row wise.
	 */
	public void encode(byte[] data, int w, int h) {
		encode_rowswise(data, w, h);
		encode_col0(data, w, h);
	}

	/**
	 * data is expected to be in (r,g,b) format, 3 bytes per pixel, row wise.
	 */
	public void decode(byte[] data, int w, int h) {
		decode_col0(data, w, h);
		decode_rowswise(data, w, h);
	}

	private static void encode_rowswise(byte[] data, int w, int h) {
		byte[] p = new byte[3];

		for (int y = 0, i = 0; y < h; y++) {
			for (int ch = 0; ch < 3; ch++, i++) {
				p[ch] = data[i];
			}

			for (int x = 1; x < w; x++) {
				for (int ch = 0; ch < 3; ch++, i++) {
					byte p_ch_prev = p[ch];
					p[ch] = data[i];
					data[i] = (byte) (data[i] - p_ch_prev);
				}
			}
		}
	}

	private static void decode_rowswise(byte[] data, int w, int h) {
		for (int y = 0, i = 0; y < h; y++) {
			i += 3;
			for (int x = 1; x < w; x++) {
				for (int ch = 0; ch < 3; ch++, i++) {
					data[i] = (byte) (data[i] + data[i - 3]);
				}
			}
		}
	}

	private static void encode_col0(byte[] data, int w, int h) {
		byte[] p = new byte[3];

		int stride = 3 * w;
		int i_step = stride - 3;

		int i = 0;
		for (int ch = 0; ch < 3; ch++, i++) {
			p[ch] = data[i];
		}
		i += i_step;
		for (int y = 1; y < h; y++, i += i_step) {
			for (int ch = 0; ch < 3; ch++, i++) {
				byte p_ch_prev = p[ch];
				p[ch] = data[i];
				data[i] = (byte) (data[i] - p_ch_prev);
			}
		}
	}

	private static void decode_col0(byte[] data, int w, int h) {
		int stride = 3 * w;
		int i_step = stride - 3;

		for (int y = 1, i_prev = 0, i = stride; y < h; y++, i_prev += i_step, i += i_step) {
			for (int ch = 0; ch < 3; ch++, i_prev++, i++) {
				data[i] = (byte) (data[i] + data[i_prev]);
			}
		}
	}

	// private static void encode_colwise(byte[] data, int w, int h) {
	// byte[] p = new byte[3];
	//
	// int stride = (w - 1) * 3;
	//
	// for (int x = 0; x < w; x++) {
	// int i = x * 3;
	// for (int ch = 0; ch < 3; ch++, i++) {
	// p[ch] = data[i];
	// }
	// i += stride;
	// for (int y = 1; y < h; y++, i += stride) {
	// for (int ch = 0; ch < 3; ch++, i++) {
	// data[i] = (byte) (data[i] - p[ch]);
	// p[ch] = (byte) (data[i] + p[ch]);
	// }
	// }
	// }
	// }
}
