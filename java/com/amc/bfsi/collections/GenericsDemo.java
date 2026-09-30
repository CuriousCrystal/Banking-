package com.amc.bfsi.collections;

import java.util.*;

public class GenericsDemo {

    static class Box<T> {
        private final T value;
        Box(T value) { this.value = value; }
        T get() { return value; }
    }

    static <T> T firstOrDefault(List<T> list, T fallback) {
        return list.isEmpty() ? fallback : list.get(0);
    }

    public static void main(String[] args) {
        List<String> names = new ArrayList<>();
        names.add("Anita");
        // names.add(101);
        String first = names.get(0);

        Box<Double> balance = new Box<>(52000.0);
        double b = balance.get();

        System.out.println(first);
        System.out.println(b);
        System.out.println(firstOrDefault(new ArrayList<String>(), "none"));
    }
}