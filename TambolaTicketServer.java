import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TambolaTicketServer {
    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/onito";
    private static final String JDBC_USER = "postgres";
    private static final String JDBC_PASSWORD = "postgres";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("PostgreSQL JDBC driver not found. Check your classpath.");
        }
    }

    public static void main(String[] args) {
        Spark.port(8080);

        Spark.get("/tickets", TambolaTicketServer::getAllTickets);

        Spark.awaitStop();
    }

    private static String getAllTickets(Request request, Response response) {
        List<Map<String, List<List<Integer>>>> fetchedTickets = fetchAllTambolaTickets();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.writeValueAsString(fetchedTickets);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            response.status(500);
            return "Error converting data to JSON";
        }
    }

    private static List<Map<String, List<List<Integer>>>> fetchAllTambolaTickets() {
        List<Map<String, List<List<Integer>>>> fetchedTickets = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT set_number, ticket_data FROM tickets")) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        int setNumber = resultSet.getInt("set_number");
                        String ticketDataString = resultSet.getString("ticket_data");

                        List<List<Integer>> ticketData = parseTicketData(ticketDataString);

                        Map<String, List<List<Integer>>> ticketSet = new HashMap<>();
                        ticketSet.put(Integer.toString(setNumber), ticketData);
                        fetchedTickets.add(ticketSet);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return fetchedTickets;
    }

    private static List<List<Integer>> parseTicketData(String ticketDataString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(ticketDataString, new TypeReference<List<List<Integer>>>() {});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Error parsing ticketData from JSON");
        }
    }
}
