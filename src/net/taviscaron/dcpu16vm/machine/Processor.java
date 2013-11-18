package net.taviscaron.dcpu16vm.machine;

import java.util.Arrays;

/**
 * Processor interface
 * @author Andrei Senchuk
 */
public abstract class Processor {
    /** DCPU-16 has 8 registers */
    public enum Register {
        A, B, C, X, Y, Z, I, J
    }
    
    /** Processor debugger interface */
    public interface Debugger {
        public void dumpState(State state, Memory memory);
    }
    
    /** DCPU-16 processor state */
    public class State {
        /** common registers */
        public short[] registers = new short[8];

        /** program counter */
        public short pc;

        /** stack pointer */
        public short sp;

        /** extra/excess */
        public short ex;

        /** interrupt address */
        public short ia;
        
        public void reset() {
            pc = 0;
            sp = 0;
            ex = 0;
            ia = 0;
            
            Arrays.fill(registers, (short)0);
        }
        
        public short readRegister(Register register) {
            return registers[register.ordinal()];
        }
        
        public void writeRegister(Register register, short value) {
            registers[register.ordinal()] = value;
        }
        
        public void incRegister(Register register) {
            registers[register.ordinal()]++;
        }
        
        public void decRegister(Register register) {
            registers[register.ordinal()]--;
        }
    }
    
    protected State state = new State();
    protected Machine.MemoryBus memoryBus;
    protected Machine.HardwareBus hardwareBus;
    protected Debugger debugger;
    
    /** Setup processor->memory communication bus */
    public void setMemoryBus(Machine.MemoryBus memoryBus) {
        this.memoryBus = memoryBus;
    }

    /** Setup processor->devices communication bus */
    public void setHardwareBus(Machine.HardwareBus hardwareBus) {
        this.hardwareBus = hardwareBus;
    }
    
    /** Attach debugger to the processor */
    public void attachDebugger(Debugger debugger) {
        this.debugger = debugger;
    }
    
    public void detachDebugger() {
        this.debugger = null;
    }
    
    protected void dumpState() {
        if(debugger != null) {
            debugger.dumpState(state, memoryBus.memory());
        }
    }
    
    /** Start processor working */
    public abstract void start();
}
