package com.amc.bfsi.collections;

import java.util.*;

public class DuplicatePanDemo {
    public static void main(String[] args) {
        Set<String> pans = new HashSet<>();
        String[] incoming = {"ABCDE1234F", "BCDEF2345G", "ABCDE1234F", "CDEFG3456H"};

        for (String pan : incoming) {
            if (!pans.add(pan)) {
                System.out.println("Duplicate PAN rejected: " + pan);
            }
        }
        System.out.println(pans.size());
        System.out.println(pans.contains("BCDEF2345G"));
    }
}