package com.amc.bfsi.collections;

import java.util.*;

public class ArrayListDemo {
    public static void main(String[] args) {
        List<String> customers = new ArrayList<>();
        customers.add("Anita");
        customers.add("Vikram");
        customers.add("Rahul");
        customers.add(1, "Sneha");

        System.out.println(customers.get(0));
        System.out.println(customers.size());
        System.out.println(customers.contains("Rahul Nair"));

        customers.set(2, "Vikram");
        customers.remove("Rahul");
        Collections.sort(customers);

        for (String c : customers) {
            System.out.println(c);
        }
        // Anita Rao, Sneha Iyer, Vikram S. Shah
    }
}