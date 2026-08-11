package br.cdb.feature.f003._1_application.service;

import br.cdb.feature.f003._0_domain.model.CreditCard;
import br.cdb.feature.f003._0_domain.repository.CreditCardRepository;
import br.commons.Result;
import br.commons.business.BusinessError;
import br.commons.framework.cdi.Context;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
public class CreditCardService {

    private final CreditCardRepository repository = Context.get(CreditCardRepository.class);

    public List<CreditCard> findAll() {
        return repository.findAll();
    }

    public List<CreditCard> findByAccount(UUID accountId) {
        return repository.findAll().stream()
                .filter(c -> accountId.equals(c.accountId()))
                .toList();
    }

    public List<CreditCard> findAllByPerson(String personId) {
        return repository.findAllByPerson(personId);
    }

    public List<CreditCard> findByAccountAndPerson(UUID accountId, String personId) {
        return repository.findByAccountAndPerson(accountId, personId);
    }

    public Result<CreditCard, BusinessError> findById(UUID id) {
        return repository.findById(id)
                .<Result<CreditCard, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("CreditCard not found: %s", id)));
    }

    public CreditCard save(CreditCard creditCard) {
        return repository.save(creditCard);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
