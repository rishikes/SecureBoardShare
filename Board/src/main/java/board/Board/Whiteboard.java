package board.Board;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import javax.swing.*;

public class Whiteboard implements MouseListener {
	
    // pre-defined colors
    public static final Color BLACK      = Color.BLACK;
    public static final Color BLUE       = Color.BLUE;
    public static final Color CYAN       = Color.CYAN;
    public static final Color DARK_GRAY  = Color.DARK_GRAY;
    public static final Color GRAY       = Color.GRAY;
    public static final Color GREEN      = Color.GREEN;
    public static final Color LIGHT_GRAY = Color.LIGHT_GRAY;
    public static final Color MAGENTA    = Color.MAGENTA;
    public static final Color ORANGE     = Color.ORANGE;
    public static final Color PINK       = Color.PINK;
    public static final Color RED        = Color.RED;
    public static final Color WHITE      = Color.WHITE;
    public static final Color YELLOW     = Color.YELLOW;
	
    // default colors
    private static final Color DEFAULT_PEN_COLOR   = BLACK;
    private static final Color DEFAULT_CLEAR_COLOR = WHITE;
    
    // current pen color
    private static Color penColor;
	
    // default canvas size is DEFAULT_SIZE-by-DEFAULT_SIZE
    private static final int DEFAULT_SIZE = 512;
    private static int width  = DEFAULT_SIZE;
    private static int height = DEFAULT_SIZE;
    
    // default pen radius
    private static final double DEFAULT_PEN_RADIUS = 0.002;

    // current pen radius
    private static double penRadius;
	
    // the frame for drawing to the screen
    private static BufferedImage offscreenImage, onscreenImage;
    private static Graphics2D offscreen, onscreen;
    private static JFrame frame;
    
    // mouse state
    private static boolean mousePressed = false;
    private static double mouseX = 0;
    private static double mouseY = 0;
    
    // singleton for callbacks: avoids generation of extra .class files
    private static Whiteboard board = new Whiteboard();
   
    // boundary of drawing canvas, 5% border
    private static final double BORDER = 0.05;
    private static final double DEFAULT_XMIN = 0.0;
    private static final double DEFAULT_XMAX = 1.0;
    private static final double DEFAULT_YMIN = 0.0;
    private static final double DEFAULT_YMAX = 1.0;
    private static double xmin, ymin, xmax, ymax;

    /*************************************************************************
     *  User and screen coordinate systems
     *************************************************************************/

     /**
      * Set the x-scale to be the default (between 0.0 and 1.0).
      */
     public static void setXscale() { setXscale(DEFAULT_XMIN, DEFAULT_XMAX); }

     /**
      * Set the y-scale to be the default (between 0.0 and 1.0).
      */
     public static void setYscale() { setYscale(DEFAULT_YMIN, DEFAULT_YMAX); }

     /**
      * Set the x-scale (a 10% border is added to the values)
      * @param min the minimum value of the x-scale
      * @param max the maximum value of the x-scale
      */
     public static void setXscale(double min, double max) {
         double size = max - min;
         {
             xmin = min - BORDER * size;
             xmax = max + BORDER * size;
         }
     }

     /**
      * Set the y-scale (a 10% border is added to the values).
      * @param min the minimum value of the y-scale
      * @param max the maximum value of the y-scale
      */
     public static void setYscale(double min, double max) {
         double size = max - min;
          {
             ymin = min - BORDER * size;
             ymax = max + BORDER * size;
         }
     }

     /**
      * Set the x-scale and y-scale (a 10% border is added to the values)
      * @param min the minimum value of the x- and y-scales
      * @param max the maximum value of the x- and y-scales
      */
     public static void setScale(double min, double max) {
         double size = max - min;
          {
             xmin = min - BORDER * size;
             xmax = max + BORDER * size;
             ymin = min - BORDER * size;
             ymax = max + BORDER * size;
         }
     }

     // helper functions that scale from user coordinates to screen coordinates and back
     private static double  scaleX(double x) { return width  * (x - xmin) / (xmax - xmin); }
     private static double  scaleY(double y) { return height * (ymax - y) / (ymax - ymin); }
     private static double factorX(double w) { return w * width  / Math.abs(xmax - xmin);  }
     private static double factorY(double h) { return h * height / Math.abs(ymax - ymin);  }
     private static double   userX(double x) { return xmin + x * (xmax - xmin) / width;    }
     private static double   userY(double y) { return ymax - y * (ymax - ymin) / height;   }
    
     /**
      * Clear the screen to the default color (white).
      */
     public static void clear() { clear(DEFAULT_CLEAR_COLOR); }
     /**
      * Clear the screen to the given color.
      * @param color the Color to make the background
      */
     public static void clear(Color color) {
         offscreen.setColor(color);
         offscreen.fillRect(0, 0, width, height);
         offscreen.setColor(penColor);
         draw();
     }
    
    static { init(); }
    
    private static void init() {
        if (frame != null) frame.setVisible(false);
        frame = new JFrame();
        offscreenImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        onscreenImage  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        offscreen = offscreenImage.createGraphics();
        onscreen  = onscreenImage.createGraphics();
        setXscale();
        setYscale();
        offscreen.setColor(DEFAULT_CLEAR_COLOR);
        offscreen.fillRect(0, 0, width, height);
        setPenColor();
        setPenRadius();
        //setFont();
        clear();
        draw();
        
        // frame stuff
        ImageIcon icon = new ImageIcon(onscreenImage);
        JLabel draw = new JLabel(icon);

        draw.addMouseListener(board);

        frame.setContentPane(draw);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);            // closes all windows
        frame.setTitle("White Board");
        frame.pack();
        frame.requestFocusInWindow();
        frame.setVisible(true);
    }
    
    /**
     * Get the current pen radius.
     */
    public static double getPenRadius() { return penRadius; }

    /**
     * Set the pen size to the default (.002).
     */
    public static void setPenRadius() { setPenRadius(DEFAULT_PEN_RADIUS); }
    /**
     * Set the radius of the pen to the given size.
     * @param r the radius of the pen
     * @throws RuntimeException if r is negative
     */
    public static void setPenRadius(double r) {
        if (r < 0) throw new RuntimeException("pen radius must be positive");
        penRadius = r;
        float scaledPenRadius = (float) (r * DEFAULT_SIZE);
        BasicStroke stroke = new BasicStroke(scaledPenRadius, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        // BasicStroke stroke = new BasicStroke(scaledPenRadius);
        offscreen.setStroke(stroke);
    }
    /**
     * Get the current pen color.
     */
    public static Color getPenColor() { return penColor; }

    /**
     * Set the pen color to the default color (black).
     */
    public static void setPenColor() { setPenColor(DEFAULT_PEN_COLOR); }
    /**
     * Set the pen color to the given color. The available pen colors are
     * BLACK, BLUE, CYAN, DARK_GRAY, GRAY, GREEN, LIGHT_GRAY, MAGENTA,
     * ORANGE, PINK, RED, WHITE, and YELLOW.
     * @param color the Color to make the pen
     */
    public static void setPenColor(Color color) {
        penColor = color;
        offscreen.setColor(penColor);
    }
    
    // draw onscreen if defer is false
    private static void draw() {
       // if (defer) return;
        onscreen.drawImage(offscreenImage, 0, 0, null);
        frame.repaint();
    }

    
	//MouseListener API
	public void mouseClicked(MouseEvent e) {
	//	StdOut.println("Apple");
		
	}
	
	public void mouseEntered(MouseEvent e) {
		
	}
		
	public void mouseExited(MouseEvent e) {
		
	}
	
	public void mousePressed(MouseEvent e) {
	//	StdOut.println("Mouse is pressed");
		System.out.println("Mouse is pressed");
	//	StdOut.printf("%d %d", e.getX(), e.getY());
		offscreen.fillRect((int) Math.round(scaleX(e.getX())), (int) Math.round(scaleY(e.getY())), 1, 1);
		draw();
	}
	
	public void mouseReleased(MouseEvent e) {
		
	}
	
    public static void main(String[] args) {
    	
        while(true) {
        	if (Draw.mousePressed()) {
        		double x = Draw.mouseX();
        		double y = Draw.mouseY();
        		Draw.point(x, y);
        	}
        }
    }

}


