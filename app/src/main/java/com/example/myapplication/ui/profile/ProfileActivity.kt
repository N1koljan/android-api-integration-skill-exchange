package com.example.myapplication.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.ActivityProfileBinding
import com.example.myapplication.models.ProfileResponse
import com.example.myapplication.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Show cached data immediately
        val cachedName = sessionManager.getName() ?: "—"
        val cachedEmail = sessionManager.getEmail() ?: "—"
        binding.tvName.text = cachedName
        binding.tvEmail.text = cachedEmail
        binding.tvAvatar.text = cachedName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        binding.tvBack.setOnClickListener { finish() }

        loadProfile()
    }

    private fun loadProfile() {
        val token = sessionManager.getToken() ?: run {
            showError("Session expired. Please login again.")
            finish()
            return
        }
        binding.progressBar.visibility = View.VISIBLE

        RetrofitClient.instance.getProfile(token).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                binding.progressBar.visibility = View.GONE
                when (response.code()) {
                    200 -> {
                        val user = response.body()?.user
                        if (user != null) {
                            binding.tvName.text = user.name
                            binding.tvEmail.text = user.email
                            binding.tvCreatedAt.text = formatDate(user.created_at)
                            binding.tvAvatar.text = user.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                            // Update cache
                            sessionManager.saveName(user.name)
                            sessionManager.saveEmail(user.email)
                        }
                    }
                    401 -> {
                        showError("Unauthorized. Please login again.")
                        finish()
                    }
                    500 -> showError("Server error. Please try again.")
                    else -> showError("Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                showError("Network error. Showing cached data.")
            }
        })
    }

    // Format ISO date string to readable format
    private fun formatDate(isoDate: String?): String {
        if (isoDate.isNullOrEmpty()) return "N/A"
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
            inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            val date = inputFormat.parse(isoDate)
            if (date != null) outputFormat.format(date) else isoDate
        } catch (e: Exception) {
            isoDate
        }
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}