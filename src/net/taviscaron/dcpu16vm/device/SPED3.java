package net.taviscaron.dcpu16vm.device;

import com.sun.opengl.util.Animator;
import com.sun.opengl.util.FPSAnimator;
import java.awt.Dimension;
import java.awt.EventQueue;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;
import net.taviscaron.dcpu16vm.machine.Processor.Register;
import net.taviscaron.dcpu16vm.machine.Processor.State;
import net.taviscaron.dcpu16vm.machine.device.Device;
import net.taviscaron.dcpu16vm.machine.device.HardwareInfo;

/**
 * Suspended Particle Exciter Display implementation
 * @author Andrei Senchuk
 */
@HardwareInfo(id = 0x42babf3c, version = 0x0003, manufacturer = 0x1eb37e91)
public class SPED3 extends Device {   
    /** interrupts */
    private static final int POLL_DEVICE_INT = 0x0000;
    private static final int MAP_REGION_INT = 0x0001;
    private static final int ROTATE_DEVICE_INT = 0x0002;
    
    /** state codes */
    private static final short STATE_NO_DATA = 0x0000;
    private static final short STATE_RUNNING = 0x0001;
    private static final short STATE_TURNING = 0x0002;
    
    /** error codes */
    private static final short ERROR_NONE = 0x0000;
    private static final short ERROR_BROKEN = (short)0xffff;
    
    /** device size */
    private static final int DISPLAY_WIDTH = 480;
    private static final int DISPLAY_HEIGHT = 480;
    private static final int UPDATE_FREQ = 20;
    private static final float ROTATION_SPEED = 50;
    
    /** graphic size */
    private static final float DEFAULT_SIZE = 1f;
    private static final float VERTEX_INTENSE_SIZE = 3f;
    private static final float CAMERA_DISTANCE_FROM_CENTER = 3.5f;
    
    /** colors */
    private static final int DEFAULT_LINE_COLOR_INDEX = 2;
    private static final float[][] COLORS = new float[][] {
        new float[] {0f, 0f, 0f},
        new float[] {1f, 0f, 0f},
        new float[] {0f, 1f, 0f},
        new float[] {0f, 0f, 1f},
    };
    
    private final Object lock = new Object();
    private JFrame frame;
    private short errno = ERROR_NONE;
    private short mapOffset;
    private short vertexCount;
    private short rotation;
    private boolean rotating;
    
    private final GLEventListener renderer = new GLEventListener() {
        private final GLU glu = new GLU();
        private float currentRotation = 0.0f;
        
        private void drawVertexs(Vertex[] vertexs, GLAutoDrawable glDrawable, GL gl) {
            // draw lines
            gl.glLineWidth(DEFAULT_SIZE);
            gl.glColor3fv(COLORS[DEFAULT_LINE_COLOR_INDEX], 0);
            gl.glBegin(GL.GL_LINE_STRIP);
            
            for(Vertex v : vertexs) {
                gl.glVertex3f(v.x, v.z, v.y);
            }
            
            gl.glEnd();
                    
            // draw points
            for(Vertex v : vertexs) {
                gl.glPointSize((v.intense) ? VERTEX_INTENSE_SIZE : DEFAULT_SIZE);
                gl.glColor3fv(COLORS[v.color], 0);
                gl.glBegin(GL.GL_POINTS);
                gl.glVertex3f(v.x, v.z, v.y);
                gl.glEnd();
            }
        }
        
        @Override
        public void display(GLAutoDrawable gLDrawable) {
            GL gl = gLDrawable.getGL();
            
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            
            // reset matrix
            gl.glLoadIdentity();
            
            synchronized(lock) {
                if(currentRotation != rotation) {
                    rotating = true;
                    
                    int direction = (currentRotation < rotation) ? 1 : -1;
                    currentRotation += direction * Math.min(ROTATION_SPEED / UPDATE_FREQ, Math.abs(rotation - currentRotation));
                    if(currentRotation > 360) {
                        currentRotation -= 360;
                    }
                } else {
                    rotating = false;
                }
                
                double angle = currentRotation / 180 * Math.PI;
                double dx = Math.sin(angle) * CAMERA_DISTANCE_FROM_CENTER;
                double dz = Math.cos(angle) * CAMERA_DISTANCE_FROM_CENTER;
                
                glu.gluLookAt(dx + 0.5, 0.5, dz + 0.5, 0.5, 0.5, 0.5, 0, 1, 0);
                
                if(mapOffset != 0 && vertexCount != 0) {
                    int count = vertexCount & 0xffff;
                    Vertex[] vertexs = new Vertex[count];
                    
                    for(int i = 0; i < count; i++) {
                        short first = memoryBus.memory().readWord((short)(mapOffset + i * 2));
                        short second = memoryBus.memory().readWord((short)(mapOffset + i * 2 + 1));
                        vertexs[i] = new Vertex(first, second);
                    }
                    
                    drawVertexs(vertexs, gLDrawable, gl);
                }
            }
        }

        @Override
        public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) {
            // noop
        }
        
        @Override
        public void init(GLAutoDrawable gLDrawable) {
            GL gl = gLDrawable.getGL();
            gl.glShadeModel(GL.GL_SMOOTH);
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClearDepth(1.0f);
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        }
        
        @Override   
        public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) {
            GL gl = gLDrawable.getGL();
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glLoadIdentity();
            glu.gluPerspective(30.0f, width/(float)height, 1.0, 1000.0);
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glLoadIdentity();
        }
    };
    
    @Override
    public void init() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.setResizable(false);
                frame.setVisible(true);
                frame.setTitle(SPED3.class.getCanonicalName());
                frame.getContentPane().setPreferredSize(new Dimension(DISPLAY_WIDTH, DISPLAY_HEIGHT));
                
                GLCanvas canvas = new GLCanvas();
                canvas.addGLEventListener(renderer);
                frame.add(canvas);
                
                frame.pack();

                Animator animator = new FPSAnimator(UPDATE_FREQ);
                animator.add(canvas);
                animator.start();
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
    public void interrupt(State state) {
        synchronized(lock) {
            short regA = state.readRegister(Register.A);
            switch(regA) {
                case POLL_DEVICE_INT:
                    short st = (mapOffset != 0 && vertexCount > 0) ? ((rotating) ? STATE_TURNING : STATE_RUNNING) : STATE_TURNING;
                    state.writeRegister(Register.B, st);
                    state.writeRegister(Register.C, errno);
                    errno = ERROR_NONE;
                    break;
                case MAP_REGION_INT:
                    mapOffset = state.readRegister(Register.X);
                    vertexCount = state.readRegister(Register.Y);
                    break;
                case ROTATE_DEVICE_INT:
                    rotation = (short)((state.readRegister(Register.X) & 0xffff) % 360);
                    break;
                default:
                    System.err.format("SPED-3: unknown interrupt 0x%04x\n", regA);
                    break;
            }
        }
    }
    
    /** vertex wrapper */
    private static class Vertex {
        public final float x;
        public final float y;
        public final float z;
        public final boolean intense;
        public final int color;
        
        public Vertex(short first, short second) {
            x = (first & 0xff) / 255f;
            y = ((first >> 8) & 0xff) / 255f;
            z = (second & 0xff) / 255f;
            color = (second >> 8) & 0x03;
            intense = (((second >> 10) & 0x01) == 1);
        }
    }
}
