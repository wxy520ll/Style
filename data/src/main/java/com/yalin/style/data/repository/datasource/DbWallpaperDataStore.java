package com.yalin.style.data.repository.datasource;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import com.yalin.style.data.cache.WallpaperCache;
import com.yalin.style.data.entity.WallpaperEntity;
import com.yalin.style.data.log.LogUtil;
import com.yalin.style.data.repository.datasource.provider.StyleContract;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

import io.reactivex.Observable;

/**
 * @author jinyalin
 * @since 2017/4/18.
 */

public class DbWallpaperDataStore implements WallpaperDataStore {
    private static final String TAG = "DbWallpaperDataStore";

    private static final String DEFAULT_WALLPAPER_ID = "-1";

    private final Context context;

    private final WallpaperCache wallpaperCache;

    public DbWallpaperDataStore(Context context, WallpaperCache wallpaperCache) {
        this.context = context;
        this.wallpaperCache = wallpaperCache;
    }

    @Override
    public Observable<WallpaperEntity> getWallPaperEntity() {
        return createEntitiesObservable().doOnNext(wallpaperCache::put).map(Queue::peek);
    }

    @Override
    public Observable<WallpaperEntity> switchWallPaperEntity() {
        return getWallPaperEntity();
    }

    @Override
    public Observable<InputStream> openInputStream(String wallpaperId) {
        return Observable.create(emitter -> {
            try {
                InputStream inputStream;
                if (DEFAULT_WALLPAPER_ID.equals(wallpaperId)) {
                    inputStream = context.getAssets().open("painterly-architectonic.jpg");
                } else {
                    inputStream = context.getContentResolver().openInputStream(
                            StyleContract.Wallpaper.buildWallpaperUri(wallpaperId));
                }
                emitter.onNext(inputStream);
                emitter.onComplete();
            } catch (IOException e) {
                LogUtil.D(TAG, "Open input stream failed for id : " + wallpaperId);
                emitter.onError(e);
            }
        });
    }

    @Override
    public Observable<Integer> getWallpaperCount() {
        return Observable.create(emitter -> {
            Cursor cursor = null;
            int count = 0;
            ContentResolver contentResolver = context.getContentResolver();
            cursor = contentResolver.query(StyleContract.Wallpaper.CONTENT_URI,
                    null, null, null, null);
            if (cursor != null) {
                count = cursor.getCount();
                cursor.close();
            }
            emitter.onNext(count);
            emitter.onComplete();
        });
    }

    private Observable<Queue<WallpaperEntity>> createEntitiesObservable() {
        return Observable.create(emitter -> {
            Cursor cursor = null;
            Queue<WallpaperEntity> validWallpapers = null;
            try {
                ContentResolver contentResolver = context.getContentResolver();
                cursor = contentResolver.query(StyleContract.Wallpaper.CONTENT_URI,
                        null, null, null, null);
                validWallpapers = readCursor(cursor);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (validWallpapers == null) {
                validWallpapers = new LinkedList<>();
            }
            validWallpapers.add(buildDefaultWallpaper());

            emitter.onNext(validWallpapers);
            emitter.onComplete();
        });
    }

    private Queue<WallpaperEntity> readCursor(Cursor cursor) {
        Queue<WallpaperEntity> validWallpapers = new LinkedList<>();
        while (cursor != null && cursor.moveToNext()) {
            WallpaperEntity wallpaperEntity = readEntityFromCursor(cursor);
            try {
                // valid input stream
                InputStream is = context.getContentResolver().openInputStream(
                        StyleContract.Wallpaper.buildWallpaperUri(
                                wallpaperEntity.wallpaperId));
                validWallpapers.add(wallpaperEntity);
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
                LogUtil.D(TAG, "File not found with wallpaper id : "
                        + wallpaperEntity.wallpaperId);
            }
        }
        return validWallpapers;
    }

    private WallpaperEntity readEntityFromCursor(Cursor cursor) {
        WallpaperEntity wallpaperEntity = new WallpaperEntity();

        wallpaperEntity.id = cursor.getInt(cursor.getColumnIndex(
                StyleContract.Wallpaper._ID));
        wallpaperEntity.title = cursor.getString(cursor.getColumnIndex(
                StyleContract.Wallpaper.COLUMN_NAME_TITLE));
        wallpaperEntity.wallpaperId = cursor.getString(cursor.getColumnIndex(
                StyleContract.Wallpaper.COLUMN_NAME_WALLPAPER_ID));
        wallpaperEntity.imageUri = cursor.getString(cursor.getColumnIndex(
                StyleContract.Wallpaper.COLUMN_NAME_IMAGE_URI));
        wallpaperEntity.byline = cursor.getString(cursor.getColumnIndex(
                StyleContract.Wallpaper.COLUMN_NAME_BYLINE));
        wallpaperEntity.attribution = cursor.getString(cursor.getColumnIndex(
                StyleContract.Wallpaper.COLUMN_NAME_ATTRIBUTION));

        return wallpaperEntity;
    }

    private WallpaperEntity buildDefaultWallpaper() {
        WallpaperEntity wallpaperEntity = new WallpaperEntity();
        wallpaperEntity.id = -1;
        wallpaperEntity.attribution = "kinglloy.com";
        wallpaperEntity.byline = "Lyubov Popova, 1918";
        wallpaperEntity.imageUri = "imageUri";
        wallpaperEntity.title = "Painterly Architectonic";
        wallpaperEntity.wallpaperId = DEFAULT_WALLPAPER_ID;
        return wallpaperEntity;
    }
}
