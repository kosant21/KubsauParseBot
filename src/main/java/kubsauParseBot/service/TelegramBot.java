package kubsauParseBot.service;

import kubsauParseBot.config.BotConfig;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        if (update.hasCallbackQuery()) {
            long chat_id = update.getCallbackQuery().getMessage().getChatId();
            //handleCallback(chat_id, update.getCallbackQuery());
            startCommandReceived(chat_id, parseKubsau(update.getCallbackQuery().getData()));
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    //List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
                    List<InlineKeyboardButton> buttons = new ArrayList<>();
                    List directions = new ArrayList<>();
                    directions.add("ИТ (Очное)");
                    directions.add("ПИ (Очное)");
                    directions.add("ИТ (Заочное)");
                    directions.add("ПИ (Зачное)");

                    for (var el : directions) {
                        buttons.add(
                                InlineKeyboardButton.builder()
                                        .text(el.toString())
                                        .callbackData(el.toString())
                                        .build()
                        );
                    }

                    //parseKubsau();
                    //startCommandReceived(chat_id, parseKubsau(update.getCallbackQuery().getData()));

                    //startCommandReceived(chat_id, "Выбери направление");
                    try {
                        execute(
                                SendMessage.builder()
                                        .text("Выбери направление")
                                        .chatId(chat_id)
                                        .replyMarkup(InlineKeyboardMarkup.builder().keyboard(Collections.singleton(buttons)).build())
                                        .build());
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }


                    break;
                default:
                    sendMessage(chat_id, "You're wrong");
            }


        }
    }

    private void handleCallback(long chat_id, CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();
        String direction = callbackQuery.getData();
        sendMessage(chat_id, direction);

    }

    private String parseKubsau(String data) {
        String url;
        switch (data) {
            case "ИТ (Очное)":
                url = "https://kubsau.ru/upload/slpd/in_lists/main/139_09.04.02_000000869_%D0%94.html?0.8338041724968897";
                break;
            case "ПИ (Очное)":
                url = "https://kubsau.ru/upload/slpd/in_lists/main/143_09.04.03_000000875_%D0%94.html?0.2018514914147438";
                break;
            case "ИТ (Заочное)":
                url = "https://kubsau.ru/upload/slpd/in_lists/main/137_09.04.02_000000866_%D0%97.html?0.2775243817824298";
                break;
            case "ПИ (Зачное)":
                url = "https://kubsau.ru/upload/slpd/in_lists/main/179_09.04.03_000000917_%D0%97.html?0.942003177762937";
                break;
            default:
                url = "https://kubsau.ru/upload/slpd/in_lists/main/139_09.04.02_000000869_%D0%94.html?0.8338041724968897";
        }


        try {
            var document = Jsoup
                    .connect(url)
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
            String message = message(results);

            return message;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String[][] sortStringArray(String mas[][]) {
        for (int i = 0; i < mas.length; i++) {
            for (int j = 0; j < mas.length - 1; j++) {
                if (Integer.valueOf(mas[j][2]) < Integer.valueOf(mas[j + 1][2])) {
                    var temp = mas[j];
                    mas[j] = mas[j + 1];
                    mas[j + 1] = temp;
                }
            }
        }
        for (int i = 0; i < mas.length; i++) {
            mas[i][0] = String.valueOf(i + 1) + ".";
        }
        return mas;
    }

    private String message(String mas[][]) {
        String message = "";
        for (int i = 0; i < mas.length; i++) {
            message += mas[i][0] + " " + mas[i][1] + " " + mas[i][2] + " " + mas[i][3] + " " + mas[i][4] + "\n";
        }
        return message;
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

    private void startCommandReceived(long chatId, String list) {
        //String answer = "Hi, " + name + ", nice to meet you!";
        //String answer = list;
        sendMessage(chatId, list);
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
