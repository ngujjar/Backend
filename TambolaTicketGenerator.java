
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TambolaTicketGenerator {
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
        int numberOfSets = 5;
        List<Map<String, List<List<Integer>>>> generatedTickets = generateTambolaSets(numberOfSets);

        saveTicketsToDatabase(generatedTickets);

        List<Map<String, List<List<Integer>>>> fetchedTickets = fetchTambolaTicketsFromDatabase(1, 10);
        System.out.println("Fetched Tickets: " + fetchedTickets);
    }

    private static List<Map<String, List<List<Integer>>>> generateTambolaSets(int numberOfSets) {
        List<Map<String, List<List<Integer>>>> sets = new ArrayList<>();

        for (int setNumber = 11; setNumber <= 16; setNumber++) {
            Map<String, List<List<Integer>>> ticketSet = new HashMap<>();
            for (int i = 0; i < 6; i++) {
                List<List<Integer>> ticket = generateTambolaTicket();
                ticketSet.put(Integer.toString(setNumber * 10 + i), ticket);
            }
            sets.add(ticketSet);
        }

        return sets;
    }

    private static List<List<Integer>> generateTambolaTicket() {
        List<List<Integer>> ticket = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 90; i++) {
            numbers.add(i);
        }
        Collections.shuffle(numbers);

        for (int row = 0; row < 3; row++) {
            List<Integer> rowNumbers = new ArrayList<>();
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                if (index < numbers.size()) {
                    rowNumbers.add(numbers.get(index));
                } else {
                    rowNumbers.add(0); // Blank cell filled with zero
                }
            }
            ticket.add(rowNumbers);
        }
        return ticket;
    }

    private static void saveTicketsToDatabase(List<Map<String, List<List<Integer>>>> tickets) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            for (Map<String, List<List<Integer>>> ticketSet : tickets) {
                for (Map.Entry<String, List<List<Integer>>> entry : ticketSet.entrySet()) {
                    String setNumber = entry.getKey();
                    List<List<Integer>> ticketData = entry.getValue();

                    try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO tambola_tickets (set_id, ticket) VALUES (?, ?)")) {
                        preparedStatement.setInt(1, Integer.parseInt(setNumber));
                        preparedStatement.setString(2, convertToJson(ticketData));
                        preparedStatement.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<Map<String, List<List<Integer>>>> fetchTambolaTicketsFromDatabase(int page, int pageSize) {
        List<Map<String, List<List<Integer>>>> fetchedTickets = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT id, set_id, ticket FROM tambola_tickets LIMIT ? OFFSET ?")) {
                preparedStatement.setInt(1, pageSize);
                preparedStatement.setInt(2, (page - 1) * pageSize);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        int setId = resultSet.getInt("set_id");
                        int ticketId = resultSet.getInt("id");
                        String ticketDataString = resultSet.getString("ticket");

                        // Debug information to inspect the raw JSON data from the database
                        System.out.println("Raw JSON from Database for set " + setId + ", ticket " + ticketId + ": " + ticketDataString);

                        try {
                            // Attempt to directly parse the data as a List of List of Integers
                            List<List<Integer>> ticketData = parseTicketData(ticketDataString);
                            System.out.println("Parsed Ticket Data for set " + setId + ", ticket " + ticketId + ": " + ticketData);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Map<String, List<List<Integer>>> ticketSet = new HashMap<>();
                        ticketSet.put(Integer.toString(setId), ticketData);
                        fetchedTickets.add(ticketSet);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return fetchedTickets;
    }



    private static String convertToJson(Object data) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Serialize the "tickets" field directly as an array
            return objectMapper.writeValueAsString(Collections.singletonMap("tickets", data));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Error converting data to JSON");
        }
    }

    private static List<List<Integer>> parseTicketData(String ticketDataString) {
        try {
            if (ticketDataString == null) {
                throw new RuntimeException("Ticket data is null");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

            // Attempt to directly parse the data as a List of List of Integers
            return objectMapper.readValue(ticketDataString, new TypeReference<List<List<Integer>>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error parsing data from JSON: " + e.getMessage());
        }
    }
}
