import java.io.File;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class Config {
    private int port = 8080;
    private String documentRoot = ".";
    private boolean enableDirectoryListing = false;
    private List<String> acceptList = new ArrayList<>();
    private List<String> rejectList = new ArrayList<>();
    private String securityOrderFirst = "accept";
    private String securityOrderLast = "reject";
    private String securityDefault = "accept";
    private String accessLog = "";
    private String errorLog = "";

    public Config(String configPath) {
        loadConfig(configPath);
    }

    private void loadConfig(String configPath) {
        try {
            File file = new File(configPath);
            if (!file.exists()) {
                System.out.println("Fichier de configuration non trouvé. Valeurs par défaut utilisées.");
                return;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();

            NodeList portNode = root.getElementsByTagName("port");
            if (portNode.getLength() > 0) {
                port = Integer.parseInt(portNode.item(0).getTextContent().trim());
            }

            NodeList docRootNode = root.getElementsByTagName("DocumentRoot");
            if (docRootNode.getLength() > 0) {
                documentRoot = docRootNode.item(0).getTextContent().trim();
            }

            NodeList dirNodes = root.getElementsByTagName("Directory");
            if (dirNodes.getLength() > 0) {
                Element dir = (Element) dirNodes.item(0);
                NodeList opts = dir.getElementsByTagName("Options");
                if (opts.getLength() > 0 && opts.item(0).getTextContent().contains("Indexes"))
                    enableDirectoryListing = true;
            }

            NodeList secNode = root.getElementsByTagName("security");
            if (secNode.getLength() > 0) {
                Element sec = (Element) secNode.item(0);

                NodeList orderNode = sec.getElementsByTagName("order");
                if (orderNode.getLength() > 0) {
                    Element order = (Element) orderNode.item(0);
                    securityOrderFirst = order.getElementsByTagName("first").item(0).getTextContent().trim();
                    securityOrderLast = order.getElementsByTagName("last").item(0).getTextContent().trim();
                }

                NodeList defNode = sec.getElementsByTagName("default");
                if (defNode.getLength() > 0) {
                    securityDefault = defNode.item(0).getTextContent().trim();
                }

                NodeList acceptNodes = sec.getElementsByTagName("accept");
                for (int i = 0; i < acceptNodes.getLength(); i++) {
                    acceptList.add(acceptNodes.item(i).getTextContent().trim());
                }

                NodeList rejectNodes = sec.getElementsByTagName("reject");
                for (int i = 0; i < rejectNodes.getLength(); i++) {
                    rejectList.add(rejectNodes.item(i).getTextContent().trim());
                }
            }

            NodeList accessNode = root.getElementsByTagName("accesslog");
            if (accessNode.getLength() > 0) {
                accessLog = accessNode.item(0).getTextContent().trim();
            }

            NodeList errorNode = root.getElementsByTagName("errorlog");
            if (errorNode.getLength() > 0) {
                errorLog = errorNode.item(0).getTextContent().trim();
            }
        } catch (Exception e) {
            System.err.println("Erreur de chargement configuration : " + e.getMessage());
        }
    }

    public int getPort() { return port; }
    public String getDocumentRoot() { return documentRoot; }
    public boolean isDirectoryListingEnabled() { return enableDirectoryListing; }
    public List<String> getAcceptList() { return acceptList; }
    public List<String> getRejectList() { return rejectList; }
    public String getSecurityOrderFirst() { return securityOrderFirst; }
    public String getSecurityOrderLast() { return securityOrderLast; }
    public String getSecurityDefault() { return securityDefault; }
    public String getAccessLog() { return accessLog; }
    public String getErrorLog() { return errorLog; }

    public boolean isIpAllowed(String ip) {
        if (securityOrderFirst.equals("accept")) {
            if (acceptList.contains(ip)) return true;
            if (rejectList.contains(ip)) return false;
        } else {
            if (rejectList.contains(ip)) return false;
            if (acceptList.contains(ip)) return true;
        }

        return securityDefault.equals("accept");
    }
}
