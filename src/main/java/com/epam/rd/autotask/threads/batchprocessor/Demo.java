package com.epam.rd.autotask.threads.batchprocessor;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class Demo {

    static String msg1 = "The <root> element configures the root logger. " +
            "  It supports a single attribute, namely the level attribute. " +
            "  It does not allow any other attributes because the additivity " +
            "flag does not apply to the root logger. \n" +
            "  Moreover, since the root logger is already named as \"ROOT\", " +
            "it does not allow a name attribute either. " +
            "The value of the level attribute can be one of the case-insensitive " +
            "strings TRACE, DEBUG, INFO, WARN, ERROR, ALL or OFF.\n" +
            "Note that the level of the root logger cannot be set to INHERITED or NULL. ";
    static String msg2 = "Logback-classic will automatically ask the web-server " +
            "to install a LogbackServletContainerInitializer " +
            "(https://logback.qos.ch/apidocs/ch/qos/logback/classic/serv" +
            "let/LogbackServletContainerInitializer.html) implementing the " +
            "ServletContainerInitializer interface (available in servlet-api 3.x and later). \n" +
            "  This initializer will in turn install and instance of LogbackServletContextListener " +
            "(http://logback.qos.ch/apidocs/ch/qos/logback/classic/servlet/Logback" +
            "ServletContextListener.html) . This listener will stop the current " +
            "logback-classic context when the web-app is stopped or reloaded.";

    public static UnaryOperator<String> trimSpaces =
            s -> s.replaceAll("\\s+", " ").trim();
    public static UnaryOperator<String> removeLinks =
            s -> s.replaceAll("https?://\\S+|www\\.\\S+", "");
    public static UnaryOperator<String> toLower = String::toLowerCase;
    public static UnaryOperator<String> removePunctuation =
            s -> s.replaceAll("\\p{Punct}", " ");

    public static void main(String[] args) throws InterruptedException {
        List<String> messages = List.of(msg1, msg2, "");
        MessagePreprocessor conveyor = new MessagePreprocessor(messages,
                List.of(removeLinks, toLower, removePunctuation, trimSpaces));
        System.out.printf("Initial state:%n%s%n", conveyor.getState());
        conveyor.start();
        while (conveyor.getResult().equals(Optional.empty())) {
            Thread.sleep(1);
        }
        System.out.printf("Finished state:%n%s%n", conveyor.getState());
        System.out.printf("Processed messages:%n%s%n", conveyor.getResult().get());
    }
}
