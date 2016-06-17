package vn.hhtv.mygifmaker2.utils;

/**
 * Created by Nienlm on 6/16/16 2:21 PM.
 */
public class LogUtil {

    /*
     * Used to identify the source of a log message. It usually identifies the
     * class or activity where the log call occurs
     */
    public static String LOGTAG = "mygifmaker";

    /*
     * Is sent a VERBOSE log message. Set true if you want to show log to debug
     * application, else set false
     */
    public static boolean LOGV = false;

    /*
     * Is sent a DEBUG log message. Set true if you want to show log to debug
     * application, else set false
     */
    public static boolean LogD = true;

    /*
     * Is sent a ERROR log message. Set true if you want to show log to debug
     * application, else set false
     */
    public static boolean LogE = true;

    /*
     * Is sent a INFO log message. Set true if you want to show log to debug
     * application, else set false
     */
    public static boolean LOGI = false;

    /*
     * Is sent a WARN log message. Set true if you want to show log to debug
     * application, else set false
     */
    public static boolean LOGW = false;

    /**
     * Send a VERBOSE log message.
     *
     * @param log : The message you would like logged.
     */
    public static void v(String log) {
        if (LOGV) {
            android.util.Log.v(LOGTAG, log);
        }
    }

    /**
     * Send a VERBOSE log message and log the exception.
     *
     * @param log : The message you would like logged.
     * @param tr  : An exception to log
     */
    public static void v(String log, Throwable tr) {
        if (LOGV) {
            android.util.Log.v(LOGTAG, log, tr);
        }
    }

    /**
     * Send a DEBUG log message.
     *
     * @param log The message you would like logged.
     */
    public static void d(String log) {
        if (LogD) {
            android.util.Log.d(LOGTAG, log);
        }
    }

    /**
     * Send a DEBUG log message and log the exception.
     *
     * @param log       The message you would like logged.
     * @param throwable An exception to log
     */
    public static void d(String log, Throwable throwable) {
        if (LogD) {
            android.util.Log.d(LOGTAG, log, throwable);
        }
    }

    /**
     * Send an ERROR log message.
     *
     * @param log The message you would like logged.
     */
    public static void e(String log) {
        if (LogE) {
            android.util.Log.e(LOGTAG, log);
        }
    }

    /**
     * Send a ERROR log message and log the exception.
     *
     * @param log The message you would like logged.
     * @param tr  An exception to log
     */
    public static void e(String log, Throwable tr) {
        if (LogE) {
            android.util.Log.e(LOGTAG, log, tr);
        }
    }

    /**
     * Send an INFO log message.
     *
     * @param log The message you would like logged.
     */
    public static void i(String log) {
        if (LOGI) {
            android.util.Log.i(LOGTAG, log);
        }
    }

    /**
     * Send a INFO log message and log the exception.
     *
     * @param log The message you would like logged.
     * @param tr  An exception to log
     */
    public static void i(String log, Throwable tr) {
        if (LOGI) {
            android.util.Log.i(LOGTAG, log, tr);
        }
    }

    /**
     * Send a WARN log message
     *
     * @param log The message you would like logged.
     */
    public static void w(String log) {
        if (LOGW) {
            android.util.Log.w(LOGTAG, log);
        }
    }

    /**
     * Send a WARN log message and log the exception.
     *
     * @param log The message you would like logged.
     * @param tr  An exception to log
     */
    public static void w(String log, Throwable tr) {
        if (LOGW) {
            android.util.Log.w(LOGTAG, log, tr);
        }
    }


    /************WTHLOG************/
    public static final String WTHLOG = "ohgodplease";
    public static final boolean WTH = true;
    public static void wth(String log){
        if (WTH){
            android.util.Log.d(WTHLOG, log);
        }
    }
}
