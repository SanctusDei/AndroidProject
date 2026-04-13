package utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;

public class MarkerUtils {

    /**
     * 生成带红/绿边框的圆形商家图标
     *
     * @param context    上下文，用于获取资源
     * @param resId      商家图片的资源 ID（drawable）
     * @param borderColor 边框颜色 (Color.RED 或 Color.GREEN)
     * @param sizeDp     图标在地图上的目标大小（单位 DP，建议 40-50dp）
     * @return 用于高德 Marker 的 BitmapDescriptor
     */
    public static BitmapDescriptor getRoundedMarkerBitmap(Context context, int resId, int borderColor, int sizeDp) {
        // 1. 将 DP 转换为 PX
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (sizeDp * density);
        int borderWidthPx = (int) (2 * density); // 2dp 宽的边框

        // 2. 加载原始图片并缩放至目标大小（包含边框空间）
        Bitmap sourceBitmap = BitmapFactory.decodeResource(context.getResources(), resId);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(sourceBitmap, sizePx, sizePx, false);

        // 3. 创建用于绘制的空白画布
        Bitmap outputBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true); // 开启抗锯齿，边缘更平滑
        canvas.drawARGB(0, 0, 0, 0); // 先画透明背景

        // --- 核心步骤 A：绘制圆形图片 ---

        // --- 核心步骤 A：绘制圆形图片 ---
        Rect rect = new Rect(0, 0, sizePx, sizePx);
        RectF rectF = new RectF(rect);

// 1. 先画一个白色的底圆（作为蒙版的目标）
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL); // 确保是填充模式
        canvas.drawOval(rectF, paint);

// 2. 【关键修复】设置混合模式为 SRC_IN
// SRC_IN 的含义是：只保留源图像（你的商家图片）在目标图像（白圆）范围内的内容。
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(scaledBitmap, rect, rect, paint);

        // --- 核心步骤 B：绘制红/绿边框 ---
        paint.setXfermode(null); // 还原混合模式
        paint.setStyle(Paint.Style.STROKE); // 设置为描边模式
        paint.setStrokeWidth(borderWidthPx); // 边框宽度
        paint.setColor(borderColor); // 设置为传入的红/绿颜色

        // 绘制边框 (需要向内偏移半个边框宽度，避免边缘切断)
        canvas.drawOval(new RectF(borderWidthPx/2f, borderWidthPx/2f,
                sizePx - borderWidthPx/2f, sizePx - borderWidthPx/2f), paint);

        // 释放临时资源
        scaledBitmap.recycle();

        // 4. 将 Bitmap 转换为高德需要的 BitmapDescriptor
        return BitmapDescriptorFactory.fromBitmap(outputBitmap);
    }
}