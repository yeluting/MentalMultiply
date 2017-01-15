package wyf.MentalMultiply;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JFrame;

/**
 * wav文件波形查看器，ctrl+滚轮水平缩放，alt+滚轮竖直缩放
 */
public class Oscillo extends JFrame {
	Short[] data;
	int pos;
	final int width = 1200, height = 400;
	BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
	int samplePerPixel = 1;// 一个像素跨越多少个音频点
	double k = 0.6;

	public static void main(String[] args) throws FileNotFoundException {
		Scanner cin = new Scanner(new File("debug.txt"));
		ArrayList<Short> a = new ArrayList<>();
		while (cin.hasNext()) {
			short x = cin.nextShort();
			a.add(x);
		}
		cin.close();
		Short[] data = new Short[a.size()];
		data = a.toArray(data);
		new Oscillo(data);
	}

	public Oscillo(Short[] data2) {
		data = data2;
		setSize(width, height);
		setVisible(true);
		addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.isControlDown()) {
					samplePerPixel += e.getWheelRotation() * 4;
					if (samplePerPixel < 1)
						samplePerPixel = 1;
					if (samplePerPixel > data.length / 2)
						samplePerPixel = data.length / 2;
				} else if (e.isAltDown()) {
					k += e.getPreciseWheelRotation() * 0.1;
				} else {
					pos += e.getWheelRotation() * samplePerPixel * 20;
					if (pos < 0)
						pos = 0;
					if (pos > data.length - samplePerPixel * width) {
						pos = data.length - samplePerPixel * width;
						if (pos < 0)
							pos = 0;
					}
				}
				repaint();
			}
		});
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D gg = image.createGraphics();
		gg.clearRect(0, 0, image.getWidth(), image.getHeight());
		Point last = new Point(0, height / 2);
		for (int i = 0; i < width && pos + samplePerPixel * i < data.length; i++) {
			int h = (int) (k * data[pos + i * samplePerPixel]);
			Point now = new Point(i, height / 2 + h);
			gg.drawLine(last.x, last.y, now.x, now.y);
			last = now;
		}
		g.drawImage(image, 0, 0, null);
	}
}
