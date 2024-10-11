
import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import org.json.*;
import java.sql.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * INSY Webshop Server
 */
public class Server {

    /**
     * Port to bind to for HTTP service
     */
    private int port = 8000;

    /**
     * Connect to the database
     */
    Connection setupDB() {
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        Properties dbProps = new Properties();
        try {
            dbProps.load(new FileInputStream(rootPath + "db.properties"));
            String url = dbProps.getProperty("url");
            String user = dbProps.getProperty("user");
            String password = dbProps.getProperty("password");

            // Verbindung zur PostgreSQL-Datenbank herstellen
            return DriverManager.getConnection(url, user, password);
        } catch (Exception throwables) {
            throwables.printStackTrace();
        }
        return null;
    }


    /**
     * Startup the Webserver
     */
    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/articles", new ArticlesHandler());
        server.createContext("/clients", new ClientsHandler());
        server.createContext("/placeOrder", new PlaceOrderHandler());
        server.createContext("/orders", new OrdersHandler());
        server.createContext("/", new IndexHandler());

        server.start();
    }


    public static void main(String[] args) throws Throwable {
        Server webshop = new Server();
        webshop.start();
        System.out.println("Webshop running at http://127.0.0.1:" + webshop.port);
    }


    /**
     * Handler for listing all articles
     */
    class ArticlesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();
            JSONArray res = new JSONArray();

            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM articles");

                while (rs.next()) {
                    JSONObject article = new JSONObject();
                    article.put("id", rs.getInt("id"));
                    article.put("description", rs.getString("description"));
                    article.put("price", rs.getInt("price"));
                    article.put("amount", rs.getInt("amount"));
                    res.put(article);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            answerRequest(t, res.toString());
        }
    }

    /**
     * Handler for listing all clients
     */
    class ClientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();
            JSONArray res = new JSONArray();

            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM clients");

                while (rs.next()) {
                    JSONObject client = new JSONObject();
                    client.put("id", rs.getInt("id"));
                    client.put("name", rs.getString("name"));
                    client.put("address", rs.getString("address"));
                    client.put("city", rs.getString("city"));
                    client.put("country", rs.getString("country"));
                    res.put(client);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            answerRequest(t, res.toString());
        }
    }



    /**
     * Handler for listing all orders
     */
    class OrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Connection conn = setupDB();
            JSONArray res = new JSONArray();

            try {
                String query = "SELECT orders.id, clients.name AS client, COUNT(order_lines.id) AS lines, "
                        + "SUM(order_lines.amount * articles.price) AS price "
                        + "FROM orders "
                        + "JOIN clients ON orders.client_id = clients.id "
                        + "JOIN order_lines ON order_lines.order_id = orders.id "
                        + "JOIN articles ON articles.id = order_lines.article_id "
                        + "GROUP BY orders.id, clients.name";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query);

                while (rs.next()) {
                    JSONObject order = new JSONObject();
                    order.put("id", rs.getInt("id"));
                    order.put("client", rs.getString("client"));
                    order.put("lines", rs.getInt("lines"));
                    order.put("price", rs.getInt("price"));
                    res.put(order);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            answerRequest(t, res.toString());
        }
    }



    /**
     * Handler class to place an order
     */
    class PlaceOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {

            Connection conn = setupDB();
            Map <String,String> params  = queryToMap(t.getRequestURI().getQuery());

            int client_id = Integer.parseInt(params.get("client_id"));

            String response = "";
            int order_id = 1;
            try {


                //TODO Get the next free order id

                //TODO Create a new order with this id for client client_id


                for (int i = 1; i <= (params.size()-1) / 2; ++i ){
                    int article_id = Integer.parseInt(params.get("article_id_"+i));
                    int amount = Integer.parseInt(params.get("amount_"+i));


		    //TODO Get the available amount for article article_id
                    int available = 1000;


                    if (available < amount)
                        throw new IllegalArgumentException(String.format("Not enough items of article #%d available", article_id));

		    //TODO Decrease the available amount for article article_id by amount

		    //TODO Insert new order line
                }

                response = String.format("{\"order_id\": %d}", order_id);
            } catch (IllegalArgumentException iae) {
                response = String.format("{\"error\":\"%s\"}", iae.getMessage());
            }

            answerRequest(t, response);


        }
    }

    /**
     * Handler for listing static index page
     */
    class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "<!doctype html>\n" +
                    "<html><head><title>INSY Webshop</title><link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/water.css@2/out/water.css\"></head>" +
                    "<body><h1>INSY Pseudo-Webshop</h1>" +
                    "<h2>Verf&uuml;gbare Endpoints:</h2><dl>"+
                    "<dt>Alle Artikel anzeigen:</dt><dd><a href=\"http://127.0.0.1:"+port+"/articles\">http://127.0.0.1:"+port+"/articles</a></dd>"+
                    "<dt>Alle Bestellungen anzeigen:</dt><dd><a href=\"http://127.0.0.1:"+port+"/orders\">http://127.0.0.1:"+port+"/orders</a></dd>"+
                    "<dt>Alle Kunden anzeigen:</dt><dd><a href=\"http://127.0.0.1:"+port+"/clients\">http://127.0.0.1:"+port+"/clients</a></dd>"+
                    "<dt>Bestellung abschicken:</dt><dd><a href=\"http://127.0.0.1:"+port+"/placeOrder?client_id=<client_id>&article_id_1=<article_id_1>&amount_1=<amount_1&article_id_2=<article_id_2>&amount_2=<amount_2>\">http://127.0.0.1:"+port+"/placeOrder?client_id=&lt;client_id>&article_id_1=&lt;article_id_1>&amount_1=&lt;amount_1>&article_id_2=&lt;article_id_2>&amount_2=&lt;amount_2></a></dd>"+
                    "</dl></body></html>";

            answerRequest(t, response);
        }

    }


    /**
     * Helper function to send an answer given as a String back to the browser
     * @param t HttpExchange of the request
     * @param response Answer to send
     */
    private void answerRequest(HttpExchange t, String response) throws IOException {
        byte[] payload = response.getBytes();
        t.sendResponseHeaders(200, payload.length);
        OutputStream os = t.getResponseBody();
        os.write(payload);
        os.close();
    }

    /**
     * Helper method to parse query paramaters
     */
    public static Map<String, String> queryToMap(String query){
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }

  
}
