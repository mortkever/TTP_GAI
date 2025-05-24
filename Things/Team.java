package Things;
public class Team {
    private String name;
    private Integer ID;


    public Team(String name) {
        this.name = name;
        this.ID = Integer.parseInt(name.substring(5)) - 1; // Team 1 -> 0, Team 2 -> 1, ...
    }

    public String getName() {
        return name;
    }
    public Integer getID() {
        return ID;
    }

    @Override
    public String toString() {
        return name;
    }
}
