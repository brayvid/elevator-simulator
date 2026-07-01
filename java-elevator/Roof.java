public class Roof extends Floor {
    private final UpDownButton downButton;

    public Roof(int floorNumber, Building building) {
        super(floorNumber, building);
        this.downButton = new UpDownButton(Direction.DOWN, this);
    }

    @Override
    public void pressUpButton() {
        // Roof has no up button
    }

    @Override
    public void pressDownButton() {
        downButton.push();
        building.addFloorRequest(floorNumber, Direction.DOWN);
    }

    @Override
    public boolean isUpButtonPressed() {
        return false;
    }

    @Override
    public boolean isDownButtonPressed() {
        return downButton.isPressed();
    }

    @Override
    public void resetUpButton() {
        // No-op
    }

    @Override
    public void resetDownButton() {
        downButton.reset();
    }
}