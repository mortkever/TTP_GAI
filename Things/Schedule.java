package Things;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;



public class Schedule {
    private Map<Integer, List<Match>> schedule;
    private int objectiveValue;

    public Schedule(Map<Integer, List<Match>> shedule) {
        this.schedule = shedule;
    }

    public Schedule() {
        this.schedule = new HashMap<>();
    }

    public void addMatches(int round, List<Match> matches) {
        schedule.put(round, matches);
    }

    public List<Match> getMatches(int round) {
        return schedule.getOrDefault(round, new ArrayList<>());
    }

    public Set<Team> getTeams() {
        // Get all teams from the schedule. They are all present in the first round (or any round)
        Set<Team> teams = new HashSet<>();

        // Get the first round
        Integer firstKey = this.schedule.keySet().iterator().next();
        List<Match> firstRound = this.schedule.get(firstKey);

        // Add the teams
        for (Match match : firstRound) {
            teams.add(match.getTeamHome());
            teams.add(match.getTeamAway());
        }
        return teams;
    }
    public int getObjectiveValue() {
        return objectiveValue;
    }
    public void setObjectiveValue(int objectiveValue) {
        this.objectiveValue = objectiveValue;
    }

    public static Schedule loadScheduleFromXML(String filePath) {
        // Works with Schedules from "robinxval.ugent.be/RobinX"
        Schedule schedule = new Schedule();
        Set<Integer> teamIndexes = new HashSet<>();

        try {
            // Load the XML file
            File xmlFile = new File(filePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Extract the "objective" value
            Element objectiveElement = (Element) doc.getElementsByTagName("ObjectiveValue").item(0);
            if (objectiveElement != null && objectiveElement.hasAttribute("objective")) {
                schedule.setObjectiveValue(Integer.parseInt(objectiveElement.getAttribute("objective")));
            }

            // Get the list of ScheduledMatch elements
            NodeList matchNodes = doc.getElementsByTagName("ScheduledMatch");

            // First pass: Determine the number of teams dynamically
            for (int i = 0; i < matchNodes.getLength(); i++) {
                Node node = matchNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    int homeIndex = Integer.parseInt(element.getAttribute("home"));
                    int awayIndex = Integer.parseInt(element.getAttribute("away"));

                    teamIndexes.add(homeIndex);
                    teamIndexes.add(awayIndex);
                }
            }

            // Determine total number of teams dynamically
            int numTeams = teamIndexes.size();

            // Create a list of teams dynamically
            List<Team> teams = new ArrayList<>();
            for (int i = 0; i < numTeams; i++) {
                teams.add(new Team("Team " + (i + 1)));
            }

            // Second pass: Create matches and add them to the schedule
            for (int i = 0; i < matchNodes.getLength(); i++) {
                Node node = matchNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    // Read attributes
                    int homeIndex = Integer.parseInt(element.getAttribute("home"));
                    int awayIndex = Integer.parseInt(element.getAttribute("away"));
                    int slot = Integer.parseInt(element.getAttribute("slot"));

                    // Create the match object
                    Match match = new Match(teams.get(homeIndex), teams.get(awayIndex));

                    // Add match to the correct round in the schedule
                    schedule.getSchedule().computeIfAbsent(slot+1, k -> new ArrayList<>()).add(match);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return schedule;
    }

    public void printSchedule() {
        for (Map.Entry<Integer, List<Match>> entry : schedule.entrySet()) {
            System.out.println("Ronde " + entry.getKey() + ":");
            for (Match match : entry.getValue()) {
                System.out.println("  " + match);
            }
            System.out.println();
        }
    }

    public Map<Integer, List<Match>> getSchedule() {
        return schedule;
    }
}
