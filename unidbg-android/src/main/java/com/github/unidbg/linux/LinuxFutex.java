package com.github.unidbg.linux;

import com.sun.jna.Pointer;

import java.util.HashMap;

public class LinuxFutex {

    private LinuxFutex(){
        futex_q = new HashMap<>();
    }

    private static class SingleHolder{
        private static final LinuxFutex Instance = new LinuxFutex();
    }

    public static LinuxFutex getInstance(){
        return SingleHolder.Instance;
    }

    public HashMap<FutexWrapper, Integer> futex_q;

}
