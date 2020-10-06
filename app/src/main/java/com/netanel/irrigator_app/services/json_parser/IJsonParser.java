package com.netanel.irrigator_app.services.json_parser;

import java.io.Reader;
import java.lang.reflect.Type;

/**
 * <p>Supplies an interface for parsing JSON data.</p>
 * Implement to create a custom {@code IJsonParser}
 * @author Netanel Iting
 * @version %I%, %G%
 * @since 1.0
 */
public interface IJsonParser {

    /**
     * Parses a data stream containing a json input and map it into a class of the requested type.
     * @param reader a data reader object extending {@link Reader}, containing a json input.
     * @param typeOfT the type of the requested object to map.
     * @param <T> the type of the requested object to map (same as {@code typeOfT}).
     * @return an object of type {@code <T>} mapped from the json input, if no suitable
     * object was found, returns {@code null}.
     */
    <T> T fromJson(Reader reader, Type typeOfT);

    /**
     * Parses a data stream containing a json input and map it into a class of the requested type.
     * @param reader a data reader object extending {@link Reader}, containing a json input.
     * @param typeOfT the type of the requested object to map.
     * @param memberName the member name of the object in the json input.
     * @param <T> the type of the requested object to map (same as {@code typeOfT}).
     * @return an object of type {@code <T>} mapped from the json input, if no suitable
     * object was found, returns {@code null}.
     */
    <T> T fromJson(Reader reader, Type typeOfT, String memberName);

    /**
     * Parses a string containing a json input and map it into a class of the requested type.
     * @param json a string containing a json input.
     * @param typeOfT the type of the requested object to map.
     * @param <T> the type of the requested object to map (same as {@code typeOfT}).
     * @return an object of type {@code <T>} mapped from the json input, if no suitable
     * object was found, returns {@code null}.
     */
    <T> T fromJson(String json, Type typeOfT);

    /**
     * Parses a string containing a json input and map it into a class of the requested type.
     * @param json a string containing a json input.
     * @param typeOfT the type of the requested object to map.
     * @param memberName the member name of the object in the json input.
     * @param <T> the type of the requested object to map (same as {@code typeOfT}).
     * @return an object of type {@code <T>} mapped from the json input, if no suitable
     * object was found, returns {@code null}.
     */
    <T> T fromJson(String json, Type typeOfT, String memberName);

    /***
     * map an object and convert it to a Json string.
     * @param object an object to parse.
     * @param <T> type of object to parse.
     * @return a Json string representing the mapped object.
     */
    <T> String toJson(T object);
}


