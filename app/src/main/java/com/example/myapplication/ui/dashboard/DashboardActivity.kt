package com.example.myapplication.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.databinding.ActivityDashboardBinding
import com.example.myapplication.models.DashboardResponse
import com.example.myapplication.models.GenericResponse
import com.example.myapplication.ui.auth.LoginActivity
import com.example.myapplication.ui.profile.ChangePasswordActivity
import com.example.myapplication.ui.profile.ProfileActivity
import com.example.myapplication.ui.profile.UpdateProfileActivity
import com.example.myapplication.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Show cached data immediately while API loads
        binding.tvWelcome.text = "Welcome, ${sessionManager.getName() ?: "User"}!"
        binding.tvEmail.text = sessionManager.getEmail() ?: ""

        loadDashboard()

        binding.cardProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.cardUpdateProfile.setOnClickListener {
            startActivity(Intent(this, UpdateProfileActivity::class.java))
        }
        binding.cardChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }
        binding.tvLogout.setOnClickListener { confirmLogout() }
    }

    override fun onResume() {
        super.onResume()
        // Refresh display after returning from UpdateProfile
        binding.tvWelcome.text = "Welcome, ${sessionManager.getName() ?: "User"}!"
        binding.tvEmail.text = sessionManager.getEmail() ?: ""
    }

    private fun loadDashboard() {
        val token = sessionManager.getToken() ?: run {
            goToLogin()
            return
        }
        binding.progressBar.visibility = View.VISIBLE

        RetrofitClient.instance.getDashboard(token).enqueue(object : Callback<DashboardResponse> {
            override fun onResponse(call: Call<DashboardResponse>, response: Response<DashboardResponse>) {
                binding.progressBar.visibility = View.GONE
                when (response.code()) {
                    200 -> {
                        val body = response.body()
                        if (body?.success == true) {
                            body.data?.user?.let {
                                sessionManager.saveName(it.name)
                                sessionManager.saveEmail(it.email)
                                binding.tvWelcome.text = body.data.welcome_message
                                binding.tvEmail.text = it.email
                            }
                        }
                    }
                    401 -> {
                        Toast.makeText(this@DashboardActivity,
                            "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                        goToLogin()
                    }
                    500 -> Toast.makeText(this@DashboardActivity,
                        "Server error.", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(this@DashboardActivity,
                        "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DashboardResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@DashboardActivity,
                    "Network error. Showing cached data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ -> performLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        val token = sessionManager.getToken()
        if (token != null) {
            RetrofitClient.instance.logout(token).enqueue(object : Callback<GenericResponse> {
                override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                    clearAndGoToLogin()
                }
                override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                    // Logout locally even if API call fails
                    clearAndGoToLogin()
                }
            })
        } else {
            clearAndGoToLogin()
        }
    }

    private fun clearAndGoToLogin() {
        sessionManager.clearSession()
        goToLogin()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}