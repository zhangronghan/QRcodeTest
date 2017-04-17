package com.example.administrator.qrcodetest;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;

import com.google.zxing.DecodeHintType;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.Decoder;
import com.journeyapps.barcodescanner.DecoderFactory;
import com.journeyapps.barcodescanner.DecoderResultPointCallback;
import com.journeyapps.barcodescanner.DecoderThread;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.journeyapps.barcodescanner.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyBarcodeView extends MyCameraPreview {

    private enum DecodeMode {
        NONE,
        SINGLE,
        CONTINUOUS
    }

    private MyBarcodeView.DecodeMode decodeMode = MyBarcodeView.DecodeMode.NONE;
    private BarcodeCallback callback = null;
    private DecoderThread decoderThread;

    private DecoderFactory decoderFactory;


    private Handler resultHandler;

    private final Handler.Callback resultCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == com.google.zxing.client.android.R.id.zxing_decode_succeeded) {
                BarcodeResult result = (BarcodeResult) message.obj;

                if (result != null) {
                    if (callback != null && decodeMode != MyBarcodeView.DecodeMode.NONE) {
                        callback.barcodeResult(result);
                        if (decodeMode == MyBarcodeView.DecodeMode.SINGLE) {
                            stopDecoding();
                        }
                    }
                }
                return true;
            } else if (message.what == com.google.zxing.client.android.R.id.zxing_decode_failed) {
                // Failed. Next preview is automatically tried.
                return true;
            } else if (message.what == com.google.zxing.client.android.R.id.zxing_possible_result_points) {
                List<ResultPoint> resultPoints = (List<ResultPoint>) message.obj;
                if (callback != null && decodeMode != MyBarcodeView.DecodeMode.NONE) {
                    callback.possibleResultPoints(resultPoints);
                }
                return true;
            }
            return false;
        }
    };


    public MyBarcodeView(Context context) {
        super(context);
        initialize(context, null);
    }

    public MyBarcodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public MyBarcodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }


    private void initialize(Context context, AttributeSet attrs) {
        decoderFactory = new DefaultDecoderFactory();
        resultHandler = new Handler(resultCallback);
    }


    /**
     * Set the DecoderFactory to use. Use this to specify the formats to decode.
     *
     * Call this from UI thread only.
     *
     * @param decoderFactory the DecoderFactory creating Decoders.
     * @see DefaultDecoderFactory
     */
    public void setDecoderFactory(DecoderFactory decoderFactory) {
        Util.validateMainThread();

        this.decoderFactory = decoderFactory;
        if (this.decoderThread != null) {
            this.decoderThread.setDecoder(createDecoder());
        }
    }

    private Decoder createDecoder() {
        if (decoderFactory == null) {
            decoderFactory = createDefaultDecoderFactory();
        }
        DecoderResultPointCallback callback = new DecoderResultPointCallback();
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, callback);
        Decoder decoder = this.decoderFactory.createDecoder(hints);
        callback.setDecoder(decoder);
        return decoder;
    }

    /**
     *
     * @return the current DecoderFactory in use.
     */
    public DecoderFactory getDecoderFactory() {
        return decoderFactory;
    }

    /**
     * Decode a single barcode, then stop decoding.
     *
     * The callback will only be called on the UI thread.
     *
     * @param callback called with the barcode result, as well as possible ResultPoints
     */
    public void decodeSingle(BarcodeCallback callback) {
        this.decodeMode = MyBarcodeView.DecodeMode.SINGLE;
        this.callback = callback;
        startDecoderThread();
    }


    /**
     * Continuously decode barcodes. The same barcode may be returned multiple times per second.
     *
     * The callback will only be called on the UI thread.
     *
     * @param callback called with the barcode result, as well as possible ResultPoints
     */
    public void decodeContinuous(BarcodeCallback callback) {
        this.decodeMode = MyBarcodeView.DecodeMode.CONTINUOUS;
        this.callback = callback;
        startDecoderThread();
    }

    /**
     * Stop decoding, but do not stop the preview.
     */
    public void stopDecoding() {
        this.decodeMode = MyBarcodeView.DecodeMode.NONE;
        this.callback = null;
        stopDecoderThread();
    }

    protected DecoderFactory createDefaultDecoderFactory() {
        return new DefaultDecoderFactory();
    }

    private void startDecoderThread() {
        stopDecoderThread(); // To be safe

        if (decodeMode != MyBarcodeView.DecodeMode.NONE && isPreviewActive()) {
            // We only start the thread if both:
            // 1. decoding was requested
            // 2. the preview is active
            decoderThread = new DecoderThread(getCameraInstance(), createDecoder(), resultHandler);
            decoderThread.setCropRect(getPreviewFramingRect());
            decoderThread.start();
        }
    }

    @Override
    protected void previewStarted() {
        super.previewStarted();

        startDecoderThread();
    }

    private void stopDecoderThread() {
        if (decoderThread != null) {
            decoderThread.stop();
            decoderThread = null;
        }
    }
    /**
     * Stops the live preview and decoding.
     *
     * Call from the Activity's onPause() method.
     */
    @Override
    public void pause() {
        stopDecoderThread();

        super.pause();
    }

}
