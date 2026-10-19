package com.example.mobleq

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContent {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        background = Color.Black,
                        surface = Color(0xFF242424),
                        onBackground = Color.White,
                        onSurface = Color.White
                    )
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black
                    ) {
                        MobilEqApp()
                    }
                }
            }
        } catch (t: Throwable) {
            setContent {
                Surface(
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = "Startup Error / Başlatma Hatası:",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${t.javaClass.simpleName}: ${t.localizedMessage ?: ""}\n\n${t.stackTraceToString()}",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// --- BİLDİRİM VE WIDGET AKSİYON SABİTLERİ ---
private const val ACTION_TOGGLE_EQ = "com.example.mobleq.ACTION_TOGGLE_EQ"
private const val ACTION_TOGGLE_LIMITER = "com.example.mobleq.ACTION_TOGGLE_LIMITER"
private const val ACTION_TOGGLE_MODE = "com.example.mobleq.ACTION_TOGGLE_MODE"
private const val NOTIFICATION_CHANNEL_ID = "mobileq_wavelet_channel"
private const val NOTIFICATION_ID = 1001

// --- RENK PALETİ ---
private val BgBlack = Color(0xFF000000)
private val PillDarkGray = Color(0xFF242424)
private val ContainerGray = Color(0xFF272727)
private val PopupBgGray = Color(0xFF2B2B2B)
private val PopupHeaderGray = Color(0xFF3C3C3C)
private val ActionBottomBarGray = Color(0xFF676767)
private val AccentGreen = Color(0xFF3DAC4A)
private val AccentRed = Color(0xFFD92B2B)
private val SelectionTeal = Color(0xFF0095B6)
private val DividerLightGray = Color(0xFF8A8A8A)
private val ThinFrameGray = Color(0xFF424242)

// --- SES GÖRSELLEŞTİRİCİ 24 EŞİT ÇUBUK FREKANS HARİTASI ---
private val VisualizerBarFrequenciesHz = floatArrayOf(
    20f, 31f, 45f, 62f, 125f, 250f, 500f, 1000f,
    2000f, 4000f, 6300f, 10000f, 10800f, 11600f, 12400f, 13200f,
    14000f, 14800f, 15600f, 16400f, 17200f, 18000f, 19000f, 20000f
)

enum class AudioOutputType {
    BLUETOOTH,
    WIRED_HEADPHONES,
    USB_DAC,
    SPEAKER
}

data class AudioOutputInfo(
    val type: AudioOutputType,
    val customDeviceName: String? = null
)

data class ConnectedAudioDevicesTree(
    val bluetoothDevices: List<String>,
    val wiredDevices: List<String>,
    val usbDacDevices: List<String>,
    val hasSpeaker: Boolean = true
)

data class DetectedPlayerApp(
    val packageName: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val lastSessionId: Int = 0
)

private val PresetColorProfiles = listOf(
    Color.Transparent,
    Color(0xFF0095B6), // Turkuaz (1731.png)
    Color(0xFF7BD33C), // Açık Yeşil (1733.png)
    Color(0xFF3DAC4A), // Zümrüt Yeşili
    Color(0xFF7B1FA2), // Mor
    Color(0xFFC62828), // Koyu Kırmızı
    Color(0xFFEF6C00), // Turuncu
    Color(0xFF1565C0), // Safir Mavi
    Color(0xFFAD1457), // Pembe
    Color(0xFF00897B)  // Çam Yeşili
)

// --- ÇOKLU DİL DESTEĞİ ---
data class AppStrings(
    val isEnglish: Boolean,
    val defaultProfileName: String,
    val graphic: String,
    val parametric: String,
    val graphicProfilesSection: String,
    val parametricProfilesSection: String,
    val addNewColumn: String,
    val delete: String,
    val color: String,
    val all: String,
    val outputBluetooth: String,
    val outputWired: String,
    val outputUsbDac: String,
    val outputSpeaker: String,
    val outputPrefix: String,
    val warnSessionError: String,
    val rename: String,
    val saveAndAssign: String,
    val importProfile: String,
    val exportProfile: String,
    val loadDefault: String,
    val autoSave: String,
    val saveAssignTitle: String,
    val profileTypeLabel: String,
    val overwriteCurrentProfile: String,
    val profileNameLabel: String,
    val profileNamePlaceholder: String,
    val assignDeviceTreeLabel: String,
    val noDeviceConnectedInBranch: String,
    val assignNone: String,
    val colorPickerTitle: String,
    val presetColorsLabel: String,
    val customColorBaretLabel: String,
    val brightnessBaretLabel: String,
    val clearColor: String,
    val searchAutoEq: String,
    val clearAutoEq: String,
    val profilesTitle: String,
    val addNewProfileBtn: String,
    val renameProfileTitle: String,
    val save: String,
    val apply: String,
    val cancel: String,
    val editFreqTitle: String,
    val settingsTitle: String,
    val myAccount: String,
    val language: String,
    val equalizer: String,
    val report: String,
    val turkish: String,
    val english: String,
    val loginOrRegisterHeader: String,
    val usernamePlaceholder: String,
    val emailPlaceholder: String,
    val verificationCode: String,
    val requestCode: String,
    val submit: String,
    val passwordSection: String,
    val passwordRepeatPlaceholder: String,
    val createAccountBtn: String,
    val logOutBtn: String,
    val sync: String,
    val changePassword: String,
    val changeEmail: String,
    val changePasswordEmailTitle: String,
    val currentPassword: String,
    val newPassword: String,
    val repeatNewPassword: String,
    val savePasswordBtn: String,
    val emailSection: String,
    val newEmail: String,
    val enableAudioProcessing: String,
    val autoPreAmp: String,
    val detectedPlayers: String,
    val noDetectedPlayersYet: String,
    val graphicEqMenu: String,
    val graphicEqTitle: String,
    val graphicEqBandsHeader: String,
    val bandSuffix: String,
    val sendAutoReport: String,
    val toastOutputStatus: (String) -> String,
    val toastSavedAssigned: (String, String, String) -> String,
    val toastAutoSwitchedProfile: (String, String, String) -> String,
    val toastProfileDeleted: (String) -> String,
    val toastCannotDeleteDefault: String,
    val toastImported: (String) -> String,
    val toastImportError: String,
    val toastExported: (String) -> String,
    val toastDefaultLoaded: String,
    val toastAutoEqApplied: (String) -> String,
    val toastAutoEqCleared: String,
    val toastBandsActivated: (Int) -> String,
    val toastPasswordSaved: String,
    val toastCodeRequested: String,
    val toastCodeVerified: String,
    val toastAccountCreated: (String) -> String,
    val toastAccountUpdated: String,
    val toastEnterProfileName: String
)

private fun getAppStrings(language: String): AppStrings {
    val en = language == "İngilizce" || language == "English"
    return if (en) {
        AppStrings(
            isEnglish = true,
            defaultProfileName = "Default",
            graphic = "Graphic",
            parametric = "Parametric",
            graphicProfilesSection = "Graphic Profiles",
            parametricProfilesSection = "Parametric Profiles",
            addNewColumn = "Add\nNew\nColumn",
            delete = "Delete",
            color = "Color",
            all = "All",
            outputBluetooth = "Bluetooth",
            outputWired = "Wired Headset",
            outputUsbDac = "USB DAC",
            outputSpeaker = "Speaker",
            outputPrefix = "Output",
            warnSessionError = "Audio is playing, but Android audio session could not be detected!",
            rename = "Rename",
            saveAndAssign = "Save & Assign",
            importProfile = "Import",
            exportProfile = "Export",
            loadDefault = "Load Default",
            autoSave = "Auto Save",
            saveAssignTitle = "Save & Assign Device",
            profileTypeLabel = "Equalizer Mode",
            overwriteCurrentProfile = "Update Current",
            profileNameLabel = "Profile Name",
            profileNamePlaceholder = "Enter new profile name...",
            assignDeviceTreeLabel = "Connected Devices & Outputs (Tree)",
            noDeviceConnectedInBranch = "No connected device",
            assignNone = "Do Not Assign (General)",
            colorPickerTitle = "Column Color Palette",
            presetColorsLabel = "Preset Color Profiles",
            customColorBaretLabel = "Custom Color Hue Bar",
            brightnessBaretLabel = "Tone / Brightness Bar",
            clearColor = "Clear Color",
            searchAutoEq = "Search AutoEq",
            clearAutoEq = "Disable AutoEq Selection",
            profilesTitle = "Profiles",
            addNewProfileBtn = "+ Save Current as New Profile",
            renameProfileTitle = "Rename Profile",
            save = "Save",
            apply = "Apply",
            cancel = "Cancel",
            editFreqTitle = "Set Frequency (Hz)",
            settingsTitle = "Settings",
            myAccount = "My Account",
            language = "Language",
            equalizer = "Equalizer",
            report = "Report",
            turkish = "Turkish",
            english = "English",
            loginOrRegisterHeader = "Sign In/Create Account",
            usernamePlaceholder = "Username",
            emailPlaceholder = "E-mail",
            verificationCode = "Verification Code",
            requestCode = "Request Code",
            submit = "Submit",
            passwordSection = "Password",
            passwordRepeatPlaceholder = "Repeat Password",
            createAccountBtn = "Create",
            logOutBtn = "Sign Out",
            sync = "Synchronization",
            changePassword = "Change Password",
            changeEmail = "Change E-mail",
            changePasswordEmailTitle = "Change Password/E-mail",
            currentPassword = "Current Password",
            newPassword = "New Password",
            repeatNewPassword = "Repeat New Password",
            savePasswordBtn = "Save Password",
            emailSection = "E-mail",
            newEmail = "New E-mail",
            enableAudioProcessing = "Enable Audio Processing",
            autoPreAmp = "Auto PreAmp",
            detectedPlayers = "Detected Players",
            noDetectedPlayersYet = "No media player sessions detected yet.\nPlay music in Spotify, YouTube Music, Deezer, etc.",
            graphicEqMenu = "Graphic Equalizer",
            graphicEqTitle = "Graphic Equalizer",
            graphicEqBandsHeader = "Graphic Equalizer Bands",
            bandSuffix = "Bands",
            sendAutoReport = "Send Automatic Reports",
            toastOutputStatus = { out -> "Active Audio Output: $out" },
            toastSavedAssigned = { name, mode, out -> "\"$name\" ($mode) saved and assigned to $out" },
            toastAutoSwitchedProfile = { dev, name, mode -> "$dev connected -> \"$name\" ($mode) applied" },
            toastProfileDeleted = { name -> "Profile \"$name\" deleted" },
            toastCannotDeleteDefault = "Default profile reset to flat (0 dB)",
            toastImported = { name -> "Profile \"$name\" imported successfully" },
            toastImportError = "Invalid profile file format",
            toastExported = { name -> "\"$name\" exported as JSON file" },
            toastDefaultLoaded = "Default flat (0 dB) settings loaded",
            toastAutoEqApplied = { name -> "$name AutoEQ preset activated" },
            toastAutoEqCleared = "AutoEQ selection cleared",
            toastBandsActivated = { count -> "$count Bands Graphic EQ activated" },
            toastPasswordSaved = "Password saved successfully",
            toastCodeRequested = "Verification code sent to your e-mail",
            toastCodeVerified = "Verification code confirmed",
            toastAccountCreated = { name -> "Welcome, $name! Account active." },
            toastAccountUpdated = "E-mail updated successfully",
            toastEnterProfileName = "Please enter a profile name"
        )
    } else {
        AppStrings(
            isEnglish = false,
            defaultProfileName = "Varsayılan",
            graphic = "Grafik",
            parametric = "Parametric",
            graphicProfilesSection = "Grafik Profiller",
            parametricProfilesSection = "Parametric Profiller",
            addNewColumn = "Yeni\nSütun\nEkle",
            delete = "sil",
            color = "Renk",
            all = "Hepsi",
            outputBluetooth = "Bluetooth",
            outputWired = "Kablolu Kulaklık",
            outputUsbDac = "USB DAC",
            outputSpeaker = "Hoparlör",
            outputPrefix = "Çıkış",
            warnSessionError = "Müzik çalıyor ancak Android ses oturumu algılanamadı!",
            rename = "Yeniden Adlandır",
            saveAndAssign = "Kaydet&Ata",
            importProfile = "İçe Aktar",
            exportProfile = "Dışa aktar",
            loadDefault = "Var Sayılanı yükle",
            autoSave = "Otomatik Kayıt et",
            saveAssignTitle = "Kaydet & Cihaz Ata",
            profileTypeLabel = "Ekolayzer Türü",
            overwriteCurrentProfile = "Mevcut Profili Güncelle",
            profileNameLabel = "Profil İsmi",
            profileNamePlaceholder = "Yeni profil ismi girin...",
            assignDeviceTreeLabel = "Bağlı Cihazlar ve Ses Çıkışları",
            noDeviceConnectedInBranch = "Bağlı cihaz yok",
            assignNone = "Cihaz Atama (Genel)",
            colorPickerTitle = "Sütun Renk Paleti",
            presetColorsLabel = "Sabit Renk Profilleri",
            customColorBaretLabel = "Özel Renk Bareti (Ton)",
            brightnessBaretLabel = "Parlaklık / Koyu Ton Bareti",
            clearColor = "Rengi Kaldır",
            searchAutoEq = "AutoEq Ara",
            clearAutoEq = "AutoEq Seçimini Kaldır",
            profilesTitle = "Profiles",
            addNewProfileBtn = "+ Yeni Profil Kaydet",
            renameProfileTitle = "Profili Yeniden Adlandır",
            save = "Kaydet",
            apply = "Uygula",
            cancel = "İptal",
            editFreqTitle = "Frekans (Hz) Ayarla",
            settingsTitle = "Ayarlar",
            myAccount = "Hesabım",
            language = "Dil",
            equalizer = "Equalizer",
            report = "Bildir",
            turkish = "Türkçe",
            english = "İngilizce",
            loginOrRegisterHeader = "Giriş Yap/Hesap Oluştur",
            usernamePlaceholder = "Kullanıcı Adı",
            emailPlaceholder = "E-mail",
            verificationCode = "Doğrulama Kodu",
            requestCode = "Kod İste",
            submit = "Gönder",
            passwordSection = "Şifre",
            passwordRepeatPlaceholder = "Şifre Tekrar",
            createAccountBtn = "Oluştur",
            logOutBtn = "Çıkış Yap",
            sync = "Senkronizasyon",
            changePassword = "Şifre Değiştir",
            changeEmail = "E-mail Değiştir",
            changePasswordEmailTitle = "Şifre/E-mail Değiştir",
            currentPassword = "Mevcut Şifre",
            newPassword = "Yeni Şifre",
            repeatNewPassword = "Yeni Şifre Tekrar",
            savePasswordBtn = "Şifreyi kaydet",
            emailSection = "E-mail",
            newEmail = "Yeni E-mail",
            enableAudioProcessing = "Ses İşlemeyi Etkinleştir",
            autoPreAmp = "Auto PreAmp",
            detectedPlayers = "Okunan Oynatıcılar",
            noDetectedPlayersYet = "Henüz okunan oynatıcı oturumu yok.\nSpotify, Deezer, YouTube Music vb. bir oynatıcıda şarkı başlatın.",
            graphicEqMenu = "Grafik Equlaizer",
            graphicEqTitle = "Grafik Equalizer",
            graphicEqBandsHeader = "Grafik Equlaizer Bant",
            bandSuffix = "Bant",
            sendAutoReport = "Otomatik Bildirim Yolla",
            toastOutputStatus = { out -> "Aktif Ses Çıkışı: $out" },
            toastSavedAssigned = { name, mode, out -> "\"$name\" ($mode) kaydedildi ve $out çıkışına atandı" },
            toastAutoSwitchedProfile = { dev, name, mode -> "$dev algılandı -> \"$name\" ($mode) otomatik uygulandı" },
            toastProfileDeleted = { name -> "\"$name\" profili silindi" },
            toastCannotDeleteDefault = "Varsayılan profil sıfırlandı (0 dB)",
            toastImported = { name -> "\"$name\" profili başarıyla içe aktarıldı" },
            toastImportError = "Geçersiz profil dosyası formatı",
            toastExported = { name -> "\"$name\" JSON dosyası olarak dışa aktarıldı" },
            toastDefaultLoaded = "Varsayılan sıfır (Flat) ayarlar yüklendi",
            toastAutoEqApplied = { name -> "$name AutoEQ profili etkinleştirildi" },
            toastAutoEqCleared = "AutoEQ seçimi kaldırıldı",
            toastBandsActivated = { count -> "$count Bant Grafik EQ aktif edildi" },
            toastPasswordSaved = "Şifre başarıyla kaydedildi",
            toastCodeRequested = "Doğrulama kodu e-posta adresine gönderildi",
            toastCodeVerified = "Doğrulama kodu onaylandı",
            toastAccountCreated = { name -> "Hoş geldin $name! Hesabın oluşturuldu." },
            toastAccountUpdated = "E-mail adresi başarıyla güncellendi",
            toastEnterProfileName = "Lütfen bir profil ismi girin"
        )
    }
}

// --- VERİ MODELLERİ ---
data class GraphicBand(
    val id: Int,
    val label: String,
    val frequencyHz: Int,
    val db: Float
)

data class ParametricColumn(
    val id: Int,
    val db: Float = 0f,
    val q: Float = 1.40f,
    val hz: Int = 31,
    val isSelected: Boolean = false,
    val highlightColor: Color = SelectionTeal
)

enum class EqMode {
    GRAPHIC, PARAMETRIC
}

data class EqProfile(
    val name: String,
    val eqMode: EqMode = EqMode.GRAPHIC,
    val preAmpDb: Float,
    val graphicGains: List<Float>,
    val parametricColumns: List<ParametricColumn>,
    val autoEqModel: String? = null,
    val assignedDevice: String? = null,
    val isDefault: Boolean = false
)

data class AutoEqPreset(
    val modelName: String,
    val preAmpDb: Float,
    val gains10Band: List<Float>
)

enum class AppScreen {
    MAIN,
    SETTINGS_MAIN,
    SETTINGS_ACCOUNT,
    SETTINGS_PASSWORD_EMAIL,
    SETTINGS_EQUALIZER,
    SETTINGS_GRAPHIC_BANDS,
    SETTINGS_PLAYERS,
    SETTINGS_REPORT
}

// --- VARSAYILAN VERİLER (10 Bant) ---
private val Default10Bands = listOf(
    GraphicBand(0, "31", 31, 0f),
    GraphicBand(1, "62", 62, 0f),
    GraphicBand(2, "125", 125, 0f),
    GraphicBand(3, "250", 250, 0f),
    GraphicBand(4, "500", 500, 0f),
    GraphicBand(5, "1k", 1000, 0f),
    GraphicBand(6, "2k", 2000, 0f),
    GraphicBand(7, "4k", 4000, 0f),
    GraphicBand(8, "8k", 8000, 0f),
    GraphicBand(9, "16k", 16000, 0f)
)

private fun buildBandsForCount(count: Int, previousBands: List<GraphicBand>): List<GraphicBand> {
    val chosen = when (count) {
        8 -> listOf(31 to "31", 62 to "62", 125 to "125", 250 to "250", 500 to "500", 1000 to "1k", 4000 to "4k", 16000 to "16k")
        10 -> listOf(31 to "31", 62 to "62", 125 to "125", 250 to "250", 500 to "500", 1000 to "1k", 2000 to "2k", 4000 to "4k", 8000 to "8k", 16000 to "16k")
        12 -> listOf(31 to "31", 62 to "62", 125 to "125", 250 to "250", 500 to "500", 1000 to "1k", 2000 to "2k", 4000 to "4k", 6000 to "6k", 8000 to "8k", 12000 to "12k", 16000 to "16k")
        14 -> listOf(31 to "31", 62 to "62", 125 to "125", 250 to "250", 500 to "500", 1000 to "1k", 2000 to "2k", 4000 to "4k", 6000 to "6k", 8000 to "8k", 10000 to "10k", 12000 to "12k", 14000 to "14k", 16000 to "16k")
        else -> Default10Bands.map { it.frequencyHz to it.label }
    }
    return chosen.mapIndexed { idx, (hz, label) ->
        val existing = previousBands.find { it.frequencyHz == hz }?.db ?: 0f
        GraphicBand(id = idx, label = label, frequencyHz = hz, db = existing)
    }
}

private fun createBuiltInDefaultGraphicProfile(): EqProfile {
    return EqProfile(
        name = "Varsayılan",
        eqMode = EqMode.GRAPHIC,
        preAmpDb = 0f,
        graphicGains = Default10Bands.map { 0f },
        parametricColumns = listOf(ParametricColumn(1, 0f, 1.40f, 31, false, SelectionTeal)),
        autoEqModel = null,
        assignedDevice = null,
        isDefault = true
    )
}

private fun createBuiltInDefaultParametricProfile(): EqProfile {
    return EqProfile(
        name = "Varsayılan (Parametric)",
        eqMode = EqMode.PARAMETRIC,
        preAmpDb = 0f,
        graphicGains = Default10Bands.map { 0f },
        parametricColumns = listOf(ParametricColumn(1, 0f, 1.40f, 31, false, SelectionTeal)),
        autoEqModel = null,
        assignedDevice = null,
        isDefault = true
    )
}

private val SampleAutoEqPresets = listOf(
    AutoEqPreset("Samsung Galaxy Buds Fe", -1.5f, listOf(-1.5f, 0f, 1.5f, -5f, 0f, 5f, 2.5f, -1f, 3f, 0f)),
    AutoEqPreset("Samsung Galaxy Buds 2", -2.0f, listOf(1.0f, 2.0f, 0.5f, -2.5f, -1.0f, 3.5f, 4.0f, 1.5f, -2.0f, 1.0f)),
    AutoEqPreset("JBL Live 670 Nc", -2.5f, listOf(-3.0f, -1.5f, 0f, 1.5f, 2.0f, 1.0f, 3.0f, 2.5f, -1.0f, 2.0f)),
    AutoEqPreset("BeyerDynamic Dt990", -4.0f, listOf(3.5f, 2.0f, 0f, -1.5f, 0.5f, 1.0f, -0.5f, -4.5f, -6.0f, 0f)),
    AutoEqPreset("Sony WH-1000XM5", -2.0f, listOf(-2.5f, -2.0f, -1.0f, 0.5f, 1.5f, 2.5f, 3.0f, 1.0f, 0.5f, 2.0f)),
    AutoEqPreset("Apple AirPods Pro 2", -1.0f, listOf(0.5f, 0f, -0.5f, 0f, 1.0f, 1.5f, 0.5f, -1.0f, 1.0f, 0.5f)),
    AutoEqPreset("Sennheiser HD 600", -3.0f, listOf(4.0f, 2.5f, 0.5f, -0.5f, 0f, -1.0f, 0f, 1.5f, 1.0f, 1.0f)),
    AutoEqPreset("Anker Soundcore Space Q45", -2.5f, listOf(-3.5f, -2.5f, -1.0f, 1.0f, 2.0f, 1.5f, 3.0f, 1.0f, -1.5f, 1.5f))
)

// ============================================================================
// WAVELET TARZI BİLDİRİM SİSTEMİ YÖNETİCİSİ
// ============================================================================
object MobilEqNotificationHelper {
    fun showOrUpdateNotification(
        context: Context,
        enabled: Boolean,
        isEqEnabled: Boolean,
        isLimiterEnabled: Boolean,
        eqMode: EqMode,
        outputLabel: String,
        profileName: String,
        preAmpDb: Float,
        isEnglish: Boolean
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!enabled) {
            try { nm.cancel(NOTIFICATION_ID) } catch (_: Throwable) {}
            return
        }
        if (Build.VERSION.SDK_INT >= 33) {
            val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (perm != PackageManager.PERMISSION_GRANTED) return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "MobilEQ Hızlı Kontrol Bildirimi",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Wavelet tarzı hızlı ekolayzer durum ve kontrol bildirimi"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context, 10, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val toggleEqPending = PendingIntent.getBroadcast(
                context, 11, Intent(ACTION_TOGGLE_EQ).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val toggleLimiterPending = PendingIntent.getBroadcast(
                context, 12, Intent(ACTION_TOGGLE_LIMITER).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val toggleModePending = PendingIntent.getBroadcast(
                context, 13, Intent(ACTION_TOGGLE_MODE).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val modeLabel = if (eqMode == EqMode.GRAPHIC) {
                if (isEnglish) "Graphic" else "Grafik"
            } else "Parametric"

            val statusLabel = if (isEqEnabled) {
                if (isEnglish) "Active" else "Aktif"
            } else {
                if (isEnglish) "Bypassed (Off)" else "Devre Dışı (Kapalı)"
            }

            val title = "MobilEQ • $statusLabel ($modeLabel)"
            val subtitle = if (isEnglish) {
                "Output: $outputLabel • $profileName • PreAmp: ${formatDb(preAmpDb)} dB"
            } else {
                "Çıkış: $outputLabel • $profileName • PreAmp: ${formatDb(preAmpDb)} dB"
            }

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(openPending)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(0, if (isEqEnabled) "on/off: ON" else "on/off: OFF", toggleEqPending)
                .addAction(0, if (isLimiterEnabled) "limiter: ON" else "limiter: OFF", toggleLimiterPending)
                .addAction(0, modeLabel, toggleModePending)
                .build()

            nm.notify(NOTIFICATION_ID, notification)
        } catch (_: Throwable) {}
    }
}

// ============================================================================
// 4 FARKLI ANA EKRAN WIDGET SINIFI VE HIZLI ERİŞİM KARESİ (1663.png)
// ============================================================================
object MobilEqWidgetRenderer {
    fun renderSingleCircleBitmap(label: String, isActive: Boolean): Bitmap {
        val size = 240
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#242424")
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor(if (isActive) "#3DAC4A" else "#D92B2B")
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val radius = (size / 2f) - 8f
        c.drawCircle(size / 2f, size / 2f, radius, fillPaint)
        c.drawCircle(size / 2f, size / 2f, radius, strokePaint)
        val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        c.drawText(label, size / 2f, yPos, textPaint)
        return bmp
    }

    fun renderFullCardBitmap(
        outputLabel: String,
        profileName: String,
        limiterOn: Boolean,
        eqOn: Boolean,
        autoEqOn: Boolean,
        preAmpText: String
    ): Bitmap {
        val w = 840
        val h = 380
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#242424")
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 52f, 52f, bgPaint)
        c.drawRoundRect(RectF(28f, 26f, w - 28f, 142f), 58f, 58f, pillPaint)
        c.drawText("Çıkış: $outputLabel, $profileName", 56f, 98f, textPaint)

        val btnCentersX = floatArrayOf(125f, 322f, 518f, 715f)
        val btnCenterY = 264f
        val r = 82f

        val labels = arrayOf("limiter", "on/off", "AutoEq", preAmpText)
        val borders = intArrayOf(
            android.graphics.Color.parseColor(if (limiterOn) "#3DAC4A" else "#D92B2B"),
            android.graphics.Color.parseColor(if (eqOn) "#3DAC4A" else "#D92B2B"),
            if (autoEqOn) android.graphics.Color.parseColor("#3DAC4A") else android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT
        )

        val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        for (i in 0..3) {
            c.drawCircle(btnCentersX[i], btnCenterY, r, pillPaint)
            if (borders[i] != android.graphics.Color.TRANSPARENT) {
                val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = borders[i]
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                }
                c.drawCircle(btnCentersX[i], btnCenterY, r, sp)
            }
            if (i == 3) {
                c.drawText("preamp", btnCentersX[i], btnCenterY - 8f, btnTextPaint)
                c.drawText(labels[i], btnCentersX[i], btnCenterY + 30f, btnTextPaint)
            } else {
                val yPos = btnCenterY - ((btnTextPaint.descent() + btnTextPaint.ascent()) / 2f)
                c.drawText(labels[i], btnCentersX[i], yPos, btnTextPaint)
            }
        }
        return bmp
    }

    fun renderCompactCardBitmap(outputLabel: String, limiterOn: Boolean, eqOn: Boolean): Bitmap {
        val w = 440
        val h = 380
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#242424")
            style = Paint.Style.FILL
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 52f, 52f, bgPaint)
        c.drawRoundRect(RectF(24f, 26f, w - 24f, 142f), 58f, 58f, pillPaint)
        c.drawText(outputLabel, w / 2f, 98f, titlePaint)

        val centersX = floatArrayOf(120f, 320f)
        val centerY = 264f
        val r = 82f
        val labels = arrayOf("limiter", "on/off")
        val states = booleanArrayOf(limiterOn, eqOn)

        val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        for (i in 0..1) {
            c.drawCircle(centersX[i], centerY, r, pillPaint)
            val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor(if (states[i]) "#3DAC4A" else "#D92B2B")
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            c.drawCircle(centersX[i], centerY, r, sp)
            val yPos = centerY - ((btnTextPaint.descent() + btnTextPaint.ascent()) / 2f)
            c.drawText(labels[i], centersX[i], yPos, btnTextPaint)
        }
        return bmp
    }

    fun refreshAllWidgets(context: Context) {
        try {
            val mgr = AppWidgetManager.getInstance(context)
            val providers = arrayOf(
                LimiterWidgetProvider::class.java,
                OnOffWidgetProvider::class.java,
                FullControlWidgetProvider::class.java,
                CompactControlWidgetProvider::class.java
            )
            providers.forEach { cls ->
                val ids = mgr.getAppWidgetIds(ComponentName(context, cls))
                if (ids != null && ids.isNotEmpty()) {
                    val intent = Intent(context, cls).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            }
        } catch (_: Throwable) {}
    }
}

class LimiterWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(EqStorageManager.PREFS_NAME, Context.MODE_PRIVATE)
        val limiterOn = prefs.getBoolean("limiter_enabled", true)
        val bmp = MobilEqWidgetRenderer.renderSingleCircleBitmap("limiter", limiterOn)
        val pending = PendingIntent.getBroadcast(
            context, 201,
            Intent(ACTION_TOGGLE_LIMITER).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val layoutId = context.resources.getIdentifier("widget_image_only", "layout", context.packageName).takeIf { it != 0 } ?: android.R.layout.activity_list_item
        val imgId = context.resources.getIdentifier("widget_img", "id", context.packageName).takeIf { it != 0 } ?: android.R.id.icon
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, layoutId).apply {
                setImageViewBitmap(imgId, bmp)
                setOnClickPendingIntent(imgId, pending)
            }
            try { appWidgetManager.updateAppWidget(id, views) } catch (_: Throwable) {}
        }
    }
}

class OnOffWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(EqStorageManager.PREFS_NAME, Context.MODE_PRIVATE)
        val eqOn = prefs.getBoolean("eq_enabled", true)
        val bmp = MobilEqWidgetRenderer.renderSingleCircleBitmap("on/off", eqOn)
        val pending = PendingIntent.getBroadcast(
            context, 202,
            Intent(ACTION_TOGGLE_EQ).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val layoutId = context.resources.getIdentifier("widget_image_only", "layout", context.packageName).takeIf { it != 0 } ?: android.R.layout.activity_list_item
        val imgId = context.resources.getIdentifier("widget_img", "id", context.packageName).takeIf { it != 0 } ?: android.R.id.icon
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, layoutId).apply {
                setImageViewBitmap(imgId, bmp)
                setOnClickPendingIntent(imgId, pending)
            }
            try { appWidgetManager.updateAppWidget(id, views) } catch (_: Throwable) {}
        }
    }
}

class FullControlWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(EqStorageManager.PREFS_NAME, Context.MODE_PRIVATE)
        val limiterOn = prefs.getBoolean("limiter_enabled", true)
        val eqOn = prefs.getBoolean("eq_enabled", true)
        val prof = prefs.getString("active_profile_name", "Varsayılan") ?: "Varsayılan"
        val outInfo = detectCurrentAudioOutput(context)
        val outLabel = when (outInfo.type) {
            AudioOutputType.USB_DAC -> outInfo.customDeviceName ?: "USB DAC"
            AudioOutputType.BLUETOOTH -> "Bluetooth"
            AudioOutputType.WIRED_HEADPHONES -> "Kablolu Kulaklık"
            AudioOutputType.SPEAKER -> "Hoparlör"
        }
        val bmp = MobilEqWidgetRenderer.renderFullCardBitmap(
            outputLabel = outLabel,
            profileName = prof,
            limiterOn = limiterOn,
            eqOn = eqOn,
            autoEqOn = false,
            preAmpText = "db 0"
        )
        val pending = PendingIntent.getBroadcast(
            context, 203,
            Intent(ACTION_TOGGLE_EQ).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val layoutId = context.resources.getIdentifier("widget_image_only", "layout", context.packageName).takeIf { it != 0 } ?: android.R.layout.activity_list_item
        val imgId = context.resources.getIdentifier("widget_img", "id", context.packageName).takeIf { it != 0 } ?: android.R.id.icon
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, layoutId).apply {
                setImageViewBitmap(imgId, bmp)
                setOnClickPendingIntent(imgId, pending)
            }
            try { appWidgetManager.updateAppWidget(id, views) } catch (_: Throwable) {}
        }
    }
}

class CompactControlWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(EqStorageManager.PREFS_NAME, Context.MODE_PRIVATE)
        val limiterOn = prefs.getBoolean("limiter_enabled", true)
        val eqOn = prefs.getBoolean("eq_enabled", true)
        val outInfo = detectCurrentAudioOutput(context)
        val outLabel = when (outInfo.type) {
            AudioOutputType.USB_DAC -> outInfo.customDeviceName ?: "USB DAC"
            AudioOutputType.BLUETOOTH -> "Bluetooth"
            AudioOutputType.WIRED_HEADPHONES -> "Kablolu"
            AudioOutputType.SPEAKER -> "Hoparlör"
        }
        val bmp = MobilEqWidgetRenderer.renderCompactCardBitmap(outLabel, limiterOn, eqOn)
        val pending = PendingIntent.getBroadcast(
            context, 204,
            Intent(ACTION_TOGGLE_EQ).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val layoutId = context.resources.getIdentifier("widget_image_only", "layout", context.packageName).takeIf { it != 0 } ?: android.R.layout.activity_list_item
        val imgId = context.resources.getIdentifier("widget_img", "id", context.packageName).takeIf { it != 0 } ?: android.R.id.icon
        appWidgetIds.forEach { id ->
            val views = RemoteViews(context.packageName, layoutId).apply {
                setImageViewBitmap(imgId, bmp)
                setOnClickPendingIntent(imgId, pending)
            }
            try { appWidgetManager.updateAppWidget(id, views) } catch (_: Throwable) {}
        }
    }
}

class MobilEqTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        syncTileState()
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences(EqStorageManager.PREFS_NAME, Context.MODE_PRIVATE)
        val newState = !prefs.getBoolean("eq_enabled", true)
        prefs.edit().putBoolean("eq_enabled", newState).apply()
        sendBroadcast(Intent(ACTION_TOGGLE_EQ).setPackage(packageName))
        syncTileState()
    }

    private fun syncTileState() {
        val tile = qsTile ?: return
        val prefs = getSharedPreferences(EqStorageManager.PREFS_NAME, Context.MODE_PRIVATE)
        val eqOn = prefs.getBoolean("eq_enabled", true)
        val prof = prefs.getString("active_profile_name", "Varsayılan") ?: "Varsayılan"
        tile.label = "MobilEQ"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (eqOn) prof else "Kapalı"
        }
        tile.state = if (eqOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}

// ============================================================================
// JSON DÖNÜŞTÜRME VE KALICI HAFIZA YÖNETİMİ
// ============================================================================
private object EqStorageManager {
    const val PREFS_NAME = "mobileq_storage_v7"

    fun profileToJsonString(profile: EqProfile): String {
        val obj = JSONObject()
        obj.put("name", profile.name)
        obj.put("eqMode", profile.eqMode.name)
        obj.put("preAmpDb", profile.preAmpDb.toDouble())
        obj.put("autoEqModel", profile.autoEqModel ?: JSONObject.NULL)
        obj.put("assignedDevice", profile.assignedDevice ?: JSONObject.NULL)
        obj.put("isDefault", profile.isDefault)

        val gainsArr = JSONArray()
        profile.graphicGains.forEach { gainsArr.put(it.toDouble()) }
        obj.put("graphicGains", gainsArr)

        val paramArr = JSONArray()
        profile.parametricColumns.forEach { col ->
            val cObj = JSONObject()
            cObj.put("id", col.id)
            cObj.put("db", col.db.toDouble())
            cObj.put("q", col.q.toDouble())
            cObj.put("hz", col.hz)
            cObj.put("color", col.highlightColor.toArgb())
            paramArr.put(cObj)
        }
        obj.put("parametricColumns", paramArr)
        return obj.toString(2)
    }

    fun profileFromJsonObject(obj: JSONObject): EqProfile {
        val name = obj.optString("name", "Imported Profile")
        val modeStr = obj.optString("eqMode", EqMode.GRAPHIC.name)
        val eqMode = if (modeStr == EqMode.PARAMETRIC.name) EqMode.PARAMETRIC else EqMode.GRAPHIC
        val preAmp = obj.optDouble("preAmpDb", 0.0).toFloat()
        val autoEq = if (obj.isNull("autoEqModel")) null else obj.optString("autoEqModel").takeIf { it.isNotBlank() }
        val assigned = if (obj.isNull("assignedDevice")) null else obj.optString("assignedDevice").takeIf { it.isNotBlank() }
        val isDef = obj.optBoolean("isDefault", false)

        val gainsArr = obj.optJSONArray("graphicGains") ?: JSONArray()
        val gains = mutableListOf<Float>()
        for (i in 0 until gainsArr.length()) {
            gains.add(gainsArr.optDouble(i, 0.0).toFloat())
        }

        val paramArr = obj.optJSONArray("parametricColumns") ?: JSONArray()
        val cols = mutableListOf<ParametricColumn>()
        for (k in 0 until paramArr.length()) {
            val cObj = paramArr.getJSONObject(k)
            val cInt = cObj.optInt("color", SelectionTeal.toArgb())
            cols.add(
                ParametricColumn(
                    id = cObj.optInt("id", k + 1),
                    db = cObj.optDouble("db", 0.0).toFloat(),
                    q = cObj.optDouble("q", 1.4).toFloat(),
                    hz = cObj.optInt("hz", 31),
                    highlightColor = if (cInt == 0) Color.Transparent else Color(cInt)
                )
            )
        }

        return EqProfile(
            name = name,
            eqMode = eqMode,
            preAmpDb = preAmp,
            graphicGains = if (gains.isEmpty()) Default10Bands.map { 0f } else gains,
            parametricColumns = if (cols.isEmpty()) listOf(ParametricColumn(1, 0f, 1.40f, 31, false, SelectionTeal)) else cols,
            autoEqModel = autoEq,
            assignedDevice = assigned,
            isDefault = isDef
        )
    }

    fun parseImportedJson(rawJson: String): EqProfile? {
        return try {
            val trimmed = rawJson.trim()
            if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                if (arr.length() > 0) profileFromJsonObject(arr.getJSONObject(0)).copy(isDefault = false) else null
            } else {
                profileFromJsonObject(JSONObject(trimmed)).copy(isDefault = false)
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun saveProfiles(
        context: Context,
        profiles: List<EqProfile>,
        activeProfileName: String,
        lastGraphicProfileName: String,
        lastParametricProfileName: String
    ) {
        try {
            val arr = JSONArray()
            profiles.forEach { p ->
                arr.put(JSONObject(profileToJsonString(p)))
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("saved_profiles", arr.toString())
                .putString("active_profile_name", activeProfileName)
                .putString("last_graphic_profile", lastGraphicProfileName)
                .putString("last_parametric_profile", lastParametricProfileName)
                .apply()
        } catch (_: Throwable) {}
    }

    fun loadProfiles(context: Context): List<EqProfile> {
        val defGraphic = createBuiltInDefaultGraphicProfile()
        val defParam = createBuiltInDefaultParametricProfile()
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val str = prefs.getString("saved_profiles", null)
                ?: return listOf(defGraphic, defParam)
            val arr = JSONArray(str)
            val list = mutableListOf<EqProfile>()
            for (i in 0 until arr.length()) {
                list.add(profileFromJsonObject(arr.getJSONObject(i)))
            }
            if (list.none { it.isDefault && it.eqMode == EqMode.GRAPHIC }) {
                list.add(0, defGraphic)
            }
            if (list.none { it.isDefault && it.eqMode == EqMode.PARAMETRIC }) {
                list.add(1, defParam)
            }
            list
        } catch (_: Throwable) {
            listOf(defGraphic, defParam)
        }
    }

    fun saveDetectedPlayers(context: Context, players: List<DetectedPlayerApp>) {
        try {
            val arr = JSONArray()
            players.forEach { p ->
                val o = JSONObject()
                o.put("pkg", p.packageName)
                o.put("name", p.displayName)
                o.put("enabled", p.isEnabled)
                o.put("session", p.lastSessionId)
                arr.put(o)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("detected_players", arr.toString())
                .apply()
        } catch (_: Throwable) {}
    }

    fun loadDetectedPlayers(context: Context): List<DetectedPlayerApp> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val str = prefs.getString("detected_players", null) ?: return emptyList()
            val arr = JSONArray(str)
            val list = mutableListOf<DetectedPlayerApp>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    DetectedPlayerApp(
                        packageName = o.getString("pkg"),
                        displayName = o.getString("name"),
                        isEnabled = o.optBoolean("enabled", true),
                        lastSessionId = o.optInt("session", 0)
                    )
                )
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }
}

private fun resolveAppDisplayName(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString().takeIf { it.isNotBlank() } ?: packageName
    } catch (_: Throwable) {
        when {
            packageName.contains("spotify", ignoreCase = true) -> "Spotify"
            packageName.contains("deezer", ignoreCase = true) -> "Deezer"
            packageName.contains("youtube.music", ignoreCase = true) -> "YouTube Music"
            packageName.contains("youtube", ignoreCase = true) -> "YouTube"
            packageName.contains("apple.android.music", ignoreCase = true) -> "Apple Music"
            packageName.contains("poweramp", ignoreCase = true) -> "Poweramp"
            packageName.contains("musicolet", ignoreCase = true) -> "Musicolet"
            packageName.contains("tidal", ignoreCase = true) -> "TIDAL"
            else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }
}

private fun readConnectedDevicesTree(context: Context, strings: AppStrings): ConnectedAudioDevicesTree {
    return try {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ConnectedAudioDevicesTree(emptyList(), emptyList(), emptyList())
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        val btList = mutableListOf<String>()
        val wiredList = mutableListOf<String>()
        val dacList = mutableListOf<String>()

        devices.forEach { dev ->
            val rawName = dev.productName?.toString()?.trim()?.takeIf {
                it.isNotEmpty() && !it.equals(Build.MODEL, ignoreCase = true)
            }
            when {
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                        dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                            dev.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
                        )) -> {
                    val label = rawName ?: "${strings.outputBluetooth} #${btList.size + 1}"
                    if (!btList.contains(label)) btList.add(label)
                }

                dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    dev.type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
                    dev.type == AudioDeviceInfo.TYPE_LINE_DIGITAL ||
                    dev.type == AudioDeviceInfo.TYPE_AUX_LINE -> {
                    val label = rawName ?: strings.outputWired
                    if (!wiredList.contains(label)) wiredList.add(label)
                }

                dev.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    dev.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    dev.type == AudioDeviceInfo.TYPE_USB_ACCESSORY -> {
                    val label = rawName ?: strings.outputUsbDac
                    if (!dacList.contains(label)) dacList.add(label)
                }
            }
        }

        ConnectedAudioDevicesTree(
            bluetoothDevices = btList,
            wiredDevices = wiredList,
            usbDacDevices = dacList,
            hasSpeaker = true
        )
    } catch (_: Throwable) {
        ConnectedAudioDevicesTree(emptyList(), emptyList(), emptyList(), true)
    }
}

private fun detectCurrentAudioOutput(context: Context): AudioOutputInfo {
    return try {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return AudioOutputInfo(AudioOutputType.SPEAKER)
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        val usbDac = devices.firstOrNull { dev ->
            dev.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                dev.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                dev.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
        }
        if (usbDac != null) {
            val dacName = usbDac.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: "USB DAC"
            return AudioOutputInfo(AudioOutputType.USB_DAC, dacName)
        }

        val btDevice = devices.firstOrNull { dev ->
            dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                    dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                        dev.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
                    ))
        }
        if (btDevice != null) {
            val btName = btDevice.productName?.toString()?.trim()?.takeIf {
                it.isNotEmpty() && !it.equals(Build.MODEL, ignoreCase = true)
            }
            return AudioOutputInfo(AudioOutputType.BLUETOOTH, btName)
        }

        val wiredDevice = devices.firstOrNull { dev ->
            dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                dev.type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
                dev.type == AudioDeviceInfo.TYPE_LINE_DIGITAL ||
                dev.type == AudioDeviceInfo.TYPE_AUX_LINE
        }
        if (wiredDevice != null) {
            val wiredName = wiredDevice.productName?.toString()?.trim()?.takeIf {
                it.isNotEmpty() && !it.equals(Build.MODEL, ignoreCase = true)
            }
            return AudioOutputInfo(AudioOutputType.WIRED_HEADPHONES, wiredName)
        }

        AudioOutputInfo(AudioOutputType.SPEAKER)
    } catch (_: Throwable) {
        AudioOutputInfo(AudioOutputType.SPEAKER)
    }
}

// ============================================================================
// ANA UYGULAMA BİLEŞENİ (MobilEqApp)
// ============================================================================
@Composable
fun MobilEqApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(EqStorageManager.PREFS_NAME, Context.MODE_PRIVATE) }

    // Kayıtlı Profiller (Hem Varsayılan Grafik hem Varsayılan Parametric hazır bulunur)
    val initialProfiles = remember { EqStorageManager.loadProfiles(context) }
    var profiles by remember { mutableStateOf(initialProfiles) }

    // Grafik ve Parametrik modların son seçili profilleri ayrı ayrı hatırlanır!
    // Böylece bir Parametric profilin içindeyken Grafik EQ'ya geçildiğinde Varsayılan (veya son seçili) Grafik profile geçer,
    // tekrar Parametric EQ'ya dönüldüğünde ise kayıtlı Parametric profile geri döner ve kayıtlar asla bozulmaz!
    var lastGraphicProfileName by remember {
        mutableStateOf(prefs.getString("last_graphic_profile", "Varsayılan") ?: "Varsayılan")
    }
    var lastParametricProfileName by remember {
        mutableStateOf(prefs.getString("last_parametric_profile", "Varsayılan (Parametric)") ?: "Varsayılan (Parametric)")
    }

    val initialActiveName = remember {
        prefs.getString("active_profile_name", "Varsayılan") ?: "Varsayılan"
    }
    var activeProfileName by remember { mutableStateOf(initialActiveName) }

    val initialActiveProfile = remember {
        initialProfiles.find { it.name == initialActiveName } ?: initialProfiles.first()
    }

    var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }
    var eqMode by remember { mutableStateOf(initialActiveProfile.eqMode) }

    var detectedPlayers by remember { mutableStateOf(EqStorageManager.loadDetectedPlayers(context)) }
    var selectedBandCount by remember { mutableIntStateOf(10) }

    var preAmpDb by remember { mutableFloatStateOf(initialActiveProfile.preAmpDb) }
    var graphicBands by remember {
        mutableStateOf(
            Default10Bands.mapIndexed { idx, b ->
                b.copy(db = initialActiveProfile.graphicGains.getOrElse(idx) { 0f })
            }
        )
    }
    var parametricColumns by remember {
        mutableStateOf(
            initialActiveProfile.parametricColumns.ifEmpty {
                listOf(ParametricColumn(id = 1, db = 0f, q = 1.40f, hz = 31, isSelected = false, highlightColor = SelectionTeal))
            }
        )
    }
    var nextParametricId by remember {
        mutableIntStateOf((parametricColumns.maxOfOrNull { it.id } ?: 1) + 1)
    }
    var isParametricSelectionMode by remember { mutableStateOf(false) }

    // Tekil sütun renk paleti düzenleme durumu (1733.png sol üst yuvarlak renk paleti)
    var singleColorTargetColumnId by remember { mutableStateOf<Int?>(null) }

    // Alt Buton Durumları
    var isLimiterEnabled by remember { mutableStateOf(prefs.getBoolean("limiter_enabled", true)) }
    var isEqEnabled by remember { mutableStateOf(prefs.getBoolean("eq_enabled", true)) }
    var selectedAutoEqModel by remember { mutableStateOf<String?>(initialActiveProfile.autoEqModel) }
    var autoSaveEnabled by remember { mutableStateOf(true) }
    var waveletNotificationEnabled by remember { mutableStateOf(prefs.getBoolean("wavelet_notif_enabled", true)) }

    // Hesap Durumu
    var isLoggedIn by remember { mutableStateOf(prefs.getBoolean("is_logged_in", false)) }
    var userName by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    var userEmail by remember { mutableStateOf(prefs.getString("user_email", "örnekmail@gmail.com") ?: "örnekmail@gmail.com") }

    // Ayarlar Değişkenleri
    var selectedLanguage by remember { mutableStateOf("Türkçe") }
    val strings = remember(selectedLanguage) { getAppStrings(selectedLanguage) }
    var showLanguagePopup by remember { mutableStateOf(false) }
    var syncEnabled by remember { mutableStateOf(true) }
    var audioProcessingEnabled by remember { mutableStateOf(true) }
    var autoPreAmpEnabled by remember { mutableStateOf(false) }
    var autoReportEnabled by remember { mutableStateOf(true) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(waveletNotificationEnabled) {
        if (waveletNotificationEnabled && Build.VERSION.SDK_INT >= 33) {
            val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (perm != PackageManager.PERMISSION_GRANTED) {
                try { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) } catch (_: Throwable) {}
            }
        }
    }

    var isMusicPlaying by remember { mutableStateOf(false) }
    var activeSessionDetected by remember { mutableStateOf(false) }
    var activeAudioSessionId by remember { mutableIntStateOf(0) }
    var audioOutputInfo by remember { mutableStateOf(detectCurrentAudioOutput(context)) }
    var connectedDevicesTree by remember { mutableStateOf(readConnectedDevicesTree(context, strings)) }

    // Profili ve kendi modunu (Grafik veya Parametric) yükleyen fonksiyon
    fun applyProfileComplete(profile: EqProfile) {
        activeProfileName = profile.name
        eqMode = profile.eqMode
        if (profile.eqMode == EqMode.GRAPHIC) {
            lastGraphicProfileName = profile.name
        } else {
            lastParametricProfileName = profile.name
        }
        isParametricSelectionMode = false
        preAmpDb = profile.preAmpDb
        selectedAutoEqModel = profile.autoEqModel
        if (profile.eqMode == EqMode.GRAPHIC) {
            val targetCount = if (profile.graphicGains.size in listOf(8, 10, 12, 14)) {
                profile.graphicGains.size
            } else {
                selectedBandCount
            }
            selectedBandCount = targetCount
            val baseBands = buildBandsForCount(targetCount, graphicBands)
            graphicBands = baseBands.mapIndexed { idx, b ->
                b.copy(db = profile.graphicGains.getOrElse(idx) { 0f })
            }
        } else {
            parametricColumns = profile.parametricColumns.ifEmpty {
                listOf(ParametricColumn(id = 1, db = 0f, q = 1.40f, hz = 31, isSelected = false, highlightColor = SelectionTeal))
            }
        }
        EqStorageManager.saveProfiles(
            context,
            profiles,
            profile.name,
            lastGraphicProfileName,
            lastParametricProfileName
        )
    }

    // GRAFİK <-> PARAMETRIC MOD GEÇİŞ FONKSİYONU (Kayıt Bozulmasını Engelleyen Çözüm):
    // - Kullanıcı bir Parametric profildeyken Grafik EQ'ya geçerse:
    //   Kayıtlı Parametric profil ASLA Grafik'e dönüşmez; uygulama Varsayılan Grafik EQ profiline (veya son seçili Grafik profile) geçer.
    // - Kullanıcı geri Parametric EQ'ya geçtiğinde ise kayıtlı olan Parametric profile geri döner.
    // - Aynı kural Grafik profillerden Parametric EQ'ya geçerken de birebir çalışır!
    fun switchBetweenGraphicAndParametricMode() {
        val targetMode = if (eqMode == EqMode.GRAPHIC) EqMode.PARAMETRIC else EqMode.GRAPHIC
        if (targetMode == EqMode.GRAPHIC) {
            // Grafik moda geçiliyor: Eğer özel bir Parametric profildeysek Varsayılan Grafik profile,
            // ya da daha önce seçilmiş kayıtlı bir Grafik profil varsa ona geç.
            val currentProf = profiles.find { it.name == activeProfileName }
            val targetGraphicProfile = if (currentProf != null && !currentProf.isDefault && currentProf.eqMode == EqMode.PARAMETRIC) {
                // Özel bir Parametric profilden Grafik'e geçildiğinde Varsayılan Grafik profiline (veya son kayıtlı Grafik profile) geç
                profiles.find { it.name == lastGraphicProfileName && it.eqMode == EqMode.GRAPHIC }
                    ?: profiles.firstOrNull { it.isDefault && it.eqMode == EqMode.GRAPHIC }
                    ?: createBuiltInDefaultGraphicProfile()
            } else {
                profiles.find { it.name == lastGraphicProfileName && it.eqMode == EqMode.GRAPHIC }
                    ?: profiles.firstOrNull { it.isDefault && it.eqMode == EqMode.GRAPHIC }
                    ?: createBuiltInDefaultGraphicProfile()
            }
            applyProfileComplete(targetGraphicProfile)
        } else {
            // Parametric moda geçiliyor: Kayıtlı Parametric profil varsa ona, yoksa Varsayılan Parametric profile geç.
            val targetParamProfile = profiles.find { it.name == lastParametricProfileName && it.eqMode == EqMode.PARAMETRIC }
                ?: profiles.firstOrNull { !it.isDefault && it.eqMode == EqMode.PARAMETRIC }
                ?: profiles.firstOrNull { it.isDefault && it.eqMode == EqMode.PARAMETRIC }
                ?: createBuiltInDefaultParametricProfile()
            applyProfileComplete(targetParamProfile)
        }
    }

    DisposableEffect(context, strings) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                audioOutputInfo = detectCurrentAudioOutput(context)
                connectedDevicesTree = readConnectedDevicesTree(context, strings)
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                audioOutputInfo = detectCurrentAudioOutput(context)
                connectedDevicesTree = readConnectedDevicesTree(context, strings)
            }
        }
        try {
            am?.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        } catch (_: Throwable) {}

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val pkg = intent?.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)
                val sessionId = intent?.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0) ?: 0

                when (intent?.action) {
                    AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                        if (sessionId > 0) {
                            activeSessionDetected = true
                            activeAudioSessionId = sessionId
                        }
                        if (!pkg.isNullOrBlank() && pkg != context.packageName) {
                            val appLabel = resolveAppDisplayName(context, pkg)
                            val updated = if (detectedPlayers.any { it.packageName == pkg }) {
                                detectedPlayers.map {
                                    if (it.packageName == pkg) it.copy(displayName = appLabel, lastSessionId = sessionId) else it
                                }
                            } else {
                                detectedPlayers + DetectedPlayerApp(
                                    packageName = pkg,
                                    displayName = appLabel,
                                    isEnabled = true,
                                    lastSessionId = sessionId
                                )
                            }
                            detectedPlayers = updated
                            EqStorageManager.saveDetectedPlayers(context, updated)
                        }
                    }
                    AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                        if (sessionId == activeAudioSessionId) {
                            activeSessionDetected = false
                            activeAudioSessionId = 0
                        }
                    }
                    ACTION_TOGGLE_EQ -> {
                        isEqEnabled = !isEqEnabled
                    }
                    ACTION_TOGGLE_LIMITER -> {
                        isLimiterEnabled = !isLimiterEnabled
                    }
                    ACTION_TOGGLE_MODE -> {
                        switchBetweenGraphicAndParametricMode()
                    }
                }
                audioOutputInfo = detectCurrentAudioOutput(context)
                connectedDevicesTree = readConnectedDevicesTree(context, strings)
            }
        }
        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(ACTION_TOGGLE_EQ)
            addAction(ACTION_TOGGLE_LIMITER)
            addAction(ACTION_TOGGLE_MODE)
        }
        try {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: Throwable) {}

        onDispose {
            try { am?.unregisterAudioDeviceCallback(deviceCallback) } catch (_: Throwable) {}
            try { context.unregisterReceiver(receiver) } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(context, strings) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        while (true) {
            isMusicPlaying = try {
                audioManager?.isMusicActive == true
            } catch (_: Throwable) {
                false
            }
            audioOutputInfo = detectCurrentAudioOutput(context)
            connectedDevicesTree = readConnectedDevicesTree(context, strings)
            delay(500L)
        }
    }

    val currentOutputLabel = when (audioOutputInfo.type) {
        AudioOutputType.USB_DAC -> audioOutputInfo.customDeviceName ?: strings.outputUsbDac
        AudioOutputType.BLUETOOTH -> strings.outputBluetooth
        AudioOutputType.WIRED_HEADPHONES -> strings.outputWired
        AudioOutputType.SPEAKER -> strings.outputSpeaker
    }

    val displayProfileName = when {
        (activeProfileName == "Varsayılan" || activeProfileName == "Varsayılan (Parametric)") && strings.isEnglish -> "Default"
        activeProfileName == "Varsayılan (Parametric)" -> "Varsayılan"
        else -> activeProfileName
    }

    LaunchedEffect(
        waveletNotificationEnabled,
        isEqEnabled,
        isLimiterEnabled,
        eqMode,
        currentOutputLabel,
        displayProfileName,
        preAmpDb,
        selectedLanguage
    ) {
        prefs.edit()
            .putBoolean("eq_enabled", isEqEnabled)
            .putBoolean("limiter_enabled", isLimiterEnabled)
            .putBoolean("wavelet_notif_enabled", waveletNotificationEnabled)
            .apply()

        MobilEqWidgetRenderer.refreshAllWidgets(context)
        MobilEqNotificationHelper.showOrUpdateNotification(
            context = context,
            enabled = waveletNotificationEnabled,
            isEqEnabled = isEqEnabled,
            isLimiterEnabled = isLimiterEnabled,
            eqMode = eqMode,
            outputLabel = currentOutputLabel,
            profileName = displayProfileName,
            preAmpDb = preAmpDb,
            isEnglish = strings.isEnglish
        )
    }

    // TAKILAN CİHAZA ATANMIŞ PROFİLİ VE MODUNU OTOMATİK UYGULA
    var lastAutoMatchedSignature by remember { mutableStateOf("") }
    LaunchedEffect(
        audioOutputInfo.type,
        audioOutputInfo.customDeviceName,
        connectedDevicesTree.bluetoothDevices,
        connectedDevicesTree.wiredDevices,
        connectedDevicesTree.usbDacDevices,
        profiles.size
    ) {
        val candidateTargets = buildList {
            audioOutputInfo.customDeviceName?.let { add(it) }
            when (audioOutputInfo.type) {
                AudioOutputType.USB_DAC -> {
                    addAll(connectedDevicesTree.usbDacDevices)
                    add(strings.outputUsbDac)
                    add("USB DAC")
                }
                AudioOutputType.BLUETOOTH -> {
                    addAll(connectedDevicesTree.bluetoothDevices)
                    add(strings.outputBluetooth)
                    add("Bluetooth")
                }
                AudioOutputType.WIRED_HEADPHONES -> {
                    addAll(connectedDevicesTree.wiredDevices)
                    add(strings.outputWired)
                    add("Kablolu Kulaklık")
                    add("Wired Headset")
                }
                AudioOutputType.SPEAKER -> {
                    add(strings.outputSpeaker)
                    add("Hoparlör")
                    add("Speaker")
                }
            }
        }.distinct()

        val signature = "${audioOutputInfo.type}_${candidateTargets.joinToString("|")}"
        if (signature != lastAutoMatchedSignature) {
            lastAutoMatchedSignature = signature
            val matchedProfile = candidateTargets.firstNotNullOfOrNull { targetName ->
                profiles.firstOrNull { p ->
                    p.assignedDevice != null && p.assignedDevice.equals(targetName, ignoreCase = true)
                }
            }
            if (matchedProfile != null && (activeProfileName != matchedProfile.name || eqMode != matchedProfile.eqMode)) {
                applyProfileComplete(matchedProfile)
                val modeText = if (matchedProfile.eqMode == EqMode.GRAPHIC) strings.graphic else strings.parametric
                Toast.makeText(
                    context,
                    strings.toastAutoSwitchedProfile(
                        matchedProfile.assignedDevice ?: currentOutputLabel,
                        matchedProfile.name,
                        modeText
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // OTOMATİK KAYIT:
    // Yalnızca aktif profilin kendi modu (p.eqMode == eqMode) ile ekran modu aynıysa değerleri günceller;
    // profilin kendi eqMode türünü ASLA değiştirmez!
    LaunchedEffect(preAmpDb, graphicBands, parametricColumns, selectedAutoEqModel, autoSaveEnabled) {
        if (autoSaveEnabled && profiles.any { it.name == activeProfileName && it.eqMode == eqMode }) {
            val updated = profiles.map { p ->
                if (p.name == activeProfileName && p.eqMode == eqMode) {
                    p.copy(
                        preAmpDb = preAmpDb,
                        graphicGains = if (eqMode == EqMode.GRAPHIC) graphicBands.map { it.db } else p.graphicGains,
                        parametricColumns = if (eqMode == EqMode.PARAMETRIC) parametricColumns else p.parametricColumns,
                        autoEqModel = selectedAutoEqModel
                    )
                } else p
            }
            profiles = updated
            EqStorageManager.saveProfiles(
                context,
                updated,
                activeProfileName,
                lastGraphicProfileName,
                lastParametricProfileName
            )
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val profileToExport = EqProfile(
                    name = activeProfileName,
                    eqMode = eqMode,
                    preAmpDb = preAmpDb,
                    graphicGains = graphicBands.map { it.db },
                    parametricColumns = parametricColumns,
                    autoEqModel = selectedAutoEqModel,
                    assignedDevice = audioOutputInfo.customDeviceName ?: currentOutputLabel
                )
                val jsonBytes = EqStorageManager.profileToJsonString(profileToExport).toByteArray(Charsets.UTF_8)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonBytes)
                }
                Toast.makeText(context, strings.toastExported(displayProfileName), Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
                Toast.makeText(context, strings.toastImportError, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                val importedProfile = content?.let { EqStorageManager.parseImportedJson(it) }
                if (importedProfile != null) {
                    val updatedList = profiles.filterNot { it.name.equals(importedProfile.name, ignoreCase = true) && !it.isDefault } + importedProfile
                    profiles = updatedList
                    applyProfileComplete(importedProfile)
                    Toast.makeText(context, strings.toastImported(importedProfile.name), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, strings.toastImportError, Toast.LENGTH_SHORT).show()
                }
            } catch (_: Throwable) {
                Toast.makeText(context, strings.toastImportError, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showQuickMenu by remember { mutableStateOf(false) }
    var showAutoEqDialog by remember { mutableStateOf(false) }
    var showProfilesDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showSaveAndAssignDialog by remember { mutableStateOf(false) }
    var showBulkColorPickerModal by remember { mutableStateOf(false) }
    var editingHzColumnId by remember { mutableStateOf<Int?>(null) }

    val hasSessionDetectionProblem = isMusicPlaying && !activeSessionDetected && activeAudioSessionId <= 0
    val activeWarningMessage: String? = if (hasSessionDetectionProblem) {
        strings.warnSessionError
    } else {
        null
    }

    BackHandler(enabled = currentScreen != AppScreen.MAIN || isParametricSelectionMode || showQuickMenu) {
        when {
            showQuickMenu -> showQuickMenu = false
            isParametricSelectionMode -> {
                isParametricSelectionMode = false
                parametricColumns = parametricColumns.map { it.copy(isSelected = false) }
            }
            currentScreen == AppScreen.SETTINGS_PASSWORD_EMAIL -> currentScreen = AppScreen.SETTINGS_ACCOUNT
            currentScreen == AppScreen.SETTINGS_GRAPHIC_BANDS || currentScreen == AppScreen.SETTINGS_PLAYERS -> currentScreen = AppScreen.SETTINGS_EQUALIZER
            currentScreen != AppScreen.MAIN -> currentScreen = AppScreen.SETTINGS_MAIN.takeIf { currentScreen != AppScreen.SETTINGS_MAIN } ?: AppScreen.MAIN
        }
    }

    fun updateGraphicBandCount(count: Int) {
        selectedBandCount = count
        graphicBands = buildBandsForCount(count, graphicBands)
    }

    when (currentScreen) {
        AppScreen.MAIN -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgBlack)
                    .systemBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. ÜST SATIR: Sol Üstte "by KostiSoftware" (veya Parametrik Seçim Modunda Geri Oku)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (isParametricSelectionMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    isParametricSelectionMode = false
                                    parametricColumns = parametricColumns.map { it.copy(isSelected = false) }
                                }
                            ) {
                                Text(
                                    text = "←",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "by KostiSoftware",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                )
                            }
                        } else {
                            Text(
                                text = "by KostiSoftware",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }

                    // 2. ORTA BÖLÜM: FERAH YENİ GRAFİK EQ (1730.png) VEYA PARAMETRİK EQ (1731.png & 1733.png)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp)
                    ) {
                        if (eqMode == EqMode.GRAPHIC) {
                            GraphicEqualizerSection(
                                preAmpDb = preAmpDb,
                                onPreAmpChange = { newPreAmp ->
                                    preAmpDb = newPreAmp
                                },
                                bands = graphicBands,
                                onBandChange = { bandId, newDb ->
                                    graphicBands = graphicBands.map { b ->
                                        if (b.id == bandId) b.copy(db = newDb) else b
                                    }
                                }
                            )
                        } else {
                            ParametricEqualizerSection(
                                strings = strings,
                                preAmpDb = preAmpDb,
                                onPreAmpChange = { newPreAmp ->
                                    preAmpDb = newPreAmp
                                },
                                columns = parametricColumns,
                                isSelectionMode = isParametricSelectionMode,
                                onEnterSelectionMode = { colId ->
                                    isParametricSelectionMode = true
                                    parametricColumns = parametricColumns.map {
                                        if (it.id == colId) it.copy(isSelected = true) else it
                                    }
                                },
                                onToggleSelectColumn = { colId ->
                                    parametricColumns = parametricColumns.map {
                                        if (it.id == colId) it.copy(isSelected = !it.isSelected) else it
                                    }
                                },
                                onOpenSingleColumnColorPicker = { colId ->
                                    singleColorTargetColumnId = colId
                                },
                                onUpdateColumnDb = { colId, newDb ->
                                    parametricColumns = parametricColumns.map {
                                        if (it.id == colId) it.copy(db = newDb) else it
                                    }
                                },
                                onUpdateColumnQ = { colId, newQ ->
                                    parametricColumns = parametricColumns.map {
                                        if (it.id == colId) it.copy(q = newQ) else it
                                    }
                                },
                                onAddColumn = {
                                    val defaultFreqs = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
                                    val palette = listOf(SelectionTeal, Color(0xFF7BD33C), Color(0xFF7B1FA2), Color(0xFFEF6C00))
                                    val nextHz = defaultFreqs.getOrElse(parametricColumns.size % defaultFreqs.size) { 1000 }
                                    val nextColColor = palette[parametricColumns.size % palette.size]
                                    parametricColumns = parametricColumns + ParametricColumn(
                                        id = nextParametricId++,
                                        db = 0f,
                                        q = 1.40f,
                                        hz = nextHz,
                                        isSelected = false,
                                        highlightColor = nextColColor
                                    )
                                },
                                onEditHzClick = { colId ->
                                    editingHzColumnId = colId
                                }
                            )
                        }
                    }

                    // 3. ALT BÖLÜM: PARAMETRİK TOPLU DÜZENLEME ÇUBUĞU (1733.png) VEYA ANA KONTROLLER (1730.png & 1731.png)
                    if (eqMode == EqMode.PARAMETRIC && isParametricSelectionMode) {
                        val currentPreviewColor = parametricColumns.firstOrNull { it.isSelected && it.highlightColor != Color.Transparent }?.highlightColor
                            ?: SelectionTeal

                        ParametricSelectionBottomBar(
                            strings = strings,
                            allSelected = parametricColumns.isNotEmpty() && parametricColumns.all { it.isSelected },
                            selectedColor = currentPreviewColor,
                            onDeleteSelected = {
                                val remaining = parametricColumns.filterNot { it.isSelected }
                                parametricColumns = remaining.ifEmpty {
                                    listOf(ParametricColumn(id = nextParametricId++, db = 0f, q = 1.40f, hz = 31, isSelected = false, highlightColor = SelectionTeal))
                                }
                                isParametricSelectionMode = false
                            },
                            onOpenBulkColorPicker = {
                                showBulkColorPickerModal = true
                            },
                            onToggleSelectAll = { selectAll ->
                                parametricColumns = parametricColumns.map { it.copy(isSelected = selectAll) }
                                if (!selectAll) {
                                    isParametricSelectionMode = false
                                }
                            }
                        )
                    } else {
                        // SES GÖRSELLEŞTİRİCİ BAR (1730.png & 1731.png)
                        AudioVisualizerBar(
                            isMusicPlaying = isMusicPlaying,
                            eqMode = eqMode,
                            graphicBands = graphicBands,
                            parametricColumns = parametricColumns,
                            preAmpDb = preAmpDb
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // UYARI SİMGESİ SATIRI (1730.png & 1731.png: Ses barının hemen altında, limiter butonunun üstünde)
                        WarningIndicatorRow(
                            visible = activeWarningMessage != null,
                            warningText = activeWarningMessage ?: ""
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        BottomControlPanel(
                            strings = strings,
                            isLimiterEnabled = isLimiterEnabled,
                            onToggleLimiter = { isLimiterEnabled = !isLimiterEnabled },
                            isEqEnabled = isEqEnabled,
                            onToggleEq = { isEqEnabled = !isEqEnabled },
                            isAutoEqSelected = selectedAutoEqModel != null,
                            onAutoEqClick = {
                                showQuickMenu = false
                                showAutoEqDialog = true
                            },
                            audioOutputLabel = currentOutputLabel,
                            onAudioOutputClick = {
                                audioOutputInfo = detectCurrentAudioOutput(context)
                                connectedDevicesTree = readConnectedDevicesTree(context, strings)
                                Toast.makeText(
                                    context,
                                    strings.toastOutputStatus(currentOutputLabel),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            activeProfileName = displayProfileName,
                            onProfileClick = {
                                showQuickMenu = false
                                showProfilesDialog = true
                            },
                            onQuickMenuClick = { showQuickMenu = !showQuickMenu },
                            onSettingsClick = {
                                showQuickMenu = false
                                currentScreen = AppScreen.SETTINGS_MAIN
                            },
                            eqMode = eqMode,
                            onToggleEqMode = {
                                showQuickMenu = false
                                switchBetweenGraphicAndParametricMode()
                            }
                        )
                    }
                }

                // 4. HIZLI ERİŞİM MENÜSÜ
                AnimatedVisibility(
                    visible = showQuickMenu,
                    enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.9f),
                    exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.9f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 148.dp)
                ) {
                    QuickAccessMenuCard(
                        strings = strings,
                        autoSaveEnabled = autoSaveEnabled,
                        onRename = {
                            showQuickMenu = false
                            showRenameDialog = true
                        },
                        onSaveAndAssign = {
                            showQuickMenu = false
                            connectedDevicesTree = readConnectedDevicesTree(context, strings)
                            showSaveAndAssignDialog = true
                        },
                        onImport = {
                            showQuickMenu = false
                            try {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            } catch (_: Throwable) {
                                Toast.makeText(context, strings.toastImportError, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onExport = {
                            showQuickMenu = false
                            try {
                                val fileName = "${activeProfileName.replace(" ", "_")}.json"
                                exportLauncher.launch(fileName)
                            } catch (_: Throwable) {
                                Toast.makeText(context, strings.toastImportError, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLoadDefault = {
                            showQuickMenu = false
                            preAmpDb = 0f
                            selectedAutoEqModel = null
                            if (eqMode == EqMode.GRAPHIC) {
                                graphicBands = graphicBands.map { it.copy(db = 0f) }
                            } else {
                                parametricColumns = listOf(ParametricColumn(id = nextParametricId++, db = 0f, q = 1.40f, hz = 31, isSelected = false, highlightColor = SelectionTeal))
                            }
                            Toast.makeText(context, strings.toastDefaultLoaded, Toast.LENGTH_SHORT).show()
                        },
                        onToggleAutoSave = {
                            autoSaveEnabled = !autoSaveEnabled
                        }
                    )
                }

                // 5A. TEKİL SÜTUN RENK PALETİ MODALI (1733.png Sol Üst Yuvarlağa Tıklanınca)
                singleColorTargetColumnId?.let { targetColId ->
                    val targetCol = parametricColumns.find { it.id == targetColId }
                    if (targetCol != null) {
                        ColorPickerModal(
                            strings = strings,
                            initialColor = targetCol.highlightColor,
                            onApplyColor = { chosenColor ->
                                parametricColumns = parametricColumns.map { col ->
                                    if (col.id == targetColId) col.copy(highlightColor = chosenColor) else col
                                }
                                singleColorTargetColumnId = null
                            },
                            onDismiss = { singleColorTargetColumnId = null }
                        )
                    }
                }

                // 5B. TOPLU SÜTUN RENK PALETİ MODALI (Alt Çubuktaki "Renk" Butonuna Tıklanınca)
                if (showBulkColorPickerModal) {
                    val initialColColor = parametricColumns.firstOrNull { it.isSelected }?.highlightColor ?: SelectionTeal
                    ColorPickerModal(
                        strings = strings,
                        initialColor = initialColColor,
                        onApplyColor = { chosenColor ->
                            parametricColumns = parametricColumns.map { col ->
                                if (col.isSelected) col.copy(highlightColor = chosenColor) else col
                            }
                            showBulkColorPickerModal = false
                        },
                        onDismiss = { showBulkColorPickerModal = false }
                    )
                }

                // 6. AUTOEQ ARAMA MENÜSÜ
                if (showAutoEqDialog) {
                    AutoEqSearchModal(
                        strings = strings,
                        presets = SampleAutoEqPresets,
                        selectedPresetName = selectedAutoEqModel,
                        onSelectPreset = { preset ->
                            selectedAutoEqModel = preset.modelName
                            eqMode = EqMode.GRAPHIC
                            preAmpDb = preset.preAmpDb
                            selectedBandCount = 10
                            graphicBands = Default10Bands.mapIndexed { idx, band ->
                                band.copy(db = preset.gains10Band.getOrElse(idx) { 0f })
                            }
                            showAutoEqDialog = false
                            Toast.makeText(context, strings.toastAutoEqApplied(preset.modelName), Toast.LENGTH_SHORT).show()
                        },
                        onClearPreset = {
                            selectedAutoEqModel = null
                            showAutoEqDialog = false
                            Toast.makeText(context, strings.toastAutoEqCleared, Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = { showAutoEqDialog = false }
                    )
                }

                // 7. KAYITLI PROFİLLER MENÜSÜ
                if (showProfilesDialog) {
                    SavedProfilesModal(
                        strings = strings,
                        profiles = profiles,
                        activeProfileName = activeProfileName,
                        onSelectProfile = { profile ->
                            applyProfileComplete(profile)
                            showProfilesDialog = false
                        },
                        onDeleteProfile = { profileToDelete ->
                            if (profileToDelete.isDefault) {
                                val resetDef = if (profileToDelete.eqMode == EqMode.GRAPHIC) {
                                    createBuiltInDefaultGraphicProfile()
                                } else {
                                    createBuiltInDefaultParametricProfile()
                                }
                                val updated = profiles.map {
                                    if (it.name == profileToDelete.name) resetDef else it
                                }
                                profiles = updated
                                if (activeProfileName == profileToDelete.name) {
                                    applyProfileComplete(resetDef)
                                } else {
                                    EqStorageManager.saveProfiles(context, updated, activeProfileName, lastGraphicProfileName, lastParametricProfileName)
                                }
                                Toast.makeText(context, strings.toastCannotDeleteDefault, Toast.LENGTH_SHORT).show()
                            } else {
                                val remaining = profiles.filterNot { it.name == profileToDelete.name }
                                profiles = remaining
                                if (activeProfileName == profileToDelete.name) {
                                    val fallback = remaining.firstOrNull { it.eqMode == profileToDelete.eqMode }
                                        ?: remaining.first()
                                    applyProfileComplete(fallback)
                                } else {
                                    EqStorageManager.saveProfiles(context, remaining, activeProfileName, lastGraphicProfileName, lastParametricProfileName)
                                }
                                Toast.makeText(context, strings.toastProfileDeleted(profileToDelete.name), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAddNewProfileClick = {
                            showProfilesDialog = false
                            connectedDevicesTree = readConnectedDevicesTree(context, strings)
                            showSaveAndAssignDialog = true
                        },
                        onDismiss = { showProfilesDialog = false }
                    )
                }

                // 8. KAYDET & ATA AĞAÇ MENÜSÜ
                if (showSaveAndAssignDialog) {
                    SaveAndAssignTreeModal(
                        strings = strings,
                        currentActiveProfileName = activeProfileName,
                        currentEqMode = eqMode,
                        connectedTree = connectedDevicesTree,
                        defaultSelectedDevice = audioOutputInfo.customDeviceName ?: currentOutputLabel,
                        onConfirm = { enteredName, chosenMode, selectedDeviceTarget ->
                            val cleanName = enteredName.trim()
                            if (cleanName.isEmpty()) {
                                Toast.makeText(context, strings.toastEnterProfileName, Toast.LENGTH_SHORT).show()
                            } else {
                                val assignedDev = if (selectedDeviceTarget == strings.assignNone) null else selectedDeviceTarget
                                val isDef = cleanName.equals("Varsayılan", ignoreCase = true) ||
                                    cleanName.equals("Varsayılan (Parametric)", ignoreCase = true) ||
                                    cleanName.equals("Default", ignoreCase = true)
                                val savedProfile = EqProfile(
                                    name = cleanName,
                                    eqMode = chosenMode,
                                    preAmpDb = preAmpDb,
                                    graphicGains = graphicBands.map { it.db },
                                    parametricColumns = parametricColumns,
                                    autoEqModel = selectedAutoEqModel,
                                    assignedDevice = assignedDev,
                                    isDefault = isDef
                                )
                                val updatedList = if (profiles.any { it.name.equals(cleanName, ignoreCase = true) }) {
                                    profiles.map {
                                        if (it.name.equals(cleanName, ignoreCase = true)) savedProfile else it
                                    }
                                } else {
                                    profiles + savedProfile
                                }
                                profiles = updatedList
                                activeProfileName = cleanName
                                eqMode = chosenMode
                                if (chosenMode == EqMode.GRAPHIC) {
                                    lastGraphicProfileName = cleanName
                                } else {
                                    lastParametricProfileName = cleanName
                                }
                                EqStorageManager.saveProfiles(
                                    context,
                                    updatedList,
                                    cleanName,
                                    lastGraphicProfileName,
                                    lastParametricProfileName
                                )
                                showSaveAndAssignDialog = false
                                val modeText = if (chosenMode == EqMode.GRAPHIC) strings.graphic else strings.parametric
                                Toast.makeText(
                                    context,
                                    strings.toastSavedAssigned(cleanName, modeText, selectedDeviceTarget),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onDismiss = { showSaveAndAssignDialog = false }
                    )
                }

                if (showRenameDialog) {
                    RenameProfileDialog(
                        title = strings.renameProfileTitle,
                        saveText = strings.save,
                        cancelText = strings.cancel,
                        currentName = displayProfileName,
                        onConfirm = { newName ->
                            val trimmed = newName.trim()
                            if (trimmed.isNotBlank()) {
                                val updatedList = profiles.map {
                                    if (it.name == activeProfileName) it.copy(name = trimmed, isDefault = false) else it
                                }
                                profiles = updatedList
                                activeProfileName = trimmed
                                if (eqMode == EqMode.GRAPHIC) lastGraphicProfileName = trimmed else lastParametricProfileName = trimmed
                                EqStorageManager.saveProfiles(context, updatedList, trimmed, lastGraphicProfileName, lastParametricProfileName)
                            }
                            showRenameDialog = false
                        },
                        onDismiss = { showRenameDialog = false }
                    )
                }

                editingHzColumnId?.let { targetId ->
                    val targetCol = parametricColumns.find { it.id == targetId }
                    if (targetCol != null) {
                        FrequencyEditDialog(
                            strings = strings,
                            initialHz = targetCol.hz,
                            onConfirm = { newHz ->
                                parametricColumns = parametricColumns.map {
                                    if (it.id == targetId) it.copy(hz = newHz.coerceIn(20, 20000)) else it
                                }
                                editingHzColumnId = null
                            },
                            onDismiss = { editingHzColumnId = null }
                        )
                    }
                }
            }
        }

        // --- AYARLAR EKRANLARI ---
        AppScreen.SETTINGS_MAIN -> {
            SettingsMainScreen(
                strings = strings,
                showLanguagePopup = showLanguagePopup,
                onSelectLanguage = { langKey ->
                    selectedLanguage = langKey
                    showLanguagePopup = false
                },
                onToggleLanguagePopup = { showLanguagePopup = !showLanguagePopup },
                onNavigate = { currentScreen = it },
                onBack = { currentScreen = AppScreen.MAIN }
            )
        }

        AppScreen.SETTINGS_ACCOUNT -> {
            SettingsAccountScreen(
                strings = strings,
                isLoggedIn = isLoggedIn,
                userName = userName,
                userEmail = userEmail,
                syncEnabled = syncEnabled,
                onToggleSync = { syncEnabled = it },
                onCreateAccountSuccess = { enteredUser, enteredMail ->
                    val finalUser = enteredUser.trim().ifEmpty { "Kullanıcı" }
                    val finalMail = enteredMail.trim().ifEmpty { "örnekmail@gmail.com" }
                    userName = finalUser
                    userEmail = finalMail
                    isLoggedIn = true
                    prefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("user_name", finalUser)
                        .putString("user_email", finalMail)
                        .apply()
                    Toast.makeText(context, strings.toastAccountCreated(finalUser), Toast.LENGTH_SHORT).show()
                },
                onLogOut = {
                    isLoggedIn = false
                    prefs.edit().putBoolean("is_logged_in", false).apply()
                },
                onNavigatePasswordEmail = { currentScreen = AppScreen.SETTINGS_PASSWORD_EMAIL },
                onBack = { currentScreen = AppScreen.SETTINGS_MAIN }
            )
        }

        AppScreen.SETTINGS_PASSWORD_EMAIL -> {
            SettingsPasswordEmailScreen(
                strings = strings,
                onUpdateEmail = { newMail ->
                    if (newMail.isNotBlank()) {
                        userEmail = newMail.trim()
                        prefs.edit().putString("user_email", userEmail).apply()
                    }
                },
                onBack = { currentScreen = AppScreen.SETTINGS_ACCOUNT }
            )
        }

        AppScreen.SETTINGS_EQUALIZER -> {
            SettingsEqualizerScreen(
                strings = strings,
                audioProcessingEnabled = audioProcessingEnabled,
                onToggleAudioProcessing = { audioProcessingEnabled = it },
                autoPreAmpEnabled = autoPreAmpEnabled,
                onToggleAutoPreAmp = { enabled ->
                    autoPreAmpEnabled = enabled
                    if (enabled) {
                        val maxPeak = if (eqMode == EqMode.GRAPHIC) {
                            graphicBands.maxOfOrNull { it.db } ?: 0f
                        } else {
                            parametricColumns.maxOfOrNull { it.db } ?: 0f
                        }
                        if (maxPeak > 0f) {
                            preAmpDb = (-maxPeak).coerceIn(-15f, 0f)
                        }
                    }
                },
                onNavigatePlayers = { currentScreen = AppScreen.SETTINGS_PLAYERS },
                onNavigateGraphicBands = { currentScreen = AppScreen.SETTINGS_GRAPHIC_BANDS },
                onBack = { currentScreen = AppScreen.SETTINGS_MAIN }
            )
        }

        AppScreen.SETTINGS_GRAPHIC_BANDS -> {
            SettingsGraphicBandsScreen(
                strings = strings,
                selectedBandCount = selectedBandCount,
                onSelectBandCount = { count ->
                    updateGraphicBandCount(count)
                    Toast.makeText(context, strings.toastBandsActivated(count), Toast.LENGTH_SHORT).show()
                },
                onBack = { currentScreen = AppScreen.SETTINGS_EQUALIZER }
            )
        }

        AppScreen.SETTINGS_PLAYERS -> {
            SettingsPlayersScreen(
                strings = strings,
                detectedPlayers = detectedPlayers,
                onTogglePlayer = { pkg, enabled ->
                    val updated = detectedPlayers.map {
                        if (it.packageName == pkg) it.copy(isEnabled = enabled) else it
                    }
                    detectedPlayers = updated
                    EqStorageManager.saveDetectedPlayers(context, updated)
                },
                onBack = { currentScreen = AppScreen.SETTINGS_EQUALIZER }
            )
        }

        AppScreen.SETTINGS_REPORT -> {
            SettingsReportScreen(
                strings = strings,
                autoReportEnabled = autoReportEnabled,
                onToggleAutoReport = { autoReportEnabled = it },
                onBack = { currentScreen = AppScreen.SETTINGS_MAIN }
            )
        }
    }
}

// ============================================================================
// 1. UYARI GÖSTERGESİ SATIRI (1730.png & 1731.png: Ses barının altında, limiter'ın üstünde)
// ============================================================================
@Composable
fun WarningIndicatorRow(
    visible: Boolean,
    warningText: String
) {
    if (!visible) {
        Spacer(modifier = Modifier.height(40.dp))
        return
    }

    var isExpanded by remember { mutableStateOf(false) }
    var triangleVisible by remember { mutableStateOf(true) }
    val borderProgress = remember { Animatable(0f) }
    val circleAlpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun toggleWarning() {
        scope.launch {
            if (!isExpanded) {
                isExpanded = true
                triangleVisible = false
                delay(180L)
                circleAlpha.snapTo(0f)
                triangleVisible = true
                borderProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
                )
            } else {
                borderProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
                )
                circleAlpha.animateTo(1f, tween(200))
                isExpanded = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(if (isExpanded) 275.dp else 44.dp)
                .height(44.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { toggleWarning() }
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 1.4.dp.toPx()
                val circleDiameter = size.height - strokeWidth * 2

                if (circleAlpha.value > 0f) {
                    drawArc(
                        color = Color.White.copy(alpha = circleAlpha.value),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth, strokeWidth),
                        size = Size(circleDiameter, circleDiameter),
                        style = Stroke(width = strokeWidth)
                    )
                }

                if (borderProgress.value > 0f) {
                    val fullPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = Rect(
                                    left = strokeWidth,
                                    top = strokeWidth,
                                    right = size.width - strokeWidth,
                                    bottom = size.height - strokeWidth
                                ),
                                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                            )
                        )
                    }
                    val measure = PathMeasure()
                    measure.setPath(fullPath, false)
                    val segmentPath = Path()
                    measure.getSegment(
                        startDistance = 0f,
                        stopDistance = measure.length * borderProgress.value,
                        destination = segmentPath,
                        startWithMoveTo = true
                    )
                    drawPath(
                        path = segmentPath,
                        color = Color.White,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                val centerX = size.height / 2f
                val centerY = size.height / 2f
                val triHalfW = 11.dp.toPx()
                val triTopY = centerY - 10.dp.toPx()
                val triBotY = centerY + 9.5.dp.toPx()

                if (triangleVisible) {
                    val trianglePath = Path().apply {
                        moveTo(centerX, triTopY)
                        lineTo(centerX - triHalfW, triBotY)
                        lineTo(centerX + triHalfW, triBotY)
                        close()
                    }
                    drawPath(
                        path = trianglePath,
                        color = AccentRed,
                        style = Stroke(width = 1.7.dp.toPx(), join = StrokeJoin.Round)
                    )
                }

                drawRoundRect(
                    color = AccentRed,
                    topLeft = Offset(centerX - 1.1.dp.toPx(), centerY - 3.5.dp.toPx()),
                    size = Size(2.2.dp.toPx(), 7.dp.toPx()),
                    cornerRadius = CornerRadius(1.1.dp.toPx())
                )
                drawCircle(
                    color = AccentRed,
                    radius = 1.3.dp.toPx(),
                    center = Offset(centerX, centerY + 6.dp.toPx())
                )
            }

            if (isExpanded && borderProgress.value > 0.75f) {
                Text(
                    text = warningText,
                    color = Color.White.copy(alpha = ((borderProgress.value - 0.75f) * 4f).coerceIn(0f, 1f)),
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 46.dp, end = 12.dp)
                )
            }
        }
    }
}

// ============================================================================
// 2. YENİ FERAH GRAFİK EQUALIZER BÖLÜMÜ (1730.png Tasarımı)
//    - PreAmp çubuğu ince kapsül çerçeve içinde
//    - Sol, orta ve sağda ince dikey ayırıcı çizgiler
//    - Tüm çubukların tutamaçları beyaz çerçeveli
// ============================================================================
@Composable
fun GraphicEqualizerSection(
    preAmpDb: Float,
    onPreAmpChange: (Float) -> Unit,
    bands: List<GraphicBand>,
    onBandChange: (Int, Float) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Bottom
    ) {
        // Sol İnce Dikey Çizgi
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(ThinFrameGray)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // PREAMP SÜTUNU (Kapsül çerçeveli)
        Column(
            modifier = Modifier
                .width(68.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(52.dp)
                    .border(1.2.dp, ThinFrameGray, RoundedCornerShape(26.dp))
                    .padding(vertical = 4.dp)
            ) {
                VerticalEqSlider(
                    sliderKey = "graphic_preamp",
                    value = preAmpDb,
                    valueRange = -15f..15f,
                    step = 0.5f,
                    tapStep = 1.0f,
                    thumbFillColor = PillDarkGray,
                    onValueChange = onPreAmpChange,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LabelPill(
                text = "PreAmp",
                width = 66.dp,
                height = 38.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LabelPill(
                text = "db ${formatDb(preAmpDb)}",
                width = 66.dp,
                height = 38.dp,
                onClick = { onPreAmpChange(0f) }
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Orta İnce Dikey Çizgi
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(ThinFrameGray)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // FREKANS ÇUBUKLARI (Beyaz tutamaçlı, ferah yerleşim)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bands.forEach { band ->
                key(band.id, band.frequencyHz) {
                    Column(
                        modifier = Modifier
                            .width(44.dp)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VerticalEqSlider(
                            sliderKey = "graphic_band_${band.id}_${band.frequencyHz}",
                            value = band.db,
                            valueRange = -15f..15f,
                            step = 0.5f,
                            tapStep = 1.0f,
                            thumbFillColor = BgBlack,
                            onValueChange = { newDb -> onBandChange(band.id, newDb) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LabelPill(
                            text = band.label,
                            width = 42.dp,
                            height = 38.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LabelPill(
                            text = formatDb(band.db),
                            width = 42.dp,
                            height = 38.dp,
                            onClick = { onBandChange(band.id, 0f) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Sağ İnce Dikey Çizgi
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(ThinFrameGray)
        )
    }
}

// ============================================================================
// 3. YENİ PARAMETRİK EQUALIZER BÖLÜMÜ (1731.png & 1733.png Tasarımı)
//    - Sütunlardaki çubukların iç dolgusu sütun rengiyle BİREBİR aynıdır, çerçevesi beyazdır
//    - Altta 2x2 kapsül düzeni: [db 0] [Q 1.40] / [hz 31] [Kalem (Edit) veya Seçim Tik (✓)]
//    - Düzenleme modunda her sütunun sol üst köşesinde kendi yuvarlak Renk Paleti butonu bulunur
// ============================================================================
@Composable
fun ParametricEqualizerSection(
    strings: AppStrings,
    preAmpDb: Float,
    onPreAmpChange: (Float) -> Unit,
    columns: List<ParametricColumn>,
    isSelectionMode: Boolean,
    onEnterSelectionMode: (Int) -> Unit,
    onToggleSelectColumn: (Int) -> Unit,
    onOpenSingleColumnColorPicker: (Int) -> Unit,
    onUpdateColumnDb: (Int, Float) -> Unit,
    onUpdateColumnQ: (Int, Float) -> Unit,
    onAddColumn: () -> Unit,
    onEditHzClick: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Bottom
    ) {
        // Sol İnce Dikey Çizgi
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(ThinFrameGray)
        )

        if (!isSelectionMode) {
            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier
                    .width(68.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(52.dp)
                        .border(1.2.dp, ThinFrameGray, RoundedCornerShape(26.dp))
                        .padding(vertical = 4.dp)
                ) {
                    VerticalEqSlider(
                        sliderKey = "parametric_preamp",
                        value = preAmpDb,
                        valueRange = -15f..15f,
                        step = 0.5f,
                        tapStep = 1.0f,
                        thumbFillColor = PillDarkGray,
                        onValueChange = onPreAmpChange,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LabelPill(
                    text = "PreAmp",
                    width = 66.dp,
                    height = 38.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LabelPill(
                    text = "db ${formatDb(preAmpDb)}",
                    width = 66.dp,
                    height = 38.dp,
                    onClick = { onPreAmpChange(0f) }
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(ThinFrameGray)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            columns.forEach { col ->
                key(col.id) {
                    val colBgColor = col.highlightColor
                    // Çubukların iç rengi sütunun kendi rengiyle aynı olur (şeffafsa siyah)
                    val sliderThumbFill = if (colBgColor == Color.Transparent) BgBlack else colBgColor

                    Box(
                        modifier = Modifier
                            .width(156.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(colBgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isSelectionMode) onToggleSelectColumn(col.id)
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // ÇUBUKLAR ALANI
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Sol Çubuk: dB
                                    VerticalEqSlider(
                                        sliderKey = "param_db_${col.id}",
                                        value = col.db,
                                        valueRange = -15f..15f,
                                        step = 0.5f,
                                        tapStep = 1.0f,
                                        thumbFillColor = sliderThumbFill,
                                        onValueChange = { newDb ->
                                            onUpdateColumnDb(col.id, newDb)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Sağ Çubuk: Q
                                    VerticalEqSlider(
                                        sliderKey = "param_q_${col.id}",
                                        value = col.q,
                                        valueRange = 0.20f..8.00f,
                                        step = 0.05f,
                                        tapStep = 0.10f,
                                        thumbFillColor = sliderThumbFill,
                                        onValueChange = { newQ ->
                                            onUpdateColumnQ(col.id, newQ)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }

                                // 1733.png: DÜZENLEME MODUNDA SOL ÜSTTE HER SÜTUNUN KENDİ RENK PALETİ YUVARLAĞI
                                if (isSelectionMode) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (colBgColor == Color.Transparent) BgBlack else colBgColor)
                                            .border(1.6.dp, Color.White, CircleShape)
                                            .clickable { onOpenSingleColumnColorPicker(col.id) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // ALT 2x2 KAPSÜL IZGARASI (1731.png & 1733.png)
                            // Üst Satır: [db 0]   [Q 1.40]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LabelPill(
                                    text = "db ${formatDb(col.db)}",
                                    width = 64.dp,
                                    height = 38.dp,
                                    onClick = {
                                        if (isSelectionMode) onToggleSelectColumn(col.id)
                                        else onUpdateColumnDb(col.id, 0f)
                                    }
                                )
                                LabelPill(
                                    text = "Q ${String.format(Locale.US, "%.2f", col.q)}",
                                    width = 64.dp,
                                    height = 38.dp,
                                    onClick = {
                                        if (isSelectionMode) onToggleSelectColumn(col.id)
                                        else onUpdateColumnQ(col.id, 1.40f)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Alt Satır: [hz 31]   [Kalem (Edit) Butonu (1731.png) VEYA Seçim Tik (✓) Kutusu (1733.png)]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LabelPill(
                                    text = "hz ${col.hz}",
                                    width = 64.dp,
                                    height = 38.dp,
                                    onClick = {
                                        if (isSelectionMode) onToggleSelectColumn(col.id)
                                        else onEditHzClick(col.id)
                                    }
                                )

                                if (!isSelectionMode) {
                                    // 1731.png: KALEM (EDIT) BUTONU -> Tıklanınca düzenleme menüsüne geçer
                                    Box(
                                        modifier = Modifier
                                            .width(64.dp)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(19.dp))
                                            .background(PillDarkGray)
                                            .clickable { onEnterSelectionMode(col.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        EditPenIcon()
                                    }
                                } else {
                                    // 1733.png: DÜZENLEME MODUNDA SAĞ ALT SEÇİM KAPSÜLÜ (Beyaz Çerçeveli + Tik ✓)
                                    Box(
                                        modifier = Modifier
                                            .width(64.dp)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(19.dp))
                                            .background( Color.Transparent )
                                            .border(1.6.dp, Color.White, RoundedCornerShape(19.dp))
                                            .clickable { onToggleSelectColumn(col.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (col.isSelected) {
                                            Canvas(modifier = Modifier.size(18.dp)) {
                                                val path = Path().apply {
                                                    moveTo(size.width * 0.15f, size.height * 0.55f)
                                                    lineTo(size.width * 0.44f, size.height * 0.85f)
                                                    lineTo(size.width * 0.88f, size.height * 0.15f)
                                                }
                                                drawPath(
                                                    path = path,
                                                    color = Color.White,
                                                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isSelectionMode) {
                Column(
                    modifier = Modifier
                        .width(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onAddColumn() }
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Canvas(modifier = Modifier.size(60.dp)) {
                        drawCircle(
                            color = Color.White,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        val pad = 14.dp.toPx()
                        drawLine(
                            color = Color.White,
                            start = Offset(size.width / 2f, pad),
                            end = Offset(size.width / 2f, size.height - pad),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(pad, size.height / 2f),
                            end = Offset(size.width - pad, size.height / 2f),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = strings.addNewColumn,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Sağ İnce Dikey Çizgi
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(ThinFrameGray)
        )
    }
}

// 1731.png'deki Kalem (Edit) İkonu
@Composable
fun EditPenIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        // Kalem gövdesi (diyagonal altın/sarı ve gri/mavi uç)
        drawLine(
            color = Color(0xFFD4AF37),
            start = Offset(w * 0.78f, h * 0.22f),
            end = Offset(w * 0.32f, h * 0.68f),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF80CBC4),
            start = Offset(w * 0.30f, h * 0.70f),
            end = Offset(w * 0.16f, h * 0.84f),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

// ============================================================================
// 4. TAM İZOLE DİKEY EŞİTLEME ÇUBUĞU (1730, 1731, 1733: Beyaz Çerçeveli Tutamaç, Sütun Rengi Dolgulu)
// ============================================================================
@Composable
fun VerticalEqSlider(
    sliderKey: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float = 0.5f,
    tapStep: Float = 1.0f,
    thumbFillColor: Color = BgBlack,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var heightPx by remember(sliderKey) { mutableFloatStateOf(1f) }
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val thumbHeight = 56.dp
    val thumbWidth = 22.dp
    val density = LocalDensity.current
    val thumbHalfHeightPx = with(density) { (thumbHeight / 2).toPx() }

    fun yOffsetToValue(yPx: Float): Float {
        val usableHeight = (heightPx - 2f * thumbHalfHeightPx).coerceAtLeast(1f)
        val norm = 1f - ((yPx - thumbHalfHeightPx) / usableHeight).coerceIn(0f, 1f)
        val raw = valueRange.start + norm * (valueRange.endInclusive - valueRange.start)
        val invStep = (1f / step).roundToInt().coerceAtLeast(1)
        val stepped = (raw * invStep).roundToInt() / invStep.toFloat()
        return stepped.coerceIn(valueRange.start, valueRange.endInclusive)
    }

    Box(
        modifier = modifier
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(sliderKey, valueRange, step, tapStep) {
                detectTapGestures(
                    onTap = { offset ->
                        val delta = if (offset.y < heightPx / 2f) tapStep else -tapStep
                        val invStep = (1f / step).roundToInt().coerceAtLeast(1)
                        val updated = ((currentValue + delta) * invStep).roundToInt() / invStep.toFloat()
                        currentOnValueChange(updated.coerceIn(valueRange.start, valueRange.endInclusive))
                    }
                )
            }
            .pointerInput(sliderKey, valueRange, step) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        currentOnValueChange(yOffsetToValue(change.position.y))
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val topPadding = 8.dp.toPx()
            val bottomPadding = size.height - 8.dp.toPx()

            // Dikey Beyaz Çizgi
            drawLine(
                color = Color.White,
                start = Offset(centerX, topPadding),
                end = Offset(centerX, bottomPadding),
                strokeWidth = 3.2.dp.toPx(),
                cap = StrokeCap.Round
            )

            val norm = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            val usableH = (size.height - thumbHeight.toPx()).coerceAtLeast(1f)
            val thumbCenterY = (1f - norm) * usableH + thumbHeight.toPx() / 2f

            val tW = thumbWidth.toPx()
            val tH = thumbHeight.toPx()

            // 1. Tutamaç İç Dolgusu (Sütun rengiyle aynı, böylece arkadaki dikey çizgi içinden geçmez)
            drawRoundRect(
                color = thumbFillColor,
                topLeft = Offset(centerX - tW / 2f, thumbCenterY - tH / 2f),
                size = Size(tW, tH),
                cornerRadius = CornerRadius(tW / 2f, tW / 2f)
            )

            // 2. Tutamaç Beyaz Dış Çerçevesi (1730.png, 1731.png, 1733.png)
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(centerX - tW / 2f, thumbCenterY - tH / 2f),
                size = Size(tW, tH),
                cornerRadius = CornerRadius(tW / 2f, tW / 2f),
                style = Stroke(width = 3.2.dp.toPx())
            )

            // 3. Tutamaç Ortasındaki Beyaz Yatay Çentik
            val innerW = tW * 0.72f
            val innerH = 8.dp.toPx()
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(centerX - innerW / 2f, thumbCenterY - innerH / 2f),
                size = Size(innerW, innerH),
                cornerRadius = CornerRadius(innerH / 2f, innerH / 2f)
            )
        }
    }
}

// ============================================================================
// 5. ÇİFT KATMANLI 24 EŞİT ÇUBUKLU FREKANS SES BARI (1730.png & 1731.png)
// ============================================================================
private fun computeEqGainAtFrequency(
    targetHz: Float,
    eqMode: EqMode,
    graphicBands: List<GraphicBand>,
    parametricColumns: List<ParametricColumn>,
    preAmpDb: Float
): Float {
    if (eqMode == EqMode.GRAPHIC) {
        if (graphicBands.isEmpty()) return preAmpDb
        val sorted = graphicBands.sortedBy { it.frequencyHz }
        if (targetHz <= sorted.first().frequencyHz) return preAmpDb + sorted.first().db
        if (targetHz >= sorted.last().frequencyHz) return preAmpDb + sorted.last().db
        for (i in 0 until sorted.lastIndex) {
            val left = sorted[i]
            val right = sorted[i + 1]
            if (targetHz in left.frequencyHz.toFloat()..right.frequencyHz.toFloat()) {
                val logT = ln(targetHz)
                val logL = ln(left.frequencyHz.toFloat())
                val logR = ln(right.frequencyHz.toFloat())
                val ratio = ((logT - logL) / (logR - logL).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
                return preAmpDb + (left.db + (right.db - left.db) * ratio)
            }
        }
        return preAmpDb
    } else {
        var totalGain = preAmpDb
        val logTarget = ln(targetHz.coerceAtLeast(20f))
        parametricColumns.forEach { col ->
            val logCenter = ln(col.hz.toFloat().coerceAtLeast(20f))
            val octaves = abs(logTarget - logCenter) / ln(2f)
            val bandwidthOctaves = (1.0f / col.q.coerceAtLeast(0.2f)).coerceAtLeast(0.15f)
            val influence = exp(-(octaves * octaves) / (2f * bandwidthOctaves * bandwidthOctaves))
            totalGain += col.db * influence
        }
        return totalGain.coerceIn(-20f, 20f)
    }
}

@Composable
fun AudioVisualizerBar(
    isMusicPlaying: Boolean,
    eqMode: EqMode,
    graphicBands: List<GraphicBand>,
    parametricColumns: List<ParametricColumn>,
    preAmpDb: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "freq_visualizer")
    val timePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "freq_phase"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ContainerGray),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val midY = size.height / 2f

            // Boydan boya yatay beyaz merkez çizgisi
            drawLine(
                color = Color.White,
                start = Offset(0f, midY),
                end = Offset(size.width, midY),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Butt
            )

            if (!isMusicPlaying) {
                return@Canvas
            }

            val tickCount = VisualizerBarFrequenciesHz.size // 24
            val sideMargin = 10.dp.toPx()
            val usableWidth = (size.width - 2f * sideMargin).coerceAtLeast(1f)

            val gapToWidthRatio = 16f / 25.5f
            val tickW = usableWidth / (tickCount + (tickCount - 1) * gapToWidthRatio)
            val gapW = tickW * gapToWidthRatio

            val minTickH = 12.dp.toPx()
            val maxTickH = 38.dp.toPx()

            for (idx in 0 until tickCount) {
                val freqHz = VisualizerBarFrequenciesHz[idx]
                val leftX = sideMargin + idx * (tickW + gapW)

                val eqGainDb = computeEqGainAtFrequency(
                    targetHz = freqHz,
                    eqMode = eqMode,
                    graphicBands = graphicBands,
                    parametricColumns = parametricColumns,
                    preAmpDb = preAmpDb
                )
                val eqMultiplier = ((eqGainDb + 18f) / 28f).coerceIn(0.22f, 1.25f)

                val rawResponse = when {
                    freqHz <= 125f -> {
                        val kick = abs(sin(timePhase * 1.0f + idx * 0.40f))
                        val sub = abs(cos(timePhase * 2.0f - idx * 0.30f))
                        kick * 0.68f + sub * 0.32f
                    }
                    freqHz <= 2000f -> {
                        val vocal = abs(sin(timePhase * 2.0f + idx * 0.80f))
                        val harm = abs(cos(timePhase * 3.0f - idx * 0.50f))
                        vocal * 0.62f + harm * 0.38f
                    }
                    else -> {
                        val treble = abs(sin(timePhase * 3.0f + idx * 1.05f))
                        val shimmer = abs(cos(timePhase * 4.0f + idx * 0.65f))
                        val naturalRollOff = if (freqHz > 14000f) 0.78f else 0.90f
                        (treble * 0.58f + shimmer * 0.42f) * naturalRollOff
                    }
                }

                val amplitude = ((0.20f + rawResponse * 0.80f) * eqMultiplier).coerceIn(0f, 1f)
                val currentTickH = minTickH + (maxTickH - minTickH) * amplitude
                val outerShadowH = (currentTickH + 9.dp.toPx()).coerceAtMost(size.height - 6.dp.toPx())

                // 1730.png & 1731.png: Dış gri gölge kapsül + İç parlak beyaz kapsül
                drawRoundRect(
                    color = Color(0xFF787878),
                    topLeft = Offset(leftX, midY - outerShadowH / 2f),
                    size = Size(tickW, outerShadowH),
                    cornerRadius = CornerRadius(tickW / 2f, tickW / 2f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(leftX, midY - currentTickH / 2f),
                    size = Size(tickW, currentTickH),
                    cornerRadius = CornerRadius(tickW / 2f, tickW / 2f)
                )
            }
        }
    }
}

// ============================================================================
// 6. ALT KONTROL PANELİ (1730.png & 1731.png Düzeni)
// ============================================================================
@Composable
fun BottomControlPanel(
    strings: AppStrings,
    isLimiterEnabled: Boolean,
    onToggleLimiter: () -> Unit,
    isEqEnabled: Boolean,
    onToggleEq: () -> Unit,
    isAutoEqSelected: Boolean,
    onAutoEqClick: () -> Unit,
    audioOutputLabel: String,
    onAudioOutputClick: () -> Unit,
    activeProfileName: String,
    onProfileClick: () -> Unit,
    onQuickMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    eqMode: EqMode,
    onToggleEqMode: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gear_spin")
    val gearRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gear_rotation_angle"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Sol Sütun: limiter, on/off, AutoEq
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            ControlPillButton(
                text = "limiter",
                width = 76.dp,
                height = 44.dp,
                borderColor = if (isLimiterEnabled) AccentGreen else AccentRed,
                onClick = onToggleLimiter
            )
            ControlPillButton(
                text = "on/off",
                width = 76.dp,
                height = 44.dp,
                borderColor = if (isEqEnabled) AccentGreen else AccentRed,
                onClick = onToggleEq
            )
            ControlPillButton(
                text = "AutoEq",
                width = 76.dp,
                height = 44.dp,
                borderColor = if (isAutoEqSelected) AccentGreen else Color.Transparent,
                onClick = onAutoEqClick
            )
        }

        // Orta Sütun: Aktif Ses Çıkışı ve Kayıtlı Profil
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ControlPillButton(
                text = audioOutputLabel,
                width = 148.dp,
                height = 50.dp,
                borderColor = Color.Transparent,
                fontSize = if (audioOutputLabel.length > 12) 12 else 14,
                onClick = onAudioOutputClick
            )
            ControlPillButton(
                text = activeProfileName,
                width = 148.dp,
                height = 50.dp,
                borderColor = Color.Transparent,
                onClick = onProfileClick
            )
        }

        // Sağ Sütun: Üstte Yan Yana [≡] ve [⚙] (1730.png & 1731.png), Altta [Grafik / Parametric]
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.width(96.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PillDarkGray)
                        .clickable { onQuickMenuClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        val stroke = 2.dp.toPx()
                        drawLine(Color.White, Offset(0f, size.height * 0.2f), Offset(size.width, size.height * 0.2f), stroke, StrokeCap.Round)
                        drawLine(Color.White, Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), stroke, StrokeCap.Round)
                        drawLine(Color.White, Offset(0f, size.height * 0.8f), Offset(size.width, size.height * 0.8f), stroke, StrokeCap.Round)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PillDarkGray)
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        rotate(degrees = gearRotation) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val rOuter = size.minDimension / 2f
                            val rInner = rOuter * 0.42f
                            drawCircle(Color.White, radius = rOuter * 0.72f, center = center, style = Stroke(width = 1.6.dp.toPx()))
                            drawCircle(Color.White, radius = rInner, center = center, style = Stroke(width = 1.4.dp.toPx()))
                            for (angle in 0 until 360 step 45) {
                                val rad = Math.toRadians(angle.toDouble())
                                val x1 = center.x + (rOuter * 0.65f) * cos(rad).toFloat()
                                val y1 = center.y + (rOuter * 0.65f) * sin(rad).toFloat()
                                val x2 = center.x + rOuter * cos(rad).toFloat()
                                val y2 = center.y + rOuter * sin(rad).toFloat()
                                drawLine(Color.White, Offset(x1, y1), Offset(x2, y2), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                            }
                        }
                    }
                }
            }

            ControlPillButton(
                text = if (eqMode == EqMode.GRAPHIC) strings.graphic else strings.parametric,
                width = 96.dp,
                height = 46.dp,
                borderColor = Color.Transparent,
                fontSize = if (eqMode == EqMode.GRAPHIC) 14 else 12,
                onClick = onToggleEqMode
            )
        }
    }
}

// ============================================================================
// 7. AÇILIR MENÜLER VE MODALLAR
// ============================================================================

@Composable
fun QuickAccessMenuCard(
    strings: AppStrings,
    autoSaveEnabled: Boolean,
    onRename: () -> Unit,
    onSaveAndAssign: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onLoadDefault: () -> Unit,
    onToggleAutoSave: () -> Unit
) {
    val standardItems = listOf(
        strings.rename to onRename,
        strings.saveAndAssign to onSaveAndAssign,
        strings.importProfile to onImport,
        strings.exportProfile to onExport,
        strings.loadDefault to onLoadDefault
    )

    Column(
        modifier = Modifier
            .width(204.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PopupBgGray.copy(alpha = 0.96f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        standardItems.forEach { (title, action) ->
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { action() }
                    .padding(vertical = 8.dp)
            )
            HorizontalDivider(color = DividerLightGray, thickness = 1.dp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleAutoSave() }
                .padding(top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.autoSave,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            MiniCapsuleSwitch(checked = autoSaveEnabled)
        }
    }
}

@Composable
private fun MiniCapsuleSwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) AccentGreen else AccentRed)
            .padding(2.5.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(15.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// PARAMETRİK EQ RENK SEÇME MENÜSÜ
@Composable
fun ColorPickerModal(
    strings: AppStrings,
    initialColor: Color,
    onApplyColor: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember {
        mutableStateOf(if (initialColor == Color.Transparent) SelectionTeal else initialColor)
    }
    var customHue by remember { mutableFloatStateOf(188f) }
    var customValue by remember { mutableFloatStateOf(0.75f) }

    val rainbowColors = remember {
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFFFF00),
            Color(0xFF00FF00),
            Color(0xFF00FFFF),
            Color(0xFF0000FF),
            Color(0xFFFF00FF),
            Color(0xFFFF0000)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(PopupBgGray)
                .padding(bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PopupHeaderGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.colorPickerTitle,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = strings.presetColorsLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PresetColorProfiles.chunked(5).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowColors.forEach { presetColor ->
                                val isChosen = selectedColor == presetColor
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (presetColor == Color.Transparent) PillDarkGray else presetColor)
                                        .border(
                                            width = if (isChosen) 2.5.dp else 1.2.dp,
                                            color = if (isChosen) AccentGreen else Color.White.copy(alpha = 0.45f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = presetColor },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (presetColor == Color.Transparent) {
                                        Text("✕", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = DividerLightGray, thickness = 1.dp)

                Text(
                    text = strings.customColorBaretLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                var hueBarWidth by remember { mutableFloatStateOf(1f) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(rainbowColors))
                        .border(1.5.dp, Color.White, RoundedCornerShape(16.dp))
                        .onSizeChanged { hueBarWidth = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                customHue = ((offset.x / hueBarWidth).coerceIn(0f, 1f)) * 360f
                                selectedColor = Color.hsv(customHue, 0.85f, customValue)
                            }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                customHue = ((change.position.x / hueBarWidth).coerceIn(0f, 1f)) * 360f
                                selectedColor = Color.hsv(customHue, 0.85f, customValue)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val indicatorX = (customHue / 360f).coerceIn(0f, 1f) * size.width
                        drawCircle(
                            color = Color.White,
                            radius = 11.dp.toPx(),
                            center = Offset(indicatorX, size.height / 2f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                Text(
                    text = strings.brightnessBaretLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                var valBarWidth by remember { mutableFloatStateOf(1f) }
                val pureHueColor = Color.hsv(customHue, 0.85f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF141414), pureHueColor)
                            )
                        )
                        .border(1.5.dp, Color.White, RoundedCornerShape(14.dp))
                        .onSizeChanged { valBarWidth = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                customValue = 0.2f + ((offset.x / valBarWidth).coerceIn(0f, 1f)) * 0.8f
                                selectedColor = Color.hsv(customHue, 0.85f, customValue)
                            }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                customValue = 0.2f + ((change.position.x / valBarWidth).coerceIn(0f, 1f)) * 0.8f
                                selectedColor = Color.hsv(customHue, 0.85f, customValue)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val normVal = ((customValue - 0.2f) / 0.8f).coerceIn(0f, 1f)
                        val indicatorX = normVal * size.width
                        drawCircle(
                            color = Color.White,
                            radius = 9.dp.toPx(),
                            center = Offset(indicatorX, size.height / 2f),
                            style = Stroke(width = 2.6.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedColor == Color.Transparent) PillDarkGray else selectedColor)
                                .border(1.5.dp, Color.White, RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (selectedColor == Color.Transparent) strings.clearColor else strings.color,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(PillDarkGray)
                                .clickable { onDismiss() }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(strings.cancel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(PillDarkGray)
                                .border(1.8.dp, AccentGreen, RoundedCornerShape(18.dp))
                                .clickable { onApplyColor(selectedColor) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.apply,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// KAYDET & ATA AĞAÇ MENÜSÜ
@Composable
fun SaveAndAssignTreeModal(
    strings: AppStrings,
    currentActiveProfileName: String,
    currentEqMode: EqMode,
    connectedTree: ConnectedAudioDevicesTree,
    defaultSelectedDevice: String,
    onConfirm: (String, EqMode, String) -> Unit,
    onDismiss: () -> Unit
) {
    var profileName by remember {
        mutableStateOf(
            if (currentActiveProfileName == "Varsayılan" || currentActiveProfileName == "Varsayılan (Parametric)" || currentActiveProfileName == "Default") ""
            else currentActiveProfileName
        )
    }
    var selectedEqMode by remember { mutableStateOf(currentEqMode) }
    var selectedTarget by remember { mutableStateOf(defaultSelectedDevice) }

    var btExpanded by remember { mutableStateOf(true) }
    var wiredExpanded by remember { mutableStateOf(true) }
    var dacExpanded by remember { mutableStateOf(connectedTree.usbDacDevices.isNotEmpty()) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(PopupBgGray)
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PopupHeaderGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.saveAssignTitle,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.profileNameLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedPillInput(
                    value = profileName,
                    onValueChange = { profileName = it },
                    placeholder = strings.profileNamePlaceholder,
                    isPassword = false
                )

                Text(
                    text = strings.profileTypeLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(EqMode.GRAPHIC to strings.graphic, EqMode.PARAMETRIC to strings.parametric).forEach { (mode, label) ->
                        val isChosenMode = selectedEqMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(21.dp))
                                .background(PillDarkGray)
                                .then(
                                    if (isChosenMode) {
                                        Modifier.border(1.8.dp, AccentGreen, RoundedCornerShape(21.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedEqMode = mode },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = strings.assignDeviceTreeLabel,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ContainerGray)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TreeBranchHeader(
                        title = "${strings.outputBluetooth} (${connectedTree.bluetoothDevices.size})",
                        isExpanded = btExpanded,
                        isSelected = selectedTarget == strings.outputBluetooth,
                        onToggleExpand = { btExpanded = !btExpanded },
                        onSelectCategory = { selectedTarget = strings.outputBluetooth }
                    )
                    if (btExpanded) {
                        if (connectedTree.bluetoothDevices.isEmpty()) {
                            TreeLeafRow(
                                label = strings.noDeviceConnectedInBranch,
                                isLast = true,
                                isSelected = false,
                                isEnabled = false,
                                onClick = {}
                            )
                        } else {
                            connectedTree.bluetoothDevices.forEachIndexed { idx, btName ->
                                TreeLeafRow(
                                    label = btName,
                                    isLast = idx == connectedTree.bluetoothDevices.lastIndex,
                                    isSelected = selectedTarget == btName,
                                    isEnabled = true,
                                    onClick = { selectedTarget = btName }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DividerLightGray.copy(alpha = 0.35f), thickness = 1.dp)

                    TreeBranchHeader(
                        title = "${strings.outputWired} (${connectedTree.wiredDevices.size})",
                        isExpanded = wiredExpanded,
                        isSelected = selectedTarget == strings.outputWired,
                        onToggleExpand = { wiredExpanded = !wiredExpanded },
                        onSelectCategory = { selectedTarget = strings.outputWired }
                    )
                    if (wiredExpanded) {
                        if (connectedTree.wiredDevices.isEmpty()) {
                            TreeLeafRow(
                                label = strings.noDeviceConnectedInBranch,
                                isLast = true,
                                isSelected = false,
                                isEnabled = false,
                                onClick = {}
                            )
                        } else {
                            connectedTree.wiredDevices.forEachIndexed { idx, wiredName ->
                                TreeLeafRow(
                                    label = wiredName,
                                    isLast = idx == connectedTree.wiredDevices.lastIndex,
                                    isSelected = selectedTarget == wiredName,
                                    isEnabled = true,
                                    onClick = { selectedTarget = wiredName }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DividerLightGray.copy(alpha = 0.35f), thickness = 1.dp)

                    TreeBranchHeader(
                        title = "${strings.outputUsbDac} (${connectedTree.usbDacDevices.size})",
                        isExpanded = dacExpanded,
                        isSelected = selectedTarget == strings.outputUsbDac,
                        onToggleExpand = { dacExpanded = !dacExpanded },
                        onSelectCategory = { selectedTarget = strings.outputUsbDac }
                    )
                    if (dacExpanded) {
                        if (connectedTree.usbDacDevices.isEmpty()) {
                            TreeLeafRow(
                                label = strings.noDeviceConnectedInBranch,
                                isLast = true,
                                isSelected = false,
                                isEnabled = false,
                                onClick = {}
                            )
                        } else {
                            connectedTree.usbDacDevices.forEachIndexed { idx, dacName ->
                                TreeLeafRow(
                                    label = dacName,
                                    isLast = idx == connectedTree.usbDacDevices.lastIndex,
                                    isSelected = selectedTarget == dacName,
                                    isEnabled = true,
                                    onClick = { selectedTarget = dacName }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DividerLightGray.copy(alpha = 0.35f), thickness = 1.dp)

                    TreeSingleSelectableRow(
                        title = strings.outputSpeaker,
                        isSelected = selectedTarget == strings.outputSpeaker,
                        onClick = { selectedTarget = strings.outputSpeaker }
                    )

                    HorizontalDivider(color = DividerLightGray.copy(alpha = 0.35f), thickness = 1.dp)

                    TreeSingleSelectableRow(
                        title = strings.assignNone,
                        isSelected = selectedTarget == strings.assignNone,
                        onClick = { selectedTarget = strings.assignNone }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(PillDarkGray)
                            .clickable { onDismiss() }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(strings.cancel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(PillDarkGray)
                            .border(1.8.dp, AccentGreen, RoundedCornerShape(18.dp))
                            .clickable { onConfirm(profileName, selectedEqMode, selectedTarget) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.saveAndAssign,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeBranchHeader(
    title: String,
    isExpanded: Boolean,
    isSelected: Boolean,
    onToggleExpand: () -> Unit,
    onSelectCategory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(PillDarkGray)
            .then(
                if (isSelected) {
                    Modifier.border(1.8.dp, AccentGreen, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onSelectCategory() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clickable { onToggleExpand() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isExpanded) "▾" else "▸",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TreeLeafRow(
    label: String,
    isLast: Boolean,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(start = 18.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(PillDarkGray)
            .then(
                if (isSelected) {
                    Modifier.border(1.8.dp, AccentGreen, RoundedCornerShape(19.dp))
                } else {
                    Modifier
                }
            )
            .then(if (isEnabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isLast) "└─ " else "├─ ",
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TreeSingleSelectableRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(PillDarkGray)
            .then(
                if (isSelected) {
                    Modifier.border(1.8.dp, AccentGreen, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AutoEqSearchModal(
    strings: AppStrings,
    presets: List<AutoEqPreset>,
    selectedPresetName: String?,
    onSelectPreset: (AutoEqPreset) -> Unit,
    onClearPreset: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, presets) {
        if (query.isBlank()) presets
        else presets.filter { it.modelName.contains(query, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 340.dp, max = 440.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(PopupBgGray)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PopupHeaderGray)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = strings.searchAutoEq,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Canvas(modifier = Modifier.size(48.dp)) {
                    drawCircle(
                        color = Color.White,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                    val glassCenter = Offset(size.width * 0.44f, size.height * 0.42f)
                    val glassRadius = size.minDimension * 0.22f
                    drawCircle(
                        color = Color.White,
                        radius = glassRadius,
                        center = glassCenter,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(glassCenter.x + glassRadius * 0.7f, glassCenter.y + glassRadius * 0.7f),
                        end = Offset(size.width * 0.74f, size.height * 0.74f),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedPresetName != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(PillDarkGray)
                            .border(1.8.dp, AccentRed, RoundedCornerShape(20.dp))
                            .clickable { onClearPreset() }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "✕ ${strings.clearAutoEq} ($selectedPresetName)",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HorizontalDivider(color = DividerLightGray, thickness = 1.dp)
                }

                filtered.forEach { preset ->
                    val isCurrent = preset.modelName == selectedPresetName
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .then(
                                if (isCurrent) {
                                    Modifier
                                        .background(PillDarkGray)
                                        .border(1.8.dp, AccentGreen, RoundedCornerShape(18.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                } else {
                                    Modifier.padding(vertical = 4.dp)
                                }
                            )
                            .clickable { onSelectPreset(preset) },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = preset.modelName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SavedProfilesModal(
    strings: AppStrings,
    profiles: List<EqProfile>,
    activeProfileName: String,
    onSelectProfile: (EqProfile) -> Unit,
    onDeleteProfile: (EqProfile) -> Unit,
    onAddNewProfileClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val graphicProfiles = remember(profiles) { profiles.filter { it.eqMode == EqMode.GRAPHIC } }
    val parametricProfiles = remember(profiles) { profiles.filter { it.eqMode == EqMode.PARAMETRIC } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 470.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(PopupBgGray)
                .padding(bottom = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PopupHeaderGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.profilesTitle,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (graphicProfiles.isNotEmpty()) {
                    Text(
                        text = strings.graphicProfilesSection,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                    )
                    graphicProfiles.forEach { profile ->
                        ProfileRowCard(
                            strings = strings,
                            profile = profile,
                            isSelected = profile.name == activeProfileName,
                            onSelect = { onSelectProfile(profile) },
                            onDelete = { onDeleteProfile(profile) }
                        )
                    }
                }

                if (parametricProfiles.isNotEmpty()) {
                    if (graphicProfiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = DividerLightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = strings.parametricProfilesSection,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                    )
                    parametricProfiles.forEach { profile ->
                        ProfileRowCard(
                            strings = strings,
                            profile = profile,
                            isSelected = profile.name == activeProfileName,
                            onSelect = { onSelectProfile(profile) },
                            onDelete = { onDeleteProfile(profile) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(PillDarkGray)
                        .border(1.8.dp, AccentGreen, RoundedCornerShape(22.dp))
                        .clickable { onAddNewProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.addNewProfileBtn,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRowCard(
    strings: AppStrings,
    profile: EqProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val displayName = when {
        profile.isDefault && strings.isEnglish -> "Default"
        profile.name == "Varsayılan (Parametric)" -> "Varsayılan"
        else -> profile.name
    }
    val modeTag = if (profile.eqMode == EqMode.GRAPHIC) strings.graphic else strings.parametric
    val subtitle = if (!profile.assignedDevice.isNullOrBlank()) {
        "$modeTag • ${profile.assignedDevice}"
    } else {
        modeTag
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PillDarkGray)
            .then(
                if (isSelected) {
                    Modifier.border(1.8.dp, AccentGreen, RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            )
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                color = Color.White,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (!profile.isDefault) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                TrashCanIcon(color = Color.White)
            }
        }
    }
}

@Composable
fun TrashCanIcon(color: Color = Color.White) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.8.dp.toPx()
        val w = size.width
        val h = size.height

        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.22f),
            end = Offset(w * 0.85f, h * 0.22f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.38f, h * 0.12f),
            end = Offset(w * 0.62f, h * 0.12f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.24f, h * 0.26f),
            size = Size(w * 0.52f, h * 0.64f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.42f, h * 0.40f),
            end = Offset(w * 0.42f, h * 0.76f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.58f, h * 0.40f),
            end = Offset(w * 0.58f, h * 0.76f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

// 1733.png: TOPLU DÜZENLEME ALT ÇUBUĞU (sil | ● Renk | ☐ Hepsi)
@Composable
fun ParametricSelectionBottomBar(
    strings: AppStrings,
    allSelected: Boolean,
    selectedColor: Color,
    onDeleteSelected: () -> Unit,
    onOpenBulkColorPicker: () -> Unit,
    onToggleSelectAll: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ActionBottomBarGray)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = strings.delete,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { onDeleteSelected() }
                .padding(8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onOpenBulkColorPicker() }
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                    .border(2.dp, Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.color,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onToggleSelectAll(!allSelected) }
                .padding(8.dp)
        ) {
            CustomCheckBox(
                checked = allSelected,
                onCheckedChange = { onToggleSelectAll(it) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.all,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================================
// 8. AYARLAR EKRANLARI
// ============================================================================

@Composable
fun SettingsMainScreen(
    strings: AppStrings,
    showLanguagePopup: Boolean,
    onSelectLanguage: (String) -> Unit,
    onToggleLanguagePopup: () -> Unit,
    onNavigate: (AppScreen) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsScreenHeader(title = strings.settingsTitle, onBack = onBack)
            Spacer(modifier = Modifier.height(22.dp))

            SettingsRowItem(title = strings.myAccount, onClick = { onNavigate(AppScreen.SETTINGS_ACCOUNT) })
            SettingsDivider()
            SettingsRowItem(title = strings.language, onClick = onToggleLanguagePopup)
            SettingsDivider()
            SettingsRowItem(title = strings.equalizer, onClick = { onNavigate(AppScreen.SETTINGS_EQUALIZER) })
            SettingsDivider()
            SettingsRowItem(title = strings.report, onClick = { onNavigate(AppScreen.SETTINGS_REPORT) })
        }

        if (showLanguagePopup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleLanguagePopup
                    )
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 135.dp, end = 8.dp)
                        .width(265.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PopupBgGray)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Türkçe" to strings.turkish, "İngilizce" to strings.english).forEach { (langKey, displayLabel) ->
                        val isCurrentLang = (langKey == "İngilizce" && strings.isEnglish) || (langKey == "Türkçe" && !strings.isEnglish)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(PillDarkGray)
                                .border(
                                    width = 1.8.dp,
                                    color = if (isCurrentLang) AccentGreen else Color.White,
                                    shape = RoundedCornerShape(23.dp)
                                )
                                .clickable { onSelectLanguage(langKey) }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = displayLabel,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAccountScreen(
    strings: AppStrings,
    isLoggedIn: Boolean,
    userName: String,
    userEmail: String,
    syncEnabled: Boolean,
    onToggleSync: (Boolean) -> Unit,
    onCreateAccountSuccess: (String, String) -> Unit,
    onLogOut: () -> Unit,
    onNavigatePasswordEmail: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var inputUsername by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }
    var inputCode by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var inputPasswordRepeat by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsScreenHeader(title = strings.myAccount, onBack = onBack)
        Spacer(modifier = Modifier.height(22.dp))

        if (!isLoggedIn) {
            SettingsPassiveRowItem(title = strings.loginOrRegisterHeader)
            SettingsDivider()
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PopupBgGray)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(PillDarkGray)
                        .border(1.5.dp, Color.Black, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = strings.loginOrRegisterHeader,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedPillInput(
                    value = inputUsername,
                    onValueChange = { inputUsername = it },
                    placeholder = strings.usernamePlaceholder,
                    isPassword = false,
                    height = 50.dp
                )

                OutlinedPillInput(
                    value = inputEmail,
                    onValueChange = { inputEmail = it },
                    placeholder = strings.emailPlaceholder,
                    isPassword = false,
                    height = 50.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1.18f)) {
                        OutlinedPillInput(
                            value = inputCode,
                            onValueChange = { inputCode = it },
                            placeholder = strings.verificationCode,
                            isPassword = false,
                            height = 50.dp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(0.82f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(PillDarkGray)
                            .border(1.8.dp, AccentGreen, RoundedCornerShape(25.dp))
                            .clickable {
                                Toast.makeText(context, strings.toastCodeRequested, Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = strings.requestCode,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(PillDarkGray)
                        .border(1.8.dp, AccentGreen, RoundedCornerShape(25.dp))
                        .clickable {
                            Toast.makeText(context, strings.toastCodeVerified, Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = strings.submit,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedPillInput(
                    value = inputPassword,
                    onValueChange = { inputPassword = it },
                    placeholder = strings.passwordSection,
                    isPassword = true,
                    height = 50.dp
                )

                OutlinedPillInput(
                    value = inputPasswordRepeat,
                    onValueChange = { inputPasswordRepeat = it },
                    placeholder = strings.passwordRepeatPlaceholder,
                    isPassword = true,
                    height = 50.dp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(PillDarkGray)
                        .border(1.8.dp, AccentGreen, RoundedCornerShape(25.dp))
                        .clickable {
                            onCreateAccountSuccess(inputUsername, inputEmail)
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = strings.createAccountBtn,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            val displayHeader = if (userName.isNotBlank()) "$userName ($userEmail)" else userEmail
            SettingsPassiveRowItem(title = displayHeader)
            SettingsDivider()
            SettingsToggleRowItem(
                title = strings.sync,
                checked = syncEnabled,
                activeTrackColor = AccentGreen,
                onCheckedChange = onToggleSync
            )
            SettingsDivider()
            SettingsRowItem(title = strings.changePassword, onClick = onNavigatePasswordEmail)
            SettingsDivider()
            SettingsRowItem(title = strings.changeEmail, onClick = onNavigatePasswordEmail)
            SettingsDivider()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PillDarkGray)
                    .border(1.8.dp, AccentRed, RoundedCornerShape(18.dp))
                    .clickable { onLogOut() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = strings.logOutBtn,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SettingsPasswordEmailScreen(
    strings: AppStrings,
    onUpdateEmail: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var newPassRepeat by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsScreenHeader(title = strings.changePasswordEmailTitle, onBack = onBack)
        Spacer(modifier = Modifier.height(22.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(PopupBgGray)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PillDarkGray)
                    .border(1.5.dp, Color.Black, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(strings.passwordSection, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedPillInput(
                value = currentPass,
                onValueChange = { currentPass = it },
                placeholder = strings.currentPassword,
                isPassword = true,
                height = 48.dp
            )
            OutlinedPillInput(
                value = newPass,
                onValueChange = { newPass = it },
                placeholder = strings.newPassword,
                isPassword = true,
                height = 48.dp
            )
            OutlinedPillInput(
                value = newPassRepeat,
                onValueChange = { newPassRepeat = it },
                placeholder = strings.repeatNewPassword,
                isPassword = true,
                height = 48.dp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(PillDarkGray)
                    .border(1.8.dp, AccentGreen, RoundedCornerShape(24.dp))
                    .clickable {
                        Toast.makeText(context, strings.toastPasswordSaved, Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = strings.savePasswordBtn,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(PopupBgGray)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PillDarkGray)
                    .border(1.5.dp, Color.Black, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(strings.emailSection, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedPillInput(
                value = newEmail,
                onValueChange = { newEmail = it },
                placeholder = strings.newEmail,
                isPassword = false,
                height = 48.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1.18f)) {
                    OutlinedPillInput(
                        value = verificationCode,
                        onValueChange = { verificationCode = it },
                        placeholder = strings.verificationCode,
                        isPassword = false,
                        height = 48.dp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(0.82f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(PillDarkGray)
                        .border(1.8.dp, AccentGreen, RoundedCornerShape(24.dp))
                        .clickable {
                            Toast.makeText(context, strings.toastCodeRequested, Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = strings.requestCode,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(PillDarkGray)
                    .border(1.8.dp, AccentGreen, RoundedCornerShape(24.dp))
                    .clickable {
                        if (newEmail.contains("@")) {
                            onUpdateEmail(newEmail)
                        }
                        Toast.makeText(context, strings.toastAccountUpdated, Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(strings.submit, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsEqualizerScreen(
    strings: AppStrings,
    audioProcessingEnabled: Boolean,
    onToggleAudioProcessing: (Boolean) -> Unit,
    autoPreAmpEnabled: Boolean,
    onToggleAutoPreAmp: (Boolean) -> Unit,
    onNavigatePlayers: () -> Unit,
    onNavigateGraphicBands: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        SettingsScreenHeader(title = strings.equalizer, onBack = onBack)
        Spacer(modifier = Modifier.height(22.dp))

        SettingsToggleRowItem(
            title = strings.enableAudioProcessing,
            checked = audioProcessingEnabled,
            activeTrackColor = AccentGreen,
            inactiveTrackColor = AccentRed,
            onCheckedChange = onToggleAudioProcessing
        )
        SettingsDivider()
        SettingsToggleRowItem(
            title = strings.autoPreAmp,
            checked = autoPreAmpEnabled,
            activeTrackColor = AccentGreen,
            inactiveTrackColor = AccentRed,
            onCheckedChange = onToggleAutoPreAmp
        )
        SettingsDivider()
        SettingsRowItem(title = strings.detectedPlayers, onClick = onNavigatePlayers)
        SettingsDivider()
        SettingsRowItem(title = strings.graphicEqMenu, onClick = onNavigateGraphicBands)
    }
}

@Composable
fun SettingsGraphicBandsScreen(
    strings: AppStrings,
    selectedBandCount: Int,
    onSelectBandCount: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        SettingsScreenHeader(title = strings.graphicEqTitle, onBack = onBack)
        Spacer(modifier = Modifier.height(22.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(PopupBgGray)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PillDarkGray)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(strings.graphicEqBandsHeader, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(8, 10, 12, 14).forEach { count ->
                    val isSelected = selectedBandCount == count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PillDarkGray)
                            .border(
                                width = 1.8.dp,
                                color = if (isSelected) AccentGreen else Color.White,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectBandCount(count) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$count ${strings.bandSuffix}",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPlayersScreen(
    strings: AppStrings,
    detectedPlayers: List<DetectedPlayerApp>,
    onTogglePlayer: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsScreenHeader(title = strings.detectedPlayers, onBack = onBack)
        Spacer(modifier = Modifier.height(22.dp))

        if (detectedPlayers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PillDarkGray)
                    .padding(horizontal = 16.dp, vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.noDetectedPlayersYet,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            detectedPlayers.forEachIndexed { index, player ->
                SettingsToggleRowItem(
                    title = player.displayName,
                    checked = player.isEnabled,
                    activeTrackColor = AccentGreen,
                    inactiveTrackColor = AccentRed,
                    onCheckedChange = { enabled ->
                        onTogglePlayer(player.packageName, enabled)
                    }
                )
                if (index < detectedPlayers.lastIndex) {
                    SettingsDivider()
                }
            }
        }
    }
}

@Composable
fun SettingsReportScreen(
    strings: AppStrings,
    autoReportEnabled: Boolean,
    onToggleAutoReport: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        SettingsScreenHeader(title = strings.report, onBack = onBack)
        Spacer(modifier = Modifier.height(22.dp))

        SettingsPassiveRowItem(title = "info@markam.com")
        SettingsDivider()
        SettingsToggleRowItem(
            title = strings.sendAutoReport,
            checked = autoReportEnabled,
            activeTrackColor = AccentGreen,
            inactiveTrackColor = AccentRed,
            onCheckedChange = onToggleAutoReport
        )
    }
}

// ============================================================================
// 9. ORTAK YARDIMCI BİLEŞENLER
// ============================================================================

@Composable
fun LabelPill(
    text: String,
    width: Dp,
    height: Dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(PillDarkGray)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = { onLongClick?.invoke() }
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun ControlPillButton(
    text: String,
    width: Dp,
    height: Dp,
    borderColor: Color,
    fontSize: Int = 14,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(PillDarkGray)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.8.dp, borderColor, RoundedCornerShape(height / 2))
                } else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Composable
fun CustomCheckBox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.8.dp, Color.White, RoundedCornerShape(6.dp))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.15f, size.height * 0.55f)
                    lineTo(size.width * 0.42f, size.height * 0.82f)
                    lineTo(size.width * 0.88f, size.height * 0.18f)
                }
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

@Composable
fun BackArrowButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(PillDarkGray)
            .border(1.5.dp, Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val stroke = 2.2.dp.toPx()
            val w = size.width
            val h = size.height
            drawLine(
                color = Color.White,
                start = Offset(w * 0.18f, h * 0.5f),
                end = Offset(w * 0.85f, h * 0.5f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            val arrowPath = Path().apply {
                moveTo(w * 0.48f, h * 0.20f)
                lineTo(w * 0.15f, h * 0.50f)
                lineTo(w * 0.48f, h * 0.80f)
            }
            drawPath(
                path = arrowPath,
                color = Color.White,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun SettingsScreenHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackArrowButton(onClick = onBack)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun SettingsRowItem(
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PillDarkGray)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsPassiveRowItem(
    title: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PillDarkGray)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsToggleRowItem(
    title: String,
    checked: Boolean,
    activeTrackColor: Color = AccentGreen,
    inactiveTrackColor: Color = AccentRed,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PillDarkGray)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .width(52.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (checked) activeTrackColor else inactiveTrackColor)
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun SettingsDivider() {
    Column {
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = DividerLightGray, thickness = 1.dp)
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
fun OutlinedPillInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean,
    height: Dp = 44.dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(PillDarkGray)
            .border(1.5.dp, Color.White, RoundedCornerShape(height / 2))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun RenameProfileDialog(
    title: String,
    saveText: String,
    cancelText: String,
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PopupBgGray,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = Color.White
                )
            )
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(PillDarkGray)
                    .border(1.8.dp, AccentGreen, RoundedCornerShape(18.dp))
                    .clickable { onConfirm(name) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(saveText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(PillDarkGray)
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(cancelText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun FrequencyEditDialog(
    strings: AppStrings,
    initialHz: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialHz.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PopupBgGray,
        title = { Text(strings.editFreqTitle, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { ch -> ch.isDigit() } },
                singleLine = true,
                label = { Text("20 - 20000 Hz", color = Color.White) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = Color.White
                )
            )
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(PillDarkGray)
                    .border(1.8.dp, AccentGreen, RoundedCornerShape(18.dp))
                    .clickable { onConfirm(text.toIntOrNull() ?: initialHz) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.apply, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(PillDarkGray)
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.cancel, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun formatDb(db: Float): String {
    val rounded = (db * 2f).roundToInt() / 2f
    return if (rounded % 1f == 0f) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", rounded)
    }
}
