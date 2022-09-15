package arithmetic_compression.coder.arithmetic;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

import arithmetic_compression.coder.arithmetic.ArithmeticCoder;
import arithmetic_compression.coder.arithmetic.SymbolProbabilityDistribution;
import arithmetic_compression.io.BitInputStream;
import arithmetic_compression.io.BitOutputStream;
import arithmetic_compression.io.twiddlingbits.BinaryFormatter;

public class ArithmeticCoderTest {

	@Test
	public void encodingTest() {
		int[] count = new int[]{40, 1, 9};
		SymbolProbabilityDistribution dist = new SymbolProbabilityDistribution(count);

		String msg = "0210";

		ArithmeticCoder ac = new ArithmeticCoder(dist);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BitOutputStream bos = new BitOutputStream(baos);

		for (int i = 0; i < msg.length(); i++) {
			int symbol = msg.charAt(i) - '0';
			ac.encode(symbol, bos);
		}

		ac.finish_encoding(bos);
		bos.flushAll();
		byte[] a = baos.toByteArray();
		String binStr = BinaryFormatter.toBinStrLE(a);

		assertEquals("11000100 10000000", binStr);
	}

	@Test
	public void decodingTest() {
		int[] count = new int[]{40, 1, 9};
		SymbolProbabilityDistribution dist = new SymbolProbabilityDistribution(count);

		String binStr = "1100010010000000";
		int dataI = Integer.parseInt(binStr, 2);
		dataI = Integer.reverse(dataI) >> 16;
		byte[] data = new byte[]{(byte) dataI, (byte) (dataI >> 8)};

		ArithmeticCoder ac = new ArithmeticCoder(dist);

		ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
		BitInputStream bitStream = new BitInputStream(byteStream);

		ac.start_decoding(bitStream);
		char[] result = new char[4];
		for (int i = 0; i < result.length; i++) {
			int symbol = ac.decode(bitStream);
			result[i] = (char) ('0' + symbol);
		}

		String actual = new String(result);
		String expected = "0210";

		assertEquals(expected, actual);
	}

}
