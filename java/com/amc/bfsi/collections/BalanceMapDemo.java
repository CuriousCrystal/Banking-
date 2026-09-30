package com.amc.bfsi.collections;

import java.util.*;

public class BalanceMapDemo {
    public static void main(String[] args) {
        Map<String, Double> balances = new HashMap<>();
        balances.put("AC5001", 52000.0);
        balances.put("AC5002", 130000.0);
        balances.put("AC5003", 18000.0);

        System.out.println(balances.get("AC5002"));
        System.out.println(balances.getOrDefault("AC9999", 0.0));
        balances.merge("AC5001", 5000.0, Double::sum);
        balances.computeIfPresent("AC5003", (k, v) -> v - 2000);

        for (Map.Entry<String, Double> e : balances.entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue());
        }

        Map<String, Integer> count = new HashMap<>();
        for (String t : List.of("DEPOSIT", "WITHDRAW", "DEPOSIT", "TRANSFER", "DEPOSIT")) {
            count.merge(t, 1, Integer::sum);
        }
        System.out.println(count);
    }
}