package net.taviscaron.dcpu16vm.test;

import net.taviscaron.dcpu16vm.machine.Machine;
import net.taviscaron.dcpu16vm.machine.impl.MachineImpl;
import net.taviscaron.dcpu16vm.machine.impl.MemoryImpl;
import net.taviscaron.dcpu16vm.machine.impl.ProcessorImpl;
import org.junit.Before;

/**
 * Perform tests on default machine
 * @author Andrei Senchuk
 */
public class DefaultMachineTest {
    protected Machine machine;

    @Before
    public void setUp() {
        machine = new MachineImpl();
        machine.setProcessor(new ProcessorImpl());
        machine.setMemory(new MemoryImpl());
    }
}
