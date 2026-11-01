package com.hiregrid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.hiregrid.app.core.datastore.SessionManager
import com.hiregrid.app.core.network.RetrofitClient
import com.hiregrid.app.core.ui.theme.HireGridTheme
import com.hiregrid.app.presentation.navigation.HireGridNavGraph

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(applicationContext)
        RetrofitClient.initSessionSync(sessionManager, lifecycleScope)

        setContent {
            HireGridTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    HireGridNavGraph(
                        navController = navController,
                        sessionManager = sessionManager
                    )
                }
            }
        }
    }
}
