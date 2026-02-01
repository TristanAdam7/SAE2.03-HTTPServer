import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

public class HttpServer {
    public static void main(String[] args) throws IOException {
        Config config = new Config("documents/myweb.config");

        int portNumber = config.getPort();

        ServerSocket server = new ServerSocket(portNumber);
        System.out.println("Server started on port: " + portNumber);

        ArrayList<String> compressibleExtensions = new ArrayList<>();
        compressibleExtensions.add("jpg");
        compressibleExtensions.add("jpeg");
        compressibleExtensions.add("png");
        compressibleExtensions.add("gif");
        compressibleExtensions.add("bmp");
        compressibleExtensions.add("mp3");
        compressibleExtensions.add("wav");
        compressibleExtensions.add("mp4");
        compressibleExtensions.add("webm");
        compressibleExtensions.add("ogg");


        while (true) {
            // On récup le fichier
            Socket client = server.accept();
            String clientIp = client.getInetAddress().getHostAddress();
            // Covertir l'adresse de localhost en IPv6 vers IPv4
            if (clientIp.equals("0:0:0:0:0:0:0:1")) {
                clientIp = "127.0.0.1/32";
            }

            //Recharge de la config a chaque passage de la boucle
            config = new Config("documents/myweb.config");
            Log log = new Log(config);
            String root = config.getDocumentRoot();

            // Récupération des InputStream et OutputStream ainsi que la requête
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            OutputStream output = client.getOutputStream();
            String requestLine = reader.readLine();
            String[] tabRequest = requestLine.split(" ");
            String method   = tabRequest[0];
            String fullPath = tabRequest[1];
            String fileName = fullPath.startsWith("/") ? fullPath.substring(1).split("\\?", 2)[0] : fullPath;

            if (fullPath.startsWith("/sendForm")) {
                if (method.equals("GET")) {
                    String query = fullPath.contains("?") ? fullPath.split("\\?", 2)[1] : "";
                    Formulaire.gererForm(query, output);
                } else if (method.equals("POST")) {
                    int contentLength = 0;
                    String line;
                    while (!(line = reader.readLine()).isEmpty()) {
                        if (line.toLowerCase().startsWith("content-length:")) {
                            contentLength = Integer.parseInt(line.split(":", 2)[1].trim());
                        }
                    }
                    char[] body = new char[contentLength];
                    reader.read(body);
                    String postData = new String(body);
                    Formulaire.gererForm(postData, output);
                }
                client.close();
                continue;
            }

            // Fonctionnalité 6 – affichage de l'état de la machine
            if (fileName.equals("status")) {
                StringBuffer html = new StringBuffer();
                html.append("<html><head><title>État du serveur</title></head><body>");
                html.append("<h1>État de la machine</h1>");

                // Mémoire disponible
                long freeMemory = Runtime.getRuntime().freeMemory();
                html.append("<p>Mémoire disponible : ").append(freeMemory / 1024).append(" Ko</p>");

                // Espace disque disponible
                File rootFile = new File("/");
                long freeSpace = rootFile.getFreeSpace();
                html.append("<p>Espace disque disponible : ").append(freeSpace / (1024 * 1024)).append(" Mo</p>");

                // Nombre de processus
                int processCount = 0;
                try {
                    Process proc = Runtime.getRuntime().exec("tasklist");
                    BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    while (br.readLine() != null) {
                        processCount++;
                    }
                    processCount -= 3; // en général, les premières lignes sont des en-têtes
                } catch (Exception e) {
                    html.append("<p>Impossible d'obtenir la liste des processus.</p>");
                }
                html.append("<p>Nombre de processus : ").append(processCount).append("</p>");

                // Nombre d'utilisateurs connectés
                int userCount = 0;
                try {
                    Process p = Runtime.getRuntime().exec("who");
                    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while (br.readLine() != null) {
                        userCount++;
                    }
                } catch (Exception e) {
                    html.append("<p>Erreur récupération utilisateurs.</p>");
                }
                html.append("<p>Nombre d'utilisateurs connectés : ").append(userCount).append("</p>");

                html.append("</body></html>");

                output.write("HTTP/1.1 200 OK\r\n".getBytes());
                output.write("Content-Type: text/html\r\n\r\n".getBytes());
                output.write(html.toString().getBytes());

                output.flush();
                client.close();
                continue; // pour ne pas exécuter le reste
            }

            // Traitement de la requête
            File file = new File(root, fileName);
            if (file.isDirectory()) { //Si c'est un répertoire (pour '/')
                File indexFile = new File(file, "index.html");
                if (indexFile.exists()) {
                    file = indexFile;
                    fileName = "index.html";
                } else if (config.isDirectoryListingEnabled()) { // Si le listing est demandé dans le fichier de config
                    // Générer un listing HTML
                    StringBuffer listing = new StringBuffer("<html><body><ul>");
                    for (File f : file.listFiles()) {
                        listing.append("<li><a href=\"")
                                .append(fileName).append("/").append(f.getName())
                                .append("\">").append(f.getName()).append("</a></li>");
                    }
                    listing.append("</ul></body></html>");

                    output.write("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n".getBytes());
                    output.write(listing.toString().getBytes());
                    log.logAccess("Listing servi à " + clientIp);
                } else { // si le listing n'est pas demandé
                    String msgErreur = "<h1>403 - Table non demandee dans le fichier de configuration </h1>";
                    output.write("HTTP/1.1 403 Forbidden\r\n".getBytes());
                    output.write("Content-Type: text/html\r\n\r\n".getBytes());
                    output.write(msgErreur.getBytes());
                    log.logError("403 Forbidden : tentative d'accès au listing " + fileName + " sans précision dans le fichier de configuration");
                }
            }

            if (!config.isIpAllowed(clientIp)) { // Si l'ip n'est pas autorisé
                String msgErreur = "<h1>403 - Acces refuse </h1>";
                output.write("HTTP/1.1 403 Access Denied\r\n".getBytes());
                output.write("Content-Type: text/html\r\n\r\n".getBytes());
                output.write(msgErreur.getBytes());
                log.logError("403 Access Denied : " + clientIp);
            } else if (file.exists() && file.isFile()) {
                String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
                boolean compress = compressibleExtensions.contains(extension);

                if (compress) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        gzipOut.write(buffer, 0, bytesRead);
                    }
                    fis.close();
                    gzipOut.close();

                    byte[] compressedData = baos.toByteArray();

                    output.write("HTTP/1.1 200 OK\r\n".getBytes());
                    output.write(("Content-Type: " + getExtension(fileName) + "\r\n").getBytes());
                    output.write("Content-Encoding: gzip\r\n".getBytes());
                    output.write(("Content-Length: " + compressedData.length + "\r\n").getBytes());
                    output.write("\r\n".getBytes());
                    output.write(compressedData);
                } else {
                    output.write("HTTP/1.1 200 OK\r\n".getBytes());
                    output.write(("Content-Type: " + getExtension(fileName) + "\r\n\r\n").getBytes());

                    FileInputStream fileStream = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileStream.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                    fileStream.close();
                }

                log.logAccess("Fichier servi à " + clientIp + " : " + fileName);
            } else { // si le fichier n'est pas trouvé
                String msgErreur = "<h1>404 - Fichier non trouve </h1>";
                output.write("HTTP/1.1 404 Not Found\r\n".getBytes());
                output.write("Content-Type: text/html\r\n\r\n".getBytes());
                output.write(msgErreur.getBytes());
                log.logError("404 Not Found : " + fileName + ", demandé par " + clientIp);
            }

            output.flush();
            client.close();
        }
    }

    private static String getExtension(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) return "text/html";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".mp3")) return "audio/mpeg";
        if (fileName.endsWith(".wav")) return "audio/wav";
        if (fileName.endsWith(".mp4")) return "video/mp4";
        if (fileName.endsWith(".webm")) return "video/webm";
        return "application/octet-stream";
    }
}
