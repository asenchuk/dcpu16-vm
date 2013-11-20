package net.taviscaron.dcpu16vm.machine.impl;

import net.taviscaron.dcpu16vm.machine.Machine;
import net.taviscaron.dcpu16vm.machine.Memory;
import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.machine.device.Device;

/**
 * Machine implementation
 * @author Andrei Senchuk
 */
public class MachineImpl implements Machine {
    private Processor processor;
    private Memory memory;
    private Device[] devices;

    private final MemoryBus memoryBus = new MemoryBus() {
        @Override
        public Memory memory() {
            return memory;
        }
    };

    private final InterruptionBus interruptionBus = new InterruptionBus() {
        @Override
        public void interrupt(short code) {
            processor.interrupt(code);
        }
    };

    private final HardwareBus hardwareBus = new HardwareBus() {
        @Override
        public Device[] devices() {
            return devices;
        }

        @Override
        public Device device(short index) {
            if(index < 0 || index >= hardwareBus.devices().length) {
                throw new RuntimeException("Here is no device with index " + index);
            }
            return devices[index];
        }
    };

    public MachineImpl(Processor processor, Memory memory) {
        this.processor = processor;
        this.memory = memory;
        this.devices = new Device[0];
        init();        
    }

    public MachineImpl(Processor processor, Memory memory, Device[] devices) {
        this.processor = processor;
        this.memory = memory;
        this.devices = devices;
        init();
    }

    private void init() {
        processor.setMemoryBus(memoryBus);
        processor.setHardwareBus(hardwareBus);

        for(Device device : devices) {
            device.init();
            device.setInterruptionBus(interruptionBus);
            device.setMemoryBus(memoryBus);
        }
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
