package com.example.walletmicroservice.service;

import com.example.walletmicroservice.dto.UpdateCoaRequest;
import com.example.walletmicroservice.entity.ChartOfAccounts;
import com.example.walletmicroservice.repository.ChartOfAccountsRepository;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChartOfAccountsService {

    private final ChartOfAccountsRepository repo;

    public List<ChartOfAccounts> getAll() {
        return repo.findAll();
    }

    public ChartOfAccounts getById(String accountId) {
        return repo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("COA not found"));
    }

    public ChartOfAccounts update(String accountId, UpdateCoaRequest req) {
        ChartOfAccounts coa = getById(accountId);

        if (req.getDescription() != null)
            coa.setDescription(req.getDescription());

        if (req.getIsActive() != null)
            coa.setIsActive(req.getIsActive());

        return repo.save(coa);
    }
}
