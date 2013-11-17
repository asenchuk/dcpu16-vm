package net.taviscaron.dcpu16vm.machine.impl;

import net.taviscaron.dcpu16vm.machine.Machine;
import net.taviscaron.dcpu16vm.machine.Memory;
import net.taviscaron.dcpu16vm.machine.Processor;

/**
 * Machine implementation
 * @author Andrei Senchuk
 */
public class MachineImpl implements Machine {
    private Processor processor;
    private Memory memory;
    
    public MachineImpl(Processor processor, Memory memory) {
        this.processor = processor;
        this.memory = memory;
        init();        
    }
    
    private void init() {
        processor.setMemoryBus(new MemoryBus() {
            @Override
            public Memory memory() {
                return memory;
            }
        });
    }
    
    @Override
    public void loadProgram(short[] program) {
        memory.set((short)0, program);
    }

    @Override
    public void start() {
        processor.start();
    }
}
