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
    public synchronized short readWord(short offset) {
        return memory[offset & 0xffff];
    }

    @Override
    public synchronized void writeWord(short offset, short value) {
        memory[offset & 0xffff] = value;
    }

    @Override
    public short sizeInWords() {
        return (short)memory.length;
    }

    @Override
    public synchronized void set(short offset, short[] buffer) {
        int start = offset & 0xffff;
        int size = Math.min(buffer.length, memory.length - start);
        System.arraycopy(buffer, 0, memory, start, size);
    }
}
