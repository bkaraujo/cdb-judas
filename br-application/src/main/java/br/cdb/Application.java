package br.cdb;

import br.cdb.core.Logging;
import br.commons.tools.Strings;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.val;
import org.jspecify.annotations.NullMarked;

@NullMarked
@QuarkusMain
public class Application implements QuarkusApplication {

    static void main(String[] args) {

        System.getenv().entrySet().stream()
                .filter(entry -> Strings.upper(entry.getKey()).startsWith("CDB_"))
                .forEach(entry -> System.setProperty(
                        Strings.lower(entry.getKey()).replace("_", "."),
                        entry.getValue()
                ));

        Logging.configure();

        Quarkus.run(Application.class, args);
    }

    @Override
    public int run(String... args) {
        Quarkus.waitForExit();
        return 0;
    }


}
