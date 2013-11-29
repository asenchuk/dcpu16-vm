package net.taviscaron.dcpu16vm.util;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Different program utils
 * @author Andrei Senchuk
 */
public class ProgramUtils {
    /**
     * Read program from file and returns short array
     * @param f file path (relative or absolute)
     * @return program short array
     * @throws IOException on some IO errors
     */
    public static short[] loadFromFile(String f) throws IOException {
        return loadFromFile(f, false);
    }

    /**
     * Read program from file and returns short array
     * @param f file path (relative or absolute)
     * @param littleEndian think that words is file are little endian
     * @return program short array
     * @throws IOException on some IO errors
     */
    public static short[] loadFromFile(String f, boolean littleEndian) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);

            byte[] buffer = new byte[fis.available()];
            int size = fis.read(buffer);

            if(size <= 0) {
                throw new IOException("File " + f + " is empty or some IO error occurred");
            }

            short[] program = new short[size / 2 + size % 2];
            for(int i = 0; i < size; i++) {
                int shift = 8 * (((littleEndian) ? i : i + 1) % 2);
                program[i / 2] |= (buffer[i] & 0xff) << shift;
            }

            return program;
        } finally {
            if(fis != null) {
                fis.close();
            }
        }
    }
}
