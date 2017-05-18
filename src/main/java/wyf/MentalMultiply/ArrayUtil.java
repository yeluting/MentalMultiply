package wyf.MentalMultiply;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

//数组工具类，用于快速转换数据类型，不考虑时空效率
public class ArrayUtil {
static short[] bytesToShortArray(byte[] bytes) {
   short[] ans = new short[bytes.length >> 1];
   for (int i = 0; i < ans.length; i++) {
      ans[i] = (short) ((bytes[i << 1] & 0xff) << 8 | (bytes[i << 1 | 1] & 0xff));
   }
   return ans;
}

/**
 * 将byte数组转换成short数组，注意大头序、小头序
 */
static short[] bytesToShortArray(byte[] bytes, boolean bigEndian) {
   ShortBuffer buf = ByteBuffer.wrap(bytes)
           .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
           .asShortBuffer();
   short[] data = new short[buf.limit()];
   for (int i = 0; i < data.length; i++) {
      data[i] = buf.get(i);
   }
   return data;
}

static double[] arrayToDouble(short[] a) {
   double ans[] = new double[a.length];
   for (int i = 0; i < a.length; i++) ans[i] = a[i];
   return ans;
}

static short[] arrayToShort(double[] a) {
   short[] ans = new short[a.length];
   for (int i = 0; i < a.length; i++) ans[i] = (short) a[i];
   return ans;
}

static double[] magnitude(Complex[] a) {
   double[] ans = new double[a.length];
   for (int i = 0; i < a.length; i++) ans[i] = a[i].magnitude();
   return ans;
}

static double[] phi(Complex[] a) {
   double[] ans = new double[a.length];
   for (int i = 0; i < a.length; i++) ans[i] = a[i].phi();
   return ans;
}
static double[] realPart(Complex[] a) {
   double[] ans = new double[a.length];
   for (int i = 0; i < a.length; i++) ans[i] = a[i].real;
   return ans;
}

static short[] realPartToShort(Complex[] a) {
   short[] ans = new short[a.length];
   for (int i = 0; i < a.length; i++) ans[i] = (short) a[i].real;
   return ans;
}

//大头序
static ByteArrayOutputStream shortToOutputStream(short[] data) {
   ByteArrayOutputStream cout = new ByteArrayOutputStream();
   for (short x : data) {
      cout.write(x >> 8);
      cout.write(x & 255);
   }
   return cout;
}
}
