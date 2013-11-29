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
import java.util.Iterator;
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

    private final Queue<Short> keyboardBuffer = new LinkedList<Short>();
    private final Set<Short> pressedKeys = new HashSet<Short>();
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
                    Short next = keyboardBuffer.poll();
                    state.writeRegister(Processor.Register.C, (next != null) ? next : 0);
                    break;
                case CHECK_KEY_INT:
                    state.writeRegister(Processor.Register.C, (short)((pressedKeys.contains(regB)) ? 1 : 0));
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

    private short keyFromKeyCode(int code) {
        short key = 0;
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
                key = (short)0x90;
                break;
            case KeyEvent.VK_CONTROL:
                key = (short)0x91;
                break;
            case KeyEvent.VK_UP:
                key = (short)0x80;
                break;
            case KeyEvent.VK_DOWN:
                key = (short)0x81;
                break;
            case KeyEvent.VK_LEFT:
                key = (short)0x82;
                break;
            case KeyEvent.VK_RIGHT:
                key = (short)0x83;
                break;
            default:
                if(code >= 0x20 && code <= 0x7f) {
                    key = (short)code;
                }
                break;
        }
        return key;
    }

    private class KeyboardFrame extends JFrame {
        private JTextArea textArea;

        private KeyListener keyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                short code = (short)keyEvent.getKeyChar();
                if(code >= 0x20 && code <= 0x7f) {
                    synchronized(lock) {
                        keyboardBuffer.add(code);
                        onKeyEvent();
                        updateState();
                    }
                }
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                short ch = keyFromKeyCode(keyEvent.getKeyCode());
                if(ch != 0) {
                    synchronized(lock) {
                        // add arrow keys to the keyboard buffer
                        if(ch < 0x20 || ch > 0x7f) {
                            keyboardBuffer.add(ch);
                        }

                        pressedKeys.add(ch);
                        onKeyEvent();
                        updateState();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent keyEvent) {
                short ch = keyFromKeyCode(keyEvent.getKeyCode());
                if(ch != 0) {
                    synchronized(lock) {
                        pressedKeys.remove(ch);
                        onKeyEvent();
                        updateState();
                    }
                }
            }
        };
        
        public KeyboardFrame() {
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setResizable(false);
            setVisible(true);
            setTitle(GenericKeyboard.class.getCanonicalName());
            setFocusable(true);
            getContentPane().setPreferredSize(new Dimension(300, 150));
            addKeyListener(keyListener);

            textArea = new JTextArea();
            textArea.setFocusable(false);
            textArea.setEditable(false);
            textArea.setLineWrap(true);

            add(textArea);
            pack();
        }

        private void updateState() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("buffer: [%s]\n", iterableToString(keyboardBuffer)));
            sb.append(String.format("pressed: [%s]\n",  iterableToString(pressedKeys)));
            textArea.setText(new String(sb));
        }
        
        private String iterableToString(Iterable<Short> iterable) {
            StringBuilder sb = new StringBuilder();
            Iterator<Short> it = iterable.iterator();
            while(it.hasNext()) {
                Short key = it.next();
                sb.append(String.format("0x%02x", key));
                
                if(it.hasNext()) {
                    sb.append(", ");
                }
            }
            return new String(sb);
        }
    }
}
