package com.budget.api.controller;

import com.budget.api.dto.MonthlySummaryResponse;
import com.budget.api.dto.TransactionResponse;
import com.budget.api.service.TransactionService;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public final class TransactionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int adjustedSize = Math.min(size, MAX_PAGE_SIZE);
        return ResponseEntity.ok(
            transactionService.getAllTransactions(PageRequest.of(page, adjustedSize)));
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
