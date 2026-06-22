package com.example.proyectocarpooling.presentation.schedules.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.data.model.trip.RecurringReservation;
import com.example.proyectocarpooling.data.model.trip.TripSchedule;
import com.example.proyectocarpooling.domain.usecase.trip.ManageScheduleUseCase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripSchedulesViewModel extends AndroidViewModel {

    private final ManageScheduleUseCase manageScheduleUseCase;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<TripSchedule>> driverSchedules = new MutableLiveData<>();
    private final MutableLiveData<List<RecurringReservation>> passengerSubscriptions = new MutableLiveData<>();
    private final MutableLiveData<List<TripSchedule>> activeSchedules = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    public TripSchedulesViewModel(@NonNull Application application) {
        super(application);
        CarPoolingApplication app = (CarPoolingApplication) application;
        this.manageScheduleUseCase = new ManageScheduleUseCase(app.getTripScheduleRepository());
    }

    public LiveData<List<TripSchedule>> getDriverSchedules() {
        return driverSchedules;
    }

    public LiveData<List<RecurringReservation>> getPassengerSubscriptions() {
        return passengerSubscriptions;
    }

    public LiveData<List<TripSchedule>> getActiveSchedules() {
        return activeSchedules;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public void loadDriverSchedules(String driverUserId) {
        loading.postValue(true);
        executor.execute(() -> {
            try {
                List<TripSchedule> list = manageScheduleUseCase.getDriverSchedules(driverUserId);
                driverSchedules.postValue(list);
            } catch (Exception e) {
                errorMessage.postValue("Error al cargar horarios: " + e.getMessage());
            } finally {
                loading.postValue(false);
            }
        });
    }

    public void loadPassengerSubscriptions(String passengerUserId) {
        loading.postValue(true);
        executor.execute(() -> {
            try {
                List<RecurringReservation> list = manageScheduleUseCase.getPassengerSubscriptions(passengerUserId);
                passengerSubscriptions.postValue(list);
            } catch (Exception e) {
                errorMessage.postValue("Error al cargar suscripciones: " + e.getMessage());
            } finally {
                loading.postValue(false);
            }
        });
    }

    public void loadActiveSchedules() {
        loading.postValue(true);
        executor.execute(() -> {
            try {
                List<TripSchedule> list = manageScheduleUseCase.getActiveSchedules();
                activeSchedules.postValue(list);
            } catch (Exception e) {
                errorMessage.postValue("Error al cargar horarios activos: " + e.getMessage());
            } finally {
                loading.postValue(false);
            }
        });
    }

    public void toggleSchedule(TripSchedule schedule, boolean active, String driverUserId) {
        loading.postValue(true);
        executor.execute(() -> {
            try {
                boolean success = manageScheduleUseCase.toggleSchedule(schedule.id, active);
                if (success) {
                    successMessage.postValue(active ? "Horario activado" : "Horario pausado");
                    loadDriverSchedules(driverUserId);
                } else {
                    errorMessage.postValue("No se pudo actualizar el horario");
                }
            } catch (Exception e) {
                errorMessage.postValue("Error: " + e.getMessage());
            } finally {
                loading.postValue(false);
            }
        });
    }

    public void cancelSubscription(RecurringReservation sub, String passengerUserId) {
        loading.postValue(true);
        executor.execute(() -> {
            try {
                boolean success = manageScheduleUseCase.cancelSubscription(sub.id);
                if (success) {
                    successMessage.postValue("Suscripción cancelada");
                    loadPassengerSubscriptions(passengerUserId);
                } else {
                    errorMessage.postValue("No se pudo cancelar la suscripción");
                }
            } catch (Exception e) {
                errorMessage.postValue("Error: " + e.getMessage());
            } finally {
                loading.postValue(false);
            }
        });
    }

    public void subscribeToSchedule(TripSchedule schedule, String passengerUserId, int seats) {
        loading.postValue(true);
        executor.execute(() -> {
            try {
                RecurringReservation sub = manageScheduleUseCase.subscribe(schedule.id, passengerUserId, seats);
                if (sub != null) {
                    successMessage.postValue("¡Suscripción exitosa!");
                    loadPassengerSubscriptions(passengerUserId);
                } else {
                    errorMessage.postValue("No se pudo realizar la suscripción");
                }
            } catch (Exception e) {
                errorMessage.postValue("Error al suscribirse: " + e.getMessage());
            } finally {
                loading.postValue(false);
            }
        });
    }
    public void deleteSchedule(String scheduleId, String driverUserId) {
        loading.postValue(true);
        executor.execute(() -> {
            try {
                boolean success = manageScheduleUseCase.deleteSchedule(scheduleId);
                if (success) {
                    successMessage.postValue("Horario programado eliminado");
                    loadDriverSchedules(driverUserId);
                } else {
                    errorMessage.postValue("No se pudo eliminar el horario");
                }
            } catch (Exception e) {
                errorMessage.postValue("Error al eliminar: " + e.getMessage());
            } finally {
                loading.postValue(false);
            }
        });
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
