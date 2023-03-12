package org.piki;

import com.sun.mail.imap.IMAPStore;
import jakarta.mail.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
/**
 * Hello world!
 *
 */
public class App
{

    static String user = null;

    static String password = null;

    static String host = null;

    static String src = null;

    static String dest = null;

    static Date maxDate = null;

    static boolean ifId = false;

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (i == 0) {
                host = arg;
            } else if (i == 1) {
                user = arg;
            } else if (i == 2) {
                password = arg;
            } else if ("-s".equals(arg)) {
                src = args[++i];
            } else if ("-d".equals(arg)) {
                dest = args[++i];
            } else if ("-D".equals(arg)) {
                try {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    maxDate = simpleDateFormat.parse(args[++i]);
                } catch (ParseException e) {
                    System.out.println("date format: yyyy-MM-dd");
                    System.exit(1);
                }
            } else if ("-id".equals(arg)) {
                ifId = true;
            } else {
                System.out.println("Usage: host user password [-s source mbox] [-d max date] [-id]");
                System.out.println("\t date format: yyyy-MM-dd");
                System.exit(1);
            }
        }

        Map<String, String> iam = new HashMap<>();
        //imap id info
        iam.put("name", user);
        iam.put("version", "1.0.0");
        iam.put("vendor", "client");
        iam.put("support-email", user);

        Properties properties = new Properties();
        Session session = Session.getInstance(properties, null);

        IMAPStore store = null;
        try {
            store = (IMAPStore) session.getStore("imap");

        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
        try {
            if (user != null || password != null || host != null) {
                store.connect(host, user, password);
            } else {
                store.connect();
            }

            if(ifId){
                store.id(iam);
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        if (src == null || dest == null || maxDate == null) {
            listFolder(store);
        } else {
            Folder folder = store.getFolder(src);
            folder.open(Folder.READ_WRITE);
            try {
                if (!folder.exists()) {
                    throw new Exception("action move, source not exist");
                }

                List<Message> messageList = getMsgList(store, folder, maxDate);
                int deleted = move(store, folder, dest, messageList);
                System.out.println("delete " + deleted + " messages");
            } finally {
                folder.close();
            }

        }


    }

    public static void listFolder(IMAPStore store) throws MessagingException {
        Folder defaultFolder = store.getDefaultFolder();
        Folder[] allFolder = defaultFolder.list();
        System.out.println("folders: ");
        for (Folder folder : allFolder) {
            int count = folder.getMessageCount();
            System.out.println("    " + folder.getName() + "(" + count + ")");
        }
    }

    public static List<Message> getMsgList(Store store, Folder folder, Date maxDate) throws Exception {

        // filter
        Message[] msgs = folder.getMessages();
        List<Message> msgList = new ArrayList<>();
        for (Message msg : msgs) {
            if (msg.getSentDate() == null || maxDate.compareTo(msg.getSentDate()) > 0) {
                msgList.add(msg);
            }
        }

        return msgList;

    }

    public static int move(Store store, Folder sFolder, String dest, List<Message> moveMsgList) throws Exception {
        if (moveMsgList.size() == 0) {
            return 0;
        }

        int srcMsgCount = sFolder.getMessageCount();
        if (srcMsgCount == 0) {
            return 0;
        }

        Folder destFolder = store.getFolder(dest);
        if (!destFolder.exists()) {
            destFolder.create(Folder.HOLDS_MESSAGES);
        }

        // move
        Message[] moveMsgArray = moveMsgList.toArray(new Message[0]);
        sFolder.copyMessages(moveMsgArray, destFolder);
        sFolder.setFlags(moveMsgArray, new Flags(Flags.Flag.DELETED), true);

        return moveMsgList.size();

    }


}
