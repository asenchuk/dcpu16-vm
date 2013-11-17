package net.taviscaron.dcpu16vm.machine;

/**
 * Memory interface
 * @author Andrei Senchuk
 */
public interface Memory {
    public short readWord(short offset);
    public void writeWord(short offset, short value);
    public void fill(short from, short to, short value);
    public void set(short offset, short[] buffer);
    public short sizeInWords();
}
