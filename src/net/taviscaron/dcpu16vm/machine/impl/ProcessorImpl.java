package net.taviscaron.dcpu16vm.machine.impl;

import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.machine.device.Device;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Processor implementation
 * @author Andrei Senchuk
 */
public class ProcessorImpl extends Processor {
    /** Processor operation interface. */
    private interface Operation {
        public void perform(short opcode, short aCode, Value a, short bCode, Value b);
    }
    
    /** Conditional operation base class. Sets skip bit on falce condition. */
    private abstract class ConditionalOperation implements Operation {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            if(!condition(a, b)) {
                state.skipping = true;
            }
        }
        
        public abstract boolean condition(Value a, Value b);
    }
    
    /** Special operation base class. Passes needed args to the perform method. */
    private abstract class SpecialOperation implements Operation {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            perform(opcode, bCode, aCode, a);
        }
        
        public abstract void perform(short opcode, short specialOpcode, short aCode, Value a);
    }
    
    /** Operation arg value interface */
    private interface Value {
        public short get();
        public void set(short value);
    }
    
    private class RegisterValue implements Value {
        private short index;
        
        public RegisterValue(short index) {
            this.index = index;
        }

        @Override
        public short get() {
            return state.registers[index];
        }

        @Override
        public void set(short value) {
            state.registers[index] = value;
        }
    }
    
    /** [position] get/set */
    private class MemoryValue implements Value {
        private short pos;
        
        public MemoryValue(short pos) {
            this.pos = pos;
        }
        
        @Override
        public short get() {
            return memoryBus.memory().readWord(pos);
        }

        @Override
        public void set(short value) {
            memoryBus.memory().writeWord(pos, value);
        }
    }
    
    /** [register + next word] get/set */
    private class RegisterAddressValue extends MemoryValue {
        public RegisterAddressValue(short index, boolean withOffset) {            
            super((short)(state.registers[index] + ((withOffset) ? nextWord() : 0)));
        }
    }
    
    /** literal get/set */
    private class LiteralValue implements Value {
        private short value;
        
        public LiteralValue(short value) {
            this.value = value;
        }
        
        @Override
        public short get() {
            return value;
        }

        @Override
        public void set(short value) {
            // attempting to write to a literal value fails silently
        }
    }

    /** push value into the stack */
    private final Value pushValue = new Value() {
        @Override
        public short get() {
            return 0;
        }

        @Override
        public void set(short value) {
            memoryBus.memory().writeWord(--state.sp, value);
        }
    };

    /** pop value from the stack */
    private final Value popValue = new Value() {
        @Override
        public short get() {
            return memoryBus.memory().readWord(state.sp++);
        }

        @Override
        public void set(short value) {
            // nothing on pop
        }
    };

    /** SP */
    private final Value spValue = new Value() {
        @Override
        public short get() {
            return state.sp;
        }

        @Override
        public void set(short value) {
            state.sp = value;
        }
    };
    
    /** PC */
    private final Value pcValue = new Value() {
        @Override
        public short get() {
            return state.pc;
        }

        @Override
        public void set(short value) {
            state.pc = value;
        }
    };
    
    /** EX */
    private final Value exValue = new Value() {
        @Override
        public short get() {
            return state.ex;
        }

        @Override
        public void set(short value) {
            state.ex = value;
        }
    };
    
    /** SET b, a | sets b to a */
    private final Operation setOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            b.set(a.get());
        }
    };
    
    /** ADD b, a | sets b to b+a, sets EX to 0x0001 if there's an overflow, 0x0 otherwise */
    private final Operation addOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            int newB = b.get() + a.get();
            b.set((short)newB);
            state.ex = (short)((newB != b.get()) ? 0x1 : 0x0);
        }
    };
    
    /** SUB b, a | sets b to b-a, sets EX to 0xffff if there's an underflow, 0x0 otherwise */
    private final Operation subOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            int newB = b.get() - a.get();
            b.set((short)newB);
            state.ex = (short)((newB != b.get()) ? 0xffff : 0x0);
        }
    };
    
    /** MUL b, a | sets b to b*a, sets EX to ((b*a)>>16)&0xffff (treats b, a as unsigned) */
    private final Operation mulOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            int result = (a.get() & 0xffff) * (b.get() & 0xffff);
            b.set((short)result);
            state.ex = (short)((result >> 16) & 0xffff);
        }
    };
    
    /** MLI b, a | like MUL, but treat b, a as signed */
    private final Operation mliOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            int result = a.get() * b.get();
            b.set((short)result);
            state.ex = (short)((result >> 16) & 0xffff);
        }
    };
    
    /** DIV b, a | sets b to b/a, sets EX to ((b<<16)/a)&0xffff. if a==0, sets b and EX to 0 instead. (treats b, a as unsigned) */
    private final Operation divOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            if(a.get() == 0) {
                b.set((short)0);
                state.ex = 0;
            } else {
                int aVal = a.get() & 0xffff;
                int bVal = b.get() & 0xffff;
                b.set((short)(bVal / aVal));
                state.ex = (short)(((bVal << 16) / aVal) & 0xffff);
            }
        }
    };
    
    /** DVI b, a | like DIV, but treat b, a as signed. Rounds towards 0 */
    private final Operation dviOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            if(a.get() == 0) {
                b.set((short)0);
                state.ex = 0;
            } else {
                int aVal = a.get();
                int bVal = b.get();
                b.set((short)(bVal / aVal));
                state.ex = (short)(((bVal << 16) / aVal) & 0xffff);
            }
        }
    };
    
    /** MOD b, a | sets b to b%a. if a==0, sets b to 0 instead. */
    private final Operation modOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            if(a.get() == 0) {
                b.set((short)0);
            } else {
                b.set((short)((b.get() & 0xffff) % (a.get() & 0xffff)));
            }
        }
    };
    
    /** AND b, a | sets b to b&a */
    private final Operation andOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            b.set((short)(b.get() & a.get()));
        }
    };
    
    /** BOR b, a | sets b to b|a */
    private final Operation borOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            b.set((short)(b.get() | a.get()));
        }
    };
    
    /** XOR b, a | sets b to b^a */
    private final Operation xorOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            b.set((short)(b.get() ^ a.get()));
        }
    };
    
    /** MDI b, a | like MOD, but treat b, a as signed. (MDI -7, 16 == -7) */
    private final Operation mdiOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            if(a.get() == 0) {
                b.set((short)0);
            } else {
                b.set((short)(b.get() % a.get()));
            }
        }
    };
    
    /** SHR b, a | sets b to b>>>a, sets EX to ((b<<16)>>a)&0xffff (logical shift) */
    private final Operation shrOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            int aVal = a.get() & 0xffff;
            int bVal = b.get() & 0xffff;
            b.set((short)(bVal >>> aVal));
            state.ex = (short)(((bVal << 16) >> aVal) & 0xffff);
        }
    };
    
    /** ASR b, a | sets b to b>>a, sets EX to ((b<<16)>>>a)&0xffff (arithmetic shift) (treats b as signed) */
    private final Operation asrOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            int aVal = a.get() & 0xffff;
            int bVal = b.get();
            b.set((short)(bVal >> aVal));
            state.ex = (short)(((bVal << 16) >> aVal) & 0xffff);
        }
    };
    
    /** SHL b, a | sets b to b<<a, sets EX to ((b<<a)>>16)&0xffff */
    private final Operation shlOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            int aVal = a.get() & 0xffff;
            int bVal = b.get();
            b.set((short)(bVal << aVal));
            state.ex = (short)(((bVal << aVal) >> 16) & 0xffff);
        }
    };
    
    /** IFB b, a | performs next instruction only if (b&a)!=0 */
    private final Operation ifbOp = new ConditionalOperation() {
        @Override
        public boolean condition(Value a, Value b) {
            return (b.get() & a.get()) != 0;
        }
    };
    
    /** IFC b, a | performs next instruction only if (b&a)==0 */
    private final Operation ifcOp = new ConditionalOperation() {
        @Override
        public boolean condition(Value a, Value b) {
            return (b.get() & a.get()) == 0;
        }
    };

    /** IFE b, a | performs next instruction only if b==a */
    private final Operation ifeOp = new ConditionalOperation() {
        @Override
        public boolean condition(Value a, Value b) {
            return b.get() == a.get();
        }
    };
    
    /** IFN b, a | performs next instruction only if b!=a */
    private final Operation ifnOp = new ConditionalOperation() {
        @Override
        public boolean condition(Value a, Value b) {
            return b.get() != a.get();
        }
    };
    
    /** IFG b, a | performs next instruction only if b>a */
    private final Operation ifgOp = new ConditionalOperation() {
        @Override
        public boolean condition(Value a, Value b) {
            return (b.get() & 0xffff) > (a.get() & 0xffff);
        }
    };
    
    /** IFA b, a | performs next instruction only if b>a (signed) */
    private final Operation ifaOp = new ConditionalOperation() {
        @Override
        public boolean condition(Value a, Value b) {
            return (b.get() > a.get());
        }
    };
    
    /** IFL b, a | performs next instruction only if b<a */
    private final Operation iflOp = new ConditionalOperation() {
        @Override
        public boolean condition(Value a, Value b) {
            return (b.get() & 0xffff) < (a.get() & 0xffff);
        }
    };
    
    /** IFU b, a | performs next instruction only if b<a (signed) */
    private final Operation ifuOp = new ConditionalOperation() {
        @Override
        public boolean condition(Value a, Value b) {
            return (b.get() < a.get());
        }
    };
    
    /** ADX b, a | sets b to b+a+EX, sets EX to 0x0001 if there is an overflow, 0x0 otherwise */
    private final Operation adxOp = new Operation() {
         @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            int result = b.get() + a.get() + state.ex;
            b.set((short)result);
            state.ex = (short)((result != b.get()) ? 0x1 : 0x0);
        }
    };
    
    /** SBX b, a | sets b to b-a+EX, sets EX to 0xFFFF if there is an underflow, 0x0001 if there's an overflow, 0x0 otherwise */
    private final Operation sbxOp = new Operation() {
         @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            byte overflow = 0;
            int result = b.get() - a.get();
            overflow += (((short)result != result) ? -1 : 0);
            result += state.ex;
            overflow += (((short)result != result) ? 1 : 0);
            b.set((short)result);
            
            state.ex = 0;
            if(overflow < 0) {
                state.ex = (short)0xffff;
            } else if(overflow > 0) {
                state.ex = 0x1;
            }
        }
    };
    
    /** STI b, a | sets b to a, then increases I and J by 1 */
    private final Operation stiOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            b.set(a.get());
            state.incRegister(Register.I);
            state.incRegister(Register.J);
        }
    };
    
    /** STD b, a | sets b to a, then decreases I and J by 1 */
    private final Operation stdOp = new Operation() {
        @Override
        public void perform(short opcode, short aCode, Value a, short bCode, Value b) {
            b.set(a.get());
            state.decRegister(Register.I);
            state.decRegister(Register.J);
        }
    };
    
    private final Operation[] operations = new Operation[] {
        /* 0x00 */ null,
        /* 0x01 */ setOp,
        /* 0x02 */ addOp,
        /* 0x03 */ subOp,
        /* 0x04 */ mulOp,
        /* 0x05 */ mliOp,
        /* 0x06 */ divOp,
        /* 0x07 */ dviOp,
        /* 0x08 */ modOp,
        /* 0x09 */ mdiOp,
        /* 0x0A */ andOp,
        /* 0x0B */ borOp,
        /* 0x0C */ xorOp,
        /* 0x0D */ shrOp,
        /* 0x0E */ asrOp,
        /* 0x0F */ shlOp,
        /* 0x10 */ ifbOp,
        /* 0x11 */ ifcOp,
        /* 0x12 */ ifeOp,
        /* 0x13 */ ifnOp,
        /* 0x14 */ ifgOp,
        /* 0x15 */ ifaOp,
        /* 0x16 */ iflOp,
        /* 0x17 */ ifuOp,
        /* 0x18 */ null,
        /* 0x19 */ null,
        /* 0x1A */ adxOp,
        /* 0x1B */ sbxOp,
        /* 0x1C */ null,
        /* 0x1D */ null,
        /* 0x1E */ stiOp,
        /* 0x1F */ stdOp,
    };
    
    /** JSR a | pushes the address of the next instruction to the stack, then sets PC to a */
    private final SpecialOperation jsrOp = new SpecialOperation() {
        @Override
        public void perform(short opcode, short specialOpcode, short aCode, Value a) {
            memoryBus.memory().writeWord(--state.sp, state.pc);
            state.pc = a.get();
        }
    };
    
    /** INT a | triggers a software interrupt with message a */
    private final SpecialOperation intOp = new SpecialOperation() {
        @Override
        public void perform(short opcode, short specialOpcode, short aCode, Value a) {
            if(forceInterruptsQueuing && interruptsQueuing) {
                interrupt(a.get());
            } else {
                doInterrupt(a.get());
            }
        }
    };
    
    /** IAG a | sets a to IA */
    private final SpecialOperation iagOp = new SpecialOperation() {
        @Override
        public void perform(short opcode, short specialOpcode, short aCode, Value a) {
            a.set(state.ia);
        }
    };
    
    /** IAS a | sets IA to a */
    private final SpecialOperation iasOp = new SpecialOperation() {
        @Override
        public void perform(short opcode, short specialOpcode, short aCode, Value a) {
            state.ia = a.get();
        }
    };
    
    /** RFI a | disables interrupt queueing, pops A from the stack, then pops PC from the stack */
    private final SpecialOperation rfiOp = new SpecialOperation() {
        @Override
        public void perform(short opcode, short specialOpcode, short aCode, Value a) {
            interruptsQueuing = false;
            state.writeRegister(Register.A, memoryBus.memory().readWord(state.sp++));
            state.pc = memoryBus.memory().readWord(state.sp++);
        }
    };
    
    /** IAQ a | if a is nonzero, interrupts will be added to the queue instead of triggered. if a is zero, interrupts will be triggered as normal again */
    private final SpecialOperation iaqOp = new SpecialOperation() {
        @Override
        public void perform(short opcode, short specialOpcode, short aCode, Value a) {
            forceInterruptsQueuing = (a.get() != 0);
        }
    };
    
    /** HWN a | sets a to number of connected hardware devices */
    private final SpecialOperation hwnOp = new SpecialOperation() {
        @Override
        public void perform(short opcode, short specialOpcode, short aCode, Value a) {
            a.set((short)hardwareBus.devices().length);
        }
    };
    
    /** HWQ a | sets A, B, C, X, Y registers to information about hardware a A+(B<<16) is a 32 bit word identifying the hardware id C is the hardware version X+(Y<<16) is a 32 bit word identifying the manufacturer */
    private final SpecialOperation hwqOp = new SpecialOperation() {
        @Override
        public void perform(short opcode, short specialOpcode, short aCode, Value a) {
            Device device = hardwareBus.device(a.get());

            int deviceId = device.getDeviceId();
            state.writeRegister(Register.A, (short)deviceId);
            state.writeRegister(Register.B, (short)(deviceId >> 16));

            int manufacturer = device.getManufacturer();
            state.writeRegister(Register.X, (short)manufacturer);
            state.writeRegister(Register.Y, (short)(manufacturer >> 16));

            short version = device.getVersion();
            state.writeRegister(Register.C, version);
        }
    };
    
    /** HWI a | sends an interrupt to hardware a */
    private final SpecialOperation hwiOp = new SpecialOperation() {
        @Override
        public void perform(short opcode, short specialOpcode, short aCode, Value a) {
            Device device = hardwareBus.device(a.get());
            device.interrupt(state);
        }
    };
    
    private final Operation[] specialOperations = new Operation[] {
        /* 0x00 */ null,
        /* 0x01 */ jsrOp,
        /* 0x02 */ null,
        /* 0x03 */ null,
        /* 0x04 */ null,
        /* 0x05 */ null,
        /* 0x06 */ null,
        /* 0x07 */ null,
        /* 0x08 */ intOp,
        /* 0x09 */ iagOp,
        /* 0x0A */ iasOp,
        /* 0x0B */ rfiOp,
        /* 0x0C */ iaqOp,
        /* 0x0D */ null,
        /* 0x0E */ null,
        /* 0x0F */ null,
        /* 0x10 */ hwnOp,
        /* 0x11 */ hwqOp,
        /* 0x12 */ hwiOp,
        /* 0x13 */ null,
        /* 0x14 */ null,
        /* 0x15 */ null,
        /* 0x16 */ null,
        /* 0x17 */ null,
        /* 0x18 */ null,
        /* 0x19 */ null,
        /* 0x1A */ null,
        /* 0x1B */ null,
        /* 0x1C */ null,
        /* 0x1D */ null,
        /* 0x1E */ null,
        /* 0x1F */ null,
    };

    /**
     * Force interrupts queuing. If true - interrupts will be queued.
     * Otherwise interrupts will be triggered as normal.
     */
    private boolean forceInterruptsQueuing;

    /** Interrupts queuing */
    private boolean interruptsQueuing;

    /**
     * Interrupts queue.
     * If the queue grows longer than 256 interrupts, the DCPU-16 will catch fire.
     */
    private Queue<Short> interruptsQueue = new LinkedList<Short>();

    /**
     * Processor interrupts lock
     */
    private final Object lock = new Object();

    @Override
    public void start() {
        state.skipping = false;
        state.reset();
        state.sp = memoryBus.memory().sizeInWords();
        
        while(true) {
            // interrupts are not triggered while the DCPU-16 is skipping.
            if(!state.skipping && !interruptsQueuing) {
                Short code = null;
                synchronized(lock) {
                    if(interruptsQueue.size() >= MAX_QUEUED_INT) {
                        throw new RuntimeException("Interrupts queue grow to " + MAX_QUEUED_INT + " interrupts. DCPU-16 catched fire.");
                    }

                    code = interruptsQueue.poll();
                }

                if(code != null) {
                    doInterrupt(code);
                }
            }
            
            // perform loop
            loop();
            
            dumpState();
        }
    }

    @Override
    public void interrupt(short code) {
        synchronized(lock) {
            interruptsQueue.add(code);
        }
    }

    private void loop() {
        short word = nextWord();
        
        short opcode = (short)(word & 0x001f);
        short aCode = (short)((word & 0xfc00) >> 10);
        short bCode = (short)((word & 0x03e0) >> 5);
        
        Operation operation = null;
        Value a = valueForCode(aCode, true);
        Value b = null;
        
        // special operation
        if (opcode == 0x0000) {
            if (bCode >= specialOperations.length || (operation = specialOperations[bCode]) == null) {
                throw new RuntimeException(String.format("Special operation 0x%04X is not supported", bCode));
            }
        } else {
            b = valueForCode(bCode, false);
            if (opcode >= operations.length || (operation = operations[opcode]) == null) {
                throw new RuntimeException(String.format("Operation 0x%04X is not supported", opcode));
            }
        }
        
        if(state.skipping) {
            state.skipping = (operation instanceof ConditionalOperation);
        } else {
            operation.perform(opcode, aCode, a, bCode, b);
        }
    }
    
    private void doInterrupt(short code) {
        if(state.ia != 0) {
            interruptsQueuing = true;
            memoryBus.memory().writeWord(--state.sp, state.pc);
            memoryBus.memory().writeWord(--state.sp, state.readRegister(Register.A));
            state.writeRegister(Register.A, code);
            state.pc = state.ia;
        }
    }
    
    private short nextWord() {
        return memoryBus.memory().readWord(state.pc++);
    }
    
    private Value valueForCode(short code, boolean isAValue) {
        Value value = null;
        if(code >= 0x00 && code <= 0x07) {
            value = new RegisterValue(code);
        } else if(code >= 0x08 && code <= 0x0f) {
            value = new RegisterAddressValue((short)(code - 0x08), false);
        } else if(code >= 0x10 && code <= 0x17) {
            value = new RegisterAddressValue((short)(code - 0x10), true);
        } else if(code == 0x18) {
            value = ((isAValue) ? popValue : pushValue);
        } else if(code == 0x19) {
            value = new MemoryValue(state.sp);
        } else if(code == 0x1a) {
            value = new MemoryValue((short)(state.sp + nextWord()));
        } else if(code == 0x1b) {
            value = spValue;
        } else if(code == 0x1c) {
            value = pcValue;
        } else if(code == 0x1d) {
            value = exValue;
        } else if(code == 0x1e) {
            value = new MemoryValue(nextWord());
        } else if(code == 0x1f) {
            value = new LiteralValue(nextWord());
        } else if(code >= 0x20 && code <= 0x3f && isAValue) {
            value = new LiteralValue((short)(code - 0x20 - 1));
        }
        return value;
    }
}
