package arithmetic_compression.coder;

public class SubtractGreenTransform {

	public static final SubtractGreenTransform INSTANCE = new SubtractGreenTransform();

	public void encode(byte[] data) {
		for (int i = 0; i < data.length; i += 3) {
			int g = data[i + 1];
			data[i] = (byte) (data[i] - g);
			data[i + 2] = (byte) (data[i + 2] - g);
		}
	}

	public void decode(byte[] data) {
		for (int i = 0; i < data.length; i += 3) {
			int g = data[i + 1];
			data[i] = (byte) (data[i] + g);
			data[i + 2] = (byte) (data[i + 2] + g);
		}
	}
}
