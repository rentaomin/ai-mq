package com.rtm.mq.tool;

import com.rtm.mq.tool.cli.Main;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point supporting dual mode: CLI and REST API.
 *
 * <p>This application can run in two modes:</p>
 * <ul>
 *   <li><b>CLI Mode</b>: When command-line arguments are provided, delegates to {@link Main} CLI runner</li>
 *   <li><b>REST Mode</b>: When no arguments are provided, starts Spring Boot REST API server</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>
 * # Start REST API server (default port 8080)
 * java -jar mq-spec-tool.jar
 *
 * # Run as CLI tool
 * java -jar mq-spec-tool.jar generate -i spec.xlsx -o ./output
 * java -jar mq-spec-tool.jar validate -i ./output
 * java -jar mq-spec-tool.jar parse -i spec.xlsx -o ./parsed.json
 * </pre>
 *
 * @see Main
 * @see com.rtm.mq.tool.api.controller.SpecGenerationController
 */
@SpringBootApplication
public class Application {

    /**
     * Main entry point with dual-mode support.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            // CLI mode: delegate to existing CLI implementation
            Main cli = new Main();
            int exitCode = cli.run(args);
            System.exit(exitCode);
        } else {
            // REST mode: start Spring Boot application
            SpringApplication.run(Application.class, args);
        }
    }
}
