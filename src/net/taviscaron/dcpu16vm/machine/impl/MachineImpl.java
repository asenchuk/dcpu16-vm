package net.taviscaron.dcpu16vm.machine.impl;

import net.taviscaron.dcpu16vm.machine.Machine;
import net.taviscaron.dcpu16vm.machine.MachineException;
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
    private short[] program;

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

    @Override
    public void setMemory(Memory memory) {
        this.memory = memory;
    }

    @Override
    public Memory getMemory() {
        return memory;
    }

    @Override
    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    @Override
    public Processor getProcessor() {
        return processor;
    }

    @Override
    public void setDevices(Device[] devices) {
        this.devices = devices;
    }

    @Override
    public Device[] getDevices() {
        return devices;
    }

    @Override
    public Device getDevice(int index) {
        return devices[index];
    }

    @Override
    public void setProgram(short[] program) {
        this.program = program;
    }

    @Override
    public short[] getProgram() {
        return program;
    }

    @Override
    public void start() throws MachineException {
        if(processor == null) {
            throw new RuntimeException("Machine is missing processor");
        }

        if(memory == null) {
            throw new RuntimeException("Machine is missing memory");
        }

        // init processor->memory/hw communication bus
        processor.setMemoryBus(memoryBus);
        processor.setHardwareBus(hardwareBus);

        // init devices
        if(devices == null) {
            devices = new Device[0];
        }

        for(Device device : devices) {
            device.init();
            device.setInterruptionBus(interruptionBus);
            device.setMemoryBus(memoryBus);
        }

        // load program
        memory.set((short)0, program);

        try {
            processor.start();
        } catch(Throwable th) {
            // shutdown devices
            for(Device device : devices) {
                device.shutdown();
            }

            // re-throw exception
            throw new MachineException(th);
        }
    }
}
