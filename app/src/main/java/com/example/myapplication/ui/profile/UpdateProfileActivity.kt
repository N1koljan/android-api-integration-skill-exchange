package com.example.myapplication.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.ActivityUpdateProfileBinding
import com.example.myapplication.models.GenericResponse
import com.example.myapplication.models.UpdateProfileRequest
import com.example.myapplication.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpdateProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateProfileBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Pre-fill with saved data
        binding.etName.setText(sessionManager.getName())
        binding.etEmail.setText(sessionManager.getEmail())

        binding.tvBack.setOnClickListener { finish() }
        binding.btnUpdate.setOnClickListener { attemptUpdate() }
    }

    private fun attemptUpdate() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        if (name.isEmpty()) { binding.etName.error = "Name is required"; return }
        if (email.isEmpty()) { binding.etEmail.error = "Email is required"; return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter a valid email"; return
        }

        setLoading(true)

        val token = sessionManager.getToken() ?: run {
            showError("Session expired. Please login again.")
            setLoading(false)
            return
        }
        val request = UpdateProfileRequest(name, email)

        RetrofitClient.instance.updateProfile(token, request).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                setLoading(false)
                when (response.code()) {
                    200 -> {
                        // ✅ Update local session cache
                        sessionManager.saveName(name)
                        sessionManager.saveEmail(email)
                        Toast.makeText(this@UpdateProfileActivity,
                            "Profile updated successfully!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    400 -> {
                        val errorMsg = response.errorBody()?.string()
                        showError(if (!errorMsg.isNullOrEmpty()) parseErrorMessage(errorMsg)
                        else "Invalid input. Please check your details.")
                    }
                    401 -> showError("Unauthorized. Please login again.")
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
            obj.optString("message", "Update failed")
        } catch (e: Exception) {
            "Update failed"
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnUpdate.isEnabled = !isLoading
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}