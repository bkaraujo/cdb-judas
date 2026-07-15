package br.cdb.context.monetary;

import br.cdb.context.monetary._1_application.usecase.AccountUseCase;
import br.cdb.context.monetary._1_application.usecase.CostCenterUseCase;
import br.cdb.context.monetary._1_application.usecase.CreditCardUseCase;
import br.cdb.context.monetary._1_application.usecase.TransactionUseCase;
import br.commons.Registry;
import br.commons.annotation.Facade;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class MonetaryContext implements Facade {

    public static CreditCardUseCase ucCreditCard() {
        return Registry.tryGet(CreditCardUseCase.class);
    }

    public static AccountUseCase ucAccount() {
        return Registry.tryGet(AccountUseCase.class);
    }

    public static CostCenterUseCase ucCostCenter() {
        return Registry.tryGet(CostCenterUseCase.class);
    }

    public static TransactionUseCase ucTransaction() {
        return Registry.tryGet(TransactionUseCase.class);
    }

}
