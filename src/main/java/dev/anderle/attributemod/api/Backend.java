package dev.anderle.attributemod.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anderle.attributemod.AttributeMod;
import dev.anderle.attributemod.utils.Helper;
import org.apache.http.client.HttpResponseException;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;

public class Backend {
    public static final String URL = "https://anderle.dev/api";

    // Cached attribute price data, refreshed once a minute.
    public JsonObject data;

    public void refreshPrices() {
        sendGetRequest("/evaluate", "",
                (String response) -> data = new JsonParser().parse(response).getAsJsonObject(),
                (IOException error) -> data = null);
    }

    public int getLbin(String itemId) {
        return Optional.ofNullable(data)
                .map(obj -> obj.getAsJsonObject("lbin"))
                .map(obj -> obj.get(itemId))
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsInt)
                .orElse(0);
    }
    public int getAttributePrice(String itemId, String attribute) {
        return Optional.ofNullable(data)
                .map(obj -> obj.getAsJsonObject("single"))
                .map(obj -> obj.getAsJsonObject(itemId))
                .map(obj -> obj.get(attribute))
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsInt)
                .orElse(0);
    }
    public int getCombinationPrice(String itemId, String firstAttribute, String secondAttribute) {
        return Optional.ofNullable(data)
                .map(obj -> obj.getAsJsonObject("combinations"))
                .map(obj -> obj.getAsJsonObject(itemId))
                .map(obj -> obj.getAsJsonObject(firstAttribute))
                .map(obj -> obj.get(secondAttribute))
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsInt)
                .orElse(0);
    }

    public void sendGetRequest(final String path, final String params, Consumer<String> onResponse, Consumer<IOException> onError) {
        new Thread(() -> {
            try {
                URL url = buildUrl(path, params);
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

                con.setSSLSocketFactory(getAllowAllFactory());
                con.setRequestMethod("GET");
                con.setConnectTimeout(20000);
                con.setReadTimeout(20000);

                handleResponse(onResponse, con);
            } catch(IOException e) {
                AttributeMod.mc.addScheduledTask(() -> onError.accept(e));
            }
        }).start();
    }

    public void sendPostRequest(final String path, final String params, final String body, Consumer<String> onResponse, Consumer<IOException> onError) {
        new Thread(() -> {
            try {
                URL url = buildUrl(path, params);
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

                con.setSSLSocketFactory(getAllowAllFactory());
                con.setRequestMethod("POST");
                con.setDoOutput(true);  // To send data in the request body
                con.setRequestProperty("Content-Type", "application/json");
                con.setConnectTimeout(20000);
                con.setReadTimeout(20000);

                try (OutputStream os = con.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                handleResponse(onResponse, con);

            } catch(IOException e) {
                AttributeMod.mc.addScheduledTask(() -> onError.accept(e));
            }
        }).start();
    }

    private void handleResponse(Consumer<String> onResponse, HttpsURLConnection con) throws IOException {
        int status = con.getResponseCode();
        if(status != 200) throw new HttpResponseException(status, con.getResponseMessage());

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) content.append(inputLine);
        in.close();

        con.disconnect();
        AttributeMod.mc.addScheduledTask(() -> onResponse.accept(content.toString()));
    }

    private URL buildUrl(String path, String params) throws MalformedURLException {
        String key = AttributeMod.config.modkey.trim();
        String uuid = Helper.getPlayerUUID();

        URL url = new URL(URL + path + "?key=" + key + "&uuid=" + uuid + "&version=" + AttributeMod.VERSION + params);
        System.out.println(url);
        return url;
    }

    /**
     * Get ssl factory, that makes java trust my own connections.
     * From <a href="https://github.com/TGWaffles/iTEM/blob/8b52fe19a706196d168a73b56078730aac9a5bd6/src/main/java/club/thom/tem/util/RequestUtil.java">...</a>
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
}
