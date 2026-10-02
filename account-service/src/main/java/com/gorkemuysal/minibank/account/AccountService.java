package com.gorkemuysal.minibank.account;

import com.gorkemuysal.minibank.account.dto.AccountResponse;
import com.gorkemuysal.minibank.account.dto.CreateAccountRequest;
import com.gorkemuysal.minibank.account.dto.MoneyRequest;
import com.gorkemuysal.minibank.exception.AccountNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final IbanGenerator ibanGenerator;

    public AccountService(AccountRepository accountRepository, IbanGenerator ibanGenerator) {
        this.accountRepository = accountRepository;
        this.ibanGenerator = ibanGenerator;
    }

    /**
     * Handle the transactional creating account operations
     *
     * @params Gets a request object as a CreateAccountRequest object
     * @return AccountResponse object
     * */
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, String ownerId, String ownerName) {

        String accountNumber = ibanGenerator.generate();
        Account account = Account.open(accountNumber, ownerId, ownerName, request.currency());
        return AccountResponse.from(accountRepository.save(account));

    }

    /**
     * Handle find account with ID number
     *
     * @param id as a Long type
     * @return AccountResponse object
     * @throw If there is no account with entered id, throws ResourceNotFoundException
     * */
    @Transactional(readOnly = true)
    public AccountResponse getById(Long id, CurrentUser user) {
        return AccountResponse.from(loadAccessibleAccount(id, user));
    }

    // Handle deposit a balance with specific amount
    @Transactional
    public AccountResponse deposit(Long id, MoneyRequest request) {
        Account account = findOrThrow(id);
        account.deposit(request.amount(), request.currency());
        return AccountResponse.from(accountRepository.saveAndFlush(account));
    }

    // Handle withdraw a balance with specific amount
    @Transactional
    public AccountResponse withdraw(Long id, MoneyRequest request, CurrentUser user) {
        Account account = loadAccessibleAccount(id, user);
        account.withdraw(request.amount(), request.currency());
        return AccountResponse.from(accountRepository.saveAndFlush(account));
    }

    // Handle freeze the account if it is active
    @Transactional
    public AccountResponse freeze(Long id) {
        Account account = findOrThrow(id);
        account.freeze();
        return AccountResponse.from(accountRepository.saveAndFlush(account));
    }

    // Handle freeze the account if it is freeze
    @Transactional
    public AccountResponse active(Long id) {
        Account account = findOrThrow(id);
        account.active();
        return AccountResponse.from(accountRepository.saveAndFlush(account));
    }

    private Account findOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    // Handle load account according to current user
    // throws exception when account not found
    private Account loadAccessibleAccount(Long id, CurrentUser user) {
        Optional<Account> account = user.operator()
                ? accountRepository.findById(id)
                : accountRepository.findByIdAndOwnerId(id, user.id());
        return account.orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> listMine(CurrentUser user) {
        return accountRepository.findAllByOwnerId(user.id()).stream()
                .map(AccountResponse::from)
                .toList();
    }
}
