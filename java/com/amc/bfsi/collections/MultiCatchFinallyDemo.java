package com.amc.bfsi.collections;
public class MultiCatchFinallyDemo {

    static int parseAndDivide(String amount, String parts) {
        try {
            int a = Integer.parseInt(amount);
            int p = Integer.parseInt(parts);
            return a / p;
        } catch (NumberFormatException | ArithmeticException e) {      // multi-catch: one block, two types
            System.out.println("  bad input -> " + e.getClass().getSimpleName());
            return -1;
        } finally {
            System.out.println("  finally runs, even after return");     // always runs
        }
    }

    public static void main(String[] args) {
        System.out.println("Case 1: 1000 / 4");
        System.out.println("  result = " + parseAndDivide("1000", "4"));

        System.out.println("Case 2: 1000 / abc");
        System.out.println("  result = " + parseAndDivide("1000", "abc"));

        System.out.println("Case 3: 1000 / 0");
        System.out.println("  result = " + parseAndDivide("1000", "0"));

        // order matters: the most specific exception must come first
        try {
            Object o = "not a number";
            Integer n = (Integer) o;
        } catch (ClassCastException e) {                 // specific first
            System.out.println("Specific catch: ClassCastException");
        } catch (RuntimeException e) {                   // general last
            System.out.println("General catch");
        }
    }
}