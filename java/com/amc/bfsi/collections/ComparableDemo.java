package com.amc.bfsi.collections;

import java.util.*;

public class ComparableDemo {

    static class Customer implements Comparable<Customer> {
        private final int id;
        private final String name;

        Customer(int id, String name) { this.id = id; this.name = name; }

        @Override
        public int compareTo(Customer other) {
            return Integer.compare(this.id, other.id);
        }

        @Override
        public String toString() { return id + " " + name; }
    }

    public static void main(String[] args) {
        List<Customer> customers = new ArrayList<>(List.of(
                new Customer(103, "Rahul"),
                new Customer(101, "Anita"),
                new Customer(102, "Vikram")));

        Collections.sort(customers);
        System.out.println(customers);

        TreeSet<Customer> sorted = new TreeSet<>(customers);
        System.out.println(sorted.first());
    }
}