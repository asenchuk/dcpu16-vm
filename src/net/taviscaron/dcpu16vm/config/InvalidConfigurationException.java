package net.taviscaron.dcpu16vm.config;

/**
 * Exception raised on invalid machine configuration
 * @author Andrei Senchuk
 */
public class InvalidConfigurationException extends Exception {
    public InvalidConfigurationException(String msg) {
        super(msg);
    }

    public InvalidConfigurationException(String msg, Exception e) {
        super(msg, e);
    }
}
