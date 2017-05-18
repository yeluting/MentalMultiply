package wyf.MentalMultiply;

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.Properties;

public class Baidu {
final static private HttpClient client = HttpClients.createDefault();

// 定义token相关
static String apiId;
static String apiKey;
static String secretKey;
static String accessToken = null;

static {
   try {
      Properties p = new Properties();
      p.load(Baidu.class.getResourceAsStream("/baidu.properties"));
      apiId = p.getProperty("apiId");
      apiKey = p.getProperty("apiKey");
      secretKey = p.getProperty("secretKey");
   } catch (Exception e) {
      e.printStackTrace();
   }
}

static HttpClient getClient() {
   return client;
}

// 获取令牌串
static String getAccessTocken() {
   try {
      String url = "https://openapi.baidu.com/oauth/2.0/token";
      HttpUriRequest req = RequestBuilder.post(url)
              .addParameter("grant_type", "client_credentials")
              .addParameter("client_id", apiKey)
              .addParameter("client_secret", secretKey)
              .build();
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
}
