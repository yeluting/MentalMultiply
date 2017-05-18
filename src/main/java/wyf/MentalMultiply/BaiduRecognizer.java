package wyf.MentalMultiply;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;

import static wyf.MentalMultiply.AudioUtil.sampleRate;
import static wyf.MentalMultiply.Baidu.accessToken;
import static wyf.MentalMultiply.Baidu.getAccessTocken;
import static wyf.MentalMultiply.Baidu.getClient;
import static wyf.MentalMultiply.Multiply.properties;

/**
 * 百度语音识别器
 */
public class BaiduRecognizer implements Recognizer {

// 识别一段音频
public String recognize(byte[] b) {
   try {
      String url = "http://vop.baidu.com/server_api?lan=zh&token=" + accessToken + "&cuid=weidiao";
      HttpUriRequest req = RequestBuilder.post(url)
              .addHeader("Content-Type", "audio/pcm;rate=" + sampleRate)
              .setEntity(new ByteArrayEntity(b)).build();
      HttpResponse resp = getClient().execute(req);
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
         System.out.println(i.toString());
      }
      // 只返回最合适的答案
      return result.getString(0);
   } catch (Exception e) {
      e.printStackTrace();
   }
   return null;
}
}
