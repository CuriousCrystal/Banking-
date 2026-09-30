package com.amc.bfsi.collections;
public class TryWithResourcesDemo {

    // Anything that implements AutoCloseable can go in try( ... )
    static class BankConnection implements AutoCloseable {
        private final String name;
        BankConnection(String name) {
            this.name = name;
            System.out.println("  open  " + name);
        }
        void fetchBalance(String account) {
            if (account.isEmpty()) {
                throw new IllegalArgumentException("account number is empty");
            }
            System.out.println("  balance of " + account + " = 52000");
        }
        @Override
        public void close() {
            System.out.println("  close " + name);        // called automatically
        }
    }

    public static void main(String[] args) {
        System.out.println("Normal run:");
        try (BankConnection con = new BankConnection("core-banking")) {
            con.fetchBalance("AC5001");
        }

        System.out.println("Run that fails:");
        try (BankConnection con = new BankConnection("core-banking")) {
            con.fetchBalance("");                          // throws
        } catch (IllegalArgumentException e) {
            System.out.println("  caught: " + e.getMessage());
        }
        // In both runs close() was called - no finally block needed
    }
}