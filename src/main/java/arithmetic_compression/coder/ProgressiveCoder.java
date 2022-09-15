package arithmetic_compression.coder;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.net.URL;

import arithmetic_compression.ui.desktop.DesktopApp;

public class ProgressiveCoder {

	public static final ProgressiveCoder INSTANCE = new ProgressiveCoder();

	public static void main(String[] args) {
		URL srcUrl = ProgressiveCoder.class.getClassLoader().getResource("p1000238_cropped_1920.png");
		System.out.println("src image: " + srcUrl);

		BufferedImage img = DesktopApp.loadImage(srcUrl);
		byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

		INSTANCE.encode(data, img.getWidth(), img.getHeight(), 3);
	}

	public void encode(byte[] data, int w, int h, int channels_per_pixel) {
		byte[] avg_per_channel = avg_per_channel(data, w, h, channels_per_pixel, 0, 0, w, h);

		StringBuilder buf = new StringBuilder("(");
		for (int ch = 0; ch < avg_per_channel.length; ch++) {
			buf.append(avg_per_channel[ch]).append(",");
		}
		if (avg_per_channel.length != 0) {
			buf.setLength(buf.length() - 1);
		}
		buf.append(")");

		System.out.println("avg color: " + buf);
	}

	private byte[] avg_per_channel(byte[] data, int w, int h, int channels_per_pixel, int area_x, int area_y,
			int area_w, int area_h) {
		long[] sum_per_channel = new long[channels_per_pixel];

		for (int i = 0; i < data.length;) {
			for (int ch = 0; ch < channels_per_pixel; ch++, i++) {
				sum_per_channel[ch] += data[i] & 0xffL;
			}
		}

		byte[] result = new byte[channels_per_pixel];
		int pixel_count = w * h;
		for (int ch = 0; ch < channels_per_pixel; ch++) {
			result[ch] = (byte) ((sum_per_channel[ch] + pixel_count / 2) / pixel_count);
		}

		return result;
	}

}
