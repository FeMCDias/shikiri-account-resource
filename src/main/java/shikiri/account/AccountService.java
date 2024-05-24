package shikiri.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;

import lombok.NonNull;
import shikiri.account.exceptions.AccountNotFoundException;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @CachePut(value = "accountCache", key = "#result.id")
    public Account create(Account in) {
        in.hash(calculateHash(in.password()));
        in.password(null);
        return accountRepository.save(new AccountModel(in)).to();
    }

    @Cacheable(value = "accountCache", key = "#id")
    public Account read(@NonNull String id) {
        return accountRepository.findById(id).map(AccountModel::to).orElse(null);
    }

    @CachePut(value = "accountCache", key = "#id")
    public Account update(@NonNull String id, Account in) {
        return accountRepository.findById(id)
            .map(existingAccountModel -> {
                existingAccountModel.name(in.name())
                                    .email(in.email())
                                    .hash(calculateHash(in.password()));
                return accountRepository.save(existingAccountModel).to();
            })
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    @Cacheable(value = "accountLoginCache", key = "#email")
    public Account login(String email, String password) {
        String hash = calculateHash(password);
        return accountRepository.findByEmailAndHash(email, hash).map(AccountModel::to).orElse(null);
    }

    private String calculateHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            byte[] encoded = Base64.getEncoder().encode(hash);
            return new String(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
}
