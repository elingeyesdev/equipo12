package com.example.proyectocarpooling.data.local;

import android.content.Context;

import com.example.proyectocarpooling.R;
import com.example.proyectocarpooling.data.remote.user.UsersRemoteDataSource;
import com.example.proyectocarpooling.data.repository.user.UserRepositoryImpl;
import com.example.proyectocarpooling.domain.repository.user.UserRepository;
import com.example.proyectocarpooling.domain.usecase.user.UserAccessUseCase;

public final class UserAccessProvider {

    private UserAccessProvider() {
    }

    public static UserAccessUseCase create(Context context) {
        String apiBaseUrl = context.getString(R.string.api_base_url);
        UsersRemoteDataSource remoteDataSource = new UsersRemoteDataSource(apiBaseUrl);
        UserRepository repository = new UserRepositoryImpl(remoteDataSource);
        return new UserAccessUseCase(repository);
    }
}
