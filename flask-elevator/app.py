import os
import random
from enum import Enum
from typing import List, Set, Optional, Dict, Any
from flask import Flask, render_template, jsonify, request

# Explicitly bind template folder to prevent TemplateNotFound errors
BASE_DIR = os.path.abspath(os.path.dirname(__file__))
TEMPLATE_DIR = os.path.join(BASE_DIR, "templates")

app = Flask(__name__, template_folder=TEMPLATE_DIR)

# ==============================================================================
# Domain Model (1:1 Port of Java Architecture)
# ==============================================================================

class Direction(str, Enum):
    IDLE = "IDLE"
    UP = "UP"
    DOWN = "DOWN"

    def __str__(self):
        return self.value


class FloorRequest:
    def __init__(self, floor_number: int, direction: Direction):
        self.floor_number = floor_number
        self.direction = direction

    def __eq__(self, other):
        if not isinstance(other, FloorRequest):
            return False
        return self.floor_number == other.floor_number and self.direction == other.direction

    def __hash__(self):
        return hash((self.floor_number, self.direction))

    def __repr__(self):
        return f"({self.floor_number}, {self.direction})"

    def to_dict(self):
        return {"floorNumber": self.floor_number, "direction": str(self.direction)}


class Button:
    def __init__(self):
        self.pressed: bool = False

    def push(self):
        self.pressed = True

    def reset(self):
        self.pressed = False

    def is_pressed(self) -> bool:
        return self.pressed


class UpDownButton(Button):
    def __init__(self, direction: Direction, floor):
        super().__init__()
        self.direction = direction
        self.floor = floor


class DestButton(Button):
    def __init__(self, floor_number: int, elevator):
        super().__init__()
        self.floor_number = floor_number
        self.elevator = elevator


class Floor:
    def __init__(self, floor_number: int, building):
        if floor_number < 0:
            raise ValueError("'floor_number' must be non-negative.")
        self.floor_number: int = floor_number
        self.building = building
        self.waiting: List['Rider'] = []

    def add_rider_waiting(self, rider: 'Rider'):
        if rider not in self.waiting:
            self.waiting.append(rider)

    def remove_rider_waiting(self, rider: 'Rider'):
        if rider in self.waiting:
            self.waiting.remove(rider)

    def press_up_button(self): pass
    def press_down_button(self): pass
    def is_up_button_pressed(self) -> bool: return False
    def is_down_button_pressed(self) -> bool: return False
    def reset_up_button(self): pass
    def reset_down_button(self): pass


class Lobby(Floor):
    def __init__(self, floor_number: int, building):
        super().__init__(floor_number, building)
        self.up_button = UpDownButton(Direction.UP, self)

    def press_up_button(self):
        self.up_button.push()
        self.building.add_floor_request(self.floor_number, Direction.UP)

    def is_up_button_pressed(self) -> bool:
        return self.up_button.is_pressed()

    def reset_up_button(self):
        self.up_button.reset()


class MiddleFloor(Floor):
    def __init__(self, floor_number: int, building):
        super().__init__(floor_number, building)
        self.up_button = UpDownButton(Direction.UP, self)
        self.down_button = UpDownButton(Direction.DOWN, self)

    def press_up_button(self):
        self.up_button.push()
        self.building.add_floor_request(self.floor_number, Direction.UP)

    def press_down_button(self):
        self.down_button.push()
        self.building.add_floor_request(self.floor_number, Direction.DOWN)

    def is_up_button_pressed(self) -> bool:
        return self.up_button.is_pressed()

    def is_down_button_pressed(self) -> bool:
        return self.down_button.is_pressed()

    def reset_up_button(self):
        self.up_button.reset()

    def reset_down_button(self):
        self.down_button.reset()


class Roof(Floor):
    def __init__(self, floor_number: int, building):
        super().__init__(floor_number, building)
        self.down_button = UpDownButton(Direction.DOWN, self)

    def press_down_button(self):
        self.down_button.push()
        self.building.add_floor_request(self.floor_number, Direction.DOWN)

    def is_down_button_pressed(self) -> bool:
        return self.down_button.is_pressed()

    def reset_down_button(self):
        self.down_button.reset()


class Rider:
    _next_id = 1

    def __init__(self, building, origin_floor_num: int, dest_floor_num: int, weight: int):
        self.id = Rider._next_id
        Rider._next_id += 1

        self.weight = weight
        self.current_floor_num = origin_floor_num
        self.dest_floor_num = dest_floor_num
        self.building = building
        self.elevator: Optional['Elevator'] = None

        if (origin_floor_num < 0 or origin_floor_num >= building.get_num_floors() or
                dest_floor_num < 0 or dest_floor_num >= building.get_num_floors()):
            raise ValueError("Invalid origin or destination floor for Rider.")

        building.get_floor(self.current_floor_num).add_rider_waiting(self)
        building.add_occupant(self)
        building.log(f"Rider {self.id} created at {building.get_floor_name(self.current_floor_num)}, destination: {building.get_floor_name(self.dest_floor_num)}")

    def request_elevator(self):
        if self.elevator is not None:
            self.building.log(f"Rider {self.id}: Already in elevator {self.elevator.id}, cannot request another.")
            return

        if self.current_floor_num == self.dest_floor_num:
            self.building.log(f"Rider {self.id}: Already at destination, no request needed.")
            return

        floor_obj = self.building.get_floor(self.current_floor_num)
        if self.dest_floor_num > self.current_floor_num:
            self.building.log(f"Rider {self.id} at {self.building.get_floor_name(self.current_floor_num)} requests UP call")
            floor_obj.press_up_button()
        elif self.dest_floor_num < self.current_floor_num:
            self.building.log(f"Rider {self.id} at {self.building.get_floor_name(self.current_floor_num)} requests DOWN call")
            floor_obj.press_down_button()

    def enter(self, elevator_obj: 'Elevator'):
        if self.current_floor_num != elevator_obj.current_floor:
            raise RuntimeError("Elevator is not at the rider's current floor.")
        if self.weight + elevator_obj.current_weight > elevator_obj.weight_limit:
            raise RuntimeError("Elevator would be over weight limit.")

        self.building.get_floor(self.current_floor_num).remove_rider_waiting(self)
        elevator_obj.occupants.append(self)
        self.elevator = elevator_obj
        elevator_obj.current_weight += self.weight

    def exit_elev(self, elevator_obj: Optional['Elevator'] = None):
        target = elevator_obj if elevator_obj is not None else self.elevator
        if target is None:
            raise RuntimeError("Rider is not in an elevator.")

        if self in target.occupants:
            target.occupants.remove(self)

        self.current_floor_num = target.current_floor
        if self.current_floor_num != self.dest_floor_num:
            self.building.get_floor(self.current_floor_num).add_rider_waiting(self)

        target.current_weight -= self.weight
        self.elevator = None

        if self.current_floor_num == self.dest_floor_num:
            self.building.log(f"Rider {self.id} arrived at destination {self.building.get_floor_name(self.current_floor_num)}.")

    def to_dict(self):
        return {
            "id": self.id,
            "weight": self.weight,
            "currentFloor": self.current_floor_num,
            "destFloor": self.dest_floor_num,
            "elevatorId": self.elevator.id if self.elevator else None
        }


class Elevator:
    DOOR_OPEN_DURATION = 2

    def __init__(self, building, elevator_id: int, weight_limit: int = 2000):
        self.id = elevator_id
        self.weight_limit = weight_limit
        self.building = building
        self.current_floor = 0
        self.current_weight = 0
        self.occupants: List[Rider] = []
        self.direction = Direction.IDLE
        self.destination_requests: Set[int] = set()
        self.pickup_requests: List[FloorRequest] = []
        self.buttons: List[DestButton] = [DestButton(i, self) for i in range(building.get_num_floors())]
        self.doors_open = False
        self.door_timer = 0

    def press_destination_button(self, floor_number: int):
        if 0 <= floor_number < self.building.get_num_floors():
            self.buttons[floor_number].push()
            if floor_number != self.current_floor:
                self.destination_requests.add(floor_number)
                self.building.log(f"Elevator {self.id}: Destination set to {self.building.get_floor_name(floor_number)}")

    def assign_pickup_request(self, floor_number: int, direction: Direction):
        req = FloorRequest(floor_number, direction)
        if req not in self.pickup_requests:
            self.pickup_requests.append(req)
            self.building.log(f"Elevator {self.id}: Assigned pickup at {self.building.get_floor_name(floor_number)} going {direction}")

    def has_destinations_above(self) -> bool:
        return any(f > self.current_floor for f in self.destination_requests)

    def has_destinations_below(self) -> bool:
        return any(f < self.current_floor for f in self.destination_requests)

    def has_requests_above(self) -> bool:
        if self.has_destinations_above():
            return True
        return any(r.floor_number > self.current_floor for r in self.pickup_requests)

    def has_requests_below(self) -> bool:
        if self.has_destinations_below():
            return True
        return any(r.floor_number < self.current_floor for r in self.pickup_requests)

    def get_all_stops(self) -> Set[int]:
        stops = set(self.destination_requests)
        for req in self.pickup_requests:
            stops.add(req.floor_number)
        return stops

    def get_next_target_floor(self) -> Optional[int]:
        if not self.destination_requests and not self.pickup_requests:
            return None

        if self.direction == Direction.UP:
            valid_stops_above = [f for f in self.destination_requests if f >= self.current_floor]
            for r in self.pickup_requests:
                if r.floor_number >= self.current_floor and r.direction == Direction.UP:
                    valid_stops_above.append(r.floor_number)

            if not self.has_destinations_above():
                highest_down = -1
                for r in self.pickup_requests:
                    if r.direction == Direction.DOWN and r.floor_number > highest_down:
                        highest_down = r.floor_number
                if highest_down >= self.current_floor:
                    has_up_above = any(r.direction == Direction.UP and r.floor_number > highest_down for r in self.pickup_requests)
                    if not has_up_above:
                        valid_stops_above.append(highest_down)

            if valid_stops_above:
                return min(valid_stops_above)

            all_stops = self.get_all_stops()
            return min(all_stops) if all_stops else None

        elif self.direction == Direction.DOWN:
            valid_stops_below = [f for f in self.destination_requests if f <= self.current_floor]
            for r in self.pickup_requests:
                if r.floor_number <= self.current_floor and r.direction == Direction.DOWN:
                    valid_stops_below.append(r.floor_number)

            if not self.has_destinations_below():
                lowest_up = self.building.get_num_floors() + 1
                for r in self.pickup_requests:
                    if r.direction == Direction.UP and r.floor_number < lowest_up:
                        lowest_up = r.floor_number
                if lowest_up <= self.current_floor:
                    has_down_below = any(r.direction == Direction.DOWN and r.floor_number < lowest_up for r in self.pickup_requests)
                    if not has_down_below:
                        valid_stops_below.append(lowest_up)

            if valid_stops_below:
                return max(valid_stops_below)

            all_stops = self.get_all_stops()
            return max(all_stops) if all_stops else None

        else:  # IDLE
            all_stops = self.get_all_stops()
            if not all_stops:
                return None
            closest = None
            min_dist = self.building.get_num_floors() + 1
            for floor in all_stops:
                dist = abs(floor - self.current_floor)
                if dist < min_dist or (dist == min_dist and floor == self.current_floor):
                    min_dist = dist
                    closest = floor
            return closest

    def can_rider_board(self, rider_req_dir: Direction) -> bool:
        if self.direction == Direction.IDLE:
            return True
        if self.direction == Direction.UP:
            if rider_req_dir == Direction.UP:
                return True
            if rider_req_dir == Direction.DOWN:
                return not self.has_requests_above()
        if self.direction == Direction.DOWN:
            if rider_req_dir == Direction.DOWN:
                return True
            if rider_req_dir == Direction.UP:
                return not self.has_requests_below()
        return False

    def should_stop_at_current_floor(self) -> bool:
        if self.current_floor in self.destination_requests:
            return True
        if not self.pickup_requests:
            return False

        target = self.get_next_target_floor()
        if target is not None and target == self.current_floor:
            return True

        for req in self.pickup_requests:
            if req.floor_number == self.current_floor and req.direction == self.direction:
                return True
        return False

    def handle_alighting(self):
        for rider in list(self.occupants):
            if rider.dest_floor_num == self.current_floor:
                rider.exit_elev(self)
                self.buttons[self.current_floor].reset()
                self.building.log(f"Elevator {self.id}: Rider {rider.id} exited at {self.building.get_floor_name(self.current_floor)}")
        self.destination_requests.discard(self.current_floor)

    def handle_boarding(self):
        current_floor_obj = self.building.get_floor(self.current_floor)

        for rider in list(current_floor_obj.waiting):
            if self.current_weight + rider.weight <= self.weight_limit:
                rider_wants_up = rider.dest_floor_num > self.current_floor
                rider_req_dir = Direction.UP if rider_wants_up else Direction.DOWN

                if self.can_rider_board(rider_req_dir):
                    pickup_to_clear = None
                    for pr in self.pickup_requests:
                        if pr.floor_number == self.current_floor and pr.direction == rider_req_dir:
                            pickup_to_clear = pr
                            break

                    rider.enter(self)
                    self.building.log(f"Elevator {self.id}: Rider {rider.id} boarded (Dest: {self.building.get_floor_name(rider.dest_floor_num)})")
                    self.press_destination_button(rider.dest_floor_num)

                    if pickup_to_clear and pickup_to_clear in self.pickup_requests:
                        self.pickup_requests.remove(pickup_to_clear)
                        self.building.clear_floor_request(pickup_to_clear.floor_number, pickup_to_clear.direction)

        pickups_at_this_floor = [
            pr for pr in self.pickup_requests
            if pr.floor_number == self.current_floor and self.can_rider_board(pr.direction)
        ]

        for pr in pickups_at_this_floor:
            has_waiting_riders = False
            for w in current_floor_obj.waiting:
                w_dir = Direction.UP if w.dest_floor_num > self.current_floor else Direction.DOWN
                if w_dir == pr.direction:
                    has_waiting_riders = True
                    break
            if not has_waiting_riders:
                if pr in self.pickup_requests:
                    self.pickup_requests.remove(pr)
                    self.building.clear_floor_request(pr.floor_number, pr.direction)
                    self.building.log(f"Elevator {self.id}: Cleared pickup {pr}")

    def update(self):
        if self.doors_open:
            self.door_timer += 1
            if self.door_timer >= self.DOOR_OPEN_DURATION:
                self.doors_open = False
                self.door_timer = 0
                self.building.log(f"Elevator {self.id}: Doors closing at {self.building.get_floor_name(self.current_floor)}")
            else:
                self.handle_boarding()
                return

        next_target = self.get_next_target_floor()
        if next_target is not None:
            if next_target > self.current_floor:
                self.direction = Direction.UP
            elif next_target < self.current_floor:
                self.direction = Direction.DOWN
        else:
            self.direction = Direction.IDLE

        if self.should_stop_at_current_floor():
            if not self.doors_open:
                self.doors_open = True
                self.door_timer = 0
                self.building.log(f"Elevator {self.id}: Doors opening at {self.building.get_floor_name(self.current_floor)}")
                self.handle_alighting()
            return

        if self.direction == Direction.UP:
            self.current_floor += 1
            self.building.log(f"Elevator {self.id}: Moving UP to {self.building.get_floor_name(self.current_floor)}")
        elif self.direction == Direction.DOWN:
            self.current_floor -= 1
            self.building.log(f"Elevator {self.id}: Moving DOWN to {self.building.get_floor_name(self.current_floor)}")

    def to_dict(self):
        return {
            "id": self.id,
            "currentFloor": self.current_floor,
            "currentWeight": self.current_weight,
            "weightLimit": self.weight_limit,
            "occupants": [r.to_dict() for r in self.occupants],
            "direction": str(self.direction),
            "doorsOpen": self.doors_open,
            "destinationRequests": list(self.destination_requests),
            "pickupRequests": [r.to_dict() for r in self.pickup_requests]
        }


class Building:
    def __init__(self, num_floors: int = 10, num_elevators: int = 1):
        if num_floors < 2 or num_elevators < 1:
            raise ValueError("Building must have >= 2 floors and >= 1 elevator.")
        self.num_floors = num_floors
        self.num_elevators = num_elevators
        self.occupants: List[Rider] = []
        self.floor_requests: Set[FloorRequest] = set()
        self.logs: List[str] = ["Simulation started."]
        self.fade_counters: Dict[int, int] = {}

        self.floors: List[Floor] = [Lobby(0, self)]
        for i in range(1, num_floors - 1):
            self.floors.append(MiddleFloor(i, self))
        self.floors.append(Roof(num_floors - 1, self))

        self.elevators: List[Elevator] = [
            Elevator(self, i + 1, weight_limit=2000) for i in range(num_elevators)
        ]

    def log(self, message: str):
        self.logs.append(message)
        if len(self.logs) > 60:
            self.logs.pop(0)

    def get_num_floors(self) -> int:
        return self.num_floors

    def get_num_elevators(self) -> int:
        return self.num_elevators

    def get_floor(self, index: int) -> Floor:
        return self.floors[index]

    def get_floor_name(self, floor_number: int) -> str:
        if floor_number == 0:
            return "Lobby"
        elif floor_number == self.num_floors - 1:
            return "Roof"
        return f"Floor {floor_number + 1}"

    def add_occupant(self, rider: Rider):
        if rider not in self.occupants:
            self.occupants.append(rider)

    def remove_occupant(self, rider: Rider):
        if rider in self.occupants:
            self.occupants.remove(rider)

    def add_floor_request(self, floor_number: int, direction: Direction):
        req = FloorRequest(floor_number, direction)
        if req not in self.floor_requests:
            self.floor_requests.add(req)
            self.log(f"Building: Received call at {self.get_floor_name(floor_number)} for {direction}.")
            self.dispatch_request(req)

    def clear_floor_request(self, floor_number: int, direction: Direction):
        req = FloorRequest(floor_number, direction)
        if req in self.floor_requests:
            self.floor_requests.remove(req)
            floor_obj = self.floors[floor_number]
            if direction == Direction.UP:
                floor_obj.reset_up_button()
            elif direction == Direction.DOWN:
                floor_obj.reset_down_button()

    def dispatch_request(self, request: FloorRequest):
        best_elev = None
        min_score = float('inf')

        for elev in self.elevators:
            dist = abs(elev.current_floor - request.floor_number)
            if elev.direction == Direction.IDLE:
                score = dist
            elif elev.direction == request.direction:
                score = dist + 5
            else:
                score = dist + 30

            if score < min_score:
                min_score = score
                best_elev = elev

        if best_elev:
            self.log(f"Building: Assigned request {request} to Elevator {best_elev.id}")
            best_elev.assign_pickup_request(request.floor_number, request.direction)

    def tick(self):
        for r in list(self.occupants):
            if r.elevator is None and r.current_floor_num == r.dest_floor_num:
                if r.id not in self.fade_counters:
                    self.fade_counters[r.id] = 2

        to_remove_ids = []
        for rider_id, ticks_left in list(self.fade_counters.items()):
            ticks_left -= 1
            if ticks_left <= 0:
                to_remove_ids.append(rider_id)
            else:
                self.fade_counters[rider_id] = ticks_left

        for rider_id in to_remove_ids:
            del self.fade_counters[rider_id]
            rider_obj = next((r for r in self.occupants if r.id == rider_id), None)
            if rider_obj:
                self.remove_occupant(rider_obj)
                self.get_floor(rider_obj.current_floor_num).remove_rider_waiting(rider_obj)
                self.log(f"Rider {rider_id} left the building.")

        for elevator in self.elevators:
            elevator.update()

        for req in list(self.floor_requests):
            is_handled = any(req in elev.pickup_requests for elev in self.elevators)
            if not is_handled:
                self.dispatch_request(req)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "numFloors": self.num_floors,
            "numElevators": self.num_elevators,
            "floorRequests": [r.to_dict() for r in self.floor_requests],
            "floors": [{
                "floorNumber": f.floor_number,
                "name": self.get_floor_name(f.floor_number),
                "upPressed": f.is_up_button_pressed(),
                "downPressed": f.is_down_button_pressed(),
                "waitingRiderIds": [r.id for r in f.waiting]
            } for f in self.floors],
            "elevators": [e.to_dict() for e in self.elevators],
            "occupants": [r.to_dict() for r in self.occupants],
            "fadeCounters": self.fade_counters,
            "logs": self.logs[-25:]
        }


# ==============================================================================
# Simulation State Instance
# ==============================================================================

building_sim = Building(num_floors=10, num_elevators=1)

# ==============================================================================
# Flask Endpoints
# ==============================================================================

@app.route("/")
def index():
    return render_template("index.html")

@app.route("/api/state", methods=["GET"])
def get_state():
    return jsonify(building_sim.to_dict())

@app.route("/api/tick", methods=["POST"])
def post_tick():
    building_sim.tick()
    return jsonify(building_sim.to_dict())

@app.route("/api/call", methods=["POST"])
def call_elevator():
    data = request.get_json() or {}
    floor = int(data.get("floor", 0))
    direction_str = data.get("direction", "UP").upper()
    dest_floor = data.get("destination")

    dir_enum = Direction.UP if direction_str == "UP" else Direction.DOWN

    if dest_floor is not None:
        dest_floor = int(dest_floor)
        if dir_enum == Direction.UP and dest_floor <= floor:
            return jsonify({"error": "Destination must be above origin"}), 400
        if dir_enum == Direction.DOWN and dest_floor >= floor:
            return jsonify({"error": "Destination must be below origin"}), 400
    else:
        if dir_enum == Direction.UP and floor < building_sim.num_floors - 1:
            dest_floor = random.randint(floor + 1, building_sim.num_floors - 1)
        elif dir_enum == Direction.DOWN and floor > 0:
            dest_floor = random.randint(0, floor - 1)
        else:
            dest_floor = floor

    if dest_floor != floor:
        random_weight = random.randint(100, 200)
        rider = Rider(building_sim, floor, dest_floor, random_weight)
        rider.request_elevator()

    return jsonify(building_sim.to_dict())

@app.route("/api/reset", methods=["POST"])
def reset_simulation():
    global building_sim
    building_sim = Building(num_floors=10, num_elevators=1)
    return jsonify(building_sim.to_dict())

if __name__ == "__main__":
    app.run(debug=True, port=5000)