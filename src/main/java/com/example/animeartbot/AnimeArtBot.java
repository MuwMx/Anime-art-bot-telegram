package com.example.animeartbot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AnimeArtBot extends TelegramLongPollingBot {
    private final GoogleCustomSearchClient googleCustomSearchClient = new GoogleCustomSearchClient();
    private final Map<Long, String> userSites = new HashMap<>();
    private final Map<String, String> fileLinks = new HashMap<>();



    //отдельные ключи для поисковиков ОЧЕНЬ ВАЖНО
    private static final String DANBOORU = "e1880fa03a94f4dc0";
    private static final String ZEROCHAN = "820a11e93fa334e27";
    private static final String GLOBAL = "521df7b00515b479b";

    @Override
    public String getBotUsername() {
        return "@anim_art_bot";
    }

    @Override
    public String getBotToken() {
        return "7358373917:AAGI8VKSEPkVCPi87LEalGjAgJCEesu0KTE";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                sendSiteSelectionKeyboard(chatId);
            } else if (messageText.equals("Global🌍") || messageText.equals("Danbooru❤️‍🔥") || messageText.equals("Zerochan🩷")) {
                String site = GLOBAL;
                String siteName = "Global🌍";
                if (messageText.equals("Danbooru❤️‍🔥")) {
                    site = DANBOORU;
                    siteName = "Danbooru❤️‍🔥";
                } else if (messageText.equals("Zerochan🩷")) {
                    site = ZEROCHAN;
                    siteName = "Zerochan🩷";
                }
                userSites.put(chatId, site);
                sendSiteConfirmation(chatId, siteName);
                removeKeyboard(chatId);
            } else {
                String site = userSites.getOrDefault(chatId, GLOBAL);
                sendAnimeArtByKeyword(chatId, messageText, site);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.startsWith("file_")) {
                String fileLink = fileLinks.get(callbackData);
                sendFile(chatId, fileLink);
            }
        }
    }

    private void sendSiteSelectionKeyboard(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Виберіть сайт для пошуку аніме-артів:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Global🌍"));
        row1.add(new KeyboardButton("Danbooru❤️‍🔥"));
        row1.add(new KeyboardButton("Zerochan🩷"));

        keyboard.add(row1);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendSiteConfirmation(long chatId, String siteName) {
        String confirmationMessage;
        if (siteName.equals("Global🌍")) {
            confirmationMessage = "Ви обрали Global🌍, це пошук у всьому інтернеті можна шукати будь які фото.\nАле все ж краще використовувати Danbooru або Zerochan для пошуку аніме-артів😊";
        } else {
            confirmationMessage = "Ви обрали " + siteName + " для пошуку аніме-артів.";
        }
        sendMessage(chatId, confirmationMessage);
    }

    private void removeKeyboard(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Введіть запит\n\n" + "(Для зміни системи пошуку натисніть /start)");
        ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove(true);
        message.setReplyMarkup(removeKeyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendAnimeArtByKeyword(long chatId, String query, String site) {
        try {
            List<GoogleCustomSearchClient.Result> results = googleCustomSearchClient.searchAnimeArt(query, site);
            if (!results.isEmpty()) {
                GoogleCustomSearchClient.Result result = results.get(new Random().nextInt(results.size()));
                String imageUrl = result.getImageUrl();
                String originalLink = result.getOriginalLink();

                SendPhoto photoMessage = new SendPhoto();
                photoMessage.setChatId(String.valueOf(chatId));
                photoMessage.setPhoto(new InputFile(imageUrl));

                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                InlineKeyboardButton originalButton = new InlineKeyboardButton();
                originalButton.setText("Оригінал");
                originalButton.setUrl(originalLink);
                rowInline.add(originalButton);

                String callbackData = "file_" + System.currentTimeMillis();
                fileLinks.put(callbackData, imageUrl);
                InlineKeyboardButton fileButton = new InlineKeyboardButton();
                fileButton.setText("Завантажити файл");
                fileButton.setCallbackData(callbackData);
                rowInline.add(fileButton);

                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);

                photoMessage.setReplyMarkup(markupInline);
                execute(photoMessage);
            } else {
                sendMessage(chatId, "Не знайдено зображень за запитом: " + query);
            }
        } catch (IOException e) {
            sendMessage(chatId, "Помилка під час пошуку зображень: " + e.getMessage());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendFile(long chatId, String fileLink) {
        SendDocument documentMessage = new SendDocument();
        documentMessage.setChatId(String.valueOf(chatId));
        documentMessage.setDocument(new InputFile(fileLink));
        try {
            execute(documentMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}