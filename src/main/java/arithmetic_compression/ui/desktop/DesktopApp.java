package arithmetic_compression.ui.desktop;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import arithmetic_compression.coder.DeltaCoder;
import arithmetic_compression.coder.SubtractGreenTransform;
import arithmetic_compression.coder.ZigzagCoder;
import arithmetic_compression.coder.arithmetic.ArithmeticCoder;
import arithmetic_compression.coder.arithmetic.SymbolProbabilityDistribution;
import arithmetic_compression.io.BitInputStream;
import arithmetic_compression.io.BitOutputStream;

@SuppressWarnings("serial")
public class DesktopApp extends FullScreenWindow {

	BufferedImage img;
	byte[] data;

	int[] hist = new int[256];

	@Override
	public void mouseClicked(MouseEvent e) {
		System.exit(0);
	}

	public void repaintInline() {
		Graphics g = getGraphics();
		paint(g);
		g.dispose();
	}

	private void process() {
		int w = img.getWidth();
		int h = img.getHeight();

		int stepIdx = -1;

		stepIdx++;
		// saveWithAndWithoutHist(stepIdx + ". original", path, data);
		repaintInline();

		SubtractGreenTransform.INSTANCE.encode(data);
		stepIdx++;
		// saveWithAndWithoutHist(stepIdx + ". subtractgreen", path, data);
		repaintInline();

		DeltaCoder.INSTANCE.encode(data, w, h);
		stepIdx++;
		// saveWithAndWithoutHist(stepIdx + ". delta-encoded", path, data);
		repaintInline();

		ZigzagCoder.INSTANCE.enc(data);
		stepIdx++;
		// saveWithAndWithoutHist(stepIdx + ". zigzag-encoded", path, data);
		repaintInline();

		calcHistogram(data);
		byte[] arithEncoded;
		{
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length / 2);
			BitOutputStream bitStream = new BitOutputStream(byteStream);

			SymbolProbabilityDistribution dist = new SymbolProbabilityDistribution(hist);
			ArithmeticCoder arithCodec = new ArithmeticCoder(dist);
			int max_write_count = 0;
			int max_symbol = 0;
			for (int i = 0; i < data.length; i++) {
				// if (i % 5000 == 0) {
				// System.out.printf("%.2f\n", ((double) i) / (data.length -
				// 1));
				// System.out.println(arithCodec);
				// }

				int symbol = data[i] & 0xff;
				int write_count = arithCodec.encode(symbol, bitStream);
				// if (write_count > 255) {
				// System.out.println("write_count > 255: " + write_count);
				// write_count = 255;
				// }
				data[i] = (byte) write_count;
				max_write_count = Math.max(max_write_count, write_count);
				max_symbol = Math.max(max_symbol, symbol);
			}
			arithCodec.finish_encoding(bitStream);
			bitStream.flushAll();

			System.out.println("max_write_count: " + max_write_count);
			System.out.println("max_symbol: " + max_symbol);

			// saveWithAndWithoutHist(++stepIdx + ".2.
			// bits-per-symbol-absolute", path, data);
			repaintInline();
			for (int i = 0; i < data.length; i++) {
				data[i] = (byte) ((data[i] & 0xff) * 255 / max_write_count);
			}
			// saveWithAndWithoutHist(stepIdx + ".3. bits-per-symbol-upscaled",
			// path, data);
			repaintInline();

			arithEncoded = byteStream.toByteArray();
			System.out.println("size: " + arithEncoded.length + " byte");
			Arrays.fill(data, (byte) 0);
			System.arraycopy(arithEncoded, 0, data, 0, arithEncoded.length);
		}

		// saveWithAndWithoutHist(stepIdx + ".1. arithmetic-encoded", path,
		// arithEncoded);
		repaintInline();

		// long t = System.nanoTime();
		{
			ByteArrayInputStream byteStream = new ByteArrayInputStream(arithEncoded);
			BitInputStream bitStream = new BitInputStream(byteStream);

			SymbolProbabilityDistribution dist = new SymbolProbabilityDistribution(hist);
			ArithmeticCoder arithCodec = new ArithmeticCoder(dist);

			arithCodec.start_decoding(bitStream);
			for (int i = 0; i < data.length; i++) {
				int symbol = arithCodec.decode(bitStream);
				data[i] = (byte) symbol;
			}
		}
		// t = System.nanoTime() - t;
		// System.out.println("time: " + TimeDurationFormatter.formatDura(t));
		repaintInline();

		ZigzagCoder.INSTANCE.dec(data);
		repaintInline();

		DeltaCoder.INSTANCE.decode(data, w, h);
		repaintInline();

		SubtractGreenTransform.INSTANCE.decode(data);
		repaintInline();
	}

	public void saveWithAndWithoutHist(String caption, String path, byte[] dataForHist) {
		String filenameCaption = caption.replace(" ", "");
		filenameCaption = filenameCaption.replace('.', '_');
		filenameCaption = filenameCaption.replace('-', '_');

		String fileBasename = path + "/" + filenameCaption + "_1920";

		repaintInline();
		writePngFullCompression(img, fileBasename + ".png");

		byte[] data_backup = Arrays.copyOf(data, data.length);

		calcHistogram(dataForHist);
		drawHistogramToImg(caption);

		repaintInline();
		writePngFullCompression(img, fileBasename + "_hist.png");

		System.arraycopy(data_backup, 0, data, 0, data.length);
	}

	public void drawHistogramToImg(String histCaption) {
		Graphics g = img.getGraphics();
		drawHist(g, histCaption);
		g.dispose();
	}

	public static void writePngFullCompression(BufferedImage img, String dstFilename) {
		File dstFile = new File(dstFilename);
		writePngFullCompression(img, dstFile);
	}

	public static void writePngFullCompression(BufferedImage img, File dstFile) {
		Iterator<ImageWriter> imgWriterIter = ImageIO.getImageWritersByFormatName("png");
		ImageWriter imgWriter = imgWriterIter.next();

		ImageWriteParam params = imgWriter.getDefaultWriteParam();
		params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		params.setCompressionQuality(0.0f); // 0.0 means "high compression"
		params.setCompressionType("Deflate");

		try {
			FileOutputStream fileOutputStream = new FileOutputStream(dstFile);
			ImageOutputStream imgOutputStream = ImageIO.createImageOutputStream(fileOutputStream);
			imgWriter.setOutput(imgOutputStream);
			imgWriter.write(null, new IIOImage(img, null, null), params);
			imgOutputStream.flush();
			imgWriter.dispose();
			imgOutputStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void storeToFile(byte[] data, String filename) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(filename);
			fos.write(data);
			fos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws IOException {
		System.setProperty("sun.awt.noerasebackground", "true");

		// for (byte b = -5; b <= 5; b++) {
		// System.out.print(b + " ");
		// System.out.print(zigzag(b) + " ");
		// System.out.print(Integer.toBinaryString(b & 0xff) + " ");
		// System.out.print(Integer.toBinaryString(zigzag(b) & 0xff) + " ");
		// System.out.println();
		// }
		// for (int b = 0; b <= 255; b++) {
		// byte b_ = (byte) b;
		// byte z_ = zigzag_encode(b_);
		// int z = z_ & 0xff;
		// byte d_ = zigzag_decode(z_);
		// int d = d_ & 0xff;
		// System.out.println(b + "(" + b_ + ") -> " + z + "(" + z_ + ")" + " ->
		// " + d + "(" + d_ + ")");
		// }
		// System.exit(0);

		try {
			DesktopApp app = new DesktopApp();
			app.instanceMain();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	DesktopApp() {
		setBackground(Color.BLACK);
		setVisible(true);
		requestFocus();
		toFront();
	}

	private void instanceMain() throws IOException {
		URL srcUrl;
		srcUrl = getClass().getClassLoader().getResource("p1000238_cropped_1920.png");
		System.out.println("src image: " + srcUrl);

		img = loadImage(srcUrl);
		data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();

		System.out.println("image loaded: " + img.getWidth() + "x" + img.getHeight());

		repaint();

		process();

		repaint();
	}

	@Override
	public void paint(Graphics g) {
		g.drawImage(img, 0, 0, null);
	}

	public void drawHist(Graphics g, String histCaption) {
		int w = getWidth();
		// int h = getHeight();

		int histH = 256;
		g.setColor(new Color(255, 255, 255, 223));
		int frameW = 4;
		g.fillRect(w - hist.length - frameW * 2, 0, hist.length + frameW * 2, histH + frameW * 2);

		g.setColor(Color.BLACK);
		g.drawString(histCaption, w - 230, 20);

		int max = Integer.MIN_VALUE;
		int min = Integer.MAX_VALUE;
		int total = 0;
		for (int i = 0; i < hist.length; i++) {
			min = Math.min(min, hist[i]);
			max = Math.max(max, hist[i]);
			total += hist[i];
		}
		int avg = total / hist.length;

		if (max > 0) {
			int maxH = histH - 20;
			int x, y;
			int yBase = histH + frameW - 1;

			for (int i = 0; i < hist.length; i++) {
				x = w - frameW - hist.length + i;
				y = yBase - (hist[i] * maxH / max);
				g.drawLine(x, y, x, yBase);
			}

			{
				x = w - frameW - hist.length;

				y = yBase - (min * maxH / max);
				g.setColor(new Color(0, 255, 0, 200));
				g.drawLine(x, y, x + hist.length, y);

				y = yBase - (avg * maxH / max);
				g.setColor(new Color(255, 255, 0, 200));
				g.drawLine(x, y, x + hist.length, y);

				y = yBase - (max * maxH / max);
				g.setColor(new Color(255, 0, 0, 200));
				g.drawLine(x, y, x + hist.length, y);
			}
		}
	}

	public void printHistogram(boolean signed) {
		System.out.println(data.length);
		System.out.println("value\tcount");
		if (signed) {
			for (int v = 128; v < hist.length; v++) {
				System.out.println((v - 256) + "\t" + hist[v]);
			}
			for (int v = 0; v < 128; v++) {
				System.out.println(v + "\t" + hist[v]);
			}
		} else {
			for (int v = 0; v < hist.length; v++) {
				System.out.println(v + "\t" + hist[v]);
			}
		}
	}

	public void calcHistogram(byte[] a) {
		Arrays.fill(hist, 0);
		for (int i = 0; i < a.length; i++) {
			int v = a[i] & 0xff;
			hist[v]++;
		}
	}

	public static BufferedImage loadImage(URL srcUrl) {
		BufferedImage img;
		try {
			img = ImageIO.read(srcUrl);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		img = convertTo(img, BufferedImage.TYPE_3BYTE_BGR);

		return img;
	}

	private static BufferedImage convertTo(BufferedImage src, int type) {
		BufferedImage result = new BufferedImage(src.getWidth(), src.getHeight(), type);

		Graphics g = result.createGraphics();
		g.drawImage(src, 0, 0, null);
		g.dispose();

		return result;
	}
}