package com.studiowavi.hwick

import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.studiowavi.hwick.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding

    // 효과음 변수 세팅
    private lateinit var soundPool: SoundPool
    private var soundIdSwing: Int = 0
    private var soundIdJump: Int = 0
    private var soundIdWalk: Int = 0
    private var sfxType = "swing"
    private var effectVolume = 1.0f  // 음악 볼륨 (기본값 1.0)
    private var walkingVolume = 0.0f

    // 센서 변수 세팅
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gravitySensor: Sensor? = null

    private var isSwinging = false // 휘두름 상태 플래그
    private val swingThreshold = 80.0f // 센서 제어값 상수 세팅

    private var isJumping = false // 휘두름 상태 플래그
    private var gDirection = "z" // 기울어진 방향
    private var recentTimestamp = 0L // 점프 시간대조
    private var currentTimestamp = 0L

    private var blockingStep = false
    private var blockingStepJob: Job? = null
    private var lastStepTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSensorManager()
        initSoundPool()
        initLayout()

        // 권한 확인 및 요청
        checkPermission()
    }

    override fun onResume() {
        super.onResume()
        sensorManager.also {
            it.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            it.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL)
            it.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // ================================
    // Setting :: 운동 센서
    // ================================

    private fun initSensorManager() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    }

    override fun onSensorChanged(event: SensorEvent?) {

        event?.let {
            var ax: Float = 0f
            var ay: Float = 0f
            var az: Float = 0f
            var gx: Float = 0f
            var gy: Float = 0f
            var gz: Float = 0f
            var steps: Int = 0

            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    // 가속도계 이벤트 처리
                    ax = event.values[0]
                    ay = event.values[1]
                    az = event.values[2]
                }

                Sensor.TYPE_GRAVITY -> {
                    // 중력 센서 이벤트 처리
                    gx = event.values[0]
                    gy = event.values[1]
                    gz = event.values[2]
                }

                Sensor.TYPE_STEP_COUNTER -> {
                    steps = event.values[0].toInt()
                    handleWalking(steps)
                }
            }

            when (sfxType) {
                "swing" -> {
                    handleSwinging(ax, ay, az)
                }

                "jump" -> {
                    checkDirection(gx, gy, gz)
                    handleJumping(ax, ay, az)
                }

                "everything" -> {
                    handleSwinging(ax, ay, az)
                    checkDirection(gx, gy, gz)
                    handleJumping(ax, ay, az)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    // ================================
    // Handle :: 워킹 / 스윙 / 점프
    // ================================

    private fun handleWalking(steps: Int) {
        if (sfxType == "everything" || sfxType == "walk") {

            // 걸음 수에 따라 walkScale을 업데이트
            val elapsedTime = System.currentTimeMillis() - lastStepTime

            walkingVolume += 0.05f
            walkingVolume = walkingVolume.coerceAtMost(1.0f) // 최대값 1.0f로 제한
            if (elapsedTime > 1000) {
                // 1초 동안 걸음이 없으면 walkScale을 0으로 리셋
                walkingVolume = 0.0f
            }

            // 걸음 수가 1초 내에 감지되었으면 playSound 호출
            if (!blockingStep && walkingVolume > 0) {
                playSound("walk")
            }

            // 마지막 걸음 시간을 업데이트
            lastStepTime = System.currentTimeMillis()

        }
    }

    private fun blockingStep() {
        blockingStep = true
        walkingVolume = 0f

        blockingStepJob?.cancel()
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            blockingStep = false
        }
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

                isJumping = false
                blockingStep()
            }
        } else {
            // 가속도가 줄어들면 휘두름 종료
            isSwinging = false
        }
    }

    private fun checkDirection(gx: Float, gy: Float, gz: Float) {
        when {
            abs(gx) > abs(gy) && abs(gx) > abs(gz) -> {
                gDirection = "x"
            }

            abs(gy) > abs(gx) && abs(gy) > abs(gz) -> {
                gDirection = "y"
            }

            abs(gz) > abs(gx) && abs(gz) > abs(gy) -> {
                gDirection = "z"
            }
        }
    }

    private fun handleJumping(ax: Float, ay: Float, az: Float) {

        fun jump(aa: Float) {
            if (aa > -50 && aa < -8 && !isJumping) {
                isJumping = true
                recentTimestamp = System.currentTimeMillis()

                CoroutineScope(Dispatchers.Main).launch {
                    delay(450)
                    if (isJumping == true) {
                        isJumping = false
                    }
                }

                blockingStep()
            }

            // 점프 후 하강 중인 경우 감지 (Z축 값이 0에 가까워지면)
            if (aa < 30 && aa > 20 && isJumping) {
                isJumping = false
                currentTimestamp = System.currentTimeMillis()

                if (currentTimestamp - recentTimestamp in 200..450) {
                    playSound("jump")
                }
            }
        }

        when (gDirection) {
            "x" -> {
                jump(ax)
            }

            "y" -> {
                jump(ay)
            }

            "z" -> {
                jump(az)
            }
        }
    }

    // ================================
    // Setting :: 사운드
    // ================================

    fun initSoundPool() {
        soundPool = SoundPool.Builder().setMaxStreams(10).build()
        soundIdSwing = soundPool.load(this, R.raw.sfx_swosh_001, 2)
        soundIdJump = soundPool.load(this, R.raw.sfx_swosh_003, 1)
        soundIdWalk = soundPool.load(this, R.raw.sfx_walk_001, 0)
        soundPool.setOnLoadCompleteListener { soundPool, sampleId, status ->
        }
    }

    fun playSound(type: String) {
        when (type) {
            "swing" -> {
                soundPool.play(soundIdSwing, 5f*effectVolume, 5f*effectVolume, 2, 0, 1.0f)
            }

            "jump" -> {
                soundPool.play(soundIdJump, 5f*effectVolume, 5f*effectVolume, 1, 0, 1.0f)
            }

            "walk" -> {
                soundPool.play(soundIdWalk, walkingVolume*effectVolume, walkingVolume*effectVolume, 0, 0, 1.0f)
            }
        }
    }

    // ================================
    // Setting :: 레이아웃
    // ================================

    private fun initLayout() {
        initSwitchButton()
        initSeekBar()
        initOptionPickerSwing()
    }

    fun initSwitchButton() {
        binding.btnSwing.setOnClickListener {
            playSound("swing")
            sfxType = "swing"
        }

        binding.btnJump.setOnClickListener {
            playSound("jump")
            sfxType = "jump"
        }

        binding.btnEverything.setOnClickListener {
            sfxType = "everything"
        }
    }

    fun initSeekBar(){
        binding.seekBarEffectVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                effectVolume = when (progress){
                    in 0.. 20 -> {
                        0.01f
                    }
                    in 21.. 40 -> {
                        0.5f
                    }
                    in 41.. 60 -> {
                        1f
                    }
                    in 61.. 80 -> {
                        3f
                    }
                    else -> {
                        10f
                    }
                }

//                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
//                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
//                val reducedVolume = maxVolume * progress/100
//                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, reducedVolume, 0)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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

    // ================================
    // Setting :: 퍼미션 권한 획득
    // ================================

    private fun checkPermission() {
        // Android 10 이상에서는 ACCESS_FINE_LOCATION 권한이 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 활동 인식 권한 요청
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                // 권한이 이미 부여된 경우
                Log.d("MainActivity", "ACTIVITY_RECOGNITION 권한 이미 부여됨.")
            }
        } else {
            // Android 10 미만에서는 다른 권한이 필요할 수 있음
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 허용된 경우
                Log.d("MainActivity", "권한 허용됨")
            } else {
                // 권한 거부된 경우
                Toast.makeText(this, "권한이 거부되었습니다. 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
    }

}