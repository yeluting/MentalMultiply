package wyf.MentalMultiply;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class Multiply extends JFrame {
	final HttpClient client = HttpClients.createDefault();

	// 定义token相关
	final String apiId = "8614141";
	final String apiKey = "qXUeB1kXyOPgD9mkGCzcnIID";
	final String secretKey = "3447f3b010f22ad1eabe54614ef4519d";
	String accessToken = null;

	int pitch = 3, volume = 3, speed = 3, person = 0;

	boolean isRecording;

	int one, two;
	final Random random = new Random();
	final String numChar = "零一二三四五六七八九";
	final Properties properties = new Properties();
	final Path propertyPath = Paths.get("config.properties");
	// 声音格式
	final float sampleRate = 8000;
	final int sampleSizeInBits = 16;
	final int channels = 1;
	final int frameSize = 2;
	final AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, sampleSizeInBits, 1, 2,
			sampleRate, true);

	void print(String s) {
		if (area.getText().length() > 1000) {
			area.setText(s);
		} else {
			area.append(s + "\n");
		}
		int end = area.getText().length() - 1;
		area.select(end, end);
	}

	// 获取令牌串
	String getAccessTocken() {
		try {
			String url = "https://openapi.baidu.com/oauth/2.0/token";
			HttpUriRequest req = RequestBuilder.post(url).addParameter("grant_type", "client_credentials")
					.addParameter("client_id", apiKey).addParameter("client_secret", secretKey).build();
			HttpResponse resp = client.execute(req);
			String s = EntityUtils.toString(resp.getEntity());
			String accessToken = JSON.parseObject(s).getString("access_token");
			return accessToken;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	void loadProperties() {
		try {
			if (Files.exists(propertyPath))
				properties.load(Files.newBufferedReader(propertyPath));
		} catch (Exception e) {
			e.printStackTrace();
		}
		accessToken = properties.getProperty("token");
		if (accessToken == null) {
			accessToken = getAccessTocken();
			properties.put("token", accessToken);
		}
		pitch = Integer.parseInt(properties.getProperty("pitch", "5"));
		volume = Integer.parseInt(properties.getProperty("volume", "5"));
		speed = Integer.parseInt(properties.getProperty("speed", "5"));
		person = Integer.parseInt(properties.getProperty("person", "0"));
	}

	void saveProperties() {
		try {
			properties.store(Files.newBufferedWriter(propertyPath), "Mental Multiply Configuration");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void synthetize(String text) throws Exception {
		System.out.println(pitch + " " + volume + " " + speed + " " + person);
		synthetize(pitch, volume, speed, person, text);
	}

	// 根据pitch(音调),volume(音量),person(性别),text(文本)来合成mp3文本
	void synthetize(int pitch, int volume, int speed, int person, String text) throws Exception {
		// ctp(clientType)不是1会导致backend error
		String url = "http://tsn.baidu.com/text2audio?lan=zh&tok=" + accessToken + "&ctp=1&cuid=weidiao&spd=" + speed
				+ "&pit=" + pitch + "&per=" + person + "&vol=" + volume + "&tex=" + URLEncoder.encode(text, "utf8");
		HttpResponse resp = client.execute(new HttpGet(url));
		Header[] contentType = resp.getHeaders("Content-Type");
		if (contentType[0].getValue().contains("json")) {
			String s = EntityUtils.toString(resp.getEntity(), "utf8");
			JSONObject o = JSON.parseObject(s);
			int errorNo = o.getIntValue("err_no");
			// 验证失败,换一个token并保存,然后递归调用本函数
			if (errorNo == 3302 || errorNo == 502) {
				accessToken = getAccessTocken();
				properties.put("token", accessToken);
				synthetize(pitch, volume, speed, person, text);
			}
			if (errorNo != 0) {
				throw new Exception(o.getString("err_msg"));
			}
		}
		play(resp.getEntity().getContent());
	}

	// 识别一段音频
	String recognize(byte[] b) throws Exception {
		String url = "http://vop.baidu.com/server_api?lan=zh&token=" + accessToken + "&cuid=weidiao";
		HttpUriRequest req = RequestBuilder.post(url).addHeader("Content-Type", "audio/pcm;rate=" + sampleRate)
				.setEntity(new ByteArrayEntity(b)).build();
		HttpResponse resp = client.execute(req);
		String s = EntityUtils.toString(resp.getEntity(), "utf8");
		JSONObject o = JSON.parseObject(s);
		int errorNo = o.getIntValue("err_no");
		// 验证失败,换一个token并保存,然后递归调用本函数
		if (errorNo == 3302 || errorNo == 502) {
			accessToken = getAccessTocken();
			properties.put("token", accessToken);
			return recognize(b);
		}
		if (errorNo != 0) {
			throw new Exception(o.getString("err_msg"));
		}
		// 打印候选集合
		JSONArray result = o.getJSONArray("result");
		for (Object i : result) {
			print((String) i);
		}
		// 只返回最合适的答案
		return result.getString(0);
	}

	// 获取录音数据
	byte[] getRecordData(ByteArrayOutputStream cout) throws IOException {
		byte[] b = cout.toByteArray();
		ByteArrayOutputStream o = new ByteArrayOutputStream();
		AudioInputStream audioInputStream = new AudioInputStream(new ByteArrayInputStream(b), audioFormat,
				b.length / audioFormat.getFrameSize());
		AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, o);
		byte[] data = o.toByteArray();
		print("record data size" + data.length);
		return data;
	}

	// 播放音频，这里是一种同步的播放，是进程阻塞的播放
	void play(InputStream in) throws LineUnavailableException, UnsupportedAudioFileException, IOException {
		BufferedInputStream cin = new BufferedInputStream(in);
		AudioInputStream ais = AudioSystem.getAudioInputStream(cin);
		AudioFormat format = ais.getFormat();
		if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
			format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), 16, format.getChannels(),
					format.getChannels() * 2, format.getSampleRate(), false);
			ais = AudioSystem.getAudioInputStream(format, ais);
		}
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, ais.getFormat());
		SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
		sourceDataLine.open(ais.getFormat(), sourceDataLine.getBufferSize());
		sourceDataLine.start();
		int numRead = 0;
		byte[] buf = new byte[sourceDataLine.getBufferSize()];
		while ((numRead = ais.read(buf, 0, buf.length)) >= 0) {
			int offset = 0;
			while (offset < numRead) {
				offset += sourceDataLine.write(buf, offset, numRead - offset);
			}
		}
		sourceDataLine.drain();
		sourceDataLine.close();
	}

	// 下一个问题
	void nextProblem() throws Exception {
		one = Math.abs(random.nextInt()) % 89 + 11;
		two = Math.abs(random.nextInt()) % 89 + 11;
		synthetize(one + "乘以" + two);
	}

	// 将字符串转换为数字，如“一三九六”=1396
	int toInteger(String s) {
		int ans = 0;
		for (int i = 0; i < s.length(); i++) {
			int ind = numChar.indexOf(s.charAt(i));
			if (ind == -1)
				return ans;
			ans = ans * 10 + ind;
		}
		return ans;
	}

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
			if (data == null)
				return;
			String recognizeResult = recognize(data);
			if (like(recognizeResult, "再说一遍")) {
				synthetize(one + "乘以" + two);
				return;
			}
			if (like(recognizeResult, "下一题")) {
				nextProblem();
				return;
			}
			if (like(recognizeResult, "他妈的")) {
				synthetize("你他妈的，给你出题还骂人");
				return;
			}
			if (like(recognizeResult, "不算了")) {
				synthetize("不算拉倒");
				saveProperties();
				System.exit(0);
			}
			int ans = one * two;
			int mine = toInteger(recognizeResult);
			print(String.format("toInteger(%s)=%d\n", recognizeResult, mine));
			if (mine == -1) {
				synthetize("没听清");
			} else if (mine == ans) {
				synthetize("正确,真聪明");
				nextProblem();
			} else {
				synthetize("错误");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 进行录音操作
	void go() {
		try {
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
								print("submitting");
								submit(getRecordData(cout));
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
		pitchCombo.setSelectedIndex(pitch);
		row1.add(pitchCombo);
		pitchCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				pitch = pitchCombo.getSelectedIndex();
				properties.put("pitch", pitch + "");
			}
		});
		panel.add(row1);
		JPanel row2 = new JPanel();
		row2.add(new JLabel("音量"));

		JComboBox<String> volumeCombo = new JComboBox<>(nineGrade);
		volumeCombo.setSelectedIndex(volume);
		row2.add(volumeCombo);
		volumeCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				volume = volumeCombo.getSelectedIndex();
				properties.put("volume", volume + "");
			}
		});
		panel.add(row2);
		JPanel row3 = new JPanel();
		row3.add(new JLabel("语速"));
		JComboBox<String> speedComboBox = new JComboBox<>(nineGrade);
		speedComboBox.setSelectedIndex(speed);
		row3.add(speedComboBox);
		speedComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				speed = speedComboBox.getSelectedIndex();
				properties.put("speed", speed + "");
			}
		});
		panel.add(row3);
		JPanel row4 = new JPanel();
		row4.add(new JLabel("性别"));
		JComboBox<String> sexComboBox = new JComboBox<>("女 男".split(" "));
		sexComboBox.setSelectedIndex(person);
		row4.add(sexComboBox);
		panel.add(row4);
		sexComboBox.addActionListener(e -> {
			person = sexComboBox.getSelectedIndex();
			properties.put("person", person + "");
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
				System.out.println("window closing");
				saveProperties();
				System.exit(0);
			}
		});
		isRecording = true;
		new Thread(new Runnable() {
			public void run() {
				go();
			}
		}).start();
	}

	public static void main(String[] args) throws Exception {
		new Multiply();
	}
}
