package wyf.MentalMultiply;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Random;

import static wyf.MentalMultiply.AudioUtil.audioFormat;
import static wyf.MentalMultiply.AudioUtil.play;
import static wyf.MentalMultiply.AudioUtil.wrapToBytes;
import static wyf.MentalMultiply.Baidu.accessToken;
import static wyf.MentalMultiply.Baidu.getAccessTocken;

/**
 * 心算主程序
 */
public class Multiply extends JFrame {
boolean isRecording;//是否正在录音

int one, two;//第一个数字和第二个数字
final Random random = new Random();
final String numChar = "零一二三四五六七八九";

final Recognizer recognizer = new BaiduRecognizer();
final BaiduSynthesizer synthesizer = new BaiduSynthesizer();

final static Properties properties = new Properties();
final Path propertyPath = Paths.get("config.properties");


void loadProperties() {
   try {
      if (Files.exists(propertyPath)) properties.load(Files.newBufferedReader(propertyPath));
   } catch (Exception e) {
      e.printStackTrace();
   }
   accessToken = properties.getProperty("token");
   if (accessToken == null) {
      accessToken = getAccessTocken();
      properties.put("token", accessToken);
   }
   synthesizer.pitch = Integer.parseInt(properties.getProperty("pitch", "5"));
   synthesizer.volume = Integer.parseInt(properties.getProperty("volume", "5"));
   synthesizer.speed = Integer.parseInt(properties.getProperty("speed", "5"));
   synthesizer.person = Integer.parseInt(properties.getProperty("person", "0"));
}

void saveProperties() {
   try {
      properties.store(Files.newBufferedWriter(propertyPath), "Mental Multiply Configuration");
   } catch (IOException e) {
      e.printStackTrace();
   }
}

void print(String s) {
   if (area.getText().length() > 1000) {
      area.setText(s);
   } else {
      area.append(s + "\n");
   }
   int end = area.getText().length() - 1;
   area.select(end, end);
}

void synthesize(String text) {
   play(synthesizer.synthesize(text));
}

// 下一个问题
void nextProblem() throws Exception {
   one = Math.abs(random.nextInt()) % 89 + 11;
   two = Math.abs(random.nextInt()) % 89 + 11;
   synthesize(one + "乘以" + two);
}

// 将字符串转换为数字，如“一三九六”=1396
int toInteger(String s) {
   int ans = 0;
   for (int i = 0; i < s.length(); i++) {
      int ind = (s.charAt(i) >= '0' && s.charAt(i) <= '9') ? s.charAt(i) - '0' : numChar.indexOf(s.charAt(i));
      if (ind == -1) return ans;
      ans = ans * 10 + ind;
   }
   return ans;
}

/**
 * 字符串相似度
 */
boolean like(String m, String n) {
   int cnt = 0;
   for (int i = 0; i < m.length(); i++) {
      for (int j = 0; j < n.length(); j++) {
         if (n.charAt(j) == m.charAt(i)) {
            cnt++;
         }
      }
   }
   return cnt > 2;
}

// 提交答案并进行评判
void submit(byte[] data) {
   try {
      if (data == null) return;
      String recognizeResult = recognizer.recognize(data);
      if (like(recognizeResult, "再说一遍")) {
         synthesize(one + "乘以" + two);
         return;
      }
      if (like(recognizeResult, "下一题")) {
         nextProblem();
         return;
      }
      if (like(recognizeResult, "他妈的")) {
         synthesize("你他妈的，给你出题还骂人");
         return;
      }
      if (like(recognizeResult, "不算了")) {
         synthesize("不算拉倒");
         saveProperties();
         System.exit(0);
      }
      int ans = one * two;
      int mine = toInteger(recognizeResult);
      print(String.format("toInteger(%s)=%d\n", recognizeResult, mine));
      if (mine == -1) {
         synthesize("没听清");
      } else if (mine == ans) {
         synthesize("正确,真聪明");
         nextProblem();
      } else {
         synthesize("错误");
      }
   } catch (Exception e) {
      e.printStackTrace();
   }
}

// 进行录音操作，这个函数是一个死循环，需要一个进程专门来运行它
void go() {
   try {
      isRecording = true;
      nextProblem();
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
                     print("submitting");
                     submit(wrapToBytes(cout, audioFormat));
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

JPanel initConfigSoundPanel() {
   JPanel panel = new JPanel(new GridLayout(4, 1));
   JPanel row1 = new JPanel();
   String[] nineGrade = "0 1 2 3 4 5 6 7 8 9".split(" ");
   row1.add(new JLabel("音调"));
   JComboBox<String> pitchCombo = new JComboBox<>(nineGrade);
   pitchCombo.setSelectedIndex(synthesizer.pitch);
   row1.add(pitchCombo);
   pitchCombo.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
         synthesizer.pitch = pitchCombo.getSelectedIndex();
         properties.put("pitch", synthesizer.pitch + "");
      }
   });
   panel.add(row1);
   JPanel row2 = new JPanel();
   row2.add(new JLabel("音量"));

   JComboBox<String> volumeCombo = new JComboBox<>(nineGrade);
   volumeCombo.setSelectedIndex(synthesizer.volume);
   row2.add(volumeCombo);
   volumeCombo.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
         synthesizer.volume = volumeCombo.getSelectedIndex();
         properties.put("volume", synthesizer.volume + "");
      }
   });
   panel.add(row2);
   JPanel row3 = new JPanel();
   row3.add(new JLabel("语速"));
   JComboBox<String> speedComboBox = new JComboBox<>(nineGrade);
   speedComboBox.setSelectedIndex(synthesizer.speed);
   row3.add(speedComboBox);
   speedComboBox.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
         synthesizer.speed = speedComboBox.getSelectedIndex();
         properties.put("speed", synthesizer.speed + "");
      }
   });
   panel.add(row3);
   JPanel row4 = new JPanel();
   row4.add(new JLabel("性别"));
   JComboBox<String> sexComboBox = new JComboBox<>("女 男".split(" "));
   sexComboBox.setSelectedIndex(synthesizer.person);
   row4.add(sexComboBox);
   panel.add(row4);
   sexComboBox.addActionListener(e -> {
      synthesizer.person = sexComboBox.getSelectedIndex();
      properties.put("person", synthesizer.person + "");
   });
   return panel;
}

JTextArea area = new JTextArea();

JPanel initMainPanel() {
   JButton stopButton = new JButton("Stop");
   JPanel panel = new JPanel(new BorderLayout());
   panel.add(stopButton, BorderLayout.NORTH);
   stopButton.addActionListener(e -> {
      print("stop button");
      isRecording = false;
   });
   area.setFont(new Font("微软雅黑", Font.BOLD, 19));
   area.setEditable(false);
   panel.add(new JScrollPane(area), BorderLayout.CENTER);
   return panel;
}

public Multiply() throws Exception {
   loadProperties();
   JTabbedPane tabbedPane = new JTabbedPane();
   add(tabbedPane);
   tabbedPane.add("主界面", initMainPanel());
   tabbedPane.add("设置语音", initConfigSoundPanel());
   setSize(300, 300);
   setLocationRelativeTo(null);
   setVisible(true);
   addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
         saveProperties();
         System.exit(0);
      }
   });

   new Thread(() -> go()).start();
}

public static void main(String[] args) throws Exception {
   new Multiply();
}
}
