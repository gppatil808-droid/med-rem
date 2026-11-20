package com.example.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.MedicineEntity
import com.example.data.ReminderSettings
import com.example.data.ScheduleTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class TtsPlaybackState(
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val activeKey: String? = null,
    val errorMessage: String? = null,
)

class MedicineTtsManager(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _state = MutableStateFlow(TtsPlaybackState())
    val state: StateFlow<TtsPlaybackState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isInitializing = false
    private var pendingUtterance: PendingSpeech? = null
    private var activeUtteranceId: String? = null
    private var lastDedupeKey: String? = null
    private var lastDedupeAtMillis: Long = 0L
    private var focusRequest: AudioFocusRequest? = null

    private data class PendingSpeech(
        val text: String,
        val key: String,
        val utteranceId: String,
        val rate: Float,
        val pitch: Float,
        val onDone: (() -> Unit)? = null,
    )

    init {
        initEngine()
    }

    private fun initEngine() {
        if (isInitializing) return
        isInitializing = true
        var createdEngine: TextToSpeech? = null
        createdEngine = TextToSpeech(appContext) { status ->
            mainHandler.post {
                isInitializing = false
                val engine = createdEngine ?: tts
                if (status == TextToSpeech.SUCCESS && engine != null) {
                    tts = engine
                    configureLanguage(engine)
                    configureAudioAttributes(engine)
                    engine.setOnUtteranceProgressListener(
                        object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                mainHandler.post {
                                    _state.value = _state.value.copy(
                                        isSpeaking = true,
                                        errorMessage = null,
                                    )
                                }
                            }

                            override fun onDone(utteranceId: String?) {
                                mainHandler.post {
                                    abandonAudioFocus()
                                    if (utteranceId == activeUtteranceId) {
                                        val finishedCallback = pendingUtterance?.takeIf {
                                            it.utteranceId == utteranceId
                                        }?.onDone
                                        pendingUtterance = null
                                        activeUtteranceId = null
                                        _state.value = _state.value.copy(
                                            isSpeaking = false,
                                            activeKey = null,
                                        )
                                        finishedCallback?.invoke()
                                    }
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                mainHandler.post {
                                    handleUtteranceError(utteranceId, "Unable to play voice announcement.")
                                }
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                mainHandler.post {
                                    handleUtteranceError(
                                        utteranceId,
                                        "Voice announcement failed (code $errorCode).",
                                    )
                                }
                            }
                        },
                    )
                    _state.value = _state.value.copy(isReady = true, errorMessage = null)
                    pendingUtterance?.let { queued ->
                        pendingUtterance = null
                        speakInternal(queued)
                    }
                } else {
                    Log.e(TAG, "TextToSpeech init failed with status=$status")
                    _state.value = _state.value.copy(
                        isReady = false,
                        isSpeaking = false,
                        errorMessage = "Text-to-speech engine unavailable on this device.",
                    )
                    pendingUtterance?.onDone?.invoke()
                    pendingUtterance = null
                }
            }
        }
        if (tts == null) {
            tts = createdEngine
        }
    }

    private fun handleUtteranceError(utteranceId: String?, message: String) {
        abandonAudioFocus()
        if (utteranceId == null || utteranceId == activeUtteranceId) {
            val finishedCallback = pendingUtterance?.onDone
            pendingUtterance = null
            activeUtteranceId = null
            _state.value = _state.value.copy(
                isSpeaking = false,
                activeKey = null,
                errorMessage = message,
            )
            finishedCallback?.invoke()
        }
    }

    private fun configureLanguage(engine: TextToSpeech) {
        val candidates = listOfNotNull(
            Locale.getDefault(),
            Locale.US,
            Locale.ENGLISH,
            engine.defaultVoice?.locale,
        )
        for (locale in candidates) {
            val res = engine.setLanguage(locale)
            if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                return
            }
        }
        Log.w(TAG, "No preferred TTS locale reported fully available; using engine default.")
    }

    private fun configureAudioAttributes(engine: TextToSpeech) {
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .build()
            focusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    fun speakMedicine(
        medicine: MedicineEntity,
        scheduledTime: ScheduleTime?,
        settings: ReminderSettings,
        activeKey: String,
        isScheduledReminder: Boolean = false,
        dedupeKey: String? = null,
        onDone: (() -> Unit)? = null,
    ) {
        val now = System.currentTimeMillis()
        if (dedupeKey != null &&
            dedupeKey == lastDedupeKey &&
            (now - lastDedupeAtMillis) < 6_000L &&
            (_state.value.isSpeaking || pendingUtterance != null)
        ) {
            onDone?.invoke()
            return
        }
        if (dedupeKey != null) {
            lastDedupeKey = dedupeKey
            lastDedupeAtMillis = now
        }

        val text = if (scheduledTime != null) {
            MedicineTtsFormatter.formatMedicineAnnouncement(
                medicine = medicine,
                scheduledTime = scheduledTime,
                includeInstructions = settings.includeInstructionsInSpeech,
                isScheduledReminder = isScheduledReminder,
            )
        } else {
            MedicineTtsFormatter.formatCabinetOverview(medicine, settings)
        }
        speakText(
            text = text,
            activeKey = activeKey,
            rate = settings.ttsSpeechRate,
            pitch = settings.ttsPitch,
            onDone = onDone,
        )
    }

    fun speakTodaySummary(
        pendingItems: List<Pair<MedicineEntity, ScheduleTime>>,
        settings: ReminderSettings,
    ) {
        val text = MedicineTtsFormatter.formatTodaySummary(pendingItems, settings)
        speakText(
            text = text,
            activeKey = SUMMARY_KEY,
            rate = settings.ttsSpeechRate,
            pitch = settings.ttsPitch,
        )
    }

    fun speakText(
        text: String,
        activeKey: String = UUID.randomUUID().toString(),
        rate: Float = 0.95f,
        pitch: Float = 1.0f,
        onDone: (() -> Unit)? = null,
    ) {
        val speech = PendingSpeech(
            text = text,
            key = activeKey,
            utteranceId = UUID.randomUUID().toString(),
            rate = rate.coerceIn(0.6f, 1.5f),
            pitch = pitch.coerceIn(0.75f, 1.3f),
            onDone = onDone,
        )
        mainHandler.post {
            if (!_state.value.isReady || tts == null) {
                pendingUtterance = speech
                initEngine()
                return@post
            }
            speakInternal(speech)
        }
    }

    private fun speakInternal(speech: PendingSpeech) {
        val engine = tts ?: run {
            speech.onDone?.invoke()
            return
        }
        pendingUtterance = speech
        activeUtteranceId = speech.utteranceId
        configureAudioAttributes(engine)
        requestAudioFocus()
        engine.setSpeechRate(speech.rate)
        engine.setPitch(speech.pitch)
        _state.value = _state.value.copy(
            isSpeaking = true,
            activeKey = speech.key,
            errorMessage = null,
        )
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val result = engine.speak(speech.text, TextToSpeech.QUEUE_FLUSH, params, speech.utteranceId)
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "TextToSpeech.speak returned ERROR; reinitializing engine")
            abandonAudioFocus()
            _state.value = _state.value.copy(
                isReady = false,
                isSpeaking = false,
                activeKey = null,
                errorMessage = "Could not speak medicine reminder.",
            )
            tts = null
            speech.onDone?.invoke()
            initEngine()
        }
    }

    fun stop() {
        mainHandler.post {
            pendingUtterance = null
            activeUtteranceId = null
            tts?.stop()
            abandonAudioFocus()
            _state.value = _state.value.copy(isSpeaking = false, activeKey = null)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun shutdown() {
        pendingUtterance = null
        activeUtteranceId = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        abandonAudioFocus()
        _state.value = TtsPlaybackState()
    }

    companion object {
        private const val TAG = "MedicineTtsManager"
        const val SUMMARY_KEY = "today_summary"
    }
}
