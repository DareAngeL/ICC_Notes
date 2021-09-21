package iccnote;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class InternetConnection implements Runnable {

    private final AppCompatActivity mActivity;

    private final OnConnectionResponseListener mListener;
    public interface OnConnectionResponseListener {
        void isConnected();
        void isNotConnected();
    }

    public InternetConnection(final AppCompatActivity activity, final OnConnectionResponseListener listener) {
        mListener = listener;
        mActivity = activity;
    }

    @Override
    public void run() {
        // ping the google server on run();
        try {
            Socket sock = new Socket();
            sock.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
            sock.close();

            mActivity.runOnUiThread(mListener::isConnected);
        } catch (IOException e) {
            mActivity.runOnUiThread(mListener::isNotConnected);
        }
    }
}
