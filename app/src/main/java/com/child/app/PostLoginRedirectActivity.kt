package com.child.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.child.app.child.ChildDashboardActivity
import com.child.app.parent.ParentDashboardActivity

class PostLoginRedirectActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_login_redirect)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val user = auth.currentUser

        if (user == null) {
            finish()
            return
        }

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->

                val role = doc.getString("role")

                when (role) {

                    "Parent" -> {
                        startActivity(
                            Intent(
                                this,
                                ParentDashboardActivity::class.java
                            )
                        )
                    }

                    "Child" -> {
                        startActivity(
                            Intent(
                                this,
                                ChildDashboardActivity::class.java
                            )
                        )
                    }

                    else -> {
                        Toast.makeText(
                            this,
                            "Role not assigned",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                finish()
            }
    }
}
