package com.amc.bfsi.collections;

import java.util.*;

/** The same five entries through all four families, so the difference is visible. */
public class FourFamiliesDemo {

    public static void main(String[] args) {

        String[] arriving = {"Anita", "Vikram", "Anita", "Rahul", "Sneha"};   // Anita twice

        // LIST - keeps order, keeps duplicates, reachable by position
        List<String> list = new ArrayList<>();
        for (String name : arriving) list.add(name);
        System.out.println("LIST  " + list);
        System.out.println("      size " + list.size() + ", position 0 = " + list.get(0)
                + ", contains Rahul = " + list.contains("Rahul"));

        // SET - drops duplicates, no position
        Set<String> set = new LinkedHashSet<>();          // LinkedHashSet so the order is readable
        for (String name : arriving) set.add(name);
        System.out.println("SET   " + set);
        System.out.println("      size " + set.size() + " - Anita stored once");

        // MAP - a value for each key, keys unique
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("Anita", 52000.0);
        map.put("Vikram", 18000.0);
        map.put("Anita", 57000.0);                        // same key: replaces, does not add
        map.put("Rahul", 96000.0);
        System.out.println("MAP   " + map);
        System.out.println("      size " + map.size() + ", balance of Anita = " + map.get("Anita"));

        // QUEUE - order of service, taken from the front
        Queue<String> queue = new ArrayDeque<>();
        for (String name : arriving) queue.offer(name);
        System.out.println("QUEUE " + queue);
        System.out.println("      serving " + queue.poll() + ", then " + queue.poll()
                + ", waiting " + queue);

        System.out.println();
        System.out.println("Choose by the question you need answered:");
        System.out.println("  What is at position 3?        -> List");
        System.out.println("  Have I seen this before?      -> Set");
        System.out.println("  What is the value for a key?  -> Map");
        System.out.println("  Who is next?                  -> Queue");
    }
}