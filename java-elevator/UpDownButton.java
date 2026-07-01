public class UpDownButton extends Button {
    private final Direction direction;
    private final Floor floor;

    public UpDownButton(Direction direction, Floor floor) {
        this.direction = direction;
        this.floor = floor;
    }

    public Direction getDirection() {
        return direction;
    }

    public Floor getFloor() {
        return floor;
    }

    @Override
    public String toString() {
        return "UpDownButton(Floor: " + floor.getFloorNumber() + ", Dir: " + direction + ", Pressed: " + pressed + ")";
    }
}