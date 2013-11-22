package net.taviscaron.dcpu16vm.machine;

/**
 * Machine runtime exception
 */
public class MachineException extends Exception {
    public MachineException(Throwable e) {
        super(e);
    }
}
