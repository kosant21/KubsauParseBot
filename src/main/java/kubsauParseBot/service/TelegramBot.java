package kubsauParseBot.service;

import kubsauParseBot.config.BotConfig;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    //parseKubsau();
                    startCommandReceived(chat_id, parseKubsau());
                    //startCommandReceived(chat_id, update.getMessage().getChat().getFirstName());
                    break;
                default:
                    sendMessage(chat_id, "You're wrong");
            }


        }
    }

    private String parseKubsau() {

        try {
            var document = Jsoup
                    .connect("https://kubsau.ru/upload/slpd/in_lists/main/139_09.04.02_000000869_%D0%94.html?0.8338041724968897")
                    .sslSocketFactory(socketFactory())
                    .get();


            var rowElements = document.select("tr.R9");
            rowElements.remove(0);
            String results[][] = new String[rowElements.size()][5];
            for (int i = 0; i < rowElements.size(); i++) {
                var el = rowElements.get(i);
                results[i][0] = String.valueOf(i + 1);
                results[i][1] = el.select("td:nth-of-type(1)").text();

                String score = el.select("td:nth-of-type(2)").text();
                results[i][2] = score.equals("") ? "0" : score;

                String agreement = el.select("td:nth-of-type(3)").text();
                results[i][3] = agreement.equals("") ? "-" : "+";

                String original = el.select("td:nth-of-type(4)").text();
                results[i][4] = original.equals("Оригинал") ? "О" : "К";
            }
            results = sortStringArray(results);
            System.out.println(Arrays.deepToString(results));
            return rowElements.text();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String[][] sortStringArray(String mas[][]) {
        for (int i = 0; i < mas.length; i++) {
            for (int j = 0; j < mas.length - 1; j++) {
                if (Integer.parseInt(mas[j][2]) < Integer.parseInt(mas[j + 1][2])) {
                    var temp = mas[j];
                    mas[j] = mas[j + 1];
                    mas[j + 1] = temp;
                }
            }
        }
        for (int i = 0; i < mas.length; i++) {
            mas[i][0] = String.valueOf(i + 1);
        }
        return mas;
    }

    private SSLSocketFactory socketFactory() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to create a SSL socket factory", e);
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Hi, " + name + ", nice to meet you!";

        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
