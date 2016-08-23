package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    final String REMOTE_PORTS[] = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;
    int count = 0;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    static int Spg=0;
    static int avdId;
    static SparseIntArray sequenceVector = new SparseIntArray();
    Socket clientSockets[]= new Socket[5];
    static PriorityQueue<Message> delivaryQueue = new PriorityQueue<Message>(10, new MessageComparator());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        avdId = Integer.parseInt(portStr);
        sequenceVector.put(5554, 0);
        sequenceVector.put(5556, 0);
        sequenceVector.put(5558, 0);
        sequenceVector.put(5560, 0);
        sequenceVector.put(5562, 0);
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT, 25);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, e.toString());

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        Log.v(TAG, "send is pressed");

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        final EditText editText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Message message = new Message();
            int clientAvdId=0;
            Message tempMessage;
            Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
            int proposedPriority;
            ObjectInputStream inputStream;
            ObjectOutputStream outputStream;
            Iterator<Message> delivaryQueueIterator;
            while(true) {
                Socket socket = null;
                try {

                    socket = serverSocket.accept();

                    //Initialize input and output stream for full duplex sockect connection
                    inputStream = new ObjectInputStream(socket.getInputStream());
                    outputStream = new ObjectOutputStream(socket.getOutputStream());

                    //read the object from input stream
                    message = (Message)inputStream.readObject();
                    clientAvdId = message.getAvdId();
                    Log.e(TAG,"Client AVD ID : " + clientAvdId);
                    proposedPriority = sequenceVector.get(message.getAvdId())+1;
                    if(proposedPriority > message.getInitialPriority()){
                        message.setProposedPriority(proposedPriority);
                        message.setAvdId(avdId);
                    }else{
                        message.setProposedPriority(message.getInitialPriority());
                    }
                    message.setDeliverable(false);
                    delivaryQueue.add(message);
                    outputStream.writeObject(message);
                    outputStream.flush();


                    message = (Message)inputStream.readObject();
                    delivaryQueueIterator = delivaryQueue.iterator();
                    while(delivaryQueueIterator.hasNext()){
                        tempMessage = delivaryQueueIterator.next();
                        if(tempMessage.getInitialPriority() == message.getInitialPriority()
                                && tempMessage.getAvdId() == message.getAvdId()){
                            delivaryQueue.remove(tempMessage);
                            message.setDeliverable(true);
                            delivaryQueue.add(message);
                            break;
                        }
                    }
                    while(delivaryQueue.peek()!=null && delivaryQueue.peek().isDeliverable()){
                        message = delivaryQueue.poll();
                        ContentResolver contentResolver = getContentResolver();
                        int key = count++;
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(KEY_FIELD, Integer.toString(key));
                        contentValues.put(VALUE_FIELD, message.getMessage());
                        contentResolver.insert(mUri, contentValues);
                        Log.e(TAG, "Message Delivered : " + message.toString());
                    }

                    publishProgress(message.getMessage(), Integer.toString(message.getAgreedPriority()));

                }catch (EOFException e){
                    e.printStackTrace();

                    Log.e(TAG, "ClientTask socket EOFException");
                }
                catch (StreamCorruptedException e){
                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket StreamCorruptedException");
                }
                catch (SocketTimeoutException e){

                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket SocketTimeoutException");
                }
                catch (IOException e) {

                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket IOException");
                }
                catch (Exception e){
                    e.printStackTrace();
                    Log.e(TAG,"error in reading from socket");
                }
                finally{
                    cleanUp(clientAvdId);
                }

            }
        }
        protected void cleanUp(int clientAvdId){
            Iterator<Message> delivaryQueueIterator = delivaryQueue.iterator();
            Message tempMessage;
            while(delivaryQueueIterator.hasNext()){
                tempMessage = delivaryQueueIterator.next();
                if(tempMessage.getAvdId() == clientAvdId) {
                    delivaryQueue.remove(tempMessage);
                }
            }
        }

        @Override
        protected void onProgressUpdate(String... messages) {


            /*
             * The following code displays what is received in doInBackground().
             */
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(messages[0] + " : " + messages[1] + "\n");
            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            Spg++;
            String messageToSend = null;
            Socket sockets[] = new Socket[5];

            int count=0;
            ObjectOutputStream outPutStreams[] = new ObjectOutputStream[5];
            ObjectInputStream inputStreams[] = new ObjectInputStream[5];
            //used to keep the count of replies
            int responseCount=0;
            //to store responses from other machines
            List<Message> messageList = new ArrayList<Message>();
            ObjectOutputStream out;
            ObjectInputStream in;
            Socket socket;
            Message message;
            for (String remotePort: REMOTE_PORTS) {
                try {
                    sockets[count] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(remotePort));
                    socket = sockets[count];
                    outPutStreams[count] = new ObjectOutputStream(socket.getOutputStream());
                    inputStreams[count] = new ObjectInputStream(socket.getInputStream());
                    out = outPutStreams[count];
                    in = inputStreams[count];
                    socket.setSoTimeout(2000);
                    messageToSend = msgs[0];
                    message = new Message(messageToSend,Spg,MessageType.InitialMessage, avdId);
                    Log.e(TAG, "Message to send is : " + message.toString());
                    out.writeObject(message);
                    out.flush();
                    Log.e(TAG, "************************Receiving Message******************");
                    message = (Message)in.readObject();
                    messageList.add(message);
                    Log.e(TAG, "Recieved Message : " + message.toString());
                    count++;

                }catch (UnknownHostException e) {
                    e.printStackTrace();
                    Log.e(TAG, "ClientTask UnknownHostException");
                }catch (EOFException e){
                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket EOFException");
                }
                catch (StreamCorruptedException e){
                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket StreamCorruptedException");
                }
                catch (SocketTimeoutException e){
                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket SocketTimeoutException");
                }
                catch (IOException e) {

                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket IOException");
                }catch (Exception e){
                    e.printStackTrace();
                    Log.e(TAG, "Exception Encountered");

                }finally {
                    //clean up after exception occurred

                }

            }

            Message finalMessage=new Message(messageList.get(0));
            int agreedPriority = Spg;
            for(Message messageInList: messageList) {
                if (messageInList.getProposedPriority() > agreedPriority) {
                    agreedPriority = messageInList.getProposedPriority();
                    finalMessage=messageInList;
                }
            }

            Spg=agreedPriority;

            finalMessage.setAgreedPriority(agreedPriority);
            Log.e(TAG,"Message with agreed Sequence (client log) : " + finalMessage.toString());

            for(int i=0;i<5;i++){
                try{
                    socket = sockets[i];
                    out = outPutStreams[i];
                    out.writeObject(finalMessage);
                    out.flush();
                    socket.close();
                }catch (UnknownHostException e) {
                    e.printStackTrace();
                    Log.e(TAG, "ClientTask UnknownHostException");
                }catch (EOFException e){
                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket EOFException");
                }
                catch (StreamCorruptedException e){
                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket StreamCorruptedException");
                }
                catch (SocketTimeoutException e){
                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket SocketTimeoutException");
                }
                catch (IOException e) {

                    e.printStackTrace();
                    Log.e(TAG, "ClientTask socket IOException");
                }catch (Exception e){
                    e.printStackTrace();
                    Log.e(TAG, "Exception Encountered");

                }finally{
                    //clean up after exception occurred

                }
            }


            Log.e(TAG,"Message List : " + messageList.toString());
            return null;
        }
    }

}
