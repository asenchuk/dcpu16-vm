package net.taviscaron.dcpu16vm.test.processor;

import net.taviscaron.dcpu16vm.machine.MachineException;
import net.taviscaron.dcpu16vm.machine.Memory;
import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.test.DefaultMachineTest;
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
    private static final short[] program = new short[] {
        (short)0x7d40, (short)0x0008, (short)0x7c01, (short)0x1234,
        (short)0x7d00, (short)0xbabe, (short)0x7f81, (short)0x0006,
        (short)0x7c21, (short)0xbeef, (short)0x0041, (short)0x1960
    };
    
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

        machine.setProgram(program);
        machine.start();
    }
}
