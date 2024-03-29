package com.netanel.irrigator_app.services.connection;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.model.Valve;

import java.util.LinkedList;
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

public class FirebaseConnection implements IDataBaseConnection {

    private static final String PATH_VALVES = "valves";
    private static final String PATH_COMMANDS = "commands";

    private final FirebaseFirestore mDb;

    public FirebaseConnection() {
        mDb = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings =
                new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build();
        mDb.setFirestoreSettings(settings);
    }

    @Override
    public void getValves(final TaskListener<List<Valve>> result) {

        mDb.collection(PATH_VALVES).orderBy("index", Query.Direction.ASCENDING).get().addOnCompleteListener(
                new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            //task is successful and has a non empty result
                            LinkedList<Valve> valves = new LinkedList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Valve valve = document.toObject(Valve.class);
                                valves.add(valve);
                            }
                            result.onComplete(valves, null);
                        } else {
                            if (task.getException() != null) {
                                //task is unsuccessful
                                result.onComplete(null, task.getException());
                            } else {
                                //task was successful but returned an empty or null result
                                result.onComplete(null, null);
                            }
                        }
                    }
                });
    }

    @Override
    public void addCommand(final Command command, final TaskListener<Command> onComplete) {
        mDb.collection(PATH_COMMANDS).add(command)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (!task.isSuccessful()) {
                            if (onComplete != null) {
                                onComplete.onComplete(null, task.getException());
                            }

                        }
                        else if(task.getResult() != null) {
                            if (onComplete != null) {
                                command.setId(task.getResult().getId());
                                onComplete.onComplete(command,null);
                            }
                        }
                    }
                });
    }

    @Override
    public void addOnValveChangedListener(String docId, final OnDataChangedListener<Valve> dataChangedListener) {
        mDb.collection(PATH_VALVES).document(docId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    dataChangedListener.onDataChanged(null, error);
                }
                else if (value != null && value.exists()) {
                    dataChangedListener.onDataChanged(value.toObject(Valve.class), null);
                }
            }
        });
    }

    public void addValve(final Valve valve, final TaskListener<String> onComplete) {
        mDb.collection(PATH_VALVES).add(valve).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if(task.isSuccessful() && task.getResult() != null && onComplete != null) {
                    onComplete.onComplete(task.getResult().getId(),null);
                }
                else {

                }
            }
        });
    }
}


