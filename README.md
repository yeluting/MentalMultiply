1.需要注册一个百度应用账号，需要根据以下三项请求accessToken，accessToken的有效期为一个月,所以一个应用必须得要能够自动更新token
```
String apiId = "8614141";
String apiKey = "qXUeB1kXyOPgD9mkGCzcnIID";
String secretKey = "3447f3b010f22ad1eabe54614ef4519d";
```

2.关于frame和sample和channel
* frameSize表示一个frame包含的字节数，单位是byte
* 一个frame包含channel个sample，即一帧包括样本个数即为通道个数，每个通道记录一个采样
* 故frameSize=sampleSize/8*channelCount，sampleSize表示采样位数，单位是bit
* frameRate和sampleRate是一回事，都表示采样率

3.百度语音语音识别当然是用post方式，百度语音语音合成有两种方式get和post。get方式优点是简单速度快容易调试，缺点是只能传递256个字节的内容，也就是如果合成长文本必然会出错。所以长文本只能使用post方式。

4.targetDataline永远不能调用drain函数，即录音机不能耗费掉它的现有音频，否则进入一个死循环，因为录音机永远无法耗尽它的内容，它时刻都在进行录音。

5.问题：给定一个数组，这个数组中包含两种无用信息：
* 噪音
* 空白音，就是没有实际意义的音

如何把空白音删除掉，当越过一个真实声音山峰之后，再遇到空白音持续一段时间则进行提交

6.改良：自己实现语音识别模块，因为用户说的只能是“数字”，就像验证码图片一样只包含特定的字符，这样一来就能够提高识别的准确率。

## 知识
* 频域对称轴为采样率的二分之一