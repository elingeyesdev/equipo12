package com.example.proyectocarpooling.presentation.history.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.proyectocarpooling.BackgroundTaskRunner;
import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.data.model.history.TripHistoryDetailItem;
import com.example.proyectocarpooling.domain.usecase.history.TripHistoryUseCase;

public class TripHistoryDetailViewModel extends AndroidViewModel {

    private final TripHistoryUseCase tripHistoryUseCase;
    private final BackgroundTaskRunner taskRunner;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<TripHistoryDetailItem> detail = new MutableLiveData<>();
    private final MutableLiveData<String> errorEvent = new MutableLiveData<>();

    public TripHistoryDetailViewModel(@NonNull Application application) {
        super(application);
        CarPoolingApplication app = (CarPoolingApplication) application;
        tripHistoryUseCase = new TripHistoryUseCase(app.getTripHistoryRepository());
        taskRunner = app.getTaskRunner();
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<TripHistoryDetailItem> getDetail() { return detail; }
    public LiveData<String> getErrorEvent() { return errorEvent; }

    public void loadDetail(String userId, String tripId, String passengerName) {
        loading.setValue(true);
        taskRunner.runWithResult(
                () -> tripHistoryUseCase.getHistoryDetail(userId, tripId, passengerName),
                new BackgroundTaskRunner.ResultCallback<TripHistoryDetailItem>() {
                    @Override public void onSuccess(TripHistoryDetailItem d) {
                        loading.postValue(false);
                        detail.postValue(d);
                    }
                    @Override public void onError(String message) {
                        loading.postValue(false);
                        errorEvent.postValue("Error cargando detalle");
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
