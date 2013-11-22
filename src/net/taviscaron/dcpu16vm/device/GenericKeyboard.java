package net.taviscaron.dcpu16vm.device;

import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.machine.device.Device;
import net.taviscaron.dcpu16vm.machine.device.HardwareInfo;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * Generic keyboard implementation
 * @author Andrei Senchuk
 */
@HardwareInfo(id = 0x30cf7406, version = 0x0001)
public class GenericKeyboard extends Device {
    private static final int CLEAR_BUFFER_INT = 0;
    private static final int READ_KEY_INT = 1;
    private static final int CHECK_KEY_INT = 2;
    private static final int TURN_INTERRUPTS_INT = 3;

    private final Queue<Byte> keyboardBuffer = new LinkedList<Byte>();
    private final Set<Byte> pressedKeys = new HashSet<Byte>();
    private final Object lock = new Object();
    private short interruptMessage;
    private KeyboardFrame keyboardFrame;

    @Override
    public void init() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                keyboardFrame = new KeyboardFrame();
                keyboardFrame.setVisible(true);
            }
        });
    }

    @Override
    public void shutdown() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                if(keyboardFrame != null) {
                    keyboardFrame.dispose();
                }
            }
        });
    }

    @Override
    public void interrupt(Processor.State state) {
        short regA = state.readRegister(Processor.Register.A);
        short regB = state.readRegister(Processor.Register.B);

        synchronized(lock) {
            switch(regA) {
                case CLEAR_BUFFER_INT:
                    keyboardBuffer.clear();
                    break;
                case READ_KEY_INT:
                    Byte next = keyboardBuffer.poll();
                    state.writeRegister(Processor.Register.C, (next != null) ? next : 0);
                    break;
                case CHECK_KEY_INT:
                    state.writeRegister(Processor.Register.C, (short)((pressedKeys.contains((byte)regB)) ? 1 : 0));
                    break;
                case TURN_INTERRUPTS_INT:
                    interruptMessage = regB;
                    break;
                default:
                    break;
            }
        }
    }

    private void onKeyEvent() {
        if(interruptMessage != 0) {
            interruptionBus.interrupt(interruptMessage);
        }
    }

    private byte keyFromKeyCode(int code) {
        byte key = 0;
        switch(code) {
            case KeyEvent.VK_BACK_SPACE:
                key = 0x10;
                break;
            case KeyEvent.VK_ENTER:
                key = 0x11;
                break;
            case KeyEvent.VK_INSERT:
                key = 0x12;
                break;
            case KeyEvent.VK_DELETE:
                key = 0x13;
                break;
            case KeyEvent.VK_SHIFT:
                key = (byte)0x90;
                break;
            case KeyEvent.VK_CONTROL:
                key = (byte)0x91;
                break;
            case KeyEvent.VK_UP:
                key = (byte)0x80;
                break;
            case KeyEvent.VK_DOWN:
                key = (byte)0x81;
                break;
            case KeyEvent.VK_LEFT:
                key = (byte)0x82;
                break;
            case KeyEvent.VK_RIGHT:
                key = (byte)0x83;
                break;
            default:
                if(code >= 0x20 && code <= 0x7f) {
                    key = (byte)code;
                }
                break;
        }
        return key;
    }

    private byte charFromCharCode(char code) {
        byte ch = 0;
        if(code >= 0x20 && code <= 0x7f) {
            ch = (byte)code;
        }
        return ch;
    }

    private class KeyboardFrame extends JFrame implements KeyListener {
        private JTextArea textArea;

        public KeyboardFrame() {
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setResizable(false);
            setVisible(true);
            setTitle(GenericKeyboard.class.getCanonicalName());
            setFocusable(true);
            getContentPane().setPreferredSize(new Dimension(300, 150));
            addKeyListener(this);

            textArea = new JTextArea();
            textArea.setFocusable(false);
            textArea.setEditable(false);
            textArea.setLineWrap(true);

            add(textArea);
            pack();
        }

        @Override
        public void keyTyped(KeyEvent keyEvent) {
            byte ch = charFromCharCode(keyEvent.getKeyChar());
            if(ch != 0) {
                synchronized(lock) {
                    keyboardBuffer.add(ch);
                    onKeyEvent();
                }
                updateState();
            }
        }

        @Override
        public void keyPressed(KeyEvent keyEvent) {
            byte ch = keyFromKeyCode(keyEvent.getKeyCode());
            if(ch != 0) {
                synchronized(lock) {
                    pressedKeys.add(ch);
                    onKeyEvent();
                }
                updateState();
            }
        }

        @Override
        public void keyReleased(KeyEvent keyEvent) {
            byte ch = keyFromKeyCode(keyEvent.getKeyCode());
            if(ch != 0) {
                synchronized(lock) {
                    pressedKeys.remove(ch);
                    onKeyEvent();
                }
                updateState();
            }
        }

        private void updateState() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("buffer: %s\n", keyboardBuffer));
            sb.append(String.format("pressed: %s\n", pressedKeys));
            textArea.setText(new String(sb));
        }
    }
}
