package com.example.walletmicroservice.controller;

import com.example.walletmicroservice.dto.UpdateCoaRequest;
import com.example.walletmicroservice.entity.ChartOfAccounts;
import com.example.walletmicroservice.service.ChartOfAccountsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coa/")
@RequiredArgsConstructor
@Slf4j
public class ChartAcController {

    private final ChartOfAccountsService service;

    @GetMapping
    public List<ChartOfAccounts> getAll() {
        return service.getAll();
    }

    @GetMapping("/{accountId}")
    public ChartOfAccounts getById(@PathVariable String accountId) {
        return service.getById(accountId);
    }

    @PutMapping("/{accountId}")
    public ChartOfAccounts update(
            @PathVariable String accountId,
            @RequestBody UpdateCoaRequest request
    ) {
        return service.update(accountId, request);
    }
}