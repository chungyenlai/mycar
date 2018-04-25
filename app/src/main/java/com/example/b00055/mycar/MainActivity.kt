package com.example.b00055.mycar

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*
import kotlin.text.Charsets.US_ASCII

class MainActivity : AppCompatActivity() {
    val myDeviceName="Arduino Chungyen"

     var mBluetoothAdapter: BluetoothAdapter =BluetoothAdapter.getDefaultAdapter()
     var mmSocket: BluetoothSocket? = null
     var mmDevice: BluetoothDevice? = null
     var mmOutputStream: OutputStream? = null
     var mmInputStream: InputStream? = null
     var workerThread: Thread? = null
     var readBuffer: ByteArray=ByteArray(1024)
     var readBufferPosition: Int = 0
     var counter: Int = 0
    val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") //Standard SerialPortService ID
    var openButton :Button ?= null
    var sendButton :Button ?= null
    var closeButton :Button ?= null
    var downButton :Button ?= null
    var myLabel: TextView?=null
    var myTextbox: EditText?=null
    @Volatile
    internal var stopWorker: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        myLabel=findViewById(R.id.label)
        myTextbox=findViewById(R.id.entry)
        openButton = findViewById(R.id.open)
        sendButton = findViewById(R.id.send)
        closeButton = findViewById(R.id.close)
        downButton=findViewById(R.id.Down)
        openButton?.setOnClickListener {
            try {
                findBT()
                openBT()
            } catch (ex: IOException) {
            }
        }

        sendButton?.setOnTouchListener(object:View.OnTouchListener{
            override fun onTouch(v: View, m: MotionEvent): Boolean {
                // Perform tasks here
                if (m.getAction() == MotionEvent.ACTION_DOWN) {
                    sendData("w")
                }
                if (m.getAction() == MotionEvent.ACTION_UP) {
                    sendData("r")
                }
                return true
            }
        })
        downButton?.setOnTouchListener{v: View, event: MotionEvent ->
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendData("s")
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                sendData("r")
            }
             true
        }
        myTextbox?.visibility=View.INVISIBLE
        /*
        sendButton?.setOnClickListener {
            try {
                sendData()
            } catch (ex: IOException) {
            }
        }*/
        closeButton?.setOnClickListener {
            try {
                closeBT()
            } catch (ex: IOException) {
            }
        }
    }



    fun findBT() {
        if (!mBluetoothAdapter.isEnabled()) {
            val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetooth, 0)
        }
        val pairedDevices = mBluetoothAdapter.getBondedDevices()
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                if (device.name == myDeviceName) {
                    mmDevice = device
                    break
                }
            }
        }
        myLabel?.text = "Bluetooth Device Found"
    }

    @Throws(IOException::class)
    fun openBT() {
        mmSocket = mmDevice?.createRfcommSocketToServiceRecord(uuid)
        mmSocket!!.connect()
        mmOutputStream = mmSocket!!.getOutputStream()
        mmInputStream = mmSocket!!.getInputStream()

        beginListenForData()

        myLabel?.text = "Bluetooth Opened"
    }

     fun beginListenForData() {
        val handler = Handler()
        val delimiter: Byte = 35 //This is the ASCII code for a newline character
        stopWorker = false
        readBufferPosition = 0

        workerThread = Thread(Runnable {
            while (!Thread.currentThread().isInterrupted && !stopWorker) {
                try {
                    val bytesAvailable = mmInputStream!!.available()
                    if (bytesAvailable > 0) {
                        val packetBytes = ByteArray(bytesAvailable)
                        mmInputStream!!.read(packetBytes)
                        for (i in 0 until bytesAvailable) {
                            val b = packetBytes[i]
                            if (b == delimiter) {
                                val encodedBytes = ByteArray(readBufferPosition)
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.size)
                                val data = String(encodedBytes, US_ASCII)
                                Log.d("SKY", data)
                                readBufferPosition = 0

                                handler.post { myLabel?.text = data }
                            } else {
                                readBuffer[readBufferPosition++] = b
                            }
                        }
                    }
                } catch (ex: IOException) {
                    stopWorker = true
                }

            }
        })

        workerThread?.start()
    }

    @Throws(IOException::class)
     fun sendData(string:String ) {
        var msg = string//myTextbox?.text.toString()
     //   msg += "\n"
        mmOutputStream?.write(msg.toByteArray())
        myLabel?.text = "Data Sent:"+string
       // myTextbox?.setText(" ")
    }

    @Throws(IOException::class)
    internal fun closeBT() {
        stopWorker = true
        mmOutputStream?.close()
        mmInputStream?.close()
        mmSocket?.close()
        myLabel?.text = "Bluetooth Closed"
    }
}
