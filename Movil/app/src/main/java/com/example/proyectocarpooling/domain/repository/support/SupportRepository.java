package com.example.proyectocarpooling.domain.repository.support;

import com.example.proyectocarpooling.data.model.support.SupportTicketItem;

import java.io.IOException;
import java.util.List;

public interface SupportRepository {

    List<SupportTicketItem> listTickets(String userId) throws IOException;

    SupportTicketItem getTicket(String userId, String ticketId) throws IOException;

    SupportTicketItem createTicket(
            String userId,
            int category,
            String subject,
            String description,
            String tripId,
            String reservationId
    ) throws IOException;
}
