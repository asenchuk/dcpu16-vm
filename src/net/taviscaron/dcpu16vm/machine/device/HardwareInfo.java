package net.taviscaron.dcpu16vm.machine.device;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Hardware info annotation
 * @author Andrei Senchuk
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HardwareInfo {
    public int id();
    public short version();
    public int manufacturer();
}
