package wyf.MentalMultiply;

import org.tritonus.sampled.file.WaveAudioOutputStream;
import org.tritonus.share.sampled.file.AudioOutputStream;
import org.tritonus.share.sampled.file.TNonSeekableDataOutputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static wyf.MentalMultiply.ArrayUtil.arrayToDouble;
import static wyf.MentalMultiply.ArrayUtil.bytesToShortArray;
import static wyf.MentalMultiply.ArrayUtil.magnitude;
import static wyf.MentalMultiply.ArrayUtil.realPart;
import static wyf.MentalMultiply.AudioUtil.getData;

public class Fourier {
double eps = 1e-4;

Complex[] fourier(double[] a, double sampleRate,
                  double frequencyLimit, double deltaFrequency) {
   double dt = 1 / sampleRate;//每个采样点之间的距离
   int frequencySize = (int) (frequencyLimit / deltaFrequency);
   Complex ans[] = new Complex[frequencySize];
   for (int i = 0; i < frequencySize; i++) {
      if (i % 500 == 0) {
         System.out.println("正在进行傅里叶变化......" + i);
      }
      double f = i * deltaFrequency;
      double w = Math.PI * 2 * f;//角速度
      double real = 0, imgine = 0;
      for (int j = 0; j < a.length; j++) {
         double t = j * dt;
         real += a[j] * Math.cos(w * t);
         imgine += a[j] * Math.sin(w * t);
      }
      ans[i] = new Complex(real, -imgine);//傅里叶正变换虚部为负
   }
   return ans;
}


Complex[] inverseFourier(Complex[] a, double deltaFrequency,
                         double timeDuration, double sampleRate, double pitchRatio) {
   double dt = 1 / sampleRate;
   int timeSize = (int) (timeDuration * sampleRate);
   int frequencySize = a.length;
   Complex ans[] = new Complex[timeSize];
   for (int i = 0; i < timeSize; i++) {
      if (i % 500 == 0) {
         System.out.println("正在进行傅里叶逆变换" + i);
      }
      double t = dt * i;
      Complex s = new Complex(0, 0);
      for (int j = 0; j < frequencySize; j++) {
         double f = j * deltaFrequency * pitchRatio;
         double w = 2 * Math.PI * f;
         Complex dir = new Complex(Math.cos(w * t), Math.sin(w * t));
         s.addeq(a[j].mul(dir));
      }
      ans[i] = s.div(frequencySize);//需要除以频率个数
   }
   return ans;
}

double[] inverseFourier2(Complex[] a, double deltaFrequency,
                         double timeDuration, double sampleRate, double pitchRatio) {
   double dt = 1 / sampleRate;
   int timeSize = (int) (timeDuration * sampleRate);
   int frequencySize = a.length;
   double[] ans = new double[timeSize];
   for (int i = 0; i < timeSize; i++) {
      if (i % 500 == 0) {
         System.out.println("正在进行傅里叶逆变换" + i);
      }
      double t = dt * i;
      double s = 0;
      for (int j = 0; j < frequencySize; j++) {
         double w = j * deltaFrequency * 2 * Math.PI * pitchRatio;
         s += a[j].magnitude() * Math.cos(w * t + a[j].phi());
      }
      ans[i] = s / frequencySize;
   }
   return ans;
}

int upper(int n) {//取n的二次幂上界
   int x = 1;
   while (x < n) {
      x <<= 1;
   }
   return x;
}

Complex[] pad(double[] a, int N) {//将a转换成长度为N的复数类型
   Complex[] ans = new Complex[N];
   for (int i = 0; i < a.length; i++) {
      ans[i] = new Complex(a[i], 0);
   }
   for (int i = a.length; i < N; i++) {
      ans[i] = new Complex(0, 0);
   }
   return ans;
}

Complex[] dft(double[] a) {
   int N = a.length;
   Complex[] ans = new Complex[N];
   for (int i = 0; i < N; i++) {
      double real = 0, imagine = 0;
      for (int j = 0; j < N; j++) {
         double x = Math.PI * 2 * j * i / N;
         real += a[j] * Math.cos(x);
         imagine -= a[j] * Math.sin(x);
      }
      ans[i] = new Complex(real, imagine);
   }
   return ans;
}

Complex[] fft(double[] a) {
   int N = upper(a.length);
   return commonFFT(pad(a, N), false, 1);
}

Complex[] ifft(Complex[] a, int pitchRatio) {
   int N = upper(a.length);
   if (a.length != N) {
      System.out.println("逆傅里叶变换数组长度必须为2的整数倍");
   }
   return commonFFT(a, true, pitchRatio);
}

Complex[] commonFFT(Complex[] a, boolean inverse, int pitchRatio) {
   int tag = inverse ? 1 : -1;
   int N = a.length;
   Complex[] ans = new Complex[N];
   for (int i = N >> 1; i > 0; i >>= 1) {//组的个数
      int sz = N / i;//每组的成员数，每行2个数，sz/2行
      for (int x = 0; x < sz / 2; x++) {
         double alpha = tag * Math.PI * 2 / sz * x * pitchRatio;//(x + sz / 2.0) * pitchRatio;
         Complex w = new Complex(Math.cos(alpha), Math.sin(alpha));
         for (int y = 0; y < i; y++) {
            int me = x * i + y, he = me + N / 2;//我和我的邻居
            int left = me + i * x, right = left + i;//我的上家
            Complex temp = w.mul(a[right]);
            ans[me] = a[left].add(temp);//注意left可能等于me，所以需要把a[left]保存起来
            if ((pitchRatio & 1) == 0)
               ans[he] = a[left].add(temp);
            else ans[he] = a[left].sub(temp);
         }
      }
      Complex[] temp = ans;
      ans = a;
      a = temp;
   }
   if (inverse) {
      for (int i = 0; i < N; i++) {
         a[i].diveq(N);
      }
   }
   return a;
}

Complex[] idft(Complex[] a, double pitchRatio) {
   int N = a.length;
   Complex[] ans = new Complex[N];
   for (int i = 0; i < N; i++) {
      if (i % 500 == 0) {
         System.out.println("正在进行傅里叶逆变换......" + i);
      }
      Complex s = new Complex(0, 0);
      for (int n = 0; n < N; n++) {
         double w = Math.PI * 2 / N * i * n * pitchRatio;
         s.addeq(a[n].mul(new Complex(Math.cos(w), Math.sin(w))));
      }
      ans[i] = s;
   }
   return ans;
}

double[] idft2(Complex[] a, double pitchRatio) {
   int N = a.length;
   double[] ans = new double[N];
   for (int i = 0; i < N; i++) {
      if (i % 500 == 0) {
         System.out.println("正在进行傅里叶逆变换......" + i);
      }
      double s = 0;
      for (int n = 0; n < N; n++) {
         double x = Math.PI * 2.0 * i * n / N * pitchRatio;
         s += a[n].magnitude() * Math.cos(x + a[n].phi());
      }
      ans[i] = s / N;
   }
   return ans;
}

void showNumbers() {
   short[] data = AudioUtil.getData(Paths.get("data/0.wav"));
   new Oscillo(data, "原始时域图象");
   //        Complex[] f = fft(arrayToDouble(data));
   Complex[] f = fourier(arrayToDouble(data), 8000, 10000, 1);
   new Oscillo(magnitude(f), "频域图象");
}

void testRight() {
   double[] a = new double[]{0, 1, 2, 3, 4, 5, 6, 7};
   Complex[] f = fft(a);
   for (Complex i : f) {
      System.out.println(i);
   }
   Complex[] t = ifft(f, 1);

   new Oscillo(a, "时域图");
   new Oscillo(magnitude(f), "幅度谱");
   new Oscillo(ArrayUtil.magnitude(Arrays.copyOf(t, a.length)), "经过傅里叶变换、逆变换之后的时域图");
}

void pitchHigher() {
   Path p = Paths.get("data/0.wav");
   AudioUtil.showFormat(p);
   short[] data = getData(p);
   AudioUtil.playSimpleFormat(data);
   double sampleRate = 8000;
   new Oscillo(data, "原版时域图象");
   Complex[] f = fourier(arrayToDouble(data), sampleRate, sampleRate, 1);
   //double[] ti = inverseFourier2(fou, 1, data.length / sampleRate, sampleRate);
   Complex[] t = inverseFourier(f, 1, data.length / sampleRate, sampleRate, 2);
   double[] ti = realPart(t);
   new Oscillo(ti, "变换、逆变换之后的时域图像");
   AudioUtil.playSimpleFormat(ti);
   AudioUtil.dumpAudio(ArrayUtil.shortToOutputStream(ArrayUtil.arrayToShort(ti)), AudioUtil.audioFormat, Paths.get("pitchhigher-童年的回忆.wav"));
}

double[] pitchHigherFFT(double[] data, int pitchRatio) {
   Complex[] fou = fft(data);
   Complex[] t = ifft(fou, pitchRatio);
   //Complex[] t = idft(fou, 1.5);
   t = Arrays.copyOf(t, data.length / pitchRatio);
   double[] ans = new double[data.length];
   for (int i = 0; i < t.length; i++) {
      int ind = i * pitchRatio;
      if (ind >= ans.length) break;
      ans[ind] = t[i].real;
   }
   return ans;
}

void pitchHigherFFTFile(Path src, Path des) {
   AudioUtil.showFormat(src);
   try (AudioInputStream cin = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(src)));
   ) {
      AudioOutputStream cout = new WaveAudioOutputStream(cin.getFormat(), cin.getFrameLength() * cin.getFormat().getFrameSize(), new TNonSeekableDataOutputStream(Files.newOutputStream(des)));
      byte[] buffer = new byte[(int) (cin.getFormat().getFrameSize() * cin.getFormat().getFrameRate())];
      while (true) {
         int cnt = cin.read(buffer);
         if (cnt <= 0) break;
         double[] ans = pitchHigherFFT(arrayToDouble(bytesToShortArray(buffer, cin.getFormat().isBigEndian())), 2);
         cout.write(buffer, 0, cnt);
      }
      cout.close();
   } catch (Exception e) {
      e.printStackTrace();
   }
}

void pitchHigherFFTDemo() {
   Path p = Paths.get("music.wav");
   AudioUtil.showFormat(p);
   short[] data = getData(p);
   //AudioUtil.playSimpleFormat(data);
   new Oscillo(data, "原版时域图象");
   double[] ans = pitchHigherFFT(arrayToDouble(data), 4);
   new Oscillo(ans, "变换、逆变换之后的时域图像");
   AudioUtil.playSimpleFormat(ans);
   AudioUtil.dumpAudio(ArrayUtil.shortToOutputStream(ArrayUtil.arrayToShort(ans)), AudioUtil.audioFormat, Paths.get("pitchhigher-童年的回忆.wav"));
}

Fourier(boolean debug) {
   testRight();
   //        pitchHigherFFTDemo();
   //        pitchHigher();
   //        pitchHigherFFTFile(Paths.get("taylor.wav"), Paths.get("taylor.wav"));
}

Fourier() {

}

public static void main(String[] args) throws Exception {
   new Fourier(true);
}
}

