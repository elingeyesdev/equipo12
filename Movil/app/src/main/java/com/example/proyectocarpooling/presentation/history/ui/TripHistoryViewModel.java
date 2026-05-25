package com.example.proyectocarpooling.presentation.history.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.proyectocarpooling.BackgroundTaskRunner;
import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.data.model.history.TripHistoryListResult;
import com.example.proyectocarpooling.domain.usecase.history.TripHistoryUseCase;

public class TripHistoryViewModel extends AndroidViewModel {

    private final TripHistoryUseCase tripHistoryUseCase;
    private final BackgroundTaskRunner taskRunner;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<TripHistoryListResult> historyResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorEvent = new MutableLiveData<>();

    public TripHistoryViewModel(@NonNull Application application) {
        super(application);
        CarPoolingApplication app = (CarPoolingApplication) application;
        tripHistoryUseCase = new TripHistoryUseCase(app.getTripHistoryRepository());
        taskRunner = app.getTaskRunner();
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<TripHistoryListResult> getHistoryResult() { return historyResult; }
    public LiveData<String> getErrorEvent() { return errorEvent; }

    public void loadHistory(String userId, String passengerName) {
        loading.setValue(true);
        taskRunner.runWithResult(
                () -> tripHistoryUseCase.listHistory(userId, passengerName),
                new BackgroundTaskRunner.ResultCallback<TripHistoryListResult>() {
                    @Override public void onSuccess(TripHistoryListResult result) {
                        loading.postValue(false);
                        historyResult.postValue(result);
                    }
                    @Override public void onError(String message) {
                        loading.postValue(false);
                        errorEvent.postValue("Error cargando historial");
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
