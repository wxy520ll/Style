package com.yalin.style.domain.interactor;

import com.fernandocejas.arrow.checks.Preconditions;
import com.yalin.style.domain.executor.PostExecutionThread;
import com.yalin.style.domain.executor.ThreadExecutor;
import com.yalin.style.domain.interactor.LikeWallpaper.Params;
import com.yalin.style.domain.repository.SourcesRepository;

import io.reactivex.Observable;

import javax.inject.Inject;

/**
 * YaLin
 * On 2017/4/30.
 */

public class LikeWallpaper extends UseCase<Boolean, Params> {

    private SourcesRepository sourcesRepository;

    @Inject
    public LikeWallpaper(ThreadExecutor threadExecutor,
                         PostExecutionThread postExecutionThread,
                         SourcesRepository sourcesRepository) {
        super(threadExecutor, postExecutionThread);
        this.sourcesRepository = sourcesRepository;
    }


    @Override
    Observable<Boolean> buildUseCaseObservable(LikeWallpaper.Params params) {
        Preconditions.checkNotNull(params);
        return sourcesRepository.getWallpaperRepository().likeWallpaper(params.wallpaperId);
    }

    public static final class Params {

        private final String wallpaperId;

        private Params(String wallpaperId) {
            this.wallpaperId = wallpaperId;
        }

        public static LikeWallpaper.Params likeWallpaper(String wallpaperId) {
            return new LikeWallpaper.Params(wallpaperId);
        }
    }
}
