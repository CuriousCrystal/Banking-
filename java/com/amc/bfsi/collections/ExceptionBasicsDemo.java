package com.amc.bfsi.collections;
public class ExceptionBasicsDemo {

    public static void main(String[] args) {

        // 1. ArithmeticException - dividing an int by zero
        try {
            int totalBalance = 50000;
            int accounts = 0;
            int average = totalBalance / accounts;          // throws here
            System.out.println("Average: " + average);      // never runs
        } catch (ArithmeticException e) {
            System.out.println("Caught: " + e.getMessage());           // / by zero
        }

        // 2. ArrayIndexOutOfBoundsException - reading past the end
        String[] branches = {"Bangalore", "Mumbai", "Chennai"};
        try {
            System.out.println(branches[3]);                // index 3 does not exist
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        // 3. NumberFormatException - bad text from a form
        String amountText = "25O00";                          // letter O, not zero
        try {
            int amount = Integer.parseInt(amountText);
            System.out.println("Amount: " + amount);
        } catch (NumberFormatException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        // 4. NullPointerException - calling a method on nothing
        String customerName = null;
        try {
            System.out.println(customerName.length());
        } catch (NullPointerException e) {
            System.out.println("Caught: NullPointerException");
        }

        System.out.println("The program is still running.");  // proof the catches worked
    }
}