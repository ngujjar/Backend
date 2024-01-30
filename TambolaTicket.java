import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class TambolaTicket {
    private int ticket_id;
    private int set_number;
    private String ticket_data;

    // Getter and setter for ticket_id
    public int getTicketId() {
        return ticket_id;
    }

    public void setTicketId(int ticketId) {
        this.ticket_id = ticketId;
    }

    // Getter and setter for set_number
    public int getSetNumber() {
        return set_number;
    }

    public void setSetNumber(int setNumber) {
        this.set_number = setNumber;
    }

    // Getter and setter for ticket_data
    public String getTicketData() {
        return ticket_data;
    }

    public void setTicketData(String ticketData) {
        this.ticket_data = ticketData;
    }

    // Getter for parsedTicketData
    public List<List<Integer>> getParsedTicketData() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            return objectMapper.readValue(ticket_data, new TypeReference<List<List<Integer>>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Error parsing ticketData from JSON", e);
        }
    }

    @Override
    public String toString() {
        return "TambolaTicket{" +
                "ticket_id=" + ticket_id +
                ", set_number=" + set_number +
                ", ticket_data='" + ticket_data + '\'' +
                '}';
    }
}
