package rs.fncore2.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import java.net.InetAddress;
import java.net.UnknownHostException;

import rs.log.Logger;

@SuppressWarnings("deprecation")
public class UtilsCore {

    public static Boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network nw = connectivityManager.getActiveNetwork();
            if (nw == null) return false;
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        } else {
            
			NetworkInfo nwInfo = connectivityManager.getActiveNetworkInfo();
            return nwInfo != null && nwInfo.isConnected();
        }
    }

    public static boolean isInternetAvailable() {
        try {
            InetAddress address = InetAddress.getByName("www.google.com");
            return !address.equals("");
        } catch (UnknownHostException e) {
            // Log error
        }
        return false;
    }

    public static boolean isConnected(Context context) {
        boolean isNetworkAvailable=isNetworkAvailable(context);
        boolean isInternetAvailable=false;
        if (isNetworkAvailable){
            isInternetAvailable=isInternetAvailable();
        }

        if (!(isNetworkAvailable && isInternetAvailable)){
            Logger.i("WARNING, no internet : isNetworkAvailable: %s, isInternetAvailable: %s",
                    isNetworkAvailable, isInternetAvailable);
        }
        return isNetworkAvailable && isInternetAvailable;
    }

    public static String removeLeadingZeros(String str)    {
        String regex = "^0+(?!$)";
        str = str.replaceAll(regex, "");
        return str;
    }

    public static String dblToString(double d) {
        return ("" + d).replaceAll("\\.0+$", "");
    }
}
