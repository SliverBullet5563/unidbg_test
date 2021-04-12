package trans;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.LibraryResolver;
import com.github.unidbg.arm.ARMEmulator;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class HelloNeon extends AbstractJni {
    private final AndroidEmulator emulator;
    //    private final Module module;
    private final VM vm;

    private LibraryResolver createLibraryResolver() {
        return new AndroidResolver(23);
    }

    private ARMEmulator<?> createARMEmulator(String processName) {
        return AndroidEmulatorBuilder.for32Bit().setProcessName(processName).build();
    }

    static {
        String soPath = "example_binaries/libhello-neon.so";
        ClassPathResource classPathResource = new ClassPathResource(soPath);
        try {
            InputStream inputStream = classPathResource.getInputStream();
            Files.copy(inputStream, Paths.get("./libhello-neon.so"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HelloNeon(){
        emulator = (AndroidEmulator) createARMEmulator("com.google.translate"); // 创建模拟器实例，要模拟32位或者64位，在这里区分
//        emulator.getSyscallHandler().addIOResolver(this);

        final Memory memory = emulator.getMemory(); // 模拟器的内存操作接口
        memory.setLibraryResolver(new AndroidResolver(23));// 设置系统类库解析


        vm = emulator.createDalvikVM(null); // 创建Android虚拟机
        vm.setJni(this);
        vm.setVerbose(true);// 设置是否打印Jni调用细节

        // 自行修改文件路径,loadLibrary是java加载so的方法
        DalvikModule dm = vm.loadLibrary(new File("./libhello-neon.so"), false);
        dm.callJNI_OnLoad(emulator);// 手动执行JNI_OnLoad函数
//        module = dm.getModule();// 加载好的so对应为一个模块
    }
    private void testHelloNeon(){
        DvmClass HelloNeon = vm.resolveClass("com/example/helloneon/HelloNeon");
        String methodSign = "stringFromJNI()Ljava/lang/String;";
        StringObject str = HelloNeon.callStaticJniMethodObject(emulator,methodSign);
        System.out.println("HelloNeon:" + str);

    }

    public static void main(String[] args) {
        HelloNeon helloNeon = new HelloNeon();
        helloNeon.testHelloNeon();
    }



}
