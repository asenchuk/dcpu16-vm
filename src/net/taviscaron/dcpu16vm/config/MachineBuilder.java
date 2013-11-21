package net.taviscaron.dcpu16vm.config;

import net.taviscaron.dcpu16vm.machine.Machine;

/**
 * Machine builder interface. It's used for instantiating machine
 * from config or based or some other configuration.
 * @author Andrei Senchuk
 */
public interface MachineBuilder {
    public Machine createMachine() throws InvalidConfigurationException;
}
