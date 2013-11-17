package net.taviscaron.dcpu16vm;

import net.taviscaron.dcpu16vm.machine.Machine;
import net.taviscaron.dcpu16vm.machine.Processor.State;
import net.taviscaron.dcpu16vm.machine.impl.ProcessorImpl;
import net.taviscaron.dcpu16vm.machine.impl.MachineImpl;
import net.taviscaron.dcpu16vm.machine.Memory;
import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.machine.impl.MemoryImpl;
import net.taviscaron.dcpu16vm.util.DebugUtils;

/**
 * dcpu16 virtual machine entry point
 * @author Andrei Senchuk
 */
public class Main {
    public static final short[] program = new short[] {
        (short)0x7c01, (short)0x0030, (short)0x7fc1, (short)0x0020,
        (short)0x1000, (short)0x7803, (short)0x1000, (short)0xc413,
        (short)0x7f81, (short)0x0019, (short)0xacc1, (short)0x7c01,
        (short)0x2000, (short)0x22c1, (short)0x2000, (short)0x88c3,
        (short)0x84d3, (short)0xbb81, (short)0x9461, (short)0x7c20, 
        (short)0x0017, (short)0x7f81, (short)0x0019, (short)0x946f,
        (short)0x6381, (short)0xeb81, (short)0x0000
    };
    
    public static void main(String[] args) {
        Processor processor = new ProcessorImpl();
        processor.attachDebugger(new Processor.Debugger() {
            @Override
            public void dumpState(State state, Memory memory) {
                DebugUtils.printProcessorStateDump(state, System.out);
                DebugUtils.pause(100);
            }
        });
        
        Memory memory = new MemoryImpl();
        
        Machine machine = new MachineImpl(processor, memory);
        machine.loadProgram(program);
        machine.start();
    }
}
