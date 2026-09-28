package com.gorkemuysal.minibank.account;

import com.gorkemuysal.minibank.account.dto.AccountResponse;
import com.gorkemuysal.minibank.account.dto.CreateAccountRequest;
import com.gorkemuysal.minibank.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
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
     * @param Gets a request object as a CreateAccountRequest object
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
     * @throws If there is no account with entered id, throws ResourceNotFoundException
     * */
    public AccountResponse getById(Long id){
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Account couldnt find: " + id));
    }
}
