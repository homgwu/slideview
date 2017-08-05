package com.homg.slideview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.ScrollerCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 左右滑动换图片view,揭示效果。
 * Created by HomgWu on 16/9/26 19:52.
 */

public class SlideImageView extends View implements GestureDetector.OnGestureListener, ImageLoadingListener {
    private static final String TAG = SlideImageView.class.getSimpleName();
    private static final int SMOOTH_SCROLL_DURATION = 400;
    private float mFlingValidatedVelocity;
    private DisplayImageOptions mOptions = (new DisplayImageOptions.Builder()).cacheInMemory(true).cacheOnDisk(true).build();
    private ImageLoader mImageLoader = ImageLoader.getInstance();
    private GestureDetectorCompat mGestureDetector;
    private ScrollerCompat mScroller;
    private Context mContext;
    private Drawable mForegroundDrawable;
    private Drawable mBackgroundDrawable;
    private Drawable mEmptyDrawable;
    private int mEmptyColor;
    //    private Drawable mMaskDrawable;
    private ArrayList<ImageInfo> mImageInfos = new ArrayList<>(9);
    private ArrayMap<String, Drawable> mDrawableMap = new ArrayMap<>(9);
    private int mCurrentIndex = 0;
    private int mDistanceX;
    private boolean mUserScrolled = false;
    private int mTextMargin = 50;
    private OnSlideImageListener mSlideImageListener;
    /**
     * 是否加载过新设置的图片集{@link #setImageInfos(List)}
     */
    private boolean mIsLoadNewImages = false;
    private boolean mIsOnLayouted = false;
    private String mDrawName = "";
    private int mDrawIndicatorIndex = 0;
    /**
     * 1:往左滑动要翻页,2:往右滑动要翻页,3:往左滑回不翻页,4:往右滑回不翻页
     */
    private SlideDirection mSlideDirection = SlideDirection.SLIDE_INIT;
    private Rect mRect = new Rect();
    private Paint mPaint = new Paint();
    private int mNameSize;
    private int mNameColor;
    private int mIndicatorColor;
    private int mIndicatorRadius;

    public SlideImageView(Context context) {
        this(context, null);
    }

    public SlideImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SlideImageView);
        mEmptyColor = typedArray.getColor(R.styleable.SlideImageView_emptyColor, 0XFF000000);
        mTextMargin = typedArray.getDimensionPixelSize(R.styleable.SlideImageView_textMargin, 50);
        mNameSize = typedArray.getDimensionPixelSize(R.styleable.SlideImageView_nameTextSize, 30);
        mIndicatorRadius = typedArray.getDimensionPixelSize(R.styleable.SlideImageView_indicatorRadius, 15);
        mIndicatorColor = typedArray.getColor(R.styleable.SlideImageView_indicatorColor, 0xFF0000FF);
        mNameColor = typedArray.getColor(R.styleable.SlideImageView_nameTextColor, 0xFF000000);
        typedArray.recycle();
        init();
    }

    private void init() {
        mContext = getContext();
        setFocusable(true);
        setFocusableInTouchMode(true);
        mFlingValidatedVelocity = getResources().getDisplayMetrics().density * 1000;
        mGestureDetector = new GestureDetectorCompat(mContext, this);
        mScroller = ScrollerCompat.create(mContext);
//        mMaskDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0x90000000, 0x00000000});
        mEmptyDrawable = new ColorDrawable(mEmptyColor);
        mIsOnLayouted = false;
    }

    public void setDefaultImage(int resId) {
        this.mOptions = (new DisplayImageOptions.Builder()).cloneFrom(this.mOptions).showImageOnLoading(resId).showImageForEmptyUri(resId).showImageOnFail(resId).build();
    }

    /**
     * 设置图片源,null或size==0时透明
     *
     * @param imageInfos
     */
    public void setImageInfos(List<ImageInfo> imageInfos) {
        if (imageInfos == null || imageInfos.size() == 0)
            return;
        mImageInfos.clear();
        mDrawableMap.clear();
        mImageInfos.addAll(imageInfos);
        mCurrentIndex = 0;
        mSlideDirection = SlideDirection.SLIDE_INIT;
        mDistanceX = 0;
        mForegroundDrawable = null;
        mBackgroundDrawable = null;
        //防止加载完图片时,onMeasure还没有被调用
        Log.i(TAG, "setImageInfos mIsOnLayouted=" + mIsOnLayouted);
        if (mIsOnLayouted) {
            mIsLoadNewImages = true;
            loadImages();
        } else {
            mIsLoadNewImages = false;
        }
    }

    public void setOnSlideImageListener(OnSlideImageListener slideImageListener) {
        mSlideImageListener = slideImageListener;
    }

    public int getCount() {
        return mImageInfos.size();
    }

    /**
     * 清理图片,显示empty image
     */
    public void resetDisplay() {
        mImageInfos.clear();
        mDrawableMap.clear();
        mCurrentIndex = 0;
        mSlideDirection = SlideDirection.SLIDE_INIT;
        mDistanceX = 0;
        mForegroundDrawable = null;
        mBackgroundDrawable = null;
        mDrawName = "";
        mDrawIndicatorIndex = 0;
        invalidate();
    }

    private void loadImages() {
        //刷新出empty drawable
        invalidate();
        for (ImageInfo imageInfo : mImageInfos) {
            Log.i(TAG, "loadImages imageInfo.getUrl()=" + imageInfo.getUrl());
            mImageLoader.loadImage(imageInfo.getUrl(), mOptions, this);
        }
    }

    private void setDrawable(String icon, Drawable drawable) {
//        //Log.i(TAG, "setBitmapDrawable icoc=" + icon);
//        //Log.i(TAG, "setBitmapDrawable bitmapDrawable=" + drawable);
//        //Log.i(TAG, "setBitmapDrawable mRect=" + mRect.toString());
        if (drawable != null) {
            drawable.setBounds(mRect);
            mDrawableMap.put(icon, drawable);
        }
        //当前景图为空时要刷出指示点和名字
        if (mForegroundDrawable == null) {
            for (int i = 0; i < mImageInfos.size(); i++) {
                String tempIcon = mImageInfos.get(i).getUrl();
                if (icon.equals(tempIcon)) {
                    if (i == mCurrentIndex) {
                        //在这里不设置background ,因为不知道用户要网哪边滑动
                        mForegroundDrawable = getDrawable(mImageInfos.get(mCurrentIndex).getUrl());
                    }
                }
            }
            mDrawName = mImageInfos.get(mCurrentIndex).getName();
            mDrawIndicatorIndex = mCurrentIndex;
            onShowImage(mCurrentIndex, mImageInfos.get(mCurrentIndex));
            invalidate();
            //Log.i(TAG, "setBitmapDrawable invalidate");
        }
    }


    private void onShowImage(int index, ImageInfo imageInfo) {
        if (mSlideImageListener != null)
            mSlideImageListener.onShowImage(index, imageInfo);
    }

    private Drawable getDrawable(String icon) {
        Log.i(TAG, "getDrawable icon=" + icon);
        return mDrawableMap.get(icon);
    }

    private void setBackgroundDrawable(boolean left) {
        int backgroundIndex = mCurrentIndex;
        if (left) {
            backgroundIndex--;
            if (backgroundIndex < 0) {
                //循环
                mBackgroundDrawable = getDrawable(mImageInfos.get(mImageInfos.size() - 1).getUrl());
            } else {
                mBackgroundDrawable = getDrawable(mImageInfos.get(backgroundIndex).getUrl());
            }
        } else {
            backgroundIndex++;
            if (backgroundIndex >= mImageInfos.size()) {
                //循环
                mBackgroundDrawable = getDrawable(mImageInfos.get(0).getUrl());
            } else {
                mBackgroundDrawable = getDrawable(mImageInfos.get(backgroundIndex).getUrl());
            }
        }

    }

    /**
     * 换页
     *
     * @param left 是否往左边翻页
     */
    private void changePage(boolean left) {
        if (left) {
            int frontIndex = mCurrentIndex - 1;
            if (frontIndex >= 0) {
                mForegroundDrawable = mBackgroundDrawable;
                mBackgroundDrawable = null;
            } else {
                //循环
                frontIndex = mImageInfos.size() - 1;
                mForegroundDrawable = getDrawable(mImageInfos.get(frontIndex).getUrl());
                mBackgroundDrawable = null;
            }
            mCurrentIndex = frontIndex;
        } else {
            int frontIndex = mCurrentIndex + 1;
            if (frontIndex < mImageInfos.size()) {
                mForegroundDrawable = mBackgroundDrawable;
                mBackgroundDrawable = null;
            } else {
                //循环
                frontIndex = 0;
                mForegroundDrawable = getDrawable(mImageInfos.get(frontIndex).getUrl());
                mBackgroundDrawable = null;
            }
            mCurrentIndex = frontIndex;
        }

    }

    private void changeCurrentName(boolean left) {
        int frontIndex = 0;
        if (left) {
            frontIndex = mCurrentIndex - 1;
            if (frontIndex < 0) {
                //循环
                frontIndex = mImageInfos.size() - 1;
            }
            mDrawName = mImageInfos.get(frontIndex).getName();
            mDrawIndicatorIndex = frontIndex;
        } else {
            frontIndex = mCurrentIndex + 1;
            if (frontIndex >= mImageInfos.size()) {
                //循环
                frontIndex = 0;
            }
            mDrawName = mImageInfos.get(frontIndex).getName();
            mDrawIndicatorIndex = frontIndex;
        }
        onShowImage(frontIndex, mImageInfos.get(frontIndex));
    }

    private void startScroll(float velocityX, MotionEvent e2) {
        int width = getMeasuredWidth();
        int endX;
        //e2.getX大于0
        int startX = (int) (e2.getX() + 0.5);
        int distance;
        if (velocityX > 0) {
            //smooth right
            mSlideDirection = SlideDirection.SLIDE_RIGHT_TO_NEXT;
            distance = width - startX;
            endX = width;
            if (mBackgroundDrawable == null)
                setBackgroundDrawable(true);
        } else {
            //smooth left
            mSlideDirection = SlideDirection.SLIDE_LEFT_TO_NEXT;
            distance = startX;
            endX = 0;
            if (mBackgroundDrawable == null)
                setBackgroundDrawable(false);
        }
        float time = Math.abs(distance / velocityX * 1000);
//        L.it(TAG, "startScroll velocityX=%d,e2.getX()=%d,width=%d,time=%f", (int) velocityX, (int) e2.getX(),
//                width, time);
        if (time > 400) {
            time = 400;
        } else if (time < 50) {
            time = 50;
        }
        //在flyme系统上有问题,所以fling换成了scroll
//        mScroller.fling((int) e2.getX(), 0, (int) velocityX, 0, 0, width, 0, 0);
        mScroller.startScroll(startX, 0, endX - startX, 0, (int) (time + 0.5));
        mDistanceX = 0;
        changeCurrentName(mSlideDirection == SlideDirection.SLIDE_RIGHT_TO_NEXT);
        invalidate();
    }

    private void startScroll() {
        Log.i(TAG, "startScroll()");
        int endX = getMeasuredWidth();
        int startX = 0;
        if (mDistanceX < 0) {
            // smooth right
            mSlideDirection = SlideDirection.SLIDE_RIGHT_TO_NEXT;
            startX = Math.abs(mDistanceX);
            mScroller.startScroll(startX, 0, endX - startX, 0, SMOOTH_SCROLL_DURATION);
            if (mBackgroundDrawable == null)
                setBackgroundDrawable(true);
        } else {
            //smooth left
            mSlideDirection = SlideDirection.SLIDE_LEFT_TO_NEXT;
            startX = endX - mDistanceX;
            mScroller.startScroll(startX, 0, -startX, 0, SMOOTH_SCROLL_DURATION);
            if (mBackgroundDrawable == null)
                setBackgroundDrawable(false);
        }
        mDistanceX = 0;
        changeCurrentName(mSlideDirection == SlideDirection.SLIDE_RIGHT_TO_NEXT);
        invalidate();
    }

    private void startScrollBack() {
        int endX = getMeasuredWidth();
        int startX = 0;
        //往反方向滑回
        if (mDistanceX < 0) {
            mSlideDirection = SlideDirection.SLIDE_LEFT_BACK;
            startX = Math.abs(mDistanceX);
            mScroller.startScroll(startX, 0, mDistanceX, 0, SMOOTH_SCROLL_DURATION);
        } else {
            mSlideDirection = SlideDirection.SLIDE_RIGHT_BACK;
            startX = endX - mDistanceX;
            //Log.i(TAG, "startScroll mSmoothStatus endX="+endX);
            //Log.i(TAG, "startScroll mSmoothStatus mDistanceX="+mDistanceX);
            mScroller.startScroll(startX, 0, mDistanceX, 0, SMOOTH_SCROLL_DURATION);
        }
        mDistanceX = 0;
        invalidate();
    }

    private boolean checkOutOfBound(boolean left) {
        if (mImageInfos.size() < 2) {
            return true;
        }
//        else if (left) {
//            if (mCurrentIndex == 0)
//                return true;
//        } else {
//            if (mCurrentIndex == mImageInfos.size() - 1)
//                return true;
//        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawDrawable(canvas);
        drawText(canvas);
        drawIndicator(canvas);
    }


    private void drawIndicator(Canvas canvas) {
        if (mImageInfos.size() < 2)
            return;
        mPaint.setStyle(Paint.Style.FILL);
        int endX = getMeasuredWidth();
        int endY = getMeasuredHeight();
        int radius = mIndicatorRadius;
        int margin = mIndicatorRadius * 2;
        int centerY = endY - mTextMargin - radius;
        int startX = endX - (mTextMargin + (radius * 2 + margin) * mImageInfos.size());
        for (int i = 0; i < mImageInfos.size(); i++) {
            if (i == mDrawIndicatorIndex) {
                mPaint.setColor(mIndicatorColor);
            } else {
                mPaint.setColor(Color.GRAY);
            }
            int centerX = 0;
            if (i == 0) {
                centerX = startX;
            } else {
                centerX = startX + (margin + 2 * radius) * i;
            }
            canvas.drawCircle(centerX, centerY, radius, mPaint);
        }
    }

    private void drawText(Canvas canvas) {
        if (TextUtils.isEmpty(mDrawName))
            return;
        mPaint.setColor(mNameColor);
        mPaint.setTextSize(mNameSize);
        canvas.drawText(mDrawName, mTextMargin, getMeasuredHeight() - mTextMargin, mPaint);
    }

    private void drawDrawable(Canvas canvas) {
        //Log.i(TAG, "onDraw mSmoothStatus mDistanceX=" + mDistanceX);
        int endX = getMeasuredWidth();
        int endY = getMeasuredHeight();
        int startX = 0;
        int startY = 0;
        if (mScroller.computeScrollOffset()) {
            //不翻页的
            if (mSlideDirection == SlideDirection.SLIDE_LEFT_BACK) {
                startX = mScroller.getCurrX();
                //Log.i(TAG, "onDraw mSmoothStatus==3 startX=" + startX);
            } else if (mSlideDirection == SlideDirection.SLIDE_RIGHT_BACK) {
                endX = mScroller.getCurrX();
                //Log.i(TAG, "onDraw mSmoothStatus==4 endX=" + endX);
            }
            //要翻页的
            else if (mSlideDirection == SlideDirection.SLIDE_LEFT_TO_NEXT) {
                //往左动
                endX = mScroller.getCurrX();
                //Log.i(TAG, "onDraw mSmoothStatus==1 endX=" + endX);
            } else if (mSlideDirection == SlideDirection.SLIDE_RIGHT_TO_NEXT) {
                //往右动
                startX = mScroller.getCurrX();
                //Log.i(TAG, "onDraw mSmoothStatus==2 startX=" + startX);
            }
            invalidate();
        } else {
            //动画完了翻页
            if (mSlideDirection == SlideDirection.SLIDE_LEFT_TO_NEXT || mSlideDirection == SlideDirection.SLIDE_RIGHT_TO_NEXT) {
                //Log.i(TAG, "onDraw mSmoothStatus==1||mSmoothStatus==2 动画完了翻页");
                changePage(mSlideDirection == SlideDirection.SLIDE_RIGHT_TO_NEXT);
            }
            if (mDistanceX < 0) {
                //往右滑
                startX = Math.abs(mDistanceX);
            } else {
                //往左滑
                endX -= mDistanceX;
            }
        }
        //Log.i(TAG, "onDraw mSmoothStatus startX=" + startX);
        if (mBackgroundDrawable != null) {
            //Log.i(TAG, "onDraw mSmoothStatus  mBackgroundDrawable="+mBackgroundDrawable);
            mBackgroundDrawable.draw(canvas);
        } else {
            mEmptyDrawable.draw(canvas);
        }

        Rect clipRect = new Rect(startX, startY, endX, endY);
        canvas.save();
        canvas.clipRect(clipRect);
        if (mForegroundDrawable != null) {
            //Log.i(TAG, "onDraw mSmoothStatus mforegroundDrawable="+mforegroundDrawable);
            mForegroundDrawable.draw(canvas);
        } else {
            mEmptyDrawable.draw(canvas);
        }
        canvas.restore();
        //把蒙层画上去
//        mMaskDrawable.draw(canvas);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (mScroller.isFinished()) {
            Log.i(TAG, "onDown mScroller.isFinished() true");
            mSlideDirection = SlideDirection.SLIDE_INIT;
            return true;
        } else {
            Log.i(TAG, "onDown mScroller.isFinished() false");
            //动画时不能滑动
            return false;
        }
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mUserScrolled = true;
        //x往右滑为负
        //Log.i(TAG, "onScroll distanceX=" + distanceX);
        //Log.i(TAG, "onScroll mCurrentIndex=" + mCurrentIndex);
        if (Math.abs(distanceX) > Math.abs(distanceY)) {
            //这里先赋值给mDistanceX以便在up时检测
            mDistanceX += (int) distanceX;
            if (checkOutOfBound(mDistanceX < 0))
                return true;
            //Log.i(TAG, "onScroll mDistanceX=" + mDistanceX);
            if (mBackgroundDrawable == null)
                setBackgroundDrawable(mDistanceX < 0);
            invalidate();
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        //Log.i(TAG, "onFling velocityX=" + velocityX);
        //Log.i(TAG, "onFling MotionEvent e1 e1.getX()=" + e1.getX());
        //Log.i(TAG, "onFling MotionEvent e2 e2.getX()=" + e2.getX());
        Log.i(TAG, MessageFormat.format("onFling velocityX={0},mFlingValidatedVelocity={1}", velocityX, mFlingValidatedVelocity));
        //x往右滑为正
        if (Math.abs(velocityX) > mFlingValidatedVelocity) {
            if (checkOutOfBound(velocityX > 0))
                return true;
            //Log.i(TAG, "onFling changePage");
            startScroll(velocityX, e2);
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //如果用户滑动了(非fling),
        getParent().requestDisallowInterceptTouchEvent(true);
        if (mUserScrolled) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    int totalWidth = getMeasuredWidth();
                    if (checkOutOfBound(mDistanceX < 0)) {
                        //这里不刷新,在边界,不需要滑动
                        mDistanceX = 0;
                    } else {
                        if (Math.abs(mDistanceX) > totalWidth / 2) {
                            //Log.i(TAG, "MotionEvent.ACTION_UP changePage");
                            startScroll();
                        } else {
                            //Log.i(TAG, "MotionEvent.ACTION_UP invalidate");
                            startScrollBack();
                        }
                    }
                    mUserScrolled = false;
                    break;
                default:
                    break;
            }
        }
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        //正方形
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int measureWidth = getMeasuredWidth();
        int measureHeight = getMeasuredHeight();
        mRect.set(0, 0, measureWidth, measureHeight);
        mEmptyDrawable.setBounds(mRect);
        //mask高度为总高度的34%
//        mMaskDrawable.setBounds(0, (int) (measureHeight * 0.66 + 0.5), measureWidth, measureHeight);
        if (!mIsLoadNewImages) {
            mIsLoadNewImages = !mIsLoadNewImages;
            loadImages();
        }
        Log.i(TAG, "onLayout mTextMargin=" + mTextMargin);
        Log.i(TAG, "onLayout mRect=" + mRect.toString());
        mIsOnLayouted = true;
    }

    @Override
    public void onLoadingStarted(String imageUri, View view) {

    }

    @Override
    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
        if (mImageInfos.size() != 0)
            setDrawable(imageUri, mEmptyDrawable);
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        if (mImageInfos.size() == 0)
            return;
        //兼容card view 的圆角
//        if (Build.VERSION.SDK_INT >= 21) {
        setDrawable(imageUri, new BitmapDrawable(mContext.getResources(), loadedImage));
//        } else {
        //圆角图片
//        FlexibleRoundedDrawable flexibleRoundedDrawable = new FlexibleRoundedDrawable(loadedImage,
//                mContext.getResources().getDimensionPixelSize(R.dimen.pkg_details_slide_image_radius)
//                , FlexibleRoundedDrawable.CORNER_ALL);
//        setDrawable(imageUri, flexibleRoundedDrawable);
//        }

    }

    @Override
    public void onLoadingCancelled(String imageUri, View view) {

    }

    public void release() {
        mImageInfos.clear();
        mDrawableMap.clear();
//        mMaskDrawable = null;
        mBackgroundDrawable = null;
        mForegroundDrawable = null;
        mEmptyDrawable = null;
        mSlideImageListener = null;
    }

    /**
     * 1:往左滑动要翻页,2:往右滑动要翻页,3:往左滑回不翻页,4:往右滑回不翻页
     */
    public enum SlideDirection {
        SLIDE_INIT(0), SLIDE_LEFT_TO_NEXT(1), SLIDE_RIGHT_TO_NEXT(2), SLIDE_LEFT_BACK(3), SLIDE_RIGHT_BACK(4);
        private int value;

        private SlideDirection(int value) {
            this.value = value;
        }
    }

    public static class ImageInfo {
        private String url;
        private String name;

        public ImageInfo(String url, String name) {
            this.url = url;
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("uri:").append(url).append(",name:").append(name);
            return sb.toString();
        }
    }

    public interface OnSlideImageListener {
        /**
         * 当需要切换到下一张图片时调用
         *
         * @param index
         * @param imageInfo
         */
        void onShowImage(int index, ImageInfo imageInfo);
    }
}
