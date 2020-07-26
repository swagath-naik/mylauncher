package com.swagath.mylauncher

import android.Manifest
import android.app.SearchManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import kotlinx.android.synthetic.main.activity_voice_listener.*
import java.net.URLEncoder
import java.util.*
import kotlin.concurrent.schedule

class VoiceListenerActivity : AppCompatActivity(), TextToSpeech.OnInitListener,
    RecognitionListener {
    private val maxLinesInput = 10

    private var speech: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private val LOG_TAG = "VoiceRecognition"
    private var mTTS: TextToSpeech? = null
    var isChecked = true
    var listening = false
    private lateinit var mHandler: Handler
    private lateinit var mRunnable: Runnable
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_listener)

        mTTS = TextToSpeech(this, this)

        tgglLst()
    }

    private fun tgglLst() {

        if (isChecked) {
            listening = true
            start()
            ActivityCompat.requestPermissions(
                this@VoiceListenerActivity, arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_PERMISSION
            )
        } else {
            listening = false
            turnOf()
        }
    }

    fun start() {
        speech = SpeechRecognizer.createSpeechRecognizer(this)
        Log.i(LOG_TAG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this))
        speech!!.setRecognitionListener(this)
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
            "en"
        )
        recognizerIntent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent!!.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxLinesInput)
    }

    fun turnOf() {
        speech!!.stopListening()
        speech!!.destroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_RECORD_PERMISSION -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@VoiceListenerActivity, "Start speaking...", Toast.LENGTH_SHORT)
                    .show()
                speech!!.startListening(recognizerIntent)
            } else {
                Toast.makeText(this@VoiceListenerActivity, "Permission Denied!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    public override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()

    }

    override fun onStop() {
        Log.d("pkg", "OnStop method")
        finish()
        super.onStop()

    }

    override fun onReadyForSpeech(bundle: Bundle) {
        Log.i(LOG_TAG, "onReadyForSpeech")
    }

    override fun onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        Log.i(LOG_TAG, "onRmsChanged: $rmsdB")
        if (!listening) {
            turnOf()
        }
    }

    override fun onBufferReceived(bytes: ByteArray) {
        Log.i(LOG_TAG, "onBufferReceived: $bytes")
    }

    override fun onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech")
    }

    override fun onError(errorCode: Int) {
        val errorMessage = getErrorText(errorCode)
        Log.d(LOG_TAG, "FAILED $errorMessage")

        speech!!.startListening(recognizerIntent)
        if (errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            isChecked = false
            tgglLst()
            isChecked = true
            tgglLst()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onResults(results: Bundle) {
        Log.i(LOG_TAG, "onResults")
        val matches = results
            .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        var text = ""
        if (matches != null) {
            for (result in matches) text += """
         $result
    
         """.trimIndent()
        }
        Log.i(LOG_TAG, "onResults=$text")
        mHandler = Handler(Looper.getMainLooper())
        mRunnable = Runnable {
            Log.i(LOG_TAG, "Runnbale Run")
            speech!!.startListening(recognizerIntent)
        }

        // Schedule the task to repeat after 1 second
        mHandler.postDelayed(
            mRunnable, // Runnable
            7000 // Delay in milliseconds
        )

        myLauncherActions(matches!![0].toString())
    }

    override fun onPartialResults(results: Bundle) {
        Log.i(LOG_TAG, "onPartialResults")
        val matches = results
            .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        var text = ""
        if (matches != null) {
            for (result in matches) text += """
         $result
    
         """.trimIndent()
        }
        Log.i(LOG_TAG, "onPartialResults=$text")
    }

    override fun onEvent(i: Int, bundle: Bundle) {
        Log.i(LOG_TAG, "onEvent")
    }

    private fun getErrorText(errorCode: Int): String {
        val message: String
        when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> message = "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> message = "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> message = "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> message = "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> message = "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> message = "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                message = "RecognitionService busy"
                turnOf()
            }
            SpeechRecognizer.ERROR_SERVER -> message = "error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> message = "No speech input"
            else -> message = "Didn't understand, please try again."
        }
        return message
    }

    companion object {
        private const val REQUEST_RECORD_PERMISSION = 100
    }

    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS) {
            val result = mTTS!!.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d("TTS", "Language not supported")
            } else {
                Log.d("TTS", "Language supported")
            }
        } else {
            Log.d("TTS", "TextToSpeech Init failed")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (speech != null) {
            speech!!.stopListening()
            speech!!.cancel()
            speech!!.destroy()
        }
        if (mTTS != null) {
            mTTS!!.shutdown()
        }


    }

    fun myLauncherActions(fullCommand: String) {
        val voiceCmdPrefix = "^my launcher".toRegex(RegexOption.IGNORE_CASE)
        if (voiceCmdPrefix.containsMatchIn(fullCommand)) {

            resultVoiceView.text = fullCommand
            val command = fullCommand.replace("my launcher ", "")
            Log.d("command", command)

            val stRegex = "^start".toRegex(RegexOption.IGNORE_CASE)
            val stRegex1 = "^search".toRegex(RegexOption.IGNORE_CASE)
            val stRegex2 = "^lo[ar]d playlist".toRegex(RegexOption.IGNORE_CASE)
            if (command == "start media player") {
                mTTS!!.speak("starting media player", TextToSpeech.QUEUE_FLUSH, null, null)
                Log.d("command", "starting media player")
                val intent =
                    Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
                startActivity(intent)


                val CMDNAME = "command"
                val SERVICECMD = "com.android.music.musicservicecommand"
                val i = Intent(SERVICECMD)
                i.putExtra(CMDNAME, "play")
                this.sendBroadcast(i)
            } else if (stRegex.containsMatchIn(command)) {
                Log.d("command", "start app")

                val appNm = command.toLowerCase(Locale.ROOT).replace("start ", "")


                val pm: PackageManager = packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null)
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                val pkgAppsList: List<ResolveInfo> =
                    pm.queryIntentActivities(mainIntent, 0)

                for (p in pkgAppsList) {
                    if (appNm.length >= 3 &&
                        FuzzySearch.ratio(
                            appNm,
                            p.loadLabel(pm).toString().toLowerCase(Locale.ROOT)
                        ) >= 75
                    ) {
                        Log.d("pkg", "->>${p.activityInfo.packageName}")
                        val launchIntent =
                            packageManager.getLaunchIntentForPackage(p.activityInfo.packageName.toString())
                        if (launchIntent != null) {
                            val appLbl = p.loadLabel(pm).toString()
                            mTTS!!.speak("Opening $appLbl", TextToSpeech.QUEUE_FLUSH, null, null)
                            Timer().schedule(4000) {
                                startActivity(launchIntent)
                            }

                        }
                        break
                    }

                }

            } else if (stRegex1.containsMatchIn(command)) {

                val command1 = command.replace("search ", "")
                val escapedQuery: String = URLEncoder.encode(command1, "UTF-8")
                val uri: Uri = Uri.parse("https://www.google.com/search?q=$escapedQuery")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                mTTS!!.speak("Searching your query", TextToSpeech.QUEUE_FLUSH, null, null)
                Timer().schedule(3000) {
                    startActivity(intent)
                }

            } else if (command == "turn off Wi-Fi") {
                if (Build.VERSION.SDK_INT < 29) {
                    mTTS!!.speak("Turning off Wi-fi", TextToSpeech.QUEUE_FLUSH, null, null)
                    val wifi =
                        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wifi.isWifiEnabled = false
                } else {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    startActivity(intent)
                }
            } else if (command == "turn on Wi-fi") {
                if (Build.VERSION.SDK_INT < 29) {
                    mTTS!!.speak("Turning on Wi-fi", TextToSpeech.QUEUE_FLUSH, null, null)
                    val wifi =
                        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wifi.isWifiEnabled = true
                } else {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    startActivity(intent)
                }
            } else if (stRegex2.containsMatchIn(command)) {
                resultVoiceView.text = fullCommand.replace("Lord", "load")
                val command1 =
                    command.replace("lo[ar]d playlist ".toRegex(RegexOption.IGNORE_CASE), "")

                val plQuery = command1
                val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                intent.putExtra(
                    MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
                    "android.intent.extra.playlist"
                )
                intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/playlist")
                intent.putExtra(SearchManager.QUERY, plQuery)


                if (intent.resolveActivity(packageManager) != null) {
                    mTTS!!.speak("Loading your playlist", TextToSpeech.QUEUE_FLUSH, null, null)
                    Timer().schedule(3000) {
                        startActivity(intent)
                    }

                } else {
                    Log.d("music", "search request failed")
                }

            } else if (command == "turn off volume") {
                val audioManager =
                    applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_NOTIFICATION,
                        AudioManager.ADJUST_MUTE,
                        0
                    )
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_ALARM,
                        AudioManager.ADJUST_MUTE,
                        0
                    )
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        0
                    )
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_RING,
                        AudioManager.ADJUST_MUTE,
                        0
                    )
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_SYSTEM,
                        AudioManager.ADJUST_MUTE,
                        0
                    )
                } else {
                    audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
                    audioManager.setStreamMute(AudioManager.STREAM_ALARM, true)
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true)
                    audioManager.setStreamMute(AudioManager.STREAM_RING, true)
                    audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true)
                }


            } else if (command == "turn on volume") {
                val audioManager =
                    applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_NOTIFICATION,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_ALARM,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_RING,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_SYSTEM,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                } else {
                    audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
                    audioManager.setStreamMute(AudioManager.STREAM_ALARM, false)
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false)
                    audioManager.setStreamMute(AudioManager.STREAM_RING, false)
                    audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false)
                }
                mTTS!!.speak("Turned on Volume", TextToSpeech.QUEUE_FLUSH, null, null)
            } else if (command == "increase volume") {

                val audioManager =
                    applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)


            } else if (command == "decrease volume") {

                val audioManager =
                    applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
            } else if (command == "turn on Bluetooth") {
                val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                mTTS!!.speak("Blutooth On", TextToSpeech.QUEUE_FLUSH, null, null)
                adapter.enable()
            } else if (command == "turn off Bluetooth") {
                val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                mTTS!!.speak("Blutooth Off", TextToSpeech.QUEUE_FLUSH, null, null)
                adapter.disable()
            } else Log.d("command", "no match")
        } else
            Log.d("command", "Voice Prefix match failure")


    }

}