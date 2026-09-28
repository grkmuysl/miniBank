package com.gorkemuysal.minibank.account;

import com.gorkemuysal.minibank.account.dto.AccountResponse;
import com.gorkemuysal.minibank.account.dto.CreateAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService){
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request){
        AccountResponse response = accountService.createAccount(request);
        URI location = URI.create("/api/v1/accounts/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getById(id));
    }
}
