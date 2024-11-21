package com.studiowavi.hwick

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.studiowavi.hwick.databinding.ActivityMainBinding
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding : ActivityMainBinding

    // 효과음 변수 세팅
    private lateinit var soundPool : SoundPool
    private var soundIdSwing : Int = 0
    private var soundIdJump : Int = 0
    private var sfxType = "swing"

    // 센서 변수 세팅
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isSwinging = false // 휘두름 상태 플래그
    private val swingThreshold = 80.0f // 센서 제어값 상수 세팅

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSoundPool()
        initLayout()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // ================================
    // 운동 센서 파트
    // ================================
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                when (sfxType) {
                    "swing" -> {
                        handleSwinging(x, y, z)
                    }
                    "jump" -> {
                        handleJumping(x, y, z)
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    //  휘두름 동작 처리
    private fun handleSwinging(x: Float, y: Float, z: Float) {
        val accelerationMagnitude = sqrt(x * x + y * y + z * z)

        if (accelerationMagnitude > swingThreshold) {
            // 휘두름 감지 처리

            if (!isSwinging) {
                //  새로운 휘두름 동작 시작
                isSwinging = true
                playSound("swing")
            }
        }
        else {
            // 가속도가 줄어들면 휘두름 종료
            isSwinging = false
        }
    }

    private fun handleJumping(x: Float, y: Float, z: Float) {

    }

    // ================================
    // 레이아웃 & 사운드 세팅 파트
    // ================================

    fun initSoundPool() {
        soundPool = SoundPool.Builder().setMaxStreams(10).build()
        soundIdSwing = soundPool.load(this, R.raw.sfx_swosh_001, 1)
        soundIdJump = soundPool.load(this, R.raw.sfx_swosh_003, 1)
        soundPool.setOnLoadCompleteListener { soundPool, sampleId, status ->
        }
    }

    fun playSound(type:String) {
        when(type){
            "swing" -> {
                soundPool.play(soundIdSwing, 5.0f, 5.0f, 0, 0, 1.0f)
            }
            "jump" -> {
                soundPool.play(soundIdJump, 5.0f, 5.0f, 0, 0, 1.0f)
            }
        }
    }

    private fun initLayout() {
        initSwitchButton()
        initOptionPickerSwing()
    }

    fun initSwitchButton(){
        binding.btnSwing.setOnClickListener {
            playSound("swing")
            sfxType = "swing"
        }

        binding.btnJump.setOnClickListener {
            playSound("jump")
            sfxType = "jump"
        }
    }

    fun initOptionPickerSwing() {

        binding.btnOptionPicker.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.inflate(R.menu.popup_menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.option1 -> {
                        soundIdSwing = soundPool.load(this, R.raw.sfx_swosh_001, 1)
                        binding.btnOptionPicker.text = getString(R.string.sfx_swosh_001)
                        true
                    }
                    R.id.option2 -> {
                        soundIdSwing = soundPool.load(this, R.raw.sfx_swosh_002, 1)
                        binding.btnOptionPicker.text = getString(R.string.sfx_swosh_002)
                        true
                    }
                    R.id.option3 -> {
                        soundIdSwing = soundPool.load(this, R.raw.sfx_swosh_003, 1)
                        binding.btnOptionPicker.text = getString(R.string.sfx_swosh_001)
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

}