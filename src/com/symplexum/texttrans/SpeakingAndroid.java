package com.symplexum.texttrans;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.View;
import android.widget.EditText;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.content.Intent;
import java.util.Locale;
import android.widget.Toast;

import edu.gvsu.masl.asynchttp.ConnectionManager;
import edu.gvsu.masl.asynchttp.HttpConnection;

public class SpeakingAndroid extends Activity implements OnClickListener, OnInitListener {
	
		//TTS object
	private TextToSpeech myTTS;
		//status check code
	private int MY_DATA_CHECK_CODE = 0;
	
		//create the Activity
	public void onCreate(Bundle savedInstanceState) {
	
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        	
        		//get a reference to the button element listed in the XML layout
        	Button speakButton = (Button)findViewById(R.id.talk);
        		//listen for clicks
        	speakButton.setOnClickListener(this);
     
			//check for TTS data
	        Intent checkTTSIntent = new Intent();
	        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
	        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);
	}
	
		//respond to button clicks
	public void onClick(View v) {
			//change language engine if necessary
			onInit(TextToSpeech.SUCCESS);
			//get the text entered
	    	TextView enteredText = (TextView)findViewById(R.id.textView2);
	    	String words = enteredText.getText().toString();
	    	Toast.makeText(this, words, Toast.LENGTH_SHORT).show();
	    	speakWords(words);
	}
	
		//speak the user text
	private void speakWords(String speech) {

			//speak straight away
	    	myTTS.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
	}
	
		//act on result of TTS data check
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	
		if (requestCode == MY_DATA_CHECK_CODE) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				//the user has the necessary data - create the TTS
			myTTS = new TextToSpeech(this, this);
			}
			else {
					//no data - install it now
				Intent installTTSIntent = new Intent();
				installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installTTSIntent);
			}
		}
	}

		//setup TTS
	public void onInit(int initStatus) {
	
			//check for successful instantiation
		if(initStatus == TextToSpeech.SUCCESS){
		
		Spinner langspin = (Spinner) findViewById(R.id.langspinner);
		String lang = String.valueOf(langspin.getSelectedItem());
		
	
		if (lang.equals("Spanish")) {
			Locale spanish = new Locale ("es", "ES");
			myTTS.setLanguage(spanish);
	//		Toast.makeText(this, "S", Toast.LENGTH_SHORT).show();
		}
		else if (lang.equals("French")) {
			Locale french = new Locale ("fr", "FR");
			myTTS.setLanguage(french);
	//		Toast.makeText(this, "F", Toast.LENGTH_SHORT).show();
		}
		else if (lang.equals("German")) {
			Locale german = new Locale ("de", "DE");
			myTTS.setLanguage(german);
	//		Toast.makeText(this, "F", Toast.LENGTH_SHORT).show();
		}
		else if (lang.equals("Italian")) {
			Locale italian = new Locale ("it", "IT");
			myTTS.setLanguage(italian);
	//		Toast.makeText(this, "F", Toast.LENGTH_SHORT).show();
		}
	
		
		//Toast.makeText(this, (String)myTTS.getLanguage().toString()+lang, Toast.LENGTH_SHORT).show();
		
		}
		else if (initStatus == TextToSpeech.ERROR) {
			Toast.makeText(this, "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
		}
	}
		public void postTrans(View view){
			//called by translate button
			
			TextView enteredText = (TextView)findViewById(R.id.enter);
	    	String words = enteredText.getText().toString();
			words = words.replaceAll(" ", "%20");
			Spinner langspin = (Spinner) findViewById(R.id.langspinner);
			String lang = String.valueOf(langspin.getSelectedItem());
			
			if(lang.equals("Spanish")){
				post.get("http://api.microsofttranslator.com/V2/Ajax.svc/Translate?appId=78280AF4DFA1CE1676AFE86340C690023A5AC139&from=en&to=es&text="+words);
				Toast.makeText(this, "postspan", Toast.LENGTH_LONG).show();

			}
			else if(lang.equals("French")){
				post.get("http://api.microsofttranslator.com/V2/Ajax.svc/Translate?appId=78280AF4DFA1CE1676AFE86340C690023A5AC139&from=en&to=fr&text="+words);	
				Toast.makeText(this, "postfr", Toast.LENGTH_LONG).show();

			}
			else if(lang.equals("German")){
				post.get("http://api.microsofttranslator.com/V2/Ajax.svc/Translate?appId=78280AF4DFA1CE1676AFE86340C690023A5AC139&from=en&to=de&text="+words);
				Toast.makeText(this, "postde", Toast.LENGTH_LONG).show();

			}
			else if(lang.equals("Italian")){
				post.get("http://api.microsofttranslator.com/V2/Ajax.svc/Translate?appId=78280AF4DFA1CE1676AFE86340C690023A5AC139&from=en&to=it&text="+words);
				Toast.makeText(this, "postit", Toast.LENGTH_LONG).show();

			}
			
		}
		
		Handler handler = new Handler() {
			  public void handleMessage(Message message) {
			    switch (message.what) {
			    case HttpConnection.DID_START:
			     // text.setText("Starting connection...");
			      break;
			    case HttpConnection.DID_SUCCEED:
					Toast.makeText(SpeakingAndroid.this, "succeed", Toast.LENGTH_LONG).show();
			      String response = (String) message.obj;
			      Log.i("rev", response);
			      TextView text= (TextView)findViewById(R.id.textView2);
			      text.setText(response);
			      text.invalidate();
			      break;
			    case HttpConnection.DID_ERROR:
			      Exception e = (Exception) message.obj;
			      e.printStackTrace();
			    break;
			    }
			  }
			};
			HttpConnection post = new HttpConnection(handler);
}


