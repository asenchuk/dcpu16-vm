package net.taviscaron.dcpu16vm;

import net.taviscaron.dcpu16vm.device.LEM1802;
import net.taviscaron.dcpu16vm.machine.Machine;
import net.taviscaron.dcpu16vm.machine.Processor.State;
import net.taviscaron.dcpu16vm.machine.device.Device;
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
    public static final short[] lem1802HelloWorldProgram = new short[] {
        (short)0x1a00, (short)0x88c3, (short)0x1a20, (short)0x7c32,
        (short)0x7349, (short)0x7c12, (short)0xf615, (short)0x7f81,
        (short)0x000b, (short)0x84d3, (short)0x8b81, (short)0x1bc1,
        (short)0x001e, (short)0x8401, (short)0x7c21, (short)0x8000,
        (short)0x7a40, (short)0x001e, (short)0x84c1, (short)0x58e1,
        (short)0x001f, (short)0x84f2, (short)0x7f81, (short)0x002d,
        (short)0x7ceb, (short)0xa000, (short)0x1ec1, (short)0x8000,
        (short)0x88c2, (short)0xd381, (short)0x0000, (short)0x0048,
        (short)0x0065, (short)0x006c, (short)0x006c, (short)0x006f,
        (short)0x002c, (short)0x0020, (short)0x0057, (short)0x006f,
        (short)0x0072, (short)0x006c, (short)0x0064, (short)0x0021,
        (short)0x0000, (short)0x7f81, (short)0x002d, (short)0x0000,
    };

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
        Device[] devices = new Device[] {
                new LEM1802()
        };

        Machine machine = new MachineImpl(processor, memory, devices);
        machine.loadProgram(lem1802HelloWorldProgram);
        machine.start();
    }
}
