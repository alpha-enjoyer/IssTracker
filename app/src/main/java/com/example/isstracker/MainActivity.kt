package com.example.isstracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppLocalesMetadataHolderService
import androidx.constraintlayout.widget.ConstraintAttribute
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet.Constraint
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    data class ISSPositionResponse(val message: String, val iss_position: ISSPosition)

    data class ISSPosition(
        val latitude: String,
        val longitude: String
    )


    data class AstroResponse(val number: Int, val people:List<Astro>)
    data class Astro(val name:String, val craft:String)

    private var statusLokasi = false
    private var statusAstro = false

    private lateinit var handler:Handler
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //LOGIC MENU
        val menu: BottomNavigationView = findViewById(R.id.navbar)

        menu.setOnNavigationItemSelectedListener { menuItem ->
            when(menuItem.itemId) {
                R.id.lokasi -> {
                    val menuLokasi = findViewById<ConstraintLayout>(R.id.kontenLokasi)
                    menuLokasi.visibility = View.VISIBLE

                    val menuListOrang = findViewById<ConstraintLayout>(R.id.kontenListOrang)
                    menuListOrang.visibility = View.GONE

                    true
                }
                R.id.list_orang -> {
                    val menuLokasi = findViewById<ConstraintLayout>(R.id.kontenLokasi)
                    menuLokasi.visibility = View.GONE

                    val menuListOrang = findViewById<ConstraintLayout>(R.id.kontenListOrang)
                    menuListOrang.visibility = View.VISIBLE

                    true
                }

                else -> {false}
            }
        }
        //END LOGIC MENU


        //HANDLE BUTTON HELPER
        val buttonHelper = findViewById<TextView>(R.id.helper)

        buttonHelper.setOnClickListener{
            showPopUp()
        }
        //END HANDLE BUTTON HELPER

        //LOGIC LOADING INDIKATOR

        updateLoadingIndikator()

        //END LOGIC LOADING INDIKATOR

        //DEKLARASI MAP
        val webView: WebView = findViewById(R.id.mapComponent)
        webView.settings.javaScriptEnabled = true

        webView.loadUrl("file:///android_asset/leaflet_map.html")
        //END DEKLARASI MAP

        //PEMANGGIlAN DATA LOKASI ISS
        var retrofit = Retrofit.Builder()
            .baseUrl("http://api.open-notify.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        var apiService = retrofit.create(ApiService::class.java)

        handler = Handler()
        runnable = object : Runnable {
            override fun run() {
                apiService.getISSPosition().enqueue(object : Callback<ISSPositionResponse> {
                    override fun onResponse(call: Call<ISSPositionResponse>, response: Response<ISSPositionResponse>) {
                        if (response.isSuccessful) {
                            //UBAH STATUS
                            statusLokasi = true
                            updateLoadingIndikator()

                            val issPositionResponse: ISSPositionResponse? = response.body()
                            // Lakukan sesuatu dengan data dari API
                            val latitude = issPositionResponse?.iss_position?.latitude
                            val longitude = issPositionResponse?.iss_position?.longitude
                            val kontenLongitude = findViewById<TextView>(R.id.textLongitude)
                            kontenLongitude.text = "Longitude : $longitude"

                            val kontenLatitude = findViewById<TextView>(R.id.textLatitude)
                            kontenLatitude.text = "Latitude : $latitude"

                            val judul = findViewById<TextView>(R.id.judulKonten)
                            judul.text = "Lokasi ISS saat ini : "

                            val kecepatan = findViewById<TextView>(R.id.textKecepatan)
                            kecepatan.text = "Kecepatan : 28.000 km/h"


                            //SET KOORDINAT MAP
                            val jsCode = "setKoordinat($latitude, $longitude);"
                            webView.evaluateJavascript(jsCode, null)
                            //END SET KOORDINAT MAP


                            Log.d("API Response", "Response: $issPositionResponse")
                        } else {
                            // Tangani respon tidak berhasil
                            statusLokasi = true
                            updateLoadingIndikator()

                            Log.e("API Response", "Response failed: ${response.code()}")
                        }

                    }



                    override fun onFailure(call: Call<ISSPositionResponse>, t: Throwable) {
                        // Tangani kesalahan saat permintaan gagal
                        statusLokasi = true
                        updateLoadingIndikator()

                        Log.e("API Request", "Request failed: ${t.message}")
                    }
                })
                //ULANGI BLOCK RUNNABLE TIAP 30 detik
                handler.postDelayed(this,30000)
            }
        }

        handler.postDelayed(runnable,0)
        //END PEMANGGILAN DATA LOKASI ISS


        //PEMANGGILAN DATA LIST ASTRO
        retrofit = Retrofit.Builder()
            .baseUrl("http://api.open-notify.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiServiceOrang::class.java)

        val callAstro: Call<AstroResponse> = apiService.getAstro()

        callAstro.enqueue(object : Callback<AstroResponse> {
            override fun onResponse(call: Call<AstroResponse>, response: Response<AstroResponse>) {
                if (response.isSuccessful) {
                    //UBAH STATUS
                    statusAstro = true
                    updateLoadingIndikator()

                    val astroResponse = response.body()
                    // Lakukan sesuatu dengan data dari API
                    val astro = astroResponse?.people

                    val kontenAstro = findViewById<TextView>(R.id.textAstro)

                    val formattedAstro = astro?.mapIndexed{index, astro -> "${index+1}. ${astro.name} (${astro.craft})"}
                    kontenAstro.text = "Orang yang berada di dalam ISS : \n"+ (formattedAstro?.joinToString("\n")
                        ?: "")
                    Log.d("API Response", "Response: $astroResponse")
                } else {
                    statusAstro = true
                    updateLoadingIndikator()
                    // Tangani respon tidak berhasil

                    Log.e("API Response", "Response failed: ${response.code()}")
                }

            }



            override fun onFailure(call: Call<AstroResponse>, t: Throwable) {
                // Tangani kesalahan saat permintaan gagal
                statusAstro = true
                updateLoadingIndikator()

                Log.e("API Request", "Request failed: ${t.message}")
            }
        })
        //END PEMANGGIlAN DATA LIST ASTRO




    }

    //FUNGSI HELPER MESSAGE
    private fun showPopUp() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Penjelasan Singkat")
        alertDialog.setMessage("- International Space Station (ISS) adalah pesawat luar angkasa yang sangat besar. Pesawat ini bergerak mengelilingi bumi. Ini juga merupakan tempat para astronot tinggal saat menjalani misi di luar angkasa. International Space Station (ISS) sekaligus merupakan lab untuk penelitian.\n\n" +
                "- Latitude atau Garis Lintang adalah garis yang menentukan jarak di sebelah utara atau selatan Khatulistiwa.\n\n" +
                "- Longitude atau Garis Bujur adalah garis yang membentang dari utara ke selatan")
        alertDialog.setPositiveButton("OK",null)
        alertDialog.show()
    }
    //END FUNGSI HELPER MESSAGE


    private fun updateLoadingIndikator() {
        val loadingIndikator: ProgressBar = findViewById(R.id.loadingIndikator)

        if(statusLokasi && statusAstro) {
            loadingIndikator.visibility = View.GONE
        } else {
            loadingIndikator.visibility = View.VISIBLE
        }
    }

    interface ApiService {
        @GET("iss-now.json")
        fun getISSPosition(): Call<ISSPositionResponse>
    }

    interface ApiServiceOrang : ApiService {
        @GET("astros.json")
        fun getAstro(): Call<AstroResponse>
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}