package com.example.myapplication.models

// ─── Request Models ────────────────────────────────────────────────────────

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UpdateProfileRequest(
    val name: String,
    val email: String
)

data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String,
    val new_password_confirmation: String
)

// ─── Response Models ───────────────────────────────────────────────────────

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val user: User?
)

data class GenericResponse(
    val success: Boolean,
    val message: String
)

data class ProfileResponse(
    val success: Boolean,
    val message: String?,
    val user: User?
)

data class DashboardResponse(
    val success: Boolean,
    val message: String,
    val data: DashboardData?
)

data class DashboardData(
    val welcome_message: String,
    val user: User?
)

// ─── User Model ────────────────────────────────────────────────────────────

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val created_at: String?,
    val updated_at: String?
)