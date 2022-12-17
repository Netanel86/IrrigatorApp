package com.netanel.irrigator_app.services.connection;


import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 21/09/2020
 */

public interface IDataBaseConnection {
    <T> IQueryBuilder<T> getCollection(String collectionPath, @NonNull Class<T> dataType);

    <T> void addDocumentListener(@NonNull String collectionPath,
                                 @Nullable String docId,
                                 @NonNull Class<T> dataType,
                                 @NonNull TaskListener<T> documentChangedListener);
    <T> void addCollectionListener(@NonNull String collectionPath,
                                          @NonNull Class<T> dataType,
                                          @NonNull final TaskListener<List<T>> documentChangedListener);
    <T extends IMappable> void addDocument(@NonNull T document,
                                           @NonNull String collectionPath,
                                           @NonNull Class<T> dataType,
                                           TaskListener<T> taskCompletedListener);
    void removeAllListeners();

    interface TaskListener<T> {
        void onComplete(T result);
        void onFailure(Exception exception);
    }

    interface IQueryBuilder<T> {
        IQueryBuilder<T> orderBy(String field, Direction direction);
        void get(@NonNull final TaskListener<List<T>> taskCompletedListener);
    }

    enum Direction {
        ASCENDING,
        DESCENDING
    }


}
