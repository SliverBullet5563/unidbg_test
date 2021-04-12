package com.github.unidbg.linux;

import com.github.unidbg.pointer.UnidbgPointer;
import com.sun.jna.Pointer;

public class FutexWrapper {
    public Pointer uaddr;
    public long uaddr_peer;
    public int futex_op;
    public int val;
    public long flag;



    public FutexWrapper(Pointer uaddr, long uaddr_peer, int futex_op, int val, long flag) {
        this.uaddr = uaddr;
        this.uaddr_peer = uaddr_peer;
        this.futex_op = futex_op;
        this.val = val;
        this.flag = flag;
    }

}
