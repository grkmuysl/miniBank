package com.gorkemuysal.minibank.account;

import com.gorkemuysal.minibank.account.dto.AccountResponse;
import com.gorkemuysal.minibank.account.dto.CreateAccountRequest;
import com.gorkemuysal.minibank.account.dto.MoneyRequest;
import com.gorkemuysal.minibank.exception.AccountNotFoundException;
import com.gorkemuysal.minibank.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

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
    public AccountResponse createAccount(CreateAccountRequest request){

        // Generate account number with ibanGenerator
        String accountNumber = ibanGenerator.generate();

        // create account
        Account account = Account.open(accountNumber, request.ownerName() , request.currency());

        Account saved = accountRepository.save(account);

        return AccountResponse.from(saved);
    }

    /**
     * Handle find account with ID number
     *
     * @param id as a Long type
     * @return AccountResponse object
     * @throw If there is no account with entered id, throws ResourceNotFoundException
     * */
    @Transactional(readOnly = true)
    public AccountResponse getById(Long id){
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Account couldnt find: " + id));
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
    public AccountResponse withdraw(Long id, MoneyRequest request) {
        Account account = findOrThrow(id);
        account.withdraw(request.amount(), request.currency());
        return AccountResponse.from(accountRepository.saveAndFlush(account));
    }

    private Account findOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
