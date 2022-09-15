package arithmetic_compression;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.Random;

import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

import arithmetic_compression.io.BitOutputStream;
import arithmetic_compression.io.twiddlingbits.BinaryFormatter;

public class BitOutputStreamTest {

	@Test
	public void simpleTest() {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		BitOutputStream bitStream = new BitOutputStream(byteStream);

		bitStream.write(5, 6); // in LE: 101000
		bitStream.write(-1, 15); // in LE: 111111111111111
		bitStream.flushAll();

		byte[] a = byteStream.toByteArray();
		String binStr = BinaryFormatter.toBinStrLE(a);

		assertEquals("incorrect binary data written to underlying stream:", "10100011 11111111 11111000", binStr);
	}

	@Test
	public void extendedTest() {
		for (int seed = 0; seed < 1000; seed++) {
			singleExtendedTest(seed);
		}
	}

	public void singleExtendedTest(int seed) throws ArrayComparisonFailure {
		Random rng = new Random(seed);

		ByteArrayOutputStream byteStreamBitwise = new ByteArrayOutputStream();
		BitOutputStream bitStreamBitwise = new BitOutputStream(byteStreamBitwise);
		ByteArrayOutputStream byteStreamLenwise = new ByteArrayOutputStream();
		BitOutputStream bitStreamLenwise = new BitOutputStream(byteStreamLenwise);

		for (int i = 0; i < 100; i++) {
			int r = rng.nextInt();
			int len = rng.nextInt(33);

			for (int shift = 0; shift < len; shift++) {
				bitStreamBitwise.write((r >> shift) & 1, 1);
			}

			bitStreamLenwise.write(r, len);
		}

		bitStreamBitwise.flushAll();
		bitStreamLenwise.flushAll();

		byte[] aBitwise = byteStreamBitwise.toByteArray();
		byte[] aLenwise = byteStreamLenwise.toByteArray();

		assertArrayEquals(
				"binary data written to underlying stream differs for lenwise and bitwise writing (expected is bitwise, actual is lenwise):",
				aBitwise, aLenwise);
	}
}
