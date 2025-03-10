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

    public static Schedule loadScheduleFromXML(String filePath) {
        Schedule schedule = new Schedule();
        Set<Integer> teamIndexes = new HashSet<>();

        try {
            // Load the XML file
            File xmlFile = new File(filePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

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
                    schedule.getSchedule().computeIfAbsent(slot, k -> new ArrayList<>()).add(match);
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

    public void addOptimalNL4Schedule() {
        // Create a feasible schedule
        List<Team> teams = Arrays.asList(
                new Team("Team 1"), new Team("Team 2"), new Team("Team 3"),
                new Team("Team 4")
        );

        this.addMatches(1, Arrays.asList(
                new Match(teams.get(0), teams.get(2)),
                new Match(teams.get(1), teams.get(3))
        ));

        this.addMatches(2, Arrays.asList(
                new Match(teams.get(0), teams.get(1)),
                new Match(teams.get(2), teams.get(3))
        ));

        this.addMatches(3, Arrays.asList(
                new Match(teams.get(0), teams.get(3)),
                new Match(teams.get(2), teams.get(1))
        ));

        this.addMatches(4, Arrays.asList(
                new Match(teams.get(2), teams.get(0)),
                new Match(teams.get(3), teams.get(1))
        ));

        this.addMatches(5, Arrays.asList(
                new Match(teams.get(1), teams.get(0)),
                new Match(teams.get(3), teams.get(2))
        ));

        this.addMatches(6, Arrays.asList(
                new Match(teams.get(1), teams.get(2)),
                new Match(teams.get(3), teams.get(0))
        ));
    }

    public void addOptimalNL6Schedule() {
        // Create a feasible schedule
        List<Team> teams = Arrays.asList(
                new Team("Team 1"), new Team("Team 2"), new Team("Team 3"),
                new Team("Team 4"), new Team("Team 5"), new Team("Team 6")
        );

        this.addMatches(1, Arrays.asList(
                new Match(teams.get(0), teams.get(1)),
                new Match(teams.get(4), teams.get(2)),
                new Match(teams.get(3), teams.get(5))
        ));

        this.addMatches(2, Arrays.asList(
                new Match(teams.get(4), teams.get(1)),
                new Match(teams.get(5), teams.get(2)),
                new Match(teams.get(0), teams.get(3))
        ));

        this.addMatches(3, Arrays.asList(
                new Match(teams.get(4), teams.get(0)),
                new Match(teams.get(2), teams.get(1)),
                new Match(teams.get(5), teams.get(3))
        ));

        this.addMatches(4, Arrays.asList(
                new Match(teams.get(2), teams.get(0)),
                new Match(teams.get(1), teams.get(3)),
                new Match(teams.get(5), teams.get(4))
        ));

        this.addMatches(5, Arrays.asList(
                new Match(teams.get(3), teams.get(2)),
                new Match(teams.get(0), teams.get(4)),
                new Match(teams.get(1), teams.get(5))
        ));

        this.addMatches(6, Arrays.asList(
                new Match(teams.get(1), teams.get(2)),
                new Match(teams.get(4), teams.get(3)),
                new Match(teams.get(0), teams.get(5))
        ));

        this.addMatches(7, Arrays.asList(
                new Match(teams.get(3), teams.get(1)),
                new Match(teams.get(0), teams.get(2)),
                new Match(teams.get(4), teams.get(5))
        ));

        this.addMatches(8, Arrays.asList(
                new Match(teams.get(3), teams.get(0)),
                new Match(teams.get(5), teams.get(1)),
                new Match(teams.get(2), teams.get(4))
        ));

        this.addMatches(9, Arrays.asList(
                new Match(teams.get(5), teams.get(0)),
                new Match(teams.get(2), teams.get(3)),
                new Match(teams.get(1), teams.get(4))
        ));

        this.addMatches(10, Arrays.asList(
                new Match(teams.get(1), teams.get(0)),
                new Match(teams.get(3), teams.get(4)),
                new Match(teams.get(2), teams.get(5))
        ));
    }

    public void addOptimalNL8Schedule() {
        // Create a feasible schedule
        List<Team> teams = Arrays.asList(
                new Team("Team 1"), new Team("Team 2"), new Team("Team 3"),
                new Team("Team 4"), new Team("Team 5"), new Team("Team 6"),
                new Team("Team 7"), new Team("Team 8")
        );

        this.addMatches(1, Arrays.asList(
                new Match(teams.get(0), teams.get(0)),
                new Match(teams.get(0), teams.get(0)),
                new Match(teams.get(0), teams.get(0)),
                new Match(teams.get(0), teams.get(0))
        ));

        this.addMatches(2, Arrays.asList(
                new Match(teams.get(0), teams.get(0)),
                new Match(teams.get(0), teams.get(0)),
                new Match(teams.get(0), teams.get(0)),
                new Match(teams.get(0), teams.get(0))
        ));

        this.addMatches(3, Arrays.asList(
                new Match(teams.get(0), teams.get(0)),
                new Match(teams.get(0), teams.get(0)),
                new Match(teams.get(0), teams.get(0)),
                new Match(teams.get(0), teams.get(0))
        ));

        this.addMatches(4, Arrays.asList(
                new Match(teams.get(2), teams.get(0)),
                new Match(teams.get(1), teams.get(3)),
                new Match(teams.get(5), teams.get(4)),
                new Match(teams.get(3), teams.get(5))
        ));

        this.addMatches(5, Arrays.asList(
                new Match(teams.get(3), teams.get(2)),
                new Match(teams.get(0), teams.get(4)),
                new Match(teams.get(1), teams.get(5)),
                new Match(teams.get(3), teams.get(5))
        ));

        this.addMatches(6, Arrays.asList(
                new Match(teams.get(1), teams.get(2)),
                new Match(teams.get(4), teams.get(3)),
                new Match(teams.get(0), teams.get(5)),
                new Match(teams.get(3), teams.get(5))
        ));

        this.addMatches(7, Arrays.asList(
                new Match(teams.get(3), teams.get(1)),
                new Match(teams.get(0), teams.get(2)),
                new Match(teams.get(4), teams.get(5)),
                new Match(teams.get(3), teams.get(5))
        ));

        this.addMatches(8, Arrays.asList(
                new Match(teams.get(3), teams.get(0)),
                new Match(teams.get(5), teams.get(1)),
                new Match(teams.get(2), teams.get(4)),
                new Match(teams.get(3), teams.get(5))
        ));

        this.addMatches(9, Arrays.asList(
                new Match(teams.get(5), teams.get(0)),
                new Match(teams.get(2), teams.get(3)),
                new Match(teams.get(1), teams.get(4)),
                new Match(teams.get(3), teams.get(5))
        ));

        this.addMatches(10, Arrays.asList(
                new Match(teams.get(1), teams.get(0)),
                new Match(teams.get(3), teams.get(4)),
                new Match(teams.get(2), teams.get(5)),
                new Match(teams.get(3), teams.get(5))
        ));

        this.addMatches(11, Arrays.asList(
                new Match(teams.get(1), teams.get(0)),
                new Match(teams.get(3), teams.get(4)),
                new Match(teams.get(2), teams.get(5)),
                new Match(teams.get(3), teams.get(5))
        ));

        this.addMatches(12, Arrays.asList(
                new Match(teams.get(1), teams.get(0)),
                new Match(teams.get(3), teams.get(4)),
                new Match(teams.get(2), teams.get(5)),
                new Match(teams.get(3), teams.get(5))
        ));

        this.addMatches(13, Arrays.asList(
                new Match(teams.get(1), teams.get(0)),
                new Match(teams.get(3), teams.get(4)),
                new Match(teams.get(2), teams.get(5)),
                new Match(teams.get(3), teams.get(5))
        ));

        this.addMatches(14, Arrays.asList(
                new Match(teams.get(1), teams.get(0)),
                new Match(teams.get(3), teams.get(4)),
                new Match(teams.get(2), teams.get(5)),
                new Match(teams.get(3), teams.get(5))
        ));
    }

    public Map<Integer, List<Match>> getSchedule() {
        return schedule;
    }
}
