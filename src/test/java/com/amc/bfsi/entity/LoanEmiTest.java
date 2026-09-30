package com.amc.bfsi.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Pure math tests - no Spring context, runs in milliseconds. */
class LoanEmiTest {

    private Loan loan(String principal, String rate, int months) {
        return new Loan("HOME", new BigDecimal(principal), new BigDecimal(rate), months);
    }

    @Test
    void standardReducingBalanceEmi() {
        // 10,00,000 @ 8.5% for 240 months -> 8,678.23 (reference computed independently)
        BigDecimal emi = loan("1000000.00", "8.50", 240).calculateEmi();
        assertThat(emi).isEqualByComparingTo("8678.23");
        assertThat(emi.scale()).isEqualTo(2);
    }

    @Test
    void zeroRateIsSimpleDivision() {
        BigDecimal emi = loan("120000.00", "0.00", 24).calculateEmi();
        assertThat(emi).isEqualByComparingTo("5000.00");
    }

    @Test
    void totalPayableIsEmiTimesTenure() {
        Loan l = loan("500000.00", "9.00", 60);
        BigDecimal total = l.totalPayable();
        assertThat(total).isEqualByComparingTo(l.calculateEmi().multiply(BigDecimal.valueOf(60)));
    }

    @Test
    void totalInterestIsPositive() {
        Loan l = loan("500000.00", "9.00", 60);
        BigDecimal interest = l.totalPayable().subtract(l.getPrincipalAmount());
        assertThat(interest).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void emiMatchesReferenceValues() {
        // reference values computed independently with the same formula
        assertThat(loan("450000.00", "9.10", 60).calculateEmi())      // Priya's car loan
                .isEqualByComparingTo("9363.11");
        assertThat(loan("1500000.00", "8.40", 240).calculateEmi())    // Arjun's home loan
                .isEqualByComparingTo("12922.57");
    }
}
