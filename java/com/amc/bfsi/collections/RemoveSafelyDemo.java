package com.amc.bfsi.collections;
import java.util.*;

public class RemoveSafelyDemo {
    public static void main(String[] args) {
        List<Double> balances = new ArrayList<>(List.of(52000.0, 0.0, 18000.0, 0.0));

        // WRONG - throws ConcurrentModificationException:
        // for (Double b : balances) { if (b == 0.0) balances.remove(b); }

        Iterator<Double> it = balances.iterator();
        while (it.hasNext()) {
            if (it.next() == 0.0) {
                it.remove();
            }
        }
        System.out.println(balances);

        balances.removeIf(b -> b < 20000);
        System.out.println(balances);
    }
}