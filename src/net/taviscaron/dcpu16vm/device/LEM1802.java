package net.taviscaron.dcpu16vm.device;

import net.taviscaron.dcpu16vm.machine.Processor;
import net.taviscaron.dcpu16vm.machine.device.Device;
import net.taviscaron.dcpu16vm.machine.device.HardwareInfo;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.EventQueue;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import java.util.Timer;
import java.util.TimerTask;

/**
 * LEM1802 implementation
 * @author Andrei Senchuk
 */
@HardwareInfo(id = 0x7349f615, version = 0x1802, manufacturer = 0x1c6c8b36)
public class LEM1802 extends Device {
    private static final int MEM_MAP_SCREEN = 0;
    private static final int MEM_MAP_FONT = 1;
    private static final int MEM_MAP_PALETTE = 2;
    private static final int SET_BORDER_COLOR = 3;
    private static final int MEM_DUMP_FONT = 4;
    private static final int MEM_DUMP_PALETTE = 5;

    private static final int SCREEN_WIDTH = 32;
    private static final int SCREEN_HEIGHT = 12;
    private static final int CELL_WIDTH = 4;
    private static final int CELL_HEIGHT = 8;
    private static final int POINT_SIZE = 4;
    private static final int BORDER_SIZE = 4;
    
    private static final int REPAINT_FREQ = 20; // Hz
    private static final int BLINK_DELAY = 1000; // msec

    private static final short[] DEFAULT_FONT = new short[]{
        (short) 0x000f, (short) 0x0808,
        (short) 0x080f, (short) 0x0808,
        (short) 0x08f8, (short) 0x0808,
        (short) 0x00ff, (short) 0x0808,
        (short) 0x0808, (short) 0x0808,
        (short) 0x08ff, (short) 0x0808,
        (short) 0x00ff, (short) 0x1414,
        (short) 0xff00, (short) 0xff08,
        (short) 0x1f10, (short) 0x1714,
        (short) 0xfc04, (short) 0xf414,
        (short) 0x1710, (short) 0x1714,
        (short) 0xf404, (short) 0xf414,
        (short) 0xff00, (short) 0xf714,
        (short) 0x1414, (short) 0x1414,
        (short) 0xf700, (short) 0xf714,
        (short) 0x1417, (short) 0x1414,
        (short) 0x0f08, (short) 0x0f08,
        (short) 0x14f4, (short) 0x1414,
        (short) 0xf808, (short) 0xf808,
        (short) 0x0f08, (short) 0x0f08,
        (short) 0x001f, (short) 0x1414,
        (short) 0x00fc, (short) 0x1414,
        (short) 0xf808, (short) 0xf808,
        (short) 0xff08, (short) 0xff08,
        (short) 0x14ff, (short) 0x1414,
        (short) 0x080f, (short) 0x0000,
        (short) 0x00f8, (short) 0x0808,
        (short) 0xffff, (short) 0xffff,
        (short) 0xf0f0, (short) 0xf0f0,
        (short) 0xffff, (short) 0x0000,
        (short) 0x0000, (short) 0xffff,
        (short) 0x0f0f, (short) 0x0f0f,
        (short) 0x0000, (short) 0x0000,
        (short) 0x005f, (short) 0x0000,
        (short) 0x0300, (short) 0x0300,
        (short) 0x3e14, (short) 0x3e00,
        (short) 0x266b, (short) 0x3200,
        (short) 0x611c, (short) 0x4300,
        (short) 0x3629, (short) 0x7650,
        (short) 0x0002, (short) 0x0100,
        (short) 0x1c22, (short) 0x4100,
        (short) 0x4122, (short) 0x1c00,
        (short) 0x2a1c, (short) 0x2a00,
        (short) 0x083e, (short) 0x0800,
        (short) 0x4020, (short) 0x0000,
        (short) 0x0808, (short) 0x0800,
        (short) 0x0040, (short) 0x0000,
        (short) 0x601c, (short) 0x0300,
        (short) 0x3e41, (short) 0x3e00,
        (short) 0x427f, (short) 0x4000,
        (short) 0x6259, (short) 0x4600,
        (short) 0x2249, (short) 0x3600,
        (short) 0x0f08, (short) 0x7f00,
        (short) 0x2745, (short) 0x3900,
        (short) 0x3e49, (short) 0x3200,
        (short) 0x6119, (short) 0x0700,
        (short) 0x3649, (short) 0x3600,
        (short) 0x2649, (short) 0x3e00,
        (short) 0x0024, (short) 0x0000,
        (short) 0x4024, (short) 0x0000,
        (short) 0x0814, (short) 0x2241,
        (short) 0x1414, (short) 0x1400,
        (short) 0x4122, (short) 0x1408,
        (short) 0x0259, (short) 0x0600,
        (short) 0x3e59, (short) 0x5e00,
        (short) 0x7e09, (short) 0x7e00,
        (short) 0x7f49, (short) 0x3600,
        (short) 0x3e41, (short) 0x2200,
        (short) 0x7f41, (short) 0x3e00,
        (short) 0x7f49, (short) 0x4100,
        (short) 0x7f09, (short) 0x0100,
        (short) 0x3e49, (short) 0x3a00,
        (short) 0x7f08, (short) 0x7f00,
        (short) 0x417f, (short) 0x4100,
        (short) 0x2040, (short) 0x3f00,
        (short) 0x7f0c, (short) 0x7300,
        (short) 0x7f40, (short) 0x4000,
        (short) 0x7f06, (short) 0x7f00,
        (short) 0x7f01, (short) 0x7e00,
        (short) 0x3e41, (short) 0x3e00,
        (short) 0x7f09, (short) 0x0600,
        (short) 0x3e41, (short) 0xbe00,
        (short) 0x7f09, (short) 0x7600,
        (short) 0x2649, (short) 0x3200,
        (short) 0x017f, (short) 0x0100,
        (short) 0x7f40, (short) 0x7f00,
        (short) 0x1f60, (short) 0x1f00,
        (short) 0x7f30, (short) 0x7f00,
        (short) 0x7708, (short) 0x7700,
        (short) 0x0778, (short) 0x0700,
        (short) 0x7149, (short) 0x4700,
        (short) 0x007f, (short) 0x4100,
        (short) 0x031c, (short) 0x6000,
        (short) 0x0041, (short) 0x7f00,
        (short) 0x0201, (short) 0x0200,
        (short) 0x8080, (short) 0x8000,
        (short) 0x0001, (short) 0x0200,
        (short) 0x2454, (short) 0x7800,
        (short) 0x7f44, (short) 0x3800,
        (short) 0x3844, (short) 0x2800,
        (short) 0x3844, (short) 0x7f00,
        (short) 0x3854, (short) 0x5800,
        (short) 0x087e, (short) 0x0900,
        (short) 0x4854, (short) 0x3c00,
        (short) 0x7f04, (short) 0x7800,
        (short) 0x447d, (short) 0x4000,
        (short) 0x2040, (short) 0x3d00,
        (short) 0x7f10, (short) 0x6c00,
        (short) 0x417f, (short) 0x4000,
        (short) 0x7c18, (short) 0x7c00,
        (short) 0x7c04, (short) 0x7800,
        (short) 0x3844, (short) 0x3800,
        (short) 0x7c14, (short) 0x0800,
        (short) 0x0814, (short) 0x7c00,
        (short) 0x7c04, (short) 0x0800,
        (short) 0x4854, (short) 0x2400,
        (short) 0x043e, (short) 0x4400,
        (short) 0x3c40, (short) 0x7c00,
        (short) 0x1c60, (short) 0x1c00,
        (short) 0x7c30, (short) 0x7c00,
        (short) 0x6c10, (short) 0x6c00,
        (short) 0x4c50, (short) 0x3c00,
        (short) 0x6454, (short) 0x4c00,
        (short) 0x0836, (short) 0x4100,
        (short) 0x0077, (short) 0x0000,
        (short) 0x4136, (short) 0x0800,
        (short) 0x0201, (short) 0x0201,
        (short) 0x704c, (short) 0x7000,
    };
    
    private static final short[] DEFAULT_PALETTE = new short[]{
        (short) 0x0000, (short) 0x000a, (short) 0x00a0, (short) 0x00aa,
        (short) 0x0a00, (short) 0x0a0a, (short) 0x0a50, (short) 0x0aaa,
        (short) 0x0555, (short) 0x055f, (short) 0x05f5, (short) 0x05ff,
        (short) 0x0f55, (short) 0x0f5f, (short) 0x0ff5, (short) 0x0fff,
    };

    private short memMapScreen;
    private short memMapFont;
    private short memMapPalette;
    private short borderColor;
    private JFrame frame;
    private final Object repaintLockObj = new Object();

    @Override
    public void init() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.setResizable(false);
                frame.setVisible(true);
                frame.setTitle(LEM1802.class.getCanonicalName());
                frame.getContentPane().setPreferredSize(new Dimension(SCREEN_WIDTH * CELL_WIDTH * POINT_SIZE + 2 * BORDER_SIZE, SCREEN_HEIGHT * CELL_HEIGHT * POINT_SIZE + 2 * BORDER_SIZE));

                final PaintPane paintPane = new PaintPane();
                frame.add(paintPane);

                frame.pack();

                new Timer(true).scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                paintPane.toggleBlink();
                                frame.getContentPane().validate();
                                frame.getContentPane().repaint();
                            }
                        });
                    }
                }, 0, 1000 / REPAINT_FREQ);
            }
        });
    }

    @Override
    public void shutdown() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                if(frame != null) {
                    frame.dispose();
                }
            }
        });
    }

    @Override
    public void interrupt(Processor.State state) {
        short regA = state.readRegister(Processor.Register.A);
        short regB = state.readRegister(Processor.Register.B);
        
        if(regA == MEM_DUMP_FONT) {
            memoryBus.memory().set(regB, DEFAULT_FONT);
        } else if (regA == MEM_DUMP_PALETTE) {
            memoryBus.memory().set(regB, DEFAULT_PALETTE);
        } else {
            synchronized(repaintLockObj) {
                switch(state.readRegister(Processor.Register.A)) {
                    case MEM_MAP_SCREEN:
                        memMapScreen = regB;
                        break;
                    case MEM_MAP_FONT:
                        memMapFont = regB;
                        break;
                    case MEM_MAP_PALETTE:
                        memMapPalette = regB;
                        break;
                    case SET_BORDER_COLOR:
                        borderColor = (short)(regB & 0xf);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private class PaintPane extends JPanel {
        private boolean blink = false;
        private long lastBlinkChangingTime;

        public void toggleBlink() {
            long currentTime = System.currentTimeMillis();
            if(currentTime - lastBlinkChangingTime > BLINK_DELAY) {
                lastBlinkChangingTime = currentTime;
                blink = !blink;
            }
        }

        @Override
        public void paint(Graphics graphics) {
            synchronized(repaintLockObj) {
                safePaint(graphics);
            }
        }

        private void safePaint(Graphics g) {
            // draw border
            g.setColor(colorForIndex(borderColor));
            g.fillRect(0, 0, getWidth(), getHeight());
            
            // draw background
            g.setColor(colorForIndex((short)0));
            g.fillRect(BORDER_SIZE, BORDER_SIZE, getWidth() - 2 * BORDER_SIZE, getHeight() - 2 * BORDER_SIZE);
            
            // check screen is connected
            if(memMapScreen == 0) {
                return;
            }

            for(short j = 0; j < SCREEN_HEIGHT; j++) {
                for(short i = 0; i < SCREEN_WIDTH; i++) {
                    short memIndex = (short)(memMapScreen + SCREEN_WIDTH * j + i);
                    short value = memoryBus.memory().readWord(memIndex);

                    short symbolIndex = (short)(value & 0x7f);
                    short blinking = (short)((value & 0x80) >> 7);
                    short bgIndex = (short)((value & 0xf00) >> 8);
                    short fgIndex = (short)((value & 0xf000) >> 12);

                    // swap colors
                    if(blinking != 0 && blink) {
                        bgIndex ^= fgIndex;
                        fgIndex ^= bgIndex;
                        bgIndex ^= fgIndex;
                    }

                    // choose color
                    Color bg = colorForIndex(bgIndex);
                    Color fg = colorForIndex(fgIndex);

                    // choose symbol
                    int symbol = symbolForIndex(symbolIndex);

                    drawSymbol(g, i, j, symbol, fg, bg);
                }
            }
        }

        private void drawSymbol(Graphics g, short xPos, short yPos, int symbol, Color fg, Color bg) {
            // draw bg
            g.setColor(bg);
            g.fillRect(xPos * CELL_WIDTH * POINT_SIZE + BORDER_SIZE, yPos * CELL_HEIGHT * POINT_SIZE + BORDER_SIZE, CELL_WIDTH * POINT_SIZE, CELL_HEIGHT * POINT_SIZE);

            // draw symbol
            g.setColor(fg);

            for(short i = 0; i < CELL_WIDTH; i++) {
                for(short j = 0; j < CELL_HEIGHT; j++) {
                    int pos = (CELL_WIDTH - i - 1) * CELL_HEIGHT + j;
                    if((symbol & (1 << pos)) != 0) {
                        g.fillRect((xPos * CELL_WIDTH + i) * POINT_SIZE + BORDER_SIZE, (yPos * CELL_HEIGHT + j) * POINT_SIZE + BORDER_SIZE, POINT_SIZE, POINT_SIZE);
                    }
                }
            }
        }
        
        private Color colorForIndex(short index) {
            short color = ((memMapPalette != 0) ? memoryBus.memory().readWord((short) (memMapPalette + index)) : DEFAULT_PALETTE[index]);
            int blue = (color & 0xf) * 0x10;
            int green = ((color & 0xf0) >> 4) * 0x10;
            int red = ((color & 0xf00) >> 8) * 0x10;
            return new Color(red, green, blue);
        }
        
        private int symbolForIndex(short index) {
            short first = DEFAULT_FONT[index * 2];
            short second = DEFAULT_FONT[index * 2 + 1];
            if (memMapFont != 0) {
                first = memoryBus.memory().readWord((short)(memMapFont + index * 2));
                second = memoryBus.memory().readWord((short)(memMapFont + index * 2 + 1));
            }
            return (first << 16) + (second & 0xffff);
        }
    }
}
