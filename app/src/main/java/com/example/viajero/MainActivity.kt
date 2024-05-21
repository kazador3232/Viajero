package com.example.viajero

import android.content.Context
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.viajero.db.Lugar
import com.example.viajero.db.LugarDB
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.time.LocalDateTime

class CameraAppViewModel : ViewModel() {
    val accion = mutableStateOf("")
    // callbacks
    var onPermisoCamaraOk : () -> Unit = {}
    var onPermisoUbicacionOk: () -> Unit = {}
    // lanzador permisos
    var lanzadorPermisos: ActivityResultLauncher<Array<String>>? = null
    fun cambiarPantallaFoto(){ accion.value = Accion.FOTO.toString()
    }
}
class FormLugarViewModel : ViewModel() {
    val lugar           = mutableStateOf("")
    val fotoLugar       = mutableStateOf<Uri?>(null)
}
class MainActivity : ComponentActivity() {
    val cameraAppVm:CameraAppViewModel by viewModels()
    lateinit var cameraController:LifecycleCameraController
    val lanzadorPermisos = //Aqui se solicitan los permisos necesarios.
        registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions()) {
            when {
                (it[android.Manifest.permission.ACCESS_FINE_LOCATION] ?:
                false) or (it[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?:
                false) -> {
                    Log.v("callback RequestMultiplePermissions", "permiso ubicacion granted")
                    cameraAppVm.onPermisoUbicacionOk()
                }
                (it[android.Manifest.permission.CAMERA] ?: false) -> {
                    Log.v("callback RequestMultiplePermissions", "permiso camara granted")
                    cameraAppVm.onPermisoCamaraOk()
                }
                else -> {
                }
            }
        }
    private fun setupCamara() {
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector =
            CameraSelector.DEFAULT_BACK_CAMERA
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraAppVm.lanzadorPermisos = lanzadorPermisos
        setupCamara()
        setContent {
            AppLugaresUI(cameraController)
        }
    }
}
fun generarNombreSegunFechaHastaSegundo():String = LocalDateTime
    .now().toString().replace(Regex("[T:.-]"), "").substring(0, 14)
//Generador del archivo que utiliza el nombre creado por defecto
fun crearArchivoImagenPrivado(contexto: Context): File = File(
    contexto.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
    "${generarNombreSegunFechaHastaSegundo()}.jpg"
)
fun uri2imageBitmap(uri: Uri, contexto: Context) =
    BitmapFactory.decodeStream(
        contexto.contentResolver.openInputStream(uri)
    ).asImageBitmap()
//Funcion que guarda la fotografia
fun tomarFotografia(cameraController: CameraController, archivo: File,
                    contexto: Context, imagenGuardadaOk:(uri: Uri)->Unit) {
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(archivo).build()
    cameraController.takePicture(outputFileOptions,
        ContextCompat.getMainExecutor(contexto), object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.also {
                    Log.v("tomarFotografia()::onImageSaved", "Foto guardada en ${it.toString()}")
                    imagenGuardadaOk(it)
                }
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("tomarFotografia()", "Error: ${exception.message}")
            }
        })
}
class SinPermisoException(mensaje:String) : Exception(mensaje)
//Funcion para obtener la ubicacion
fun getUbicacion(contexto: Context, onUbicacionOk:(location: Location) -> Unit) {
    try {
        val servicio =
            LocationServices.getFusedLocationProviderClient(contexto)
        val tarea =
            servicio.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        tarea.addOnSuccessListener {
            onUbicacionOk(it)
        }
    } catch (e:SecurityException) {
        throw SinPermisoException(e.message?:"No tiene permisos para conseguir la ubicación")
    }
}
enum class Accion {
    LISTAR, CREAR, EDITAR, DETALLE, FOTO
}
@Composable
fun AppLugaresUI(cameraController: CameraController) {
    val contexto                  = LocalContext.current
    val (lugares, setLugares) = remember{ mutableStateOf( emptyList<Lugar>() ) }
    val (seleccion, setSeleccion) = remember{ mutableStateOf<Lugar?>(null) }
    val (accion, setAccion)       = remember{ mutableStateOf(Accion.LISTAR) }
    val cameraAppViewModel:CameraAppViewModel = viewModel()
    val formLugarVm:FormLugarViewModel = viewModel()
    LaunchedEffect(lugares) {
        withContext(Dispatchers.IO) {
            val db = LugarDB.getInstance( contexto )
            setLugares( db.lugarDao().getAll() )
            Log.v("AppLugaresUI", "LaunchedEffect()")
        }
    }
    val onSave = {
        setAccion(Accion.LISTAR)
        setLugares(emptyList())
    }
    when(accion) {
        Accion.CREAR -> {LugarFormUI(null, onSave)}
        Accion.DETALLE -> {
            if (seleccion != null) {
                DetalleLugarUI(seleccion, onSave,
                    tomarFotoOnClick = {
                    cameraAppViewModel.cambiarPantallaFoto()
                    cameraAppViewModel.lanzadorPermisos?.launch(arrayOf(android.Manifest.permission.CAMERA))
                },
                    /*actualizarUbicacionOnClick = {
                        cameraAppViewModel.onPermisoUbicacionOk = {
                            getUbicacion(contexto) {
                            }
                        }
                        cameraAppViewModel.lanzadorPermisos?.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION))
                    }*/)
            }
        }
        Accion.FOTO -> {
            PantallaFotoUI(formLugarVm, cameraAppViewModel,
                cameraController)
        }
        else -> LugaresListadoUI(
            lugares, onSave,
            onAdd = { setAccion( Accion.CREAR )
            },
            onEdit = { lugar ->
                setSeleccion(lugar)
                setAccion( Accion.DETALLE) }
            )
    }
}
@Composable
fun LugaresListadoUI(
    lugares: List<Lugar>, onSave: () -> Unit = {},
    onAdd: () -> Unit = {},
    onEdit: (l: Lugar) -> Unit = {}
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAdd() },
                icon = {
                    Icon(
                        Icons.Filled.Add, contentDescription = "agregar"
                    )
                },
                text = { Text("Agregar lugares") }
            )
        }
    ) { contentPadding ->
        if( lugares.isNotEmpty() ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            )
            {
                items(lugares) { lugar ->
                    LugarItemUI(lugar, onSave) {
                        onEdit(lugar)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay lugares en la lista")
            }
        }
    }
}
@Composable
fun LugarItemUI(lugar:Lugar, onSave:() -> Unit = {}, onClick:() -> Unit = {},
                onEdit:(l:Lugar) -> Unit = {}) {
    val contexto        = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 10.dp)
        )
    {
        Spacer(modifier = Modifier.height(10.dp))
        AsyncImage(
            model = lugar.imagen,
            contentDescription = "Imagen del lugar",
            modifier = Modifier.width(100.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column {
            Text(lugar.lugar, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text("Costo x Noche ${lugar.costoAl}")
            Text("Traslado ${lugar.costoTrans}")
            Row(
                modifier = Modifier
                    .padding(vertical = 20.dp, horizontal = 20.dp)
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = "Ubicacion",
                    modifier = Modifier.clickable {

                        }
                )
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Editar lugar",
                    modifier = Modifier.clickable { onEdit(lugar) }
                )
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar eliminar lugar",
                    modifier = Modifier.clickable {
                        alcanceCorrutina.launch( Dispatchers.IO ) {
                            val dao = LugarDB.getInstance( contexto ).lugarDao()
                            dao.delete( lugar )
                            onSave()
                        }
                    }
                )
            }
        }
        Button(onClick = {
            onEdit(lugar)
        }) {
            Text("Ver detalle")
        }
        }
}
@Composable
fun DetalleLugarUI(lugar:Lugar, onSave:() -> Unit = {}, tomarFotoOnClick: () -> Unit, ) {
    val contexto        = LocalContext.current
    val alcanceCorrutina = rememberCoroutineScope()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        AsyncImage(
            model = lugar.imagen,
            contentDescription = "Imagen del lugar",
            modifier = Modifier.width(150.dp)
        )
        Text(lugar.lugar, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text("Costo x Noche ${lugar.costoAl}")
        Text("Traslado ${lugar.costoTrans}")
        Text("Comentarios: ${lugar.comentarios}")
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 20.dp)
        ) {
            Icon(
                Icons.Filled.AddCircle,
                contentDescription = "Eliminar eliminar lugar",
                modifier = Modifier.clickable {
                    tomarFotoOnClick()
                    }
            )
            Icon(
                Icons.Filled.Edit,
                contentDescription = "Editar lugar",
                modifier = Modifier.clickable {  }
            )
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar eliminar lugar",
                modifier = Modifier.clickable {
                    alcanceCorrutina.launch( Dispatchers.IO ) {
                        val dao = LugarDB.getInstance( contexto ).lugarDao()
                        dao.delete( lugar )
                        onSave()
                    }
                }
            )
        }
        Button(onClick = {
            onSave()
        }) {
            Text("Volver")
        }
        Spacer(modifier = Modifier.height(100.dp))
        Box(
            modifier = Modifier.width(150.dp).height(150.dp)
        ) {
            MapaOsmUI(latitud = 0.0, longitud = 0.0)
        }
    }
}

@Composable
fun LugarFormUI(l: Lugar?, onSave:() -> Unit = {}){
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
                    dao.insert(lugar)
                    snackbarHostState.showSnackbar("Se agrego ${lugar.lugar} a la lista")

                }
            }) {
                Text("Agregar")
            }
            Button(onClick = {
                onSave()
            }) {
                Text("Volver")
            }
        }
    }
}
@Composable
fun PantallaFotoUI(formLugarVm:FormLugarViewModel, appViewModel:
CameraAppViewModel, cameraController: CameraController
) {
    val contexto = LocalContext.current
    AndroidView(
        factory = {
            PreviewView(it).apply {
                controller = cameraController
            }
        },
        modifier = Modifier.fillMaxSize()
    )
    Button(onClick = {
        tomarFotografia(
            cameraController,
            crearArchivoImagenPrivado(contexto),
            contexto
        ) {//Se crea el archivo de la imagen y se pasa como parametro al ViewModel
            formLugarVm.fotoLugar.value = it
            appViewModel.cambiarPantallaFoto()
        }
    }) {
        Text("Tomar foto")
    }
}
@Composable
fun MapaOsmUI(latitud:Double, longitud:Double) {
    val contexto = LocalContext.current
    AndroidView(
        factory = {
            MapView(it).also {
                it.setTileSource(TileSourceFactory.MAPNIK)
                Configuration.getInstance().userAgentValue =
                    contexto.packageName
            }
        }, update = {
            it.overlays.removeIf { true }
            it.invalidate()
            it.controller.setZoom(15.0)
            val geoPoint = GeoPoint(latitud, longitud)
            it.controller.animateTo(geoPoint)
            val marcador = Marker(it)
            marcador.position = geoPoint
            marcador.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_CENTER)
            it.overlays.add(marcador)
        }
    )
}