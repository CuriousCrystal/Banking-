package com.amc.bfsi.service;

import com.amc.bfsi.dto.SummaryResponse;
import com.amc.bfsi.repository.AccountRepository;
import com.amc.bfsi.repository.CustomerRepository;
import com.amc.bfsi.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;

    public ReportService(CustomerRepository customerRepository,
                         AccountRepository accountRepository,
                         LoanRepository loanRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.loanRepository = loanRepository;
    }

    public SummaryResponse summary() {
        return new SummaryResponse(
                customerRepository.count(),
                accountRepository.count(),
                loanRepository.count(),
                Math.round(accountRepository.totalDeposits() * 100) / 100.0,
                pairs(loanRepository.countByType()),
                pairs(accountRepository.depositsByCity()));
    }

    private List<Map<String, Object>> pairs(List<Object[]> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", row[0] == null ? "Unknown" : row[0].toString());
            m.put("value", row[1]);
            out.add(m);
        }
        return out;
    }
}
