package com.amc.bfsi.exception;

/** The bank's subscription plan does not allow this (answered with 402 Payment Required). */
public class PlanLimitException extends RuntimeException {

    public PlanLimitException(String message) {
        super(message);
    }
}
