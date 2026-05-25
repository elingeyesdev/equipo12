package com.example.proyectocarpooling.presentation.support.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.proyectocarpooling.BackgroundTaskRunner;
import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.data.model.support.SupportTicketItem;
import com.example.proyectocarpooling.domain.usecase.support.SupportUseCase;

import java.io.IOException;
import java.util.List;

public class SupportViewModel extends AndroidViewModel {

    private final SupportUseCase supportUseCase;
    private final BackgroundTaskRunner taskRunner;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<SupportTicketItem>> tickets = new MutableLiveData<>();
    private final MutableLiveData<String> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<SupportTicketItem> ticketCreatedEvent = new MutableLiveData<>();
    private final MutableLiveData<SupportTicketItem> detailTicket = new MutableLiveData<>();
    private final MutableLiveData<String> detailErrorEvent = new MutableLiveData<>();

    public SupportViewModel(@NonNull Application application) {
        super(application);
        CarPoolingApplication app = (CarPoolingApplication) application;
        supportUseCase = new SupportUseCase(app.getSupportRepository());
        taskRunner = app.getTaskRunner();
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<List<SupportTicketItem>> getTickets() {
        return tickets;
    }

    public LiveData<String> getErrorEvent() {
        return errorEvent;
    }

    public LiveData<SupportTicketItem> getTicketCreatedEvent() {
        return ticketCreatedEvent;
    }

    public void clearTicketCreatedEvent() {
        ticketCreatedEvent.setValue(null);
    }

    public LiveData<SupportTicketItem> getDetailTicket() {
        return detailTicket;
    }

    public LiveData<String> getDetailErrorEvent() {
        return detailErrorEvent;
    }

    public void clearDetailTicket() {
        detailTicket.setValue(null);
    }

    public void loadTickets(String userId) {
        loading.setValue(true);
        taskRunner.runWithResult(
                () -> supportUseCase.listTickets(userId),
                new BackgroundTaskRunner.ResultCallback<List<SupportTicketItem>>() {
                    @Override
                    public void onSuccess(List<SupportTicketItem> list) {
                        loading.postValue(false);
                        tickets.postValue(list);
                    }

                    @Override
                    public void onError(String message) {
                        loading.postValue(false);
                        errorEvent.postValue(message != null ? message : "No se pudieron cargar tus solicitudes");
                    }
                });
    }

    public void createTicket(
            String userId,
            int category,
            String subject,
            String description,
            String tripId,
            String reservationId
    ) {
        loading.setValue(true);
        taskRunner.runWithResult(
                () -> supportUseCase.createTicket(userId, category, subject, description, tripId, reservationId),
                new BackgroundTaskRunner.ResultCallback<SupportTicketItem>() {
                    @Override
                    public void onSuccess(SupportTicketItem item) {
                        loading.postValue(false);
                        ticketCreatedEvent.postValue(item);
                        loadTickets(userId);
                    }

                    @Override
                    public void onError(String message) {
                        loading.postValue(false);
                        String text = message;
                        if (message != null && message.contains("IOException")) {
                            text = "Error de conexión con el servidor";
                        }
                        errorEvent.postValue(text != null ? text : "No se pudo enviar el reporte");
                    }
                });
    }

    public void loadTicketDetail(String userId, String ticketId) {
        loading.setValue(true);
        taskRunner.runWithResult(
                () -> supportUseCase.getTicket(userId, ticketId),
                new BackgroundTaskRunner.ResultCallback<SupportTicketItem>() {
                    @Override
                    public void onSuccess(SupportTicketItem item) {
                        loading.postValue(false);
                        detailTicket.postValue(item);
                    }

                    @Override
                    public void onError(String message) {
                        loading.postValue(false);
                        detailErrorEvent.postValue(message != null ? message : "No se pudo cargar el detalle");
                    }
                });
    }
}
