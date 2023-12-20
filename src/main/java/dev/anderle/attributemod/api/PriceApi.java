package dev.anderle.attributemod.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anderle.attributemod.Main;
import org.apache.http.client.HttpResponseException;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class PriceApi {
    public static final String URL = "https://anderle.dev/api";
    public JsonObject data;

    private final String uuid;
    public PriceApi(String uuid) { this.uuid = uuid; }

    /**
     * Get ssl factory, that makes java trust my own connections.
     * From https://github.com/TGWaffles/iTEM/blob/8b52fe19a706196d168a73b56078730aac9a5bd6/src/main/java/club/thom/tem/util/RequestUtil.java
     */
    private static SSLSocketFactory getAllowAllFactory() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception ignored) {
        }
        return null;
    }

    public void refreshPrices() {
        request("/evaluate", "", new ResponseCallback() {
            @Override
            public void onResponse(String a) {
                data = new JsonParser().parse(a).getAsJsonObject();
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    public int getLbin(String itemId) {
        if(data == null) return 0;
        JsonObject lbin = data.getAsJsonObject("lbin");
        if(!lbin.has(itemId)) return 0;
        JsonElement price = lbin.get(itemId);
        if(price.isJsonNull()) return 0;
        else return price.getAsInt();
    }
    public int getAttributePrice(String itemId, String attribute) {
        if(data == null) return 0;
        if(itemId.equals("ATTRIBUTE_SHARD")) {
            JsonObject shards = data.getAsJsonObject("shards");
            if(shards.has(attribute)) {
                JsonElement price = shards.get(attribute);
                if(!price.isJsonNull()) return (int) (price.getAsInt() * Math.pow(2, -3));
            }
        }
        JsonObject single = data.getAsJsonObject("single");
        if(!single.has(itemId)) return 0;
        JsonObject item = single.getAsJsonObject(itemId);
        if(!item.has(attribute)) return 0;
        JsonElement price = item.get(attribute);
        if(price.isJsonNull()) return 0;
        else return price.getAsInt();
    }
    public int getCombinationPrice(String itemId, String firstAttribute, String secondAttribute) {
        if(data == null) return 0;
        JsonObject combinations = data.getAsJsonObject("combinations");
        if(!combinations.has(itemId)) return 0;
        JsonObject item = combinations.getAsJsonObject(itemId);
        if(!item.has(firstAttribute)) return 0;
        JsonObject prices = item.getAsJsonObject(firstAttribute);
        if(!prices.has(secondAttribute)) return 0;
        JsonElement price = prices.get(secondAttribute);
        if(price.isJsonNull()) return 0;
        else return price.getAsInt();
    }

    public void request(final String path, final String params, final ResponseCallback callback) {
        final String key = Main.config.get().get("Main Settings", "key", "").getString();
        final String uuid = this.uuid;

        new Thread(new Runnable() {
            @Override
            public void run() { try {
                URL url = new URL(URL + path + "?key=" + key + "&uuid=" + uuid + params);
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                con.setSSLSocketFactory(getAllowAllFactory());
                con.setRequestMethod("GET");
                con.setConnectTimeout(20000);
                con.setReadTimeout(20000);
                int status = con.getResponseCode();
                if(status != 200) throw new HttpResponseException(status, con.getResponseMessage());
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) content.append(inputLine);
                in.close();
                con.disconnect();
                callback.onResponse(content.toString());
            } catch(IOException e) { callback.onError(e); } }
        }).start();
    }

    public interface ResponseCallback {
        void onResponse(String a);
        void onError(Exception e);
    }
}
