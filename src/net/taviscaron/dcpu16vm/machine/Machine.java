package net.taviscaron.dcpu16vm.machine;

/**
 * Machine interface
 * @author Andrei Senchuk
 */
public interface Machine {
    public interface MemoryBus {
        public Memory memory();
    }
    
    public void loadProgram(short[] program);
    public void start();
}
