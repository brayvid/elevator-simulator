public class Lobby extends Floor {
    private final UpDownButton upButton;

    public Lobby(int floorNumber, Building building) {
        super(floorNumber, building);
        this.upButton = new UpDownButton(Direction.UP, this);
    }

    @Override
    public void pressUpButton() {
        upButton.push();
        building.addFloorRequest(floorNumber, Direction.UP);
    }

    @Override
    public void pressDownButton() {
        // Lobby has no down button
    }

    @Override
    public boolean isUpButtonPressed() {
        return upButton.isPressed();
    }

    @Override
    public boolean isDownButtonPressed() {
        return false;
    }

    @Override
    public void resetUpButton() {
        upButton.reset();
    }

    @Override
    public void resetDownButton() {
        // No-op
    }
}