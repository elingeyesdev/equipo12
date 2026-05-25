package com.example.proyectocarpooling.presentation.auth.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.proyectocarpooling.BackgroundTaskRunner;
import com.example.proyectocarpooling.CarPoolingApplication;
import com.example.proyectocarpooling.data.model.user.LoginUserRequest;
import com.example.proyectocarpooling.data.model.user.RegisterUserRequest;
import com.example.proyectocarpooling.data.model.user.UserResponse;
import com.example.proyectocarpooling.domain.usecase.user.UserAccessUseCase;

public class AuthViewModel extends AndroidViewModel {

    private final UserAccessUseCase userAccessUseCase;
    private final BackgroundTaskRunner taskRunner;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<UserResponse> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorEvent = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        CarPoolingApplication app = (CarPoolingApplication) application;
        userAccessUseCase = new UserAccessUseCase(app.getUserRepository());
        taskRunner = app.getTaskRunner();
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<UserResponse> getLoginSuccess() { return loginSuccess; }
    public LiveData<String> getErrorEvent() { return errorEvent; }

    public void login(String email, String password) {
        loading.setValue(true);
        taskRunner.runWithResult(
                () -> userAccessUseCase.login(new LoginUserRequest(email, password)),
                new BackgroundTaskRunner.ResultCallback<UserResponse>() {
                    @Override public void onSuccess(UserResponse user) {
                        loading.postValue(false);
                        loginSuccess.postValue(user);
                    }
                    @Override public void onError(String message) {
                        loading.postValue(false);
                        errorEvent.postValue(message);
                    }
                });
    }

    public void register(RegisterUserRequest request) {
        loading.setValue(true);
        taskRunner.runWithResult(
                () -> userAccessUseCase.register(request),
                new BackgroundTaskRunner.ResultCallback<UserResponse>() {
                    @Override public void onSuccess(UserResponse user) {
                        loading.postValue(false);
                        loginSuccess.postValue(user);
                    }
                    @Override public void onError(String message) {
                        loading.postValue(false);
                        errorEvent.postValue(message);
                    }
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
