package com.amc.bfsi.collections;

import java.util.*;

public class EqualsHashCodeDemo {

    // Version 1: no equals / hashCode
    static class Customer {
        private final int id;
        private final String name;
        Customer(int id, String name) { this.id = id; this.name = name; }
    }

    // Version 2: equals and hashCode based on id
    static class CustomerFixed {
        private final int id;
        private final String name;
        CustomerFixed(int id, String name) { this.id = id; this.name = name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;                          // same object in memory
            if (!(o instanceof CustomerFixed)) return false;     // different type
            return id == ((CustomerFixed) o).id;                 // same id = same customer
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);                             // the SAME field equals() uses
        }
    }

    public static void main(String[] args) {
        Set<Customer> broken = new HashSet<>();
        broken.add(new Customer(101, "Anita"));
        broken.add(new Customer(101, "Anita"));
        System.out.println("Without equals/hashCode: " + broken.size());   // 2 - duplicate kept

        Set<CustomerFixed> fixed = new HashSet<>();
        fixed.add(new CustomerFixed(101, "Anita"));
        fixed.add(new CustomerFixed(101, "Anita"));
        System.out.println("With equals/hashCode:    " + fixed.size());    // 1 - duplicate rejected

        CustomerFixed a = new CustomerFixed(101, "Anita");
        CustomerFixed b = new CustomerFixed(101, "Anita");
        System.out.println("a == b      : " + (a == b));        // false - two objects
        System.out.println("a.equals(b) : " + a.equals(b));     // true  - same customer
    }
}