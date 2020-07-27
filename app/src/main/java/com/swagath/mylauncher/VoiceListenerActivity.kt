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
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import kotlinx.android.synthetic.main.activity_voice_listener.*
import org.json.JSONException
import org.json.JSONObject
import org.kaldi.*
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.util.*
import kotlin.concurrent.schedule


class VoiceListenerActivity : AppCompatActivity(), TextToSpeech.OnInitListener,
    RecognitionListener {

    companion object {
        private const val STATE_START = 0
        private const val STATE_READY = 1
        private const val STATE_DONE = 2
        private const val STATE_FILE = 3
        private const val STATE_MIC = 4

        private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1

        init {
            System.loadLibrary("kaldi_jni")
        }
    }

    private var mTTS: TextToSpeech? = null
    private var model: Model? = null
    private var recognizer: SpeechRecognizer? = null
    var resultView: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_listener)
        mTTS = TextToSpeech(this, this, "edu.cmu.cs.speech.tts.flite")
        resultView = findViewById(R.id.resultVoiceView)
        setUiState(STATE_START)

        // Check permission
        val permissionCheck =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
            return
        }

        SetupTask(this).execute() // Async Recognizer initialization
    }

    private class SetupTask internal constructor(activity: VoiceListenerActivity) :
        AsyncTask<Void?, Void?, Exception?>() {
        var activityReference: WeakReference<VoiceListenerActivity> = WeakReference(activity)
        override fun doInBackground(vararg params: Void?): Exception? {
            try {
                val assets = Assets(activityReference.get())
                val assetDir = assets.syncAssets()
                Log.d("Kaldi", "Sync files in the folder $assetDir")
                Vosk.SetLogLevel(0)
                activityReference.get()!!.model = Model("$assetDir/model-android")
            } catch (e: IOException) {
                return e
            }
            return null
        }

        override fun onPostExecute(result: Exception?) {
            if (result != null) {
                activityReference.get()!!.setErrorState(
                    String.format(
                        activityReference.get()!!.getString(R.string.failed), result
                    )
                )
            } else {
                activityReference.get()!!.setUiState(STATE_READY)
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SetupTask(this).execute()
            } else {
                finish()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (recognizer != null) {
            recognizer!!.cancel()
            recognizer!!.shutdown()
        }
        if (mTTS != null) {
            mTTS!!.shutdown()
        }
    }

    override fun onResult(hypothesis: String) {
        try {
            val reader = JSONObject(hypothesis)
            val valTxt = reader.getString("text")
            resultView!!.text = valTxt
            myLauncherActions(valTxt)
        } catch (ignored: JSONException) {
        }
    }

    override fun onPartialResult(hypothesis: String) {
    }

    override fun onError(e: Exception) {
        setErrorState(e.message)
    }

    override fun onTimeout() {
        recognizer!!.cancel()
        recognizer = null
        setUiState(STATE_READY)
    }

    private fun setUiState(state: Int) {
        when (state) {
            STATE_START -> resultView!!.setText(R.string.preparing)
            STATE_READY -> {
                resultView!!.setText(R.string.ready)
                recognizeMicrophone()
            }
            STATE_DONE -> {
            }
            STATE_FILE -> resultView!!.text = getString(R.string.starting)
            STATE_MIC -> {
                resultView!!.text = getString(R.string.say_something)
            }
        }
    }

    private fun setErrorState(message: String?) {
        resultView!!.text = message
    }

    fun recognizeMicrophone() {
        if (recognizer != null) {
            setUiState(STATE_DONE)
            recognizer!!.cancel()
            recognizer = null
        } else {
            setUiState(STATE_MIC)
            try {
                recognizer = SpeechRecognizer(model)
                recognizer!!.addListener(this)
                recognizer!!.startListening()
            } catch (e: IOException) {
                setErrorState(e.message)
            }
        }
    }

    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS) {
            val result = mTTS!!.setLanguage(Locale.UK)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d("TTS", "Language not supported")
            } else {
                Log.d("TTS", "Language supported")
            }
        } else {
            Log.d("TTS", "TextToSpeech Init failed")
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

            } else if (command == "turn off wifi") {
                if (Build.VERSION.SDK_INT < 29) {
                    mTTS!!.speak("Turning off Wi-fi", TextToSpeech.QUEUE_FLUSH, null, null)
                    val wifi =
                        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wifi.isWifiEnabled = false
                } else {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    Timer().schedule(3000) {
                        startActivity(intent)
                    }

                }
            } else if (command == "turn on wifi") {
                if (Build.VERSION.SDK_INT < 29) {
                    mTTS!!.speak("Turning on Wi-fi", TextToSpeech.QUEUE_FLUSH, null, null)
                    val wifi =
                        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wifi.isWifiEnabled = true
                } else {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    Timer().schedule(3000) {
                        startActivity(intent)
                    }
                }
            } else if (stRegex2.containsMatchIn(command)) {
                resultVoiceView.text =
                    fullCommand.replace("Lord".toRegex(RegexOption.IGNORE_CASE), "load")
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
            } else if (command == "increase the volume") {

                val audioManager =
                    applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)


            } else if (command == "decrease the volume") {

                val audioManager =
                    applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
            } else if (command == "turn on bluetooth") {
                val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                mTTS!!.speak("Blutooth On", TextToSpeech.QUEUE_FLUSH, null, null)
                adapter.enable()
            } else if (command == "turn off bluetooth") {
                val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                mTTS!!.speak("Blutooth Off", TextToSpeech.QUEUE_FLUSH, null, null)
                adapter.disable()
            } else Log.d("command", "no match")
        } else
            Log.d("command", "Voice Prefix match failure")


    }

}