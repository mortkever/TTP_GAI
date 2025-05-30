package Things;
public class Team {
    private String name;
    private int ID;


    public Team(String name) {
        this.name = name;
        this.ID = Integer.parseInt(name.substring(5)) - 1; // Team 1 -> 0, Team 2 -> 1, ...
    }

    public Team(int ID) {
        this.name = "team " + ID;
        this.ID = ID;
    }

    public String getName() {
        return name;
    }
    public int getID() {
        return ID;
    }

    @Override
    public String toString() {
        return name;
    }
}
