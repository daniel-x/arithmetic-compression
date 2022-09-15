package arithmetic_compression;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.Random;

import org.junit.Test;

import arithmetic_compression.io.BitInputStream;
import arithmetic_compression.io.twiddlingbits.BinaryFormatter;

public class BitInputStreamTest {

	@Test
	public void simpleTest() {
		byte[] data = new byte[]{7, 8, 9};

		ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
		BitInputStream bitStream = new BitInputStream(byteStream);

		int[] inputList = new int[3];
		inputList[0] = bitStream.read(7); // in LE: 1110000
		inputList[1] = bitStream.read(12); // in LE: 000010000100
		inputList[2] = bitStream.read(5); // in LE: 10000

		System.out.println(BinaryFormatter.toBinStrLE(data));
		int[] expected = new int[]{7, 528, 1};

		assertArrayEquals("incorrect data read from underlying stream", expected, inputList);
	}

	@Test
	public void extendedTest() {
		for (int seed = 0; seed < 1000; seed++) {
			singleExtendedTest(seed);
		}
	}

	public void singleExtendedTest(int seed) {
		Random rng = new Random(seed);

		byte[] data = new byte[1024];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) rng.nextInt();
		}

		ByteArrayInputStream byteStreamBitwise = new ByteArrayInputStream(data);
		BitInputStream bitStreamBitwise = new BitInputStream(byteStreamBitwise);
		ByteArrayInputStream byteStreamLenwise = new ByteArrayInputStream(data);
		BitInputStream bitStreamLenwise = new BitInputStream(byteStreamLenwise);

		int totalLen = data.length * 8;

		int readLen = 0;
		int readLenwiseCallCount = 0;
		while (readLen < totalLen) {
			int len = rng.nextInt(33);

			if (readLen + len > totalLen) {
				len = totalLen - readLen;
			}

			int r = bitStreamLenwise.read(len);

			for (int shift = 0; shift < len; shift++) {
				int bBitwise = bitStreamBitwise.read(1);

				int bLenwise = (r >> shift) & 1;

				if (bLenwise != bBitwise) {
					assertEquals("lenwise read result differs from bitwise result at bit " + shift
							+ " (0-based index) at lenwise read call " + readLenwiseCallCount
							+ " (0-based index) for reading " + len + " bits after a total of " + readLen
							+ " bits had been read by previous lenwise read calls.", bBitwise, bLenwise);
				}
			}

			readLen += len;
			readLenwiseCallCount++;
		}
	}
}
