package exampleclient;


import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.senior_project.group_1.mobilesr.R;
import com.senior_project.group_1.mobilesr.activities.ExampleServerPage;


/*Display Activity with sending messages to server*/

@SuppressLint("NewApi")
public class ExampleServerActivity extends Activity
{
    private ListView mList;
    private ArrayList<String> arrayList;
    private Adapter mAdapter;
    private TCPClient mTcpClient = null;
    private connectTask conctTask = null;
    private String ipAddressOfServerDevice;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_tcp);

        ipAddressOfServerDevice = ExampleServerPage.getServerIp();

        arrayList = new ArrayList<String>();

        final EditText editText = (EditText) findViewById(R.id.editText);
        Button send = (Button)findViewById(R.id.send_button);

        //relate the listView from java to the one created in xml
        mList = (ListView)findViewById(R.id.list);
        mAdapter = new Adapter(this, arrayList);
        mList.setAdapter(mAdapter);

        mTcpClient = null;
        // connect to the server
        conctTask = new connectTask();
        conctTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = editText.getText().toString();
                //add the text in the arrayList
                arrayList.add("Android Client: " + message);
                //sends the message to the server
                if (mTcpClient != null)
                {
                    mTcpClient.sendMessage(message);
                }
                //refresh the list
                mAdapter.notifyDataSetChanged();
                editText.setText("");
            }
        });
    }

    /*receive the message from server with asyncTask*/
    public class connectTask extends AsyncTask<String,String,TCPClient> {
        @Override
        protected TCPClient doInBackground(String... message)
        {
            //create a TCPClient object and
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived()
            {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message)
                {
                    try
                    {
                        //this method calls the onProgressUpdate
                        publishProgress(message);
                        if(message!=null)
                        {
                            System.out.println("Return Message from Socket::::: >>>>> "+message);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            },ipAddressOfServerDevice);
            mTcpClient.run();
            if(mTcpClient!=null)
            {
                mTcpClient.sendMessage("Initial Message when connected with Socket Server");
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            //in the arrayList we add the messaged received from server
            arrayList.add(values[0]);

            // notify the adapter that the data set has changed. This means that new message received
            // from server was added to the list
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy()
    {
        try
        {
            System.out.println("onDestroy.");
            mTcpClient.sendMessage("bye");
            mTcpClient.stopClient();
            conctTask.cancel(true);
            conctTask = null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        super.onDestroy();
    }


}
