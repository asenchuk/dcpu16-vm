package net.taviscaron.dcpu16vm.machine;

import net.taviscaron.dcpu16vm.machine.device.Device;

/**
 * Machine interface
 * @author Andrei Senchuk
 */
public interface Machine {
    public interface MemoryBus {
        public Memory memory();
    }

    public interface InterruptionBus {
        public void interrupt(short code);
    }

    public interface HardwareBus {
        public Device[] devices();
        public Device device(short index);
    }

    public void loadProgram(short[] program);
    public void start();
}
