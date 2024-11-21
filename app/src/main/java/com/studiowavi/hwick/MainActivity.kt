package com.studiowavi.hwick

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.values
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.studiowavi.hwick.databinding.ActivityMainBinding
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding : ActivityMainBinding

    private lateinit var soundPool : SoundPool
    private var soundId : Int = 0

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val shakeThreshold = 50.0f
    private val minSoundInterval = 300L // 최소 소리 재생 간격 (500ms)
    private var lastSoundTime = 0L // 마지막 소리 재생 시간 기록
    private var isSwinging = false // 휘두름 상태 플래그

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSoundPool()
        initOptionPicker()
        initPlayButton()

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

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                val accelerationMagnitude = sqrt(x * x + y * y + z * z)

                if (accelerationMagnitude > shakeThreshold) {
                        // 휘두름 감지 처리
                        Log.d("센서","감지시작")
                        handleSwinging(x, y, z)
                    }
                    else {
                        Log.d("센서","멈춤")
                        // 가속도가 줄어들면 휘두름 종료
                        isSwinging = false
                    }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    //  휘두름 동작 처리
    private fun handleSwinging(x: Float, y: Float, z: Float) {
        val currentTime = System.currentTimeMillis()

        if (!isSwinging) {
            //  새로운 휘두름 동작 시작
            isSwinging = true
            lastSoundTime = currentTime
            playSound()
        } else {
            // 같은 방향에서의 휘두름 유지 시 추가 소리 차단
            if (currentTime - lastSoundTime >= minSoundInterval) {
                // 특정 축의 변화량으로 방향 유지 확인
                if (abs(x) > abs(y) && abs(x) > abs(z)) {
                    // X축을 따라 길게 유지 중
                    Log.d( "센서","X축 유지 중")
                } else if (abs(y) > abs(x) && abs(y) > abs(z)) {
                    // Y축을 따라 길게 유지 중
                    Log.d( "센서","Y축 유지 중")
                } else {
                    // Z축을 따라 길게 유지 중
                    Log.d( "센서","Z축 유지 중")
                }
            }
        }
    }

    fun initSoundPool() {
        soundPool = SoundPool.Builder().setMaxStreams(10).build()
        soundId = soundPool.load(this, R.raw.sfx_swosh_001, 1)
        soundPool.setOnLoadCompleteListener { soundPool, sampleId, status ->
        }
    }

    fun initOptionPicker() {
        binding.btnOptionPicker.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.inflate(R.menu.popup_menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.option1 -> {
                        soundId = soundPool.load(this, R.raw.sfx_swosh_001, 1)
                        binding.btnOptionPicker.text = "옵션1"
                        true
                    }
                    R.id.option2 -> {
                        soundId = soundPool.load(this, R.raw.sfx_swosh_002, 1)
                        binding.btnOptionPicker.text = "옵션2"
                        true
                    }
                    R.id.option3 -> {
                        soundId = soundPool.load(this, R.raw.sfx_swosh_003, 1)
                        binding.btnOptionPicker.text = "옵션3"
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    fun playSound() {
        soundPool.play(soundId, 5.0f, 5.0f, 0, 0, 1.0f)
    }

    fun initPlayButton() {
        binding.btnPlay.setOnClickListener {
            playSound()
        }
    }

}