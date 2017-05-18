package wyf.MentalMultiply;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * wav文件波形查看器，ctrl+滚轮水平缩放，alt+滚轮竖直缩放
 */
public class Oscillo extends JFrame {
public static void main(String[] args) throws FileNotFoundException {
   String[] s = {"576", "866", "866-2", "912", "1209"};
   for (String i : s) {
      short[] data = AudioUtil.getData(Paths.get("test-data/" + i + ".wav"));
      Oscillo o = new Oscillo(data, i);
      o.exportImage(ArrayUtil.arrayToDouble(data), i);
   }
}

static void checkRight() {
   double[] data = new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 8, 7, 6, 5, 4, 3, 2, 1};
   new Oscillo(data, "haha");
}

static void exportImage(double[] data, Path path) {
   try {
      int width = data.length;
      double height = 0;
      int H = 900;
      for (double i : data) height = Math.max(height, Math.abs(i));
      BufferedImage image = new BufferedImage(width, H, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      for (int i = 0; i < data.length; i++) {
         g.drawLine(i, H >> 1, i, (int) (-(data[i] / height * H / 2) + H / 2));
      }
      OutputStream cout = Files.newOutputStream(path);
      ImageIO.write(image, "jpg", cout);
      cout.close();
   } catch (Exception e) {
      e.printStackTrace();
   }
}

static void exportImage(double[] data, String file) {
   if (file.endsWith(".jpg") == false) file += ".jpg";
   exportImage(data, Paths.get(file));
}

void init(JPanel panel, String title) {
   setLayout(new BorderLayout());
   setTitle(title);
   add(panel);
   setLocationRelativeTo(null);
   setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
   setExtendedState(MAXIMIZED_BOTH);
   setVisible(true);
   panel.requestFocus();
}

void init(double[] data, String title) {
   JPanel panel = new OscilloPanel(data, title);
   init(panel, title);
}

Oscillo(short[] data, String title) {
   double[] a = new double[data.length];
   for (int i = 0; i < data.length; i++) a[i] = data[i];
   init(a, title);
}

Oscillo(double[] data, String title) {
   init(data, title);
}

}
