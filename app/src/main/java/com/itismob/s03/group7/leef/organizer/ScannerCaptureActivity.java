package com.itismob.s03.group7.leef.organizer;

import com.journeyapps.barcodescanner.CaptureActivity;

/**
 * The camera screen that scans a ticket's QR code. It only exists so the manifest can lock it to
 * portrait; the scanning itself is the library's {@link CaptureActivity}.
 */
public class ScannerCaptureActivity extends CaptureActivity {
}
