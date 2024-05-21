package com.example.viajero

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.viajero.ui.theme.ViajeroTheme

class DetalleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ViajeroTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting2("Android")
                }
            }
        }
    }
}
@Composable
fun Greeting2(name: String, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
    Button(onClick = {
        val intent = Intent(contexto, MainActivity::class.java)
        contexto.startActivity(intent)
    }) {
        Text("Volver")
    }
}