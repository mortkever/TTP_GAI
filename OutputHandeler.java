
// Java Program to Write XML Using DOM Parser
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBVar;

public class OutputHandeler {

    public OutputHandeler() {

    }

    public void output(GRBVar[][][][] x, int nTeams, int timeSlots, double obj) throws Exception {
        // Create a DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Create a new Document
        Document document = builder.newDocument();

        // Create root element
        Element root = document.createElement("Solution");
        document.appendChild(root);

        Element objEl = document.createElement("ObjectiveValue");
        objEl.setAttribute("objective", Integer.toString((int) obj));
        root.appendChild(objEl);
        Element games = document.createElement("Games");
        root.appendChild(games);

        // matches ipv arcs => waar teams arriveren
        Element matches[] = new Element[timeSlots];
        for (int t = 0; t < nTeams; t++) {
            for (int s = 0; s < timeSlots; s++) {
                System.out.println("\n\n\n\n\n");
                for (int i = 0; i < nTeams; i++) {
                    for (int j = 0; j < nTeams; j++) {
                        if (x[t][s][i][j].get(GRB.DoubleAttr.X) > 0.5) { // Alleen actieve variabelen tonen
                            if(t!=j){
                                matches[s] = document.createElement("ScheduledMatch");
                                matches[s].setAttribute("home", Integer.toString(j));
                                matches[s].setAttribute("away", Integer.toString(t));
                                matches[s].setAttribute("slot", Integer.toString(s));
                                games.appendChild(matches[s]);
                            }
                        }
                    }
                }
            }
        }

        // Write to XML file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);

        // Specify your local file path
        StreamResult result = new StreamResult("output.xml");
        transformer.transform(source, result);

        System.out.println("XML file created successfully!");
    }
}
