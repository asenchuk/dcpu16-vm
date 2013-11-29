package net.taviscaron.dcpu16vm.test.processor;

import net.taviscaron.dcpu16vm.machine.MachineException;
import net.taviscaron.dcpu16vm.machine.Memory;
import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.test.DefaultMachineTest;
import net.taviscaron.dcpu16vm.util.ProgramUtils;
import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;

/**
 * Test software interrupts
 *
 * IAS interrupt_handler
 * SET A, 0x1234
 * INT 0xBABE
 *
 * :end
 * SET PC, end
 *
 * :interrupt_handler
 * SET B, 0xBEEF
 * SET C, A
 * RFI I
 *
 * @author Andrei Senchuk
 */
public class SoftwareInterruptsTest extends DefaultMachineTest {
    @Test
    public void testSoftwareInterrupts() throws MachineException, IOException {
        machine.getProcessor().attachDebugger(new Processor.Debugger() {
            @Override
            public void dumpState(Processor processor, Processor.State state, Memory memory) {
                if(state.pc == 0x0006) {
                    Assert.assertEquals((short)0x1234, state.readRegister(Processor.Register.A));
                    Assert.assertEquals((short)0xbeef, state.readRegister(Processor.Register.B));
                    Assert.assertEquals((short)0xbabe, state.readRegister(Processor.Register.C));
                    processor.stop();
                }
            }
        });

        machine.setProgram(ProgramUtils.loadFromFile("program/software_interrupts_test.dcpu16"));
        machine.start();
    }
}
