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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.junit.Test;

import com.google.gson.JsonPointer;

import static org.junit.Assert.assertEquals;

/*
{
	"library": {
		"name": "library of congress",
		"section": [{
			"name": "sci-fi",
			"title": [{
				"book": {
					"name": "Mote in Gods Eye",
					"author": ["Larry Niven", "Jerry Pournelle"]
				}
			}, {
				"book": {
					"name": "Ringworld",
					"author": ["Larry Niven"]
				}
			}]
		}]
	}
}

RFC 6901
{
      "foo": ["bar", "baz"],
      "": 0,
      "a/b": 1,
      "c%d": 2,
      "e^f": 3,
      "g|h": 4,
      "i\\j": 5,
      "k\"l": 6,
      " ": 7,
      "m~n": 8
}
 */
public class JsonPointerTest {
    private final String JSON = "{\n" +
            "\t\"library\": {\n" +
            "\t\t\"name\": \"library of congress\",\n" +
            "\t\t\"section\": [{\n" +
            "\t\t\t\"name\": \"sci-fi\",\n" +
            "\t\t\t\"title\": [{\n" +
            "\t\t\t\t\"book\": {\n" +
            "\t\t\t\t\t\"name\": \"Mote in Gods Eye\",\n" +
            "\t\t\t\t\t\"author\": [\"Larry Niven\", \"Jerry Pournelle\"]\n" +
            "\t\t\t\t}\n" +
            "\t\t\t}, {\n" +
            "\t\t\t\t\"book\": {\n" +
            "\t\t\t\t\t\"name\": \"Ringworld\",\n" +
            "\t\t\t\t\t\"author\": [\"Larry Niven\"]\n" +
            "\t\t\t\t}\n" +
            "\t\t\t}]\n" +
            "\t\t}]\n" +
            "\t}\n" +
            "}";
    private final String JSON_6901 = "{\n" +
            "      \"foo\": [\"bar\", \"baz\"],\n" +
            "      \"\": 0,\n" +
            "      \"a/b\": 1,\n" +
            "      \"c%d\": 2,\n" +
            "      \"e^f\": 3,\n" +
            "      \"g|h\": 4,\n" +
            "      \"i\\\\j\": 5,\n" +
            "      \"k\\\"l\": 6,\n" +
            "      \" \": 7,\n" +
            "      \"m~n\": 8\n" +
            "   }";

    @Test
    /**
     * See https://tools.ietf.org/html/rfc6901
     */
    public void rfc6901Test() throws Exception {
        JsonElement root = new JsonParser().parse(JSON_6901);
        JsonPointer json = root.getPointer();
        assertEquals(root,json.dereference(""));
        assertEquals(new JsonParser().parse("[\"bar\", \"baz\"]"),json.dereference("/foo"));
        assertEquals("bar",json.dereference("/foo/0").getAsString());
        assertEquals(0,json.dereference("/").getAsInt());
        assertEquals(1,json.dereference("/a~1b").getAsInt());
        assertEquals(2,json.dereference("/c%d").getAsInt());
        assertEquals(3,json.dereference("/e^f").getAsInt());
        assertEquals(4,json.dereference("/g|h").getAsInt());
        assertEquals(5,json.dereference("/i\\j").getAsInt());
        assertEquals(6,json.dereference("/k\"l").getAsInt());
        assertEquals(7,json.dereference("/ ").getAsInt());
        assertEquals(8,json.dereference("/m~0n").getAsInt());
    }


    @Test
    public void constructorTest() throws Exception {
        JsonElement expected = new JsonParser().parse("{}");
        JsonPointer json = expected.getPointer();
        assertEquals(expected,json.getElement());
    }

    @Test
    public void getRootTest() throws Exception {
        JsonElement expected = new JsonParser().parse(JSON);
        JsonPointer json = expected.getPointer();
        JsonElement actual = json.dereference("");
        assertEquals(expected,actual);
    }

    @Test
    public void getSimpleStringTest() throws Exception {
        JsonPointer json = new JsonParser().parse(JSON).getPointer();
        String expected = "library of congress";
        String actual = json.dereference("/library/name").getAsString();
        assertEquals(expected,actual);
    }

    @Test
    public void getSimpleStringInArrayTest() throws Exception {
        JsonPointer json = new JsonParser().parse(JSON).getPointer();
        String expected = "sci-fi";
        String actual = json.dereference("/library/section/0/name").getAsString();
        assertEquals(expected,actual);
    }

    @Test
    public void getDeepArrayTest() throws Exception {
        JsonPointer json = new JsonParser().parse(JSON).getPointer();
        String expected = "Jerry Pournelle";
        String actual = json.dereference("/library/section/0/title/0/book/author/1").getAsString();
        assertEquals(expected,actual);
    }

    @Test
    public void mutableTest() throws Exception {
        JsonPointer json = new JsonParser().parse(JSON).getPointer();
        final String expected = "hello world";
        final String pointer = "/this/is/a/new/thing";
        json.set(pointer,new JsonPrimitive(expected));
        String actual = json.dereference(pointer).getAsString();
        assertEquals(expected,actual);
    }

    @Test
    public void setStringTestWithArrays() throws Exception {
        final String originalJson         = "[]";
        final String originalValue        = "hello earth";
        final String expectedValue        = "hello world";
        final String thingPointer         = "/4/this/is/a/0/new/thing";
        final JsonPointer pointer         = new JsonParser().parse(originalJson).getPointer();

        assertEquals(null,pointer.dereference(thingPointer));
        pointer.set(thingPointer,new JsonPrimitive(originalValue));
        assertEquals(originalValue,pointer.dereference(thingPointer).getAsString());
        pointer.set(thingPointer,new JsonPrimitive(expectedValue));
        assertEquals(expectedValue,pointer.dereference(thingPointer).getAsString());
    }

    @Test
    public void setStringTest() throws Exception {
        final String originalJson         = "{}";
        final String originalValue        = "hello earth";
        final String expectedValue        = "hello world";
        final String thingPointer         = "/this/is/a/0/new/thing";
        final JsonPointer pointer         = new JsonParser().parse(originalJson).getPointer();

        assertEquals(null,pointer.dereference(thingPointer));
        pointer.set(thingPointer,new JsonPrimitive(originalValue));
        assertEquals(originalValue,pointer.dereference(thingPointer).getAsString());
        pointer.set(thingPointer,new JsonPrimitive(expectedValue));
        assertEquals(expectedValue,pointer.dereference(thingPointer).getAsString());
    }

    @Test
    public void setBooleanTest() throws Exception {
        final String originalJson    = "{}";
        final boolean originalValue  = false;
        final boolean expectedValue  = true;
        final String thingPointer    = "/this/is/a/0/new/thing";
        final JsonPointer pointer    = new JsonParser().parse(originalJson).getPointer();

        assertEquals(null,pointer.dereference(thingPointer));
        pointer.set(thingPointer,new JsonPrimitive(originalValue));
        assertEquals(originalValue,pointer.dereference(thingPointer).getAsBoolean());
        pointer.set(thingPointer,new JsonPrimitive(expectedValue));
        assertEquals(expectedValue,pointer.dereference(thingPointer).getAsBoolean());
    }

    @Test
    public void getThenSetTest() throws Exception {
        final String originalJson   = "{}";
        final String expectedValue  = "a reason";
        final String reasonPointer  = "/data/extensions/currentVisit/reason";
        final JsonPointer pointer    = new JsonParser().parse(originalJson).getPointer();

        assertEquals(null,pointer.dereference(reasonPointer));
        pointer.set(reasonPointer,new JsonPrimitive(expectedValue));
        assertEquals(expectedValue,pointer.dereference(reasonPointer).getAsString());
    }
    @Test
    public void setTest() throws Exception {
        final String originalJson = "{}";
        final String thingPointer = "/this/is/a/0/new/thing";
        final JsonPointer pointer = new JsonParser().parse(originalJson).getPointer();
        final String value1       = "ading";
        final int value2          = 1;
        final boolean value3      = true;

        assertEquals(null,pointer.dereference(thingPointer));
        pointer.set(thingPointer,new JsonPrimitive(value1));
        assertEquals(value1,pointer.dereference(thingPointer).getAsString());
        pointer.set(thingPointer,new JsonPrimitive(value2));
        assertEquals(value2,pointer.dereference(thingPointer).getAsInt());
        pointer.set(thingPointer,new JsonPrimitive(value3));
        assertEquals(value3,pointer.dereference(thingPointer).getAsBoolean());
    }
    @Test
    public void exampleTest() throws Exception {
        final String originalJson   = "{}";
        final String expectedValue  = "baz";
        final String reasonPointer  = "/foo/bar";
        final JsonPointer pointer   = new JsonParser().parse(originalJson).getPointer();

        assertEquals(null,pointer.dereference(reasonPointer));
        pointer.set(reasonPointer,new JsonPrimitive(expectedValue));
        assertEquals(expectedValue,pointer.dereference(reasonPointer).getAsString());
        System.out.println(pointer.getElement().toString());
    }
}
