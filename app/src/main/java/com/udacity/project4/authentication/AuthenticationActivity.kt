package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity


/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    private val viewModel by viewModels<AuthViewModel>()
    private lateinit var binding: ActivityAuthenticationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        binding.loginButton.setOnClickListener {
            singIn()
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null){
            Log.d("AuthenticationActivity", "State ok to sing")
        } else {
            Log.d("AuthenticationActivity", "State not singed")
        }

        viewModel.authState.observe(this) { state ->
            if (state == AuthViewModel.AuthenticationState.AUTHENTICATED){
                Log.d("AuthenticationActivity", "State of authentication : $state")
                val intent = Intent(this, RemindersActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                Log.i("AuthenticationActivity", "Successfully sign in")
                val navigating = Intent(this, RemindersActivity::class.java)
                startActivity(navigating)
            }
            else -> {
                Log.i("AuthenticationActivity", "sign in unSuccessfully ${response?.error?.message}")
            }
        }
    }

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result: FirebaseAuthUIAuthenticationResult? ->
        result?.let {
            this.onSignInResult(it)
        }
    }

    private fun singIn() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setLogo(R.drawable.map)
            .setAvailableProviders(providers)
            .build()
        signInLauncher.launch(signInIntent)
    }
}