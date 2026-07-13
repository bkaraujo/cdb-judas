package br.cdb.context.monetary._1_application.service;

import br.cdb.context.monetary._0_domain.model.CreditCard;
import br.cdb.context.monetary._0_domain.repository.CardRepository;
import br.commons.Result;
import br.commons.business.BusinessError;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

    public List<CreditCard> findAll() {
        return cardRepository.findAll();
    }

    public List<CreditCard> findByAccount(UUID accountId) {
        return cardRepository.findAll().stream()
                .filter(c -> accountId.equals(c.accountId()))
                .toList();
    }

    public Result<CreditCard, BusinessError> findById(UUID id) {
        return cardRepository.findById(id)
                .<Result<CreditCard, BusinessError>>map(Result::success)
                .orElseGet(() -> Result.failure(new BusinessError.NotFound("CreditCard not found: " + id)));
    }

    public CreditCard save(CreditCard creditCard) {
        return cardRepository.save(creditCard);
    }

    public void deleteById(UUID id) {
        cardRepository.deleteById(id);
    }
}
