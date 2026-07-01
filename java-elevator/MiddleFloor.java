public class MiddleFloor extends Floor {
    private final UpDownButton upButton;
    private final UpDownButton downButton;

    public MiddleFloor(int floorNumber, Building building) {
        super(floorNumber, building);
        this.upButton = new UpDownButton(Direction.UP, this);
        this.downButton = new UpDownButton(Direction.DOWN, this);
    }

    @Override
    public void pressUpButton() {
        upButton.push();
        building.addFloorRequest(floorNumber, Direction.UP);
    }

    @Override
    public void pressDownButton() {
        downButton.push();
        building.addFloorRequest(floorNumber, Direction.DOWN);
    }

    @Override
    public boolean isUpButtonPressed() {
        return upButton.isPressed();
    }

    @Override
    public boolean isDownButtonPressed() {
        return downButton.isPressed();
    }

    @Override
    public void resetUpButton() {
        upButton.reset();
    }

    @Override
    public void resetDownButton() {
        downButton.reset();
    }
}