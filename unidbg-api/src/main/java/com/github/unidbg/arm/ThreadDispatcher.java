package com.github.unidbg.arm;

import com.github.unidbg.Emulator;
import com.github.unidbg.Module;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.arm.backend.CodeHook;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.unix.UnixThread;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import unicorn.ArmConst;
import unicorn.Unicorn;

import java.util.*;

public class ThreadDispatcher {
    private static final Log log = LogFactory.getLog(ThreadDispatcher.class);
    public volatile static boolean lunch = true;

    public final Map<Integer, UnixThread> threadMap = new HashMap<>(5);
    public static int thread_count_index=0;
    public int current_thread_id=0;
    private final Emulator<?> emulator;
    private long hit_count=0;
    public boolean isWait;
    public final Set<Integer> waitThread = new HashSet<>();
    private Module module;

    public ThreadDispatcher(Emulator<?> emulator){
        this.emulator=emulator;
    }

    public long CurrentThreadContext(){
        long context = threadMap.get(current_thread_id).context;
//        System.out.println("CurrentThreadContext: "+context+" current_thread_id = "+current_thread_id);
//        emulator.showRegs();
        return context;
    }

    private int GetNextThreadId(){
         Object[] key = threadMap.keySet().toArray();
         int index=-1;
         for(int i=0;i<key.length;i++){
             if((Integer) key[i]==current_thread_id){
                 index=i;
                 break;
             }
         }
         index=(index+1)%key.length;
         return (Integer) key[index];
    }

    public void ExitCurrentThread(){

        long in_pc=0;
        Backend backend=emulator.getBackend();
        int prepare_to_delect_id=current_thread_id;

        if (log.isDebugEnabled()){
            log.debug("--- ExitCurrentThread ---"+current_thread_id);
            log.debug("thread " + prepare_to_delect_id+" exit");
        }


        /*获取下个需要调度的线程*/
        current_thread_id=GetNextThreadId();
        //排除被阻塞的线程
        while (waitThread.contains(current_thread_id)){
            current_thread_id = GetNextThreadId();
            if (log.isDebugEnabled())
                log.debug("ExitCurrentThread 切换被阻塞的线程：current_thread_id="+current_thread_id+" waitThread = "+waitThread);

        }

        threadMap.remove(prepare_to_delect_id);

        backend.context_restore(threadMap.get(current_thread_id).context);
        if (log.isDebugEnabled()){
            log.debug("--------ExitCurrentThread-------- 切换寄存器 current_thread_id = "+current_thread_id+" waitThread = "+waitThread+"---------------------------------");
            emulator.showRegs();
            emulator.getUnwinder().unwind();
        }

//        backend.emu_stop();

        UnidbgPointer pc= UnidbgPointer.pointer(emulator,backend.reg_read(ArmConst.UC_ARM_REG_PC));

        Cpsr cpsr=Cpsr.getArm(backend);
        in_pc=pc.peer;
        if(cpsr.isThumb()){
            in_pc=pc.peer+1;
        }
//        System.out.println(this);
        backend.reg_write(ArmConst.UC_ARM_REG_PC,in_pc);

//        emulator.attach().debug();
    }

    public void setModule(Module module){
        this.module = module;
    }


    public synchronized void ExchangeThreadImmediately(String msg){
        if (msg != null)
            hit_count = 0;

        long in_pc=0;
        Backend backend=emulator.getBackend();
        /*保存当前上下文*/
        backend.context_save(CurrentThreadContext());
        if (log.isDebugEnabled()){
            log.debug("---- 当前寄存器 current_thread_id = ["+current_thread_id+"] waitThread = "+waitThread+" --------------------------------msg = "+msg);
            emulator.showRegs();
            emulator.getUnwinder().unwind();
        }


        /*选择下个线程*/
        current_thread_id=GetNextThreadId();

        //排除被阻塞的线程
        while (waitThread.contains(current_thread_id)){
            if (log.isDebugEnabled())
                log.debug("切换被阻塞的线程：current_thread_id="+current_thread_id);
            current_thread_id = GetNextThreadId();
        }


        /*恢复下个线程的上下文*/
        backend.context_restore(threadMap.get(current_thread_id).context);
        if (log.isDebugEnabled()){
            log.debug("---- 切换寄存器 current_thread_id = ["+current_thread_id+"] waitThread = "+waitThread+" ---------------------------------msg = "+msg);
            emulator.showRegs();
//        emulator.getUnwinder().unwind();
//        backend.emu_stop();
            log.debug("ExchangeThread! "+this+" fn: "+threadMap.get(current_thread_id).child_stack.getPointer(48));

        }


        UnidbgPointer pc= UnidbgPointer.pointer(emulator,backend.reg_read(ArmConst.UC_ARM_REG_PC));

        Cpsr cpsr=Cpsr.getArm(backend);
        in_pc=pc.peer;
        if(cpsr.isThumb()){
            in_pc=pc.peer+1;
        }

        backend.reg_write(ArmConst.UC_ARM_REG_PC,in_pc);


    }

    public void ExchangeThread(){
        hit_count++;
        if(hit_count%1000==0){
           ExchangeThreadImmediately(null);
        }
    }

    private boolean isHook;
    public void hook_thread(final Emulator<?> emulator, Backend backend) {
        //doTranslateNative 地址区间
        if(!isHook){
//                    if(!isHook && (begin >= 0x404675E4 && begin < 0x4046767C)){  //loadDictionaryNative 区间

            System.err.println("--------------进入hook--------------");

            isHook= true;
            backend.hook_add_new(new CodeHook() {

                @Override
                public void onAttach(Unicorn.UnHook unHook) {
                }

                @Override
                public void detach() {
                }

                @Override
                public void hook(Backend backend, long address, int size, Object user) {

                    ThreadDispatcher threadDispatcher=emulator.getThreadDispatcher();
                    UnidbgPointer pcPointer = emulator.getContext().getPCPointer();

                    if(threadDispatcher.threadMap.size()>1){ //存在多个线程

//                            System.out.println("address: "+Long.toHexString(address)+" size: "+size);
                        threadDispatcher.ExchangeThread();
//                                    System.err.println("hook 线程发生，切换线程");

//                                if(threadDispatcher.current_thread_id==0 && flag)
//                                    printAssemble(System.out,address,4);
                    }
                    if(address>0xffffff00l && threadDispatcher.current_thread_id!=0){
                        System.err.println("hook 退出线程");
                        threadDispatcher.ExitCurrentThread();
                    }

                }
            },1,0,this);
        }
    }

    @Override
    public String toString() {
        return "threadID："+current_thread_id+",is thumb:"+",running......";
    }

}
