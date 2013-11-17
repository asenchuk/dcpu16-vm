package net.taviscaron.dcpu16vm.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.machine.Processor.State;

/**
 * Different debug utils
 * @author Andrei Senchuk
 */
public class DebugUtils {
    public static void printProcessorStateDump(State state, OutputStream os) {
        PrintStream out = new PrintStream(os);
        out.print("+--------------------------------------------+\n");
        out.printf("| PC:  %04X   SP: %04X   IA: %04X   EX: %04X |\n", state.pc, state.sp, state.ia, state.ex);
        out.printf("| A:   %04X   B : %04X   C:  %04X            |\n", state.readRegister(Processor.Register.A), state.readRegister(Processor.Register.B), state.readRegister(Processor.Register.C));
        out.printf("| X:   %04X   Y : %04X   Z:  %04X            |\n", state.readRegister(Processor.Register.X), state.readRegister(Processor.Register.Y), state.readRegister(Processor.Register.Z));
        out.printf("| I:   %04X   J : %04X                       |\n", state.readRegister(Processor.Register.I), state.readRegister(Processor.Register.J));
        out.print("+--------------------------------------------+\n");
        // here's no need to close output stream or print stream (which will close output stream for sure).
    }
    
    public static void pause(long miliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(miliseconds);
        } catch (InterruptedException e) {
            // sleep interruption commonly is not an issue
        }
    }
}
