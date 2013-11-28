package net.taviscaron.dcpu16vm.device;

import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.machine.device.Device;
import net.taviscaron.dcpu16vm.machine.device.HardwareInfo;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.LayoutStyle;
import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Mackapar 3.5" Floppy Drive (M35FD)
 * @author Andrei Senchuk
 */
@HardwareInfo(id = 0x4fd524c5, version = 0x000b, manufacturer = 0x1eb37e91)
public class M35FD extends Device {
    /** common const */
    private static final int SIZE_IN_WORDS = 737280;
    private static final int SPEED_IN_WORDS_PER_SEC = 30700;
    private static final int TRACK_SEEKING_MICROSECONDS = 2400;
    /** interrupts */
    private static final int POLL_DEVICE_INT = 0x0000;
    private static final int TURN_INTERRUPTS_INT = 0x0001;
    private static final int READ_SECTOR_INT = 0x0002;
    private static final int WRITE_SECTOR_INT = 0x0003;
    /** state codes */
    private static final short STATE_NO_MEDIA = 0x0000;
    private static final short STATE_READY = 0x0001;
    private static final short STATE_READY_WP = 0x0002;
    private static final short STATE_BUSY = 0x0003;
    /** error codes */
    private static final short ERROR_NONE = 0x0000;
    private static final short ERROR_BUSY = 0x0001;
    private static final short ERROR_NO_MEDIA = 0x0002;
    private static final short ERROR_PROTECTED = 0x0003;
    private static final short ERROR_EJECT = 0x0004;
    private static final short ERROR_BAD_SECTOR = 0x0005;
    private static final short ERROR_BROKEN = (short)0xffff;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Object lock = new Object();
    private short error = ERROR_NONE;
    private short state = STATE_NO_MEDIA;
    private short intMessage;
    private FloppyDriveFrame frame;
    private RandomAccessFile randomAccessFile;
    private boolean writeProtection;
    private boolean busy;

    @Override
    public void init() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame = new FloppyDriveFrame();
                frame.setVisible(true);
            }
        });
    }

    @Override
    public void shutdown() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
                frame = null;
            }
        });
    }

    @Override
    public void interrupt(Processor.State state) {
        short regA = state.readRegister(Processor.Register.A);
        synchronized(lock) {
            switch(regA) {
                case POLL_DEVICE_INT:
                    state.writeRegister(Processor.Register.B, this.state);
                    state.writeRegister(Processor.Register.C, error);
                    break;
                case TURN_INTERRUPTS_INT:
                    intMessage = state.readRegister(Processor.Register.X);
                    break;
                case READ_SECTOR_INT:
                    read(state);
                    break;
                case WRITE_SECTOR_INT:
                    write(state);
                    break;
                default:
                    System.err.println("M35FD: unknown int: " + regA);
                    break;
            }
        }
    }

    /** Read sector into memory */
    private void read(Processor.State state) {
        switch(this.state) {
            case STATE_READY:
            case STATE_READY_WP:
                try {
                    executor.execute(new ReadOperation(state.readRegister(Processor.Register.X), state.readRegister(Processor.Register.Y)));
                    state.writeRegister(Processor.Register.B, (short)1);
                } catch(BadSectorException e) {
                    setError(ERROR_BAD_SECTOR);
                    state.writeRegister(Processor.Register.B, (short)0);
                }
                break;
            case STATE_BUSY:
                setError(ERROR_BUSY);
                state.writeRegister(Processor.Register.B, (short)0);
                break;
            case STATE_NO_MEDIA:
                setError(ERROR_NO_MEDIA);
                state.writeRegister(Processor.Register.B, (short)0);
                break;
        }
    }

    /** Write sector from memory to disk */
    private void write(Processor.State state) {
        switch(this.state) {
            case STATE_READY:
                try {
                    executor.execute(new WriteOperation(state.readRegister(Processor.Register.X), state.readRegister(Processor.Register.Y)));
                    state.writeRegister(Processor.Register.B, (short)1);
                } catch(BadSectorException e) {
                    setError(ERROR_BAD_SECTOR);
                    state.writeRegister(Processor.Register.B, (short)0);
                }
            case STATE_READY_WP:
                setError(ERROR_PROTECTED);
                state.writeRegister(Processor.Register.B, (short)0);
                break;
            case STATE_BUSY:
                setError(ERROR_BUSY);
                state.writeRegister(Processor.Register.B, (short)0);
                break;
            case STATE_NO_MEDIA:
                setError(ERROR_NO_MEDIA);
                state.writeRegister(Processor.Register.B, (short)0);
                break;
        }
    }

    /** Update internal device state */
    private void updateState() {
        synchronized(lock) {
            short state = STATE_NO_MEDIA;
            if(randomAccessFile != null) {
                if(busy) {
                    state = STATE_BUSY;
                } else if(writeProtection) {
                    state = STATE_READY_WP;
                } else {
                    state = STATE_READY;
                }
            }
            setState(state);
        }
    }

    /** Set device state. Interrupt processor if interrupts are enabled */
    private void setState(short state) {
        synchronized(lock) {
            boolean interrupt = (state != this.state);
            this.state = state;

            if(interrupt && intMessage != 0) {
                interruptionBus.interrupt(intMessage);
            }
        }
    }

    /** Set device error. Interrupt processor if interrupts are enabled */
    private void setError(short error) {
        synchronized(lock) {
            boolean interrupt = (error != this.error);
            this.error = error;

            if(interrupt && intMessage != 0) {
                interruptionBus.interrupt(intMessage);
            }
        }
    }

    /** Insert disk */
    private void insertDisk(File file) {
        synchronized(lock) {
            if(randomAccessFile != null) {
                throw new RuntimeException("M35FD: bad state: disk is already inserted");
            }

            try {
                writeProtection = !file.canWrite();
                busy = false;
                randomAccessFile = new RandomAccessFile(file, ((writeProtection) ? "r" : "rws"));
            } catch (IOException e) {
                System.err.println("Can't open M35FD image: " + e);
            } finally {
                updateState();
            }
        }
    }

    /** Eject disk */
    private void ejectDisk() {
        synchronized(lock) {
            try {
                randomAccessFile.close();
                randomAccessFile = null;
            } catch (IOException e) {
                System.err.println("Can't close M35FD image: " + e);
            } finally {
                updateState();
            }
        }
    }

    /** Bad section exception wrapper */
    private class BadSectorException extends Exception {
        // empty class
    }

    /** Disk write operation */
    private class WriteOperation extends IOOperation {
        public WriteOperation(short sector, short ram) throws BadSectorException {
            super(sector, ram);
        }

        @Override
        protected void operation() throws IOException {
            randomAccessFile.setLength(sector & 0xffff);
            byte first = randomAccessFile.readByte();
            byte second = randomAccessFile.readByte();
            short word = (short)(((first & 0xff) << 8) + (second & 0xff));
            memoryBus.memory().writeWord(ram, word);
        }
    }

    /** Disk read operation */
    private class ReadOperation extends IOOperation {
        public ReadOperation(short sector, short ram) throws BadSectorException {
            super(sector, ram);
        }

        @Override
        protected void operation() throws IOException {
            short word = memoryBus.memory().readWord(ram);
            randomAccessFile.setLength(sector & 0xffff);
            randomAccessFile.writeByte((word >> 8) & 0xff);
            randomAccessFile.writeByte(word & 0xff);
        }
    }

    /** Base for IO operations. Simulates delays and updates device state. */
    private abstract class IOOperation implements Runnable {
        protected final short sector;
        protected final short ram;

        public IOOperation(short sector, short ram) throws BadSectorException {
            if((sector & 0xffff) >= SIZE_IN_WORDS) {
                throw new IllegalArgumentException("bad sector");
            }

            this.ram = ram;
            this.sector = sector;
        }

        @Override
        public void run() {
            // set busy state
            synchronized(lock) {
                busy = true;
                updateState();
            }

            // simulate seek+read delay
            try {
                TimeUnit.MICROSECONDS.sleep(TRACK_SEEKING_MICROSECONDS);
                TimeUnit.MICROSECONDS.sleep((long)(1000000f / SPEED_IN_WORDS_PER_SEC));
            } catch(InterruptedException e) {
                System.err.println("M35FD: io delay interrupted");
            }

            // read if it's possible and update busy state
            synchronized(lock) {
                if(randomAccessFile != null) {
                    try {
                        operation();
                    } catch(IOException e) {
                        setError(ERROR_BROKEN);
                    }
                } else {
                    setError(ERROR_EJECT);
                }

                busy = false;
                updateState();
            }
        }

        protected abstract void operation() throws IOException;
    }

    /** UI frame */
    private class FloppyDriveFrame extends JFrame {
        private JButton insertButton;
        private JButton ejectButton;
        private JLabel filePathLabel;

        public FloppyDriveFrame() {
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setResizable(false);
            setVisible(true);
            setTitle(M35FD.class.getCanonicalName());
            setFocusable(true);

            filePathLabel = new JLabel();

            insertButton = new JButton();
            insertButton.setText("Insert");
            insertButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    insertButtonClicked();
                }
            });

            ejectButton = new JButton();
            ejectButton.setText("Eject");
            ejectButton.setEnabled(false);
            ejectButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    ejectButtonClicked();
                }
            });

            // initial state
            setInitialUIState();

            GroupLayout layout = new GroupLayout(getContentPane());
            getContentPane().setLayout(layout);

            layout.setHorizontalGroup(
                    layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                    .addContainerGap()
                                    .addComponent(filePathLabel, GroupLayout.PREFERRED_SIZE, 320, 320)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(ejectButton)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(insertButton)
                            )
            );

            layout.setVerticalGroup(
                    layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                            .addComponent(filePathLabel)
                                            .addComponent(insertButton)
                                            .addComponent(ejectButton))
                                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            )
            );

            pack();
        }

        private void insertButtonClicked() {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(false);
            chooser.showDialog(this, "Insert Floppy Disk");

            File file = chooser.getSelectedFile();
            if(file != null) {
                if(!file.exists() || file.isFile() && file.canRead() && (file.canWrite() || file.length() >= SIZE_IN_WORDS * 2)) {
                    insertDisk(file);
                    filePathLabel.setText(file.getAbsolutePath());
                    insertButton.setEnabled(false);
                    ejectButton.setEnabled(true);
                } else {
                    filePathLabel.setText("Can't insert drive due to some errors.");
                }
            }
        }

        private void setInitialUIState() {
            insertButton.setEnabled(true);
            ejectButton.setEnabled(false);
            filePathLabel.setText("Not inserted");
        }

        private void ejectButtonClicked() {
            ejectDisk();
            setInitialUIState();
        }
    }
}
