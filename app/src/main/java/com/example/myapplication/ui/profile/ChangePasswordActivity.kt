package com.example.myapplication.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.ActivityChangePasswordBinding
import com.example.myapplication.models.ChangePasswordRequest
import com.example.myapplication.models.GenericResponse
import com.example.myapplication.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        binding.tvBack.setOnClickListener { finish() }
        binding.btnChangePassword.setOnClickListener { attemptChangePassword() }
    }

    private fun attemptChangePassword() {
        val current = binding.etCurrentPassword.text.toString().trim()
        val newPass = binding.etNewPassword.text.toString().trim()
        val confirm = binding.etConfirmPassword.text.toString().trim()

        // Validation
        if (current.isEmpty()) { binding.etCurrentPassword.error = "Current password is required"; return }
        if (newPass.isEmpty()) { binding.etNewPassword.error = "New password is required"; return }
        if (newPass.length < 6) { binding.etNewPassword.error = "Min 6 characters"; return }
        if (newPass == current) { binding.etNewPassword.error = "New password must differ from current"; return }
        if (confirm.isEmpty()) { binding.etConfirmPassword.error = "Please confirm your password"; return }
        if (newPass != confirm) { binding.etConfirmPassword.error = "Passwords do not match"; return }

        setLoading(true)

        val token = sessionManager.getToken() ?: run {
            showError("Session expired. Please login again.")
            setLoading(false)
            return
        }
        val request = ChangePasswordRequest(current, newPass, confirm)

        RetrofitClient.instance.changePassword(token, request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                setLoading(false)
                when (response.code()) {
                    200 -> {
                        Toast.makeText(this@ChangePasswordActivity,
                            "Password changed successfully!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    400 -> {
                        val errorMsg = response.errorBody()?.string()
                        showError(if (!errorMsg.isNullOrEmpty()) parseErrorMessage(errorMsg)
                        else "Invalid input.")
                    }
                    401 -> {
                        val errorMsg = response.errorBody()?.string()
                        showError(if (!errorMsg.isNullOrEmpty()) parseErrorMessage(errorMsg)
                        else "Current password is incorrect.")
                    }
                    500 -> showError("Server error. Please try again.")
                    else -> showError("Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
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

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnChangePassword.isEnabled = !isLoading
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}