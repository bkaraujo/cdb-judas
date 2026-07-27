package br.cdb;

import br.cdb.core.Logging;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jspecify.annotations.NullMarked;

@NullMarked
@QuarkusMain
public class Application implements QuarkusApplication {

    static void main(String[] args) {
        Logging.configure();
        Quarkus.run(Application.class, args);
    }

    @Override
    public int run(String... args) {
        Quarkus.waitForExit();
        return 0;
    }


}
