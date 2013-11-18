package net.taviscaron.dcpu16vm.machine.device;

import net.taviscaron.dcpu16vm.machine.Machine;
import net.taviscaron.dcpu16vm.machine.Processor;

/**
 * Device base class
 * @author Andrei Senchuk
 */
public abstract class Device {
    /** Communication channel device->processor */
    protected Machine.InterruptionBus interruptionBus;

    /** Communication channer device->memory */
    protected Machine.MemoryBus memoryBus;

    /**
     * Set interruption bus - device->processor communication channel
     * @param interruptionBus interruption bus
     */
    public void setInterruptionBus(Machine.InterruptionBus interruptionBus) {
        this.interruptionBus = interruptionBus;
    }

    /**
     * Set memory bus - device->memory communication channel
     * @param memoryBus memory bus
     */
    public void setMemoryBus(Machine.MemoryBus memoryBus) {
        this.memoryBus = memoryBus;
    }

    /** Device initializer */
    public void init() {
        // init is not required
    }

    /**
     * Return 2 words device id from the HardwareInfo annotation.
     * A device can override this method in order to providing device id w/o HardwareInfo.
     * @return device id
     */
    public int getDeviceId() {
        HardwareInfo hwAnnotation = this.getClass().getAnnotation(HardwareInfo.class);
        if(hwAnnotation == null) {
            throw new RuntimeException("Annotate device class " + this.getClass().getCanonicalName() + " with @HardwareInfo annotation or override getDeviceId method in order to providing a device id.");
        }
        return hwAnnotation.id();
    }

    /**
     * Return 2 words manufacturer id from the HardwareInfo annotation.
     * A device can override this method in order to providing manufacturer id w/o HardwareInfo.
     * @return manufacturer id
     */
    public int getManufacturer() {
        HardwareInfo hwAnnotation = this.getClass().getAnnotation(HardwareInfo.class);
        if(hwAnnotation == null) {
            throw new RuntimeException("Annotate device class " + this.getClass().getCanonicalName() + " with @HardwareInfo annotation or override getManufacturer method in order to providing a device manufacturer.");
        }
        return hwAnnotation.manufacturer();
    }

    /**
     * Return 1 word device version from the HardwareInfo annotation.
     * A device can override this method in order to providing device version w/o HardwareInfo.
     * @return device version
     */
    public short getVersion() {
        HardwareInfo hwAnnotation = this.getClass().getAnnotation(HardwareInfo.class);
        if(hwAnnotation == null) {
            throw new RuntimeException("Annotate device class " + this.getClass().getCanonicalName() + " with @HardwareInfo annotation or override getVersion method in order to providing a device version.");
        }
        return hwAnnotation.version();
    }

    /**
     * Device interruption handler. Commonly a device might not handle
     * any interrupts but most of devices override this method.
     * @param state current processor state
     */
    public void interrupt(Processor.State state) {
        // noop
    }
}
