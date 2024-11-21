package com.studiowavi.hwick

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.studiowavi.hwick.databinding.ActivityMainBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding

    private lateinit var soundPool: SoundPool
    private var soundIdSwing: Int = 0
    private var soundIdJump: Int = 0

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val minSoundInterval = 300L // 최소 소리 재생 간격
    private var lastSwingTime = 0L // 마지막 스윙 소리 재생 시간
    private var lastJumpTime = 0L // 마지막 점프 감지 시간

    private var previousX = 0f
    private var previousY = 0f
    private var previousZ = 0f
    private var lastSwingDirection = 0f

    private var isSwinging = false
    private var isJumping = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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


    // SensorEvent 처리
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                handleSwingingAndJumping(x, y, z)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    // 스윙 동작 처리
    private fun handleSwingingAndJumping(x: Float, y: Float, z: Float) {
        val currentTime = System.currentTimeMillis()
        val deltaX = abs(x - previousX)
        val deltaY = abs(y - previousY)
        val deltaZ = abs(z - previousZ)

        // 이동 범위 추적 (이전 값과 현재 값의 차이)
        val movementRange = deltaX + deltaY + deltaZ

        // 이전 위치 값 갱신
        previousX = x
        previousY = y
        previousZ = z

        // 점프 우선: 작은 움직임 범위
        if (movementRange < 10.0f) {
            handleJumping(z)
        }
        else if (!isSwinging && movementRange >= 50.0f) {  // 큰 범위일 때 스윙
            val currentSwingDirection = if (abs(x) > abs(y)) x else y  // X 또는 Y 축 기준으로 휘두름 방향 결정

            // 휘두를 때 방향이 많이 바뀐 경우, 소리가 빠르게 반복되도록 설정
            if (abs(currentSwingDirection - lastSwingDirection) > 10f || currentTime - lastSwingTime > minSoundInterval) {
                isSwinging = true
                lastSwingTime = currentTime
                playSound("swing")
                Log.d("스윙", "스윙 감지: movementRange=$movementRange, 방향=$currentSwingDirection")
                lastSwingDirection = currentSwingDirection

                // 1초 뒤 스윙 상태 초기화
                binding.root.postDelayed({ isSwinging = false }, 1000)
            }
        }
    }

    // 점프 동작 처리
    private fun handleJumping(z: Float) {
        val currentTime = System.currentTimeMillis()

        // 점프 시작 감지 (범위가 크면 점프 소리 안 나도록)
        if (!isJumping && z > 12 && currentTime - lastJumpTime > minSoundInterval) {
            isJumping = true
            lastJumpTime = currentTime
            Log.d("점프", "점프 시작: z=$z")

            // 0.5초 뒤 점프 상태 초기화
            binding.root.postDelayed({
                if (isJumping) {
                    Log.d("점프", "점프 상태 초기화")
                    isJumping = false
                }
            }, 500)
        }

        // 점프 중 착지 감지
        if (isJumping && z < -8) {
            playSound("jump")
            Log.d("점프", "착지 감지: z=$z")
            isJumping = false
        }
    }


    // SoundPool 초기화
    private fun initSoundPool() {
        soundPool = SoundPool.Builder().setMaxStreams(10).build()
        soundIdSwing = soundPool.load(this, R.raw.sfx_swosh_001, 1)
        soundIdJump = soundPool.load(this, R.raw.sfx_swosh_003, 1)
    }

    private fun playSound(action: String) {
        when (action) {
            "swing" -> {
                soundPool.play(soundIdSwing, 1.0f, 1.0f, 0, 0, 1.0f)
            }
            "jump" -> {
                soundPool.play(soundIdJump, 1.0f, 1.0f, 0, 0, 1.0f)
            }
        }
    }

    // 옵션 피커 초기화
    private fun initOptionPicker() {
        binding.btnOptionPicker.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.inflate(R.menu.popup_menu)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.option1 -> {
                        soundIdSwing = soundPool.load(this, R.raw.sfx_swosh_001, 1)
                        binding.btnOptionPicker.text = getString(R.string.sfx_group01)
                        true
                    }
                    R.id.option2 -> {
                        soundIdSwing = soundPool.load(this, R.raw.sfx_swosh_002, 1)
                        binding.btnOptionPicker.text = getString(R.string.sfx_group02)
                        true
                    }
                    R.id.option3 -> {
                        soundIdSwing = soundPool.load(this, R.raw.sfx_swosh_003, 1)
                        binding.btnOptionPicker.text = getString(R.string.sfx_group03)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    // 플레이 버튼 초기화
    private fun initPlayButton() {
        binding.btnPlay.setOnClickListener {
            playSound("swing")
        }
    }
}
