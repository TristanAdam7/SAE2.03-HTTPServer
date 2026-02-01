import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class Log {

    Config configSite;

    public Log(Config c) {
        configSite = c;

        // Création du fichier de logAccess.log
        File fileAccess = new File(c.getAccessLog());
        if (!fileAccess.exists()) {
            try {
                fileAccess.createNewFile();
            } catch (IOException e) {
                System.out.println("Erreur de création du fichier logAccess" + e.getMessage());
            }
        }

        // Création du fichier de logError.log
        File fileError = new File(c.getErrorLog());
        if (!fileError.exists()) {
            try {
                fileError.createNewFile();
            } catch (IOException e) {
                System.out.println("Erreur de création du fichier logError" + e.getMessage());
            }
        }
    }

    private void log(String fileName, String message) {
        try {
            FileWriter fw = new FileWriter(fileName, true);
            fw.write("[" + new Date() + "] " + message + "\n");
            fw.close();
        } catch (IOException e) {
            System.out.println("Erreur d'écriture du log : " + e.getMessage());
        }
    }

    public void logAccess(String message) {
        log(configSite.getAccessLog(), message);
    }

    public void logError(String message) {
        log(configSite.getErrorLog(), message);
    }
}
