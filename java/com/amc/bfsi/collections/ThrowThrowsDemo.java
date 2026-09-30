package com.amc.bfsi.collections;
import java.io.IOException;

public class ThrowThrowsDemo {

    // throws = a warning on the method: "calling me may fail with IOException"
    static String readStatement(String accountNumber) throws IOException {
        if (!accountNumber.startsWith("AC")) {
            throw new IOException("No statement file for " + accountNumber);   // throw = actually fail
        }
        return "Statement for " + accountNumber;
    }

    // throw an unchecked exception for a programming mistake - no throws needed
    static double withdraw(double balance, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return balance - amount;
    }

    public static void main(String[] args) {
        // CHECKED: the compiler forces us to handle it
        try {
            System.out.println(readStatement("AC5001"));
            System.out.println(readStatement("XY9999"));
        } catch (IOException e) {
            System.out.println("Checked exception handled: " + e.getMessage());
        }

        // UNCHECKED: the compiler does not force us, but we can still catch it
        try {
            System.out.println(withdraw(5000, 1000));
            System.out.println(withdraw(5000, -50));
        } catch (IllegalArgumentException e) {
            System.out.println("Unchecked exception handled: " + e.getMessage());
        }
    }
}