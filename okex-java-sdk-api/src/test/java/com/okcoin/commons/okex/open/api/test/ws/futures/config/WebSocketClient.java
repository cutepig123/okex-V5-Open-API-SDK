package com.okcoin.commons.okex.open.api.test.ws.futures.config;

import com.alibaba.fastjson.JSONArray;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.okcoin.commons.okex.open.api.bean.other.OrderBookItem;
import com.okcoin.commons.okex.open.api.bean.other.SpotOrderBook;
import com.okcoin.commons.okex.open.api.bean.other.SpotOrderBookDiff;
import com.okcoin.commons.okex.open.api.bean.other.SpotOrderBookItem;
import com.okcoin.commons.okex.open.api.enums.CharsetEnum;
import com.okcoin.commons.okex.open.api.test.ws.futures.FuturesPublicChannelTest;
import com.okcoin.commons.okex.open.api.utils.DateUtils;
import com.okcoin.commons.okex.open.api.bean.other.OrderBookDiffer;
import lombok.Data;
import net.sf.json.JSONObject;
import okhttp3.*;
import okio.ByteString;
import org.apache.commons.compress.compressors.deflate64.Deflate64CompressorInputStream;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * webSocket client
 *
 * @author oker
 * @date 2019/7/5 1:45 AM
 */
public class WebSocketClient {
    private static WebSocket webSocket = null;
    private static Boolean flag = false;
    private static Boolean isConnect = false;
    private static String sign;
    private final static HashFunction crc32 = Hashing.crc32();
    private final static ObjectReader objectReader = new ObjectMapper().readerFor(OrderBookData.class);
    private static Map<String,Optional<SpotOrderBook>> bookMap = new HashMap<>();
    private static Logger logger = Logger.getLogger(FuturesPublicChannelTest.class);
    public WebSocketClient() {
    }



    //????????????????????????????????????????????????URL
    public static WebSocket connection(final String url) {

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            ScheduledExecutorService service;

            @Override
            public void onOpen(final WebSocket webSocket, final Response response) {
                //??????????????????????????????????????????25s????????????????????????????????????????????????????????????
                isConnect = true;
                System.out.println(Instant.now().toString() + " Connected to the server success!");
                Runnable runnable = new Runnable() {
                    public void run() {
                        // task to run goes here
                        sendMessage("ping");
                    }
                };
                service = Executors.newSingleThreadScheduledExecutor();
                // ?????????????????????????????????????????????????????????????????????????????????????????????
                service.scheduleAtFixedRate(runnable, 25, 25, TimeUnit.SECONDS);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                System.out.println("Connection is about to disconnect???");
                webSocket.close(1000, "Long time no message was sent or received???");
                webSocket = null;
            }

            @Override
            public void onClosed(final WebSocket webSocket, final int code, final String reason) {
                System.out.println("Connection dropped???");
            }

            @Override
            public void onFailure(final WebSocket webSocket, final Throwable t, final Response response) {
                System.out.println("Connection failed,Please reconnect!");
                if (Objects.nonNull(service)) {

                    service.shutdown();
                }
            }

            @Override
            public void onMessage(final WebSocket webSocket, final ByteString bytes) {
                //??????????????????????????????
                final String byteString=bytes.toString();

                final String s = uncompress(bytes.toByteArray());
                //???????????????????????????
                if (s.contains("\"table\":\"futures/depth\",")||s.contains("\"table\":\"futures/depth_l2_tbt\",")||s.contains("\"table\":\"swap/depth\",")) {//???????????????
                    if (s.contains("partial")) {//???????????????200???????????????????????????200???
                        String[] strs=s.split("],");
                        JSONObject rst = JSONObject.fromObject(s);
                        net.sf.json.JSONArray dataArr = net.sf.json.JSONArray.fromObject(rst.get("data"));
                        JSONObject data = JSONObject.fromObject(dataArr.get(0));
                        String dataStr = data.toString();
                        Optional<SpotOrderBook> oldBook = parse(dataStr);
                        String instrumentId = data.get("instrument_id").toString();
                        bookMap.put(instrumentId,oldBook);
                    } else if (s.contains("\"action\":\"update\",")) {//????????????????????????????????????????????????

                        JSONObject rst = JSONObject.fromObject(s);
                        net.sf.json.JSONArray dataArr = net.sf.json.JSONArray.fromObject(rst.get("data"));
                        JSONObject data = JSONObject.fromObject(dataArr.get(0));
                        String dataStr = data.toString();
                        String instrumentId = data.get("instrument_id").toString();
                        Optional<SpotOrderBook> oldBook = bookMap.get(instrumentId);
                        Optional<SpotOrderBook> newBook = parse(dataStr);
                        //???????????????ask
                        List<SpotOrderBookItem> askList = newBook.get().getAsks();
                        //???????????????bid
                        List<SpotOrderBookItem> bidList = newBook.get().getBids();
                        SpotOrderBookDiff bookdiff = oldBook.get().diff(newBook.get());

                        System.out.println("?????????"+instrumentId+",?????????????????????checknum?????????" + bookdiff.getChecksum() + ",????????????????????????" + bookdiff.toString());

                        String str = getStr(bookdiff.getAsks(), bookdiff.getBids());
                        System.out.println("?????????"+instrumentId+",??????????????????????????????" + str);
                        //??????checksum???
                        int checksum = checksum(bookdiff.getAsks(), bookdiff.getBids());
                        System.out.println("?????????"+instrumentId+",?????????checksum:" + checksum);
                        boolean flag = checksum==bookdiff.getChecksum()?true:false;
                        if(flag){
                            System.out.println("?????????"+instrumentId+",????????????????????????"+flag);
                            oldBook = parse(bookdiff.toString());
                            bookMap.put(instrumentId,oldBook);
                        }else{
                            System.out.println("?????????"+instrumentId+",????????????????????????"+flag+"???????????????????????????");
                            //??????????????????????????????
                            String channel = rst.get("table").toString();
                            String unSubStr = "{\"op\": \"unsubscribe\", \"args\":[\"" + channel+":"+instrumentId + "\"]}";
                            System.out.println(DateFormatUtils.format(new Date(), DateUtils.TIME_STYLE_S4) + " Send: " + unSubStr);
                            webSocket.send(unSubStr);
                            String subStr = "{\"op\": \"subscribe\", \"args\":[\"" + channel+":"+instrumentId + "\"]}";
                            System.out.println(DateFormatUtils.format(new Date(), DateUtils.TIME_STYLE_S4) + " Send: " + subStr);
                            webSocket.send(subStr);
                            System.out.println("?????????"+instrumentId+",?????????????????????");
                        }
                    }
                } else {//??????????????????

                    System.out.println(DateFormatUtils.format(new Date(), DateUtils.TIME_STYLE_S4) + " Receive: " + s);
                }
                if (null != s && s.contains("login")) {
                    if (s.endsWith("true}")) {
                        flag = true;
                    }
                }
            }
        });
        return webSocket;
    }

    // ????????????
    private static String uncompress(final byte[] bytes) {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
             final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
             final Deflate64CompressorInputStream zin = new Deflate64CompressorInputStream(in)) {
            final byte[] buffer = new byte[1024];
            int offset;
            while (-1 != (offset = zin.read(buffer))) {
                out.write(buffer, 0, offset);
            }
            return out.toString();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void isLogin(String s) {
        if (null != s && s.contains("login")) {
            if (s.endsWith("true}")) {
                flag = true;
            }
        }
    }

    //??????sign
    private static String sha256_HMAC(String message, String secret) {
        String hash = "";
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(CharsetEnum.UTF_8.charset()), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] bytes = sha256_HMAC.doFinal(message.getBytes(CharsetEnum.UTF_8.charset()));
            hash = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            System.out.println("Error HmacSHA256 ===========" + e.getMessage());
        }
        return hash;
    }

    private static String listToJson(List<String> list) {
        JSONArray jsonArray = new JSONArray();
        for (String s : list) {
            jsonArray.add(s);
        }
        return jsonArray.toJSONString();
    }

    //??????
    public static void login(String apiKey, String passPhrase, String secretKey) {
        String timestamp = (Double.parseDouble(DateUtils.getEpochTime()) + 28800) + "";
        String message = timestamp + "GET" + "/users/self/verify";
        sign = sha256_HMAC(message, secretKey);
        String str = "{\"op\"" + ":" + "\"login\"" + "," + "\"args\"" + ":" + "[" + "\"" + apiKey + "\"" + "," + "\"" + passPhrase + "\"" + "," + "\"" + timestamp + "\"" + "," + "\"" + sign + "\"" + "]}";
        sendMessage(str);
    }


    //???????????????????????????????????????
    public static void subscribe(List<String> list) {
        String s = listToJson(list);
        String str = "{\"op\": \"subscribe\", \"args\":" + s + "}";
        if (null != webSocket)
            sendMessage(str);
    }

    //?????????????????????????????????????????????
    public static void unsubscribe(List<String> list) {
        String s = listToJson(list);
        String str = "{\"op\": \"unsubscribe\", \"args\":" + s + "}";
        if (null != webSocket)
            sendMessage(str);
    }

    private static void sendMessage(String str) {
        if (null != webSocket) {
            try {
                Thread.sleep(1300);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (str.contains("account") || str.contains("position") || str.contains("order")) {
                if (!flag) {
                    System.out.println("Channels contain channels that require login privileges to operate. Please login and operate again???");
                    return;
                }
            }
            System.out.println(DateFormatUtils.format(new Date(), DateUtils.TIME_STYLE_S4)+"Send a message to the server:" + str);
            webSocket.send(str);
        } else {
            System.out.println("Please establish the connection before you operate it???");
        }
    }

    //????????????
    public static void closeConnection() {
        if (null != webSocket) {
            webSocket.close(1000, "User actively closes the connection");
        } else {
            System.out.println("Please establish the connection before you operate it???");
        }
    }

    public boolean getIsLogin() {
        return flag;
    }

    public boolean getIsConnect() {
        return isConnect;
    }

    public static <T extends OrderBookItem> int checksum(List<T> asks, List<T> bids) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            if (i < bids.size()) {
                s.append(bids.get(i).getPrice().toString());
                s.append(":");
                s.append(bids.get(i).getSize());
                s.append(":");
            }
            if (i < asks.size()) {
                s.append(asks.get(i).getPrice().toString());
                s.append(":");
                s.append(asks.get(i).getSize());
                s.append(":");
            }
        }
        final String str;
        if (s.length() > 0) {
            str = s.substring(0, s.length() - 1);
        } else {
            str = "";
        }

        return crc32.hashString(str, StandardCharsets.UTF_8).asInt();
    }

    private static <T extends OrderBookItem> String getStr(List<T> asks, List<T> bids) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            if (i < bids.size()) {
                s.append(bids.get(i).getPrice().toString());
                s.append(":");
                s.append(bids.get(i).getSize());
                s.append(":");
            }
            if (i < asks.size()) {
                s.append(asks.get(i).getPrice().toString());
                s.append(":");
                s.append(asks.get(i).getSize());
                s.append(":");
            }
        }
        final String str;
        if (s.length() > 0) {
            str = s.substring(0, s.length() - 1);
        } else {
            str = "";
        }
        return str;
    }

    public static Optional<SpotOrderBook> parse(String json) {

        try {
            OrderBookData data = objectReader.readValue(json);
            List<SpotOrderBookItem> asks =
                    data.getAsks().stream().map(x -> new SpotOrderBookItem(new String(x.get(0)), x.get(1), x.get(2)))
                            .collect(Collectors.toList());

            List<SpotOrderBookItem> bids =
                    data.getBids().stream().map(x -> new SpotOrderBookItem(new String(x.get(0)), x.get(1), x.get(2)))
                            .collect(Collectors.toList());

            return Optional.of(new SpotOrderBook(data.getInstrument_id(), asks, bids, data.getTimestamp(),data.getChecksum()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Data
    public static class OrderBookData {
        private String instrument_id;
        private List<List<String>> asks;
        private List<List<String>> bids;
        private String timestamp;
        private int checksum;

        public String getInstrument_id() {
            return instrument_id;
        }

        public void setInstrument_id(String instrument_id) {
            this.instrument_id = instrument_id;
        }

        public List<List<String>> getAsks() {
            return asks;
        }

        public void setAsks(List<List<String>> asks) {
            this.asks = asks;
        }

        public List<List<String>> getBids() {
            return bids;
        }

        public void setBids(List<List<String>> bids) {
            this.bids = bids;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public int getChecksum() {
            return checksum;
        }

        public void setChecksum(int checksum) {
            this.checksum = checksum;
        }
    }
}
