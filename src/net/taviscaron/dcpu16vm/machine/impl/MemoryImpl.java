package net.taviscaron.dcpu16vm.machine.impl;

import net.taviscaron.dcpu16vm.machine.Memory;

/**
 * Memory in-memory implementation
 * @author Andrei Senchuk
 */
public class MemoryImpl implements Memory {
    public static final int DEFAULT_MEMORY_WORDS_SIZE = 0x10000;
    
    /** memory buffer */
    private short[] memory = new short[DEFAULT_MEMORY_WORDS_SIZE];

    @Override
    public short readWord(short offset) {
        return memory[offset & 0xffff];
    }

    @Override
    public void writeWord(short offset, short value) {
        memory[offset & 0xffff] = value;
    }

    @Override
    public void fill(short from, short to, short value) {
        int start = from & 0xffff;
        int end = Math.min(to & 0xffff, memory.length);
        
        for(int i = start; i < end; i++) {
            memory[i] = value;
        }
    }

    @Override
    public short sizeInWords() {
        return (short)memory.length;
    }

    @Override
    public void set(short offset, short[] buffer) {
        int start = offset & 0xffff;
        int size = Math.min(buffer.length, memory.length - start);
        System.arraycopy(buffer, 0, memory, start, size);
    }
}
