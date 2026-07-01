public class Button {
    protected boolean pressed = false;

    public void push() {
        this.pressed = true;
    }

    public void reset() {
        this.pressed = false;
    }

    public boolean isPressed() {
        return pressed;
    }
}