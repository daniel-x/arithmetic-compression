package arithmetic_compression.io;

import java.io.IOException;
import java.io.InputStream;

public class BitInputStream {

	protected InputStream underlying;

	/**
	 * Buffered data in buf is in the buf_sz many least significant bits. The
	 * oldest bit (first read from the underlying input stream) stored in this
	 * buffer is in the least significant bit; the youngest bit of buffered
	 * content is at the most significant end of the used data area of buf.
	 */
	protected int buf;

	/**
	 * Number of data bits in buf.
	 */
	protected int buf_sz;

	public BitInputStream(InputStream underlying) {
		this.underlying = underlying;
	}

	/**
	 * Reads a single bit from this stream and returns 0 or 1 depending on the
	 * read bit. When anyways reading only signle bits, then this method offers
	 * better performance than read(int len), because read(int) can omit sanity
	 * and range checks.
	 */
	public int read() {
		if (buf_sz > 0) {
			int b = buf & 1;
			buf >>>= 1;
			buf_sz--;

			return b;
		} else {
			int b;
			try {
				b = underlying.read();
			} catch (IOException e) {
				throw new IoUncheckedException(e);
			}

			buf = b >>> 1;
			buf_sz = 7;

			return b & 1;
		}
	}

	/**
	 * Reads len many bits from this input stream and returns them in the least
	 * significant bits of the return value, where the least significant bit is
	 * the bit first read. If EOF occurs, then -1 is returned.
	 */
	public int read(int len) {
		if (len < 0) {
			throw new IllegalArgumentException( //
					"can't read a negative amount of bits (len = " + len + ")");
		}
		if (len > 32) {
			throw new IllegalArgumentException( //
					"can't read more then 32 bits at once (len = " + len + ")");
		}

		int val;

		if (buf_sz >= len) {
			if (len != 32) {
				val = buf & (~(-1 << len));
				buf >>>= len;
				buf_sz -= len;
			} else {
				val = buf;
				buf = 0;
				buf_sz = 0;
			}
		} else {
			val = buf;
			int val_sz = buf_sz;
			buf = 0;
			buf_sz = 0;

			int required_bits = len - val_sz;

			int required_bytes = (required_bits + 7) >> 3;
			for (int i = 0; i < required_bytes; i++) {
				int read;
				try {
					read = underlying.read();
				} catch (IOException e) {
					throw new IoUncheckedException(e);
				}

				if (read == -1) {
					return -1;
					// throw new EofUncheckedException("occurred on byte index "
					// + i
					// + " (0-based indexing) out of a total of " +
					// required_bytes + " bytes to be read");
				}

				buf = (read << buf_sz) | buf;
				buf_sz += 8;
			}

			if (required_bits != 32) {
				val = ((buf & (~(-1 << required_bits))) << val_sz) | val;
				buf >>>= required_bits;
				buf_sz -= required_bits;
			} else {
				val = buf;
				buf = 0;
				buf_sz = 0;
			}
		}

		return val;
	}
}
