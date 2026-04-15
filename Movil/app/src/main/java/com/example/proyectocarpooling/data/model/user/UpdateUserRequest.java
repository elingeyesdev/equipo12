package com.example.proyectocarpooling.data.model.user;

public class UpdateUserRequest {

    public final String fullName;
    public final String email;
    public final String phoneNumber;
    public final String newPassword;
    public final String role;
    public final boolean roleChangeRequested;
    public final DriverProfileRequest driverProfile;

    public UpdateUserRequest(String fullName, String email, String phoneNumber, String newPassword, String role, boolean roleChangeRequested, DriverProfileRequest driverProfile) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.newPassword = newPassword;
        this.role = role;
        this.roleChangeRequested = roleChangeRequested;
        this.driverProfile = driverProfile;
    }
}
