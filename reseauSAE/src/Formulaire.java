import java.io.*;
import java.util.List;
import java.util.ArrayList;

public class Formulaire {

    public static void gererForm(String argsString, OutputStream out) {
        String name = null;
        String mail = null;
        String[] args = argsString.isEmpty() ? new String[0] : argsString.split("&");

        for (String arg : args) {
            if (arg.startsWith("user_name=")) name = arg.substring(10);
            if (arg.startsWith("user_mail=")) mail = arg.substring(10);
        }

        try (FileWriter fw = new FileWriter("documents/donnees.txt", true)) {
            if (name != null && mail != null) {
                fw.write(name + " ; " + mail + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Réponse</title></head><body>");

        if (name != null && mail != null) {
            html.append("<h1>Bonjour ").append(name).append("</h1>");
            html.append("<p>Votre adresse email est : ").append(mail).append("</p>");
        } else {
            html.append("<h1 style='color:red;'>Erreur : nom ou email manquant.</h1>");
        }

        html.append("<form action='/form.html'><button type='submit'>Suivant</button></form>");
        html.append("<form action='/index.html'><button type='submit'>Fin</button></form>");
        html.append("</body></html>");

        try {
            out.write("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n".getBytes());
            out.write(html.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}