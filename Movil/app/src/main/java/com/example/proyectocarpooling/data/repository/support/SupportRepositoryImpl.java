package com.example.proyectocarpooling.data.repository.support;

import com.example.proyectocarpooling.data.model.support.SupportTicketItem;
import com.example.proyectocarpooling.data.remote.SupportRemoteDataSource;
import com.example.proyectocarpooling.domain.repository.support.SupportRepository;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class SupportRepositoryImpl implements SupportRepository {

    private final SupportRemoteDataSource remoteDataSource;

    public SupportRepositoryImpl(SupportRemoteDataSource remoteDataSource) {
        this.remoteDataSource = remoteDataSource;
    }

    @Override
    public List<SupportTicketItem> listTickets(String userId) throws IOException {
        try {
            return remoteDataSource.listTickets(userId);
        } catch (JSONException e) {
            throw new IOException("Respuesta inválida de soporte", e);
        }
    }

    @Override
    public SupportTicketItem getTicket(String userId, String ticketId) throws IOException {
        try {
            return remoteDataSource.getTicket(userId, ticketId);
        } catch (JSONException e) {
            throw new IOException("Respuesta inválida de soporte", e);
        }
    }

    @Override
    public SupportTicketItem createTicket(
            String userId,
            int category,
            String subject,
            String description,
            String tripId,
            String reservationId
    ) throws IOException {
        try {
            return remoteDataSource.createTicket(userId, category, subject, description, tripId, reservationId);
        } catch (JSONException e) {
            throw new IOException("Respuesta inválida de soporte", e);
        }
    }
}
