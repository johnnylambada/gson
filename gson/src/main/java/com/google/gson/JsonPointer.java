/*
 * Copyright (C) 2017 John Lombardo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * The JsonPointer class implements much of RFC6901 Json Pointers for GSON Json documents. It can
 * also set values of elements to any valid JsonElement using pointer. For Example:
 * <code>
 final String originalJson   = "{}";
 final String expectedValue  = "baz";
 final String reasonPointer  = "/foo/bar";
 final JsonPointer pointer   = new JsonPointer(originalJson);

 assertEquals(null,pointer.dereference(reasonPointer));
 pointer.set(reasonPointer,new JsonPrimitive(expectedValue));
 assertEquals(expectedValue,pointer.dereference(reasonPointer).getAsString());
 System.out.println(pointer.getElement().toString());
 * </code> produces the following <code>
 {"foo":{"bar":"baz"}}
 * </code>
 */
public class JsonPointer {
    private final JsonElement root;

    JsonPointer(JsonElement root){
        this.root = root;
    }

    public JsonElement getElement(){
        return root;
    }

    public JsonElement dereference(String pointer) {
        return dereference(pointer,0);
    }

    public JsonElement dereference(String pointer,int generation) {
        if (pointer==null)
            return null;
        List<JsonElement> list = getPath(pointer,false);
        return list.get(list.size()-1-generation);
    }

    public void set(String pointer, JsonElement value){
        if (pointer==null) return;
        Element last = getLastElement(pointer);
        JsonObject object = last.element.getAsJsonObject();
        object.remove(last.name);
        object.add(last.name,value);
    }

    private List<JsonElement> getPath(String pointer,boolean mutative){
        final String[] tokens=pointer.split("/",-1);
        final List<JsonElement> ret = new ArrayList<>(tokens.length);
        JsonElement element = null;
        for (int i = 0; i < tokens.length; i++) {
            final String token = unescape(tokens[i]);
            final String tokenNext = i == tokens.length - 1 ? null : unescape(tokens[i + 1]);
            if (i == 0) {
                element = root;
                ret.add(element);
            } else {
                if (element.isJsonArray() && "-".equals(token)){
                    element = onNewArrayElementRequested(element.getAsJsonArray());
                } else {
                    int seq = getInt(token);
                    if (seq < 0) {
                        final JsonObject object = element.getAsJsonObject();
                        element = object.has(token)
                                ? object.get(token)
                                : mutative
                                ? onTokenNotFound(object,token,tokenNext)
                                : null;
                    } else {
                        final JsonArray array = element.getAsJsonArray();
                        element = seq<array.size()
                                ? array.get(seq)
                                : mutative
                                ? onSequenceNotFound(array,seq,tokenNext)
                                : null;
                    }
                }
                ret.add(element);
                if (element==null){
                    break;
                }
            }
        }
        return ret;
    }

    private String unescape(String s){
        return s.replaceAll("~1","/").replaceAll("~0","~");
    }

    private int getInt(String s){
        int ret = -1;
        try {
            ret = Integer.parseInt(s);
        } catch (NumberFormatException ignored){}
        return ret;
    }

    private JsonElement onSequenceNotFound(JsonArray element, int seq, String tokenNext){
        JsonElement newElement=null;
        for(int i=element.size(); i<=seq; i++){
            newElement = buildHolderOf(tokenNext);
            element.add(newElement);
        }
        return newElement;
    }

    private JsonElement onTokenNotFound(JsonObject element, String token, String tokenNext){
        JsonElement newElement=buildHolderOf(tokenNext);
        element.add(token,newElement);
        return newElement;
    }

    private JsonElement onNewArrayElementRequested(JsonArray element){
        JsonObject object = new JsonObject();
        element.add(object);
        return object;
    }

    private Element getLastElement(String pointer){
        final int lastSlash          = pointer.lastIndexOf('/');
        final String parentPointer   = pointer.substring(0,lastSlash);
        final String objectName      = pointer.substring(lastSlash+1);
        final List<JsonElement> list = getPath(parentPointer,true);
        final JsonElement element    = list.get(list.size()-1);
        return new Element(element,objectName);
    }

    private JsonElement buildHolderOf(String token){
        if (token==null){
            return new JsonObject();
        } else if ("-".equals(token) || getInt(token)>=0) {
            return new JsonArray();
        } else {
            return new JsonObject();
        }
    }

    private static class Element {
        final JsonElement element;
        final String name;
        public Element(JsonElement element, String name){
            this.element = element;
            this.name = name;
        }
    }
}
