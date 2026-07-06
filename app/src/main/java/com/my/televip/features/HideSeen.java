package com.my.televip.features;

import com.my.televip.Callback.IntCallback;
import com.my.televip.Class.ClassNames;
import com.my.televip.ClientChecker;
import com.my.televip.Configs.ConfigManager;
import com.my.televip.application.AndroidUtilities;
import com.my.televip.Class.ClassLoad;
import com.my.televip.logging.Logger;
import com.my.televip.obfuscate.AutomationResolver;
import com.my.televip.virtuals.SQLite.SQLiteCursor;
import com.my.televip.virtuals.messenger.MessagesController;
import com.my.televip.virtuals.messenger.MessagesStorage;
import com.my.televip.virtuals.messenger.UserConfig;
import com.my.televip.virtuals.messenger.Utilities;
import com.my.televip.virtuals.tgnet.ConnectionsManager;
import com.my.televip.virtuals.tgnet.RequestDelegate;
import com.my.televip.virtuals.tgnet.TLRPC;

import de.robv.android.xposed.XposedHelpers;

public class HideSeen {


    public static Object TLChannels_readHistory;
    public static Object TLMessages_readHistory;

    public static void sendFakeReadResponse(Object onCompleteOrig) {
        try {
            TLRPC.TL_messages_affectedMessages fakeRes = new TLRPC.TL_messages_affectedMessages();
            fakeRes.setPts(-1);
            fakeRes.setPtsCount(0);
            RequestDelegate onComplete = new RequestDelegate(onCompleteOrig);
            Utilities.getStageQueue().postRunnable(() -> {
                try {
                    if (onComplete.requestDelegate != null) {
                        onComplete.run(fakeRes.getTL_messages_affectedMessages(), null);
                    }
                } catch (Throwable e) {
                    Logger.e(e);
                }
            });
        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    public static boolean isTLMessagesReadHistoryRequest(Object object) {
        if (object.getClass().getName().contains("TL_messages_readHistory")) return true;
        return object.getClass().getName().equals(AutomationResolver.resolve(ClassNames.TL_MESSAGES_READ_HISTORY));
    }

    public static boolean isTLChannelsReadHistoryRequest(Object object) {
        if (object.getClass().getName().contains("TL_channels_readHistory")) return true;
        return object.getClass().getName().equals(AutomationResolver.resolve(ClassNames.TL_CHANNELS_READ_HISTORY));
    }

    public static boolean isReadMessageRequest(Object object) {
        String className = object.getClass().getName();
        Class<?> objectClass = object.getClass();
        if (className.contains("TL_messages_readHistory") ||
                className.contains("TL_messages_readEncryptedHistory") ||
                className.contains("TL_messages_readDiscussion") ||
                className.contains("TL_messages_readMessageContents") ||
                className.contains("TL_channels_readMessageContents") ||
                className.contains("TL_channels_readHistory")) return true;

        return objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_READ_HISTORY))) ||
                objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_READ_ENCRYPTED_HISTORY))) ||
                objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_READ_DISCUSSION))) ||
                objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_READ_MESSAGE_CONTENTS))) ||
                objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_CHANNELS_READ_MESSAGE_CONTENTS))) ||
                objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_CHANNELS_READ_HISTORY)));
    }

    public static void saveReadHistory(Object object) {
        if (HideSeen.TLChannels_readHistory == null && HideSeen.isTLChannelsReadHistoryRequest(object)) {
            HideSeen.TLChannels_readHistory = object;
        } else if (HideSeen.TLMessages_readHistory == null && HideSeen.isTLMessagesReadHistoryRequest(object)) {
            HideSeen.TLMessages_readHistory = object;
        }
    }

    public static void handleReadAfterSend(Object object) {
        try {
            if (ConfigManager.hideSeen.isEnable() && ConfigManager.markReadAfterSend.isEnable()) {
                TLRPC.InputPeer peer = extractPeerFromSendObject(object);

                if (peer != null && peer.inputPeer != null) {
                    Long dialogId = getDialogId(peer);
                    MessagesStorage messagesStorage = getMessagesStorage();
                    messagesStorage.getStorageQueue().postRunnable(() ->
                            getDialogMaxMessageId(messagesStorage, dialogId, (param -> markReadOnServer(param, peer))));
                }
            }
        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    public static void getDialogMaxMessageId(MessagesStorage messagesStorage, long dialog_id, IntCallback callback) {
        messagesStorage.getStorageQueue().postRunnable(() -> {
            SQLiteCursor cursor = null;
            int[] max = new int[1];
            try {
                cursor = messagesStorage.getDatabase().queryFinalized("SELECT MAX(mid) FROM messages_v2 WHERE uid = " + dialog_id, new Object[]{});
                if (cursor.next()) {
                    max[0] = cursor.intValue(0);
                }
            } catch (Throwable e) {
                Logger.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
            AndroidUtilities.runOnUIThread(() -> callback.run(max[0]));
        });
    }

    public static boolean isReadMessages = false;

    public static void markReadOnServer(int messageId, TLRPC.InputPeer peer) {
        try {
            Object req;

            if (peer.inputPeer.getClass().getName().contains("TL_inputPeerChannel") || peer.inputPeer.getClass().getName().equals(AutomationResolver.resolve(ClassNames.TL_INPUT_PEER_CHANNEL))) {
                TLRPC.TL_channels_readHistory request;
                if (!ClientChecker.check(ClientChecker.ClientType.Nagram)) {
                    request = new TLRPC.TL_channels_readHistory();
                    request.setChannel(MessagesController.getInputChannel(peer));
                } else {
                    request = new TLRPC.TL_channels_readHistory(TLChannels_readHistory);
                    request.setChannel(MessagesController.getInputChannel(getDialogId(peer)));
                }
                request.setMax_id(messageId);
                req = request.getTL_channels_readHistory();
            } else {
                TLRPC.TL_messages_readHistory request;
                if (!ClientChecker.check(ClientChecker.ClientType.Nagram)) {
                    request = new TLRPC.TL_messages_readHistory();
                } else {
                    request = new TLRPC.TL_messages_readHistory(TLMessages_readHistory);
                }
                request.setPeer(peer);
                request.setMax_id(messageId);
                req = request.getTL_messages_readHistory();
            }

            isReadMessages = true;
            getConnectionsManager().sendRequest(req, RequestDelegate.run((response, error) -> {
                if (error == null) {
                    if (ClassLoad.getClass(ClassNames.TL_MESSAGES_AFFECTED).isInstance(response)) {
                        TLRPC.TL_messages_affectedMessages res = new TLRPC.TL_messages_affectedMessages(response);
                        if (!ClientChecker.check(ClientChecker.ClientType.Nagram)) {
                            getMessagesController().processNewDifferenceParams(-1, res.getPts(), -1, res.getPtsCount());
                        } else {
                            getMessagesController().processNewDifferenceParams(res.getPts(), -1, res.getPtsCount());
                        }
                    }

                }
            }));
        } catch (Throwable e) {
            Logger.e(e);
        }
    }

    private static TLRPC.InputPeer extractPeerFromSendObject(Object object) {
        String className = object.getClass().getName();
        Class<?> objectClass = object.getClass();

        if (className.contains("TL_messages_sendMessage") ||
                className.contains("TL_messages_sendMedia") ||
                className.contains("TL_messages_sendReaction") ||
                className.contains("TL_messages_sendPaidReaction") ||
                className.contains("TL_messages_sendMultiMedia")) {
            return new TLRPC.InputPeer(getPeer(object));
        }
        if (objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_SEND_MESSAGE))) ||
                objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_SEND_MEDIA))) ||
                objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_SEND_REACTION))) ||
                objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_SEND_PAID_REACTION))) ||
                objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_SEND_MULTI_MEDIA)))) {
            return new TLRPC.InputPeer(getPeer(object));
        }
        return null;
    }

    private static Object getPeer(Object msg) {
        Class<?> objectClass = msg.getClass();
        String msgName = null;
        if (objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_SEND_MESSAGE)))) msgName = "TLRPC$TL_messages_sendMessage";
        if (objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_SEND_MEDIA)))) msgName = "TLRPC$TL_messages_sendMedia";
        if (objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_SEND_REACTION)))) msgName = "TLRPC$TL_messages_sendReaction";
        if (objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_SEND_PAID_REACTION)))) msgName = "TLRPC$TL_messages_sendPaidReaction";
        if (objectClass.equals(ClassLoad.getClass(AutomationResolver.resolve(ClassNames.TL_MESSAGES_SEND_MULTI_MEDIA)))) msgName = "TLRPC$TL_messages_sendMultiMedia";

        return XposedHelpers.getObjectField(msg, AutomationResolver.resolve(msgName, "peer", AutomationResolver.ResolverType.Field));
    }

    public static Long getDialogId(TLRPC.InputPeer peer) {
        long dialogId;
        if (peer.getChat_id() != 0) {
            dialogId = -peer.getChat_id();
        } else if (peer.getChannel_id() != 0) {
            dialogId = -peer.getChannel_id();
        } else {
            dialogId = peer.getUser_id();
        }

        return dialogId;
    }

    public static MessagesStorage getMessagesStorage() {
        return MessagesStorage.getInstance(UserConfig.getSelectedAccount());
    }

    public static ConnectionsManager getConnectionsManager() {
        return ConnectionsManager.getInstance(UserConfig.getSelectedAccount());
    }

    public static MessagesController getMessagesController() {
        return MessagesController.getInstance(UserConfig.getSelectedAccount());
    }

}
