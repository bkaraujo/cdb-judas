package br.community;

import br.commons.Logger;
import br.commons.RT;
import br.commons.framework.logger.LogLevel;
import br.commons.framework.logger.channel.ConsoleChannel;
import br.community.context.monetary.MonetaryModule;
import br.community.context.security.SecurityModule;
import br.community.context.shared.SharedModule;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;

import java.util.List;

@NullMarked
@SpringBootApplication(scanBasePackages = {"br.community"})
@ConfigurationPropertiesScan
@Import({SharedModule.class, SecurityModule.class, MonetaryModule.class})
public class Application {

    static void main(String[] args) {
        RT.packages.addAll(List.of(
                "br.commons.framework.logger.",
                "br.commons.Logger",
                "br.commons.tools.Meta"
        ));

        Logger.channel(new ConsoleChannel());
        Logger.level(LogLevel.VERBOSE);
        Logger.level("br", LogLevel.VERBOSE);
        Logger.level("_org", LogLevel.ERROR);
        Logger.level("org", LogLevel.ERROR);
        Logger.level("com", LogLevel.ERROR);
        Logger.level("io", LogLevel.ERROR);

        SpringApplication.run(Application.class, args);
    }

}
