package com.budget.api.controller;

import com.budget.api.dto.MonthlySummaryResponse;
import com.budget.api.dto.TransactionResponse;
import com.budget.api.service.TransactionService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public final class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance() {
        return ResponseEntity.ok(
            Map.of("balance", transactionService.getCurrentBalance()));
    }

    @GetMapping("/summary/{year}/{month}")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(
            transactionService.getMonthlySummary(month, year));
    }
}
