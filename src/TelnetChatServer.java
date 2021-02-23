import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;

public class TelnetChatServer {

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer buffer;
    private HashMap<String, SocketChannel> clientsList = new HashMap<>();

    public TelnetChatServer() throws IOException {
        buffer = ByteBuffer.allocate(64);
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();
        serverChannel.bind(new InetSocketAddress(8189));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, OP_ACCEPT);
        while (serverChannel.isOpen()) {
            selector.select(); // block
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = keys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                keyIterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        String nick = new String();
        SocketChannel channel = (SocketChannel) key.channel();
        int read = channel.read(buffer);
        if (read == -1) {
            return;
        }
        buffer.flip();
        StringBuilder msg = new StringBuilder();
        while (buffer.hasRemaining()) {
            msg.append((char) buffer.get());
        }
        buffer.clear();
        if (msg.toString().startsWith("/nick")) {
            nick = (msg.toString().trim().split(" "))[1];
            channel.write(ByteBuffer.wrap(("Вы зарегистрированны как: " + nick + "\n").getBytes(StandardCharsets.UTF_8)));
            clientsList.put(nick, channel);
        } else if (msg.toString().startsWith("/w")) {
            ArrayList<String> privatMsg = new ArrayList<String>(Arrays.asList(msg.toString().trim().split(" ")));
            String privateMsgRecipient = privatMsg.get(1);
            privatMsg.remove(0);
            privatMsg.remove(0);
            StringBuilder clearPrivateMsg = new StringBuilder();
            for (String i : privatMsg) {
                clearPrivateMsg.append(i + " ");
            }
            for (Map.Entry<String, SocketChannel> entry : clientsList.entrySet()) {
                if (entry.getKey().equals(privateMsgRecipient)) {
                    entry.getValue().write(ByteBuffer.wrap(("Приватное сообщение от "+nick+": "+clearPrivateMsg.toString()).getBytes(StandardCharsets.UTF_8)));
                }
            }
        } else {
            System.out.println("received: " + msg);
            for (Map.Entry<String, SocketChannel> entry : clientsList.entrySet()) {
                if (entry.getKey().equals(nick)==false) {
                    entry.getValue().write(ByteBuffer.wrap((msg.toString()).getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = channel.accept();
        socketChannel.write(ByteBuffer.wrap(
                "Введите Ваш ник используя команду /nick...\n".getBytes(StandardCharsets.UTF_8)));
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, OP_READ);
    }

    public static void main(String[] args) throws IOException {
        new TelnetChatServer();
    }
}
