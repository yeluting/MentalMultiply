package wyf.MentalMultiply;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.InputStream;
import java.net.URLEncoder;

import static wyf.MentalMultiply.Baidu.accessToken;
import static wyf.MentalMultiply.Baidu.getAccessTocken;
import static wyf.MentalMultiply.Baidu.getClient;
import static wyf.MentalMultiply.Multiply.properties;

/**
 * 百度语音合成器
 */
public class BaiduSynthesizer implements Synthesizer {
int pitch = 3, volume = 3, speed = 3, person = 0;

// 根据pitch(音调),volume(音量),person(性别),text(文本)来合成mp3文本
InputStream synthesize(int pitch, int volume, int speed, int person, String text) throws Exception {
   // ctp(clientType)不是1会导致backend error
   String url = "http://tsn.baidu.com/text2audio?lan=zh&tok="
           + accessToken + "&ctp=1&cuid=weidiao&spd=" + speed
           + "&pit=" + pitch
           + "&per=" + person
           + "&vol=" + volume
           + "&tex=" + URLEncoder.encode(text, "utf8");
   HttpResponse resp = getClient().execute(new HttpGet(url));
   Header[] contentType = resp.getHeaders("Content-Type");
   if (contentType[0].getValue().contains("json")) {
      String s = EntityUtils.toString(resp.getEntity(), "utf8");
      JSONObject o = JSON.parseObject(s);
      int errorNo = o.getIntValue("err_no");
      // 验证失败,换一个token并保存,然后递归调用本函数
      if (errorNo == 3302 || errorNo == 502) {
         accessToken = getAccessTocken();
         properties.put("token", accessToken);
         synthesize(pitch, volume, speed, person, text);
      }
      if (errorNo != 0) {
         throw new Exception(o.getString("err_msg"));
      }
   }
   return resp.getEntity().getContent();
}

public InputStream synthesize(String text) {
   try {
      return synthesize(pitch, volume, speed, person, text);
   } catch (Exception e) {
      e.printStackTrace();
   }
   return null;
}
}
