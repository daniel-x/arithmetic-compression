package arithmetic_compression.io;

import java.io.IOException;
import java.io.OutputStream;

public class BitOutputStream {

	protected OutputStream underlying;

	/**
	 * Content in buf is in the buf_sz many least significant bits. After a
	 * write operation, buf contains less than a byte, i.e. at most 7 bits, i.e.
	 * buf_sz <= 7.
	 */
	int buf;

	/**
	 * Number of data bits in buf.
	 */
	int buf_sz;

	public BitOutputStream(OutputStream underlying) {
		this.underlying = underlying;
	}

	/**
	 * Writes the least significant bit of val to this stream. The other bits of
	 * val are ignored. When writing only single bits at a time, this method
	 * offers slightly better performance than write(int val, int len) because
	 * write(int) can omit sanity and range checks.
	 */
	public void write(int val) {
		buf |= (val & 1) << buf_sz;

		if (buf_sz == 7) {
			try {
				underlying.write(buf);
			} catch (IOException e) {
				throw new IoUncheckedException(e);
			}

			buf = 0;
			buf_sz = 0;
		} else {
			buf_sz++;
		}
	}

	/**
	 * Writes len many of the least significant bits of val to this output
	 * stream in little endian bit order and byte order. Ignores all other more
	 * significant bits above the len many least significant ones.
	 */
	public void write(int val, int len) {
		if (len < 0) {
			throw new IllegalArgumentException( //
					"can't write a negative amount of bits (len = " + len + ")");
		}
		if (len > 32) {
			throw new IllegalArgumentException( //
					"can't write more than 32 bits at once (len = " + len + ")");
		}

		if (len < 32) {
			val &= ~(-1 << len);
		}

		try {
			int buf_sz_afterwards = buf_sz + len;
			buf = buf | (val << buf_sz);

			if (buf_sz_afterwards <= 32) {
				buf_sz = buf_sz_afterwards;
			} else {
				underlying.write(buf);
				underlying.write(buf >> 8);
				underlying.write(buf >> 16);
				underlying.write(buf >> 24);

				int buf_vacant = 32 - buf_sz;
				buf = val >>> buf_vacant;
				buf_sz = buf_sz_afterwards - 32;
			}

			writeFullBytesToUnderlying();
		} catch (IOException e) {
			throw new IoUncheckedException(e);
		}
	}

	public void flushReadyBytes() {
		try {
			underlying.flush();
		} catch (IOException e) {
			throw new IoUncheckedException(e);
		}
	}

	/**
	 * Flushes all data to the underlying output stream. Fractions of bytes will
	 * be 0-padded and flushed as well.
	 */
	public void flushAll() {
		try {
			if (buf_sz != 0) {
				int pad_len = 8 - buf_sz;
				write(0, pad_len);
			}

			underlying.flush();
		} catch (IOException e) {
			throw new IoUncheckedException(e);
		}
	}

	protected void writeFullBytesToUnderlying() {
		try {
			while (buf_sz >= 8) {
				underlying.write(buf);
				buf >>>= 8;
				buf_sz -= 8;
			}
		} catch (IOException e) {
			throw new IoUncheckedException(e);
		}
	}
}
