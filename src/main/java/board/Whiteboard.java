package board;

public class Whiteboard {
	
	
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

