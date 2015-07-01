package com.anrc.wirelessradio;

import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class RadioActivity extends Activity {

    private String tag = "Wireless Radio";

    private EditText port_device;
    private Switch radio_Switch;
    private SeekBar volumeSeekbar;
    private ProgressBar pb;
    private ImageView volume;
    private AudioManager audioManager;
    private startAudioThread myThread;
    private DatagramSocket audiosocket = null;

    private final int SAMPLE_RATE = 48000;
    private final int SAMPLE_CHANNEL = 2; // no. of samples
    private final int SAMPLE_SIZE = 2; // bytes per sample
    private final int BUF_SIZE = SAMPLE_RATE * SAMPLE_CHANNEL * SAMPLE_SIZE;

    private boolean audiothreadrunning = false;

    private int progressChanged;
    private View v;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

/*        ActionBar actionBar = getActionBar();
        actionBar.show();*/
        layout_init();


        //attach a listener to check for changes in state
        radio_Switch.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
            boolean isChecked)
            {

                if (isChecked)
                {

                    String radioportnum = port_device.getText().toString();
                    if (radioportnum.equals(""))
                    {
                        Toast.makeText(getBaseContext(), "Enter the audio port number", Toast.LENGTH_SHORT).show();
                        radio_Switch.setChecked(false);
                    }
                    else
                    {
                        if (!audiothreadrunning) {
                            startAudioThread myThread = new startAudioThread();
                            myThread.start();
                            radio_Switch.setText("Radio is ON");
                            pb.setVisibility(View.VISIBLE);
                            Log.d(tag, "Wireless radio started");
                            audiothreadrunning = true;
                        }
                        else
                            Toast.makeText(getBaseContext(), "Unexpected Error. Restart the Radio", Toast.LENGTH_SHORT).show();

                    }
                }
                else
                {
                    radio_Switch.setText("Radio is OFF");
                    if (audiothreadrunning)
                    {
                        myThread.shutdown();
                        pb.setVisibility(View.GONE);
                        Log.d(tag, "Wireless radio stopped");
                        audiothreadrunning = false;
                    }
                }

            }
        });

        /*
		 * Setting media volume for audio listening in android
		 */
        volumeSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch (SeekBar arg0){
            }

            @Override
            public void onStartTrackingTouch (SeekBar arg0){
            }

            @Override
            public void onProgressChanged (SeekBar arg0,int progresschange, boolean arg2)
            {
                if (progresschange == 0)
                {
                    volume.setImageResource(R.drawable.ic_action_volume_muted);
                }
                else
                {
                    volume.setImageResource(R.drawable.ic_action_volume_on);
                }
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progresschange, 0);
                Log.d(tag, "Wireless radio volume has changed.");
            }
        });
    }


//********************* Audio Starting thread **********************************************************
	/*
	 * start a thread for receiving the audio data
	 */
	 class startAudioThread extends Thread
	 {
        private boolean radiothreadstop = false, threadrun = true;
        @Override
        public void run()
        {
        	try
            {
        		if(threadrun)
                {
                    Log.d(tag, "Wireless radio 1");

                    audiothreadrunning = true;
        			String portnumber = port_device.getText().toString();

                    Log.d(tag, "Wireless radio 2");

                    AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
                            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, 
                            AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE, 
                            AudioTrack.MODE_STREAM);
                    track.play();

                    Log.d(tag, "Wireless radio 3");

                    Log.d(tag, "Wireless Radio initialization done");

                   audiosocket = new DatagramSocket(Integer.parseInt(portnumber));
					byte[] buf = new byte[BUF_SIZE];
                    Log.d(tag, "Wireless radio 4");

                    while(!radiothreadstop)
                    {
                        DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
                        audiosocket.receive(packet);
                        //  Log.d(tag, "Radio Audio recv pack: " + packet.getLength());
                        track.write(packet.getData(), 0, packet.getLength());
					}
                    Log.d(tag, "Wireless radio 5");

                }
			}
        	catch (SocketException e){
				Log.d(tag, "Radio Udp Socket Exception.");
			} 
        	catch (IOException e){
				Log.d(tag, "Radio Udp IOException.");
			}	
        	catch (Exception e){
				Log.d(tag, "Radio Udp Exception.");
			}	    	
        	finally
            {
    			Log.d(tag, "Radio Thread Exit safely..");
    			audiosocket.close();
        	}
        }

        public void shutdown()  {
            radiothreadstop = true;
            audiosocket.close();
            Log.d(tag, "Radio socket closed");
        }

	}
	
	/*
	 * Layout objects initialization
	 */
	  public void layout_init()
	 {
        port_device = (EditText) findViewById(R.id.ip_port);
        radio_Switch = (Switch) findViewById(R.id.radio_onoff);
		pb = (ProgressBar) findViewById(R.id.cspinner);
		volumeSeekbar = (SeekBar)findViewById(R.id.volumebar);
		volume = (ImageView) findViewById(R.id.volumeicon);		
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        radio_Switch.setChecked(false);
		volumeSeekbar.setEnabled(true);
		pb.setVisibility(View.GONE);

        int maxvolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volumeSeekbar.setMax(maxvolume);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxvolume, 0);
        int volumelevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeSeekbar.setProgress(volumelevel);

        if(volumelevel == 1){
			volume.setImageResource(R.drawable.ic_action_volume_muted);
		}
		else{
			volume.setImageResource(R.drawable.ic_action_volume_on);
		}
		myThread = new startAudioThread();
	 }


     @Override
     public void onResume(){
        super.onResume();

        // put your code here...

     }


     @Override
     public void onStop()
     {
        super.onStop();
        // put your code here...

        radio_Switch.setText("Radio is OFF");
        if (audiothreadrunning) {
            myThread.shutdown();
            pb.setVisibility(View.GONE);
            Log.d(tag, "Wireless radio stopped");
            audiothreadrunning = false;
        }

     }

}
