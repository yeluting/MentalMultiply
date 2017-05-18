package wyf.MentalMultiply;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;

class OscilloPanel extends JPanel {
final static int IMAGE_WIDTH = 1200, IMAGE_HEIGHT = 400;//图片大小固定
BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB_PRE);
double[] data;
String title;
double pos = 0;//最左端位置
double widthScale = 1;// 宽度比例
double heightScale = 1;//高度比例
double width, height;

double mousePos;//鼠标位置
PaintPanel paintPanel;//绘图区
ShowPanel showPanel;//文字区
MouseWheelListener mouseWheelListener = new MouseWheelListener() {
   @Override
   public void mouseWheelMoved(MouseWheelEvent e) {
      if (e.isControlDown()) {
         widthScale += e.getPreciseWheelRotation() * 0.1;
         if (widthScale < 0.1) widthScale = 0.1f;
      } else if (e.isAltDown()) {
         heightScale += e.getPreciseWheelRotation() * 0.1;
         if (heightScale < 0.1) heightScale = 0.1f;
      } else {
         pos += e.getPreciseWheelRotation() * 0.01;
         if (pos < 0) pos = 0;
      }
      paintPanel.repaint();
      showPanel.change();
   }
};
MouseMotionListener mouseMotionListener = new MouseMotionListener() {
   @Override
   public void mouseDragged(MouseEvent e) {

   }

   @Override
   public void mouseMoved(MouseEvent e) {
      mousePos = (double) e.getX() / paintPanel.getWidth() / widthScale + pos;
      showPanel.change();
   }
};

public OscilloPanel(double[] data, String title) {
   this.data = data;
   this.title = title;
   width = data.length;
   for (double i : data) height = Math.max(Math.abs(i), height);
   setLayout(new BorderLayout());
   paintPanel = new PaintPanel();
   showPanel = new ShowPanel();
   add(paintPanel);
   add(showPanel, BorderLayout.SOUTH);
}

class PaintPanel extends JPanel {
   PaintPanel() {
      addMouseWheelListener(mouseWheelListener);
      addMouseMotionListener(mouseMotionListener);
      requestFocus();
   }

   @Override
   public void paint(Graphics g) {
      Graphics2D gg = image.createGraphics();
      gg.clearRect(0, 0, image.getWidth(), image.getHeight());
      for (double i = 0; i < IMAGE_WIDTH; i++) {
         double x = i / IMAGE_WIDTH * width / widthScale + width * pos;
         if (Math.round(x) >= width) break;
         double y = IMAGE_HEIGHT / 2.0 * (data[(int) Math.round(x)] / height);
         y *= heightScale;
         gg.drawLine((int) Math.round(i), Math.round(IMAGE_HEIGHT / 2), (int) Math.round(i), (int) Math.round(IMAGE_HEIGHT / 2f - y));
      }
      g.drawImage(image, 0, 0, paintPanel.getWidth(), paintPanel.getHeight(), null);
   }
}

class ShowPanel extends JPanel {
   JLabel posLabel = new JLabel();
   JLabel mousePosLabel = new JLabel();
   JLabel widthScaleLabel = new JLabel();
   JLabel heightScaleLabel = new JLabel();
   JLabel labels[] = new JLabel[]{posLabel, mousePosLabel, widthScaleLabel, heightScaleLabel};

   ShowPanel() {
      Font font = new Font("Consolas", Font.BOLD, 28);
      setLayout(new GridLayout(1, 4));
      Arrays.stream(labels).forEach(x -> {
         x.setFont(font);
         add(x);
      });
      change();
   }

   void change() {
      posLabel.setText(String.format("pos:%.4f", pos));
      widthScaleLabel.setText(String.format("widthScale:%.4f", widthScale));
      heightScaleLabel.setText(String.format("heightScale:%.4f", heightScale));
      mousePosLabel.setText(String.format("mousePos:%.4f", mousePos));
   }
}
}