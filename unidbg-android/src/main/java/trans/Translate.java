package trans;

import com.*;
import com.github.unidbg.*;
import com.github.unidbg.arm.ARMEmulator;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.ThreadDispatcher;
import com.github.unidbg.file.FileResult;
import com.github.unidbg.file.IOResolver;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.xhook.IxHook;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.XHookImpl;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.file.ByteArrayFileIO;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Translate extends AbstractJni implements IOResolver {
    private static final Log log = LogFactory.getLog(Translate.class);
    private final AndroidEmulator emulator;
    private final Module module;
    private final VM vm;

    private LibraryResolver createLibraryResolver() {
        return new AndroidResolver(23);
    }

    private ARMEmulator<?> createARMEmulator(String processName) {
        return AndroidEmulatorBuilder.for32Bit().setProcessName(processName).build();
    }

    static {
        String soPath = "example_binaries/libtranslate.so";
        ClassPathResource classPathResource = new ClassPathResource(soPath);
        try {
            InputStream inputStream = classPathResource.getInputStream();
            Files.copy(inputStream, Paths.get("./libtranslate.so"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Translate(){
        emulator = (AndroidEmulator) createARMEmulator("com.google.translate"); // 创建模拟器实例，要模拟32位或者64位，在这里区分
        emulator.getSyscallHandler().addIOResolver(this);
//        emulator.traceCode();

//        emulator.attach();
        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23));// 设置系统类库解析
        vm = emulator.createDalvikVM(null); // 创建Android虚拟机
        vm.setJni(this);
        vm.setVerbose(true);// 设置是否打印Jni调用细节
//        emulator.getSyscallHandler().setVerbose(true);
        new AndroidModule(emulator, vm).register(memory);
        new GLESTrace(emulator,vm).register(memory);

        // 自行修改文件路径,loadLibrary是java加载so的方法
        DalvikModule dm = vm.loadLibrary(new File("./libtranslate.so"), false);
//        dm.callJNI_OnLoad(emulator);// 手动执行JNI_OnLoad函数
        module = dm.getModule();// 加载好的so对应为一个模块

    }

    public static boolean b = false;
    private void test(){
        //开启多线程
        ThreadDispatcher.lunch = false;

        int patchCode = 0xbf00bf00; // nop
//        //nanosleep
        emulator.getMemory().pointer(0x40E6A282).setInt(0,patchCode);
        emulator.getMemory().pointer(0x40E9F6D4).setInt(0,patchCode);


        long startTime = System.currentTimeMillis();
        DvmClass WordLensSystem = vm.resolveClass("com/google/android/libraries/wordlens/WordLensSystem");

        //检查cpu
        String methodSign = "CheckCPUHasNeonNative()Z";
        boolean ret = WordLensSystem.callStaticJniMethodBoolean(emulator, methodSign);
        System.err.println("CheckCPUHasNeonNative:" + ret);

        //卸载模型
        DvmClass NativeLangMan = vm.resolveClass("com/google/android/libraries/wordlens/NativeLangMan");
//        NativeLangMan.callStaticJniMethod(emulator,"unloadDictionaryNative()I");
        int unload = NativeLangMan.callStaticJniMethodInt(emulator, "unloadDictionaryNative()I");
        System.err.println("unloadDictionaryNative:" + unload);


        //加载模型
        jkl jkl1 = createjkl1(Utils.zh,Utils.en);
        byte[] jkls = jkl1.toByteArray();
        int load = NativeLangMan.callStaticJniMethodInt(emulator, "loadDictionaryNative([B)I", jkls);

        System.err.println("loadDictionaryNative:" + load);

        if (load == 0){
            b = false;
            //输入文字
            byte[] doTrans = createjkn("你吃了吗").toByteArray();
            System.out.println("doTrans "+Utils.bytesToHexString(doTrans));

            ByteArray dvmObject = NativeLangMan.callStaticJniMethodObject(emulator, "doTranslateNative([B)[B",doTrans);

            if (dvmObject != null){
                System.out.println("\ndoTranslateNative:" + Utils.bytesToHexString(dvmObject.getValue()));
                System.out.println("计算用时： "+(System.currentTimeMillis()-startTime)+"ms");
                trans(dvmObject);
            }else {
                System.out.println("doTranslateNative: dvmObject == null");
            }

            //输入文字
//        byte[] doTrans2 = createjkn("你吃了吗,").toByteArray();
//        System.out.println("doTrans2 "+Utils.bytesToHexString(doTrans2));
//        long start = System.currentTimeMillis();
//        ByteArray dvmObject2 = NativeLangMan.callStaticJniMethodObject(emulator, "doTranslateNative([B)[B",doTrans2);
//        System.out.println("\ndoTranslateNative2:" + Utils.bytesToHexString(dvmObject2.getValue()));
//        System.out.println("计算用时2： "+(System.currentTimeMillis()-start)+"ms");
//        trans(dvmObject2);
        }

    }

    public void trans(ByteArray dvmObject){
        jkq jkq1 = com.jkq.h;
        try {
            jkq1 = (jkq) jgo.parseFrom(jkq.h, dvmObject.getValue());
            System.out.println("\ndoTranslateNative jkq: " + jkq1.b);
        } catch (com.jhd jhd) {
            jhd.printStackTrace();
        }
    }


    @Override
    public FileResult resolve(Emulator emulator, String pathname, int oflags) {
//        if (log.isDebugEnabled()) {
//            log.debug("emulator= "+emulator+", pathname= "+pathname+", oflags= "+oflags);
//        }

        if (("/proc/" + emulator.getPid() + "/stat").equals(pathname)) {
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, (emulator.getPid() + " (a.out) R 6723 6873 6723 34819 6873 8388608 77 0 0 0 41958 31 0 0 25 0 3 0 5882654 1409024 56 4294967295 134512640 134513720 3215579040 0 2097798 0 0 0 0 0 0 0 17 0 0 0\n").getBytes()));
        }
        if (("/proc/" + emulator.getPid() + "/wchan").equals(pathname)) {
            return FileResult.success(new ByteArrayFileIO(oflags, pathname, "sys_epoll".getBytes()));
        }

        if (pathname.contains(Utils.model_path)){
//            System.out.println("Utils.model_path:" + pathname);
        }
        if(pathname.contains("/dev/urandom")){
//            return FileResult.success(new ByteArrayFileIO(oflags,pathname,17640622700495508593L);
//            return FileResult.success(new DriverFileIO())
            log.debug("emulator= "+emulator+", pathname= "+pathname+", oflags= "+oflags);
//            Debugger attach = emulator.attach();
//            attach.debug();
        }
        return null;
    }


    private jkn createjkn(String text) {
        jgh createBuilder = jkn.g.createBuilder() ;
        jkn instance = (jkn) createBuilder.instance;
        instance.a = 31;
        instance.b = text;
        instance.c = false;
        instance.d = true;
        instance.e = true;
        instance.f = false;
        return instance;
    }

    private jkl createjkl1(String from, String to) {
        jgh createBuilder = jkl.k.createBuilder();
        jkl instance = (jkl) createBuilder.instance;

        String tmp = from.equals(Utils.en) ? to : from;
        if (tmp.equals(Utils.zh)) tmp = "zh";

        instance.a = from.equals(Utils.en) ?  303 : 47;
        instance.b = from;
        instance.c = to;
        instance.d = "25";
        instance.e = Utils.model_path+"/dict.en_"+tmp+"_25";
        instance.f = Utils.model_path;
        return instance;
    }


    public static void main(String[] args) {
        Translate translate = new Translate();
        translate.test();
//        translate.testHelloNeon();
    }
}
