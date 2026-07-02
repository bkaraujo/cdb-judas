package br.community.context.monetary._1_application.service;

import br.commons.Result;
import br.community.context.monetary._0_domain.model.Card;
import br.community.context.monetary._0_domain.repository.CardRepository;
import br.community.context.shared._0_domain.model.DomainError;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.UUID;

@NullMarked
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

    public List<Card> findAll() {
        return cardRepository.findAll();
    }

    public List<Card> findByAccount(UUID accountId) {
        return cardRepository.findAll().stream()
                .filter(c -> accountId.equals(c.accountId()))
                .toList();
    }

    public Result<Card, DomainError> findById(UUID id) {
        return cardRepository.findById(id)
                .<Result<Card, DomainError>>map(Result::success)
                .orElseGet(() -> Result.failure(new DomainError.NotFound("Card not found: " + id)));
    }

    public Card save(Card card) {
        return cardRepository.save(card);
    }

    public void deleteById(UUID id) {
        cardRepository.deleteById(id);
    }
}
