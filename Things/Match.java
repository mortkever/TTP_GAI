package Things;
public class Match {
    private Team teamHome;
    private Team teamAway;

    public Match(Team teamHome, Team teamAway) {
        this.teamHome = teamHome;
        this.teamAway = teamAway;
    }

    public Team getTeamHome() {
        return teamHome;
    }

    public Team getTeamAway() {
        return teamAway;
    }

    public void setTeamHome(Team teamHome) {
        this.teamHome = teamHome;
    }

    public void setTeamAway(Team teamAway) {
        this.teamAway = teamAway;
    }


    @Override
    public String toString() {
        return teamHome.getName() + " vs " + teamAway.getName();
    }
}
