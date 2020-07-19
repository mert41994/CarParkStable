/*
//  CREATED BY ERKAN EMİR BİLMEÇ & EMRE MERT KARACA
//  2019
*/

package com.emk.carpark

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.DialogTitle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_main.*
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region

//counter left
class MainActivity : AppCompatActivity(), BeaconConsumer, View.OnClickListener {
    companion object {
        const val TAG = "MainActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

    }

    private lateinit var beaconManager: BeaconManager
    private lateinit var region: Region
    private var scanType: Int = 0

     private fun getBlueToothOn() {
         val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
         if (bluetoothAdapter?.isEnabled == false) {
             val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
             val REQUEST_ENABLE_BT = 1
             startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
         }
     }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.action_about)
        {
            showInfo()
        }
        return true
    }

    private fun showInfo()
    {
        val dialogTitle = getString(R.string.about_title)
        val dialogMessage = getString(R.string.about_message)
        val builder = AlertDialog.Builder(this)
        builder.setTitle(dialogTitle)
        builder.setMessage(dialogMessage)
        builder.create().show()
    }


    private fun incrementCounter(ref: DocumentReference): Task<Void> {

        val db = FirebaseFirestore.getInstance()
        val shardRef = db.collection("counter").document("carCount")
        return shardRef.update("count", FieldValue.increment(1))
    }

    private fun decrementCounter(ref: QuerySnapshot): Task<Void> {

        val db = FirebaseFirestore.getInstance()
        val shardRef = db.collection("counter").document("carCount")
        return shardRef.update("count", FieldValue.increment(-1))
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        region = Region("myRangingUniqueId", null, null, null)

        beaconManager = BeaconManager.getInstanceForApplication(this)
        btnEnter.setOnClickListener(this)
        btnExit.setOnClickListener(this)
        getBlueToothOn()
        val locCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)

        if (locCheck != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            Toast.makeText(this, "Uygulama Konuma erişemiyor. Lütfen izin veriniz.", Toast.LENGTH_SHORT).show()
        }
        else
        {
            Log.w(TAG, "Location service is active")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.unbind(this)
    }

    override fun onBeaconServiceConnect() {
        beaconManager.addRangeNotifier { beacons, _ ->
            if (beacons.isNotEmpty()) {
                beaconManager.removeAllRangeNotifiers()
                beaconManager.unbind(this)

                val db = FirebaseFirestore.getInstance()
                if (scanType == 0) {
                    db.collection("uuid")
                        .whereEqualTo("enter", true)
                        .get()
                        .addOnSuccessListener { documents ->
                            for (document in documents) {
                                val beaconsIterator = beacons.iterator()
                                while (beaconsIterator.hasNext()) {
                                    if (document.id == beaconsIterator.next().id1.toString()) {
                                        Log.w(TAG, "enter ids equal")

                                        val pref =
                                            applicationContext.getSharedPreferences("CarPark", Context.MODE_PRIVATE)
                                        val currentPlate = pref.getString("plate", "")
                                        if (currentPlate.isNullOrEmpty()) {
                                            // hata durumu
                                            Toast.makeText(this, "Hata", Toast.LENGTH_SHORT).show()
                                            return@addOnSuccessListener
                                        }

                                        val checkIn = hashMapOf(
                                            "enter" to System.currentTimeMillis(),
                                            "exit" to null,
                                            "time price" to 0
                                        )

                                        db.collection("users")
                                            .document(currentPlate)
                                            .collection("checkins")
                                            .add(checkIn)
                                            .addOnSuccessListener {
                                                Log.d(TAG, "Enter DocumentSnapshot added")
                                                incrementCounter(it)
                                                // car count ++
                                                Toast.makeText(this, "Giriş kaydı başarılı bir şekilde alındı.", Toast.LENGTH_SHORT).show()

                                            }
                                            .addOnFailureListener { e ->
                                                Log.w(TAG, "Error adding document", e)
                                                Toast.makeText(this, "Hata!", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w(TAG, "Error getting documents: ", exception)
                        }

                } else {
                    db.collection("uuid")
                        .whereEqualTo("enter", false)
                        .get()
                        .addOnSuccessListener { documents1 ->
                            for (document in documents1) {
                                val beaconsIterator = beacons.iterator()
                                while (beaconsIterator.hasNext()) {
                                    if (document.id == beaconsIterator.next().id1.toString()) {
                                        Log.w(TAG, "ids equal 1")

                                        val pref =
                                            applicationContext.getSharedPreferences("CarPark", Context.MODE_PRIVATE)
                                        val currentPlate = pref.getString("plate", "")
                                        if (currentPlate.isNullOrEmpty()) {
                                            return@addOnSuccessListener

                                        }

                                        db.collection("users")
                                            .document(currentPlate)
                                            .collection("checkins")
                                            .orderBy("enter", Query.Direction.DESCENDING)
                                            .limit(1)
                                            .get()
                                            .addOnSuccessListener { // it = document
                                                val id = it.documents.first().id
                                                var totalCost : Double = (System.currentTimeMillis().toDouble() - it.documents.first().get("enter").toString().toDouble()) / 6000000 * 17
                                                totalCost = totalCost.toString().substring(0,4).toDouble()
                                                decrementCounter(it)
                                                Toast.makeText(this, "Çıkış kaydı başarılı bir şekilde alındı.\nÜcret:" + totalCost + "TL'dir." , Toast.LENGTH_SHORT).show()

                                                val checkIn = hashMapOf(
                                                    "enter" to it.documents.first().get("enter"), //giriş
                                                    "exit" to System.currentTimeMillis(), //exit
                                                    "time price" to (System.currentTimeMillis().toDouble() - it.documents.first().get("enter").toString().toDouble()) / 6000000 * 17  // hesapla
                                                )

                                                db.collection("users")
                                                    .document(currentPlate)
                                                    .collection("checkins")
                                                    .document(id)
                                                    .set(checkIn)
                                                    .addOnSuccessListener {
                                                        Log.d(TAG, "Exit DocumentSnapshot added")
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.w(TAG, "Error adding document", e)

                                                    }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.w(TAG, "Error adding document", e)
                                            }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w(TAG, "Error getting documents: ", exception)
                        }
                }
            }
        }

        try {
            beaconManager.startRangingBeaconsInRegion(region)
        } catch (e: RemoteException) {
        }
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btnEnter -> {
                scanType = 0
                getBlueToothOn()
                val db = FirebaseFirestore.getInstance()
                db.collection("counter")
                    .document("carCount")
                    .get()
                    .addOnSuccessListener { // it = document
                        it.id
                        beaconManager.bind(this)
                        Toast.makeText(this, "Giriş Yapılıyor.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding document", e)
                    }
            }
            R.id.btnExit -> {
                scanType = 1
                getBlueToothOn()
                beaconManager.bind(this)
                Toast.makeText(this, "Çıkış Yapılıyor." , Toast.LENGTH_SHORT).show()
            }

            R.id.btnCarparkLocation -> {
                Toast.makeText(this, "Haritalar açılıyor." , Toast.LENGTH_SHORT).show()
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo: 40.742218, 30.324969?q= Sakarya Üniversitesi Bilgisayar ve Bilişim Bilimleri Fakültesi")))
            }

        }
    }
}