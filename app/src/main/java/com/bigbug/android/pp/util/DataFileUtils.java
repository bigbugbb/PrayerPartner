package com.bigbug.android.pp.util;

import android.content.Context;
import android.os.Environment;

import com.bigbug.android.pp.Config;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.bigbug.android.pp.util.LogUtils.makeLogTag;

/**
 * Utilities and constants related to files
 */
public class DataFileUtils {
    private static final String TAG = makeLogTag(DataFileUtils.class);

    private static final SimpleDateFormat HourFormat = new SimpleDateFormat("HH");
    private static final int INVALID_DATA = -1;
    private static final int SUMMARY_COUNT = 24 * 60 * Config.MONITORING_DURATION_IN_SECONDS;

    public static String getSensorDataBaseDirPath(Context context) {
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + ".iTracker";
        new File(dirPath).mkdirs();

        return new StringBuilder()
                .append(dirPath)
                .append(File.separator).append("data")
                .append(File.separator).append("sensors")
                .append(File.separator)
                .toString();
    }

    public static String getSensorDataDirPath(Context context, Date date) {
        if (date == null) {
            date = new Date();
        }
        String baseDir = getSensorDataBaseDirPath(context);
        StringBuilder builder = new StringBuilder();
        return builder.append(baseDir)
                .append(DateFormatUtils.ISO_DATE_FORMAT.format(date)).append(File.separator)
                .append(HourFormat.format(date)).append(File.separator)
                .toString();
    }

    public static void writeFile(String data, File file) throws IOException {
        writeFile(data.getBytes(Charset.forName("UTF-8")), file);
    }

    public static void writeFile(byte[] data, File file) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, false));
        bos.write(data);
        bos.close();
    }

    public static String readFileAsString(File file) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        IOUtils.copy(bis, bos);
        byte[] contents = bos.toByteArray();
        bis.close();
        bos.close();
        return new String(contents, Charset.forName("UTF-8"));
    }
}