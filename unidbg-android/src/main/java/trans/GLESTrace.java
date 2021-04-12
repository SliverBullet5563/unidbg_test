package trans;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.Arm64Svc;
import com.github.unidbg.arm.ArmSvc;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.virtualmodule.VirtualModule;

import java.util.Map;

public class GLESTrace extends VirtualModule<VM> {


    protected GLESTrace(Emulator<?> emulator, VM vm) {
        super(emulator, vm, "libGLES_trace.so");
    }

    @Override
    protected void onInitialize(Emulator<?> emulator, VM extra, Map<String, UnidbgPointer> symbols) {
        boolean is64Bit = emulator.is64Bit();
        SvcMemory svcMemory = emulator.getSvcMemory();
        System.err.println("GLESTrace onInitialize:");
        symbols.put("111111",svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return 0L;
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return 0L;
            }
        }));
    }
}
