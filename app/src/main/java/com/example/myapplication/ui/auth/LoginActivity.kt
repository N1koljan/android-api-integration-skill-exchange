package com.example.myapplication.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.ActivityLoginBinding
import com.example.myapplication.models.AuthResponse
import com.example.myapplication.models.LoginRequest
import com.example.myapplication.ui.dashboard.DashboardActivity
import com.example.myapplication.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // If already logged in, skip to Dashboard
        if (sessionManager.isLoggedIn()) {
            goToDashboard()
            return
        }

        binding.btnLogin.setOnClickListener { attemptLogin() }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validation
        if (email.isEmpty()) { binding.etEmail.error = "Email is required"; return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter a valid email"; return
        }
        if (password.isEmpty()) { binding.etPassword.error = "Password is required"; return }

        setLoading(true)

        val request = LoginRequest(email, password)
        RetrofitClient.instance.login(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                setLoading(false)
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body?.success == true && body.token != null) {
                            // ✅ Save token with Bearer prefix
                            sessionManager.saveToken("Bearer ${body.token}")
                            body.user?.let {
                                sessionManager.saveName(it.name)
                                sessionManager.saveEmail(it.email)
                            }
                            goToDashboard()
                        } else {
                            showError(body?.message ?: "Login failed")
                        }
                    }
                    400 -> {
                        val errorMsg = response.errorBody()?.string()
                        showError(if (!errorMsg.isNullOrEmpty()) parseErrorMessage(errorMsg)
                        else "Invalid request. Check your input.")
                    }
                    401 -> {
                        val errorMsg = response.errorBody()?.string()
                        showError(if (!errorMsg.isNullOrEmpty()) parseErrorMessage(errorMsg)
                        else "Invalid email or password.")
                    }
                    500 -> showError("Server error. Please try again later.")
                    else -> showError("Unexpected error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                setLoading(false)
                showError("Network error. Check your internet connection.")
            }
        })
    }

    private fun parseErrorMessage(json: String): String {
        return try {
            val obj = org.json.JSONObject(json)
            obj.optString("message", "An error occurred")
        } catch (e: Exception) {
            "An error occurred"
        }
    }

    private fun goToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}