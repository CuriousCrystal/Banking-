package com.amc.bfsi.repository;

import com.amc.bfsi.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByCustomerCustomerId(Long customerId);

    long countByCustomerCustomerId(Long customerId);

    @Query("select l.loanType, count(l) from Loan l group by l.loanType")
    List<Object[]> countByType();
}
