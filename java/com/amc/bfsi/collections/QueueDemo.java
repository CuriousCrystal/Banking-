package com.amc.bfsi.collections;
import java.util.*;

/** The fourth family: Queue. Who is served next? */
public class QueueDemo {

    public static void main(String[] args) {

        // 1. QUEUE - first in, first out. The branch token system.
        Queue<String> tokens = new ArrayDeque<>();
        tokens.offer("T-101");                       // offer = add at the back
        tokens.offer("T-102");
        tokens.offer("T-103");

        System.out.println("Queue (FIFO): " + tokens);
        System.out.println("  next to be served : " + tokens.peek());   // look, do not remove
        System.out.println("  serving           : " + tokens.poll());   // remove and return
        System.out.println("  waiting now       : " + tokens);

        // 2. STACK - last in, first out. The undo list.
        Deque<String> undo = new ArrayDeque<>();
        undo.push("Deposit 5000");                   // push = add on top
        undo.push("Withdraw 2000");
        undo.push("Transfer 1000");

        System.out.println();
        System.out.println("Stack (LIFO): " + undo);
        System.out.println("  undo first : " + undo.pop());             // the newest goes first
        System.out.println("  left       : " + undo);

        // 3. PRIORITY QUEUE - best first, whatever "best" means to the Comparator.
        PriorityQueue<Loan> queue = new PriorityQueue<>(
                Comparator.comparingInt(Loan::risk).reversed());        // highest risk first
        queue.offer(new Loan("Anita", 40));
        queue.offer(new Loan("Vikram", 85));
        queue.offer(new Loan("Rahul", 60));

        System.out.println();
        System.out.println("PriorityQueue printed : " + queue);         // heap order - NOT sorted
        System.out.print("Reviewed in order     : ");
        while (!queue.isEmpty()) {
            System.out.print(queue.poll() + "  ");                      // poll gives priority order
        }
        System.out.println();

        // 4. empty queue: poll returns null, remove() throws
        Queue<String> empty = new ArrayDeque<>();
        System.out.println();
        System.out.println("poll on empty queue   : " + empty.poll());  // null, no exception
        try {
            empty.remove();
        } catch (NoSuchElementException e) {
            System.out.println("remove on empty queue : NoSuchElementException");
        }
    }

    record Loan(String customer, int risk) {
        @Override public String toString() { return customer + "(" + risk + ")"; }
    }
}