package arithmetic_compression.coder;

public class ZigzagCoder {

	public static final ZigzagCoder INSTANCE = new ZigzagCoder();

	public void enc(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			data[i] = enc(data[i]);
		}
	}

	public void dec(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			data[i] = dec(data[i]);
		}
	}

	/**
	 * Zigzag-encodes the given value, i.e. it takes the signed byte value in
	 * the range [-128..127] and maps it to an unsigned byte in the range
	 * [0..255] and returns it as a signed byte. The zigzag mapping is the
	 * following:<br/>
	 * 
	 * 0 => 0<br/>
	 * -1 => 1<br/>
	 * +1 => 2<br/>
	 * -2 => 3<br/>
	 * +2 => 4<br/>
	 * -3 => 5<br/>
	 * +3 => 6<br/>
	 * ...<br/>
	 * -127 => 253<br/>
	 * +127 => 254<br/>
	 * -128 => 255<br/>
	 */
	public byte enc(byte v) {
		int sign = ((int) v) >> 31; // 0 or -1
		int abs = Math.abs((int) v);
		return (byte) ((abs << 1) + sign);
	}

	public byte dec(byte v) {
		int sign = ((int) v) & 1; // 0 or 1 for result positive or negative
		int abs = ((((int) v) & 0xff) >>> 1) + sign;

		sign = 1 - (sign << 1); // 1 or -1 for result positive or negative
		return (byte) (abs * sign);
	}
}
