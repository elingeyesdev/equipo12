package com.example.proyectocarpooling;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BackgroundTaskRunner {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public <T> void runWithResult(TaskSupplier<T> task, ResultCallback<T> callback) {
        executor.execute(() -> {
            try {
                T result = task.run();
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(buildMessage(e)));
            }
        });
    }

    public void run(SimpleTask task, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                task.run();
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(buildMessage(e)));
            }
        });
    }

    public void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private static String buildMessage(Exception e) {
        String msg = e.getMessage();
        return (msg == null || msg.trim().isEmpty()) ? "Error de red inesperado" : msg;
    }

    public interface TaskSupplier<T> { T run() throws Exception; }
    public interface SimpleTask { void run() throws Exception; }
    public interface ResultCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }
    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }
}
