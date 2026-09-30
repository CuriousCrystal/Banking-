package com.amc.bfsi.collections;

import java.util.*;

public class ComparatorDemo {

    static class Customer {
        private final String name;
        private final String city;
        private final double balance;

        Customer(String name, String city, double balance) {
            this.name = name; this.city = city; this.balance = balance;
        }
        String getName() { return name; }
        String getCity() { return city; }
        double getBalance() { return balance; }

        @Override
        public String toString() { return name + " (" + city + ", " + balance + ")"; }
    }

    public static void main(String[] args) {
        List<Customer> list = new ArrayList<>(List.of(
                new Customer("Vikram", "Mumbai", 18000),
                new Customer("Anita", "Bangalore", 52000),
                new Customer("Rahul", "Bangalore", 96000)));

        list.sort(new Comparator<Customer>() {
            @Override
            public int compare(Customer a, Customer b) {
                return Double.compare(b.getBalance(), a.getBalance());
            }
        });
        System.out.println(list.get(0).getName());

        list.sort((a, b) -> a.getName().compareTo(b.getName()));
        System.out.println(list.get(0).getName());

        list.sort(Comparator.comparing(Customer::getCity)
                            .thenComparing(Comparator.comparingDouble(Customer::getBalance).reversed()));
        System.out.println(list);
        // [Rahul (Bangalore, 96000.0), Anita (Bangalore, 52000.0), Vikram (Mumbai, 18000.0)]
    }
}