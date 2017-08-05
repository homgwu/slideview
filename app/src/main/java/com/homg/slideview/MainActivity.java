package com.homg.slideview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private SlideImageView mSlideImageView;
    private String[] urls = new String[]{"assets://test_1.jpg", "assets://test_2.jpg",
            "assets://test_3.jpg",
            "assets://test_4.jpg",
            "assets://test_5.jpg"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageLoaderConfiguration configuration = ImageLoaderConfiguration
                .createDefault(this);
        ImageLoader.getInstance().init(configuration);
        setContentView(R.layout.activity_main);
        mSlideImageView = (SlideImageView) findViewById(R.id.main_siv);
        mSlideImageView.setOnSlideImageListener(new SlideImageView.OnSlideImageListener() {
            @Override
            public void onShowImage(int index, SlideImageView.ImageInfo imageInfo) {
                Log.i(TAG, String.format("index=%d,image info=%s", index, imageInfo));
            }
        });
        List<SlideImageView.ImageInfo> imageInfoList = new ArrayList<>(5);
        for (int i = 0; i < urls.length; i++) {
            SlideImageView.ImageInfo tempInfo = new SlideImageView.ImageInfo(urls[i], "Image-" + i);
            imageInfoList.add(tempInfo);
        }
        mSlideImageView.setImageInfos(imageInfoList);
    }
}
