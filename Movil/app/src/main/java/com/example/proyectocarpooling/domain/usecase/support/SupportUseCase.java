package com.example.proyectocarpooling.domain.usecase.support;

import com.example.proyectocarpooling.data.model.support.SupportTicketItem;
import com.example.proyectocarpooling.domain.repository.support.SupportRepository;

import java.io.IOException;
import java.util.List;

public class SupportUseCase {

    private final SupportRepository repository;

    public SupportUseCase(SupportRepository repository) {
        this.repository = repository;
    }

    public List<SupportTicketItem> listTickets(String userId) throws IOException {
        return repository.listTickets(userId);
    }

    public SupportTicketItem getTicket(String userId, String ticketId) throws IOException {
        return repository.getTicket(userId, ticketId);
    }

    public SupportTicketItem createTicket(
            String userId,
            int category,
            String subject,
            String description,
            String tripId,
            String reservationId
    ) throws IOException {
        return repository.createTicket(userId, category, subject, description, tripId, reservationId);
    }
}
