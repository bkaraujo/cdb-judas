package br.cdb.context.monetary._1_application.service;

import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.context.monetary._0_domain.repository.CreditCardRepository;
import br.commons.Registry;
import br.commons.Result;
import br.commons.business.BusinessError;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
public class CreditCardService {

    private final CreditCardRepository creditCardRepository = Registry.get(CreditCardRepository.class);

    public List<CreditCard> findAll() {
        return creditCardRepository.findAll();
    }

    public List<CreditCard> findByAccount(UUID accountId) {
        return creditCardRepository.findAll().stream()
                .filter(c -> accountId.equals(c.accountId()))
                .toList();
    }

    public Result<CreditCard, BusinessError> findById(UUID id) {
        return creditCardRepository.findById(id)
                .<Result<CreditCard, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("CreditCard not found: " + id)));
    }

    public CreditCard save(CreditCard creditCard) {
        return creditCardRepository.save(creditCard);
    }

    public void deleteById(UUID id) {
        creditCardRepository.deleteById(id);
    }
}
