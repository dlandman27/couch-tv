package dev.couchtv.ontv.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Voice search through Android's own speech recognizer, exposed as Compose state so the search
 * screen can show a quiet listening indicator and the words as they're heard. It produces a plain
 * query string; the result goes through exactly the same search as typing. Main thread only.
 */
class VoiceInput(private val ctx: Context) {
    enum class Phase { Idle, Listening, Unavailable }

    var phase by mutableStateOf(Phase.Idle); private set
    var heard by mutableStateOf(""); private set      // partial words while listening
    var level by mutableStateOf(0f); private set      // 0..1 mic level, for the indicator
    var message by mutableStateOf(""); private set

    private var rec: SpeechRecognizer? = null

    val available get() = SpeechRecognizer.isRecognitionAvailable(ctx)

    fun start(onResult: (String) -> Unit) {
        if (!available) { phase = Phase.Unavailable; message = "Voice search isn't available on this TV."; return }
        cancel()
        heard = ""; level = 0f; message = ""
        val r = SpeechRecognizer.createSpeechRecognizer(ctx)
        rec = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { phase = Phase.Listening }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { level = 0f }
            override fun onError(error: Int) {
                phase = Phase.Idle
                message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Try again."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Orbit needs microphone access for voice search."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice search needs the internet."
                    else -> "Voice search stopped."
                }
                release()
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                phase = Phase.Idle; level = 0f
                release()
                if (text.isNotBlank()) { heard = text; onResult(text) } else message = "Didn't catch that. Try again."
            }
            override fun onPartialResults(partial: Bundle?) {
                partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { heard = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        phase = Phase.Listening
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1))
    }

    fun cancel() {
        rec?.cancel(); release()
        if (phase == Phase.Listening) phase = Phase.Idle
        level = 0f
    }

    fun dismissMessage() { message = ""; if (phase == Phase.Unavailable) phase = Phase.Idle }

    private fun release() { runCatching { rec?.destroy() }; rec = null }
}
