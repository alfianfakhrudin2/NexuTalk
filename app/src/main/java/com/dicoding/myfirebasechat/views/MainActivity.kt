package com.dicoding.myfirebasechat.views

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.myfirebasechat.R
import com.dicoding.myfirebasechat.adapter.FirebaseMessageAdapter
import com.dicoding.myfirebasechat.databinding.ActivityMainBinding
import com.dicoding.myfirebasechat.model.Message
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: FirebaseMessageAdapter


    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted
            Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
        } else {
            // Permission denied
            Toast.makeText(this, "Notifications permission rejected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= 33) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.ACCESS_NOTIFICATION_POLICY)
        }

        auth = Firebase.auth
        val firebaseUser = auth.currentUser

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        // login function
        if (firebaseUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        } else {
            // Handle the case where the user is not signed in
        }

        // inisialisasi Firebase Database
        db = Firebase.database

        val messagesRef = db.reference.child(MESSAGE_CHILD)

        // Send button was clicked function
        binding.sendButton.setOnClickListener {
            val friendlyMessage = Message(
                binding.messageEditText.text.toString(),
                firebaseUser.displayName.toString(),
                firebaseUser.photoUrl.toString(),
                Date().time
            )

            // untuk mengganti data menggunakan fungsi setValue pada DatabaseReference.
            messagesRef.push().setValue(friendlyMessage) { error, _ ->
                if (error != null) {
                    // Handle the error
                    Toast.makeText(this, getString(R.string.send_error) + error.message, Toast.LENGTH_SHORT).show()
                } else {
                    // Clear the input box
                    binding.messageEditText.setText("")
                    Toast.makeText(this, getString(R.string.send_success), Toast.LENGTH_SHORT).show()

                    // Scroll to the latest message
                    binding.messageRecyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            }
        }

        val manager = LinearLayoutManager(this)
        manager.stackFromEnd = true
        binding.messageRecyclerView.layoutManager = manager

        val options = FirebaseRecyclerOptions.Builder<Message>()
            .setQuery(messagesRef, Message::class.java)
            .build()
        adapter = FirebaseMessageAdapter(options, firebaseUser.displayName)
        binding.messageRecyclerView.adapter = adapter
    }

    public override fun onResume() {
        super.onResume()
        adapter.startListening()
    }

    public override fun onPause() {
        adapter.stopListening()
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun signOut() {
        lifecycleScope.launch {
            val credentialManager = CredentialManager.create(this@MainActivity)

            auth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }

    // untuk menentukan lokasi data dengan menggunakan fungsi child pada DatabaseReference.
    companion object {
        const val MESSAGE_CHILD = "messages"
    }
}
