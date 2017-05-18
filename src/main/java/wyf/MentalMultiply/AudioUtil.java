package wyf.MentalMultiply;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static wyf.MentalMultiply.ArrayUtil.bytesToShortArray;

//声音工具类
public class AudioUtil {
// 默认声音格式
final static float sampleRate = 8000;
final static int sampleSizeInBits = 16;
final static int channels = 1;
final static int frameSize = 2;
final static boolean bigEndian = true;
final static AudioFormat audioFormat = new AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        sampleRate,
        sampleSizeInBits,
        channels,
        frameSize,
        sampleRate,
        bigEndian);//大头序

//显示wav文件格式
static void showFormat(Path path) {
   try (AudioInputStream cin = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
      AudioFormat format = cin.getFormat();
      System.out.println(
              "chanels : " + format.getChannels()
                      + "\nframeRate : " + format.getFrameRate()
                      + "\nframeSize(byte) : " + format.getFrameSize()
                      + "\nsampleRate : " + format.getSampleRate()
                      + "\nsampleSizeInBits(bit) : " + format.getSampleSizeInBits()
                      + "\nbigEndian : " + format.isBigEndian()
                      + "\nencoding : " + format.getEncoding());
      cin.close();
   } catch (Exception e) {
      e.printStackTrace();
   }
}


/**
 * 获取单声道、16位采样、wav文件数据，采样率不限
 */
static short[] getData(Path audioFile) {
   try (AudioInputStream cin = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(audioFile)))) {
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      while (true) {
         int cnt = cin.read(buffer);
         bout.write(buffer, 0, buffer.length);
         if (cnt <= 0) break;
      }
      return bytesToShortArray(bout.toByteArray(), cin.getFormat().isBigEndian());
   } catch (Exception e) {
      e.printStackTrace();
   }
   return null;
}

/**
 * 播放音频，这里是一种同步的播放，是进程阻塞的播放
 * <p>传入的是音频流包括：头部格式+数据部分。音频格式可以是MP3，
 * bitsPerSample必须是16位<p/>
 *
 * @param in ：音频流
 */
static void play(InputStream in) {
   try {
      AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(in));
      AudioFormat format = ais.getFormat();
      //如果不是wav格式，进行格式转换，解析成wav格式。在转换过程中，不影响采样率，声道数等信息
      if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
         format = new AudioFormat(//固定bitsPerSample为16
                 AudioFormat.Encoding.PCM_SIGNED,//编码
                 format.getSampleRate(),//采样率
                 16,//bitsPerSample
                 format.getChannels(),//声道数
                 format.getChannels() * 2,//bytesPerFrame
                 format.getSampleRate(),//frameRate，当然等于sampleRate
                 true//是否为大头序
         );
         //音频数据格式转换，将一个完整音频流转化为特定格式
         ais = AudioSystem.getAudioInputStream(format, ais);
      }
      SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(ais.getFormat());
      sourceDataLine.open();
      sourceDataLine.start();
      byte[] buf = new byte[1024];
      while (true) {
         int sz = ais.read(buf);
         if (sz <= 0) break;
         sourceDataLine.write(buf, 0, sz);
      }
      sourceDataLine.drain();
      sourceDataLine.close();
      ais.close();
   } catch (Exception e) {
      e.printStackTrace();
   }
}

// 音频数据在cout中，用audioformat来包装一下，获得完整的音频流
static byte[] wrapToBytes(ByteArrayOutputStream cout, AudioFormat audioFormat) throws IOException {
   AudioInputStream audioInputStream = wrapAudioData(cout.toByteArray(), audioFormat);
   ByteArrayOutputStream o = new ByteArrayOutputStream();
   AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, o);
   byte[] data = o.toByteArray();
   System.out.println("wrap data " + data.length);
   return data;
}

//音频数据部分在cout中，音频格式部分在audioFormat中，将完整的音频数据保存到path中
static void dumpAudio(ByteArrayOutputStream cout, AudioFormat audioFormat, Path path) {
   AudioInputStream audioInputStream = wrapAudioData(cout.toByteArray(), audioFormat);
   try (OutputStream o = Files.newOutputStream(path)) {
      AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, o);
   } catch (Exception e) {
      e.printStackTrace();
   }
}

/**
 * data为音频数据，audioFormat为音频格式，将二者合并为一个AudioInputStream
 */
static AudioInputStream wrapAudioData(byte[] data, AudioFormat audioFormat) {
   AudioInputStream audioInputStream = new AudioInputStream(
           new ByteArrayInputStream(data),
           audioFormat,
           data.length / audioFormat.getFrameSize()
   );
   return audioInputStream;
}

/**
 * 音频=格式+数据
 * 此处提供的格式为默认的audioFormat，数据为ByteArrayOutputStream
 * 以固定的单声道、16bitPerSample、8000采样率播放声音
 *
 * @param cout:音频的数据部分
 */
static void playSimpleFormat(ByteArrayOutputStream cout) {
   try {
      AudioFormat format = audioFormat;
      ByteArrayInputStream cin = new ByteArrayInputStream(wrapToBytes(cout, format));
      play(cin);
   } catch (IOException e) {
      e.printStackTrace();
   }
}

static void playSimpleFormat(short[] data) {
   playSimpleFormat(ArrayUtil.shortToOutputStream(data));
}

static void playSimpleFormat(double[] data) {
   playSimpleFormat(ArrayUtil.arrayToShort(data));
}

/**
 * 播放噪音
 *
 * @param timeInSeconds 噪音持续时间，单位为秒
 */
static void playNoise(int timeInSeconds) {
   try {
      ByteArrayOutputStream cout = new ByteArrayOutputStream();
      Random r = new Random();
      for (int i = 0; i < 8000 * timeInSeconds * 2; i++) {
         int x = r.nextInt();
         cout.write(x);
      }
      playSimpleFormat(cout);
   } catch (Exception e) {
      e.printStackTrace();
   }
}

/**
 * 播放频率
 */
static void playFrequency(int timeInSeconds) {
   try {
      ByteArrayOutputStream cout = new ByteArrayOutputStream();
      int A = 1000;//音量，幅度
      double df = Math.pow(2, 1 / 12.0);
      double sampleRate = 8000;
      double dt = 1 / sampleRate;
      for (double frequency = 440; frequency < 1000; frequency *= df) {
         double w = 2 * Math.PI * frequency;
         for (int i = 0; i < 8000; i++) {
            double t = dt * i;
            short x = (short) (A * Math.sin(w * t));
            cout.write(x >> 8);
            cout.write(x & 255);
         }
      }
      playSimpleFormat(cout);
   } catch (Exception e) {
      e.printStackTrace();
   }
}

/**
 * 暂停一下，输入enter继续
 */
static void pause() {
   try {
      System.in.read();
   } catch (IOException e) {
      e.printStackTrace();
   }
}

public static void main(String[] args) throws IOException {
   playFrequency(5);
}
}
