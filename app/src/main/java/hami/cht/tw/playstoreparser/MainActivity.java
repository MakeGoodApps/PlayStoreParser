package hami.cht.tw.playstoreparser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    final static String CLASS_REVIEW_CONTAINER = "d15Mdf bAhLNe";
    final static String CLASS_REVIEW_USER = "X43Kjb";
    final static String CLASS_REVIEW_TIME = "p2TkOb";
    final static String CLASS_REVIEW_STAR = "vQHuPe bUWb7c";
    final static String CLASS_REVIEW_TEXT = "UD7Dzf";

    private String mPackageName = "tw.com.cht.easyhami";
    private HashMap<String, ArrayList<PlayStoreReview>> mReviews;

    @SuppressLint("JavascriptInterface")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);

        mReviews = new HashMap<>();

        final WebView apkReviewView = findViewById(R.id.webview);
        WebSettings ws = apkReviewView.getSettings();
        ws.setJavaScriptEnabled(true);
        apkReviewView.addJavascriptInterface(new DemoJavaScriptInterface(),"HTMLOUT");
        apkReviewView.loadUrl("https://play.google.com/store/apps/details?id=tw.com.cht.easyhami&showAllReviews=true");

        final Button button = findViewById(R.id.getHtml);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                apkReviewView.loadUrl("javascript:window.HTMLOUT.processHTML(document.documentElement.outerHTML);");
            }
        });

        final Spinner apkList = findViewById(R.id.apkList);
        ArrayAdapter<CharSequence> lunchList = ArrayAdapter.createFromResource(MainActivity.this,
                R.array.apkList,
                android.R.layout.simple_spinner_dropdown_item);
        apkList.setAdapter(lunchList);
        apkList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPackageName = apkList.getSelectedItem().toString();
                String packageId = "";
                switch (position) {
                    case 0:
                        packageId = "tw.com.cht.easyhami";
                        break;
                    case 1:
                        packageId = "com.google.android.apps.walletnfcrel";
                        break;
                    case 2:
                        packageId = "com.jkos.app";
                        break;
                    case 3:
                        packageId = "com.fet.fridaywallet";
                        break;
                    case 4:
                        packageId = "com.taiwanmobile.wali";
                        break;

                }
                apkReviewView.loadUrl("https://play.google.com/store/apps/details?id="+ packageId + "&showAllReviews=true");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    final class DemoJavaScriptInterface {

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html) {
            Log.d("processHTML", html);
            ArrayList<PlayStoreReview> reviewList = new ArrayList<>();
            mReviews.remove(mPackageName);

            Document doc = Jsoup.parse(html);
            Elements reviews = doc.getElementsByClass(CLASS_REVIEW_CONTAINER);
            for(Element review : reviews) {
                PlayStoreReview aReview = new PlayStoreReview();
                aReview.setUserName(review.getElementsByClass(CLASS_REVIEW_USER).get(0).text());
                aReview.setReviewTime(review.getElementsByClass(CLASS_REVIEW_TIME).get(0).text());
                aReview.setReviewStar(String.valueOf(review.getElementsByClass(CLASS_REVIEW_STAR).size()));
                aReview.setReviewText(review.getElementsByClass(CLASS_REVIEW_TEXT).get(0).text());

                reviewList.add(aReview);
            }
            mReviews.put(mPackageName, reviewList);

            String reviewCount = "";

            for(String key : mReviews.keySet()) {
                reviewCount += key + " : " + mReviews.get(key).size() + "\n";
            }

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Review Count")
                    .setMessage(reviewCount)
                    .setCancelable(false)
                    .setNeutralButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                generateReviewList();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        }
    }

    private void generateReviewList() throws IOException {
        StringBuilder log = new StringBuilder();
        final String mLogPath = "/sdcard/";
        String fileName = "reviews.csv";
        File logFile = new File(mLogPath, fileName);
        logFile.delete();

        log.append("支付方式" + "\t" +  "評論用戶" + "\t" + "評論時間" + "\t" + "評論星等" + "\t" + "評論內容" + "\n");
        for(String key : mReviews.keySet()) {
            for(PlayStoreReview aReview : mReviews.get(key)) {
                log.append(key + "\t" +  aReview.getUserName() + "\t" + aReview.getReviewTime() + "\t" + aReview.getReviewStar() + "\t" + aReview.getReviewText() + "\n");
            }
        }

        FileWriter writer = new FileWriter(logFile);
        writer.append(log);
        writer.flush();
        writer.close();

    }

    private void verifyStoragePermissions(Activity activity) {
        int write = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (write != PackageManager.PERMISSION_GRANTED || read != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

}
