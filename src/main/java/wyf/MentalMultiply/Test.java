package wyf.MentalMultiply;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Test {
Test() throws IOException {
   Path des = Paths.get("我录单个数字频域图象");
   Fourier fourier = new Fourier();
   Files.list(Paths.get("我录单个数字")).forEach(p -> {
      short[] data = AudioUtil.getData(p);
      double[] pin = ArrayUtil.realPart(fourier.fft(ArrayUtil.arrayToDouble(data)));
      new Oscillo(pin, p.getFileName().toString());
      //      Oscillo.exportImage(pin, des.resolve(p.getFileName() + ".jpg"));
   });

}

public static void main(String[] args) throws IOException {
   new Test();
}
}
