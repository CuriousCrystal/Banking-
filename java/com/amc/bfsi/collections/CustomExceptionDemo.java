package com.amc.bfsi.collections;
import java.util.*;

public class CustomExceptionDemo {

    // CHECKED custom exception - extends Exception. The caller MUST handle it.
    static class CustomerNotFoundException extends Exception {
        CustomerNotFoundException(String message) { super(message); }
    }

    // UNCHECKED custom exception - extends RuntimeException. A rule broken by bad input.
    static class InvalidAmountException extends RuntimeException {
        InvalidAmountException(String message) { super(message); }
    }

    static class CustomerRepository {
        private final Map<Integer, String> store = new HashMap<>();

        void save(int id, String name) { store.put(id, name); }

        String findById(int id) throws CustomerNotFoundException {
            String name = store.get(id);
            if (name == null) {
                throw new CustomerNotFoundException("No customer with id " + id);
            }
            return name;
        }
    }

    static double deposit(double balance, double amount) {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit must be positive, got " + amount);
        }
        return balance + amount;
    }

    public static void main(String[] args) {
        CustomerRepository repo = new CustomerRepository();
        repo.save(101, "Anita");
        repo.save(102, "Vikram");

        try {
            System.out.println("Found: " + repo.findById(101));
            System.out.println("Found: " + repo.findById(999));
        } catch (CustomerNotFoundException e) {
            System.out.println("Handled: " + e.getMessage());
        } finally {
            System.out.println("Lookup finished");
        }

        try {
            System.out.println("New balance: " + deposit(5000, 2000));
            System.out.println("New balance: " + deposit(5000, -100));
        } catch (InvalidAmountException e) {
            System.out.println("Handled: " + e.getMessage());
        }
    }
}