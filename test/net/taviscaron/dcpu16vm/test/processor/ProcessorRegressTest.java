package net.taviscaron.dcpu16vm.test.processor;

import net.taviscaron.dcpu16vm.machine.MachineException;
import net.taviscaron.dcpu16vm.machine.Memory;
import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.test.DefaultMachineTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Simple regression test.
 * @author Andrei Senchuk
 */
public class ProcessorRegressTest extends DefaultMachineTest {
    private static final short[] program = new short[] {
        (short)0x7fc1, (short)0x00ff, (short)0x0100, (short)0x8bc1, 
        (short)0x0101, (short)0x8c01, (short)0x03c1, (short)0x0102, 
        (short)0x7bc1, (short)0x0063, (short)0x0103, (short)0x7c01,
        (short)0x0061, (short)0x43c1, (short)0x0001, (short)0x0104,
        (short)0x9b01, (short)0x67c1, (short)0x0105, (short)0x9f01,
        (short)0x63c1, (short)0x0106, (short)0x87c1, (short)0x0107,
        (short)0xa3c2, (short)0x0107, (short)0xabc1, (short)0x0108,
        (short)0x8bc3, (short)0x0108, (short)0x93c1, (short)0x0109,
        (short)0x93c4, (short)0x0109, (short)0xffc1, (short)0x010a,
        (short)0x93c6, (short)0x010a, (short)0xdfc1, (short)0x010b,
        (short)0x8bcd, (short)0x010b, (short)0x9fc1, (short)0x010c,
        (short)0x8bcf, (short)0x010c, (short)0x7fc1, (short)0x002f,
        (short)0x010d, (short)0xbbca, (short)0x010d, (short)0x8fc1,
        (short)0x010e, (short)0xb7cb, (short)0x010e, (short)0x7fc1,
        (short)0x001f, (short)0x010f, (short)0xc7cc, (short)0x010f,
        (short)0x8bf2, (short)0x0001, (short)0xc7c1, (short)0x0110,
        (short)0x8ff3, (short)0x0001, (short)0xcbc1, (short)0x0111,
        (short)0x8bf4, (short)0x0002, (short)0xcfc1, (short)0x0112,
        (short)0x8bf0, (short)0x0003, (short)0xd3c1, (short)0x0113,
        (short)0x7c20, (short)0x0050, (short)0x7f81, (short)0x0053,
        (short)0xd7c1, (short)0x0114, (short)0x6381, (short)0x7801,
        (short)0x0101, (short)0x7821, (short)0x0102, (short)0x7841,
        (short)0x0103, (short)0x7861, (short)0x0104, (short)0x7881,
        (short)0x0105, (short)0x78a1, (short)0x0106, (short)0x7f81,
        (short)0x005f, (short)0x0000, (short)0x0004, (short)0x0003
    };
    
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
        
        machine.setProgram(program);
        machine.start();
    }
}
