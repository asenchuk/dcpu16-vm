package net.taviscaron.dcpu16vm;

import net.taviscaron.dcpu16vm.config.XmlMachineBuilder;
import net.taviscaron.dcpu16vm.machine.Machine;
import net.taviscaron.dcpu16vm.util.ProgramUtils;

/**
 * dcpu16 virtual machine entry point
 * @author Andrei Senchuk
 */
public class Main {
    public static void main(String[] args) {
        if(args.length != 2) {
            System.err.println("Usage: java " + Main.class.getCanonicalName() + " machine_configuration.xml program.dcpu16");
            System.exit(1);
        }

        try {
            Machine machine = new XmlMachineBuilder(args[0]).createMachine();
            machine.setProgram(ProgramUtils.loadFromFile(args[1]));
            machine.start();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1); // force exit all other threads like AWT EventQueue, etc
        }
    }
}
