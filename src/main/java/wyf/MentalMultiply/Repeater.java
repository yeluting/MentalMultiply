package wyf.MentalMultiply;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.io.ByteArrayOutputStream;

import static wyf.MentalMultiply.AudioUtil.audioFormat;

public class Repeater extends JFrame {

// 进行录音操作
void go() {
   try {
      boolean isRecording = true;
      TargetDataLine targetDataLine = AudioSystem.getTargetDataLine(audioFormat);
      targetDataLine.open();
      targetDataLine.start();
      // 存储录音结果，用于进程间传递录音结果
      ByteArrayOutputStream cout = new ByteArrayOutputStream();

      byte[] buf = new byte[1024];
      boolean hadSound = false;// 是否已经有过吵闹声音
      int quietCount = 0;// 安静的个数
      int loudCount = 0;// 吵闹的个数
      while (isRecording) {
         int cnt = targetDataLine.read(buf, 0, buf.length);
         if (cnt > 0) {
            boolean hasSound = false;// 本次是否有声响
            for (int i = 0; i < cnt; i += 2) {
               short x = (short) (buf[i] << 8 | buf[i]);
               if (x > 100) {
                  hasSound = true;
               }
            }
            if (hasSound) {
               // print("hasSound");
               loudCount++;
               quietCount = 0;
               cout.write(buf, 0, cnt);
               if (loudCount > 4) {// 至少几个才能算作有声音
                  hadSound = true;
               }
            } else {
               quietCount++;
               loudCount = 0;
               if (quietCount > 7) {// 高潮过后有七个安静状态进行一次检查
                  if (hadSound) {
                     System.out.println("submitting");
                     submit(cout);
                     cout.reset();
                     hadSound = false;
                     loudCount = quietCount = 0;
                  } else {
                     cout.reset();
                  }
               }
            }
         }
      }
   } catch (Exception e) {
      e.printStackTrace();
   }
}

Fourier fourier = new Fourier();

void submit(ByteArrayOutputStream cout) {
   AudioUtil.playSimpleFormat(
           fourier.pitchHigherFFT(
                   ArrayUtil.arrayToDouble(
                           ArrayUtil.bytesToShortArray(
                                   cout.toByteArray()))
                   , 3));
}

Repeater() {
   setTitle("你说啥我说啥");
   setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
   setExtendedState(MAXIMIZED_BOTH);
   setVisible(true);
   new Thread(() -> {
      go();
   }).start();
}

public static void main(String[] args) {
   new Repeater();
}
}
