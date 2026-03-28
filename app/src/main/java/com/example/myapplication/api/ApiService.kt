package com.example.myapplication.api

import com.example.myapplication.models.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("register")
    fun register(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @POST("logout")
    fun logout(@Header("Authorization") token: String): Call<GenericResponse>

    @GET("dashboard")
    fun getDashboard(@Header("Authorization") token: String): Call<DashboardResponse>

    @GET("profile")
    fun getProfile(@Header("Authorization") token: String): Call<ProfileResponse>

    @PUT("profile/update")
    fun updateProfile(
        @Header("Authorization") token: String,
        @Body request: UpdateProfileRequest
    ): Call<GenericResponse>

    @PUT("profile/change-password")
    fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Call<GenericResponse>
}