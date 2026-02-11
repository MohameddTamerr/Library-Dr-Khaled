package com.library.pos;

import com.library.pos.util.SecurityUtil;

public class GetMachineID {
    public static void main(String[] args) {
        System.out.println("MACHINE_ID_START");
        System.out.println(SecurityUtil.getMachineId());
        System.out.println("MACHINE_ID_END");
    }
}
