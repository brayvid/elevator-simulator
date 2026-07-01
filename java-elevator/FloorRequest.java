import java.util.Objects;

public class FloorRequest {
    private final int floorNumber;
    private final Direction direction;

    public FloorRequest(int floorNumber, Direction direction) {
        this.floorNumber = floorNumber;
        this.direction = direction;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FloorRequest that = (FloorRequest) o;
        return floorNumber == that.floorNumber && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floorNumber, direction);
    }

    @Override
    public String toString() {
        return "(" + floorNumber + ", " + direction + ")";
    }
}