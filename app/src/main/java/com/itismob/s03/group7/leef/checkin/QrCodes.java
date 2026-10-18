package com.itismob.s03.group7.leef.checkin;

import android.graphics.Bitmap;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

/** Draws QR codes on the device, so a ticket works with no internet. */
final class QrCodes {

    private QrCodes() {
    }

    /**
     * What the check-in QR holds: the confirmation code and the event, so whoever scans it can tell
     * which registration it is. It carries nothing private (no name, email or payment detail).
     */
    @NonNull
    static String checkInPayload(@NonNull String confirmationCode, @NonNull String eventId) {
        return "LEEF:" + confirmationCode + ":" + eventId;
    }

    /** A square QR bitmap, dark modules on a light background, or null if the text can't be encoded. */
    @Nullable
    static Bitmap render(@NonNull String text, int sizePx, @ColorInt int dark, @ColorInt int light) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 0);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = matrix.get(x, y) ? dark : light;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }
}
