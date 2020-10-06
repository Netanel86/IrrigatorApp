package com.netanel.irrigator_app.services.json_parser;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * <p>Adapter representing a Json deserializer for the Gson library,
 * implements {@code JsonDeserializer<T>} to search for the registered type {@code <T>}
 * in the Json input. the scenarios this adapter handles are:</p>
 * <ul>
 *     <li>The input is a JsonObject containing an array of type {@code <T>} objects
 *     with a specified member name (should specify a member name).</li>
 *     <li>The input is a JsonArray representing a collection of type {@code <T>} objects.
 *     <li>The input is a JsonObject containing an object of type {@code <T>}
 *     with a specified member name (should specify a member name).</li>
 *     <li>The input is a JsonObject representing a single instance of a type {@code <T>} object.
 * </ul>
 *
 * @see JsonDeserializer
 * @see com.google.gson.JsonObject
 * @see com.google.gson.JsonArray
 *
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 */
public class GsonClassTypeAdapter<T> implements JsonDeserializer<T> {
    private Type classType;
    private String memberName;

    /**
     * Creates a new instance of {@code GsonClassTypeAdapter} class.
     * Use this constructor when the json represents either an array containing items
     * of the {@code targetClass} type, or its a single object of the {@code targetClass} type.
     * @param targetClass the target class type for which this deserializer should register to,
     *                    should be identical to {@code <T>}.
     */
    public GsonClassTypeAdapter(Type targetClass) {
        if (targetClass == null) {
            throw new IllegalArgumentException(
                    GsonClassTypeAdapter.class.getSimpleName() + ": 'targetClass' cant be null");
        }
        classType = targetClass;
    }

    /**
     * Creates a new instance of {@code GsonClassTypeAdapter} class.
     * Use this constructor when the {@code targetClass} type is contained in the json as a member
     * field either as an object or a collection.
     * @param targetClass the target class type for which this deserializer should register to,
     *                    should be identical to {@code <T>}.
     * @param memberName the name of the requested member described in the json input.
     */
    public  GsonClassTypeAdapter(Type targetClass, String memberName){
        this(targetClass);
        if ((this.memberName = memberName) == null ) {
            throw new IllegalArgumentException(GsonClassTypeAdapter.class.getSimpleName() +
                    ": 'memberName' cant be null. use the designated constructor if your input "
                    + "is represented as a whole object, otherwise specify a member name.");
        }
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonElement element = null;
        Type listType = classType instanceof ParameterizedType ?
                ((ParameterizedType) classType).getRawType() : null;

        if (listType != null &&
                Collection.class.isAssignableFrom((Class<?>) listType)) {
            //case the registered type is a collection
            element = searchForList(json);
        } else if (!json.getAsJsonObject().entrySet().isEmpty()) {
            //case the registered type is a single object
            element = searchForObject(json);
        }
        return new Gson().fromJson(element, classType);
    }

    private JsonElement searchForList(JsonElement json) {
        JsonElement element = null;

        if (json.isJsonObject()) {
            if(!json.getAsJsonObject().entrySet().isEmpty()) {
                //case the collection is nested in a json object
                if (memberName == null) {
                    throw new IllegalArgumentException(
                            GsonClassTypeAdapter.class.getSimpleName()
                                    + ": 'memberName' cant be null, "
                                    + "are you trying to fetch a nested collection without "
                                    + "specifying a member name? the Json input is "
                                    + "not a collection");
                }
                element = json.getAsJsonObject().getAsJsonArray(memberName);
            }
        } else {
            //case the json object is a json array (collection)
            element = json.getAsJsonArray();
        }
        return element;
    }

    private JsonElement searchForObject(JsonElement json) {
        JsonElement element;
        if (memberName != null) {
            //if a name was specified, getStringSet the requested member
            element = json.getAsJsonObject().getAsJsonObject(memberName);
        } else {
            //if no name was specified, try to return the entire object
            element = json.getAsJsonObject();
        }
        return element;
    }
}
