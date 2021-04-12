package thread;

import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.hook.HookContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.xhook.IxHook;
import com.github.unidbg.linux.android.AndroidARMEmulator;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.XHookImpl;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import trans.Translate;

import java.io.File;

public class Test  extends AbstractJni {
    private final AndroidARMEmulator emulator;
    private final VM vm;
    private final Module mymd;
    private DvmClass Native;

    private Test(){

        emulator =  (AndroidARMEmulator) AndroidEmulatorBuilder
                .for32Bit()
                .build();
        final Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM(null);
        vm.setJni(this);
        vm.setVerbose(true);

        emulator.getSyscallHandler().setVerbose(true);
        new AndroidModule(emulator, vm).register(memory);

        DalvikModule dm = vm.loadLibrary(new File("./unidbg-android/src/main/java/thread/libcond2.so"), false);
//        DalvikModule dm = vm.loadLibrary(new File("./unidbg-android/src/main/java/thread/libhello-jni.so"), false);

        mymd=dm.getModule();
        emulator.getThreadDispatcher().setModule(mymd);
        dm.callJNI_OnLoad(emulator);

    }
    private void test(){

        IxHook ixHook = XHookImpl.getInstance(emulator);
        ixHook.register("libcond2.so", "pthread_mutex_lock", new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                System.out.println("pthread_mutex_lock=" + context.getPointerArg(0)+" state="+context.getPointerArg(0).getInt(0));
                emulator.getUnwinder().unwind();
                return super.onCall(emulator, context, originFunction);
            }
        });
        ixHook.register("libcond2.so", "pthread_mutex_unlock", new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator<?> emulator, HookContext context, long originFunction) {
                System.out.println("pthread_mutex_unlock=" + context.getPointerArg(0)+" state="+context.getPointerArg(0).getInt(0));
                emulator.getUnwinder().unwind();
                return super.onCall(emulator, context, originFunction);
            }
        });
//        ixHook.refresh(); // 使Import hook生效

        int patchCode = 0xbf00bf00; // nop
        emulator.getMemory().pointer(0x400018D2).setInt(0,patchCode); //cond2 patch sleep
//        emulator.getMemory().pointer(0x400015A2).setInt(0,patchCode); //cond


        Native = vm.resolveClass("com.example.hellojni.HelloJni".replace(".", "/"));
        DvmObject ret=Native.callStaticJniMethodObject(emulator,"stringFromJNI()Ljava/lang/String;");
//        System.err.println(ret.toString());
    }
    public static void main(String[] args) throws Exception {
        Test test = new Test();
        test.test();
    }
}