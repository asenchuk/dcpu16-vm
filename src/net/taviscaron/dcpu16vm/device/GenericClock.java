package net.taviscaron.dcpu16vm.device;

import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.machine.device.Device;
import net.taviscaron.dcpu16vm.machine.device.HardwareInfo;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generic clock implementation
 * @author Andrei Senchuk
 */
@HardwareInfo(id = 0x12d0b402, version = 0x0001)
public class GenericClock extends Device {
    private static final int SET_FREQ_INT = 0;
    private static final int READ_TICKS_INT = 1;
    private static final int TURN_INTERRUPTS_INT = 2;

    private final AtomicInteger ticks = new AtomicInteger();
    private final AtomicInteger intMessage = new AtomicInteger();
    private Timer timer;

    private final TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            ticks.incrementAndGet();

            short message = intMessage.shortValue();
            if(message != 0) {
                interruptionBus.interrupt(message);
            }
        }
    };

    @Override
    public void interrupt(Processor.State state) {
        short regA = state.readRegister(Processor.Register.A);
        short regB = state.readRegister(Processor.Register.B);

        switch(regA) {
            case SET_FREQ_INT:
                if(regB == 0) {
                    if(timer != null) {
                        timer.cancel();
                        timer = null;
                    }
                } else {
                    ticks.set(0);

                    long duration = (long)Math.max(1, 60000f / (regB & 0xffff));
                    timer = new Timer(true);
                    timer.scheduleAtFixedRate(timerTask, 0, duration);
                }
                break;
            case READ_TICKS_INT:
                state.writeRegister(Processor.Register.C, (short)ticks.get());
                break;
            case TURN_INTERRUPTS_INT:
                intMessage.set(regB);
            default:
                break;
        }
    }
}
