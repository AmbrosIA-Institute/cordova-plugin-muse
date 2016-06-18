
package institute.ambrosia.plugins;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.lang3.ArrayUtils;

import org.apache.commons.math3.transform.TransformType;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

import android.util.Log;

import com.interaxon.libmuse.ConnectionState;
import com.interaxon.libmuse.Muse;
import com.interaxon.libmuse.MuseArtifactPacket;
import com.interaxon.libmuse.MuseConnectionListener;
import com.interaxon.libmuse.MuseConnectionPacket;
import com.interaxon.libmuse.MuseDataListener;
import com.interaxon.libmuse.MuseDataPacket;
import com.interaxon.libmuse.MuseDataPacketType;
import com.interaxon.libmuse.MuseManager;
import com.interaxon.libmuse.MusePreset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.Math;
import java.util.NoSuchElementException;


class CordovaMuseConnectionListener extends MuseConnectionListener {

    public static final String TAG = "Muse Plugin";
    CallbackContext myContext;

    public CordovaMuseConnectionListener(CallbackContext ctx) {
        myContext = ctx;
    }

    private void sendConnectionChange(ConnectionState past, ConnectionState current) {
        JSONObject packet = new JSONObject();
        try {
            packet.put("type", "CONNECTION");
            packet.put("past", past.toString());
            packet.put("current", current.toString());
        }
        catch (JSONException e) {
        }
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, packet);
        pluginResult.setKeepCallback(true);
        myContext.sendPluginResult(pluginResult);
    }

    @Override
    public void receiveMuseConnectionPacket(MuseConnectionPacket p) {
        final ConnectionState past = p.getPreviousConnectionState();
        final ConnectionState current = p.getCurrentConnectionState();
        sendConnectionChange(past, current);
    }
}

class CordovaMuseDataListener extends MuseDataListener {

    public static final String TAG = "Muse Plugin";
    CallbackContext myContext;
    CircularFifoQueue<Double> samples0;
    CircularFifoQueue<Double> samples1;
    CircularFifoQueue<Double> samples2;
    CircularFifoQueue<Double> samples3;
    Integer eeg_packets_since_last_raw = 0;
    Integer eeg_packets_since_last_fft = 0;

    public CordovaMuseDataListener(CallbackContext myCtx) {
        myContext = myCtx;
        samples0 = new CircularFifoQueue<Double>(256);
        samples1 = new CircularFifoQueue<Double>(256);
        samples2 = new CircularFifoQueue<Double>(256);
        samples3 = new CircularFifoQueue<Double>(256);
    }

    private void sendPacket(JSONObject packet) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, packet);
        pluginResult.setKeepCallback(true);
        myContext.sendPluginResult(pluginResult);
    }

    private void sendArtifact(MuseArtifactPacket p) {
        JSONObject packet = new JSONObject();
        try {
            packet.put("type", "ARTIFACT");
            packet.put("HeadbandOn", p.getHeadbandOn());
            packet.put("Blink", p.getBlink());
            packet.put("JawClench", p.getJawClench());
        } catch (JSONException e) {
        }
        sendPacket(packet);
    }

    private void sendValue(String type, Double val) {
        JSONObject packet = new JSONObject();
        try {
            packet.put("type", type);
            packet.put("value", val);
        } catch (JSONException e) {
        }
        sendPacket(packet);
    }

    private void sendValues(String type, ArrayList<Double> values) {
        JSONObject packet = new JSONObject();
        JSONArray packet_values = new JSONArray(values);
        try {
            packet.put("type", type);
            packet.put("values", packet_values);
        } catch (JSONException e) {
        }
        sendPacket(packet);
    }

    private void sendChannelValues(String type,
                                   ArrayList<Double> values0,
                                   ArrayList<Double> values1,
                                   ArrayList<Double> values2,
                                   ArrayList<Double> values3) {
        JSONObject packet = new JSONObject();
        try {
            packet.put("type", type);
            packet.put("values0", new JSONArray(values0));
            packet.put("values1", new JSONArray(values1));
            packet.put("values2", new JSONArray(values2));
            packet.put("values3", new JSONArray(values3));
        } catch (JSONException e) {
        }
        sendPacket(packet);
    }

    private double[] zeroFilledDoubleArray(CircularFifoQueue<Double> inputArrayList, int size) {
        double[] newArray = new double[size];
        for (int i = 0; i < inputArrayList.size(); i++)
            newArray[i] = inputArrayList.get(i);
        return newArray;
    }

    private double[] onlyAmplitude(Complex[] inputArr) {
        double[] result = new double[inputArr.length];
        for (int i = 0; i < inputArr.length; i++)
            result[i] = Math.abs(inputArr[i].getReal());
        return result;
    }

    private double[] getChannelFFTReal(FastFourierTransformer FFT, CircularFifoQueue<Double> samples) {
        Complex[] fftComplex = FFT.transform(zeroFilledDoubleArray(samples, 256), TransformType.FORWARD);
        return onlyAmplitude(fftComplex);
    }

    // FIXME: still need to resample 220/256 ~ 0.86Hz/bin
    private void sendFFT() {
        FastFourierTransformer FFT = new FastFourierTransformer(DftNormalization.STANDARD);
        double[] fft0 = getChannelFFTReal(FFT, samples0);
        double[] fft1 = getChannelFFTReal(FFT, samples1);
        double[] fft2 = getChannelFFTReal(FFT, samples2);
        double[] fft3 = getChannelFFTReal(FFT, samples3);
        Double[] fftobj0 = ArrayUtils.toObject(fft0);
        Double[] fftobj1 = ArrayUtils.toObject(fft1);
        Double[] fftobj2 = ArrayUtils.toObject(fft2);
        Double[] fftobj3 = ArrayUtils.toObject(fft3);
        ArrayList<Double> fftlist0 = new ArrayList<Double>(Arrays.asList(fftobj0));
        ArrayList<Double> fftlist1 = new ArrayList<Double>(Arrays.asList(fftobj1));
        ArrayList<Double> fftlist2 = new ArrayList<Double>(Arrays.asList(fftobj2));
        ArrayList<Double> fftlist3 = new ArrayList<Double>(Arrays.asList(fftobj3));
        sendChannelValues("FFT", fftlist0, fftlist1, fftlist2, fftlist3);
    }

    private void sendRAW() {
        ArrayList<Double> sendlist0 = new ArrayList<Double>();
        ArrayList<Double> sendlist1 = new ArrayList<Double>();
        ArrayList<Double> sendlist2 = new ArrayList<Double>();
        ArrayList<Double> sendlist3 = new ArrayList<Double>();
        int size = samples0.size();
        int start = (size < 11) ? 0 : size - 11;
        for (int i = start; i < size; i++) {
            sendlist0.add(samples0.get(i));
            sendlist1.add(samples1.get(i));
            sendlist2.add(samples2.get(i));
            sendlist3.add(samples3.get(i));
        }
        sendChannelValues("RAW", sendlist0, sendlist1, sendlist2, sendlist3);
    }

    private void handleEEGPacket(ArrayList<Double> values) {
        samples0.add(values.get(0));
        samples1.add(values.get(1));
        samples2.add(values.get(2));
        samples3.add(values.get(3));
        eeg_packets_since_last_raw += 1;
        if (eeg_packets_since_last_raw >= 11) {
            sendRAW();
            eeg_packets_since_last_raw = 0;
        }
        eeg_packets_since_last_fft += 1;
        if (eeg_packets_since_last_fft >= 22) {
            sendFFT();
            eeg_packets_since_last_fft = 0;
        }
    }

    @Override
    public void receiveMuseDataPacket(MuseDataPacket p) {
        switch (p.getPacketType()) {
            case EEG:
                handleEEGPacket(p.getValues());
                break;
            case DROPPED_EEG:
                //Log.e(TAG, "DROPPED EEG PACKET " + p.getValues().toString());
                long dropped_packets = Math.round(p.getValues().get(0));
                Double[] zeros_array = {0.0, 0.0, 0.0, 0.0};
                ArrayList<Double> zeros_list = new ArrayList<Double>(Arrays.asList(zeros_array));
                for (int i = 0; i < dropped_packets; i++) {
                    handleEEGPacket(zeros_list);
                }
                break;
            case MELLOW:
            case CONCENTRATION:
                sendValue(p.getPacketType().toString(), p.getValues().get(0));
                break;
            case HORSESHOE:
                sendValues(p.getPacketType().toString(), p.getValues());
                break;
            case ALPHA_ABSOLUTE:
            case BETA_ABSOLUTE:
            case DELTA_ABSOLUTE:
            case THETA_ABSOLUTE:
            case GAMMA_ABSOLUTE:
            case ALPHA_RELATIVE:
            case BETA_RELATIVE:
            case DELTA_RELATIVE:
            case THETA_RELATIVE:
            case GAMMA_RELATIVE:
            case ALPHA_SCORE:
            case BETA_SCORE:
            case DELTA_SCORE:
            case THETA_SCORE:
            case GAMMA_SCORE:
                sendValues(p.getPacketType().toString(), p.getValues());
                break;
            case TOTAL:
                sendValue(p.getPacketType().toString(), p.getValues().get(0));
                break;
            case BATTERY:
                sendValues(p.getPacketType().toString(), p.getValues());
                break;
            case ARTIFACTS:
                Log.e(TAG, "ARTIFACTS " + p.getValues().toString());
                break;
            case DRL_REF:
                sendValues(p.getPacketType().toString(), p.getValues());
                break;
            case ACCELEROMETER:
                sendValues(p.getPacketType().toString(), p.getValues());
                break;
            case DROPPED_ACCELEROMETER:
                Log.e(TAG, "DROPPED ACCELEROMETER " + p.getValues().toString());
                sendValues(p.getPacketType().toString(), p.getValues());
                break;
            case QUANTIZATION:
                sendValues(p.getPacketType().toString(), p.getValues());
                break;
            default:
                Log.e(TAG, "UNKNOWN PACKET" + p.getValues().toString());
                break;
        }
    }

    @Override
    public void receiveMuseArtifactPacket(MuseArtifactPacket p) {
        sendArtifact(p);
    }

}

public class MusePlugin extends CordovaPlugin {
    public static final String TAG = "Muse Plugin";
    ArrayList<CordovaMuseDataListener> dataListeners = new ArrayList<CordovaMuseDataListener>();

    public MusePlugin() {}

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.v(TAG, "Init MusePlugin");
    }

    private Muse getMuseByMacAddress(String macAddress) throws IndexOutOfBoundsException, NoSuchElementException {
        MuseManager.refreshPairedMuses();
        List<Muse> pairedMuses = MuseManager.getPairedMuses();
        if (pairedMuses.size() < 1) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = 0; i < pairedMuses.size(); i++) {
           if (pairedMuses.get(i).getMacAddress().equals(macAddress)) {
               return pairedMuses.get(i);
           }
        }
        throw new NoSuchElementException();
    }

    private Muse getMuseByName(String name) throws IndexOutOfBoundsException, NoSuchElementException {
        MuseManager.refreshPairedMuses();
        List<Muse> pairedMuses = MuseManager.getPairedMuses();
        if (pairedMuses.size() < 1) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = 0; i < pairedMuses.size(); i++) {
           if (pairedMuses.get(i).getName().equals(name)) {
               return pairedMuses.get(i);
           }
        }
        throw new NoSuchElementException();
    }

    private Muse getMuseByIndex(Integer index) throws IndexOutOfBoundsException, NoSuchElementException {
        MuseManager.refreshPairedMuses();
        List<Muse> pairedMuses = MuseManager.getPairedMuses();
        if (pairedMuses.size() < 1) {
            throw new IndexOutOfBoundsException();
        }
        else if (pairedMuses.size() <= index) {
            throw new NoSuchElementException();
        }
        return pairedMuses.get(index);
    }

    private void connectHeadset(CallbackContext ctx, Muse headset) {
        CordovaMuseDataListener dataListener = new CordovaMuseDataListener(ctx);
        headset.registerDataListener(dataListener, MuseDataPacketType.EEG);
        headset.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
        headset.registerDataListener(dataListener, MuseDataPacketType.DROPPED_ACCELEROMETER);
        headset.registerDataListener(dataListener, MuseDataPacketType.DROPPED_EEG);
        headset.registerDataListener(dataListener, MuseDataPacketType.QUANTIZATION);
        headset.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
        headset.registerDataListener(dataListener, MuseDataPacketType.DRL_REF);
        headset.registerDataListener(dataListener, MuseDataPacketType.HORSESHOE);
        headset.registerDataListener(dataListener, MuseDataPacketType.ARTIFACTS);
        headset.registerDataListener(dataListener, MuseDataPacketType.MELLOW);
        headset.registerDataListener(dataListener, MuseDataPacketType.CONCENTRATION);
        headset.setPreset(MusePreset.PRESET_14);
        headset.enableDataTransmission(true);
        headset.runAsynchronously();
        dataListeners.add(dataListener);
    }

    private void disconnectHeadset(CallbackContext ctx, Muse headset) {
        headset.disconnect(true);
        ctx.success();
    }

    private void sendSuccessKeepCallback(CallbackContext ctx, String message) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, message);
        pluginResult.setKeepCallback(true);
        ctx.sendPluginResult(pluginResult);
    }

    private void doConnectByMacAddress(final CallbackContext ctx, final String macAddress) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Muse headset = getMuseByMacAddress(macAddress);
                    connectHeadset(ctx, headset);
                    sendSuccessKeepCallback(ctx, "ok");
                } catch (IndexOutOfBoundsException e) {
                    ctx.error("no paired headsets");
                } catch (NoSuchElementException e) {
                    ctx.error("no such headset");
                }
            }
        });
    }

    private void doConnectByName(final CallbackContext ctx, final String name) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Muse headset = getMuseByName(name);
                    connectHeadset(ctx, headset);
                    sendSuccessKeepCallback(ctx, "ok");
                } catch (IndexOutOfBoundsException e) {
                    ctx.error("no paired headsets");
                } catch (NoSuchElementException e) {
                    ctx.error("no such headset");
                }
            }
        });
    }

    private void doConnectByIndex(final CallbackContext ctx, final Integer index) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Muse headset = getMuseByIndex(index);
                    connectHeadset(ctx, headset);
                    sendSuccessKeepCallback(ctx, "ok");
                } catch (IndexOutOfBoundsException e) {
                    ctx.error("no paired headsets");
                } catch (NoSuchElementException e) {
                    ctx.error("no such headset");
                }
            }
        });
    }

    private void doDisconnectByMacAddress(final CallbackContext ctx, final String macAddress) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Muse headset = getMuseByMacAddress(macAddress);
                    disconnectHeadset(ctx, headset);
                } catch (IndexOutOfBoundsException e) {
                    ctx.error("no paired headsets");
                } catch (NoSuchElementException e) {
                    ctx.error("no such headset");
                }
            }
        });
    }

    private void doDisconnectByName(final CallbackContext ctx, final String name) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Muse headset = getMuseByName(name);
                    disconnectHeadset(ctx, headset);
                } catch (IndexOutOfBoundsException e) {
                    ctx.error("no paired headsets");
                } catch (NoSuchElementException e) {
                    ctx.error("no such headset");
                }
            }
        });
    }

    private void doDisconnectByIndex(final CallbackContext ctx, final Integer index) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Muse headset = getMuseByIndex(index);
                    disconnectHeadset(ctx, headset);
                } catch (IndexOutOfBoundsException e) {
                    ctx.error("no paired headsets");
                } catch (NoSuchElementException e) {
                    ctx.error("no such headset");
                }
            }
        });
    }

    private void doListenByMacAddress(final CallbackContext ctx, final String macAddress) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Muse headset = getMuseByMacAddress(macAddress);
                    CordovaMuseConnectionListener connectionListener = new CordovaMuseConnectionListener(ctx);
                    headset.registerConnectionListener(connectionListener);
                    sendSuccessKeepCallback(ctx, "ok");
                } catch (IndexOutOfBoundsException e) {
                    ctx.error("no paired headsets");
                } catch (NoSuchElementException e) {
                    ctx.error("no such headset");
                }
            }
        });
    }

    private void doListenByName(final CallbackContext ctx, final String name) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Muse headset = getMuseByName(name);
                    CordovaMuseConnectionListener connectionListener = new CordovaMuseConnectionListener(ctx);
                    headset.registerConnectionListener(connectionListener);
                    sendSuccessKeepCallback(ctx, "ok");
                } catch (IndexOutOfBoundsException e) {
                    ctx.error("no paired headsets");
                } catch (NoSuchElementException e) {
                    ctx.error("no such headset");
                }
            }
        });
    }

    private void doListenByIndex(final CallbackContext ctx, final Integer index) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Muse headset = getMuseByIndex(index);
                    CordovaMuseConnectionListener connectionListener = new CordovaMuseConnectionListener(ctx);
                    headset.registerConnectionListener(connectionListener);
                    sendSuccessKeepCallback(ctx, "ok");
                } catch (IndexOutOfBoundsException e) {
                    ctx.error("no paired headsets");
                } catch (NoSuchElementException e) {
                    ctx.error("no such headset");
                }
            }
        });
    }

    private void doListHeadsets(final CallbackContext ctx) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                MuseManager.refreshPairedMuses();
                List<Muse> pairedMuses = MuseManager.getPairedMuses();
                JSONArray arr = new JSONArray();
                int i = 0;
                while (i < pairedMuses.size()) {
                    Muse m = pairedMuses.get(i);
                    String name = m.getName();
                    try {
                        arr.put(i, name);
                    } catch (JSONException e) {
                    }
                    i++;
                }
                ctx.success(arr);
            }
        });
    }

    private void doRefreshHeadsets(final CallbackContext ctx) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                MuseManager.refreshPairedMuses();
                ctx.success();
            }
        });
    }

    private Integer getArg0Integer(JSONArray args) {
        Integer arg0 = 0;
        if (args.length() > 0) {
            try {
                arg0 = args.getInt(0);
            }
            catch (JSONException e) {
            }
        }
        return arg0;
    }

    private String getArg0String(JSONArray args) {
        String arg0 = "";
        if (args.length() > 0) {
            try {
                arg0 = args.getString(0);
            }
            catch (JSONException e) {
            }
        }
        return arg0;
    }

    public boolean execute(final String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("list".equals(action)) {
            doListHeadsets(callbackContext);
        }
        else if ("refresh".equals(action)) {
            doRefreshHeadsets(callbackContext);
        }
        else if ("connect".equals(action) || "connect_index".equals(action)) {
            Log.i(TAG, "Connect..");
            doConnectByIndex(callbackContext, getArg0Integer(args));
        }
        else if ("connect_mac".equals(action)) {
            Log.i(TAG, "Connect..");
            doConnectByMacAddress(callbackContext, getArg0String(args));
        }
        else if ("connect_name".equals(action)) {
            Log.i(TAG, "Connect..");
            doConnectByName(callbackContext, getArg0String(args));
        }
        else if ("disconnect".equals(action) || "disconnect_index".equals(action)) {
            Log.i(TAG, "Disconnect..");
            doDisconnectByIndex(callbackContext, getArg0Integer(args));
        }
        else if ("disconnect_mac".equals(action)) {
            Log.i(TAG, "Disconnect..");
            doDisconnectByMacAddress(callbackContext, getArg0String(args));
        }
        else if ("disconnect_name".equals(action)) {
            Log.i(TAG, "Disconnect..");
            doDisconnectByName(callbackContext, getArg0String(args));
        }
        else if ("listen".equals(action) || "listen_index".equals(action)) {
            Log.i(TAG, "Listen..");
            doListenByIndex(callbackContext, getArg0Integer(args));
        }
        else if ("listen_mac".equals(action)) {
            Log.i(TAG, "Listen..");
            doListenByMacAddress(callbackContext, getArg0String(args));
        }
        else if ("listen_name".equals(action)) {
            Log.i(TAG, "Listen..");
            doListenByName(callbackContext, getArg0String(args));
        }
        return true;
    }

}
