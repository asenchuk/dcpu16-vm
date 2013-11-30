package net.taviscaron.dcpu16vm.test.processor;

import net.taviscaron.dcpu16vm.machine.MachineException;
import net.taviscaron.dcpu16vm.machine.Memory;
import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.machine.Processor.Debugger;
import net.taviscaron.dcpu16vm.machine.Processor.State;
import net.taviscaron.dcpu16vm.machine.device.Device;
import net.taviscaron.dcpu16vm.test.DefaultMachineTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test hw interrupts
 * 
 * ; expected result:
 * ; A = 0xb00b
 * ; B = 0x1234 (from the device impl below)
 * ; C = 0xabcd (from the device impl as well)
 * ; IA = 0x000d
 * ; end on 0x000b
 * 
 * IAS interrupt_handler
 * SET A, 0xb00b
 * HWI 0
 * 
 * SET I, 10
 * :loop
 *    SUB I, 1
 *    IFG I, 0
 *         SET PC, end
 * 
 * :end
 *     SET PC, end
 * 
 * :interrupt_handler
 * SET B, A
 * RFI I
 * 
 * @author Andrei Senchuk
 */
public class HardwareInterruptsTest extends DefaultMachineTest {
    private static final short[] program = new short[] {
        (short)0x7d40, (short)0x000d, (short)0x7c01, (short)0xb00b,
        (short)0x7e40, (short)0x0000, (short)0xacc1, (short)0x88c3,
        (short)0x84d4, (short)0x7f81, (short)0x000b, (short)0x7f81,
        (short)0x000b, (short)0x0021, (short)0x1960, (short)0x0000
    };
    
    @Test
    public void testHwInterrupts() throws MachineException {
        machine.setDevices(new Device[] {
            new Device() {
                @Override
                public void interrupt(State state) {
                    state.writeRegister(Processor.Register.C, (short)0xabcd);
                    interruptionBus.interrupt((short)0x1234);
                }
            }
        });
        
        machine.getProcessor().attachDebugger(new Debugger() {
            @Override
            public void dumpState(Processor processor, State state, Memory memory) {
                if(state.pc == 0x000b) {
                    Assert.assertEquals((short)0x000d, state.ia);
                    Assert.assertEquals((short)0xb00b, state.readRegister(Processor.Register.A));
                    Assert.assertEquals((short)0x1234, state.readRegister(Processor.Register.B));
                    Assert.assertEquals((short)0xabcd, state.readRegister(Processor.Register.C));
                    processor.stop();
                }
            }
        });
        
        machine.setProgram(program);
        machine.start();
    }
}
