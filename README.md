# SlideView

[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/) 

SlideView是一个Android上的以滑动揭示的方式显示并切换图片的View，以视觉对比的方式把一套相似的图片展示出来。

> 作者：竹尘居士
>
> 博客：http://www.cnblogs.com/homg/p/7291793.html



## 示例 
- 翻页图片揭示效果：
  ![](https://github.com/homgwu/slideview/blob/master/gif/slide_image_view.gif?raw=true)

## 特性

- 设置一组(List\<ImageInfo>)待加载的图片(本地图片，网络图片)，通过([ImageLoader](https://github.com/nostra13/Android-Universal-Image-Loader))库加载出图片并按List的顺序显示图片。

- 通过手势识别(GestureDetector)，判断用户手指往左或者往右滑动，根据滑动位移揭开和盖上图片，当手指松开时，根据滑动速度和滑动位移的距离决定是翻页，还是滑回当前页。

- 翻页或滑回时通过ScrollerCompat来计算并画出平滑动画，在规定时间内平滑过度。

- 继承自View，可在xml布局文件中使用，也可以java代码使用。

- 在View的onDraw方法中画出当前要显示的图片，文字(名字)和指示器。

  ​

## 使用方法

1. 布局中定义SlideImageView:

   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <RelativeLayout
       xmlns:android="http://schemas.android.com/apk/res/android"
       xmlns:app="http://schemas.android.com/apk/res-auto"
       android:layout_width="match_parent"
       android:layout_height="match_parent"
       android:background="@android:color/darker_gray"
       >

       <com.homg.slideview.SlideImageView
           android:id="@+id/main_siv"
           android:layout_width="match_parent"
           android:layout_height="match_parent"
           android:layout_centerInParent="true"
           app:indicatorColor="#FF0000FF"
           app:indicatorRadius="5dp"
           app:nameTextColor="#FF0000FF"
           app:nameTextSize="26sp"
           app:textMargin="5dp"
           />
   </RelativeLayout>
   ```

2. 设置SlideImageView，设置图片源，和图片切换监听器:

   ```java
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
           mSlideImageView.setOnSlideImageListener(new         SlideImageView.OnSlideImageListener() {
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
   ```

3. build.gradle中加入ImageLoader的依赖：

   ```java
   dependencies {
       compile fileTree(dir: 'libs', include: ['*.jar'])
       compile 'com.nostra13.universalimageloader:universal-image-loader:1.9.5'
   }
   ```