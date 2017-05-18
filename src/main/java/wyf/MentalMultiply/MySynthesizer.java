package wyf.MentalMultiply;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 我的语音合成旗
 */
public class MySynthesizer implements Synthesizer {
MySynthesizer() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
   String s = "0123456789";
   AudioInputStream[] cin = new AudioInputStream[s.length()];
   for (int i = 0; i < s.length(); i++) {
      cin[i] = AudioSystem.getAudioInputStream(new File("data/" + s.charAt(i) + ".wav"));
   }
   ByteArrayOutputStream bout[] = new ByteArrayOutputStream[cin.length];
   byte[] buffer = new byte[1024];
   for (int i = 0; i < bout.length; i++) {
      bout[i] = new ByteArrayOutputStream();
      while (true) {
         int cnt = cin[i].read(buffer);
         bout[i].write(buffer, 0, buffer.length);
         if (cnt <= 0) break;
      }
   }
   byte[][] bytes = new byte[bout.length][];
   for (int i = 0; i < bytes.length; i++) {
      bytes[i] = bout[i].toByteArray();
   }
   SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(cin[0].getFormat());
   sourceDataLine.open();
   sourceDataLine.start();
   for (int i = 0; i < 10; i++) {
      int x = s.charAt(i) - '0';
      sourceDataLine.write(bytes[x], 0, bytes[x].length);
   }
   while (sourceDataLine.isActive()) {

   }
   for (AudioInputStream in : cin) {
      in.close();
   }
   sourceDataLine.close();
}

public static void main(String[] args) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
   new MySynthesizer();
}

@Override
public InputStream synthesize(String text) {
   return null;
}
}
