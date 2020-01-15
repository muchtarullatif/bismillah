package com.latihan.firebase;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity {

    private Button btn;

    private String suara, info;
    private Toolbar toolbar;

    private DatabaseReference mDatabaseReference;  //firebase Database

    private TextToSpeech myTTS;  //Text To Speech

    protected static  final int RESULT_SPEECH =1;

    //Motion Sensor
    private SensorManager sensorManager;
    private Sensor mySensor;

    //Rumus Sensor Motion, Marai ngelu
    private float acelVal;
    private float acelLast;
    private float shake;

    private int active = 0;  //Indikator Recognize Speech

    private String urlRadio="http://111.68.27.245:8020/";  //imelda_FM
//    private String urlRadio="http://gajahmada.i.streaming.id:8000/live"; //gajahmada_FM
    private MediaPlayer mediaPlayer;

    private String urlWeb="https://www.tribunnews.com/news";  //tribunnews.com

    private Vibrator getar;  //Marai geter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        info = "Aplikasi ini dibuat guna membantu tuna netra dalam menjalani aktifitas sehari-hari. Aplikasi ini dapat melakukan " +
                "beberapa hal misalnya menyalakan alat elektronik yang ada di rumah yang tersambung dengan IoT, memutar radio, " +
                "membaca berita, dan mengetahui jam maupun tanggal saat ini. Aplikasi ini menggunakan suara sebagai inputannya, " +
                "jadi para tuna netra akan mudah dalam menjalankannya. Untuk memulai menginputkan suara, " +
                "pertama kalian yang harus dilakukan adalah menggoyang hp kalian ke kanan dan ke kiri (shake) 4-5 kali. " +
                "Kemudian akan muncul pop-up dan aplikasi mulai mendengarkan instruksi suara." +
                "Beberapa instruksi yang dapat digunakan di aplikasi ini adalah sebagai berikt :" +
                "                “nyalakan lampu”, untuk menyalakan lampu. “nyalakan kipas”, untuk menyalakan kipas. " +
                "“nyalakan air”, untuk menyalakan kran air. “nyalakan radio”, untuk streaming radio Imelda fm. “jam”, " +
                "untuk mengetahui jam sekarang. “tanggal”, untuk mengetahui tanggal sekarang. “baca berita populer”, " +
                "untuk mengetahui 5 berita popular yang ada di tribunnews. “baca berita pertama”, untuk membaca berita pertama. " +
                "“baca berita kedua”, untuk membaca berita kedua. “baca berita ketiga”, untuk membaca berita ketiga. " +
                "“baca berita ke-4”, untuk membaca berita keempat. “baca berita ke-5”, untuk membaca berita kelima.";

        mDatabaseReference = FirebaseDatabase.getInstance().getReference("Voice");
        toolbar = findViewById(R.id.toolbar);

        toolbar.setOverflowIcon(getDrawable(R.drawable.ic_info));
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        mySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorListener, mySensor, SensorManager.SENSOR_DELAY_NORMAL);

        acelVal = SensorManager.GRAVITY_EARTH;
        acelLast = SensorManager.GRAVITY_EARTH;
        shake = 0.00f;

        mediaPlayer = new MediaPlayer();

        getar = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        btn = findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myExecute();
            }
        });

        textToSpeech();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){

            case RESULT_SPEECH:{
                if(resultCode == RESULT_OK && null !=data){
                    final ArrayList <String> MasukkanSuaraAnda = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    suara = MasukkanSuaraAnda.get(0);
                    dialog(suara);
                    mDatabaseReference.setValue(suara);
//                    Voice voice = new Voice(suara);
//                    mDatabaseReference.setValue(voice);
                    active=0;
                }

                if(resultCode == RecognizerIntent.RESULT_AUDIO_ERROR){
                    showToastMessage("Audio Bermasalah");
                }else if(resultCode == RecognizerIntent.RESULT_CLIENT_ERROR){
                    showToastMessage("Client Bermasalah");
                }else if(resultCode == RecognizerIntent.RESULT_NETWORK_ERROR){
                    showToastMessage("Jaringan Bermasalah");
                }else if(resultCode == RecognizerIntent.RESULT_NO_MATCH){
                    showToastMessage("Perangkat Tidak Cocok");
                }else if(resultCode == RecognizerIntent.RESULT_SERVER_ERROR){
                    showToastMessage("Server Bermasalah");
                }
                break;

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.pilihan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()== R.id.setting){
            startActivity(new Intent(MainActivity.this, Activity_Setting.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void textToSpeech(){
        myTTS = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!= TextToSpeech.ERROR){
//                    myTTS.setLanguage(new Locale("id", "ID"));
                    myTTS.setLanguage(Locale.US);
                }
            }
        });
    }

    private void dialog(String command){
        command=command.toLowerCase();
        if(command.indexOf("nyalakan")!=-1){
            if(command.indexOf("lampu")!=-1){
                speak("OK! Lampu nyala");
            }else if(command.indexOf("kipas")!=-1){
                speak("Ok! kipas nyala");
            }else if(command.indexOf("solenoid")!=-1){
                speak("Ok! solenoid nyala");
            }else if(command.indexOf("radio")!=-1){
                radioPlay();
            }
        }else if(command.indexOf("matikan")!=-1){
            if(command.indexOf("lampu")!=-1){
                speak("OK! Lampu mati");
            }else if(command.indexOf("kipas")!=-1){
                speak("Ok! kipas mati");
            }else if(command.indexOf("solenoid")!=-1){
                speak("Ok! kipas mati");
            }
        }else if(command.indexOf("jam")!=-1){
            Calendar calender = Calendar.getInstance();
            SimpleDateFormat time = new SimpleDateFormat("HH:mm");
            String timeString = "sekarang pukul" + time.format(calender.getTime());
            speak(timeString);
        }else if(command.indexOf("tanggal")!=-1){
            Date tanggal = new Date();
            SimpleDateFormat date = new SimpleDateFormat("dd/MMMM/yyyy");
            String dateString = "sekarang tanggal" + date.format(tanggal);
            speak(dateString);
        }else if(command.indexOf("baca")!=-1){
            if(command.indexOf("berita")!=-1){
                if(command.indexOf("populer")!=-1){
                    speak("berita populer hari ini.");
                    new doit().execute();
                }else if(command.indexOf("pertama")!=-1){
                    new bacaBerita1().execute();
                }else if(command.indexOf("kedua")!=-1){
                    new bacaBerita2().execute();
                }else if(command.indexOf("ketiga")!=-1){
                    new bacaBerita3().execute();
                }else if(command.indexOf("ke-4")!=-1){
                    new bacaBerita4().execute();
                }else if(command.indexOf("ke-5")!=-1){
                    new bacaBerita5().execute();
                }
            }
        }else if(command.indexOf("info")!=-1){
            speak(info);
        }
        else{
            speak("instruksi salah. silahkan coba lagi");
        }
    }

    private void speak(String message){
        if(Build.VERSION.SDK_INT>=21){
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }else {
            myTTS.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void myExecute(){
        showToastMessage("Masukkan Suara Anda");
        String ID_ModelBahasaIndonesia = "id";
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, ID_ModelBahasaIndonesia);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, ID_ModelBahasaIndonesia);
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, ID_ModelBahasaIndonesia);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, ID_ModelBahasaIndonesia);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Masukkan Suara");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        getar.vibrate(200);
        active = 1;
        myTTS.stop();
        if(mediaPlayer.isPlaying()){
            mediaPlayer.reset();
        }

        try{
            startActivityForResult(intent, RESULT_SPEECH);
        }
        catch (ActivityNotFoundException a){}
    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            acelLast = acelVal;
            acelVal = (float) Math.sqrt((double) (x*x + y*y + z*z));
            float delta = acelVal - acelLast;
            shake = shake * 0.9f + delta;

            if(shake < -9 && active == 0){
                myExecute();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    private void radioPlay(){
        try{
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(urlRadio);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
        }catch (IOException e){}
    }

    public static String maxString(String str, int len) {
        if (str.length() > len) {
            return str.substring(0, len);
        } else {
            return str;
        }}

    @Override
    protected void onResume() {
        super.onResume();
        active=0;
    }

    @Override
    protected void onPause() {
        super.onPause();
        active=0;
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, LENGTH_SHORT).show();
    }

    public class doit extends AsyncTask<Void, Void, Void>{

        String newPage1, newPage2, newPage3, newPage4, newPage5;


        @Override
        protected Void doInBackground(Void... voids) {
            try {

                Document doc = Jsoup.connect(urlWeb).get();
                Elements newsRawTag1 = doc.select("div.populer ul li:eq(0) h3 a.txt-oev-3");
                Elements newsRawTag2 = doc.select("div.populer ul li:eq(1) h3 a.txt-oev-3");
                Elements newsRawTag3 = doc.select("div.populer ul li:eq(2) h3 a.txt-oev-3");
                Elements newsRawTag4 = doc.select("div.populer ul li:eq(3) h3 a.txt-oev-3");
                Elements newsRawTag5 = doc.select("div.populer ul li:eq(4) h3 a.txt-oev-3");
                newPage1 = newsRawTag1.text();
                newPage2 = newsRawTag2.text();
                newPage3 = newsRawTag3.text();
                newPage4 = newsRawTag4.text();
                newPage5 = newsRawTag5.text();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            speak("berita pertama."+newPage1+". "+"berita ke dua."+newPage2+". "+"berita ke tiga."+newPage3+". "+"berita ke empat."+newPage4+". "+"berita ke lima."+newPage5+".");
        }
    }

    public class bacaBerita1 extends AsyncTask<Void, Void, Void>{
        String link, page1, word;
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Document doc = Jsoup.connect(urlWeb).get();

                Elements link1 = doc.select("div.populer ul li:eq(0) h3 a[href]");

                link = link1.attr("href");


                Document berita1 = Jsoup.connect(link+"?page=all").get();
                Elements news1 = berita1.select("div.txt-article p");
                page1 = news1.text();
                word = maxString(page1, 3999);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            speak(word);
        }
    }

    public class bacaBerita2 extends AsyncTask<Void, Void, Void>{
        String link, page1, word;
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Document doc = Jsoup.connect(urlWeb).get();

                Elements link1 = doc.select("div.populer ul li:eq(1) h3 a[href]");

                link = link1.attr("href");


                Document berita1 = Jsoup.connect(link+"?page=all").get();
                Elements news1 = berita1.select("div.txt-article p");
                page1 = news1.text();
                word = maxString(page1, 3999);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            speak(word);
        }
    }

    public class bacaBerita3 extends AsyncTask<Void, Void, Void>{
        String link, page1, word;
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Document doc = Jsoup.connect(urlWeb).get();

                Elements link1 = doc.select("div.populer ul li:eq(2) h3 a[href]");

                link = link1.attr("href");


                Document berita1 = Jsoup.connect(link+"?page=all").get();
                Elements news1 = berita1.select("div.txt-article p");
                page1 = news1.text();
                word = maxString(page1, 3999);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            speak(word);
        }
    }

    public class bacaBerita4 extends AsyncTask<Void, Void, Void>{
        String link, page1, word;
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Document doc = Jsoup.connect(urlWeb).get();

                Elements link1 = doc.select("div.populer ul li:eq(3) h3 a[href]");

                link = link1.attr("href");


                Document berita1 = Jsoup.connect(link+"?page=all").get();
                Elements news1 = berita1.select("div.txt-article p");
                page1 = news1.text();
                word = maxString(page1, 3999);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            speak(word);
        }
    }

    public class bacaBerita5 extends AsyncTask<Void, Void, Void>{
        String link, page1, word;
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Document doc = Jsoup.connect(urlWeb).get();
                Elements link1 = doc.select("div.populer ul li:eq(4) h3 a[href]");
                link = link1.attr("href");


                Document berita1 = Jsoup.connect(link+"?page=all").get();
                Elements news1 = berita1.select("div.txt-article p");
                page1 = news1.text();
                word = maxString(page1, 3999);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            speak(word);
        }
    }

}