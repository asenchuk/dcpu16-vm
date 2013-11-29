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
 * Test signed & unsigned operations
 *
 * SET A, 65531    ; -5 using two's complement
 * SET B, 7
 *
 * MLI B, A        ; B should now contain -35 (0xFFDD)
 * SET X, B        ; Store in X
 *
 * DVI B, 2        ; B should now contain -17 (rounded) (0xFFEF)
 * SET Y, B        ; Store in Y
 *
 * MDI B, 4        ; B should still contain -1
 * SET B, Y        ; B should contain -17
 * MDI B, -15      ; B should contain -2 (0xFFFE)
 * SET Z, B        ; Store in Z
 *
 * :end
 * SET PC, end
 *
 * @author Andrei Senchuk
 */
public class IntegerOperationsTest extends DefaultMachineTest {
    @Test
    public void testIntegerOperations() throws MachineException, IOException {
        machine.getProcessor().attachDebugger(new Processor.Debugger() {
            private int count = 0;

            @Override
            public void dumpState(Processor processor, Processor.State state, Memory memory) {
                switch(count++) {
                    case 0:
                        Assert.assertEquals(-5, state.readRegister(Processor.Register.A));
                        break;
                    case 1:
                        Assert.assertEquals(7, state.readRegister(Processor.Register.B));
                        break;
                    case 2:
                        Assert.assertEquals(-35, state.readRegister(Processor.Register.B));
                        break;
                    case 3:
                        Assert.assertEquals(-35, state.readRegister(Processor.Register.X));
                        break;
                    case 4:
                        Assert.assertEquals(-17, state.readRegister(Processor.Register.B));
                        break;
                    case 5:
                        Assert.assertEquals(-17, state.readRegister(Processor.Register.Y));
                        break;
                    case 6:
                        Assert.assertEquals(-1, state.readRegister(Processor.Register.B));
                        break;
                    case 7:
                        Assert.assertEquals(-17, state.readRegister(Processor.Register.B));
                        break;
                    case 8:
                        Assert.assertEquals(-2, state.readRegister(Processor.Register.B));
                        break;
                    case 9:
                        Assert.assertEquals(-2, state.readRegister(Processor.Register.Z));
                        break;
                    case 10:
                        processor.stop();
                        break;
                    default:
                        break;

                }
            }
        });

        machine.setProgram(ProgramUtils.loadFromFile("program/signed_operations_test.dcpu16"));
        machine.start();
    }
}
