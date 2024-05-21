package com.example.viajero

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.viajero.db.Lugar
import com.example.viajero.db.LugarDB
import com.example.viajero.ui.theme.ViajeroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LugarFormUI(null)
        }
    }
}
@Composable
fun LugarFormUI(l: Lugar?){
    val contexto = LocalContext.current
    val (lugar, setLugar) = remember { mutableStateOf(
        l?.lugar ?: "" ) }
    val (imagen, setImagen) = remember { mutableStateOf(
        l?.imagen ?: "" ) }
    val (latitud, setLatitud) = remember { mutableStateOf(
        l?.lat ?: "" ) }
    val (longitud, setLongitud) = remember { mutableStateOf(
        l?.lon ?: "" ) }
    val (orden, setOrden) = remember { mutableStateOf(
        l?.orden ?: "" ) }
    val (costoAl, setCostoAl) = remember { mutableStateOf(
        l?.costoAl ?: "" ) }
    val (costoTrans, setCostoTrans) = remember { mutableStateOf(
        l?.costoTrans ?: "" ) }
    val (comentarios, setComentarios) = remember { mutableStateOf(
        l?.comentarios ?: "" ) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost( snackbarHostState) }
    ) {paddingValues ->
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TextField(
                value = lugar,
                onValueChange = { setLugar(it) },
                label = { Text("Lugar") }
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = imagen,
                onValueChange = { setImagen(it) },
                label = { Text("URL de imagen") }
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = latitud,
                onValueChange = { setLatitud(it) },
                label = { Text("Latitud") }
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = longitud,
                onValueChange = { setLongitud(it) },
                label = { Text("Longitud") }
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = orden,
                onValueChange = { setOrden(it) },
                label = { Text("Orden de visita") }
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = costoAl,
                onValueChange = { setCostoAl(it) },
                label = { Text("Costo de alojamiento") }
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = costoTrans,
                onValueChange = { setCostoTrans(it) },
                label = { Text("Costo de transporte") }
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                value = comentarios,
                onValueChange = { setComentarios(it) },
                label = { Text("Comentarios") }
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    val dao = LugarDB.getInstance(
                        contexto
                    ).lugarDao()
                    val lugar = Lugar(
                        l?.id ?: 0, lugar,
                        imagen, latitud, longitud, orden,
                        costoAl, costoTrans,
                        comentarios
                    )
                    if( lugar.id > 0) {
                        dao.update(lugar)
                    } else {
                        dao.insert(lugar)
                    }
                    snackbarHostState.showSnackbar("Se agrego ${lugar.lugar} a la lista")

                }
            }) {
                Text("Guardar")
            }
            Button(onClick = {
                val intent = Intent(contexto, MainActivity::class.java)
                contexto.startActivity(intent)
            }) {
                Text("Volver")
            }
        }
    }
}