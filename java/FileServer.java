import java.net.*;
import java.io.*;
import java.util.*;

public class FileServer {
    public static void main(String[] args) {
        // read arguments
        if (args.length != 2) {
            System.out.println("Usage: java -jar fileserver <port> <wwwhome>");
            System.exit(-1);
        }
        int port = Integer.parseInt(args[0]);
        String wwwhome = args[1];

        // open server socket
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Could not start server: " + e);
            System.exit(-1);
        }
        System.out.println("fileserver accepting connections on port " + port);

        // request handler loop
        while (true) {
            Socket connection = null;
            try {
                // wait for request
                connection = socket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        connection.getInputStream()));
                OutputStream out = new BufferedOutputStream(
                        connection.getOutputStream());
                PrintStream pout = new PrintStream(out);

                // read first line of request
                String request = in.readLine() + " \n";
                log(connection, request);
                while (true) {
                    String misc = in.readLine();
                    if (misc == null || misc.length() == 0)
                        break;
                    if (misc.startsWith("Host:")) {
                        continue;
                    }
                    request += misc + "\n";
                }

                // parse the line
                if (request.length() < 14) {
                    // bad request
                    errorReport(pout, connection, "400", "Bad Request",
                            "Your browser sent a request that "
                                    + "this server could not understand.");
                } else {
                    String[] requestParts = request.split(" ");
                    String req = requestParts[1];
                    if (req.indexOf("..") != -1 || req.indexOf("/.ht") != -1
                            || req.endsWith("~")) {
                        // evil hacker trying to read non-wwwhome or secret file
                        errorReport(pout, connection, "403", "Forbidden",
                                "You don't have permission to access the requested URL.");
                    } else if (req.startsWith("/proxy.php")) {
                        proxy(in, pout, requestParts);
                    } else if (req.startsWith("/_proxy/")) {
                        proxy2(in, pout, requestParts);
                    } else {
                        String path = wwwhome + "/" + req;
                        File f = new File(path);
                        if (f.isDirectory() && !path.endsWith("/")) {
                            // redirect browser if referring to directory
                            // without final '/'
                            pout.print("HTTP/1.0 301 Moved Permanently\r\n"
                                    + "Location: http://"
                                    + connection.getLocalAddress()
                                            .getHostAddress() + ":"
                                    + connection.getLocalPort() + "/" + req
                                    + "/\r\n\r\n");
                            log(connection, "301 Moved Permanently");
                        } else {
                            if (f.isDirectory()) {
                                // if directory, implicitly add 'index.html'
                                path = path + "index.html";
                                f = new File(path);
                            }
                            try {
                                // send file
                                InputStream file = new FileInputStream(f);
                                pout.print("HTTP/1.0 200 OK\r\n"
                                        + "Content-Type: "
                                        + guessContentType(path) + "\r\n"
                                        + "Date: " + new Date() + "\r\n"
                                        + "Server: FileServer 1.0\r\n\r\n");
                                sendFile(file, out); // send raw file
                                log(connection, "200 OK");
                            } catch (FileNotFoundException e) {
                                // file not found
                                errorReport(pout, connection, "404",
                                        "Not Found",
                                        "The requested URL was not found on this server.");
                            }
                        }
                    }
                }
                out.flush();
            } catch (IOException e) {
                System.err.println(e);
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    private static void proxy(BufferedReader in, PrintStream pout,
            String[] requestParts) throws UnknownHostException, IOException {
        String[] parts = requestParts[1].split("\\?proxy_url=");
        URL proxyUrl = new URL(parts[1].split(" ")[0]);
        doProxy(in, pout, requestParts, proxyUrl);
    }

    private static void proxy2(BufferedReader in, PrintStream pout,
            String[] requestParts) throws UnknownHostException, IOException {
        String[] parts = requestParts[1].split("_proxy/");
        URL proxyUrl = new URL("http://" + parts[1].split(" ")[0]);
        doProxy(in, pout, requestParts, proxyUrl);
    }

    private static void doProxy(BufferedReader in, PrintStream pout,
            String[] requestParts, URL proxyUrl) throws UnknownHostException,
            IOException {
        Socket sock = new Socket(proxyUrl.getHost(),
                proxyUrl.getPort() == -1 ? 80 : proxyUrl.getPort());
        PrintWriter sockOut = new PrintWriter(sock.getOutputStream(), true);
        // StringWriter sockOut = new StringWriter();
        InputStream sockIn = sock.getInputStream();
        sockOut.write(requestParts[0] + " "
                + (proxyUrl.getPath().equals("") ? "/" : proxyUrl.getPath()) + (proxyUrl.getQuery() == null ? "" : "?" + proxyUrl.getQuery())
                + " HTTP/1.1\n");
        sockOut.write("Host: " + proxyUrl.getHost());
        for (int i = 3; i < requestParts.length; i++) {
            sockOut.write(requestParts[i]);
            if (i < requestParts.length - 1) {
                sockOut.write(' ');
            }
        }
        sockOut.write("\n\r");

        char[] buf = new char[1024];
        int numRead;
        while (in.ready()) {
            numRead = in.read(buf, 0, 1024);
            sockOut.write(buf, 0, numRead);
        }
        // sockOut.close();
        sockOut.flush();

        // Now read the result
        byte[] bbuf = new byte[1024];
        while ((numRead = sockIn.read(bbuf, 0, 1024)) > 0) {
            pout.write(bbuf, 0, numRead);
        }
        sockIn.close();
    }

    private static void log(Socket connection, String msg) {
        System.err.println(new Date() + " ["
                + connection.getInetAddress().getHostAddress() + ":"
                + connection.getPort() + "] " + msg);
    }

    private static void errorReport(PrintStream pout, Socket connection,
            String code, String title, String msg) {
        pout.print("HTTP/1.0 " + code + " " + title + "\r\n" + "\r\n"
                + "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\r\n"
                + "<TITLE>" + code + " " + title + "</TITLE>\r\n"
                + "</HEAD><BODY>\r\n" + "<H1>" + title + "</H1>\r\n" + msg
                + "<P>\r\n" + "<HR><ADDRESS>FileServer 1.0 at "
                + connection.getLocalAddress().getHostName() + " Port "
                + connection.getLocalPort() + "</ADDRESS>\r\n"
                + "</BODY></HTML>\r\n");
        log(connection, code + " " + title);
    }

    private static String guessContentType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm"))
            return "text/html";
        else if (path.endsWith(".txt") || path.endsWith(".java"))
            return "text/plain";
        else if (path.endsWith(".gif"))
            return "image/gif";
        else if (path.endsWith(".png"))
            return "image/png";
        else if (path.endsWith(".class"))
            return "application/octet-stream";
        else if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            return "image/jpeg";
        else if (path.endsWith(".css"))
            return "text/css";
        else if (path.endsWith(".js"))
            return "application/javascript";
        else if (path.endsWith(".manifest"))
            return "text/cache-manifest";
        else
            return "text/plain";
    }

    private static void sendFile(InputStream file, OutputStream out) {
        try {
            byte[] buffer = new byte[1000];
            while (file.available() > 0)
                out.write(buffer, 0, file.read(buffer));
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
