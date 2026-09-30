package com.amc.bfsi.collections;
import java.util.*;

/** Capstone: HashMap + custom exceptions + Comparator in one small program. */
public class BankTransferDemo {

    static class InsufficientBalanceException extends Exception {
        InsufficientBalanceException(String message) { super(message); }
    }

    static class AccountNotFoundException extends RuntimeException {
        AccountNotFoundException(String message) { super(message); }
    }

    static class Account {
        private final String number;
        private final String owner;
        private double balance;
        Account(String number, String owner, double balance) {
            this.number = number; this.owner = owner; this.balance = balance;
        }
        String getNumber() { return number; }
        double getBalance() { return balance; }
        @Override
        public String toString() { return number + " " + owner + " " + balance; }
    }

    private final Map<String, Account> accounts = new HashMap<>();

    void open(Account a) { accounts.put(a.getNumber(), a); }

    Account find(String number) {
        Account a = accounts.get(number);
        if (a == null) throw new AccountNotFoundException("No account " + number);
        return a;
    }

    void transfer(String from, String to, double amount) throws InsufficientBalanceException {
        Account source = find(from);
        Account target = find(to);
        if (source.balance < amount) {
            throw new InsufficientBalanceException(
                    from + " has " + source.balance + ", cannot send " + amount);
        }
        source.balance -= amount;
        target.balance += amount;
        System.out.println("Transferred " + amount + " from " + from + " to " + to);
    }

    public static void main(String[] args) {
        BankTransferDemo bank = new BankTransferDemo();
        bank.open(new Account("AC5001", "Anita", 52000));
        bank.open(new Account("AC5002", "Vikram", 18000));
        bank.open(new Account("AC5003", "Rahul", 96000));

        String[][] requests = {
            {"AC5001", "AC5002", "10000"},      // fine
            {"AC5002", "AC5003", "90000"},      // not enough money
            {"AC5001", "AC9999", "500"}         // no such account
        };

        for (String[] r : requests) {
            try {
                bank.transfer(r[0], r[1], Double.parseDouble(r[2]));
            } catch (InsufficientBalanceException e) {
                System.out.println("Refused: " + e.getMessage());
            } catch (AccountNotFoundException e) {
                System.out.println("Refused: " + e.getMessage());
            }
        }

        List<Account> byBalance = new ArrayList<>(bank.accounts.values());
        byBalance.sort(Comparator.comparingDouble(Account::getBalance).reversed());
        System.out.println("Accounts, richest first:");
        for (Account a : byBalance) {
            System.out.println("  " + a);
        }
    }
}