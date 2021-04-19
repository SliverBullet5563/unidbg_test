基于https://github.com/zhkl0228/unidbg 0.9.4版本；

https://github.com/asmjmp0/unidbgMutilThread unidbg多线程；

以https://github.com/cxapython/unidbgok 的构建方式，增加多线程同步机制。

测试例子/unidbg-android/src/main/java/thread/Test

未完成对翻译so的调用，位于/unidbg-android/src/main/java/trans/Translate

翻译so的测试apk /unidbg-android/src/main/java/trans/translate_demo.apk


【2021.4.19】注：
翻译so的问题，已被原作者@zhkl0228帮助解决，合并到主仓库的测试用例 https://github.com/zhkl0228/unidbg/blob/master/unidbg-android/src/test/java/com/google/translate/NativeLangMan.java

经测试分析，导致翻译so调用不成功的原因不是多线程的问题，是加载模型的原因。目前尚未清楚，加载全部模型（模型位置unidbg-android/src/main/resources/android/sdk23/data/user/file/），会导致调用失败的原因。最后感谢大佬帮助解决了问题。
