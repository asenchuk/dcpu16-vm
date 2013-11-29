package net.taviscaron.dcpu16vm.test.processor;

import net.taviscaron.dcpu16vm.machine.MachineException;
import net.taviscaron.dcpu16vm.machine.Memory;
import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.test.DefaultMachineTest;
import net.taviscaron.dcpu16vm.util.ProgramUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Simple regression test.
 * @author Andrei Senchuk
 */
public class ProcessorRegressTest extends DefaultMachineTest {
    @Test
    public void baseRegressionTest() throws MachineException, IOException {
        machine.getProcessor().attachDebugger(new Processor.Debugger() {
            @Override
            public void dumpState(Processor processor, Processor.State state, Memory memory) {
                if(state.pc == 0x005f) {
                    Assert.assertEquals(1, state.readRegister(Processor.Register.A));
                    Assert.assertEquals(2, state.readRegister(Processor.Register.B));
                    Assert.assertEquals(3, state.readRegister(Processor.Register.C));
                    Assert.assertEquals(4, state.readRegister(Processor.Register.X));
                    Assert.assertEquals(5, state.readRegister(Processor.Register.Y));
                    Assert.assertEquals(6, state.readRegister(Processor.Register.Z));

                    Assert.assertEquals(0xff, memory.readWord((short)0x100));
                    for(int i = 1; i < 20; i++) {
                        Assert.assertEquals((short)i, memory.readWord((short)(0x100 + i)));
                    }

                    processor.stop();
                }
            }
        });

        machine.setProgram(ProgramUtils.loadFromFile("program/regress_test.dcpu16"));
        machine.start();
    }
}
