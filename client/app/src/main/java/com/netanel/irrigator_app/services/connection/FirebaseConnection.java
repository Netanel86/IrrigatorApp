package com.netanel.irrigator_app.services.connection;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * <p></p>
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 * Created on 21/09/2020
 */

public class FirebaseConnection implements IDataBaseConnection {

    private final FirebaseFirestore mDb;
    private final Map<String, ListenerRegistration> mDataListeners;

    public FirebaseConnection() {
        mDb = FirebaseFirestore.getInstance();
        mDataListeners = new HashMap<>();

        initializeDbSettings();
    }

    @Override
    public <T> IQueryBuilder<T> getCollection(String collectionPath, @NonNull Class<T> dataType) {
        return new FirebaseQueryBuilder<>(collectionPath, dataType);
    }

    @Override
    public <T> void addDocumentListener(@NonNull String collectionPath,
                                        String docId, @NonNull Class<T> dataType,
                                        @NonNull final TaskListener<T> documentChangedListener) {
        ListenerRegistration listener = mDb.collection(collectionPath).document(docId)
                .addSnapshotListener((docSnapshot, error) -> {
                    if (error != null) {
                        documentChangedListener.onFailure(error);
                    } else if (docSnapshot != null && docSnapshot.exists()) {
                        documentChangedListener.onComplete(docSnapshot.toObject(dataType));
                    }
                });

        mDataListeners.put(docId, listener);
    }
    @Override
    public <T> void addCollectionListener(@NonNull String collectionPath,
                                          @NonNull Class<T> dataType,
                                          @NonNull final TaskListener<List<T>> collectionChangedListener) {

        ListenerRegistration listener = mDb.collection(collectionPath)
                .addSnapshotListener((collSnapshot, error) -> {
            if (error != null) {
                collectionChangedListener.onFailure(error);
            } else if (collSnapshot != null && !collSnapshot.isEmpty()) {
                List<DocumentChange> changedDocs = collSnapshot.getDocumentChanges();
                List<T> changedObjects = new ArrayList<>();
                for (DocumentChange document : changedDocs) {
                    T object = document.getDocument().toObject(dataType);
                    changedObjects.add(object);
                }
                collectionChangedListener.onComplete(changedObjects);
            }
        });

        mDataListeners.put(collectionPath, listener);
    }

    @Override
    public <T extends IMappable> void addDocument(@NonNull final T document, @NonNull String collectionPath, @NonNull Class<T> dataType, final TaskListener<T> taskCompletedListener) {
        mDb.collection(collectionPath).add(document).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                document.setId(task.getResult().getId());
                if (taskCompletedListener != null) {
                    taskCompletedListener.onComplete(document);
                }
            } else {
                if (taskCompletedListener != null) {
                    taskCompletedListener.onFailure(task.getException());
                }

            }
        });
    }

    @Override
    public void removeAllListeners() {
        for (ListenerRegistration listener : mDataListeners.values()) {
            listener.remove();
        }
        mDataListeners.clear();
    }

    private <T> void onTaskCompleted(@NonNull Task<QuerySnapshot> task,
                                     @NonNull String collectionPath,
                                     @NonNull Class<T> dataType,
                                     @NonNull final TaskListener<List<T>> taskCompletedListener) {

        if (task.isSuccessful()) {
            if (task.getResult() != null) {
                //task is successful
                LinkedList<T> collection = new LinkedList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    T item = document.toObject(dataType);
                    collection.add(item);
                }
                taskCompletedListener.onComplete(collection);
            } else {
                //task was successful but returned null result
                taskCompletedListener.onFailure(new NullResultException(collectionPath));
            }
        } else {
            //task is unsuccessful
            taskCompletedListener.onFailure(task.getException());
        }
    }

    private void initializeDbSettings() {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false).build();
        mDb.setFirestoreSettings(settings);
    }

    public class FirebaseQueryBuilder<T> implements IQueryBuilder<T> {

        private final String mCollectionPath;
        private final Class<T> mDataType;

        private final CollectionReference mCollectionRef;
        private Query mQuery;

        public FirebaseQueryBuilder(@NonNull String collectionPath, @NonNull Class<T> dataType) {
            mCollectionPath = collectionPath;
            mDataType = dataType;
            mCollectionRef = mDb.collection(mCollectionPath);
        }

        public IQueryBuilder<T> orderBy(String field, Direction direction) {
            mQuery = mCollectionRef.orderBy(field,
                    direction == Direction.ASCENDING ? Query.Direction.ASCENDING : Query.Direction.DESCENDING);
            return this;
        }

        @Override
        public void get(@NonNull final TaskListener<List<T>> taskCompletedListener) {
            if (mQuery != null) {
                mQuery.get().addOnCompleteListener(task ->
                        onTaskCompleted(task, mCollectionPath, mDataType, taskCompletedListener))
                        .addOnFailureListener(taskCompletedListener::onFailure);
            } else {
                mCollectionRef.get().addOnCompleteListener(task ->
                        onTaskCompleted(task, mCollectionPath, mDataType, taskCompletedListener))
                        .addOnFailureListener(taskCompletedListener::onFailure);
            }
        }
    }
}


