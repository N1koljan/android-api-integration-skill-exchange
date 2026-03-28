package com.example.myapplication.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.ActivityRegisterBinding
import com.example.myapplication.models.AuthResponse
import com.example.myapplication.models.RegisterRequest
import com.example.myapplication.ui.dashboard.DashboardActivity
import com.example.myapplication.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        binding.btnRegister.setOnClickListener { attemptRegister() }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun attemptRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Validation
        if (name.isEmpty()) { binding.etName.error = "Name is required"; return }
        if (email.isEmpty()) { binding.etEmail.error = "Email is required"; return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter a valid email"; return
        }
        if (password.isEmpty()) { binding.etPassword.error = "Password is required"; return }
        if (password.length < 6) { binding.etPassword.error = "Min 6 characters"; return }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"; return
        }

        setLoading(true)

        val request = RegisterRequest(name, email, password, confirmPassword)
        RetrofitClient.instance.register(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                setLoading(false)
                when (response.code()) {
                    200, 201 -> {
                        val body = response.body()
                        if (body?.success == true) {
                            // ✅ Auto-login: save token and go straight to Dashboard
                            body.token?.let { sessionManager.saveToken("Bearer $it") }
                            body.user?.let {
                                sessionManager.saveName(it.name)
                                sessionManager.saveEmail(it.email)
                            }
                            Toast.makeText(
                                this@RegisterActivity,
                                "Registration successful! Welcome!",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this@RegisterActivity, DashboardActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            showError(body?.message ?: "Registration failed")
                        }
                    }
                    400 -> {
                        // Try to read server error message
                        val errorMsg = response.errorBody()?.string()
                        showError(if (!errorMsg.isNullOrEmpty()) parseErrorMessage(errorMsg)
                        else "Invalid input. Please check your details.")
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

    // Parse JSON error body to extract "message" field
    private fun parseErrorMessage(json: String): String {
        return try {
            val obj = org.json.JSONObject(json)
            obj.optString("message", "Registration failed")
        } catch (e: Exception) {
            "Registration failed"
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}