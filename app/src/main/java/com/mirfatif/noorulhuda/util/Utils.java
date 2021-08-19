package com.mirfatif.noorulhuda.util;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static android.text.style.DynamicDrawableSpan.ALIGN_BASELINE;
import static com.mirfatif.noorulhuda.prefs.MySettings.SETTINGS;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.TooltipCompat;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.mirfatif.noorulhuda.App;
import com.mirfatif.noorulhuda.BuildConfig;
import com.mirfatif.noorulhuda.R;
import com.mirfatif.noorulhuda.databinding.DialogTextViewBinding;
import com.mirfatif.noorulhuda.svc.NotifDismissSvc;
import com.mirfatif.noorulhuda.ui.dialog.AlertDialogFragment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;

public class Utils {

  private Utils() {}

  private static final Handler UI_EXECUTOR = new Handler(Looper.getMainLooper());

  // ContextCompat.getMainExecutor()
  @SuppressWarnings("UnusedReturnValue")
  public static MainFuture runUi(Runnable runnable) {
    MainFuture futureTask = new MainFuture(runnable);
    UI_EXECUTOR.post(futureTask);
    return futureTask;
  }

  public static class MainFuture extends FutureTask<Void> {

    public MainFuture(Runnable runnable) {
      super(runnable, null);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean waitForMe() {
      try {
        super.get();
        return true;
      } catch (ExecutionException | InterruptedException e) {
        e.printStackTrace();
        return false;
      }
    }
  }

  private static final ExecutorService BG_EXECUTOR = Executors.newCachedThreadPool();

  public static Future<?> runBg(Runnable runnable) {
    return BG_EXECUTOR.submit(runnable);
  }

  public static void runCommand(String tag, String... cmd) {
    Process proc = runCommand(tag, true, cmd);
    if (proc == null) {
      return;
    }

    try (BufferedReader stdIn = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
      String line;
      while ((line = stdIn.readLine()) != null) {
        Log.i(tag, line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      cleanStreams(proc, tag);
    }
  }

  public static Process runCommand(String tag, boolean redirectStdErr, String... cmd) {
    ProcessBuilder processBuilder = new ProcessBuilder(cmd);
    processBuilder.directory(App.getCxt().getExternalFilesDir(null));
    processBuilder.redirectErrorStream(redirectStdErr);

    Log.i(tag, "Executing: " + Arrays.toString(cmd));
    try {
      return processBuilder.start();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static void cleanStreams(Process process, String tag) {
    try {
      if (process != null) {
        process.getInputStream().close();
        process.getErrorStream().close();
        process.getOutputStream().close();
        // Try the best to kill the process. The on reading daemon's logcat might not
        // be killed because of different UID.
        process.destroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          process.destroyForcibly();
        }
      }
    } catch (Throwable e) {
      Log.e(tag, e.toString());
    }
  }

  public static SharedPreferences getDefPrefs() {
    return App.getCxt().getSharedPreferences("def_prefs", Context.MODE_PRIVATE);
  }

  public static SharedPreferences getNoBkpPrefs() {
    return App.getCxt().getSharedPreferences("no_bkp_prefs", Context.MODE_PRIVATE);
  }

  // MaterialColors.getColor(). Doesn't work with Application or Service context
  public static @ColorInt int getAttrColor(Activity activity, @AttrRes int colorAttrResId) {
    TypedValue typedValue = new TypedValue();
    if (activity.getTheme().resolveAttribute(colorAttrResId, typedValue, true)) {
      return typedValue.data;
    }
    return Color.TRANSPARENT;
  }

  public static @ColorInt int getAccentColor() {
    String key = getString(R.string.pref_main_theme_color_key);
    String defVal = getString(R.string.pref_main_theme_color_default);
    String theme = getDefPrefs().getString(key, defVal);
    if (theme.equals(getString(R.string.theme_color_blue))) {
      return App.getCxt().getColor(R.color.blue);
    } else if (theme.equals(getString(R.string.theme_color_red))) {
      return App.getCxt().getColor(R.color.red);
    } else if (theme.equals(getString(R.string.theme_color_gray))) {
      return App.getCxt().getColor(R.color.gray);
    }
    return App.getCxt().getColor(R.color.green);
  }

  public static void setTooltip(ImageView v) {
    TooltipCompat.setTooltipText(v, v.getContentDescription());
  }

  public static boolean setNightTheme(Activity activity) {
    if (!SETTINGS.getForceDarkMode()) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
      return false;
    }

    // Dark Mode applied on whole device
    if (isNightMode(activity)) {
      return false;
    }

    // Dark Mode already applied in app
    int defMode = AppCompatDelegate.getDefaultNightMode();
    if (defMode == AppCompatDelegate.MODE_NIGHT_YES) {
      return false;
    }

    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    return true;
  }

  public static boolean isNightMode(Activity activity) {
    int uiMode = activity.getResources().getConfiguration().uiMode;
    return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  }

  public static void createNotifChannel(String id, String name, int importance) {
    NotificationManagerCompat nm = NotificationManagerCompat.from(App.getCxt());
    NotificationChannelCompat ch = nm.getNotificationChannelCompat(id);
    if (ch == null) {
      ch = new NotificationChannelCompat.Builder(id, importance).setName(name).build();
      nm.createNotificationChannel(ch);
    }
  }

  public static int toPx(int dp) {
    return dp * App.getRes().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT;
  }

  public static boolean isLandscape() {
    return App.getRes().getConfiguration().orientation == ORIENTATION_LANDSCAPE;
  }

  public static String getCurrDateTime(boolean spaced) {
    if (spaced) {
      return new SimpleDateFormat("dd-MMM-yy HH:mm:ss", Locale.ENGLISH)
          .format(System.currentTimeMillis());
    } else {
      return new SimpleDateFormat("dd-MMM-yy_HH-mm-ss", Locale.ENGLISH)
          .format(System.currentTimeMillis());
    }
  }

  public static String getDeviceInfo() {
    return "Version: "
        + BuildConfig.VERSION_NAME
        + "\nSDK: "
        + VERSION.SDK_INT
        + "\nROM: "
        + Build.DISPLAY
        + "\nBuild: "
        + Build.TYPE
        + "\nDevice: "
        + Build.DEVICE
        + "\nManufacturer: "
        + Build.MANUFACTURER
        + "\nModel: "
        + Build.MODEL
        + "\nProduct: "
        + Build.PRODUCT;
  }

  public static String getString(int resId, Object... args) {
    return App.getCxt().getString(resId, args);
  }

  public static String getQtyString(int resId, int qty, Object... args) {
    return App.getRes().getQuantityString(resId, qty, args);
  }

  public static int getInteger(int resId) {
    return App.getCxt().getResources().getInteger(resId);
  }

  public static String getArNum(int num) {
    return String.format(new Locale("ar"), "%d", num);
  }

  public static Integer getStaticIntField(String name, Class<?> cls, String tag) {
    try {
      return cls.getDeclaredField(name).getInt(null);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      Log.e(tag, e.toString());
      return null;
    }
  }

  public static void showToast(String msg) {
    if (msg != null) {
      runUi(() -> showToast(msg, Toast.LENGTH_LONG));
    }
  }

  public static void showToast(int resId, Object... args) {
    if (resId != 0) {
      showToast(getString(resId, args));
    }
  }

  public static void showShortToast(int resId, Object... args) {
    if (resId != 0) {
      runUi(() -> showToast(getString(resId, args), Toast.LENGTH_SHORT));
    }
  }

  public static void showShortToast(String msg) {
    if (msg != null) {
      runUi(() -> showToast(msg, Toast.LENGTH_SHORT));
    }
  }

  private static void showToast(String msg, int duration) {
    Toast toast = Toast.makeText(App.getCxt(), msg, duration);
    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 0);
    toast.show();
  }

  public static Spanned htmlToString(int resId) {
    return htmlToString(getString(resId));
  }

  public static Spanned htmlToString(String str) {
    Spanned spanned = Html.fromHtml(str, Html.FROM_HTML_MODE_COMPACT);

    // Let's customize BulletSpans
    SpannableStringBuilder string = new SpannableStringBuilder(spanned);

    Parcel parcel = Parcel.obtain();
    parcel.writeInt(toPx(4)); // gapWidth
    parcel.writeInt(0); // wantColor
    parcel.writeInt(0); // color
    parcel.writeInt(toPx(2)); // bulletRadius

    for (BulletSpan span : string.getSpans(0, string.length(), BulletSpan.class)) {
      int start = string.getSpanStart(span);
      int end = string.getSpanEnd(span);
      string.removeSpan(span);
      parcel.setDataPosition(0); // For read
      string.setSpan(new BulletSpan(parcel), start, end, SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    Drawable d = ResourcesCompat.getDrawable(App.getRes(), R.drawable.link, null);
    if (d != null) {
      // DrawableCompat.setTint()
      d.setTint(getAccentColor());
      d.setBounds(0, 0, toPx(12), toPx(12));
    }

    for (URLSpan span : string.getSpans(0, string.length(), URLSpan.class)) {
      int start = string.getSpanStart(span);
      int end = string.getSpanEnd(span);
      if (!string.toString().substring(start, end).equals("LINK")) {
        continue;
      }
      string.setSpan(new ImageSpan(d, ALIGN_BASELINE), start, end, SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    breakParas(string);
    parcel.recycle();
    return string;
  }

  @SuppressWarnings("UnusedReturnValue")
  public static SpannableStringBuilder breakParas(SpannableStringBuilder string) {
    // Remove newLine chars at end
    while (true) {
      int len = string.length();
      if (string.charAt(len - 1) != '\n') {
        break;
      }
      string.delete(len - 1, len);
    }

    Matcher matcher = Pattern.compile("\n").matcher(string);
    int from = 0;
    while (matcher.find(from)) {
      // Replace the existing newLine char with 2 newLine chars
      string.replace(matcher.start(), matcher.end(), "\n\n");
      // On next iteration skip the newly added newLine char
      from = matcher.end() + 1;

      // Add span to the newly added newLine char
      string.setSpan(
          new RelativeSizeSpan(0.25f),
          matcher.start() + 1,
          matcher.end() + 1,
          SPAN_EXCLUSIVE_EXCLUSIVE);

      // On Android 7 Matcher is not refreshed if the string is changed
      matcher = Pattern.compile("\n").matcher(string);
    }

    return string;
  }

  public static boolean openWebUrl(Activity activity, String url) {
    Intent intent = new Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
    PackageManager pm = App.getCxt().getPackageManager();
    List<ResolveInfo> infoList = pm.queryIntentServices(intent, PackageManager.MATCH_ALL);
    boolean customTabsSupported = !infoList.isEmpty();

    if (customTabsSupported) {
      CustomTabColorSchemeParams colorSchemeParams =
          new CustomTabColorSchemeParams.Builder()
              .setToolbarColor(getAttrColor(activity, R.attr.accentTrans2))
              .build();
      CustomTabsIntent customTabsIntent =
          new CustomTabsIntent.Builder()
              .setShareState(CustomTabsIntent.SHARE_STATE_ON)
              .setDefaultColorSchemeParams(colorSchemeParams)
              .build();
      customTabsIntent.launchUrl(activity, Uri.parse(url));
      return true;
    }

    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    intent.addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try {
      activity.startActivity(intent);
      return true;
    } catch (ActivityNotFoundException ignored) {
    }

    if (VERSION.SDK_INT >= VERSION_CODES.R) {
      intent.setFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER);
      try {
        activity.startActivity(intent);
        return true;
      } catch (ActivityNotFoundException ignored) {
      }
    }

    showToast(R.string.no_browser_installed);
    return true;
  }

  @SuppressWarnings("UnusedReturnValue")
  public static boolean sendMail(Activity activity, String body) {
    Intent emailIntent = new Intent(Intent.ACTION_SENDTO).setData(Uri.parse("mailto:"));
    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {getString(R.string.email_address)});
    emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
    if (body != null) {
      emailIntent.putExtra(Intent.EXTRA_TEXT, body);
    }
    try {
      activity.startActivity(emailIntent);
    } catch (ActivityNotFoundException e) {
      showToast(R.string.no_email_app_installed);
    }
    return true;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean isInternetReachable() {
    // InetAddress#isReachable() is not reliable.
    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) new URL("https://api.github.com/users/mirfatif").openConnection();
      conn.setConnectTimeout(5000);
      if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
        return true;
      }
    } catch (IOException ignored) {
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
    return false;
  }

  public static void showThirdPartyCredits(FragmentActivity activity, boolean cancelable) {
    showTextDialog(
        activity,
        R.string.third_party_res,
        R.string.third_party_credits,
        "THIRD_PARTY_RES",
        cancelable);
  }

  public static void showTextDialog(
      FragmentActivity activity,
      @StringRes int title,
      @StringRes int msg,
      String tag,
      boolean cancelable) {
    DialogTextViewBinding b = DialogTextViewBinding.inflate(activity.getLayoutInflater());
    b.textV.setText(htmlToString(msg));
    b.textV.setMovementMethod(
        BetterLinkMovementMethod.newInstance()
            .setOnLinkClickListener((tv, url) -> openWebUrl(activity, url)));
    AlertDialog.Builder builder =
        new AlertDialog.Builder(activity).setTitle(title).setView(b.getRoot());
    if (!cancelable) {
      builder.setPositiveButton(android.R.string.ok, null);
    }
    AlertDialogFragment dialog = new AlertDialogFragment(builder.create());
    dialog.setCancelable(cancelable);
    dialog.show(activity, tag, false);
  }

  // https://stackoverflow.com/a/63455547/9165920
  public static void reduceDragSensitivity(ViewPager2 pager) {
    try {
      Field field = ViewPager2.class.getDeclaredField("mRecyclerView");
      field.setAccessible(true);
      RecyclerView recyclerView = (RecyclerView) field.get(pager);

      field = RecyclerView.class.getDeclaredField("mTouchSlop");
      field.setAccessible(true);
      int touchSlop = field.getInt(recyclerView);
      field.set(recyclerView, touchSlop * 4);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  //////////////////////////////////////////////////////////////////
  ///////////////////////////// LOGGING ////////////////////////////
  //////////////////////////////////////////////////////////////////

  private static final Object CRASH_LOG_LOCK = new Object();

  public static void writeCrashLog(String stackTrace) {
    synchronized (CRASH_LOG_LOCK) {
      File logFile = new File(App.getCxt().getExternalFilesDir(null), "NUH_crash.log");
      boolean append = true;
      if (!logFile.exists()
          || logFile.length() > 512 * 1024
          || logFile.lastModified() < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)) {
        append = false;
      }
      try {
        PrintWriter writer = new PrintWriter(new FileWriter(logFile, append));
        writer.println("=================================");
        writer.println(getDeviceInfo());
        writer.println("Time: " + getCurrDateTime(true));
        writer.println("Log ID: " + UUID.randomUUID().toString());
        writer.println("=================================");
        writer.println(stackTrace);
        writer.close();
        showCrashNotification(logFile);
      } catch (IOException ignored) {
      }
    }
  }

  private static void showCrashNotification(File logFile) {
    if (!SETTINGS.shouldAskToSendCrashReport()) {
      return;
    }

    String authority = BuildConfig.APPLICATION_ID + ".FileProvider";
    Uri logFileUri = FileProvider.getUriForFile(App.getCxt(), authority, logFile);

    final String CHANNEL_ID = "channel_crash_report";
    final String CHANNEL_NAME = getString(R.string.channel_crash_report);
    final int UNIQUE_ID = getInteger(R.integer.channel_crash_report);

    Intent intent = new Intent(Intent.ACTION_SEND);
    intent
        .setData(logFileUri)
        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_EMAIL, new String[] {getString(R.string.email_address)})
        .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " - Crash Report")
        .putExtra(Intent.EXTRA_TEXT, "Find attachment.")
        .putExtra(Intent.EXTRA_STREAM, logFileUri);

    // Adding extra information to dismiss notification after the action is tapped
    intent
        .setClass(App.getCxt(), NotifDismissSvc.class)
        .putExtra(NotifDismissSvc.EXTRA_INTENT_TYPE, NotifDismissSvc.INTENT_TYPE_ACTIVITY)
        .putExtra(NotifDismissSvc.EXTRA_NOTIF_ID, UNIQUE_ID);

    PendingIntent pi = getNotifDismissSvcPi(UNIQUE_ID, intent);
    createNotifChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManagerCompat.IMPORTANCE_HIGH);

    NotificationCompat.Builder nb =
        new NotificationCompat.Builder(App.getCxt(), CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(R.string.crash_report))
            .setContentText(getString(R.string.ask_to_report_crash_small))
            .setStyle(
                new NotificationCompat.BigTextStyle()
                    .bigText(getString(R.string.ask_to_report_crash)))
            .setContentIntent(pi)
            .addAction(0, getString(R.string.send_report), pi)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

    NotificationManagerCompat.from(App.getCxt()).notify(UNIQUE_ID, nb.build());
  }

  private static PendingIntent getNotifDismissSvcPi(int uniqueId, Intent intent) {
    return PendingIntent.getService(App.getCxt(), uniqueId, intent, getPiFlags());
  }

  public static int getPiFlags() {
    return PendingIntent.FLAG_UPDATE_CURRENT;
  }
}
