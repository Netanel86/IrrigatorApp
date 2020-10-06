package com.netanel.irrigator_app.services.connection;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.netanel.irrigator_app.model.Command;
import com.netanel.irrigator_app.model.Valve;

import java.util.LinkedHashMap;
import java.util.Map;

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

    private FirebaseFirestore mDb;

    public FirebaseConnection() {
        mDb = FirebaseFirestore.getInstance();
    }

    @Override
    public void getValves(final TaskListener<Map<String, Valve>> result) {
        mDb.collection(PATH_VALVES).orderBy("index", Query.Direction.ASCENDING).get().addOnCompleteListener(
                new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        //task is successful
                        if (task.isSuccessful() && task.getResult() != null) {
                            LinkedHashMap<String, Valve> valves = new LinkedHashMap<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Valve valve = document.toObject(Valve.class);
                                valves.put(valve.getId(), valve);
                            }
                            result.onComplete(valves, null);
                        }
                        //task is unsuccessful
                        else if (!task.isSuccessful() || task.getResult() == null) {
                            if(task.getException() != null) {
                                result.onComplete(null, task.getException());
                            } else {
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
                if(task.isSuccessful() && task.getResult() != null) {
                    onComplete.onComplete(task.getResult().getId(),null);
                }
                else {

                }
            }
        });
    }
}


